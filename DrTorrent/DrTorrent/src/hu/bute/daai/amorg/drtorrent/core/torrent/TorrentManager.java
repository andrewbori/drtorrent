package hu.bute.daai.amorg.drtorrent.core.torrent;

import hu.bute.daai.amorg.drtorrent.core.DownloadManager;
import hu.bute.daai.amorg.drtorrent.core.File;
import hu.bute.daai.amorg.drtorrent.core.UploadManager;
import hu.bute.daai.amorg.drtorrent.core.exception.AlreadyExistsException;
import hu.bute.daai.amorg.drtorrent.core.exception.DrTorrentException;
import hu.bute.daai.amorg.drtorrent.core.exception.InvalidContentException;
import hu.bute.daai.amorg.drtorrent.core.exception.NotEnoughFreeSpaceException;
import hu.bute.daai.amorg.drtorrent.core.peer.Peer;
import hu.bute.daai.amorg.drtorrent.core.peer.PeerImpl;
import hu.bute.daai.amorg.drtorrent.core.tracker.TrackerInfo;
import hu.bute.daai.amorg.drtorrent.file.FileManager;
import hu.bute.daai.amorg.drtorrent.network.HttpConnection;
import hu.bute.daai.amorg.drtorrent.network.MagnetUri;
import hu.bute.daai.amorg.drtorrent.network.NetworkManager;
import hu.bute.daai.amorg.drtorrent.util.Log;
import hu.bute.daai.amorg.drtorrent.util.Preferences;
import hu.bute.daai.amorg.drtorrent.util.analytics.Analytics;
import hu.bute.daai.amorg.drtorrent.util.bencode.Bencoded;
import hu.bute.daai.amorg.drtorrent.util.bencode.BencodedException;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Random;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.os.SystemClock;

/** Class managing the opened torrents. */
public class TorrentManager {
	private final static String LOG_TAG = "TorrentManager";
	private final static int STATE_SAVING_INTERVAL = 1000 * 1 * 60;
	private final static int REPORT_INTERVAL = 30000 * 1 * 60;
	
	final private TorrentManagerObserver torrentService_;
	final private NetworkManager networkManager_;
	final private UploadManager uploadManager_;
	final private DownloadManager downloadManager_;
	final private TorrentSchedulerThread torrentSchedulerThread_;
	private boolean updateEnabled_ = false;
	final private Vector<Torrent> torrents_;
	final private Vector<Torrent> openingTorrents_;
	
	private static String peerId_;
	private static int peerKey_;
	
	private boolean isSchedulerEnabled_ = true;

	private boolean isEnabled_ = false;
	
	private long latestSaveTime_;
	private long latestReportTime_;

	static {
		generatePeerId();
	}
	
	/** Thread that schedules the torrents. */
	class TorrentSchedulerThread extends Thread {
		@Override
		public void run() {
			startUp();
			latestSaveTime_ = SystemClock.elapsedRealtime();
			latestReportTime_ = SystemClock.elapsedRealtime();
			while (isSchedulerEnabled_) {
				long currentTime = SystemClock.elapsedRealtime();
				if (updateEnabled_) {
					Log.v(LOG_TAG, "Scheduling: " + currentTime);
					
					uploadManager_.onTimer(currentTime);
					downloadManager_.onTimer(currentTime);
					
					Torrent torrent;
					for (int i = 0; i < torrents_.size(); i++) {
						torrent = torrents_.elementAt(i);
						if (torrent.isWorking()) {
							torrent.onTimer();
						}
						updateTorrent(torrent);
					}
					for (int i = 0; i < openingTorrents_.size(); i++) {
						torrent = openingTorrents_.elementAt(i);
						if (torrent.isWorking()) {
							torrent.onTimer();
						}
					}
					updateEnabled_ = false;
					
					if (isEnabled_) {
						torrentService_.showNotification(downloadManager_.getDownloadSpeed(), uploadManager_.getUploadSpeed());
					}
				}
				
				if (latestSaveTime_ + STATE_SAVING_INTERVAL < currentTime) {
					latestSaveTime_ = currentTime;
					saveState();
					
					if (latestReportTime_ + REPORT_INTERVAL < currentTime  && networkManager_.hasConnection()) {
						latestReportTime_ = currentTime;
						
						if (Preferences.isAnalyticsEnabled()) {
							final ArrayList<TorrentInfo> analyticsTorrents = new ArrayList<TorrentInfo>();
							analyticsTorrents.addAll(torrents_);
							analyticsTorrents.addAll(openingTorrents_);
							Analytics.onTimer(analyticsTorrents);
						}
						
						if (Preferences.isIncomingConnectionsEnabled() && Preferences.isUpnpEnabled()) {
							(new Thread() {
								public void run() {
									networkManager_.addPortMapping();
								};
							}).start();
						}
					}
				}
				
				try {
					sleep(2000);
				} catch (InterruptedException e) {}
			}
		}
	}
	
