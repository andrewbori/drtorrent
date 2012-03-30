package hu.bute.daai.amorg.drtorrent.torrentengine;

import hu.bute.daai.amorg.drtorrent.DrTorrentTools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Vector;

import android.util.Log;

public class PeerConnection {
	private final static String LOG_TAG = "PeerConnection";
	
	private final static String MESSAGE_PROTOCOL_ID = "BitTorrent protocol";
	private final static int MESSAGE_ID_CHOKE 		   = 0;
	private final static int MESSAGE_ID_UNCHOKE        = 1;
	private final static int MESSAGE_ID_INTERESTED     = 2;
	private final static int MESSAGE_ID_NOT_INTERESTED = 3;
	private final static int MESSAGE_ID_HAVE           = 4;
	private final static int MESSAGE_ID_BITFIELD       = 5;
	private final static int MESSAGE_ID_REQUEST        = 6;
	private final static int MESSAGE_ID_PIECE          = 7;
	private final static int MESSAGE_ID_CANCEL         = 8;
	
	private final static int TIMEOUT_TCP_CONNECTION = 15;
	private final static int TIMEOUT_HANDSHAKE      = 15;
	private final static int TIMEOUT_PW_CONNECTION  = 2 * 60;
	private final static int TIMEOUT_REQUEST        = 60;
    private final static int KEEP_ALIVE_INTERVAL    = 2 * 60;	// once in every two minutes
    private final static int MAX_PIECE_REQUESTS     = 5;
	
	public final static int STATE_NOT_CONNECTED  = 0;
    public final static int STATE_TCP_CONNECTING = 1;
    public final static int STATE_TCP_CONNECTED  = 2;
    public final static int STATE_PW_HANDSHAKING = 3;
    public final static int STATE_PW_CONNECTED   = 4;
    public final static int STATE_CLOSING        = 5;
    
    public final static int ERROR_DELETE_PEER = 0;
    public final static int ERROR_INCREASE_ERROR_COUNTER = 1;
    public final static int ERROR_NOT_SPECIFIED = 2;
	
	private Peer peer_;
	private Torrent torrent_;
	private TorrentManager torrentManager_;
	private Vector<Block> blocksToDownload_;
	
	private boolean isIncomingConnection_;
	private boolean isReadEnabled_;
	private int state_;
	
	private boolean isInterested_;			// I am interested in the peer
	private boolean isPeerInterested_;		// The peer finds me interesting
	private boolean isChoking_;				// I am choking the peer
	private boolean isPeerChoking_;			// The peer is choking me
	
	private ConnectThread connectThread_;
	private Socket socket_;
	private InputStream inputStream_;
	private OutputStream outputStream_;
	
	private int ellapsedTime_ = 0;
	private int lastRequestTime_ = 0;
	private int lastMessageReceivedTime_ = 0;
	private int lastMessageSentTime_ = 0;
	private int reconnectAfter_;
	public static int tcpConnectionTimeoutNum_ = 0;
	private boolean hasPendingDownloadRequest_;
	
	/** Creates a new instance of PeerConnection. */
	public PeerConnection(Peer peer, Torrent torrent, TorrentManager torrentManager) {
        peer_ = peer;
        torrent_ = torrent;
        torrentManager_ = torrentManager;
        isIncomingConnection_ = false;

        isInterested_ = false;
        isPeerInterested_ = false;
        isChoking_ = true;
        isPeerChoking_ = true;

        blocksToDownload_ = new Vector<Block>();

        state_ = STATE_NOT_CONNECTED;
        isReadEnabled_ = true;
    }
	
	/** Class containing information about the piece to download. */
	/*private class PieceToDownload {
	    public Piece piece;
	    public boolean hasPendingRequest;
	    public int lastRequestTime;
	    public int lastRequestBegin;
	    public int lastRequestLength;    

	    public PieceToDownload(Piece piece0, int lastRequestTime0){
	        piece = piece0;
	        lastRequestTime = lastRequestTime0;
	        hasPendingRequest = false;
	        lastRequestBegin = -1;
	        lastRequestLength = -1;
	    }
	}*/
	
	/** Connects to the peer (including the peer wire connection). */
	public void connect() {
		String destination = peer_.getAddress() + ":" + peer_.getPort();
		connectThread_ = new ConnectThread(destination);
		connectThread_.start();
	}

