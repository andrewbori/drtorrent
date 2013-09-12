package hu.bute.daai.amorg.drtorrent.core.peer;

import hu.bute.daai.amorg.drtorrent.core.Block;

import java.net.Socket;

public interface PeerConnection {
	
	public final static int STATE_NOT_CONNECTED  = 0;
    public final static int STATE_TCP_CONNECTING = 1;
    public final static int STATE_TCP_CONNECTED  = 2;
    public final static int STATE_PW_HANDSHAKING = 3;
    public final static int STATE_PW_CONNECTED   = 4;
    public final static int STATE_CLOSING        = 5;
    
    public final static int ERROR_DELETE_PEER = 0;
    public final static int ERROR_INCREASE_ERROR_COUNTER = 1;
    public final static int ERROR_NOT_SPECIFIED = 2;
    public final static int ERROR_PEER_STOPPED = 3;
	
	/** Connects to the peer (establishes the peer wire connection). */
	public void connect();
	
	/** Connects to the peer (incoming connection). */
	public void connect(Socket socket);
	
	/** Closes the socket connection. */
	public void close(String reason);
	
	/** Closes the socket connection. */
	public void close(int errorCode, String reason);
	
	/** Schedules the connection. Mainly for checking timeouts. */
	public void onTimer();
	
	/** Issues the download requests. */
	public void issueDownload();
	
	/** Issues the upload requests. */
	public boolean issueUpload(Block block);
	
	/** Sending message: cancel. */
	public void sendCancelMessage(Block block);
	
	/** Sending message: have. */
	public void sendHaveMessage(int pieceIndex);
	
	/** Sets our chocking state. */
	public void setChoking(final boolean choking);
	
	/** Returns whether we are chocking the peer or not. */
	public boolean isChoking();

	/** Returns whether the peer is chocking us or not. */
	public boolean isPeerChoking();
	
	/** Returns whether the client is connected or not. */
	public boolean isConnected();
	
	/** Returns the connections state. */
	public int getState();
	
	/** Returns whether the metadata block is rejected or not. */
	public boolean isMetadataBlockRejected(final int index);
}
