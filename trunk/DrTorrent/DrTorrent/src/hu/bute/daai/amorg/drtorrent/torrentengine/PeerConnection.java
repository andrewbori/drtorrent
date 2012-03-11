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
    private final static int DEFALT_BLOCK_LENGTH    = 16384;	// default download block size (2^14)
    private final static int MAX_PIECE_REQUESTS     = 1;
	
	public final static int STATE_NOT_CONNECTED  = 0;
    public final static int STATE_TCP_CONNECTING = 1;
    public final static int STATE_TCP_CONNECTED  = 2;
    public final static int STATE_PW_HANDSHAKING = 3;
    public final static int STATE_PW_CONNECTED   = 4;
    public final static int STATE_CLOSING        = 5;
    
    public final static int EDeletePeer = 0;
    public final static int EIncreaseErrorCounter = 1;
    public final static int ENotSpecified = 2;
	
	private Peer peer_;
	private Torrent torrent_;
	private TorrentManager torrentManager_;
	private Vector<PieceToDownload> piecesToDownload_;
	
	private boolean incomingConnection_;
	private boolean readEnabled_;
	private int state_;
	
	private boolean isInterested_;			// I am interested in the peer
	private boolean isPeerInterested_;		// The peer finds me interesting
	private boolean isChoking_;				// I am choking the peer
	private boolean isPeerChoking_;			// The peer choked me
	
	private ConnectThread connectThread_;
	private Socket socket_;
	private InputStream inputStream_;
	private OutputStream outputStream_;
	private boolean peerWireConnected_;
	
	private int lastRequestTime_;
	private int lastMessageReceivedTime_;
	private int lastMessageSentTime_;
	private int ellapsedTime_;
	private int reconnectAfter_;
	public static int tcpConnectionTimeoutNum_ = 0;
	private boolean hasPendingDownloadRequest_;
	
	/** Creates a new instance of PeerConnection. */
	public PeerConnection(Peer peer, Torrent torrent, TorrentManager torrentManager) {
        peer_ = peer;
        torrent_ = torrent;
        torrentManager_ = torrentManager;
        incomingConnection_ = false;

        isInterested_ = false;
        isPeerInterested_ = false;
        isChoking_ = true;
        isPeerChoking_ = true;

        piecesToDownload_ = new Vector<PieceToDownload>();
        //incomingRequests_ = new Vector<BlockRequest>();

        peerWireConnected_ = false;
        state_ = STATE_NOT_CONNECTED;
        readEnabled_ = true;
    }
	
	/** Class containing information about the piece to download. */
	private class PieceToDownload {
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
	}
	
	public void connect() {
		String destination = peer_.getAddress() + ":" + peer_.getPort();
		connectThread_ = new ConnectThread(destination);
		connectThread_.start();
	}
	
	public void startDownloading() {
		Log.v(LOG_TAG, "Opening streams on: " + peer_.getAddress());

        try {
            inputStream_ = socket_.getInputStream();
            outputStream_ = socket_.getOutputStream();
        } catch(IOException ex) {
            close(EIncreaseErrorCounter, "Opening streams failed - " + ex.getMessage());
            Log.v(LOG_TAG, "Opening streams failed on: " + peer_.getAddress() + " | " + ex.getMessage());
            return;
        }

        Log.v(LOG_TAG, "Start download from: " + peer_.getAddress());
        state_ = STATE_PW_HANDSHAKING;
        sendHandshakeMessage();

        ellapsedTime_ = 0;
        //System.out.println("STARTREAD: "+peer.getAddress());
        new Thread() {
            public void run() {
                read();
            }
        }.start();
	}

	public void onTimer() {
		ellapsedTime_++;
		if (reconnectAfter_ > 0) reconnectAfter_--;

		switch (state_) {
			case STATE_TCP_CONNECTING: {
				if (ellapsedTime_ > TIMEOUT_TCP_CONNECTION) {
					tcpConnectionTimeoutNum_++;
					close(EIncreaseErrorCounter, "Timeout while trying to connect");
				}
			}
				break;

			case STATE_PW_HANDSHAKING: {
				if (ellapsedTime_ > TIMEOUT_HANDSHAKE) {
					close(EIncreaseErrorCounter, "Handshake timeout (no data received)");
				}
			}
				break;

			case STATE_PW_CONNECTED: {
				if ((ellapsedTime_ - lastMessageReceivedTime_) > TIMEOUT_PW_CONNECTION) {
					close(EIncreaseErrorCounter, "General timeout (no data received)");
					break;
				}

				if (lastRequestTime_ > 0 && ((ellapsedTime_ - lastRequestTime_) > TIMEOUT_REQUEST)) {
					lastRequestTime_ = ellapsedTime_;
					/*
					 * if (torrent_.hasTimeoutlessPeer()) {
					 * peer_.setHadRequestTimeout(true);
					 * close(EIncreaseErrorCounter, "Request timeout"); break; }
					 */
				}

				if ((ellapsedTime_ > 10) && (!isInterested()) && (!isPeerInterested())) {
					// torrentMgr.notifyTorrentObserverMain(torrent_,
					// MTTorrentObserver.EMTMainEventTorrentUploadEnded);
					close("Nobody interested!");
				}

				if ((ellapsedTime_ - lastMessageSentTime_) >= KEEP_ALIVE_INTERVAL) sendKeepAliveMessage();

				if (piecesToDownload_.size() == 0) {
					// System.out.println("REREQUEST");
					issueDownload();
				}
			}
				break;

			default:
				;
		}
	}
	
	/** Reads at most data.length bytes from the inputStream_ and puts it to the data array. */
	private boolean readData(byte [] data) throws Exception {
        return readData(data, 0, data.length);
    }

	/** Reads at most len bytes from the inputStream_ and puts it to the data array starting from the offset position. */
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
			close(EIncreaseErrorCounter, "Read error");
			return -1;
		} catch (Exception ex) {
			close(EIncreaseErrorCounter, "Read error");
			return -1;
		}

		return byteArrayToInt(array);
	}
	
    /** Reads data from the inputStream_. */
	private void read() {
        int messageLength = 0;

        while (readEnabled_) {
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
							// TODO: we have to close connection,
							issueDownload();
						} else {
							int id = inputStream_.read();
							switch (id) {
								
								case MESSAGE_ID_CHOKE: Log.v(LOG_TAG, "Reading choke message from: " + peer_.getAddress());
									readChokeMessage();
									break;

								case MESSAGE_ID_UNCHOKE: Log.v(LOG_TAG, "Reading unchoke message from: " + peer_.getAddress());
									readUnchokeMessage();
									break;

								case MESSAGE_ID_INTERESTED: Log.v(LOG_TAG, "Reading interested message from: " + peer_.getAddress());
									readInterestedMessage();
									break;
								case MESSAGE_ID_NOT_INTERESTED: Log.v(LOG_TAG, "Reading not interested message from: " + peer_.getAddress());
									readNotInterestedMessage();
									break;
								
								case MESSAGE_ID_BITFIELD: Log.v(LOG_TAG, "Reading bitfield message from: " + peer_.getAddress());
									readBitfieldMessage(messageLength);
									break;

								case MESSAGE_ID_HAVE: Log.v(LOG_TAG, "Reading have message from: " + peer_.getAddress());
									readHaveMessage();
									break;

								case MESSAGE_ID_PIECE: Log.v(LOG_TAG, "Reading piece message from: " + peer_.getAddress());
									readPieceMessage(messageLength);
									break;

								case MESSAGE_ID_REQUEST: Log.v(LOG_TAG, "Reading request message from: " + peer_.getAddress());
									readRequestMessage(messageLength);
									break;

								case MESSAGE_ID_CANCEL: Log.v(LOG_TAG, "Reading cancel message from: " + peer_.getAddress());
									readCancelMessage(messageLength);
									break;
							}
						}
						break;
					}
                }
            } catch (InterruptedIOException e) {
                close(EIncreaseErrorCounter, "Read error");
                Log.v(LOG_TAG, "--- PeerConnection interrupted exception: " + e.getMessage());
            } catch (IOException e) {
                close(EIncreaseErrorCounter, "Read error");
                Log.v(LOG_TAG, "--- PeerConnection ioexception: " + e.getMessage());
            } catch (Exception e) {
                close(EIncreaseErrorCounter, "Read error (???)");
                Log.v(LOG_TAG, "[Read Exception] " + e.getMessage());
            }
        }
    }
	
	/** Reading message: handshake. */
	private void readHandshakeMessage() throws InterruptedIOException, IOException, Exception {
		int protLength = inputStream_.read(); 	// pstrlen
		if (protLength == -1) {
			close(EIncreaseErrorCounter, "Peer disconnected!");
			return;
		}

		int handshakeLength = protLength + 48; // 49-1 because protLength has
												// been read
		byte[] handshake = new byte[handshakeLength];
		readData(handshake);

		byte[] protocolId = new byte[protLength]; // pstr
		System.arraycopy(handshake, 0, protocolId, 0, protLength);

		if (!MESSAGE_PROTOCOL_ID.equals(new String(protocolId))) {
			close(EDeletePeer, "Protocol identifier doesn't match!");
			return;
		}

		byte[] infoHash = new byte[20]; // info_hash
		System.arraycopy(handshake, 27, infoHash, 0, 20);

		if (torrent_ != null) {
			if (!DrTorrentTools.byteArrayEqual(torrent_.getInfoHashByteArray(), infoHash)) {
				close(EDeletePeer, "Torrent infohash doesn't match!");
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

		if (incomingConnection_) {
			/*
			 * torrentManager_.notifyTorrentObserver(torrent_,
			 * MTTorrentObserver.EMTEventIncomingConnectionsChanged);
			 * torrent_.incIncomingConnectionsNum(); sendHandshakeMessage();
			 * peer_.resetAddress();
			 */
		}

		byte[] peerId = new byte[20]; // peer_id
		System.arraycopy(handshake, 47, peerId, 0, 20);
		String tempPeerId = new String(peerId);
		if (peer_.getPeerId() != null) {
			if (!peer_.getPeerId().equals(tempPeerId)) {
				close(EIncreaseErrorCounter, "Peer ID doesn't match!");
				return;
			} else if (tempPeerId.equals(torrentManager_.getPeerID())) {
				close(EDeletePeer, "Connected to ourselves!");
				return;
			}
		} else {
			// peer_.setPeerId(tempPeerId);
		}

		Log.v(LOG_TAG, "Handshake completed! Peer wire connected!");
		state_ = STATE_TCP_CONNECTED; // changeState(EPeerPwConnected);
		// setPeerWireConnected();

		if (!torrent_.getBitfield().isNull()) sendBitfieldMessage();
		
		issueDownload();
	}
	
	/** Reading message: choke. */
	private void readChokeMessage() {
		peer_.resetErrorCounter();
		setPeerChoking(true);

		synchronized (piecesToDownload_) {
			for (int i = 0; i < piecesToDownload_.size(); i++)
				piecesToDownload_.elementAt(i).hasPendingRequest = false;

			for (int i = 0; i < piecesToDownload_.size(); i++)
				//torrent_.removePieceFromDownloading(piecesToDownload_.elementAt(i).piece);

			piecesToDownload_.removeAllElements();
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
		if (messageLength - 1 != peer_.getBitfield().lengthInBytes()) {
			close(EIncreaseErrorCounter, "Received bitfield length doesn't match!");
		} else {
			int bitFieldLength = messageLength - 1; // -id
													// length
			byte[] bitFieldDes = new byte[bitFieldLength];
			boolean dataReaded = readData(bitFieldDes);

			if (dataReaded) {
				peer_.havePieces(bitFieldDes, torrent_);
				issueDownload();
			} else {
				close(EIncreaseErrorCounter, "Could not read bitfield!");
			}
		}
		
	}

	/** Reading message: have. */
	private void readHaveMessage() {
		int pieceIndex = readInt();

		if ((pieceIndex >= 0) && (pieceIndex < torrent_.pieceCount())) {
			peer_.havePiece(pieceIndex, torrent_);

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

		PieceToDownload piece = null;
		synchronized (piecesToDownload_) {
			for (int i = 0; i < piecesToDownload_.size(); i++)
				if (((PieceToDownload) piecesToDownload_.elementAt(i)).piece.index() == index) {
					piece = (PieceToDownload) piecesToDownload_.elementAt(i);
					break;
				}
		}

		if (piece == null) {
			close("Error, unexpected piece (there are no pending request for the received piece index)");
		} else {
			byte[] pieceBlock = new byte[pieceBlockSize];	// block
			boolean successfullRead = readData(pieceBlock);

			if (!successfullRead) {
				close(EIncreaseErrorCounter, "Reading piece failed!");
				return;
			}

			piece.hasPendingRequest = false;

			/*
			 * if(torrent.isEndGame())
			 * torrent.endGamePieceReceived
			 * (piece.piece, peer);
			 */

			/*if (piece.piece.appendBlock(pieceBlock, begin, peer_) != Torrent.ERROR_NONE) {
				pieceBlock = null;
				close("Writing to piece failed"); // CRITICAL FAULT
			} else {
				pieceBlock = null;
				// / WHY??? (piece->Remaining() ==
				// piece->TotalSize())
				if ((piece.piece.remaining() == 0) || (piece.piece.remaining() == piece.piece.getTotalSize())) {
					synchronized (piecesToDownload_) {
						piecesToDownload_.removeElement(piece);
					}
				}

			}*/
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
        if (torrent_ != null) {
            Log.v(LOG_TAG, "Sending handshake to: " + peer_.getAddress());
            ByteArrayOutputStream baos = null;
            try {
                if (outputStream_ != null) {
                    byte[] zeros = {0, 0, 0, 0, 0, 0, 0, 0};

                    baos = new ByteArrayOutputStream();
                    baos.write((byte) MESSAGE_PROTOCOL_ID.length());				// pstrlen:   string length of <pstr>, as a single raw byte 
                    baos.write(MESSAGE_PROTOCOL_ID.getBytes());						// pstr:      string identifier of the protocol 
                    baos.write(zeros);										// reserved:  eight (8) reserved bytes. All current implementations use all zeroes.
                    baos.write(torrent_.getInfoHashByteArray());			// info_hash: 20-byte SHA1 hash of the info key in the metainfo file.
                    baos.write(torrentManager_.getPeerID().getBytes());		// peer_id:   20-byte string used as a unique ID for the client.
                    outputStream_.write(baos.toByteArray());
                    outputStream_.flush();

                    Log.v(LOG_TAG, "Handshake sent to: " + peer_.getAddress());
                } else {
                    close("Error while sending handshake, outputstream is NULL");
                }
            } catch (IOException ex) {
            	Log.v(LOG_TAG, "Error while sending handshake");
                close(EIncreaseErrorCounter, "Error while writing");
            } catch (Exception ex) {
            	Log.v(LOG_TAG, "Error while sending handshake");
                close(EIncreaseErrorCounter, "Error while writing");
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
            close(EIncreaseErrorCounter, "Error while writing keepalive");
        } catch (Exception e) {
            close(EIncreaseErrorCounter, "Error while writing keepalive");
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
            close(EIncreaseErrorCounter, "Error while writing choke");
        } catch (Exception e) {
            close(EIncreaseErrorCounter, "Error while writing choke");
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
            close(EIncreaseErrorCounter, "Error while writing unchoke");
        } catch (Exception e) {
            close(EIncreaseErrorCounter, "Error while writing unchoke");
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
			close(EIncreaseErrorCounter, "Error while writing interested: " + ex.getMessage());
		} catch (Exception ex) {
			ex.printStackTrace();
			close(EIncreaseErrorCounter, "Error while writing interested: " + ex.getMessage());
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
			close(EIncreaseErrorCounter, "Error while writing notinterested");
		} catch (Exception ex) {
			ex.printStackTrace();
			close(EIncreaseErrorCounter, "Error while writing notinterested");
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
	        close(EIncreaseErrorCounter, "Error while writing bitfield");
	    } catch (Exception ex) {
	        ex.printStackTrace();
	        close(EIncreaseErrorCounter, "Error while writing bitfield");
	    } finally {
	    	try {
				if (baos != null) baos.close();
			} catch (IOException e) {}
	    }
	}
	
	/** Sending message: request. */
	private void sendRequestMessage(PieceToDownload piece) {
		if (piece != null) {
			ByteArrayOutputStream baos = null;
			try {
				if (outputStream_ != null) {
					lastMessageSentTime_ = ellapsedTime_;

					baos = new ByteArrayOutputStream();
					baos.write(intToByteArray(13));												// len = 13
					baos.write(MESSAGE_ID_REQUEST);												// id = 6
					baos.write(intToByteArray(piece.piece.index()));							// index: zero-based piece index 
					baos.write(intToByteArray(piece.piece.getDownloadedSize()));				// begin: zero-based byte offset within the piece 
					int blockLength = piece.piece.getLength() - piece.piece.getDownloadedSize();
					//if (blockLength > DEFALT_BLOCK_LENGTH)
						blockLength = DEFALT_BLOCK_LENGTH; 	// 16 KB
					baos.write(intToByteArray(blockLength));									// length: requested length
					baos.flush();

					outputStream_.write(baos.toByteArray());
					outputStream_.flush();

					piece.lastRequestLength = blockLength;
					piece.lastRequestBegin = piece.piece.getDownloadedSize();

					Log.v(LOG_TAG, "REQUEST sent: " + piece.piece.index() + "from: " + piece.piece.getDownloadedSize() + " (block length: " + blockLength + ")");
				} else {
					close("ERROR, while send request, outputstream is NULL");
				}
			} catch (IOException e) {
				close(EIncreaseErrorCounter, "Error while writing request");
			} catch (Exception e) {
				close(EIncreaseErrorCounter, "Error while writing request");
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
			//Log.v(LOG_TAG, "Processing piece request " + pieceIndex + " Begin: " + begin + " Length: " + length + " while piece totalsize: " + piece.getTotalSize());

			if (begin + length > piece.getLength()) {
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
				ex.printStackTrace();
				close(EIncreaseErrorCounter, "Error while writing piece " + ex.getMessage());
			} catch (Exception ex) {
				ex.printStackTrace();
				close(EIncreaseErrorCounter, "Error while writing piece(e) " + ex.getMessage());
			} finally {
	        	try {
					if (baos != null) baos.close();
				} catch (IOException e) {}
	        }
		} else close("Bad PIECE index");
	}

	/** Sending message: cancel. */
	private void sendCancelMessage(PieceToDownload piece) {
		lastMessageSentTime_ = ellapsedTime_;
		ByteArrayOutputStream baos = null;
		try {
			baos = new ByteArrayOutputStream();
			baos.write(intToByteArray(13));							// len = 13
			baos.write(MESSAGE_ID_CANCEL);							// id = cancel
			baos.write(intToByteArray(piece.piece.index()));		// index
			baos.write(intToByteArray(piece.lastRequestBegin));		// begin
			baos.write(intToByteArray(piece.lastRequestLength));	// length
			baos.flush();

			outputStream_.write(baos.toByteArray());
			outputStream_.flush();

			//Log.v(LOG_TAG, "out CANCEL[" + piece.piece.index() + "] (block length: " + piece.lastRequestTime + ")");
		} catch (IOException e) {
			close(EIncreaseErrorCounter, "Error while writing cancel message");
		} catch (Exception e) {
			close(EIncreaseErrorCounter, "Error while writing cancel message");
		} finally {
        	try {
				if (baos != null) baos.close();
			} catch (IOException e) {}
        }
	}
	
	/** issue the download. */
	public void issueDownload() {
		Log.v(LOG_TAG, "issue download from " + peer_.getAddress());
		
		int numDownload = 0;
		synchronized (piecesToDownload_) {
			while (piecesToDownload_.size() < MAX_PIECE_REQUESTS) {
				Piece pieceToDownload = torrent_.getPieceToDownload(peer_);
				if (pieceToDownload != null) {
					piecesToDownload_.addElement(new PieceToDownload(pieceToDownload, ellapsedTime_));
					// iLastRequestTime = iEllapsedTime;
				} else
					break;
			}
			numDownload = piecesToDownload_.size();
		}

		if (numDownload == 0) {
			setInterested(false);
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
				synchronized (piecesToDownload_) {
					for (int i = 0; i < piecesToDownload_.size(); i++) {
						if (!((PieceToDownload) piecesToDownload_.elementAt(i)).hasPendingRequest) {
							sendRequestMessage(piecesToDownload_.elementAt(i));
							Log.v(LOG_TAG, "request sent to " + peer_.getAddress());
							piecesToDownload_.elementAt(i).hasPendingRequest = true;
						}
					}
				}
			}
		}
	}
	
	/** Closes the socket connection. */
	public void close(String reason) {
        close(ENotSpecified, reason);
    }

    /** Closes the socket connection. */
    public void close(int order, String reason) {
        Log.v(LOG_TAG, "Closing connection. Reason: " + reason);
        if(state_ != STATE_CLOSING) {
            //closeOrder = order;

            // stop receiving
            readEnabled_ = false;

            state_ = STATE_CLOSING;
            //changeState(EPeerClosing);
            if(torrent_ != null) {
                // torrent_.peerDisconnected(peer, peerWireConnected);
            }
            // peerWireConnected = false;

            if(connectThread_ != null) {
                if(connectThread_.isAlive()) {
                    connectThread_.interrupt();
                }
                connectThread_ = null;
            }

            try {
                if (inputStream_ != null) {
                    inputStream_.close();
                    inputStream_ = null;
                }
                if (outputStream_ != null) {
                    outputStream_.close();
                    outputStream_ = null;
                }
                if (socket_ != null) {
                    socket_.close();
                    socket_ = null;
                }
            } catch(Exception ex) {
                Log.v(LOG_TAG, "Exception while closing: " + ex.getMessage());
            }
            inputStream_ = null;
            outputStream_ = null;
            socket_ = null;
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

	private void setPeerChoking(boolean choking) {
		isPeerChoking_ = choking;
	}

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
	
	public void changeState(int state) {
		state_ = state;
		ellapsedTime_ = 0;
	}
	
	private boolean isChoking() {
		return isChoking_;
	}

	private boolean isPeerChoking() {
		return isPeerChoking_;
	}
	
	private boolean isInterested() {
		return isInterested_;
	}

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
	
}
