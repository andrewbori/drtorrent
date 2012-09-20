package hu.bute.daai.amorg.drtorrent.torrentengine;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.adapter.item.FileListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.TorrentListItem;
import hu.bute.daai.amorg.drtorrent.coding.bencode.Bencoded;
import hu.bute.daai.amorg.drtorrent.file.FileManager;
import hu.bute.daai.amorg.drtorrent.network.HttpConnection;
import hu.bute.daai.amorg.drtorrent.network.NetworkManager;
import hu.bute.daai.amorg.drtorrent.service.TorrentService;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

/** Class managing the opened torrents. */
public class TorrentManager {
	private final static String LOG_TAG = "TorrentManager";
	
	public static String DEFAULT_DOWNLOAD_PATH = Environment.getExternalStorageDirectory().getPath() + "/Downloads/";
	
	private TorrentService torrentService_;
	private NetworkManager networkManager_;
	private TorrentSchedulerThread torrentSchedulerThread_;
	private boolean updateEnabled_ = false;
	private Vector<Torrent> torrents_;
	private Vector<Torrent> openingTorrents_;
	
	private static String peerId_;
	private static int peerKey_;
	
	private boolean isSchedulerEnabled_ = true;
	
	/** Thread that schedules the torrents. */
	class TorrentSchedulerThread extends Thread {
		@Override
		public void run() {
			startUp();
			while (isSchedulerEnabled_) {
				if (updateEnabled_) {
					Log.v(LOG_TAG, "Scheduling: " + SystemClock.elapsedRealtime());
					Torrent torrent = null;
					for (int i = 0; i < torrents_.size(); i++) {
						torrent = torrents_.elementAt(i);
						if (torrent.isWorking()) {
							torrent.onTimer();
						}
						updateTorrent(torrent);
					}
					updateEnabled_ = false;
				}
				try {
					sleep(2000);
				} catch (InterruptedException e) {}
			}
		}
	}
	
	/** Updates the torrent: info, file list, bitfield, peer list, tracker list. */
	public void updateTorrent(Torrent torrent) {
		if (torrentService_.shouldUpdate(TorrentService.UPDATE_SOMETING)) torrentService_.updateTorrentItem(torrent);
		if (torrentService_.shouldUpdate(TorrentService.UPDATE_FILE_LIST)) torrentService_.updateFileList(torrent);
		if (torrentService_.shouldUpdate(TorrentService.UPDATE_PEER_LIST)) torrentService_.updatePeerList(torrent);
		if (torrentService_.shouldUpdate(TorrentService.UPDATE_TRACKER_LIST)) torrentService_.updateTrackerList(torrent);
		if (torrentService_.shouldUpdate(TorrentService.UPDATE_BITFIELD)) {
			if (torrent.isBitfieldChanged()) {
				torrentService_.updateBitfield(torrent);
				torrent.setBitfieldChanged(false);
			}
		}
	}

	/** Constructor of the Torrent Manager with its Torrent Service as a parameter. */
	public TorrentManager(TorrentService torrentService) {
		torrentService_ = torrentService;
		torrents_ = new Vector<Torrent>();
		openingTorrents_ = new Vector<Torrent>();
		generatePeerId();
		
		networkManager_ = new NetworkManager(this);
		torrentSchedulerThread_ = new TorrentSchedulerThread();
		torrentSchedulerThread_.start();
		updateEnabled_ = false;
	}
	
	/** Enables the Torrent Manager. */
	public void enable() {
		networkManager_.startListening();
		
		for (int i = 0; i < torrents_.size(); i++) {
			Torrent torrent = torrents_.elementAt(i);
			if (torrent.isWorking()) {
				torrent.resume();
			}
		}
	}
	
	/** Disables the Torrent Manager. */
	public void disable() {
		networkManager_.stopListening();
	}
	
