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
		
		try {
			Log.v(LOG_TAG, "Connecting to: " + destination);
			socket_ = new Socket(peer_.getAddress(), peer_.getPort());
			socket_.setSoTimeout(5000);
			Log.v(LOG_TAG, "Connected to: " + destination);
		} catch (IOException e) {
			e.printStackTrace();
			Log.v(LOG_TAG, "Failed to connect to: " + destination);
		}
		
		if (socket_ != null) startDownloading();
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

                        if(protLength == -1) // -1 error from the stream: connection closed
                        {
                            //close(EIncreaseErrorCounter, "Peer disconnected!");
                            continue;
                        }

                        int handshakeLength = protLength + 48; // -1 because of protLength

                        byte[] handshake = new byte[handshakeLength];
                        ////inputStream.read(handshake);
                        readData(handshake);

                        byte[] otherProtocolId = new byte[protLength];
                        System.arraycopy(handshake, 0, otherProtocolId, 0, protLength);

                        if(!ProtocolId.equals(new String(otherProtocolId)))
                        {
                            //close(EDeletePeer, "Protocol identifier doesn't match!");
                            continue;
                        }

                        byte[] infoHash = new byte[20];
                        System.arraycopy(handshake, 27, infoHash, 0, 20);

                        if(torrent_ != null)
                        {
                            /*if(!NetTools.byteArrayEqual(infoHash, torrent_.getInfoHashByteArray()))
                            {
                                close(EDeletePeer, "Torrent infohash doesn't match!");
                                continue;
                            }*/
                        }
                        else // if torrent is null then we should attach peer to torrent (most likely it is an incoming connection)
                        {
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
                        if(peer_.getPeerId() != null)
                        {
                            if(!peer_.getPeerId().equals(tempPId))
                            {
                                //close(EIncreaseErrorCounter, "Peer ID doesn't match!");
                                continue;
                            }
                            else if(tempPId.equals(torrentManager_.getPeerID()))
                            {
                                //close(EDeletePeer, "Connected to ourselves!");
                                continue;
                            }
                        }
                        else
                        {
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
                //close(EIncreaseErrorCounter, "Read error");
                Log.v(LOG_TAG, "--- PeerConnection interrupted exception: " + e.getMessage());
            }
            catch(IOException e)
            {
                e.printStackTrace();
                //close(EIncreaseErrorCounter, "Read error");
                Log.v(LOG_TAG, "--- PeerConnection ioexception: " + e.getMessage());
            }
            catch(Exception e)
            {
                e.printStackTrace();
                //close(EIncreaseErrorCounter, "Read error - CHECKOLNI!!!");
                Log.v(LOG_TAG, "[Read Exception] " + e.getMessage());
            }
        }
    }
	
	public void sendHandshakeMessage() {
        if(torrent_ != null) {
            Log.v(LOG_TAG, "Sending handshake to: " + peer_.getAddress());
            try
            {
                if(outputStream_ != null) {
                    byte[] zeros = {0, 0, 0, 0, 0, 0, 0, 0};

                    ByteArrayOutputStream bs = new ByteArrayOutputStream();
                    bs.write((byte) ProtocolId.length());
                    bs.write(ProtocolId.getBytes());
                    bs.write(zeros);
                    bs.write(torrent_.getInfoHashByteArray());
                    bs.write(torrentManager_.getPeerID().getBytes());

                    outputStream_.write(bs.toByteArray());
                    outputStream_.flush();
                	
                    /*byte[] zeros = {0,0,0,0,0,0,0,0};
                    
                    String handShake = (char)ProtocolId.length() + new String(ProtocolId.getBytes()) +
                            new String(zeros) + new String(torrent.getInfoHashByteArray()) + new String(torrentMgr.getPeerID().getBytes());                    
                    
                    outputStream.write(handShake.getBytes());*/

                    outputStream_.flush();

                    Log.v(LOG_TAG, "Handshake sent to: " + peer_.getAddress());
                } else {
                    //close("ERROR, while send handshake, outputstream is NULL");
                }
            }
            catch(IOException ex)
            {
            	Log.v(LOG_TAG, "ERROR, while send handshake");
                //close(EIncreaseErrorCounter, "Error while writing");
            }
            catch(Exception ex)
            {
            	Log.v(LOG_TAG, "ERROR, while send handshake");
                //close(EIncreaseErrorCounter, "Error while writing");
            }
        }
        else
        	Log.v(LOG_TAG, "ERROR, torrent is not specified, cannot send handshake");
    }
	
}
