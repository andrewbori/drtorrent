package hu.bute.daai.amorg.drtorrent.core.tracker;

/** Represents the tracker observer interface of torrents. */
public interface TrackerObserver {

	/** Adds a new peer to the list of the peers of the torrent. <br> 
	 * (Used by the Torrent Manager & trackers.) */
	public void addPeer(final String address, final int port, final String peerId);
	
	/** Refreshes the count of leechers & seeds. */
	public void refreshSeedsAndLeechersCount();
	
	/** Returns the info hash as a string. */
	public String getInfoHash();
	
	/** Returns the info hash. */
	public byte[] getInfoHashByteArray();
	
	/** Returns the number of bytes have to be downloaded. */
	public long getBytesLeft();
	
	/** Returns the number of downloaded bytes. */
	public long getBytesDownloaded();

	/** Returns the number of uploaded bytes. */
	public long getBytesUploaded();
}
