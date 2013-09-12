package hu.bute.daai.amorg.drtorrent.core.peer;

import hu.bute.daai.amorg.drtorrent.core.Block;
import hu.bute.daai.amorg.drtorrent.core.torrent.Torrent;

import java.net.Socket;

/** Represents a manageable interface of the peer. */
public interface Peer extends PeerInfo {
	
	/** Returns whether we can reconnect to the previously disconnected peer or not. */
	public boolean canConnect();
	
	/** Connects to the peer. */
	public void connect(Torrent torrent);
	
	/** Connects to the peer. (incoming connection) */
	public void connect(Socket socket);
	
	/** Sets the torrent which this peer is sharing. */
	public void setTorrent(Torrent torrent);
	
	/** Disconnects the peer. */
	public void disconnect();
	
	/** Schedules the connection. */
	public void onTimer();
	
	/** Notifies the peer that the client has the given piece. */
	public void notifyThatClientHavePiece(int pieceIndex);
	
	/** Sets our chocking state. */
	public void setChoking(boolean choking);
	
	/** Resets the timeout count of the TCP connection.  */
	public void resetTcpTimeoutCount();
	
	/** Returns the timeout of the TCP connection. */
	public int getTcpTimeoutCount();
	
	public boolean isMetadataBlockRejected(int index);
	
	/** Called when the hash of a piece downloaded by the peer was checked. */
	public void onPieceHashChecked(boolean isCorrect);
	
	/** Returns whether the peer has the given piece or not. */
	public boolean hasPiece(int index);
	
	/** Returns whether the peer has requested the block or not. */
	public boolean hasBlock(Block block);
	
	/** Cancels the request of the given block. */
	public void cancelBlock(final Block block);
	
	/** Called when a new block was uploaded. */
	public boolean issueUpload(Block block);
}
