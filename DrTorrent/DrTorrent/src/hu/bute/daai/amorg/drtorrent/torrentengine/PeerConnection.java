package hu.bute.daai.amorg.drtorrent.torrentengine;

import hu.bute.daai.amorg.drtorrent.Tools;
import hu.bute.daai.amorg.drtorrent.coding.sha1.SHA1;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Vector;

import android.os.SystemClock;
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
	
	private final static int TIMEOUT_TCP_CONNECTION = 1000 * 30;
	private final static int TIMEOUT_HANDSHAKE      = 1000 * 15;
	private final static int TIMEOUT_PW_CONNECTION  = 1000 * 2 * 65;
	private final static int TIMEOUT_REQUEST        = 1000 * 60;
    private final static int KEEP_ALIVE_INTERVAL    = 1000 * 2 * 55;	// once in every two minutes
    //private final static int MAX_PIECE_REQUESTS     = 5;
    //private final static int TIMEOUT_BLOCK = 10 * 1000;
    
	public final static int STATE_NOT_CONNECTED  = 0;
    public final static int STATE_TCP_CONNECTING = 1;
    public final static int STATE_TCP_CONNECTED  = 2;
    public final static int STATE_PW_HANDSHAKING = 3;
    public final static int STATE_PW_CONNECTED   = 4;
    public final static int STATE_CLOSING        = 5;
    
    public final static int ERROR_DELETE_PEER = 0;
    public final static int ERROR_INCREASE_ERROR_COUNTER = 1;
    public final static int ERROR_NOT_SPECIFIED = 2;
	
	private Peer peer_;							// the peer
	private Torrent torrent_;					// the torrent
	
	private Vector<Block> blocksToUpload_;
	
	private boolean isIncomingConnection_;
	private boolean isReadEnabled_;
	private int state_;
	
	private boolean isInterested_;			// I am interested in the peer
	private boolean isPeerInterested_;		// The peer finds me interesting
	private boolean isChoking_;				// I am choking the peer
	private boolean isPeerChoking_;			// The peer is choking me
	
	private ConnectThread connectThread_;
	private Socket socket_;
	private DataInputStream inputStream_;
	private DataOutputStream outputStream_;
	
	private int elapsedTime_ = 0;
	private long lastTime_ = 0;
	private int latestRequestTime_ = 0;
	private int latestMessageReceivedTime_ = 0;
	private int latestMessageSentTime_ = 0;
	private int reconnectAfter_;
	public static int tcpConnectionTimeoutNum_ = 0;
	
	/** Creates a new instance of PeerConnection. */
	public PeerConnection(Peer peer, Torrent torrent, boolean isIncomingConnection) {
        peer_ = peer;
        torrent_ = torrent;
        isIncomingConnection_ = isIncomingConnection;

        isInterested_ = false;
        isPeerInterested_ = false;
        isChoking_ = true;
        isPeerChoking_ = true;
        
        blocksToUpload_ = new Vector<Block>();
        
        state_ = STATE_NOT_CONNECTED;
        isReadEnabled_ = true;
    }
	
	/** Connects to the peer (including the peer wire connection). */
	public void connect() {
		isIncomingConnection_ = false;
		
		String destination = peer_.getAddress() + ":" + peer_.getPort();
		connectThread_ = new ConnectThread(destination);
		connectThread_.start();
	}
	
	public void connect(Socket socket) {
		socket_ = socket;
		String destination = peer_.getAddress() + ":" + peer_.getPort();
		connectThread_ = new ConnectThread(destination);
		connectThread_.start();
	}

	/** Schedules the connection. Mainly for checking timeouts. */
	public void onTimer() {
		
		calculateElapsedTime();
		
		if (reconnectAfter_ > 0) reconnectAfter_--;

		switch (state_) {
			
			case STATE_TCP_CONNECTING:
				if (elapsedTime_ > TIMEOUT_TCP_CONNECTION) {
					tcpConnectionTimeoutNum_++;
					close(ERROR_INCREASE_ERROR_COUNTER, "Timeout while trying to connect.");
				}
				break;

			case STATE_PW_HANDSHAKING:
				if (elapsedTime_ > TIMEOUT_HANDSHAKE) {
					close(ERROR_INCREASE_ERROR_COUNTER, "Handshake timeout (no data received).");
				}
				break;

			case STATE_PW_CONNECTED:
				peer_.calculateSpeed(lastTime_);
				
				// Timeout: Because no messages have been received in the recent time
				if ((elapsedTime_ - latestMessageReceivedTime_) > TIMEOUT_PW_CONNECTION) {
					close(ERROR_INCREASE_ERROR_COUNTER, "General timeout (no data received)");
					break;
				}

				if (latestRequestTime_ > 0 && ((elapsedTime_ - latestRequestTime_) > TIMEOUT_REQUEST)) {
					latestRequestTime_ = 0;
					/*if (torrent_.hasTimeoutlessPeer()) {
					 peer_.setHadRequestTimeout(true);
					 close(EIncreaseErrorCounter, "Request timeout"); break; }
					 */
					close(ERROR_INCREASE_ERROR_COUNTER, "Request timeout");
					break;
					
				}
				
				// Nobody interested
				if (elapsedTime_ > 10000 && !isInterested() && !isPeerInterested()) {
					close("Nobody interested!");
				}
				
				// Keep alive: because we have not sent message in the recent time
				if ((elapsedTime_ - latestMessageSentTime_) >= KEEP_ALIVE_INTERVAL) {
					sendKeepAliveMessage();
				}
				
				if (peer_.getRequestsCount() == 0) {
					issueDownload();
				} else {
					/*try {
						if (blocksDownloading_.size() > 1) {
							Block first = blocksDownloading_.firstElement();
							if (first.getRequestDelay() > TIMEOUT_BLOCK) {
								Block last = blocksDownloading_.lastElement();
								cancelBlock(last);
								torrent_.cancelBlock(last);
							}
						}
					} catch(Exception e) {}*/
				}

				break;

			default:
				break;
		}
	}
	
	/** Issues the upload requests. */
	public void issueUpload() {
		if (!isChoking()) {
			while (blocksToUpload_.size() > 0) {
				Block block = (Block) blocksToUpload_.elementAt(0);
				sendPieceMessage(block);
				blocksToUpload_.removeElementAt(0);
			}
		}
	}
	
	/** Issues the download requests. */
	public void issueDownload(){
		if (!isPeerChoking()) {
			ArrayList<Block> blocksToDownload = peer_.issueDownload();
			if (blocksToDownload != null) {
				setInterested(true);
				sendRequestMessage(blocksToDownload);
			}
		}
	}
	
	/** Writes the byte array to the outputStream_. */
	private void write(byte buffer[]) throws Exception {
		try {
			outputStream_.write(buffer);
			outputStream_.flush();
		} catch(Exception e) {}
	}
	
	/** Writes a big-endian integer to the outputStream_.*/
	private void writeInt(int i) throws Exception {
		try {
			outputStream_.writeInt(i);
			outputStream_.flush();
		} catch(Exception e) {}
	}
	
	/** Reads data.length bytes from the inputStream_ and puts it into the data array. */
	private boolean readData(byte [] data) throws Exception {
        try {
        	inputStream_.readFully(data);
        } catch (Exception e) {
        	Log.v("AAAAAAA", "Exception was thrown while readFully...");
        	if (e.getMessage() != null) Log.v("AAAAAA", e.getMessage());
        	return false;
        }
        return true;
    }
	
	/** 
	 * Reads a single byte from the inputStream_ and returns it as an integer in the range from 0 to 255. <br>
	 * Returns -1 if the end of this stream has been reached.
	 */
	private int readByte() throws Exception {
		try {
			return inputStream_.read();
		} catch (Exception ex) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Read error " + ex.getMessage());
			return -1;
		}
	}
    
    /** Reads a big-endian integer from the inputStream_. */
	public int readInt() throws Exception {
		try {
			return inputStream_.readInt();
		} catch (Exception ex) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Read error " + ex.getMessage());
			return -1;
		}
	}
	
    /** Reads data from the inputStream_. */
	private void read() {
        int messageLength = 0;

        while (isReadEnabled_) {
            try {
                switch (state_) {
                	
                    case STATE_PW_HANDSHAKING: {
                    	Log.v(LOG_TAG, "Reading choke message from: " + peer_.getAddress());
                        readHandshakeMessage();
                        break;
                    }
                    
					case STATE_PW_CONNECTED: {

						messageLength = readInt();				// length prefix
						if (messageLength == -1) continue;
						
						calculateElapsedTime();
						latestMessageReceivedTime_ = elapsedTime_;
						if (messageLength == 0) {
							// keep-alive
							Log.v(LOG_TAG, "Reading keep alive message from: " + peer_.getAddress());
							// issueDownload();
						} else {
							int id = readByte();				// message ID
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
									
								default:
									Log.v(LOG_TAG, "UNKNOWN MESSAGE " + messageLength + " " + id + " from: " + peer_.getAddress());
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
		int protLength = readByte(); 					// pstrlen
		if (protLength == -1) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Peer disconnected!");
			return;
		}

		int handshakeLength = protLength + 48; 			// 49-1 because protLength has been read
		byte[] handshake = new byte[handshakeLength];
		readData(handshake);

		byte[] protocolId = new byte[protLength]; 		// pstr
		System.arraycopy(handshake, 0, protocolId, 0, protLength);

		if (!MESSAGE_PROTOCOL_ID.equals(new String(protocolId))) {
			close(ERROR_DELETE_PEER, "Protocol identifier doesn't match!");
			return;
		}

		byte[] infoHash = new byte[20]; // info_hash
		System.arraycopy(handshake, 27, infoHash, 0, 20);

		if (!isIncomingConnection_) {
			if (!Tools.byteArrayEqual(torrent_.getInfoHashByteArray(), infoHash)) {
				close(ERROR_DELETE_PEER, "Torrent infohash doesn't match!");
				return;
			}
		} else { // if torrent is null then we should attach peer to torrent (most likely it is an incoming connection)
			Log.v(LOG_TAG, "Attach");
			boolean result = peer_.attachTorrent(SHA1.resultToString(infoHash));
			if (result) sendHandshakeMessage();
			else close(ERROR_DELETE_PEER, "Invalid infohash or peer is already connected or too many peers!"); 
		}

		byte[] peerId = new byte[20]; // peer_id
		System.arraycopy(handshake, 47, peerId, 0, 20);
		String tempPeerId = new String(peerId);
		if (peer_.getPeerId() != null) {
			if (!peer_.getPeerId().equals(tempPeerId)) {
				close(ERROR_INCREASE_ERROR_COUNTER, "Peer ID doesn't match!");
				return;
			} else if (tempPeerId.equals(TorrentManager.getPeerID())) {
				close(ERROR_DELETE_PEER, "Connected to ourselves!");
				return;
			}
		} else {
			peer_.setPeerId(tempPeerId);
		}

		Log.v(LOG_TAG, "Handshake completed! Peer wire connected!");
		changeState(STATE_PW_CONNECTED);

		if (!torrent_.getBitfield().isNull()) sendBitfieldMessage();
	}
	
	/** Reading message: choke. */
	private void readChokeMessage() {
		peer_.resetErrorCounter();
		setPeerChoking(true);

		peer_.cancelBlocks();
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
		setPeerInterested(true);
	}

	/** Reading message: not interested. */
	private void readNotInterestedMessage() {
		peer_.resetErrorCounter();
		setPeerInterested(false);
		//issueDownload();
	}

	/** Reading message: bitfield. */
	private void readBitfieldMessage(int messageLength) throws InterruptedIOException, IOException, Exception {
		int bitFieldLength = messageLength - 1; // bitfield length
		if (bitFieldLength != peer_.getBitfield().getLengthInBytes()) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Received bitfield length doesn't match! " + bitFieldLength + " instead of " + peer_.getBitfield().getLengthInBytes());
		} else {
			byte[] bitfield = new byte[bitFieldLength];
			boolean dataReaded = readData(bitfield);
			if (dataReaded) {
				peer_.peerHasPieces(bitfield);
				Log.v(LOG_TAG, "Bitfield has been read from " + peer_.getAddress());
				
				setInterested(true);
			} else {
				close(ERROR_INCREASE_ERROR_COUNTER, "Could not read bitfield!");
			}
		}
	}

	/** Reading message: have. */
	private void readHaveMessage() throws InterruptedIOException, IOException, Exception {
		int pieceIndex = readInt();

		if ((pieceIndex >= 0) && (pieceIndex < torrent_.pieceCount())) {
			peer_.peerHasPiece(pieceIndex);

			setInterested(true);
		}
	}

	/** Reading message: piece. */
	private void readPieceMessage(int messageLength) throws InterruptedIOException, IOException, Exception {
		peer_.resetErrorCounter();
		latestRequestTime_ = 0;
		//peer_.setHadRequestTimeout(false);
		int index = readInt();								// index
		int begin = readInt();								// begin
		int pieceBlockSize = messageLength - 9;

		Log.v(LOG_TAG, "Recieved PIECE, Index: " + index + " Begin: " + begin + " Length: " + pieceBlockSize);

		if (pieceBlockSize < 0 || pieceBlockSize > Piece.DEFALT_BLOCK_LENGTH) {
			close("Error: unexpected block (invalid size: " + pieceBlockSize + ").");
		} else {
			byte[] pieceBlock = new byte[pieceBlockSize];	// read the unexpected block
			boolean successfullRead = readData(pieceBlock);
			if (!successfullRead) {
				close(ERROR_INCREASE_ERROR_COUNTER, "Reading piece failed!");
				return;
			}
			
			peer_.blockDownloaded(index, begin, pieceBlock);
		}
		
		issueDownload();
	}

	/** Reading message: request. */
	private void readRequestMessage(int messageLength) throws InterruptedIOException, IOException, Exception {
		peer_.resetErrorCounter();
		if (messageLength < 13) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Received request message length is smaller than 13!");
		} else {
			int pieceIndex = readInt();
			int begin = readInt();
			int length = readInt();
			
			if (isChoking()) return;
			
			Block block = new Block(torrent_.getPiece(pieceIndex), begin, length);
			blocksToUpload_.add(block);
			
			issueUpload();
			
			//log("in REQUEST Index: " + pieceIndex + " Begin: " + begin + " Length: " + length);

			/*if (Preferences.UploadEnabled) {
				ellapsedTime_ = 0;

				incomingRequests_.addElement(new BlockRequest(pieceIndex, begin, length));

				issueUpload();
			}*/
		}
	}

	/** Reading message: cancel. */
	private void readCancelMessage(int messageLength) throws InterruptedIOException, IOException, Exception {
		peer_.resetErrorCounter();
		if (messageLength < 13) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Received CANCEL message length is smaller than 13!");
		} else {
			int pieceIndex = readInt();
			int begin = readInt();
			int length = readInt();

			for (int i = 0; i < blocksToUpload_.size(); i++) {
				if (blocksToUpload_.elementAt(i).pieceIndex() == pieceIndex &&
				blocksToUpload_.elementAt(i).begin() == begin &&
				blocksToUpload_.elementAt(i).length() == length) {
					blocksToUpload_.removeElementAt(i);
					break;
				}
			}
		}
	}

	/** Sending message: handshake. */
	private void sendHandshakeMessage() {
		changeState(STATE_PW_HANDSHAKING);
        if (torrent_ != null) {
            Log.v(LOG_TAG, "Sending handshake to: " + peer_.getAddress());
            ByteArrayOutputStream baos = null;
            try {
                byte[] zeros = {0, 0, 0, 0, 0, 0, 0, 0};

                baos = new ByteArrayOutputStream();
                baos.write((byte) MESSAGE_PROTOCOL_ID.length());	// pstrlen:   string length of <pstr>, as a single raw byte 
                baos.write(MESSAGE_PROTOCOL_ID.getBytes());			// pstr:      string identifier of the protocol 
                baos.write(zeros);									// reserved:  eight (8) reserved bytes. All current implementations use all zeroes.
                baos.write(torrent_.getInfoHashByteArray());		// info_hash: 20-byte SHA1 hash of the info key in the metainfo file.
                baos.write(TorrentManager.getPeerID().getBytes());	// peer_id:   20-byte string used as a unique ID for the client.
                write(baos.toByteArray());

                Log.v(LOG_TAG, "Handshake sent to: " + peer_.getAddress());
            } catch (Exception e) {
                close(ERROR_INCREASE_ERROR_COUNTER, "Error while sending handshake: " + e.getMessage());
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
        	calculateElapsedTime();
            latestMessageSentTime_ = elapsedTime_;
            
            writeInt(0);
        } catch (Exception e) {
            close(ERROR_INCREASE_ERROR_COUNTER, "Error while sending keepalive: " + e.getMessage());
        }
    }
	
	/** Sending message: choke. */
	private void sendChokeMessage() {
		ByteArrayOutputStream baos = null;
        try {
        	calculateElapsedTime();
            latestMessageSentTime_ = elapsedTime_;

            baos = new ByteArrayOutputStream();
            baos.write(Tools.int32ToByteArray(1));	// len = 1
            baos.write(MESSAGE_ID_CHOKE);			// id = 0
            baos.flush();

            write(baos.toByteArray());
        } catch (Exception e) {
            close(ERROR_INCREASE_ERROR_COUNTER, "Error while sending choke: " + e.getMessage());
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
        	calculateElapsedTime();
            latestMessageSentTime_ = elapsedTime_;

            baos = new ByteArrayOutputStream();
            baos.write(Tools.int32ToByteArray(1));	// len = 1
            baos.write(MESSAGE_ID_UNCHOKE);			// id = 1
            baos.flush();

            write(baos.toByteArray());
        } catch (Exception e) {
            close(ERROR_INCREASE_ERROR_COUNTER, "Error while sending unchoke: " + e.getMessage());
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
			Log.v(LOG_TAG, "Sending interested to: " + peer_.getAddress());
			calculateElapsedTime();
			latestMessageSentTime_ = elapsedTime_;

			baos = new ByteArrayOutputStream();
			baos.write(Tools.int32ToByteArray(1));	// len = 1
			baos.write(MESSAGE_ID_INTERESTED);		// id = 2
			baos.flush();

			write(baos.toByteArray());
			
			Log.v(LOG_TAG, "Interested sent to: " + peer_.getAddress());
		} catch (Exception e) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Error while sending interested: " + e.getMessage());
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
			calculateElapsedTime();
			latestMessageSentTime_ = elapsedTime_;

			baos = new ByteArrayOutputStream();
			baos.write(Tools.int32ToByteArray(1));	// len = 1
			baos.write(MESSAGE_ID_NOT_INTERESTED);	// id = 3
			baos.flush();

			write(baos.toByteArray());
		} catch (Exception e) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Error while sending notinterested: " + e.getMessage());
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
			calculateElapsedTime();
			latestMessageSentTime_ = elapsedTime_;

			baos = new ByteArrayOutputStream();
			baos.write(Tools.int32ToByteArray(5));			// len = 5
			baos.write(MESSAGE_ID_HAVE);					// id = 4
			baos.write(Tools.int32ToByteArray(pieceIndex));	// piece index
			baos.flush();

			write(baos.toByteArray());
		} catch (Exception e) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Error while sending have: " + e.getMessage());
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
        	Log.v(LOG_TAG, "Sending bitfield to: " + peer_.getAddress());
        	calculateElapsedTime();
            latestMessageSentTime_ = elapsedTime_;

            baos = new ByteArrayOutputStream();
            baos.write(Tools.int32ToByteArray(1 + bitfield.length));	// len = 1 + X
            baos.write(MESSAGE_ID_BITFIELD);							// id = 5
            baos.write(bitfield);										// bitfield
            baos.flush();

            write(baos.toByteArray());
	    } catch (Exception e) {
	        close(ERROR_INCREASE_ERROR_COUNTER, "Error while sending bitfield: " + e.getMessage());
	    } finally {
	    	try {
				if (baos != null) baos.close();
			} catch (IOException e) {}
	    }
	}
	
	/** Sending message: request. */
	public void sendRequestMessage(ArrayList<Block> blocks) {
		if (blocks != null && blocks.size() > 0) {
			ByteArrayOutputStream baos = null;
			try {
				calculateElapsedTime();
				latestMessageSentTime_ = elapsedTime_;
				latestRequestTime_ = elapsedTime_;
				
				baos = new ByteArrayOutputStream();
				for (int i = 0; i < blocks.size(); i++) {
					Block block = blocks.get(i);
					block.setRequested();
					baos.write(Tools.int32ToByteArray(13));						// len = 13
					baos.write(MESSAGE_ID_REQUEST);								// id = 6
					baos.write(Tools.int32ToByteArray(block.pieceIndex()));		// index: zero-based piece index 
					baos.write(Tools.int32ToByteArray(block.begin()));			// begin: zero-based byte offset within the piece 
					baos.write(Tools.int32ToByteArray(block.length()));			// length: requested length
					
					Log.v(LOG_TAG, "REQUEST sent: " + block.pieceIndex() + " - " + block.begin() + " (length: " + block.length() + ")");
				}
				baos.flush();

				write(baos.toByteArray());
				Log.v(LOG_TAG, blocks.size() + " REQUESTS HAVE BEEN SENT.");
			} catch (Exception e) {
				close(ERROR_INCREASE_ERROR_COUNTER, "Error while sending request: " + e.getMessage());
			} finally {
	        	try {
					if (baos != null) baos.close();
				} catch (IOException e) {}
	        }
		}
	}
	
	/** Sending message: piece. */
	private void sendPieceMessage(Block block) {
		Piece piece = torrent_.getPiece(block.pieceIndex());
		
		if (piece != null) {
			Log.v(LOG_TAG, "Sending piece to " + peer_.getAddress() + " " + block.pieceIndex() + " Begin: " + block.begin() + " Length: " + block.length());

			if (block.begin() + block.length() > piece.size()) {
				close("Bad PIECE request (index is out of bounds)");
				return;
			}

			byte[] blockBytes = piece.readBlock(block);
			if (blockBytes == null) {
				close("Failed to extract block of piece");
				return;
			}
			
			ByteArrayOutputStream baos = null;
			try {
				calculateElapsedTime();
				latestMessageSentTime_ = elapsedTime_;
				
				baos = new ByteArrayOutputStream();
				baos.write(Tools.int32ToByteArray(9 + block.length()));	// len = 9 + X
				baos.write(MESSAGE_ID_PIECE);							// id = 7
				baos.write(Tools.int32ToByteArray(block.pieceIndex()));	// index
				baos.write(Tools.int32ToByteArray(block.begin()));		// begin
				baos.write(blockBytes);									// block
				baos.flush();

				write(baos.toByteArray());

				torrent_.updateBytesUploaded(block.length());

				// Log.v(LOG_TAG, "Piece sent to " + peer_.getAddress());
			} catch (Exception e) {
				close(ERROR_INCREASE_ERROR_COUNTER, "Error while sending piece: " + e.getMessage());
			} finally {
	        	try {
					if (baos != null) baos.close();
				} catch (IOException e) {}
	        }
		} else close("Bad PIECE index");
	}

	/** Sending message: cancel. */
	public void sendCancelMessage(Block block) {
		ByteArrayOutputStream baos = null;
		try {
			calculateElapsedTime();
			latestMessageSentTime_ = elapsedTime_;
			
			baos = new ByteArrayOutputStream();
			baos.write(Tools.int32ToByteArray(13));						// len = 13
			baos.write(MESSAGE_ID_CANCEL);								// id = cancel
			baos.write(Tools.int32ToByteArray(block.pieceIndex()));		// index
			baos.write(Tools.int32ToByteArray(block.begin()));			// begin
			baos.write(Tools.int32ToByteArray(block.length()));			// length
			baos.flush();

			write(baos.toByteArray());

			Log.v(LOG_TAG, "Cancel block index: " + block.pieceIndex() + " begin: " + block.begin() + " length: " + block.length());		
		} catch (Exception e) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Error while sending cancel message: " + e.getMessage());
		} finally {
        	try {
				if (baos != null) baos.close();
			} catch (IOException e) {}
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
			
			// Stop thread and close streams and socket
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
			
			// Notify the torrent about the disconnection of the peer
			peer_.disconnected();
			
			try {
				if (connectThread_ != null && connectThread_.isAlive()) connectThread_.interrupt();
			} catch (Exception e) {}
		}
	}

    /** Sets our chocking state. */
	public void setChoking(boolean choking) {
		if (choking) {
			if (!isChoking()) {
				isChoking_ = true;
				if (state_ == STATE_PW_CONNECTED) {
					blocksToUpload_ = new Vector<Block>();
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
			torrent_.peerInterested(peer_);
		} else {
			isPeerInterested_ = false;
			torrent_.peerNotInterested(peer_);
			if ((state_ == STATE_PW_CONNECTED) && (!isInterested())) {
				close("Nobody interested");
			}
		}
	}
	
	/** Changes the state of the connection. */
	public void changeState(int state) {
		state_ = state;
		calculateElapsedTime();
		elapsedTime_ = 0;
	}
	
	/** Sets the torrent that this peer is sharing. */
	public void setTorrent(Torrent torrent) {
		torrent_ = torrent;
		
	}
	
	/** Returns the connections state. */
	public int getState() {
		return state_;
	}
	
	/** Returns whether we are chocking the peer or not. */
	public boolean isChoking() {
		return isChoking_;
	}

	/** Returns whether the peer is chocking us or not. */
	public boolean isPeerChoking() {
		return isPeerChoking_;
	}
	
	/** Returns whether we find the peer interesting or not. */
	public boolean isInterested() {
		return isInterested_;
	}

	/** Returns whether the peer is interested in us or not. */
	public boolean isPeerInterested() {
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
			changeState(STATE_TCP_CONNECTING);
			try {
				if (!isIncomingConnection_) {
					Log.v(LOG_TAG, "Connecting to: " + destination_);
					socket_ = new Socket(peer_.getAddress(), peer_.getPort());
					Log.v(LOG_TAG, "Connected to: " + destination_);
				}
				socket_.setSoTimeout(TIMEOUT_REQUEST);
			} catch (IOException e) {
				if (socket_ != null) {
					try {
						socket_.close();
					} catch (IOException e1) {}
					socket_ = null;
				}
				close("Timeout: Failed to connect to " + destination_);
			}
			
			if (socket_ != null) startDownloading();
		}
	}
	
	/** Opens the streams, sends a handshake and starts reading the incoming messages. */
	private void startDownloading() {
		Log.v(LOG_TAG, "Opening streams on: " + peer_.getAddress());

        try {
            inputStream_ = new DataInputStream(socket_.getInputStream());
            outputStream_ = new DataOutputStream(socket_.getOutputStream());
        } catch(IOException ex) {
            close(ERROR_INCREASE_ERROR_COUNTER, "Opening streams failed on: " + peer_.getAddress() + " | " + ex.getMessage());
            return;
        }

        if (!isIncomingConnection_) sendHandshakeMessage();
        else changeState(STATE_PW_HANDSHAKING);
        
        read();
	}
	
	private void calculateElapsedTime() {
		long currentTime = SystemClock.elapsedRealtime();
		elapsedTime_ += (int) (currentTime - lastTime_);
		lastTime_ = currentTime;
	}
	
	public boolean isConnected() {
		return (state_ == STATE_PW_CONNECTED);
	}
}
