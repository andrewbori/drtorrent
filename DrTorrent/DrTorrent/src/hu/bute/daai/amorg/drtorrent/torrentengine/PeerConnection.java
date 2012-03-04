package hu.bute.daai.amorg.drtorrent.torrentengine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;

import android.util.Log;

public class PeerConnection {
	private static final String LOG_TAG = "PeerConnection";
	private final String ProtocolId = "BitTorrent protocol";
	
	public final static int KMessageIdChoke = 0;
    public final static int KMessageIdUnchoke = 1;
    public final static int KMessageIdInterested = 2;
    public final static int KMessageIdNotInterested = 3;
    public final static int KMessageIdHave = 4;
    public final static int KMessageIdBitfield = 5;
    public final static int KMessageIdRequest = 6;
    public final static int KMessageIdPiece = 7;
    public final static int KMessageIdCancel = 8;
	
	public final static int EPeerNotConnected = 0;
    public final static int EPeerTcpConnecting = 1;
    public final static int EPeerConnected = 2;
    public final static int EPeerPwHandshaking = 3;
    public final static int EPeerPwConnected = 4;
    public final static int EPeerClosing = 5;
    public final static int EDeletePeer = 0;
    public final static int EIncreaseErrorCounter = 1;
    public final static int ENotSpecified = 2;
	
	private Peer peer_;
	private Torrent torrent_;
	private TorrentManager torrentManager_;
	
	private boolean incomingConnection_;
	private boolean readEnabled_;
	private int state_;
	
	private boolean interested_;
	private boolean peerInterested_;
	private boolean choking_;
	private boolean peerChoking_;
	
	private ConnectThread connectThread_;
	private Socket socket_;
	private InputStream inputStream_;
	private OutputStream outputStream_;
	
	/** Creates a new instance of PeerConnection. */
	public PeerConnection(Peer peer, Torrent torrent, TorrentManager torrentManager)
    {
        peer_ = peer;
        torrent_ = torrent;
        torrentManager_ = torrentManager;
        incomingConnection_ = false;

        interested_ = false;
        peerInterested_ = false;
        choking_ = true;
        peerChoking_ = true;

        //piecesToDownload_ = new Vector<MTPieceToDownload>();
        //incomingRequests_ = new Vector<BlockRequest>();

        state_ = EPeerNotConnected;
        readEnabled_ = true;
    }
	
	public void onTimer() {
		
		
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
        }
        catch(IOException ex) {
            //close(EIncreaseErrorCounter, "Opening streams failed - " + ex.getMessage());
            Log.v(LOG_TAG, "Opening streams failed on: " + peer_.getAddress() + " | " + ex.getMessage());
            return;
        }

        Log.v(LOG_TAG, "Start download from: " + peer_.getAddress());
        state_ = EPeerPwHandshaking;
        sendHandshakeMessage();