	/** Updates the torrent: info, file list, bitfield, peer list, tracker list. */
	public void updateTorrent(final Torrent torrent) {
		if (torrentService_.shouldUpdate(TorrentManagerObserver.UPDATE_SOMETHING)) torrentService_.updateTorrentItem(torrent);
		if (torrentService_.shouldUpdate(TorrentManagerObserver.UPDATE_FILE_LIST)) torrentService_.updateFileList(torrent);
		if (torrentService_.shouldUpdate(TorrentManagerObserver.UPDATE_PEER_LIST)) torrentService_.updatePeerList(torrent);
		if (torrentService_.shouldUpdate(TorrentManagerObserver.UPDATE_TRACKER_LIST)) torrentService_.updateTrackerList(torrent);
		if (torrentService_.shouldUpdate(TorrentManagerObserver.UPDATE_BITFIELD)) {
			if (torrent.isBitfieldChanged()) {
				torrentService_.updateBitfield(torrent);
				torrent.setBitfieldChanged(false);
			}
		}
	}
	
	public void updateMetadata(final Torrent torrent) {
		torrentService_.updateMetadata(torrent);
	}

	/** Constructor of the Torrent Manager with its Torrent Service as a parameter. */
	public TorrentManager(final TorrentManagerObserver torrentService, final NetworkManager networkManager) {
		torrentService_ = torrentService;
		torrents_ = new Vector<Torrent>();
		openingTorrents_ = new Vector<Torrent>();
		generatePeerId();
		
		networkManager_ = networkManager;
		networkManager_.setTorrentManager(this);
		uploadManager_ = new UploadManager();
		downloadManager_ = new DownloadManager();
		torrentSchedulerThread_ = new TorrentSchedulerThread();
		torrentSchedulerThread_.start();
		updateEnabled_ = false;
	}
	
	/** Enables the Torrent Manager. */
	public void enable() {
		isEnabled_ = true;
		
		for (int i = 0; i < torrents_.size(); i++) {
			final Torrent torrent = torrents_.elementAt(i);
			if (torrent.isWorking()) {
				torrent.resume();
			}
		}
	}
	
	/** Disables the Torrent Manager. */
	public void disable() {
		isEnabled_ = false;
		torrentService_.showNotification(TorrentManagerObserver.MESSAGE_INACTIVE);
	}
	
	/** Starts the torrent manager, loads its previously saved state. */
	public void startUp() {
		final JSONArray jsonArray;
		try {
			String content = torrentService_.loadState();
			jsonArray = new JSONArray(content);
		} catch (Exception e) {
			Log.v(LOG_TAG, "Error while startup: " + e.getMessage());
			return;
		}
		for (int i = 0; i < jsonArray.length(); i++) {
			final JSONObject json;
			final String infoHash;
			try {
				json = jsonArray.getJSONObject(i);
				infoHash = json.getString("InfoHash");
			} catch (JSONException e) {
				Log.v(LOG_TAG, e.getMessage());
				continue;
			}
			openTorrent(infoHash, json);
		}
	}
	
	/** Shuts down the torrent manager, saves its current state. */
	public void shutDown() {
		isSchedulerEnabled_ = false;
		networkManager_.stopListening();
		uploadManager_.shutDown();
		
		saveState();
		
		for (int i = 0; i < torrents_.size(); i++) {
			final Torrent torrent = torrents_.get(i);
			torrent.stop();
			
			torrents_.removeElementAt(i);
			i--;
		}
	}
	
