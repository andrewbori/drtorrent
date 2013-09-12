package hu.bute.daai.amorg.drtorrent.core.torrent;


import java.util.ArrayList;

public interface TorrentManagerObserver {
	public static final int MESSAGE_ERROR 						  = 10;
	public static final int MESSAGE_NOT_A_TORRENT_FILE 			  = 11;
	public static final int MESSAGE_WRONG_FILE 				      = 12;
	public static final int MESSAGE_WRONG_MAGNET_LINK 			  = 13;
	public static final int MESSAGE_READING_THE_TORRENT_FILE 	  = 14;
	public static final int MESSAGE_DOWNLOADING_THE_TORRENT  	  = 15;
	public static final int MESSAGE_NOT_ENOUGH_FREE_SPACE 		  = 16;
	public static final int MESSAGE_THE_TORRENT_IS_ALREADY_OPENED = 17;
	
	public static final int MESSAGE_INACTIVE 					  = 20;
	
	public final static int UPDATE_INFO		    =  1;	// 0b00000001
	public final static int UPDATE_FILE_LIST    =  2;	// 0b00000010
	public final static int UPDATE_BITFIELD     =  4;	// 0b00000100
	public final static int UPDATE_PEER_LIST    =  8;	// 0b00001000
	public final static int UPDATE_TRACKER_LIST = 16;	// 0b00010000
	public final static int UPDATE_SOMETHING 	= 31;	// 0b00011111
	
	/** Show Notification. */
	public abstract void showNotification(long downloadSpeed, long uploadSpeed);
	
	/** Show Notification. */
	public abstract void showNotification(int messageId);

	/** Shows a new notificaion for download completed. */
	public abstract void showCompletedNotification(String torrentName);
	
	/** Saves the state of the manager. */
	public abstract void saveState(String state);
	
	/** Loads the saved state of the manager. */
	public abstract String loadState();
	
	/** Saves the content of a torrent file. */
	public abstract void saveTorrentContent(String infoHash, byte[] content);
	
	/** Removes the torrent file. */
	public abstract void removeTorrentContent(String infoHash);
	
	/** Loads the content of a torrent file. */
	public abstract byte[] loadTorrentContent(String infoHash);

	/** Shows the torrent's settings dialog. */
	public abstract void showTorrentSettings(TorrentInfo torrent);
	
	/** Torrent changed. */
	public abstract void updateTorrentItem(TorrentInfo torrent);
	
	/** Torrent deleted. */
	public abstract void removeTorrentItem(TorrentInfo torrent);
	
	/** FileList changed. */
	public abstract void updateFileList(TorrentInfo torrent);
	
	/** Peer list changed. */
	public abstract void updatePeerList(TorrentInfo torrent);
	
	/** Peer list changed. */
	public abstract void updateTrackerList(TorrentInfo torrent);
	
	/** Bitfield changed. */
	public abstract void updateBitfield(TorrentInfo torrent);
	
	/** Shows a dialog with a message id. */
	public abstract void showDialog(int messageId);
	
	/** Shows the already opened dialog. */
	public abstract void showAlreadyOpenedDialog(String infoHash, ArrayList<String> trackerUrls);
	
	/** Shows a progress dialog with a message id. */
	public abstract void showProgress(int messageId);
	
	/** Hides the progress dialog. */
	public abstract void hideProgress();
	
	/** Updates the metadata of the torrent. (torrent name & file list) */
	public abstract void updateMetadata(TorrentInfo torrent);
	
	/** Returns whether the given field should be updated or not. <br>
	 *  <br>
	 *  MSG_UPDATE_INFO		    =  1;	// 0b00000001 <br>
	 *  MSG_UPDATE_FILE_LIST    =  2;	// 0b00000010 <br>
	 *	MSG_UPDATE_BITFIELD     =  4;	// 0b00000100 <br>
	 *	MSG_UPDATE_PEER_LIST    =  8;	// 0b00001000 <br>
	 *	MSG_UPDATE_TRACKER_LIST = 16;	// 0b00010000 <br>
	 *	MSG_UPDATE_SOMETHING	= 31;	// 0b00011111 <br>
	 * 
	 */
	public abstract boolean shouldUpdate(int updateField);
}