        // ellapsedTime = 0;
        //System.out.println("STARTREAD: "+peer.getAddress());
        new Thread() {
            public void run() {
                read();
            }
        }.start();
	}
	
	private boolean readData(byte [] data) throws Exception {
        return readData(data, 0, data.length);
    }

    private boolean readData(byte [] data, int offset, int len) throws Exception {
        int remain = len;
        while(remain > 0) {
            final int readed = inputStream_.read(data, offset, remain);
            if(readed == -1) {
                break;
            }
            remain -= readed;
            offset += readed;
        }

        return remain == 0;
    }
	
	private void read() {
        int messageLength = 0;

        while(readEnabled_) {
            try {
                switch(state_) {
                	
                    case EPeerPwHandshaking: {
                        int protLength = inputStream_.read();

                        // -1 error from the stream: connection closed
                        if(protLength == -1) {
                            close(EIncreaseErrorCounter, "Peer disconnected!");
                            continue;
                        }

                        int handshakeLength = protLength + 48;	// -1 because of protLength

                        byte[] handshake = new byte[handshakeLength];
                        ////inputStream_.read(handshake);
                        readData(handshake);

                        byte[] otherProtocolId = new byte[protLength];
                        System.arraycopy(handshake, 0, otherProtocolId, 0, protLength);

                        if(!ProtocolId.equals(new String(otherProtocolId))) {
                            close(EDeletePeer, "Protocol identifier doesn't match!");
                            continue;
                        }

                        byte[] infoHash = new byte[20];
                        System.arraycopy(handshake, 27, infoHash, 0, 20);

                        if(torrent_ != null) {
                            /*if(!NetTools.byteArrayEqual(infoHash, torrent_.getInfoHashByteArray()))
                            {
                                close(EDeletePeer, "Torrent infohash doesn't match!");
                                continue;
                            }*/
                        } else { // if torrent is null then we should attach peer to torrent (most likely it is an incoming connection)
                            /*if((torrentManager_.attachPeerToTorrent(infoHash, getPeerConnection()) != MTErrorCodes.ErrNone) || (torrent_ == null)) // close if the attach failed
                            {
                                close(EDeletePeer, "Invalid infohash or peer is already connected or too many peers!");
                                continue;
                            }*/
                        }

                        if(incomingConnection_) {
                            /*torrentManager_.notifyTorrentObserver(torrent_, MTTorrentObserver.EMTEventIncomingConnectionsChanged);
                            torrent_.incIncomingConnectionsNum();
                            sendHandshakeMessage();
                            peer_.resetAddress();*/
                        }

                        byte[] peerId = new byte[20];
                        System.arraycopy(handshake, 47, peerId, 0, 20);
                        String tempPId = new String(peerId);
                        if(peer_.getPeerId() != null) {
                            if(!peer_.getPeerId().equals(tempPId)) {
                                close(EIncreaseErrorCounter, "Peer ID doesn't match!");
                                continue;
                            } else if(tempPId.equals(torrentManager_.getPeerID())) {
                                close(EDeletePeer, "Connected to ourselves!");
                                continue;
                            }
                        }
                        else {
                            //peer_.setPeerId(tempPId);
                        }

                        Log.v(LOG_TAG, "Handshake completed! Peer wire connected!");
                        state_ = EPeerConnected; //changeState(EPeerPwConnected);
                        //setPeerWireConnected();

                        /*if(!torrent_.getBitField().isNull())
                            sendBitfieldMessage();
						*/
                        break;
                    }
                    
                    case EPeerPwConnected:
                    {
                    	break;
                    }
                }
            }
            catch(InterruptedIOException e)
            {
                close(EIncreaseErrorCounter, "Read error");
                Log.v(LOG_TAG, "--- PeerConnection interrupted exception: " + e.getMessage());
            }
            catch(IOException e)
            {
                e.printStackTrace();
                close(EIncreaseErrorCounter, "Read error");
                Log.v(LOG_TAG, "--- PeerConnection ioexception: " + e.getMessage());
            }
            catch(Exception e)
            {
                e.printStackTrace();
                close(EIncreaseErrorCounter, "Read error - CHECKOLNI!!!");
                Log.v(LOG_TAG, "[Read Exception] " + e.getMessage());
            }
        }
    }
	
	public void sendHandshakeMessage() {
        if(torrent_ != null) {
            Log.v(LOG_TAG, "Sending handshake to: " + peer_.getAddress());
            ByteArrayOutputStream baos = null;
            try {
                if (outputStream_ != null) {
                    byte[] zeros = {0, 0, 0, 0, 0, 0, 0, 0};

                    baos = new ByteArrayOutputStream();
                    baos.write((byte) ProtocolId.length());					// pstrlen:   string length of <pstr>, as a single raw byte 
                    baos.write(ProtocolId.getBytes());						// pstr:      string identifier of the protocol 
                    baos.write(zeros);										// reserved:  eight (8) reserved bytes. All current implementations use all zeroes.
                    baos.write(torrent_.getInfoHashByteArray());			// info_hash: 20-byte SHA1 hash of the info key in the metainfo file.
                    baos.write(torrentManager_.getPeerID().getBytes());		// peer_id:   20-byte string used as a unique ID for the client.
                    outputStream_.write(baos.toByteArray());
                    outputStream_.flush();

                    Log.v(LOG_TAG, "Handshake sent to: " + peer_.getAddress());
                } else {
                    close("Error while sending handshake, outputstream is NULL");
                }
            } catch(IOException ex) {
            	Log.v(LOG_TAG, "Error while sending handshake");
                close(EIncreaseErrorCounter, "Error while writing");
            } catch(Exception ex) {
            	Log.v(LOG_TAG, "Error while sending handshake");
                close(EIncreaseErrorCounter, "Error while writing");
            } finally {
            	try {
					if (baos!= null) baos.close();
				} catch (IOException e) {}
            }
        } else Log.v(LOG_TAG, "Error: Torrent is not specified, cannot send handshake.");
    }
	
	
    public void close(String reason)
    {
        close(ENotSpecified, reason);
    }

    
    /** Closes the socket connection. */
    public void close(int order, String reason) {
        Log.v(LOG_TAG, "Closing connection. Reason: " + reason);
        if(state_ != EPeerClosing) {
            //closeOrder = order;

            // stop receiving
            readEnabled_ = false;

            state_ = EPeerClosing;
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