	/** Saves the state of the manager to a file. */
	private void saveState() {
		Log.v(LOG_TAG, "Saving state...");
		
		Collections.sort(torrents_);
		final JSONArray jsonArray = new JSONArray();
		for (int i = 0; i < torrents_.size(); i++) {
			final Torrent torrent = torrents_.get(i);
			if (torrent.isCreating()) {
				continue;
			}
			
			jsonArray.put(torrent.getStateInJSON());
			
			Analytics.saveSizeAndTimeInfo(torrent);
		}
		torrentService_.saveState(jsonArray.toString());
	}
	
	/** Connects to an incoming connection. */
	public void addIncomingConnection(final Socket socket) {
		final Peer peer = new PeerImpl(socket, this);
		new Thread(){
			public void run() {
				peer.connect(socket);
			};
		}.start();
	}
	
	/** Attaches a peer to a torrent. */
	public boolean attachPeerToTorrent(final String infoHash, final Peer peer) {
		final Torrent torrent = getTorrent2(infoHash);
		if (torrent != null && torrent.isConnected()) {
			peer.setTorrent(torrent);
			torrent.addIncomingPeer(peer);
			return true;
		} 
		return false;
	}
	
	/** Opens saved torrent. */
	public void openTorrent(final String infoHash, final JSONObject json) {
		final byte[] torrentContent = torrentService_.loadTorrentContent(infoHash);
		
		String downloadFolder;
		try {
			downloadFolder = json.getString("DownloadFolder");
		} catch (Exception ex) {
			downloadFolder = Preferences.getDownloadFolder();
		}
		final Torrent newTorrent = new Torrent(this, uploadManager_, downloadManager_, infoHash, downloadFolder);
		
		if (torrentContent == null) {
			Log.v(LOG_TAG, "Error occured while processing the saved torrent file");
			return;
		}
		
		Log.v(LOG_TAG, "Bencoding saved torrent: " + infoHash);
		try {
			Bencoded bencoded = Bencoded.parse(torrentContent);
			
			newTorrent.processBencodedTorrent(bencoded);
			
			if (newTorrent.setStateFromJSON(json)) {
				addTorrent(newTorrent);
				torrentService_.updateTorrentItem(newTorrent);
				if (newTorrent.getStatus() != Torrent.STATUS_STOPPED) {
					new Thread() {
						public void run() {
							newTorrent.start();
						};
					}.start();
				}
			}
		} catch (BencodedException e) {
			Log.v(LOG_TAG, "Error occured while processing the saved torrent file: " + e.getMessage());
		} catch (DrTorrentException e) {
			Log.v(LOG_TAG, "Error occured while processing the saved torrent file");
		}
	}
	
	/** Reads the saved content of the torrent with the given infohash. */
	public byte[] readTorrentContent(final String infoHash) {
		return torrentService_.loadTorrentContent(infoHash);
	}
	
	/** Creates a new torrent. */
	public void createTorrent(String filePath, String trackers, boolean isPrivate, String filePathSaveAs, boolean shouldStart) {
		CreatingTorrent torrent = new CreatingTorrent(this, uploadManager_, downloadManager_, filePath, filePath);
		torrent.setName(filePathSaveAs);
		torrents_.addElement(torrent);
		torrentService_.updateTorrentItem(torrent);
		torrent.create(filePath, trackers, isPrivate, filePathSaveAs, shouldStart);
	}
	
