package hu.bute.daai.amorg.drtorrent.core.peer;

/** Represents a readable interface of the state of the peer. */
public interface PeerInfo {
	
	/** Returns the peers ID. Only used inside this program... */
	public int getId();
	
	/** Returns the client's name. */
	public String getClientName();
	
	/** Returns the address of the peer. */
	public String getAddress();

	/** Returns the port of the peer. */
	public int getPort();
	
	/** Returns the count of downloaded bytes. */
	public long getDownloaded();
	
	/** Returns the download speed. */
	public int getDownloadSpeed();
	
	/** Returns the count of uploaded bytes. */
	public long getUploaded();
	
	/** Returns the upload speed. */
	public int getUploadSpeed();
	
	/** Returns the percent of the peer's download progress. */
	public int getPercent();
	
	/** Returns the state of the connection of the peer. */
	public int getState();
	
	/** Returns whether we are choked (the peer is choking us) or not. */ 
	public boolean isChoked();
	
	/** Returns whether the peer is choked (we are choking the peer) or not. */
	public boolean isPeerChocked();
}
