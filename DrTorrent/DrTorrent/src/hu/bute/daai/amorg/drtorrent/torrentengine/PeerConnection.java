package hu.bute.daai.amorg.drtorrent.torrentengine;

import hu.bute.daai.amorg.drtorrent.Preferences;
import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.Tools;
import hu.bute.daai.amorg.drtorrent.coding.bencode.Bencoded;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedDictionary;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedInteger;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedString;
import hu.bute.daai.amorg.drtorrent.coding.sha1.SHA1;
import hu.bute.daai.amorg.drtorrent.service.TorrentService;

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
	private final static int MESSAGE_ID_EXTENDED       = 20;
	private final static int MESSAGE_ID_EXTENDED_HANDSHAKE 		  = 0;
	private final static int MESSAGE_ID_EXTENDED_METADATA 		  = 1;
	private final static int MESSAGE_ID_EXTENDED_METADATA_REQUEST = 0;
	private final static int MESSAGE_ID_EXTENDED_METADATA_DATA 	  = 1;
	private final static int MESSAGE_ID_EXTENDED_METADATA_REJECT  = 2;
	
	private final static int TIMEOUT_TCP_CONNECTION = 1000 * 15;
	private final static int TIMEOUT_HANDSHAKE      = 1000 * 15;
	private final static int TIMEOUT_PW_CONNECTION  = 1000 * 2 * 65;
	private final static int TIMEOUT_REQUEST        = 1000 * 60;
	private final static int TIMEOUT_NOT_INTERESTED = 1000 * 15;
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
	private Vector<Integer> metadataBlockRejected_;
	private Vector<Integer> metadataBlockRequested_;
	
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
	
	private boolean hasExtensionProtocol_ = false;
	private int MESSAGE_ID_PEER_EXTENDED_METADATA = 1;
	private int metadataSize_ = 0;
	
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
        
        metadataBlockRejected_ = new Vector<Integer>();
        metadataBlockRequested_ = new Vector<Integer>();
    }
	
	/** Connects to the peer (including the peer wire connection). */
	public Runnable connect() {
		isIncomingConnection_ = false;
		
		String destination = peer_.getAddress() + ":" + peer_.getPort();
		connectThread_ = new ConnectThread(destination);
		return connectThread_;
		//connectThread_.start();
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

		switch (state_) {
			
			case STATE_NOT_CONNECTED:
				if (elapsedTime_ > 5000) {
					peer_.disconnected();
				}
				break;
		
			case STATE_TCP_CONNECTING:
				if (elapsedTime_ > TIMEOUT_TCP_CONNECTION) {
					peer_.incTcpTimeoutCount();
					close(ERROR_INCREASE_ERROR_COUNTER, "Timeout while trying to connect.");
				}
				break;

			case STATE_PW_HANDSHAKING:
				if (elapsedTime_ > TIMEOUT_HANDSHAKE) {
					//PWConnectionTimeoutCount_++;
					close(ERROR_INCREASE_ERROR_COUNTER, "Handshake timeout.");
				}
				break;

			case STATE_PW_CONNECTED:
				peer_.calculateSpeed(lastTime_);
				
				// Timeout: Because no messages have been received in the recent time
				if ((elapsedTime_ - latestMessageReceivedTime_) > TIMEOUT_PW_CONNECTION) {
					//PWConnectionTimeoutCount_++;
					close(ERROR_INCREASE_ERROR_COUNTER, "General timeout (no data received)");
					break;
				}

				if (latestRequestTime_ > 0 && ((elapsedTime_ - latestRequestTime_) > TIMEOUT_REQUEST)) {
					latestRequestTime_ = 0;
					/*if (torrent_.hasTimeoutlessPeer()) {
					 peer_.setHadRequestTimeout(true);
					 close(EIncreaseErrorCounter, "Request timeout"); break; }
					 */
					//PWConnectionTimeoutCount_++;
					close(ERROR_INCREASE_ERROR_COUNTER, "Request timeout");
					break;
					
				}
				
				if (torrent_ != null) {
					if (!torrent_.isValid()) {
						if (metadataBlockRequested_.isEmpty()) {
							issueDownload();
						}
					} else if (peer_.getState() == R.string.status_downloading && !peer_.hasRequest()) {
						Log.v(LOG_TAG, "onTimer: Peer doesn't have request");
						if (isInterested()) {
							Log.v(LOG_TAG, "onTimer: Peer doesn't have request: issueDownload");
							issueDownload();
						} else {
							if (peer_.hasInterestingBlock()) {
								setInterested(true);
							}
						}
					}
				}
				
				if (torrent_ != null && torrent_.isValid()) {
					// Nobody interested
					if (!isInterested() && !isPeerInterested()) {
						if ((elapsedTime_ - latestMessageSentTime_) > TIMEOUT_NOT_INTERESTED &&
							(elapsedTime_ - latestMessageReceivedTime_) > TIMEOUT_NOT_INTERESTED) {
							close("Nobody interested!");
							break;
						}
					}
				}
				
				// Keep alive: because we have not sent message in the recent time
				if ((elapsedTime_ - latestMessageSentTime_) >= KEEP_ALIVE_INTERVAL) {
					sendKeepAliveMessage();
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
				final Block block = (Block) blocksToUpload_.elementAt(0);
				sendPieceMessage(block);
				blocksToUpload_.removeElementAt(0);
			}
		}
	}
	
	/** Issues the download requests. */
	public synchronized void issueDownload(){
		if (torrent_ == null) return;
		if (torrent_.isValid()) {
			if (!isPeerChoking()) {
				final ArrayList<Block> blocksToDownload = peer_.issueDownload();
				if (blocksToDownload != null) {
					setInterested(true);
					sendRequestMessage(blocksToDownload);
				} else {
					if (!peer_.hasInterestingBlock()) {
						setInterested(false);
					}
				}
			}
		} else {
			if (hasExtensionProtocol_ && metadataSize_ > 0) {
				int index = torrent_.getMetadataBlockToDownload(this);
				if (index != -1) {
					metadataBlockRequested_.add(index);
					sendExtendedMetadataRequestMessage(index);
				}
			}
		}
	}
	
	/** Writes the byte array to the outputStream_. */
	private void write(final byte buffer[]) throws Exception {
		try {
			outputStream_.write(buffer);
			outputStream_.flush();
		} catch(Exception e) {}
	}
	
	/** Writes a big-endian integer to the outputStream_.*/
	private void writeInt(final int i) throws Exception {
		try {
			outputStream_.writeInt(i);
			outputStream_.flush();
		} catch(Exception e) {}
	}
	
	/** Reads data.length bytes from the inputStream_ and puts it into the data array. */
	private boolean readData(final byte [] data) throws Exception {
        try {
        	inputStream_.readFully(data);
        } catch (Exception e) {
        	Log.v("AAAAAAA", "Exception was thrown while readFully...");
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
			close(ERROR_INCREASE_ERROR_COUNTER, "ReadByte error " + ex.getMessage());
			return -1;
		}
	}
    
    /** Reads a big-endian integer from the inputStream_. */
	public int readInt() throws Exception {
		try {
			return inputStream_.readInt();
		} catch (Exception ex) {
			close(ERROR_INCREASE_ERROR_COUNTER, "ReadInt error " + ex.getMessage());
			return -1;
		}
	}
	
    /** Reads data from the inputStream_. */
	private void read() {
		try {
			while (isReadEnabled_ && (torrent_ == null || torrent_.isConnected())) {
				switch (state_) {
                	
                    case STATE_PW_HANDSHAKING: {
                    	calculateElapsedTime();
						latestMessageReceivedTime_ = elapsedTime_;
                    	
                    	Log.v(LOG_TAG, "Reading handshake message from: " + peer_.getAddress());
                        readHandshakeMessage();
                        break;
                    }
                    
					case STATE_PW_CONNECTED: {

						final int messageLength = readInt();				// length prefix
						if (messageLength == -1) {
							continue;
						}
						
						calculateElapsedTime();
						latestMessageReceivedTime_ = elapsedTime_;
						if (messageLength == 0) {
							// keep-alive
							Log.v(LOG_TAG, "Reading keep alive message from: " + peer_.getAddress());
							// issueDownload();
						} else {
							final int id = readByte();				// message ID
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
									
								case MESSAGE_ID_EXTENDED:
									Log.v(LOG_TAG, "Reading extended message from: " + peer_.getAddress());
									readExtendedMessage(messageLength);
									break;
									
								default:
									Log.v(LOG_TAG, "UNKNOWN MESSAGE " + messageLength + " " + id + " from: " + peer_.getAddress());
									break;
							}
						}
						break;
					}
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
	
	/** Reading message: handshake. */
	private void readHandshakeMessage() throws InterruptedIOException, IOException, Exception {
		final int protLength = readByte(); 					// pstrlen (19 if "BitTorrent protocol")
		if (protLength == -1) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Peer disconnected!");
			return;
		}
		
		final int handshakeLength = protLength + 48; 			// 49-1 because protLength has been read
		final byte[] handshake = new byte[handshakeLength];
		readData(handshake);

		{
			final byte[] protocolId = new byte[protLength]; 		// pstr
			System.arraycopy(handshake, 0, protocolId, 0, protLength);
	
			if (!MESSAGE_PROTOCOL_ID.equals(new String(protocolId))) {
				close(ERROR_DELETE_PEER, "Protocol identifier doesn't match!");
				return;
			}
		}
		
		{
			final byte[] reserved = new byte[8]; 					// reserved bits (8 byte long)
			System.arraycopy(handshake, protLength, reserved, 0, 8);
			if ((reserved[5] & (byte)0x10) != 0) {
				Log.v(LOG_TAG, peer_.getAddressPort() + " EXTENSION PROTOCOL SUPPORT!!!");
				hasExtensionProtocol_ = true;
			}
		}
		
		{
			final byte[] infoHash = new byte[20]; // info_hash
			System.arraycopy(handshake, protLength + 8, infoHash, 0, 20);
			
			if (!isIncomingConnection_) {
				if (!Tools.byteArrayEqual(torrent_.getInfoHashByteArray(), infoHash)) {
					close(ERROR_DELETE_PEER, "Torrent infohash doesn't match!");
					return;
				}
			} else { // if torrent is null then we should attach peer to torrent (most likely it is an incoming connection)
				Log.v(LOG_TAG, "Attach");
				boolean result = peer_.attachTorrent(SHA1.resultToString(infoHash));
				if (result) {
					sendHandshakeMessage();
				} else {
					close(ERROR_DELETE_PEER, "Invalid infohash or peer is already connected or too many peers!");
					return;
				}
			}
		}
		
		if (hasExtensionProtocol_) {
			sendExtendedHandshakeMessage();
		}

		{
			final byte[] peerId = new byte[20]; // peer_id
			System.arraycopy(handshake, protLength + 28, peerId, 0, 20);
			String tempPeerId = new String(peerId);
			if (peer_.getPeerId() != null) {
				if (!peer_.getPeerId().equals(tempPeerId)) {
					peer_.setPeerId(tempPeerId);
					//close(ERROR_INCREASE_ERROR_COUNTER, "Peer ID doesn't match!");
					return;
				} else if (tempPeerId.equals(TorrentManager.getPeerID())) {
					close(ERROR_DELETE_PEER, "Connected to ourselves!");
					return;
				}
			} else {
				peer_.setPeerId(tempPeerId);
			}
		}

		Log.v(LOG_TAG, "Handshake completed! Peer wire connected!");
		changeState(STATE_PW_CONNECTED);

		if (torrent_.getBitfield() != null && !torrent_.getBitfield().isNull()) {
			sendBitfieldMessage();
		}
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
		if (torrent_.isValid() && bitFieldLength != torrent_.getBitfield().getLengthInBytes()) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Received bitfield length doesn't match! " + bitFieldLength + " instead of " + peer_.getBitfield().getLengthInBytes());
		} else {
			final byte[] bitfield = new byte[bitFieldLength];
			final boolean dataReaded = readData(bitfield);
			if (dataReaded) {
				if (torrent_.isValid()) {
					peer_.peerHasPieces(bitfield);
				} else {
					final Bitfield b = new Bitfield(bitfield);
					peer_.setBitfield(b);
				}
				
				Log.v(LOG_TAG, "Bitfield has been read from " + peer_.getAddress());
				
				if (torrent_.isValid() && peer_.hasInterestingBlock()) {
					setInterested(true);
				}
			} else {
				close(ERROR_INCREASE_ERROR_COUNTER, "Could not read bitfield!");
			}
		}
	}

	/** Reading message: have. */
	private void readHaveMessage() throws InterruptedIOException, IOException, Exception {
		final int pieceIndex = readInt();

		if ((pieceIndex >= 0) && (pieceIndex < torrent_.pieceCount())) {
			peer_.peerHasPiece(pieceIndex);

			if (torrent_.isValid() && peer_.hasInterestingBlock()) {
				setInterested(true);
			}
		}
	}

	/** Reading message: piece. */
	private void readPieceMessage(int messageLength) throws InterruptedIOException, IOException, Exception {
		peer_.resetErrorCounter();
		latestRequestTime_ = 0;
		//peer_.setHadRequestTimeout(false);
		final int index = readInt();								// index
		final int begin = readInt();								// begin
		final int pieceBlockSize = messageLength - 9;

		Log.v(LOG_TAG, "Recieved PIECE, Index: " + index + " Begin: " + begin + " Length: " + pieceBlockSize);

		if (pieceBlockSize < 0 || pieceBlockSize > Piece.DEFALT_BLOCK_LENGTH) {
			close("Error: unexpected block (invalid size: " + pieceBlockSize + ").");
		} else {
			final byte[] pieceBlock = new byte[pieceBlockSize];	// read the unexpected block
			final boolean successfullRead = readData(pieceBlock);
			if (!successfullRead) {
				close(ERROR_INCREASE_ERROR_COUNTER, "Reading piece failed!");
				return;
			}
			
			peer_.blockDownloaded(index, begin, pieceBlock);
		}
	}

	/** Reading message: request. */
	private void readRequestMessage(int messageLength) throws InterruptedIOException, IOException, Exception {
		peer_.resetErrorCounter();
		if (messageLength < 13) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Received request message length is smaller than 13!");
		} else {
			final int pieceIndex = readInt();
			final int begin = readInt();
			final int length = readInt();
			
			if (isChoking()) {
				return;
			}
			if (!Preferences.isUploadEnabled()) {
				setChoking(true);
				return;
			}
			
			final Block block = new Block(torrent_.getPiece(pieceIndex), begin, length);
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
			final int pieceIndex = readInt();
			final int begin = readInt();
			final int length = readInt();

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
	
	/** Reading message: extended. */
	private void readExtendedMessage(int messageLength) throws InterruptedIOException, IOException, Exception {
		peer_.resetErrorCounter();
		
		final int extendedMessageId = readByte();
		Log.v(LOG_TAG, "Extended message id: " + extendedMessageId);
		
		switch (extendedMessageId) {
			case MESSAGE_ID_EXTENDED_HANDSHAKE:
				readExtendedHandshakeMessage(messageLength);
				break;
	
			case MESSAGE_ID_EXTENDED_METADATA:
				readExtendedMetadataDataMessage(messageLength);
				break;
			default:
				Log.v("LOG_TAG", "Unkown extended message " + extendedMessageId);
				readData(new byte[messageLength - 2]);
				break;
		}
	}
	
	/** Reading message: extended handshake. */
	public void readExtendedHandshakeMessage(int messageLength) throws InterruptedIOException, IOException, Exception {
		Bencoded bencoded = null;
		{
			final byte[] payload = new byte[messageLength - 2];
			boolean successfullRead = readData(payload);
			if (!successfullRead) {
				close(ERROR_INCREASE_ERROR_COUNTER, "Reading piece failed!");
				return;
			}
			
			Log.v(LOG_TAG, "Extended message HANDSHAKE: " + payload.toString());
			
			try {
				bencoded = Bencoded.parse(payload);
			} catch (Exception e) {
				Log.v(LOG_TAG, "Error occured while processing the payload of the extension message: " + e.getMessage());
				return;
			}
		}
		
		if (bencoded.type() == Bencoded.BENCODED_DICTIONARY) {
			final BencodedDictionary dict = (BencodedDictionary) bencoded;
			Bencoded tempBencoded = null;

			// announce
			tempBencoded = dict.entryValue("m");
			if (tempBencoded != null && tempBencoded.type() == Bencoded.BENCODED_DICTIONARY) {
				BencodedDictionary m = (BencodedDictionary) tempBencoded;
				
				tempBencoded = m.entryValue("ut_metadata");
				if (tempBencoded != null && tempBencoded.type() == Bencoded.BENCODED_INTEGER) {
					MESSAGE_ID_PEER_EXTENDED_METADATA = (int) ((BencodedInteger) tempBencoded).getValue();
					Log.v(LOG_TAG, "Extended message: metadata support code: " + MESSAGE_ID_PEER_EXTENDED_METADATA);
			
					tempBencoded = dict.entryValue("metadata_size");
					if (tempBencoded != null && tempBencoded.type() == Bencoded.BENCODED_INTEGER) {
						metadataSize_ = (int) ((BencodedInteger) tempBencoded).getValue();
						Log.v(LOG_TAG, "Extended message: metadata size: " + metadataSize_);
						if (torrent_ != null) {
							torrent_.setMetadataSize(metadataSize_);
						}
					}
				}
			}
			
			tempBencoded = dict.entryValue("v");
			if (tempBencoded != null && tempBencoded.type() == Bencoded.BENCODED_STRING) {
				String v = ((BencodedString) tempBencoded).getStringValue();
				peer_.setClientName(v);
				Log.v(LOG_TAG, "Extended message: client: " + v);
			}
			
			/*tempBencoded = dict.entryValue("yourip");
			if (tempBencoded != null && tempBencoded.type() == Bencoded.BENCODED_STRING) {
				String myip = ((BencodedString) tempBencoded).getStringValue();
				myip = Tools.readIp(myip);
				
				Log.v(LOG_TAG, "Extended message: myip: " + myip);
			}*/
		}
	}
	
	/** Reading message: extended metadata data. */
	public void readExtendedMetadataDataMessage(int messageLength) throws InterruptedIOException, IOException, Exception {
		final byte[] payload = new byte[messageLength - 2];
		boolean successfullRead = readData(payload);
		if (!successfullRead) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Reading piece failed!");
			return;
		}
		
		Log.v(LOG_TAG, "Extended message METADATA full length: " + payload.length);
		//Log.v(LOG_TAG, "Extended message METADATA: " + payload.toString());
		
		Bencoded bencoded = null;
		try {
			bencoded = Bencoded.parse(payload);
		} catch (Exception e) {
			Log.v(LOG_TAG, "Error occured while processing the payload of the extension message: " + e.getMessage());
			return;
		}
		
		if (bencoded.type() == Bencoded.BENCODED_DICTIONARY) {
			BencodedDictionary dict = (BencodedDictionary) bencoded;
			Bencoded tempBencoded = null;

			tempBencoded = dict.entryValue("msg_type");
			if (tempBencoded != null && tempBencoded.type() == Bencoded.BENCODED_INTEGER) {
				int msg_type = (int)((BencodedInteger) tempBencoded).getValue();
				int piece = -1;
				int total_size = 0;
				
				switch (msg_type) {
					case MESSAGE_ID_EXTENDED_METADATA_REQUEST:
						tempBencoded = dict.entryValue("piece");
						if (tempBencoded != null && tempBencoded.type() == Bencoded.BENCODED_INTEGER) {
							piece = (int) ((BencodedInteger) tempBencoded).getValue();
							Log.v(LOG_TAG, "Extended message metadata request: piece: " + piece);
							
							if (torrent_.isValid()) {
								sendExtendedMetadataDataMessage(piece);
							} else {
								sendExtendedMetadataRejectMessage(piece);
							}
						}
						break;
				
					case MESSAGE_ID_EXTENDED_METADATA_DATA:
						tempBencoded = dict.entryValue("piece");
						if (tempBencoded != null && tempBencoded.type() == Bencoded.BENCODED_INTEGER) {
							piece = (int) ((BencodedInteger) tempBencoded).getValue();
							Log.v(LOG_TAG, "Extended message metadata data: piece: " + piece);
						}
						
						tempBencoded = dict.entryValue("total_size");
						if (tempBencoded != null && tempBencoded.type() == Bencoded.BENCODED_INTEGER) {
							total_size = (int) ((BencodedInteger) tempBencoded).getValue();
							Log.v(LOG_TAG, "Extended message metadata data: total_size: " + total_size);
						}
						
						if (total_size == metadataSize_ && metadataSize_ > piece * Metadata.BLOCK_SIZE) {
							int size = metadataSize_ - piece * Metadata.BLOCK_SIZE;
							size = (size > Metadata.BLOCK_SIZE) ? Metadata.BLOCK_SIZE : size;
							
							if (payload.length - size > 0) {
								byte[] left = new byte[size];
								System.arraycopy(payload, payload.length - size, left, 0, size);
								
								//Log.v(LOG_TAG, "Extended message METADATA left length: " + left.length);
								//Log.v(LOG_TAG, "Extended message METADATA: " + left.toString());
								
								//// Removable part
								/*StringBuffer sb = new StringBuffer();
								for (int i = 0; i < left.length; i++) {
									sb.append((char)left[i]);
								}
								Log.v(LOG_TAG, "Extended message METADATA: " + sb.toString());*/
								////
								
								torrent_.addBlockToMetadata(piece, left);
								metadataBlockRequested_.removeElement(piece);
							}
						}
						
						break;
						
					case MESSAGE_ID_EXTENDED_METADATA_REJECT:
						tempBencoded = dict.entryValue("piece");
						if (tempBencoded != null && tempBencoded.type() == Bencoded.BENCODED_INTEGER) {
							piece = (int) ((BencodedInteger) tempBencoded).getValue();
							Log.v(LOG_TAG, "Extended message metadata reject: piece: " + piece);
						}
						
						if (metadataBlockRejected_.contains(piece)) {
							metadataBlockRejected_.add(piece);
							torrent_.cancelMetadataBlock(piece);
							metadataBlockRequested_.remove(piece);
						}
						break;
						
					default:
						Log.v("LOG_TAG", "Not extended metadata data message " + ((BencodedInteger) tempBencoded).getValue());
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
            
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                final byte[] reserved = {0, 0, 0, 0, 0, (byte)0x10, 0, 0};	// 

                baos.write((byte) MESSAGE_PROTOCOL_ID.length());	// pstrlen:   string length of <pstr>, as a single raw byte 
                baos.write(MESSAGE_PROTOCOL_ID.getBytes());			// pstr:      string identifier of the protocol 
                baos.write(reserved);								// reserved:  eight (8) reserved bytes. EXTENSION PROTOCOL (bit 20 from the right / reserved[5]=0x10)
                baos.write(torrent_.getInfoHashByteArray());		// info_hash: 20-byte SHA1 hash of the info key in the metainfo file.
                baos.write(TorrentManager.getPeerID().getBytes());	// peer_id:   20-byte string used as a unique ID for the client.
                write(baos.toByteArray());

                Log.v(LOG_TAG, "Handshake sent to: " + peer_.getAddress());
            } catch (Exception e) {
                close(ERROR_INCREASE_ERROR_COUNTER, "Error while sending handshake: " + e.getMessage());
            } finally {
            	try {
					if (baos != null) {
						baos.close();
					}
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
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
        	calculateElapsedTime();
            latestMessageSentTime_ = elapsedTime_;

            baos.write(Tools.int32ToByteArray(1));	// len = 1
            baos.write(MESSAGE_ID_CHOKE);			// id = 0
            baos.flush();

            write(baos.toByteArray());
        } catch (Exception e) {
            close(ERROR_INCREASE_ERROR_COUNTER, "Error while sending choke: " + e.getMessage());
        } finally {
        	try {
				if (baos != null) {
					baos.close();
				}
			} catch (IOException e) {}
        }
    }

	/** Sending message: unchoke. */
    private void sendUnchokeMessage() {
    	final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
        	calculateElapsedTime();
            latestMessageSentTime_ = elapsedTime_;

            baos.write(Tools.int32ToByteArray(1));	// len = 1
            baos.write(MESSAGE_ID_UNCHOKE);			// id = 1
            baos.flush();

            write(baos.toByteArray());
        } catch (Exception e) {
            close(ERROR_INCREASE_ERROR_COUNTER, "Error while sending unchoke: " + e.getMessage());
        } finally {
        	try {
				if (baos != null) {
					baos.close();
				}
			} catch (IOException e) {}
        }
    }

    /** Sending message: interested. */
	private void sendInterestedMessage() {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			Log.v(LOG_TAG, "Sending interested to: " + peer_.getAddress());
			calculateElapsedTime();
			latestMessageSentTime_ = elapsedTime_;

			baos.write(Tools.int32ToByteArray(1));	// len = 1
			baos.write(MESSAGE_ID_INTERESTED);		// id = 2
			baos.flush();

			write(baos.toByteArray());
			
			Log.v(LOG_TAG, "Interested sent to: " + peer_.getAddress());
		} catch (Exception e) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Error while sending interested: " + e.getMessage());
		} finally {
        	try {
				if (baos != null) {
					baos.close();
				}
			} catch (IOException e) {}
        }
	}

	/** Sending message: not interested. */
	private void sendNotInterestedMessage() {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			calculateElapsedTime();
			latestMessageSentTime_ = elapsedTime_;

			baos.write(Tools.int32ToByteArray(1));	// len = 1
			baos.write(MESSAGE_ID_NOT_INTERESTED);	// id = 3
			baos.flush();

			write(baos.toByteArray());
		} catch (Exception e) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Error while sending notinterested: " + e.getMessage());
		} finally {
        	try {
				if (baos != null) {
					baos.close();
				}
			} catch (IOException e) {}
        }
	}
	
	/** Sending message: have. */
	public void sendHaveMessage(int pieceIndex) {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			calculateElapsedTime();
			latestMessageSentTime_ = elapsedTime_;

			baos.write(Tools.int32ToByteArray(5));			// len = 5
			baos.write(MESSAGE_ID_HAVE);					// id = 4
			baos.write(Tools.int32ToByteArray(pieceIndex));	// piece index
			baos.flush();

			write(baos.toByteArray());
		} catch (Exception e) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Error while sending have: " + e.getMessage());
		} finally {
        	try {
				if (baos != null) {
					baos.close();
				}
			} catch (IOException e) {}
        }
	}
	
	/** Sending message: bitfield. */
	private void sendBitfieldMessage() {
	    byte[] bitfield = torrent_.getBitfield().data();
	    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    try {
        	Log.v(LOG_TAG, "Sending bitfield to: " + peer_.getAddress());
        	calculateElapsedTime();
            latestMessageSentTime_ = elapsedTime_;

            baos.write(Tools.int32ToByteArray(1 + bitfield.length));	// len = 1 + X
            baos.write(MESSAGE_ID_BITFIELD);							// id = 5
            baos.write(bitfield);										// bitfield
            baos.flush();

            write(baos.toByteArray());
	    } catch (Exception e) {
	        close(ERROR_INCREASE_ERROR_COUNTER, "Error while sending bitfield: " + e.getMessage());
	    } finally {
	    	try {
				if (baos != null) {
					baos.close();
				}
			} catch (IOException e) {}
	    }
	}
	
	/** Sending message: request. */
	public void sendRequestMessage(ArrayList<Block> blocks) {
		if (blocks != null && blocks.size() > 0) {
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				calculateElapsedTime();
				latestMessageSentTime_ = elapsedTime_;
				latestRequestTime_ = elapsedTime_;
				
				Block block;
				for (int i = 0; i < blocks.size(); i++) {
					block = blocks.get(i);
					block.setRequested();
					baos.write(Tools.int32ToByteArray(13));						// len = 13
					baos.write(MESSAGE_ID_REQUEST);								// id = 6
					baos.write(Tools.int32ToByteArray(block.pieceIndex()));		// index: zero-based piece index 
					baos.write(Tools.int32ToByteArray(block.begin()));			// begin: zero-based byte offset within the piece 
					baos.write(Tools.int32ToByteArray(block.length()));			// length: requested length
					
					Log.v(LOG_TAG, "REQUEST sent: " + block.pieceIndex() + " - " + block.begin() + " (length: " + block.length() + ")");
				}
				block = null;
				//Log.v(LOG_TAG, "Total: " + Tools.bytesToString(baos.size()));
				baos.flush();

				write(baos.toByteArray());
				Log.v(LOG_TAG, blocks.size() + " REQUESTS HAVE BEEN SENT.");
			} catch (Exception e) {
				close(ERROR_INCREASE_ERROR_COUNTER, "Error while sending request: " + e.getMessage());
			} finally {
	        	try {
					if (baos != null) {
						baos.close();
					}
				} catch (IOException e) {}
	        }
		}
	}
	
	/** Sending message: piece. */
	private void sendPieceMessage(Block block) {
		if (!Preferences.isUploadEnabled()) {
			setChoking(true);
			return;
		}
		
		final Piece piece = torrent_.getPiece(block.pieceIndex());
		if (piece != null) {
			Log.v(LOG_TAG, "Sending piece to " + peer_.getAddress() + " " + block.pieceIndex() + " Begin: " + block.begin() + " Length: " + block.length());

			if (block.begin() + block.length() > piece.size()) {
				close("Bad PIECE request (index is out of bounds)");
				return;
			}

			final byte[] blockBytes = piece.readBlock(block);
			if (blockBytes == null) {
				close("Failed to extract block of piece");
				return;
			}
			
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				calculateElapsedTime();
				latestMessageSentTime_ = elapsedTime_;
				
				baos.write(Tools.int32ToByteArray(9 + block.length()));	// len = 9 + X
				baos.write(MESSAGE_ID_PIECE);							// id = 7
				baos.write(Tools.int32ToByteArray(block.pieceIndex()));	// index
				baos.write(Tools.int32ToByteArray(block.begin()));		// begin
				baos.write(blockBytes);									// block
				baos.flush();

				write(baos.toByteArray());

				torrent_.updateBytesUploaded(block.length());
				peer_.blockUploaded(blockBytes.length);

				// Log.v(LOG_TAG, "Piece sent to " + peer_.getAddress());
			} catch (Exception e) {
				close(ERROR_INCREASE_ERROR_COUNTER, "Error while sending piece: " + e.getMessage());
			} finally {
	        	try {
					if (baos != null) {
						baos.close();
					}
				} catch (IOException e) {}
	        }
		} else {
			close("Bad PIECE index");
		}
	}

	/** Sending message: cancel. */
	public void sendCancelMessage(Block block) {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			calculateElapsedTime();
			latestMessageSentTime_ = elapsedTime_;
			
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
				if (baos != null) {
					baos.close();
				}
			} catch (IOException e) {}
        }
	}
	
	/** Sending a Extended handshake message. */
	public void sendExtendedHandshakeMessage() {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			calculateElapsedTime();
			latestMessageSentTime_ = elapsedTime_;
			
			final String clientName =  "DrTorrent " + TorrentService.APP_VERSION_NAME;
			
			//String payload = "d1:v" + clientName.length() + ":" + clientName + "e";
			BencodedDictionary dict = new BencodedDictionary();
			BencodedDictionary m = new BencodedDictionary();
			m.addEntry("ut_metadata", MESSAGE_ID_EXTENDED_METADATA);
			dict.addEntry("m", m);
			dict.addEntry("v", clientName);
			if (torrent_ != null && torrent_.isValid() && torrent_.getMetadataSize() > 0) {
				dict.addEntry("metadata_size", torrent_.getMetadataSize());
			}
			final byte[] payload = dict.Bencode();
			dict = null;
			m = null;
			
			baos.write(Tools.int32ToByteArray(1 + 1 + payload.length));
			baos.write(MESSAGE_ID_EXTENDED);
			baos.write(MESSAGE_ID_EXTENDED_HANDSHAKE);
			baos.write(payload);
			baos.flush();
			
			write(baos.toByteArray());
			
		} catch (Exception e) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Error while sending extended handshake message: " + e.getMessage());
		} finally {
        	try {
				if (baos != null) {
					baos.close();
				}
			} catch (IOException e) {}
        }
	}
	
	/** Sending a Extended metadata request message. */
	public void sendExtendedMetadataRequestMessage(int piece) {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			calculateElapsedTime();
			latestMessageSentTime_ = elapsedTime_;
			
			//String payload = "d8:msg_typei0e5:piecei" + piece + "ee";
			BencodedDictionary dict = new BencodedDictionary();
			dict.addEntry("msg_type", MESSAGE_ID_EXTENDED_METADATA_REQUEST);
			dict.addEntry("piece", piece);
			final byte[] payload = dict.Bencode();
			dict = null;
			
			baos.write(Tools.int32ToByteArray(1 + 1 + payload.length));
			baos.write(MESSAGE_ID_EXTENDED);
			baos.write(MESSAGE_ID_PEER_EXTENDED_METADATA);
			baos.write(payload);
			baos.flush();
			
			write(baos.toByteArray());
			
			Log.v(LOG_TAG, "Extended message sent metadata request");
		} catch (Exception e) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Error while sending extended metadata request message: " + e.getMessage());
		} finally {
        	try {
				if (baos != null) {
					baos.close();
				}
			} catch (IOException e) {}
        }
	}
	
	/** Sending a Extended metadata data message. */
	public void sendExtendedMetadataDataMessage(int piece) {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			calculateElapsedTime();
			latestMessageSentTime_ = elapsedTime_;
			
			//String payload = "d8:msg_typei0e5:piecei" + piece + "ee";
			BencodedDictionary dict = new BencodedDictionary();
			dict.addEntry("msg_type", MESSAGE_ID_EXTENDED_METADATA_DATA);
			dict.addEntry("piece", piece);
			dict.addEntry("total_size", torrent_.getMetadataSize());
			final byte[] dictionary = dict.Bencode();
			dict = null;
			final byte[] metadata = torrent_.readMetadataBlock(piece);
			if (metadata == null) {
				sendExtendedMetadataRejectMessage(piece);
				return;
			}
			
			baos.write(Tools.int32ToByteArray(1 + 1 + dictionary.length + metadata.length));
			baos.write(MESSAGE_ID_EXTENDED);
			baos.write(MESSAGE_ID_PEER_EXTENDED_METADATA);
			baos.write(dictionary);
			baos.write(metadata);
			baos.flush();
			
			write(baos.toByteArray());
			
			Log.v(LOG_TAG, "Extended message sent metadata data");
		} catch (Exception e) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Error while sending extended metadata data message: " + e.getMessage());
		} finally {
        	try {
				if (baos != null) {
					baos.close();
				}
			} catch (IOException e) {}
        }
	}
	
	/** Sending a Extended metadata reject message. */
	public void sendExtendedMetadataRejectMessage(int piece) {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			calculateElapsedTime();
			latestMessageSentTime_ = elapsedTime_;
			
			//String payload = "d8:msg_typei0e5:piecei" + piece + "ee";
			BencodedDictionary dict = new BencodedDictionary();
			dict.addEntry("msg_type", MESSAGE_ID_EXTENDED_METADATA_REJECT);
			dict.addEntry("piece", piece);
			final byte[] payload = dict.Bencode();
			dict = null;
			
			baos.write(Tools.int32ToByteArray(1 + 1 + payload.length));
			baos.write(MESSAGE_ID_EXTENDED);
			baos.write(MESSAGE_ID_PEER_EXTENDED_METADATA);
			baos.write(payload);
			baos.flush();
			
			write(baos.toByteArray());
			
			Log.v(LOG_TAG, "Extended message sent metadata reject");
		} catch (Exception e) {
			close(ERROR_INCREASE_ERROR_COUNTER, "Error while sending extended metadata reject message: " + e.getMessage());
		} finally {
        	try {
				if (baos != null) {
					baos.close();
				}
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
			if (torrent_ != null && !torrent_.isValid()) {
				for (int i = 0; i < metadataBlockRequested_.size(); i++) {
					torrent_.cancelMetadataBlock(metadataBlockRequested_.elementAt(i));
				}
				metadataBlockRequested_.removeAllElements();
			}
			
			try {
				if (connectThread_ != null && connectThread_.isAlive()) {
					connectThread_.interrupt();
				}
			} catch (Exception e) {}
		}
	}

    /** Sets our chocking state. */
	public void setChoking(final boolean choking) {
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
	private void setPeerChoking(final boolean choking) {
		isPeerChoking_ = choking;
	}

	/** Sets our interest. */
	private void setInterested(final boolean interested) {
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
	private void setPeerInterested(final boolean interested) {
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
	public void changeState(final int state) {
		state_ = state;
		calculateElapsedTime();
		elapsedTime_ = 0;
	}
	
	public boolean isMetadataBlockRejected(final int index) {
		return metadataBlockRequested_.contains(index);
	}
	
	/** Sets the torrent that this peer is sharing. */
	public void setTorrent(final Torrent torrent) {
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
				peer_.incTcpTimeoutCount();
				close("Timeout: Failed to connect to " + destination_);
			}
			
			if (socket_ != null) {
				startDownloading();
			}
		}
	}
	
	/** Opens the streams, sends a handshake and starts reading the incoming messages. */
	private void startDownloading() {
		Log.v(LOG_TAG, "Opening streams on: " + peer_.getAddress());

        try {
            inputStream_ = new DataInputStream(socket_.getInputStream());
            outputStream_ = new DataOutputStream(socket_.getOutputStream());
        } catch(IOException ex) {
        	peer_.incTcpTimeoutCount();
            close(ERROR_INCREASE_ERROR_COUNTER, "Opening streams failed on: " + peer_.getAddress() + " | " + ex.getMessage());
            return;
        }
        
        if (!isIncomingConnection_) {
        	sendHandshakeMessage();
        } else {
        	changeState(STATE_PW_HANDSHAKING);
        }
        peer_.resetTcpTimeoutCount();
        
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