	/** Opens torrent and starts seeding it. */
	public void openCreatedTorrent(CreatingTorrent creatingTorrent) {
		byte[] torrentContent = FileManager.read(creatingTorrent.getTorrentPath());
		
		Log.v(LOG_TAG, "Bencoding the file: " + creatingTorrent.getTorrentPath());
		Bencoded bencoded = null;
		try {
			bencoded = Bencoded.parse(torrentContent);
		} catch (BencodedException e) {
			torrentService_.hideProgress();
			torrentService_.showDialog(TorrentManagerObserver.MESSAGE_NOT_A_TORRENT_FILE);
			return;
		}
		
		if (bencoded == null) {
			torrentService_.hideProgress();
			torrentService_.showDialog(TorrentManagerObserver.MESSAGE_NOT_A_TORRENT_FILE);
			return;
		}
		
		final Torrent newTorrent = new Torrent(creatingTorrent);
		try {
			newTorrent.processBencodedTorrent(bencoded);
			
			torrentService_.saveTorrentContent(newTorrent.getInfoHashString(), torrentContent);
			
			// addTorrent(newTorrent); // already added with this id
			synchronized (torrents_) {
				torrents_.removeElement(creatingTorrent);
				torrents_.addElement(newTorrent);
			}
			torrentService_.updateTorrentItem(newTorrent);
			
			if (creatingTorrent.shouldStart_) {
				newTorrent.shouldStart();
			}
			newTorrent.startSeeding();
			
			Analytics.newTorrent(newTorrent);
			
			latestSaveTime_ = SystemClock.elapsedRealtime() - STATE_SAVING_INTERVAL;
			
			//showTorrentSettings(newTorrent);
			//newTorrent.start();
			
		} catch (InvalidContentException e) {
			torrents_.removeElement(creatingTorrent);
			torrentService_.removeTorrentItem(creatingTorrent);
			torrentService_.showDialog(TorrentManagerObserver.MESSAGE_WRONG_FILE);
			
		} catch (AlreadyExistsException e) {
			torrents_.removeElement(creatingTorrent);
			torrentService_.removeTorrentItem(creatingTorrent);
			final ArrayList<TrackerInfo> newTrackers = newTorrent.getTrackers();
			if (newTrackers != null && !newTrackers.isEmpty()) {
				final ArrayList<String> newTrackerUrls = new ArrayList<String>();
				for (TrackerInfo newTracker : newTrackers) {
					newTrackerUrls.add(newTracker.getUrl());
				}
				
				torrentService_.showAlreadyOpenedDialog(newTorrent.getInfoHash(), newTrackerUrls);
				
			} else {
				torrentService_.showDialog(TorrentManagerObserver.MESSAGE_THE_TORRENT_IS_ALREADY_OPENED);
			}
			
		} catch (Exception e)  {
			torrents_.removeElement(creatingTorrent);
			torrentService_.removeTorrentItem(creatingTorrent);
			torrentService_.showDialog(TorrentManagerObserver.MESSAGE_ERROR);
		}
		torrentService_.hideProgress();
	}
	
	/** Opens a new Torrent file with the given file path. */
	public void openTorrent(final Uri torrentUri) {
		openTorrent(torrentUri, Preferences.getDownloadFolder());
	}
	
	/**
	 * Opens and starts a new Torrent file with the given file path.
	 * Also defines its download folder.   
	 */
	public void openTorrent(final Uri torrentUri, final String downloadPath) {
		byte[] torrentContent = null;
		String path = "";
		String scheme = "";
		
		try {
			scheme = torrentUri.getScheme();
			if (scheme == null || torrentUri.getSchemeSpecificPart() == null) {
				throw new Exception();
			}
		} catch (Exception e) {
			if (torrentUri.toString().startsWith("magnet")) {
				torrentService_.showDialog(TorrentManagerObserver.MESSAGE_WRONG_MAGNET_LINK);
			} else {
				torrentService_.showDialog(TorrentManagerObserver.MESSAGE_WRONG_FILE);
			}
			return;
		}
		
		if (scheme.equalsIgnoreCase("file")) {
			torrentService_.showProgress(TorrentManagerObserver.MESSAGE_READING_THE_TORRENT_FILE);
			
			path = torrentUri.getPath();
			Log.v(LOG_TAG, "Reading the torrent: " + path);
			torrentContent = FileManager.read(path);
			
		} else if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) {
			torrentService_.showProgress(TorrentManagerObserver.MESSAGE_DOWNLOADING_THE_TORRENT);
			
			path = "http:" + torrentUri.getEncodedSchemeSpecificPart();
			Log.v(LOG_TAG, "Downloading the torrent: " + path);
			torrentContent = (new HttpConnection(path)).execute();
			if (torrentContent == null) {
				path = "http://" + torrentUri.getHost() + torrentUri.getPath();
				Log.v(LOG_TAG, "Downloading the torrent: " + path);
				torrentContent = (new HttpConnection(path)).execute();
			}
			
			torrentService_.showProgress(TorrentManagerObserver.MESSAGE_READING_THE_TORRENT_FILE);
			
		} else if (scheme.equalsIgnoreCase("magnet")) {
			torrentService_.showProgress(TorrentManagerObserver.MESSAGE_READING_THE_TORRENT_FILE);
			
			MagnetUri magnetUri = new MagnetUri(torrentUri);
			openTorrent(magnetUri, downloadPath);
			return;
		}
		
