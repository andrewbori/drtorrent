package hu.bute.daai.amorg.drtorrent.service;

import android.os.Messenger;

public class TorrentClient {
	public final static int CLIENT_TYPE_ALL    		   = 1;
	public final static int CLIENT_TYPE_SINGLE 		   = 2;
	public final static int CLIENT_TYPE_ALL_AND_SINGLE = 3;
	public final static int CLIENT_TYPE_SETTINGS	   = 4;
	
	private int id_;
	private Messenger messenger_ = null;
	private int clientType_ = 1;
	private int torrentId_ = -1;
	private int updateField_ = 0;
	
	public TorrentClient(int id, Messenger messenger, int clientType, int torrentId) {
		id_ = id;
		messenger_ = messenger;
		clientType_ = clientType;
		torrentId_ = torrentId;
	}
	
	public boolean isTypeAll() {
		return (clientType_ == CLIENT_TYPE_ALL || clientType_ == CLIENT_TYPE_ALL_AND_SINGLE);
	}
	
	public boolean isTypeSingle(int torrentId) {
		return (torrentId_ == torrentId &&
				(clientType_ == CLIENT_TYPE_SINGLE || 
				 clientType_ == CLIENT_TYPE_ALL_AND_SINGLE ||
				 clientType_ == CLIENT_TYPE_SETTINGS));
	}
	
	public boolean isTypeSettings(int torrentId) {
		return torrentId_ == torrentId && clientType_ == CLIENT_TYPE_SETTINGS;
	}
	
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
	public boolean shouldUpdate(int updateField) {
		return (updateField == TorrentService.UPDATE_SOMETHING && isTypeAll()) || ((updateField_ & updateField) != 0);
	}
	
	public Messenger getMessenger() {
		return messenger_;
	}
	
	public void setTorrentId(int torrentId) {
		torrentId_ = torrentId;
	}

	public void setUpdateField(int updateField) {
		updateField_ = updateField;
	}
	
	public boolean hasId(int id) {
		return id_ == id;
	}
	
	private static int id = 0;
	public static int generateId() {
		return id++;
	}
}