	/** Schedules the connection. Mainly for checking timeouts. */
	public void onTimer() {
		ellapsedTime_++;
		if (reconnectAfter_ > 0) reconnectAfter_--;

		switch (state_) {
			
			case STATE_TCP_CONNECTING:
				if (ellapsedTime_ > TIMEOUT_TCP_CONNECTION) {
					tcpConnectionTimeoutNum_++;
					close(ERROR_INCREASE_ERROR_COUNTER, "Timeout while trying to connect.");
				}
				break;

			case STATE_PW_HANDSHAKING:
				if (ellapsedTime_ > TIMEOUT_HANDSHAKE) {
					close(ERROR_INCREASE_ERROR_COUNTER, "Handshake timeout (no data received).");
				}
				break;

			case STATE_PW_CONNECTED:
				// Timeout: Because no message have been received in the recent time
				if ((ellapsedTime_ - lastMessageReceivedTime_) > TIMEOUT_PW_CONNECTION) {
					close(ERROR_INCREASE_ERROR_COUNTER, "General timeout (no data received)");
					break;
				}

				if (lastRequestTime_ > 0 && ((ellapsedTime_ - lastRequestTime_) > TIMEOUT_REQUEST)) {
					lastRequestTime_ = 0;
					/*if (torrent_.hasTimeoutlessPeer()) {
					 peer_.setHadRequestTimeout(true);
					 close(EIncreaseErrorCounter, "Request timeout"); break; }
					 */
					close(ERROR_INCREASE_ERROR_COUNTER, "Request timeout");
					break;
					
				}
				
				// Nobody interested
				if (ellapsedTime_ > 10 && !isInterested() && !isPeerInterested()) {
					close("Nobody interested!");
				}
				
				// Keep alive: because we have not sent message in the recent time
				if ((ellapsedTime_ - lastMessageSentTime_) >= KEEP_ALIVE_INTERVAL) {
					sendKeepAliveMessage();
				}
				
				if (blocksToDownload_.size() == 0) {
					issueDownload();
				}

				break;

			default:
				break;
		}
	}
	
	/** Reads at most data.length bytes from the inputStream_ and puts it into the data array. */
	private boolean readData(byte [] data) throws Exception {
        return readData(data, 0, data.length);
    }

	/** Reads at most len bytes from the inputStream_ and puts it into the data array starting from the offset position. */
    private boolean readData(byte [] data, int offset, int len) throws Exception {
        int remain = len;
        while (remain > 0) {
            final int readed = inputStream_.read(data, offset, remain);
            if (readed == -1) {
                break;
            }
            remain -= readed;
            offset += readed;
        }
        return remain == 0;
    }
    