		if (torrentContent == null) {
			torrentService_.hideProgress();
			torrentService_.showDialog(TorrentManagerObserver.MESSAGE_NOT_A_TORRENT_FILE);
			return;
		}
		
		Log.v(LOG_TAG, "Bencoding the file: " + path);
		Bencoded bencoded = null;
		try {
			bencoded = Bencoded.parse(torrentContent);
		} catch (BencodedException e) {
			torrentService_.hideProgress();
			torrentService_.showDialog(TorrentManagerObserver.MESSAGE_NOT_A_TORRENT_FILE);
			return;
		}
		
		if (bencoded == null) {
			torrentService_.hideProgress();
			torrentService_.showDialog(TorrentManagerObserver.MESSAGE_NOT_A_TORRENT_FILE);
			return;
		}
		
		final Torrent newTorrent = new Torrent(this, uploadManager_, downloadManager_, path, downloadPath);
		
		try {
			newTorrent.processBencodedTorrent(bencoded);
			
			torrentService_.saveTorrentContent(newTorrent.getInfoHashString(), torrentContent);
			openingTorrents_.add(newTorrent);
			showTorrentSettings(newTorrent);
			//newTorrent.start();
			
		} catch(InvalidContentException e) {
			torrentService_.showDialog(TorrentManagerObserver.MESSAGE_WRONG_FILE);
			
		} catch(AlreadyExistsException e) {
			final ArrayList<TrackerInfo> newTrackers = newTorrent.getTrackers();
			if (newTrackers != null && !newTrackers.isEmpty()) {
				final ArrayList<String> newTrackerUrls = new ArrayList<String>();
				for (TrackerInfo newTracker : newTrackers) {
					newTrackerUrls.add(newTracker.getUrl());
				}
				
				torrentService_.showAlreadyOpenedDialog(newTorrent.getInfoHash(), newTrackerUrls);
				
			} else {
				torrentService_.showDialog(TorrentManagerObserver.MESSAGE_THE_TORRENT_IS_ALREADY_OPENED);
			}
			
		} catch(NotEnoughFreeSpaceException e) {
			torrentService_.showDialog(TorrentManagerObserver.MESSAGE_NOT_ENOUGH_FREE_SPACE);
			
		} catch(Exception e) {
			torrentService_.showDialog(TorrentManagerObserver.MESSAGE_ERROR);
		}
		
