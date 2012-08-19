package hu.bute.daai.amorg.drtorrent.torrentengine;

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

import android.net.Uri;
import android.util.Log;

/** Class managing the opened torrents. */
public class TorrentManager {
	private final static String LOG_TAG = "TorrentManager";
	
	public static String DEFAULT_DOWNLOAD_PATH = "/sdcard/Downloads/";
	
	private TorrentService torrentService_;
	private NetworkManager networkManager_;
	private TorrentSchedulerThread torrentSchedulerThread_;
	private boolean updateEnabled_;
	private boolean schedulerEnabled_;
	private Vector<Torrent> torrents_;
	private Vector<Torrent> openingTorrents_;
	
	private Vector<Peer> incomingPeers_;
	
	private static String peerId_;
	private static int peerKey_;
	
	/** Thread that schedules the torrents. */
	class TorrentSchedulerThread extends Thread {
		@Override
		public void run() {
			while (schedulerEnabled_) {
				if (updateEnabled_) {
					Log.v(LOG_TAG, "Scheduling: " + System.currentTimeMillis());
					Torrent torrent = null;
					for (int i = 0; i < torrents_.size(); i++) {
						torrent = torrents_.elementAt(i);
						if (torrent.isWorking()) {
							torrent.onTimer();
							
							if (torrentService_.shouldUpdate(TorrentService.UPDATE_INFO)) torrentService_.updateTorrentItem(torrent);
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
					}
					updateEnabled_ = false;
				}
				try {
					sleep(2000);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	/** Constructor of the Torrent Manager with its Torrent Service as a parameter. */
	public TorrentManager(TorrentService torrentService) {
		torrentService_ = torrentService;
		torrents_ = new Vector<Torrent>();
		openingTorrents_ = new Vector<Torrent>();
		incomingPeers_ = new Vector<Peer>();
		generatePeerId();
		
		schedulerEnabled_ = true;
		updateEnabled_ = false;
		torrentSchedulerThread_ = new TorrentSchedulerThread();
		torrentSchedulerThread_.start();
		
		networkManager_ = new NetworkManager(this);
		networkManager_.startListening();
	}
	
	/** Shuts down the torrent manager. */
	public void shutDown() {
		schedulerEnabled_ = false;
		networkManager_.stopListening();
		
		for (int i = 0; i < torrents_.size(); i++) {
			Torrent torrent = torrents_.get(i);
			torrent.stop();
			torrents_.removeElementAt(i);
			i--;
		}
	}

	public void addIncomingConnection(Socket socket) {
		Peer peer = new Peer(socket, this);
		peer.connect(socket);
		//incomingPeers_.addElement(peer);
	}
	
	public boolean attachPeerToTorrent(String infoHash, Peer peer) {
		Torrent torrent = getTorrent2(infoHash);
		if (torrent != null) {
			peer.setTorrent(torrent);
			torrent.addIncomingPeer(peer);
			return true;
		} 
		return false;
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
			openingTorrents_.add(newTorrent);
			showTorrentSettings(newTorrent);
			//newTorrent.start();
		} else if (result == Torrent.ERROR_WRONG_CONTENT) {					// Error.
			showDialog("Wrong file format!");
		} else if (result == Torrent.ERROR_ALREADY_EXISTS) {
			showDialog("The torrent is already opened!");
		} else if (result == Torrent.ERROR_NO_FREE_SIZE) {
			showDialog("There is not enough free space on the SD card.");
		}
	}
	
	public void setTorrentSettings(TorrentListItem item, ArrayList<FileListItem> fileList) { 
		for (int i = 0; i < openingTorrents_.size(); i++) {
			Torrent torrent = openingTorrents_.elementAt(i);
			if (torrent.getInfoHash().equals(item.getInfoHash())) {
				Vector<File> files = torrent.getFiles();
				if (fileList.size() == files.size()) {
					for (int j = 0; j < fileList.size(); j++) {
						files.get(j).setPriority(fileList.get(j).getPriority());
					}
					openingTorrents_.remove(torrent);
					addTorrent(torrent);
					updateTorrent(torrent);
					
					torrent.setTorrentActivePieces();
					torrent.checkHash();
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
		String infoHash = torrent.getInfoHash();
		boolean found = false;
		for (int i = 0; i < torrents_.size(); i++) {
			if (torrents_.get(i).getInfoHash().equals(infoHash)) {
				found = true;
				i = torrents_.size();
			}
		}
		
		if (!found) {
			torrents_.add(torrent);
			return true;
		}
		
		return false;
	}
	
	/** Starts a torrent. */
	public void startTorrent(String infoHash) {
		Torrent torrent = getTorrent(infoHash);
		if (torrent != null) torrent.start();
	}
	
	/** Stops a torrent. */
	public void stopTorrent(String infoHash) {
		Torrent torrent = getTorrent(infoHash);
		if (torrent != null) torrent.stop();
	}
	
	/** Closes a torrent. */
	public void closeTorrent(String infoHash) {
		Torrent torrent = getTorrent(infoHash);
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
	
	/** Returns whether the manager the given torrent already has. */
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
	
	/** Returns whether the manager the given torrent already has. */
	public Torrent getTorrent2(String infoHash) {
		Torrent tempTorrent;
		String tempInfoHash;
		for (int i = 0; i < torrents_.size(); i++) {
			tempTorrent = (Torrent) torrents_.elementAt(i);
			tempInfoHash = tempTorrent.getInfoHashString();
			if (tempInfoHash != null && tempInfoHash.equals(infoHash)) {
				return tempTorrent;
			}
		}

		return null;
	}
	
	public void changeFilePriority(String infoHash, FileListItem item) {
		Torrent torrent = getTorrent(infoHash);
		if (torrent != null ) torrent.changeFilePriority(item.getIndex(), item.getPriority());
	}
	
	public void showTorrentSettings(Torrent torrent) {
		torrentService_.showTorrentSettings(torrent);
	}
	
	/** Updates the torrent item. */
	public void updateTorrent(Torrent torrent) {
		if (torrents_.contains(torrent)) torrentService_.updateTorrentItem(torrent);
	}
	
	/** Updates the peer item. */
	public void updatePeer(Torrent torrent, Peer peer) {
		updatePeer(torrent, peer, false);
	}
	
	/** Updates the peer item. */
	public void updatePeer(Torrent torrent, Peer peer, boolean isDisconnected) {
		torrentService_.updatePeerItem(torrent, peer, isDisconnected);
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
        peerId_ = "-DR0001-";
        for (int i=0; i<12; i++) {
            peerId_ += (char)(Math.abs(r.nextInt()) % 25 + 97); // random lower case alphabetic characters ('a' - 'z')
        }
        
        Log.v(LOG_TAG, "PeerID: " + peerId_);
        Log.v(LOG_TAG, "PeerKey: " + peerKey_);
	}


	public void update() {
		updateEnabled_ = true;
		torrentSchedulerThread_.interrupt();
	}
}