	/** Starts the torrent manager, loads its previously saved state. */
	public void startUp() {
		JSONArray jsonArray = new JSONArray();
		try {
			String content = torrentService_.loadState();
			Log.v(LOG_TAG, content);
			jsonArray = new JSONArray(content);
		} catch (Exception e) {
			Log.v(LOG_TAG, "Error while startup: " + e.getMessage());
		}
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject json = null;
			String infoHash = "";
			try {
				json = jsonArray.getJSONObject(i);
				infoHash = json.getString("InfoHash");
			} catch (JSONException e) {
				Log.v(LOG_TAG, e.getMessage());
			}
			Log.v(LOG_TAG, infoHash);
			openTorrent(infoHash, json);
		}
	}
	
	/** Shuts down the torrent manager, saves its current state. */
	public void shutDown() {
		isSchedulerEnabled_ = false;
		networkManager_.stopListening();
		
		JSONArray jsonArray = new JSONArray();
		for (int i = 0; i < torrents_.size(); i++) {
			Torrent torrent = torrents_.get(i);
			jsonArray.put(torrent.getJSON());
			torrent.stop();
			
			torrents_.removeElementAt(i);
			i--;
		}
		torrentService_.saveState(jsonArray.toString());
	}
	
	/** Connects to an incoming connection. */
	public void addIncomingConnection(Socket socket) {
		Peer peer = new Peer(socket, this);
		peer.connect(socket);
	}
	
	/** Attaches a peer to a torrent. */
	public boolean attachPeerToTorrent(String infoHash, Peer peer) {
		Torrent torrent = getTorrent2(infoHash);
		if (torrent != null && torrent.isConnected()) {
			peer.setTorrent(torrent);
			torrent.addIncomingPeer(peer);
			return true;
		} 
		return false;
	}
	
	/** Opens saved torrent. */
	public void openTorrent(String infoHash, JSONObject json) {
		byte[] torrentContent = torrentService_.loadTorrentContent(infoHash);
		
		if (torrentContent == null) return;

		Log.v(LOG_TAG, "Bencoding saved torrent: " + infoHash);
		Bencoded bencoded = null;
		try {
			bencoded = Bencoded.parse(torrentContent);
		} catch (Exception e) {
			Log.v(LOG_TAG, "Error occured while processing the saved torrent file: " + e.getMessage());
			return;
		}
		
		final Torrent newTorrent = new Torrent(this, infoHash, DEFAULT_DOWNLOAD_PATH);
		int result = newTorrent.processBencodedTorrent(bencoded);
		if (result == Torrent.ERROR_NONE) {								// No error.
			if (newTorrent.setJSON(json)) {
				addTorrent(newTorrent);
				torrentService_.updateTorrentItem(newTorrent);
				if (newTorrent.getStatus() != R.string.status_stopped) {
					new Thread() {
						public void run() {
							newTorrent.start();
						};
					}.start();
				}
			}
		} else {
			Log.v(LOG_TAG, "Error occured while processing the saved torrent file");
		}
	}
	
	/** Opens a new Torrent file with the given file path. */
	public void openTorrent(Uri torrentUri) {
		openTorrent(torrentUri, DEFAULT_DOWNLOAD_PATH);
	}
	
	/**
	 * Opens and starts a new Torrent file with the given file path.
	 * Also defines its download folder.   
	 */
	public void openTorrent(Uri torrentUri, String downloadPath) {
		
		byte[] torrentContent = null;
		String path = "";
		
		showProgress("Reading the torrent file...");
		
		if (torrentUri.getScheme().equalsIgnoreCase("file")) {
			path = torrentUri.getPath();
			Log.v(LOG_TAG, "Reading the torrent: " + path);
			torrentContent = FileManager.readFile(path);
		} else {
			path = "http://" + torrentUri.getHost() + torrentUri.getPath();
			Log.v(LOG_TAG, "Downloading the torrent: " + path);
			torrentContent = (new HttpConnection(path)).execute();
		}
		
		if (torrentContent == null) {
			hideProgress();
			showDialog("Not a torrent file!");
			return;
		}
		
		Log.v(LOG_TAG, "Bencoding the file: " + path);
		Bencoded bencoded = null;
		try {
			bencoded = Bencoded.parse(torrentContent);
		} catch (Exception e) {
			hideProgress();
			showDialog("Not a torrent file!");
			return;
		}
		
		if (bencoded == null) {
			hideProgress();
			showDialog("Not a torrent file!");
			return;
		}
		
		Torrent newTorrent = new Torrent(this, path, downloadPath);
		int result = newTorrent.processBencodedTorrent(bencoded);
		hideProgress();
		if (result == Torrent.ERROR_NONE) {									// No error.
			torrentService_.saveTorrentContent(newTorrent.getInfoHashString(), torrentContent);
			openingTorrents_.add(newTorrent);
			showTorrentSettings(newTorrent);
			//newTorrent.start();
		} else if (result == Torrent.ERROR_WRONG_CONTENT) {					// Error.
			showDialog("Wrong file format!");
		} else if (result == Torrent.ERROR_ALREADY_EXISTS) {
			showDialog("The torrent is already opened!");
		} else if (result == Torrent.ERROR_NO_FREE_SIZE) {
			showDialog("There is not enough free space on the SD card.");
		} else {
			showDialog("Error.");
		}
	}
	
	/** Shows the settings dialog of the torrent (file list). */
	public void showTorrentSettings(Torrent torrent) {
		torrentService_.showTorrentSettings(torrent);
	}
	
	/** Sets the settings of the torrent and starts it. */
	public void setTorrentSettings(TorrentListItem item, ArrayList<FileListItem> fileList) { 
		for (int i = 0; i < openingTorrents_.size(); i++) {
			Torrent torrent = openingTorrents_.elementAt(i);
			if (torrent.getId() == item.getId()) {
				Vector<File> files = torrent.getFiles();
				if (fileList.size() == files.size()) {
					for (int j = 0; j < fileList.size(); j++) {
						files.get(j).setPriority(fileList.get(j).getPriority());
					}
					openingTorrents_.remove(torrent);
					addTorrent(torrent);
					torrentService_.updateTorrentItem(torrent);
					
					torrent.start();
				} else {	
					openingTorrents_.remove(torrent);
				}
				break;
			}
		}
	}
	
	/** Adds a new torrent to the list of the managed torrents. */
	public boolean addTorrent(Torrent torrent) {
		if (!hasTorrent(torrent.getInfoHash())) {
			torrents_.add(torrent);
			return true;
		}
		return false;
	}
	
	/** Starts a torrent. */
	public void startTorrent(int torrentId) {
		Torrent torrent = getTorrent(torrentId);
		if (torrent != null) torrent.start();
	}
	
	/** Stops a torrent. */
	public void stopTorrent(int torrentId) {
		Torrent torrent = getTorrent(torrentId);
		if (torrent != null) {
			torrent.stop();
			torrentService_.updateTorrentItem(torrent);
		}
	}
	
	/** Closes a torrent. */
	public void closeTorrent(int torrentId) {
		Torrent torrent = getTorrent(torrentId);
		if (torrent != null) {
			torrents_.removeElement(torrent);
			torrentService_.removeTorrentItem(torrent);
			torrent.stop();
		}
	}

	/** Returns whether the manager the given torrent already has. */
	public boolean hasTorrent(String infoHash) {
		if (getTorrent(infoHash) != null)
			return true;
		
		return false;
	}
	
	/** Returns a torrent by its ID. */
	public Torrent getTorrent(int torrentId) {
		Torrent torrent;
		for (int i = 0; i < torrents_.size(); i++) {
			torrent = torrents_.elementAt(i);
			if (torrent.getId() == torrentId) {
				return torrent;
			}
		}
		return null;
	}
	
	/** Returns a torrent by its info hash. */
	public Torrent getTorrent(String infoHash) {
		Torrent tempTorrent;
		String tempInfoHash;
		for (int i = 0; i < torrents_.size(); i++) {
			tempTorrent = (Torrent) torrents_.elementAt(i);
			tempInfoHash = tempTorrent.getInfoHash();
			if (tempInfoHash != null && tempInfoHash.equals(infoHash)) {
				return tempTorrent;
			}
		}
		return null;
	}
	
	/** Returns a torrent by its info hash string. */
	public Torrent getTorrent2(String infoHashString) {
		Torrent tempTorrent;
		String tempInfoHash;
		for (int i = 0; i < torrents_.size(); i++) {
			tempTorrent = (Torrent) torrents_.elementAt(i);
			tempInfoHash = tempTorrent.getInfoHashString();
			if (tempInfoHash != null && tempInfoHash.equals(infoHashString)) {
				return tempTorrent;
			}
		}
		return null;
	}
	
	/** Changes the priority of a file of a torrent. */
	public void changeFilePriority(int torrentId, FileListItem item) {
		Torrent torrent = getTorrent(torrentId);
		if (torrent != null ) torrent.changeFilePriority(item.getIndex(), item.getPriority());
	}
	
	/** Shows a toast with a message. */
	public void showToast(String s) {
		torrentService_.showToast(s);
	}
	
	/** Shows a dialog with a message. */
	public void showDialog(String s) {
		torrentService_.showDialog(s);
	}
	
	/** Shows a progress dialog with a message. */
	public void showProgress(String s) {
		torrentService_.showProgress(s);
	}
	
	/** Hides the progress dialog. */
	public void hideProgress() {
		torrentService_.hideProgress();
	}
	
	/** Returns the ID of the peer. */
	public static String getPeerID() {
		return peerId_;
		//return "-DR0001-alkjfdcgjhsw";
	}
	
	/** Returns the key of the peer. */
	public static int getPeerKey() {
		return peerKey_;
	}
	
	/**Generates the ID and the key of the peer. */
	private void generatePeerId() {
		Date d = new Date();
        long seed = d.getTime();   
        Random r = new Random();
        r.setSeed(seed);
        
        peerKey_ = Math.abs(r.nextInt());
        peerId_ = "-DR0100-";
        for (int i=0; i<12; i++) {
            peerId_ += (char)(Math.abs(r.nextInt()) % 25 + 97); // random lower case alphabetic characters ('a' - 'z')
        }
        
        Log.v(LOG_TAG, "PeerID: " + peerId_);
        Log.v(LOG_TAG, "PeerKey: " + peerKey_);
	}

	/** Activates the scheduler thread to update the UI. */
	public void update() {
		updateEnabled_ = true;
		if (torrentSchedulerThread_ != null) torrentSchedulerThread_.interrupt();
	}
}