		torrentService_.hideProgress();
	}
	
	/**
	 * Opens and starts a new Torrent with the given Magnet link.
	 * Also defines its download folder.   
	 */
	public void openTorrent(final MagnetUri magnetUri, final String downloadPath) {
		final Torrent newTorrent = new Torrent(this, uploadManager_, downloadManager_, magnetUri.toString(), downloadPath);
		
		try {
			newTorrent.processMagnetTorrent(magnetUri);
			
			openingTorrents_.add(newTorrent);
			showTorrentSettings(newTorrent);
			newTorrent.start();
			
		} catch(InvalidContentException e) {
			torrentService_.showDialog(TorrentManagerObserver.MESSAGE_WRONG_MAGNET_LINK);
			
		} catch(AlreadyExistsException e) {
			final ArrayList<TrackerInfo> newTrackers = newTorrent.getTrackers();
			if (newTrackers != null && !newTrackers.isEmpty()) {
				final ArrayList<String> newTrackerUrls = new ArrayList<String>();
				for (TrackerInfo newTracker : newTrackers) {
					newTrackerUrls.add(newTracker.getUrl());
				}
				torrentService_.showAlreadyOpenedDialog(newTorrent.getInfoHash(), newTrackerUrls);
			} else {
				torrentService_.showDialog(TorrentManagerObserver.MESSAGE_THE_TORRENT_IS_ALREADY_OPENED);
			}
			
		} catch(Exception e) {
			torrentService_.showDialog(TorrentManagerObserver.MESSAGE_ERROR);
		}
		
		torrentService_.hideProgress();
	}
	
	/** Shows the settings dialog of the torrent (file list). */
	public void showTorrentSettings(final Torrent torrent) {
		torrentService_.showTorrentSettings(torrent);
	}
	
	/** Sets the settings of the torrent and starts it. */
	public void setTorrentSettings(final int torrentId, final String downloadFolder, final boolean isRemoved, final ArrayList<Integer> filePriorities) { 
		for (int i = 0; i < openingTorrents_.size(); i++) {
			final Torrent torrent = openingTorrents_.elementAt(i);
			if (torrent.getId() == torrentId) {
				final Vector<File> files = torrent.getFiles();
				if (!isRemoved) {
					for (int j = 0; j < filePriorities.size(); j++) {
						files.get(j).setPriority(filePriorities.get(j));
					}
					torrent.setDownloadFolder(downloadFolder);
					
					openingTorrents_.remove(torrent);
					addTorrent(torrent);
					torrentService_.updateTorrentItem(torrent);
					
					torrent.shouldStart();
					torrent.start();
					
					Analytics.newTorrent(torrent);
					
					latestSaveTime_ = SystemClock.elapsedRealtime() - STATE_SAVING_INTERVAL;
				} else {
					torrent.stop();
					openingTorrents_.remove(torrent);
					torrentService_.removeTorrentContent(torrent.getInfoHashString());
				}
				break;
			}
		}
	}
	
	/** Saves the metadata of the torrent as a torrent file. */
	public void saveTorrentMetadata(final Torrent torrent, final byte[] data) {
		/** d4:info[data]e */
		final byte[] dataPre = "d4:info".getBytes();
		final byte[] dataPost = "e".getBytes();
		final byte[] torrentContent = new byte[data.length + dataPre.length + dataPost.length];
		System.arraycopy(dataPre, 0, torrentContent, 0, dataPre.length);
		System.arraycopy(data, 0, torrentContent, dataPre.length, data.length);
		System.arraycopy(dataPost, 0, torrentContent, dataPre.length + data.length, dataPost.length);
		
		torrentService_.saveTorrentContent(torrent.getInfoHashString(), torrentContent);
	}
	
	/** Adds a new torrent to the list of the managed torrents. */
	public boolean addTorrent(final Torrent torrent) {
		if (!hasTorrent(torrent.getInfoHash())) {
			torrents_.add(torrent);
			return true;
		}
		return false;
	}
	
	/** Starts a torrent. */
	public void startTorrent(final int torrentId) {
		final Torrent torrent = getTorrent(torrentId);
		if (torrent != null) torrent.start();
	}
	
	/** Stops a torrent. */
	public void stopTorrent(final int torrentId) {
		final Torrent torrent = getTorrent(torrentId);
		if (torrent != null) {
			torrent.stop();
			torrentService_.updateTorrentItem(torrent);
		}
	}
	
	/** Closes a torrent. */
	public void closeTorrent(final int torrentId, final boolean deleteFiles) {
		final Torrent torrent = getTorrent(torrentId);
		if (torrent != null) {
			if (Preferences.isAnalyticsEnabled()) {
				torrent.setRemovedOn();
				Analytics.saveSizeAndTimeInfo(torrent);
				Analytics.saveRemovedOn(torrent);
			} else {
				Analytics.removeTorrent(torrent);
			}
				
			torrents_.removeElement(torrent);
			torrentService_.removeTorrentItem(torrent);
			torrentService_.removeTorrentContent(torrent.getInfoHashString());
			torrent.stop();
			
			if (deleteFiles && torrent.isValid()) {
				(new Thread() {
					public void run() {
						torrent.removeFiles();
					};
				}).start();
			}
			
			latestSaveTime_ = SystemClock.elapsedRealtime() - STATE_SAVING_INTERVAL;
		}
	}
	
	/** Adds a peer to the torrent. */
	public void addPeer(final int torrentId, final String address, final int port) {
		final Torrent torrent = getTorrent(torrentId);
		if (torrent != null) {
			torrent.addPeer(address, port, null);
		}
	}
	
	/** Adds a tracker to the torrent. */
	public void addTracker(final int torrentId, final String trackerUrl) {
		final Torrent torrent = getTorrent(torrentId);
		if (torrent != null) {
			torrent.addTracker(trackerUrl);
		}
	}
	
	/** Adds trackers to the torrent. */
	public void addTrackers(final String infoHash, final ArrayList<String> trackerUrls) {
		final Torrent torrent = getTorrent(infoHash);
		if (torrent != null) {
			for (String trackerUrl : trackerUrls) {
				torrent.addTracker(trackerUrl);
			}
		}
	}

	/** Updates a tracker from the torrent. */
	public void updateTracker(final int torrentId, final int trackerId) {
		final Torrent torrent = getTorrent(torrentId);
		if (torrent != null) {
			torrent.updateTracker(trackerId);
		}
	}
	
	/** Removes a tracker from the torrent. */
	public void removeTracker(final int torrentId, final int trackerId) {
		final Torrent torrent = getTorrent(torrentId);
		if (torrent != null) {
			torrent.removeTracker(trackerId);
		}
	}
	
	/** Returns whether the manager the given torrent already has. */
	public boolean hasTorrent(final String infoHash) {
		if (getTorrent(infoHash) != null) {
			return true;
		}
		
		return false;
	}
	
	/** Returns a torrent by its ID. */
	public Torrent getTorrent(final int torrentId) {
		for (int i = 0; i < torrents_.size(); i++) {
			final Torrent torrent = torrents_.elementAt(i);
			if (torrent.getId() == torrentId) {
				return torrent;
			}
		}
		return null;
	}
	
	/** Returns a torrent by its info hash. */
	public Torrent getTorrent(final String infoHash) {
		for (int i = 0; i < torrents_.size(); i++) {
			final Torrent tempTorrent = (Torrent) torrents_.elementAt(i);
			final String tempInfoHash = tempTorrent.getInfoHash();
			if (tempInfoHash != null && tempInfoHash.equals(infoHash)) {
				return tempTorrent;
			}
		}
		return null;
	}
	
	/** Returns a torrent by its info hash string. */
	public Torrent getTorrent2(final String infoHashString) {
		for (int i = 0; i < torrents_.size(); i++) {
			final Torrent tempTorrent = (Torrent) torrents_.elementAt(i);
			final String tempInfoHash = tempTorrent.getInfoHashString();
			if (tempInfoHash != null && tempInfoHash.equals(infoHashString)) {
				return tempTorrent;
			}
		}
		return null;
	}
	
	public Torrent getOpeningTorrent(final int id) {
		for (int i = 0; i < openingTorrents_.size(); i++) {
			final Torrent torrent = openingTorrents_.elementAt(i);
			if (torrent.getId() == id) {
				return torrent;
			}
		}
		return null;
	}
	
	/** Changes the priority of a file of a torrent. */
	public void changeFilePriority(final int torrentId, final int fileIndex, final int filePriority) {
		final Torrent torrent = getTorrent(torrentId);
		if (torrent != null ) torrent.changeFilePriority(fileIndex, filePriority);
	}
	
	/** Shows the completed notification. */
	public void showCompletedNotification(final Torrent torrent) {
		torrentService_.showCompletedNotification(torrent.getName());
	}
	
	/** Returns the ID of the peer. */
	public static String getPeerID() {
		return peerId_;
		//return "-DR1000-abcdefghijkl";
	}
	
	/** Returns the key of the peer. */
	public static int getPeerKey() {
		return peerKey_;
	}
	
	/**Generates the ID and the key of the peer. */
	private static void generatePeerId() {
		Date d = new Date();
        long seed = d.getTime();   
        Random r = new Random();
        r.setSeed(seed);
        
        peerKey_ = Math.abs(r.nextInt());
        peerId_ = "-DR1260-";	// TODO: Refresh!
        for (int i=0; i<12; i++) {
            peerId_ += (char)(Math.abs(r.nextInt()) % 25 + 97); // random lower case alphabetic characters ('a' - 'z')
        }
        
        Log.v(LOG_TAG, "PeerID: " + peerId_);
        Log.v(LOG_TAG, "PeerKey: " + peerKey_);
	}

	/** Activates the scheduler thread to update the UI. */
	public void update() {
		updateEnabled_ = true;
		if (torrentSchedulerThread_ != null) {
			torrentSchedulerThread_.interrupt();
		}
	}
	
	/** Returns wheter the the download/upload is enabled or not. */
	public boolean isEnabled() {
		return isEnabled_;
	}
}
