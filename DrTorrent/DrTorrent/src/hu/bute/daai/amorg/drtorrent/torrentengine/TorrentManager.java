package hu.bute.daai.amorg.drtorrent.torrentengine;

import hu.bute.daai.amorg.drtorrent.coding.bencode.Bencoded;
import hu.bute.daai.amorg.drtorrent.file.FileManager;
import hu.bute.daai.amorg.drtorrent.service.TorrentService;

import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import android.util.Log;

/** Class managing the opened torrents. */
public class TorrentManager {
	private final static String LOG_TAG = "TorrentManager";
	
	public static String DEFAULT_DOWNLOAD_PATH = "/sdcard/Downloads/";
	
	private TorrentService torrentService_;
	private TorrentSchedulerTask torrentSchedulerTask_;
	private boolean schedulerEnabled_;
	private Vector<Torrent> torrents_;
	
	private static String peerId_;
	private static int peerKey_;
	
	/** TimerTask that schedules the torrents. */
	class TorrentSchedulerTask extends TimerTask {

		@Override
		public void run() {
			if (schedulerEnabled_) {
				//Log.v(LOG_TAG, "Scheduling: " + System.currentTimeMillis());
				Torrent torrent = null;
				for (int i = 0; i < torrents_.size(); i++) {
					torrent = torrents_.elementAt(i);
					if (torrent.isWorking()) {
						torrent.onTimer();
						
						torrentService_.updatePeerList(torrent);
						if (torrent.isBitfieldChanged()) {
							torrentService_.updateBitfield(torrent);
							torrent.setBitfieldChanged(false);
						}
					}
				}
			}
		}
	}

	/** Constructor of the Torrent Manager with its Torrent Service as a parameter. */
	public TorrentManager(TorrentService torrentService) {
		torrentService_ = torrentService;
		torrents_ = new Vector<Torrent>();
		generatePeerId();
		
		schedulerEnabled_ = true;
		torrentSchedulerTask_ = new TorrentSchedulerTask();
		Timer timer = new Timer();
		timer.schedule(torrentSchedulerTask_, 0, 1000);
	}

	/** Opens a new Torrent file with the given file path. */
	public void openTorrent(String filePath) {
		openTorrent(filePath, DEFAULT_DOWNLOAD_PATH);
	}
	
	/**
	 * Opens and starts a new Torrent file with the given file path.
	 * Also defines its download folder.   
	 */
	public void openTorrent(String filePath, String downloadPath) {
		showProgress("Reading the torrent file...");
		Torrent newTorrent = new Torrent(this, filePath, downloadPath);
		
		Log.v(LOG_TAG, "Reading the file: " + filePath);
		byte[] fileContent = FileManager.readFile(filePath);

		if (fileContent == null) {
			hideProgress();
			showDialog("Not a torrent file!");
		}

		Log.v(LOG_TAG, "Bencoding the file: " + filePath);
		Bencoded bencoded = Bencoded.parse(fileContent);
		Log.v(LOG_TAG, "Bencoding over.");
		if (bencoded == null) {
			hideProgress();
			showDialog("Not a torrent file!");
		}
		
		int result = newTorrent.processBencodedTorrent(bencoded);
		if (result == Torrent.ERROR_NONE) {									// No error.
			newTorrent.start();
		} else if (result == Torrent.ERROR_WRONG_CONTENT) {					// Error.
			showDialog("Wrong file format!");
		} else if (result == Torrent.ERROR_ALREADY_EXISTS) {
			showDialog("The torrent is already opened!");
		} else if (result == Torrent.ERROR_NO_FREE_SIZE) {
			showDialog("There is not enough free space on the SD card.");
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
}