    /** Reads an integer from the inputStream_. */
	public int readInt() {
		byte[] array = new byte[4];
		try {
			readData(array);
		} catch (IOException ex) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Read error");
			return -1;
		} catch (Exception ex) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Read error");
			return -1;
		}

		return byteArrayToInt(array);
	}
	
    /** Reads data from the inputStream_. */
	private void read() {
        int messageLength = 0;

        while (isReadEnabled_) {
            try {
                switch (state_) {
                	
                    case STATE_PW_HANDSHAKING: {
                        readHandshakeMessage();
                        break;
                    }
                    
					case STATE_PW_CONNECTED: {
						byte[] initData = new byte[4];

						readData(initData);

						messageLength = byteArrayToInt(initData);

						lastMessageReceivedTime_ = ellapsedTime_;
						if (messageLength == 0) {
							// keep-alive
							//Log.v(LOG_TAG, "Reading keep alive message from: " + peer_.getAddress());
							// TODO: we have to close connection,
							issueDownload();
						} else {
							int id = inputStream_.read();
							switch (id) {
								
								case MESSAGE_ID_CHOKE:
									Log.v(LOG_TAG, "Reading choke message from: " + peer_.getAddress());
									readChokeMessage();
									break;

								case MESSAGE_ID_UNCHOKE:
									Log.v(LOG_TAG, "Reading unchoke message from: " + peer_.getAddress());
									readUnchokeMessage();
									break;

								case MESSAGE_ID_INTERESTED:
									Log.v(LOG_TAG, "Reading interested message from: " + peer_.getAddress());
									readInterestedMessage();
									break;
								case MESSAGE_ID_NOT_INTERESTED:
									Log.v(LOG_TAG, "Reading not interested message from: " + peer_.getAddress());
									readNotInterestedMessage();
									break;
								
								case MESSAGE_ID_BITFIELD:
									Log.v(LOG_TAG, "Reading bitfield message from: " + peer_.getAddress());
									readBitfieldMessage(messageLength);
									break;

								case MESSAGE_ID_HAVE:
									Log.v(LOG_TAG, "Reading have message from: " + peer_.getAddress());
									readHaveMessage();
									break;

								case MESSAGE_ID_PIECE:
									Log.v(LOG_TAG, "Reading piece message from: " + peer_.getAddress());
									readPieceMessage(messageLength);
									break;

								case MESSAGE_ID_REQUEST:
									Log.v(LOG_TAG, "Reading request message from: " + peer_.getAddress());
									readRequestMessage(messageLength);
									break;

								case MESSAGE_ID_CANCEL:
									Log.v(LOG_TAG, "Reading cancel message from: " + peer_.getAddress());
									readCancelMessage(messageLength);
									break;
							}
						}
						break;
					}
                }
            } catch (InterruptedIOException e) {
                close(ERROR_INCREASE_ERROR_COUNTER, "Read error");
                Log.v(LOG_TAG, "Interrupted IO exception: " + e.getMessage());
            } catch (IOException e) {
                close(ERROR_INCREASE_ERROR_COUNTER, "Read error");
                Log.v(LOG_TAG, "IO exception: " + e.getMessage());
            } catch (Exception e) {
                close(ERROR_INCREASE_ERROR_COUNTER, "Read error (???)");
                Log.v(LOG_TAG, "Read exception: " + e.getMessage());
            }
        }
    }
	
	/** Reading message: handshake. */
	private void readHandshakeMessage() throws InterruptedIOException, IOException, Exception {
		int protLength = inputStream_.read(); 	// pstrlen
		if (protLength == -1) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Peer disconnected!");
			return;
		}

		int handshakeLength = protLength + 48; // 49-1 because protLength has been read
		byte[] handshake = new byte[handshakeLength];
		readData(handshake);

		byte[] protocolId = new byte[protLength]; // pstr
		System.arraycopy(handshake, 0, protocolId, 0, protLength);

		if (!MESSAGE_PROTOCOL_ID.equals(new String(protocolId))) {
			close(ERROR_DELETE_PEER, "Protocol identifier doesn't match!");
			return;
		}

		byte[] infoHash = new byte[20]; // info_hash
		System.arraycopy(handshake, 27, infoHash, 0, 20);

		if (torrent_ != null) {
			if (!DrTorrentTools.byteArrayEqual(torrent_.getInfoHashByteArray(), infoHash)) {
				close(ERROR_DELETE_PEER, "Torrent infohash doesn't match!");
				return;
			}
		} else { // if torrent is null then we should attach peer to torrent (most likely it is an incoming connection)
		/*
		 * if((torrentManager_.attachPeerToTorrent(infoHash,
		 * getPeerConnection()) != MTErrorCodes.ErrNone) || (torrent_ == null))
		 * // close if the attach failed { close(EDeletePeer,
		 * "Invalid infohash or peer is already connected or too many peers!");
		 * continue; }
		 */
		}

		if (isIncomingConnection_) {
			/*
			 * torrent_.incIncomingConnectionsNum();
			 * sendHandshakeMessage();
			 * peer_.resetAddress();
			 */
		}

		byte[] peerId = new byte[20]; // peer_id
		System.arraycopy(handshake, 47, peerId, 0, 20);
		String tempPeerId = new String(peerId);
		if (peer_.getPeerId() != null) {
			if (!peer_.getPeerId().equals(tempPeerId)) {
				close(ERROR_INCREASE_ERROR_COUNTER, "Peer ID doesn't match!");
				return;
			} else if (tempPeerId.equals(torrentManager_.getPeerID())) {
				close(ERROR_DELETE_PEER, "Connected to ourselves!");
				return;
			}
		} else {
			// peer_.setPeerId(tempPeerId);
		}

		Log.v(LOG_TAG, "Handshake completed! Peer wire connected!");
		changeState(STATE_PW_CONNECTED);

		if (!torrent_.getBitfield().isNull()) sendBitfieldMessage();
	}
	
	/** Reading message: choke. */
	private void readChokeMessage() {
		peer_.resetErrorCounter();
		setPeerChoking(true);

		synchronized (blocksToDownload_) {
			Block block = null;
			for (int i = 0; i < blocksToDownload_.size(); i++) {
				block = blocksToDownload_.elementAt(i);
				block.setNotRequested();
				Piece piece = torrent_.getPiece(block.pieceIndex());
				piece.addBlockToRequest(block);
			}

			blocksToDownload_.removeAllElements();
		}
	}

	/** Reading message: unchoke. */
	private void readUnchokeMessage() {
		peer_.resetErrorCounter();
		setPeerChoking(false);
		issueDownload();
	}

	/** Reading message: interested. */
	private void readInterestedMessage() {
		peer_.resetErrorCounter();
		/*if (Preferences.UploadEnabled) {
			setPeerInterested(true);
			setChoking(false);
		}*/
	}

	/** Reading message: not interested. */
	private void readNotInterestedMessage() {
		peer_.resetErrorCounter();
		setPeerInterested(false);
		issueDownload();
	}

	/** Reading message: bitfield. */
	private void readBitfieldMessage(int messageLength) throws InterruptedIOException, IOException, Exception {
		int bitFieldLength = messageLength - 1; // bitfield length
		if (bitFieldLength != peer_.getBitfield().getLengthInBytes()) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Received bitfield length doesn't match!");
		} else {
			byte[] bitfield = new byte[bitFieldLength];
			boolean dataReaded = readData(bitfield);
			if (dataReaded) {
				peer_.peerHasPieces(bitfield, torrent_);
				Log.v(LOG_TAG, "Bitfield has been read from " + peer_.getAddress());
				issueDownload();
			} else {
				close(ERROR_INCREASE_ERROR_COUNTER, "Could not read bitfield!");
			}
		}	
	}

	/** Reading message: have. */
	private void readHaveMessage() {
		int pieceIndex = readInt();

		if ((pieceIndex >= 0) && (pieceIndex < torrent_.pieceCount())) {
			peer_.peerHasPiece(pieceIndex, torrent_);

			if (!hasPendingDownloadRequest_) issueDownload();
		}
	}

	/** Reading message: piece. */
	private void readPieceMessage(int messageLength) throws InterruptedIOException, IOException, Exception {
		peer_.resetErrorCounter();
		lastRequestTime_ = 0;
		//peer_.setHadRequestTimeout(false);
		int index = readInt();								// index
		int begin = readInt();								// begin
		int pieceBlockSize = messageLength - 9;

		Log.v(LOG_TAG, "Recieved PIECE, Index: " + index + " Begin: " + begin + " Length: " + pieceBlockSize);

		Block block = null;
		Piece piece = null;
		synchronized (blocksToDownload_) {
			for (int i = 0; i < blocksToDownload_.size(); i++) {
				block = blocksToDownload_.elementAt(i);
				if (block.pieceIndex() == index && block.begin() == begin) {
					piece = torrent_.getPiece(block.pieceIndex());
					break;
				}
			}
		}

		if (piece == null) {
			//close("Error, unexpected piece (there is no pending request for the received piece index)");
			byte[] pieceBlock = new byte[pieceBlockSize];	// read the unexpected block
			boolean successfullRead = readData(pieceBlock);
		} else {
			byte[] pieceBlock = new byte[pieceBlockSize];	// block
			boolean successfullRead = readData(pieceBlock);

			if (!successfullRead) {
				close(ERROR_INCREASE_ERROR_COUNTER, "Reading piece failed!");
				return;
			}

			// //if(torrent_.isEndGame()) torrent_.endGamePieceReceived(piece.piece, peer);

			int appendResult = piece.appendBlock(pieceBlock, block, peer_);
			
			blocksToDownload_.removeElement(block);
			pieceBlock = null;
			
			if (appendResult != Torrent.ERROR_NONE) {
				close("Writing to piece failed"); // CRITICAL FAULT
			} else {	
				Log.v(LOG_TAG, "Check remaining: " + piece.remaining());
				if (piece.remaining() == 0) {
					
				}
			}
		}
		System.gc();
		issueDownload();
	}

	/** Reading message: request. */
	private void readRequestMessage(int messageLength) throws InterruptedIOException, IOException, Exception {
		/*peer_.resetErrorCounter();
		if (messageLength < 13) {
			close(EIncreaseErrorCounter, "Received request message length is smaller than 13!");
		} else {
			int pieceIndex = readInt();
			int begin = readInt();
			int length = readInt();

			//log("in REQUEST Index: " + pieceIndex + " Begin: " + begin + " Length: " + length);

			if (Preferences.UploadEnabled) {
				ellapsedTime_ = 0; // ///////////////////////////////////////////

				incomingRequests_.addElement(new BlockRequest(pieceIndex, begin, length));

				issueUpload();
			}
		}*/
	}

	/** Reading message: cancel. */
	private void readCancelMessage(int messageLength) throws InterruptedIOException, IOException, Exception {
		/*peer_.resetErrorCounter();
		if (messageLength < 13) {
			close(EIncreaseErrorCounter, "Received CANCEL message length is smaller than 13!");
		} else {
			int pieceIndex = readInt();
			int begin = readInt();
			int length = readInt();

			for (int i = 0; i < incomingRequests_.size(); i++) {
				if (((BlockRequest) incomingRequests_.elementAt(i)).pieceIndex == pieceIndex
						&& ((BlockRequest) incomingRequests_.elementAt(i)).begin == begin
						&& ((BlockRequest) incomingRequests_.elementAt(i)).length == length) {
					incomingRequests_.removeElementAt(i);
					break;
				}
			}
		}*/
	}

	/** Sending message: handshake. */
	private void sendHandshakeMessage() {
		state_ = STATE_PW_HANDSHAKING;
        if (torrent_ != null) {
            Log.v(LOG_TAG, "Sending handshake to: " + peer_.getAddress());
            ByteArrayOutputStream baos = null;
            try {
                if (outputStream_ != null) {
                    byte[] zeros = {0, 0, 0, 0, 0, 0, 0, 0};

                    baos = new ByteArrayOutputStream();
                    baos.write((byte) MESSAGE_PROTOCOL_ID.length());	// pstrlen:   string length of <pstr>, as a single raw byte 
                    baos.write(MESSAGE_PROTOCOL_ID.getBytes());			// pstr:      string identifier of the protocol 
                    baos.write(zeros);									// reserved:  eight (8) reserved bytes. All current implementations use all zeroes.
                    baos.write(torrent_.getInfoHashByteArray());		// info_hash: 20-byte SHA1 hash of the info key in the metainfo file.
                    baos.write(torrentManager_.getPeerID().getBytes());	// peer_id:   20-byte string used as a unique ID for the client.
                    outputStream_.write(baos.toByteArray());
                    outputStream_.flush();

                    Log.v(LOG_TAG, "Handshake sent to: " + peer_.getAddress());
                } else {
                    close("Error while sending handshake, outputstream is NULL");
                }
            } catch (IOException ex) {
            	Log.v(LOG_TAG, "Error while sending handshake");
                close(ERROR_INCREASE_ERROR_COUNTER, "Error while writing");
            } catch (Exception ex) {
            	Log.v(LOG_TAG, "Error while sending handshake");
                close(ERROR_INCREASE_ERROR_COUNTER, "Error while writing");
            } finally {
            	try {
					if (baos != null) baos.close();
				} catch (IOException e) {}
            }
        } else Log.v(LOG_TAG, "Error: Torrent is not specified, cannot send handshake.");
    }
	
	/** Sending message: keep-alive. */
	public void sendKeepAliveMessage() {
        try {
            if (outputStream_ != null) {
                lastMessageSentTime_ = ellapsedTime_;
                
                outputStream_.write(intToByteArray(0));
                outputStream_.flush();
            } else {
                close("ERROR, while send keepalive, outputstream is NULL");
            }
        } catch (IOException e) {
            close(ERROR_INCREASE_ERROR_COUNTER, "Error while writing keepalive");
        } catch (Exception e) {
            close(ERROR_INCREASE_ERROR_COUNTER, "Error while writing keepalive");
        }
    }
	
	/** Sending message: choke. */
	private void sendChokeMessage() {
		ByteArrayOutputStream baos = null;
        try {
            if (outputStream_ != null) {
                lastMessageSentTime_ = ellapsedTime_;

                baos = new ByteArrayOutputStream();
                baos.write(intToByteArray(1));	// len = 1
                baos.write(MESSAGE_ID_CHOKE);	// id = 0
                baos.flush();

                outputStream_.write(baos.toByteArray());
                outputStream_.flush();
            } else {
                close("ERROR, while send choke, outputstream is NULL");
            }
        } catch (IOException e) {
            close(ERROR_INCREASE_ERROR_COUNTER, "Error while writing choke");
        } catch (Exception e) {
            close(ERROR_INCREASE_ERROR_COUNTER, "Error while writing choke");
        } finally {
        	try {
				if (baos != null) baos.close();
			} catch (IOException e) {}
        }
    }

	/** Sending message: unchoke. */
    private void sendUnchokeMessage() {
    	ByteArrayOutputStream baos = null;
        try {
            if (outputStream_ != null) {
                lastMessageSentTime_ = ellapsedTime_;

                baos = new ByteArrayOutputStream();
                baos.write(intToByteArray(1));	// len = 1
                baos.write(MESSAGE_ID_UNCHOKE);	// id = 1
                baos.flush();

                outputStream_.write(baos.toByteArray());
                outputStream_.flush();
            } else {
                close("ERROR, while send unchoke, outputstream is NULL");
            }
        } catch (IOException e) {
            close(ERROR_INCREASE_ERROR_COUNTER, "Error while writing unchoke");
        } catch (Exception e) {
            close(ERROR_INCREASE_ERROR_COUNTER, "Error while writing unchoke");
        } finally {
        	try {
				if (baos != null) baos.close();
			} catch (IOException e) {}
        }
    }

    /** Sending message: interested. */
	private void sendInterestedMessage() {
		ByteArrayOutputStream baos = null;
		try {
			if (outputStream_ != null) {
				Log.v(LOG_TAG, "Sending interested to: " + peer_.getAddress());
				
				lastMessageSentTime_ = ellapsedTime_;

				baos = new ByteArrayOutputStream();
				baos.write(intToByteArray(1));		// len = 1
				baos.write(MESSAGE_ID_INTERESTED);	// id = 2
				baos.flush();

				outputStream_.write(baos.toByteArray());
				outputStream_.flush();
				
				Log.v(LOG_TAG, "Interested sent to: " + peer_.getAddress());
			} else {
				close("ERROR, while send interested, outputstream is NULL");
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			close(ERROR_INCREASE_ERROR_COUNTER, "Error while writing interested: " + ex.getMessage());
		} catch (Exception ex) {
			ex.printStackTrace();
			close(ERROR_INCREASE_ERROR_COUNTER, "Error while writing interested: " + ex.getMessage());
		} finally {
        	try {
				if (baos != null) baos.close();
			} catch (IOException e) {}
        }
	}

	/** Sending message: not interested. */
	private void sendNotInterestedMessage() {
		ByteArrayOutputStream baos = null;
		try {
			if (outputStream_ != null) {
				lastMessageSentTime_ = ellapsedTime_;

				baos = new ByteArrayOutputStream();
				baos.write(intToByteArray(1));			// len = 1
				baos.write(MESSAGE_ID_NOT_INTERESTED);	// id = 3
				baos.flush();

				outputStream_.write(baos.toByteArray());
				outputStream_.flush();
			} else {
				close("ERROR, while send notinterested, outputstream is NULL");
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			close(ERROR_INCREASE_ERROR_COUNTER, "Error while writing notinterested");
		} catch (Exception ex) {
			ex.printStackTrace();
			close(ERROR_INCREASE_ERROR_COUNTER, "Error while writing notinterested");
		} finally {
        	try {
				if (baos != null) baos.close();
			} catch (IOException e) {}
        }
	}
	
	/** Sending message: have. */
	public void sendHaveMessage(int pieceIndex) {
		ByteArrayOutputStream baos = null;
		try {
			if (outputStream_ != null) {
				lastMessageSentTime_ = ellapsedTime_;

				baos = new ByteArrayOutputStream();
				baos.write(intToByteArray(5));			// len = 5
				baos.write(MESSAGE_ID_HAVE);			// id = 4
				baos.write(intToByteArray(pieceIndex));	// piece index
				baos.flush();

				outputStream_.write(baos.toByteArray());
				outputStream_.flush();
			} else {
				close("ERROR, while send have, outputstream is NULL");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
        	try {
				if (baos != null) baos.close();
			} catch (IOException e) {}
        }
	}
	
	/** Sending message: bitfield. */
	private void sendBitfieldMessage() {
	    byte[] bitfield = torrent_.getBitfield().data();
	    ByteArrayOutputStream baos = null;
	    try {
	        if (outputStream_ != null) {
	        	Log.v(LOG_TAG, "Sending bitfield to: " + peer_.getAddress());
	            lastMessageSentTime_ = ellapsedTime_;
	
	            baos = new ByteArrayOutputStream();
	            baos.write(intToByteArray(1 + bitfield.length));	// len = 1 + X
	            baos.write(MESSAGE_ID_BITFIELD);					// id = 5
	            baos.write(bitfield);								// bitfield
	            baos.flush();
	
	            outputStream_.write(baos.toByteArray());
	            outputStream_.flush();                
	        } else {
	            close("ERROR, while send bitfield, outputstream is NULL");
	        }
	    } catch (IOException ex) {
	        ex.printStackTrace();
	        close(ERROR_INCREASE_ERROR_COUNTER, "Error while writing bitfield");
	    } catch (Exception ex) {
	        ex.printStackTrace();
	        close(ERROR_INCREASE_ERROR_COUNTER, "Error while writing bitfield");
	    } finally {
	    	try {
				if (baos != null) baos.close();
			} catch (IOException e) {}
	    }
	}
	
	/** Sending message: request. */
	private void sendRequestMessage(Block block) {
		if (block != null) {
			ByteArrayOutputStream baos = null;
			try {
				if (outputStream_ != null) {
					lastMessageSentTime_ = lastRequestTime_ = ellapsedTime_;

					baos = new ByteArrayOutputStream();
					baos.write(intToByteArray(13));							// len = 13
					baos.write(MESSAGE_ID_REQUEST);							// id = 6
					baos.write(intToByteArray(block.pieceIndex()));			// index: zero-based piece index 
					baos.write(intToByteArray(block.begin()));				// begin: zero-based byte offset within the piece 
					baos.write(intToByteArray(block.length()));				// length: requested length
					baos.flush();

					outputStream_.write(baos.toByteArray());
					outputStream_.flush();

					Log.v(LOG_TAG, "REQUEST sent: " + block.pieceIndex() + "from: " + block.begin() + " (block length: " + block.length() + ")");
				} else {
					close("ERROR, while send request, outputstream is NULL");
				}
			} catch (IOException e) {
				close(ERROR_INCREASE_ERROR_COUNTER, "Error while writing request");
			} catch (Exception e) {
				close(ERROR_INCREASE_ERROR_COUNTER, "Error while writing request");
			} finally {
	        	try {
					if (baos != null) baos.close();
				} catch (IOException e) {}
	        }
		}
	}
	
	/** Sending message: piece. */
	private void sendPieceMessage(int pieceIndex, int begin, int length) {
		Piece piece = torrent_.getPiece(pieceIndex);
		
		if (piece != null) {
			//Log.v(LOG_TAG, "Processing piece request " + pieceIndex + " Begin: " + begin + " Length: " + length + " while piece totalsize: " + piece.size());

			if (begin + length > piece.size()) {
				close("Bad PIECE request (index is out of bounds)");
				return;
			}

			byte[] block = piece.getBlock(begin, length);
			if (block == null) {
				close("Failed to extract block of piece");
				return;
			}
			
			ByteArrayOutputStream baos = null;
			try {
				if (outputStream_ != null) {
					baos = new ByteArrayOutputStream();
					baos.write(intToByteArray(9 + length));	// len = 9 + X
					baos.write(MESSAGE_ID_PIECE);			// id = 7
					baos.write(intToByteArray(pieceIndex));	// index
					baos.write(intToByteArray(begin));		// begin
					baos.write(block);						// block
					baos.flush();

					outputStream_.write(baos.toByteArray());
					outputStream_.flush();

					//torrent_.updateBytesUploaded(length, true);

					lastMessageSentTime_ = ellapsedTime_;
					//Log.v(LOG_TAG, "out PIECE Index: " + pieceIndex + " Begin: " + begin + " Length: " + length);
				} else {
					close("ERROR, while send piece, outputstream is NULL");
				}
			} catch (IOException ex) {
				close(ERROR_INCREASE_ERROR_COUNTER, "Error while writing piece " + ex.getMessage());
			} catch (Exception ex) {
				close(ERROR_INCREASE_ERROR_COUNTER, "Error while writing piece(e) " + ex.getMessage());
			} finally {
	        	try {
					if (baos != null) baos.close();
				} catch (IOException e) {}
	        }
		} else close("Bad PIECE index");
	}

	/** Sending message: cancel. */
	private void sendCancelMessage(Block block) {
		lastMessageSentTime_ = ellapsedTime_;
		ByteArrayOutputStream baos = null;
		try {
			baos = new ByteArrayOutputStream();
			baos.write(intToByteArray(13));							// len = 13
			baos.write(MESSAGE_ID_CANCEL);							// id = cancel
			baos.write(intToByteArray(block.pieceIndex()));			// index
			baos.write(intToByteArray(block.begin()));				// begin
			baos.write(intToByteArray(block.length()));				// length
			baos.flush();

			outputStream_.write(baos.toByteArray());
			outputStream_.flush();

			//Log.v(LOG_TAG, "out CANCEL[" + piece.piece.index() + "] (block length: " + piece.lastRequestTime + ")");
		} catch (IOException e) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Error while writing cancel message");
		} catch (Exception e) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Error while writing cancel message");
		} finally {
        	try {
				if (baos != null) baos.close();
			} catch (IOException e) {}
        }
	}
	
	/** Issue the download. */
	public void issueDownload() {
		
		int numDownload = 0;
		synchronized (blocksToDownload_) {
			while (blocksToDownload_.size() < MAX_PIECE_REQUESTS) {
				Block blockToDownload = torrent_.getBlockToDownload(peer_);
				if (blockToDownload != null) {
					blocksToDownload_.addElement(blockToDownload);
					Log.v(LOG_TAG, "BLOCK TO DOWNLOAD: " + blockToDownload.pieceIndex() + " - " + blockToDownload.begin());
					// /iLastRequestTime = iEllapsedTime;
				} else
					break;
			}
			numDownload = blocksToDownload_.size();
			//Log.v(LOG_TAG, blocksToDownload_.size() + " issue download from " + peer_.getAddress());
		}

		if (numDownload == 0) {
			//setInterested(false);
/*
			if (ellapsedTime_ > 15) {
				if (!isPeerInterested()) {
					//torrentMgr.notifyTorrentObserverMain(torrent_, MTTorrentObserver.EMTMainEventTorrentUploadEnded);

					int closeOrder = ENotSpecified;
					if (torrent_.isComplete())
						closeOrder = EDeletePeer;

					close("No needed piecese and peer not interested");
				} else if (incomingRequests.size() == 0) // wait for possible incoming interested message
				{
					close("No pieces need and peer is not interested");
				}
			}*/
		} else {
			setInterested(true);

			if (!isPeerChoking()) {
				synchronized (blocksToDownload_) {
					for (int i = 0; i < blocksToDownload_.size(); i++) {
						if (!blocksToDownload_.elementAt(i).isRequested()) {
							sendRequestMessage(blocksToDownload_.elementAt(i));
							blocksToDownload_.elementAt(i).setRequested();
							Log.v(LOG_TAG, "request sent to " + peer_.getAddress());
						}
					}
				}
			}
		}
	}
	
	/** Closes the socket connection. */
	public void close(String reason) {
        close(ERROR_NOT_SPECIFIED, reason);
    }

    /** Closes the socket connection. */
	public void close(int order, String reason) {
		if (state_ != STATE_CLOSING && state_ != STATE_NOT_CONNECTED) {
			Log.v(LOG_TAG, "Closing connection. Reason: " + reason);
			changeState(STATE_CLOSING);
			
			isReadEnabled_ = false;		// Stop reading the socket

			// Notify the torrent about the disconnection of the peer
			if (torrent_ != null) {
				// torrent_.peerDisconnected(peer, peerWireConnected);
			}
			
			// Stop thread and close streams and socket
			try {
				if (connectThread_ != null && connectThread_.isAlive()) connectThread_.interrupt();
			} catch (Exception e) {}
			try {
				if (inputStream_ != null) inputStream_.close();
			} catch (Exception e) {}
			try {
				if (outputStream_ != null) outputStream_.close();
			} catch (Exception e) {}
			try {
				if (socket_ != null) socket_.close();
			} catch (Exception e) {}

			connectThread_ = null;
			inputStream_ = null;
			outputStream_ = null;
			socket_ = null;

			changeState(STATE_NOT_CONNECTED);
			
			// Cancel the downloading blocks
			if (blocksToDownload_ != null && !blocksToDownload_.isEmpty()) {
				for (int i = 0; i < blocksToDownload_.size(); i++) {
					torrent_.cancelBlock(blocksToDownload_.elementAt(i));
				}
				blocksToDownload_.removeAllElements();
			}
			torrent_.peerDisconnected(peer_);
		}
	}
    
    /** Converts an integer value to byte array (four byte big-endian value). */
    public byte[] intToByteArray(int value) {
        return new byte[] {
	        (byte)(value >>> 24),
	        (byte)(value >>> 16),
	        (byte)(value >>> 8),
	        (byte) value
        };
	}
    
    /** Converts a byte array (four byte big-endian value) to an integer value. */
    public int byteArrayToInt(byte [] array) {
        return (array[0] << 24) +
              ((array[1] & 0xFF) << 16) +
              ((array[2] & 0xFF) << 8) +
               (array[3] & 0xFF);
    }

    /** Sets our chocking state. */
	private void setChoking(boolean choking) {
		if (choking) {
			if (!isChoking()) {
				isChoking_ = true;
				if (state_ == STATE_PW_CONNECTED) {
					sendChokeMessage();
				}
			}
		} else if (isChoking()) {
			isChoking_ = false;
			if (state_ == STATE_PW_CONNECTED) {
				sendUnchokeMessage();
			}
		}
	}

	/** Sets the chocking state of the peer. */
	private void setPeerChoking(boolean choking) {
		isPeerChoking_ = choking;
	}

	/** Sets our interest. */
	private void setInterested(boolean interested) {
		if (interested) {
			if (!isInterested()) {
				isInterested_ = true;
				if (state_ == STATE_PW_CONNECTED) {
					sendInterestedMessage();
				}
			}
		} else if (isInterested()) {
			isInterested_ = false;
			if (state_ == STATE_PW_CONNECTED) {
				sendNotInterestedMessage();

				if (!isPeerInterested()) {
					close("Nobody interested");
				}
			}
		}
	}

	/** Sets the interest of the peer. */
	private void setPeerInterested(boolean interested) {
		if (interested) {
			isPeerInterested_ = true;
		}
		else {
			isPeerInterested_ = false;
			if ((state_ == STATE_PW_CONNECTED) && (!isInterested())) {
				close("Nobody interested");
			}
		}
	}
	
	/** Changes the state of the connection. */
	public void changeState(int state) {
		state_ = state;
		ellapsedTime_ = 0;
	}
	
	/** Returns the connections state. */
	public int getState() {
		return state_;
	}
	
	/** Returns wheter we are chocking the peer or not. */
	private boolean isChoking() {
		return isChoking_;
	}

	/** Returns whether the peer is chocking us or not. */
	private boolean isPeerChoking() {
		return isPeerChoking_;
	}
	
	/** Returns whether we find the peer interesting or not. */
	private boolean isInterested() {
		return isInterested_;
	}

	/** Returns whether the peer is interested in us or not. */
	private boolean isPeerInterested() {
		return isPeerInterested_;
	}
    
	/** Thread that connects to the peer. */
	private class ConnectThread extends Thread {
		private String destination_;
		
		public ConnectThread(String destination) {
			destination_ = destination;
		}
		
		@Override
		public void run() {
			try {
				Log.v(LOG_TAG, "Connecting to: " + destination_);
				socket_ = new Socket(peer_.getAddress(), peer_.getPort());
				Log.v(LOG_TAG, "Connected to: " + destination_);
			} catch (IOException e) {
				if (socket_ != null) {
					try {
						socket_.close();
					} catch (IOException e1) {}
					socket_ = null;
				}
				Log.v(LOG_TAG, "Failed to connect to: " + destination_);
			}
			
			if (socket_ != null) startDownloading();
		}
	}
	
	/** Opens the streams, sends a handshake and starts reading the incoming messages. */
	private void startDownloading() {
		Log.v(LOG_TAG, "Opening streams on: " + peer_.getAddress());

        try {
            inputStream_ = socket_.getInputStream();
            outputStream_ = socket_.getOutputStream();
        } catch(IOException ex) {
            close(ERROR_INCREASE_ERROR_COUNTER, "Opening streams failed - " + ex.getMessage());
            Log.v(LOG_TAG, "Opening streams failed on: " + peer_.getAddress() + " | " + ex.getMessage());
            return;
        }

        sendHandshakeMessage();

        ellapsedTime_ = 0;
        
        read();
	}
	
}
