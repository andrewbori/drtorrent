package hu.bute.daai.amorg.drtorrent.torrentengine;

import hu.bute.daai.amorg.drtorrent.Preferences;
import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.Tools;
import hu.bute.daai.amorg.drtorrent.coding.bencode.Bencoded;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedDictionary;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedInteger;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedList;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedString;
import hu.bute.daai.amorg.drtorrent.coding.sha1.SHA1;
import hu.bute.daai.amorg.drtorrent.file.FileManager;
import hu.bute.daai.amorg.drtorrent.network.NetworkManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.SystemClock;
import android.util.Log;
import android.webkit.URLUtil;

/** Class representing a torrent. */
public class Torrent {
	private final static String LOG_TAG = "Torrent";

	public final static int ERROR_NONE = 0;
	public final static int ERROR_WRONG_CONTENT = -1;
	public final static int ERROR_NO_FREE_SIZE = -2;
	public final static int ERROR_ALREADY_EXISTS = -3;
	public final static int ERROR_FILE_HANDLING = -4;
	public final static int ERROR_OVERFLOW = -5;
	public final static int ERROR_TORRENT_NOT_PARSED = -6;
	public final static int ERROR_NOT_ATTACHED = -7;
	public final static int ERROR_GENERAL = -8;

	private static int ID = 0;
	private int id_;

	private TorrentManager torrentManager_;
	private FileManager fileManager_;

	private int status_ = R.string.status_stopped;

	private String infoHash_;
	private String infoHashString_;
	private byte[] infoHashByteArray_;

	private String torrentFilePath_;
	private String torrentFileName_;
	private String downloadFolder_;
	private String name_;
	private String path_; // downloadFolder + name
	private int pieceLength_;

	private String comment_ = "";
	private String createdBy_ = "";
	private long creationDate_ = 0;
	private boolean valid_ = false;

	private int seeds_ = 0;
	private int leechers_ = 0;

	private long fullSize_ = 0; 			// Size of the full torrent
	private long activeSize_ = 0; 			// Size of the selected files (pieces)
	private long activeDownloadedSize_ = 0; // Size of the selected downloaded files (pieces)
	private long checkedSize_ = 0; 			// Size of the checked bytes (during hash check)
	private long downloadedSize_ = 0; 		// Size of the downloaded bytes
	private long uploadedSize_ = 0; 		// Size of the uploaded bytes
	private Speed downloadSpeed_;
	private Speed uploadSpeed_;
	private Vector<File> files_;
	private Vector<Piece> pieces_;
	private Vector<Piece> activePieces_;
	private Vector<Piece> downloadablePieces_;
	private Vector<Piece> rarestPieces_;
	private Vector<Piece> downloadingPieces_;
	private Vector<Block> requestedBlocks_;
	private Bitfield bitfield_;
	private Bitfield downloadingBitfield_;

	private long elapsedTime_ = 0;
	private long lastTime_ = 0;

	private Vector<Peer> peers_;
	private Vector<Peer> connectedPeers_;
	private Vector<Peer> notConnectedPeers_;
	private Vector<Peer> interestedPeers_;
	private Vector<Tracker> trackers_;

	private boolean isEndGame_ = false;
	
	private ThreadPoolExecutor peerTPE_ = null;

	/** Constructor with the manager and the torrent file as a parameter. */
	public Torrent(TorrentManager torrentManager, String filePath, String downloadPath) {
		torrentManager_ = torrentManager;
		fileManager_ = new FileManager(this);
		downloadFolder_ = downloadPath;

		torrentFilePath_ = filePath;
		torrentFileName_ = filePath.substring(filePath.lastIndexOf('/') + 1, filePath.length());

		files_ = new Vector<File>();
		pieces_ = new Vector<Piece>();
		activePieces_ = new Vector<Piece>();
		downloadablePieces_ = new Vector<Piece>();
		rarestPieces_ = new Vector<Piece>();
		downloadingPieces_ = new Vector<Piece>();
		requestedBlocks_ = new Vector<Block>();
		peers_ = new Vector<Peer>();
		connectedPeers_ = new Vector<Peer>();
		notConnectedPeers_ = new Vector<Peer>();
		interestedPeers_ = new Vector<Peer>();
		downloadSpeed_ = new Speed();
		uploadSpeed_ = new Speed();

		id_ = ++ID;
	}

	/**
	 * Processes the bencoded torrent.
	 * 
	 * @param torrentBencoded The torrent file's bencoded content.
	 * @return Error code.
	 */
	public int processBencodedTorrent(Bencoded torrentBencoded) {
		long time = SystemClock.elapsedRealtime();

		status_ = R.string.status_opening;

		if (torrentBencoded.type() != Bencoded.BENCODED_DICTIONARY)
			return ERROR_WRONG_CONTENT; // bad bencoded torrent

		BencodedDictionary torrent = (BencodedDictionary) torrentBencoded;
		Bencoded tempBencoded = null;

		trackers_ = new Vector<Tracker>();
		// announce
		tempBencoded = torrent.entryValue("announce");
		if (tempBencoded != null && tempBencoded.type() == Bencoded.BENCODED_STRING) {
			String announce = ((BencodedString) tempBencoded).getStringValue();
			addTracker(announce);
		}

		// announce-list
		tempBencoded = torrent.entryValue("announce-list");
		if (tempBencoded != null && tempBencoded.type() == Bencoded.BENCODED_LIST) {
			trackers_.removeAllElements();

			BencodedList parseAnnounceLists = (BencodedList) tempBencoded;
			for (int i = 0; i < parseAnnounceLists.count(); i++) {
				tempBencoded = parseAnnounceLists.item(i);
				if (tempBencoded != null && tempBencoded.type() == Bencoded.BENCODED_LIST) {
					BencodedList announces = (BencodedList) tempBencoded;
					for (int j = 0; j < announces.count(); j++) {
						tempBencoded = announces.item(j);
						if (tempBencoded != null && tempBencoded.type() == Bencoded.BENCODED_STRING) {
							String announceURL = ((BencodedString) tempBencoded).getStringValue();
							Log.v(LOG_TAG, announceURL);
							addTracker(announceURL);
						}
					}
					Log.v(LOG_TAG, "----------------------");
				}
			}
		}

		// info
		tempBencoded = torrent.entryValue("info");
		if (tempBencoded == null || tempBencoded.type() != Bencoded.BENCODED_DICTIONARY)
			return ERROR_WRONG_CONTENT;

		BencodedDictionary info = (BencodedDictionary) tempBencoded;

		SHA1 sha1 = new SHA1();
		sha1.update(info.Bencode());
		int[] hashResult = sha1.digest();
		infoHash_ = SHA1.resultToByteString(hashResult);

		if (torrentManager_.hasTorrent(infoHash_))
			return ERROR_ALREADY_EXISTS;

		infoHashByteArray_ = SHA1.resultToByte(hashResult);
		infoHashString_ = SHA1.resultToString(hashResult);
		Log.v(LOG_TAG, "Infohash of the torrrent(hex): " + infoHashString_);
		hashResult = null;

		// info/piece lenght
		tempBencoded = info.entryValue("piece length");
		if (tempBencoded == null || tempBencoded.type() != Bencoded.BENCODED_INTEGER)
			return ERROR_WRONG_CONTENT;

		pieceLength_ = (int) ((BencodedInteger) tempBencoded).getValue();

		// file/files

		tempBencoded = info.entryValue("files");
		if (tempBencoded != null) {
			// multi-file torrent
			if (tempBencoded.type() != Bencoded.BENCODED_LIST)
				return ERROR_WRONG_CONTENT;

			BencodedList files = (BencodedList) tempBencoded;

			// info/name

			tempBencoded = info.entryValue("name");
			if (tempBencoded == null || tempBencoded.type() != Bencoded.BENCODED_STRING)
				return ERROR_WRONG_CONTENT;

			name_ = ((BencodedString) tempBencoded).getStringValue();
			path_ = downloadFolder_ + name_ + "/";

			long fileBegin = 0;
			for (int i = 0; i < files.count(); i++) {
				tempBencoded = files.item(i);
				if (tempBencoded.type() != Bencoded.BENCODED_DICTIONARY)
					return ERROR_WRONG_CONTENT;

				BencodedDictionary file = (BencodedDictionary) tempBencoded;

				tempBencoded = file.entryValue("path");
				if (tempBencoded == null || tempBencoded.type() != Bencoded.BENCODED_LIST)
					return ERROR_WRONG_CONTENT;

				BencodedList inPath = (BencodedList) tempBencoded;

				String pathBuf = "";

				for (int j = 0; j < inPath.count(); j++) {
					tempBencoded = inPath.item(j);
					if (tempBencoded.type() != Bencoded.BENCODED_STRING)
						return ERROR_WRONG_CONTENT;

					pathBuf = pathBuf + ((BencodedString) tempBencoded).getStringValue();

					if (j != (inPath.count() - 1))
						pathBuf = pathBuf + "/";
				}

				tempBencoded = file.entryValue("length");
				if (tempBencoded == null || tempBencoded.type() != Bencoded.BENCODED_INTEGER)
					return ERROR_WRONG_CONTENT;

				long length = ((BencodedInteger) tempBencoded).getValue();
				fullSize_ += length;

				files_.addElement(new File(this, i, (int) (fileBegin / pieceLength_), path_, pathBuf, length));
				fileBegin += length;
			}
		} else {
			// single-file torrent

			// info/length
			tempBencoded = info.entryValue("length");
			if (tempBencoded == null || tempBencoded.type() != Bencoded.BENCODED_INTEGER)
				return ERROR_WRONG_CONTENT;

			long length = ((BencodedInteger) tempBencoded).getValue();
			fullSize_ = length;

			// info/name
			tempBencoded = info.entryValue("name");
			if (tempBencoded == null || tempBencoded.type() != Bencoded.BENCODED_STRING)
				return ERROR_WRONG_CONTENT;

			name_ = ((BencodedString) tempBencoded).getStringValue();
			path_ = downloadFolder_; // + name_ + "/";

			files_.addElement(new File(this, 0, 0, path_, name_, length));
		}

		// info/pieces

		tempBencoded = info.entryValue("pieces");

		if (tempBencoded == null || tempBencoded.type() != Bencoded.BENCODED_STRING)
			return ERROR_WRONG_CONTENT;

		byte[] piecesArray = ((BencodedString) tempBencoded).getValue();

		if ((piecesArray.length % 20) != 0)
			return ERROR_WRONG_CONTENT;

		int processedLength = 0;
		int pieceCount = piecesArray.length / 20;

		byte[] hash;
		Piece piece;
		for (int i = 0; i < pieceCount; i++) {
			hash = new byte[20];
			System.arraycopy(piecesArray, i * 20, hash, 0, 20);

			if (i != pieceCount - 1) {
				processedLength += pieceLength_;
				piece = new Piece(this, hash, i, pieceLength_);
			} else {
				piece = new Piece(this, hash, i, (int) (fullSize_ - processedLength));
			}

			pieces_.addElement(piece);
			downloadablePieces_.addElement(piece);
		}
		Log.v(LOG_TAG, "Pieces: " + pieceCount + " size: " + Tools.bytesToString(pieceLength_));

		bitfield_ = new Bitfield(pieces_.size(), false);
		downloadingBitfield_ = new Bitfield(pieces_.size(), false);

		// comment
		tempBencoded = torrent.entryValue("comment");
		if (tempBencoded != null && tempBencoded.type() == Bencoded.BENCODED_STRING)
			comment_ = ((BencodedString) tempBencoded).getStringValue();

		// created by
		tempBencoded = torrent.entryValue("created by");
		if (tempBencoded != null && tempBencoded.type() == Bencoded.BENCODED_STRING)
			createdBy_ = ((BencodedString) tempBencoded).getStringValue();

		// creation date
		tempBencoded = torrent.entryValue("creation date");
		if (tempBencoded != null && tempBencoded.type() == Bencoded.BENCODED_INTEGER)
			creationDate_ = ((BencodedInteger) tempBencoded).getValue();

		calculateFileFragments();
		valid_ = true;

		/*
		 * DecimalFormat dec = new DecimalFormat("###.##"); float filesize =
		 * (float) (getSize() / 1048576.0); name_ += " (" + dec.format(filesize)
		 * + " MB)";
		 */

		Log.v(LOG_TAG, "Bencode processing over. " + (float) (SystemClock.elapsedRealtime() - time) / 1000.0 + " seconds.");

		return ERROR_NONE;
	}

	/** Sets the active pieces according to the selected file list of the torrent. */
	protected void setTorrentActivePieces() {
		synchronized (activePieces_) {
			synchronized (downloadablePieces_) {
				synchronized (downloadingPieces_) {
					activeSize_ = 0;
					activeDownloadedSize_ = 0;
					activePieces_ = new Vector<Piece>();
					for (int i = 0; i < pieces_.size(); i++) {
						Piece piece = pieces_.get(i);
						piece.calculatePriority();
						if (piece.priority() > File.PRIORITY_SKIP) {
							activePieces_.addElement(piece);
							activeSize_ += piece.size();
							activeDownloadedSize_ += piece.downloaded();
							if (!bitfield_.isBitSet(i)) {
								if (!downloadablePieces_.contains(piece) && !downloadingPieces_.contains(piece)) {
									downloadablePieces_.addElement(piece);

									if (status_ == R.string.status_seeding) {
										status_ = R.string.status_downloading;
									}
								}
							}
						} else {
							downloadablePieces_.removeElement(piece);
							downloadingPieces_.removeElement(piece);
						}
					}
				}
			}
		}

		if (isComplete()) {
			if (status_ == R.string.status_downloading)
				status_ = R.string.status_seeding;
		} else {
			if (status_ == R.string.status_seeding)
				status_ = R.string.status_downloading;
		}
	}
	
	/** Adds a new file (and files that contain its piece) to the file manger to create. */
	protected void addFileToFileManager(File file) {
		fileManager_.addFile(file);
		
		Piece firstPiece = pieces_.elementAt(file.getIndexOfFirstPiece());
		Piece lastPiece = null;
		int indexOfNextFile = file.index() + 1;
		if (files_.size() > indexOfNextFile) {
			lastPiece = pieces_.elementAt(files_.elementAt(indexOfNextFile).getIndexOfFirstPiece());
		}
		
		for (int j = file.index() - 1; j > 0; j--) {
			if (firstPiece.hasFile(files_.elementAt(j))) {
				fileManager_.addFile(files_.elementAt(j));
			} else {
				break;
			}
		}
		
		if (lastPiece != null) {
			for (int j = file.index() + 1; j < files_.size(); j++) {
				if (lastPiece.hasFile(files_.elementAt(j))) {
					fileManager_.addFile(files_.elementAt(j));
				} else {
					break;
				}
			}
		}
	}

	/** Changes the priority of the given file. */
	public void changeFilePriority(int index, int priority) {
		if (index >= 0 && index < files_.size()) {
			final File file = files_.elementAt(index);
			if (file.getPriority() != priority) {
				if (priority > File.PRIORITY_SKIP) {
					addFileToFileManager(file);
					createFiles();
				}
				
				file.setPriority(priority);
				setTorrentActivePieces();
			}
		}
	}

	/** Checks the pieces of the torrent. */
	public void checkHash() {
		status_ = R.string.status_hash_check;

		for (File file : files_) {
			if (file.getPriority() > 0) {
				int result = createFile(file);
				if (result == ERROR_ALREADY_EXISTS) {
					file.checkHash(true);
				} else {
					file.checkHash(false);
				}
			}
			if (status_ == R.string.status_stopped) {
				return;
			}
		}
	}
	
	/** Calls the FileManager to create the files previously were given to him. */
	public void createFiles() {
		if (!fileManager_.isCreating()) {
			(new Thread() {
				@Override
				public void run() {
					fileManager_.createFiles();
				}
			}).start();
		}
	}

	/** Adds bytes to the size of the checked bytes and calculates the percent of checked bytes. */
	public void addCheckedBytes(int bytes) {
		checkedSize_ += bytes;
	}

	/**
	 * Calculates the file fragments of pieces. This is important because a
	 * torrent can contain multiple files, so when saving the received blocks of
	 * pieces we have to know the file boundaries. If a torrent contains only
	 * one file then "piece" ~ "file fragment".
	 */
	protected void calculateFileFragments() {
		int pieceIndex = 0;
		Piece piece = pieces_.elementAt(pieceIndex);
		int piecePos = 0;

		for (int i = 0; i < files_.size(); i++) {
			File file = (File) files_.elementAt(i);
			// If the file is NOT BIGGER than the remaining part of the piece...
			if (piecePos + file.getSize() <= piece.size()) {
				piece.addFileFragment(file, 0, (int) file.getSize());
				piecePos += file.getSize();

				// If EQUALS then iterates to the next piece
				if (piecePos == piece.size()) {
					if (pieces_.size() > pieceIndex + 1) {
						piece = pieces_.elementAt(++pieceIndex);
						piecePos = 0;
					}
				}
				// ...if the file is BIGGER
			} else {
				int filePos = 0;
				while (filePos < file.getSize()) {
					// If the remaining part of the file is BIGGER than the
					// remaining part of the piece...
					if ((file.getSize() - filePos + piecePos) > piece.size()) {
						piece.addFileFragment(file, filePos, piece.size() - piecePos);
						filePos += piece.size() - piecePos;

						if (pieces_.size() > pieceIndex + 1) {
							piece = pieces_.elementAt(++pieceIndex);
							piecePos = 0;
						}
						// ...if SMALLER or EQUALS
					} else {
						piece.addFileFragment(file, filePos, (int) (file.getSize() - filePos));
						piecePos += file.getSize() - filePos;

						// If EQUALS than get the iterates to the next piece
						if (piecePos == piece.size()) {
							if (pieces_.size() > pieceIndex + 1) {
								piece = pieces_.elementAt(++pieceIndex);
								piecePos = 0;
							}
						}
						break; // Iterates to the next file
					}
				}
			}
		}
	}

	private boolean isFirstStart_ = true;

	/** Starts the torrent. */
	public void start() {
		if (valid_) {
			lastTime_ = SystemClock.elapsedRealtime();

			if (isFirstStart_) {
				setTorrentActivePieces();
				checkHash();

				if (status_ == R.string.status_stopped) {
					return;
				}
				
				isFirstStart_ = false;
			}
			createFiles();

			downloadSpeed_ = new Speed();
			uploadSpeed_ = new Speed();
			
			lastOptimisticUnchokingTime_ = lastUnchokingTime_ = 0;

			peerTPE_ = new ThreadPoolExecutor(
					Preferences.getMaxConnectedPeers(), Preferences.getMaxConnectedPeers(),
					30, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(5)
			);
			
			notConnectedPeers_ = new Vector<Peer>();

			for (int i = 0; i < peers_.size(); i++) {
				Peer peer = peers_.elementAt(i);
				peer.resetTcpTimeoutCount();
				notConnectedPeers_.add(peer);
			}

			if (isComplete()) {
				status_ = R.string.status_seeding;
			} else {
				status_ = R.string.status_downloading;
			}

			if (torrentManager_.isEnabled()) {
				for (int i = 0; i < trackers_.size(); i++) {
					trackers_.elementAt(i).changeEvent(Tracker.EVENT_STARTED);
				}
			}
		}
	}
	
	/** Resumes the torrent when network connection is established. */
	public void resume() {
		Log.v(LOG_TAG, "Resuming...");
		for (int i = 0; i < notConnectedPeers_.size(); i++) {
			Peer peer = notConnectedPeers_.elementAt(i);
			peer.resetTcpTimeoutCount();
		}
		
		lastTime_ = SystemClock.elapsedRealtime();
		
		for (int i = 0; i < trackers_.size(); i++) {
			Tracker tracker = trackers_.elementAt(i);
			if (tracker.getStatus() == Tracker.STATUS_FAILED) {
				tracker.resetLastRequest();
			}
		}
	}

	/** Stops the torrent. */
	public void stop() {
		if (isConnected()) {
			if (torrentManager_.isEnabled()) {
				for (int i = 0; i < trackers_.size(); i++) {
					trackers_.elementAt(i).changeEvent(Tracker.EVENT_STOPPED);
				}
			}

			for (int i = 0; i < connectedPeers_.size(); i++) {
				connectedPeers_.get(i).disconnect();
			}
			connectedPeers_ = new Vector<Peer>();
			notConnectedPeers_ = new Vector<Peer>();
			interestedPeers_ = new Vector<Peer>();
			
			peerTPE_.shutdown();
			peerTPE_ = null;
		}
		lastTime_ = 0;
		status_ = R.string.status_stopped;
	}

	/** Removes a disconnected peer from the array of connected peers. */
	public void peerDisconnected(Peer peer) {
		connectedPeers_.removeElement(peer);
		interestedPeers_.removeElement(peer);
		if (peer.getTcpTimeoutCount() >= 3) {
			peers_.removeElement(peer);
		} else {
			notConnectedPeers_.addElement(peer);
		}
	}

	/** Adds the given peer to the interested peers. */
	public void peerInterested(Peer peer) {
		if (!interestedPeers_.contains(peer))
			interestedPeers_.addElement(peer);
	}

	/** Removes the given peer from the interested peers. */
	public void peerNotInterested(Peer peer) {
		interestedPeers_.removeElement(peer);
	}

	private long latestBytesDownloaded_ = 0;
	private long latestBytesUploaded_ = 0;

	/** Scheduler method. Schedules the peers and the trackers. */
	public void onTimer() {
		if (lastTime_ != 0) {
			long currentTime = SystemClock.elapsedRealtime();
			elapsedTime_ += (int) (currentTime - lastTime_);
			lastTime_ = currentTime;
		}
		downloadSpeed_.addBytes(downloadedSize_ - latestBytesDownloaded_, lastTime_);
		uploadSpeed_.addBytes(uploadedSize_ - latestBytesUploaded_, lastTime_);
		latestBytesDownloaded_ = downloadedSize_;
		latestBytesUploaded_ = uploadedSize_;

		if (isConnected() && torrentManager_.isEnabled()) {
			if (valid_) {
				Log.v(LOG_TAG,
						"peers: " + connectedPeers_.size() + " Blocks: " + requestedBlocks_.size() + " Downloadable: " + downloadablePieces_.size()
								+ " Downloading: " + downloadingPieces_.size());

				// Updates the peers
				for (int i = 0; i < connectedPeers_.size(); i++) {
					connectedPeers_.elementAt(i).onTimer();
				}

				if (Preferences.isUploadEnabled()) {
					chokeAlgorithm();
				}

				// Updates the trackers
				for (int i = 0; i < trackers_.size(); i++) {
					trackers_.elementAt(i).onTimer();
				}

				// Connects to peers
				if (status_ == R.string.status_downloading) {
					try {
						int activeCount = peerTPE_.getActiveCount();
						for (int i = 0; i < notConnectedPeers_.size() && peerTPE_.getActiveCount() < peerTPE_.getCorePoolSize() && activeCount < peerTPE_.getCorePoolSize() && connectedPeers_.size() < peerTPE_.getCorePoolSize(); i++) {
							Peer peer = notConnectedPeers_.elementAt(i);
							if (peer.canConnect()) {
								Runnable command = peer.connect(this);
								peerTPE_.execute(command);
								connectedPeers_.addElement(peer);
								notConnectedPeers_.removeElement(peer);
								i--;
								activeCount++;
							}
						}
					} catch (Exception e) {}
				}
			}
		}
	}

	private long lastUnchokingTime_ = 0;
	private Peer optimisticUnchokedPeer_ = null;
	private long lastOptimisticUnchokingTime_ = 0;

	/** Selects the unchoked peers and unchokes them. */
	public void chokeAlgorithm() {
		// Choking algorithm runs in every 10 seconds
		long currentTime = SystemClock.elapsedRealtime();
		if (!(lastUnchokingTime_ + 10000.0 < currentTime || lastOptimisticUnchokingTime_ + 30000.0 < currentTime))
			return;

		if (interestedPeers_.size() > 0) {
			Vector<Peer> peersToUnchoke = new Vector<Peer>();

			synchronized (interestedPeers_) {
				// Choosing the optimistic unchoked peer (a random peer) in
				// every 30 seconds
				if (lastOptimisticUnchokingTime_ + 30000.0 < currentTime) {
					lastOptimisticUnchokingTime_ = currentTime;
					Collections.shuffle(interestedPeers_); // shuffle to select
															// a random peer
					optimisticUnchokedPeer_ = interestedPeers_.elementAt(0);
				}

				if (lastUnchokingTime_ + 10000.0 < currentTime) {
					lastUnchokingTime_ = currentTime;

					// TODO: Choosing peers in seed mode!!!
					// Tit for tat: unchoking the peers with the most upload.
					Peer peer = null;
					for (int i = 0; i < interestedPeers_.size(); i++) {
						peer = interestedPeers_.elementAt(i);

						int speed = peer.getDownloadSpeed();
						boolean isFound = false;
						if (speed > 0 && peer != optimisticUnchokedPeer_) {
							for (int j = 0; j < peersToUnchoke.size(); j++) {
								if (peersToUnchoke.elementAt(j).getDownloadSpeed() > speed) {
									peersToUnchoke.add(j, peer);
									isFound = true;
									break;
								}
							}
							if (!isFound)
								peersToUnchoke.add(peer);
						}
					}

					// Only keeps 3 peers and the optimistic unchoked peer
					for (int i = 3; i < peersToUnchoke.size(); i++) {
						peersToUnchoke.remove(i);
						i--;
					}
					if (optimisticUnchokedPeer_ != null)
						peersToUnchoke.add(optimisticUnchokedPeer_);

					for (int i = 0; i < interestedPeers_.size(); i++) {
						peer = interestedPeers_.elementAt(i);
						if (peersToUnchoke.contains(peer)) {
							peer.setChoking(false);
						} else {
							peer.setChoking(true);
						}
					}
				}
			}
		} else
			optimisticUnchokedPeer_ = null;
	}

	/** Returns a downloadable block for the given peer. */
	public synchronized ArrayList<Block> getBlockToDownload(Peer peer, int number) {
		ArrayList<Block> blocks = new ArrayList<Block>();
		Block block = null;
		Piece piece = null;

		if (downloadablePieces_.size() > 0) {
			// NORMAL MODE

			for (int priority = File.PRIORITY_HIGH; priority >= File.PRIORITY_LOW; priority--) {
				if (Preferences.isStreamingMode()) {
					// STREAMING MODE

					for (int i = 0; i < activePieces_.size(); i++) {
						piece = activePieces_.elementAt(i);
						if (!piece.canDownload() || piece.priority() < priority)
							continue;
						if (downloadablePieces_.contains(piece) || downloadingPieces_.contains(piece)) {
							if (peer.hasPiece(piece.index())) {
								if (piece.hasUnrequestedBlock()) {
									block = piece.getUnrequestedBlock();
									if (block != null) {
										if (downloadablePieces_.contains(piece)) {
											downloadablePieces_.removeElement(piece);
											downloadingPieces_.addElement(piece);
											downloadingBitfield_.setBit(piece.index());
										}
										requestedBlocks_.addElement(block);

										blocks.add(block);
										if (blocks.size() >= number)
											return blocks;
									}
								}
							}
						}
					}

				} else {
					// RAREST/RANDOM PIECE FIRST MODE

					synchronized (downloadingPieces_) {
						// Get a block from the downloading pieces
						for (int i = 0; i < downloadingPieces_.size(); i++) {
							piece = downloadingPieces_.elementAt(i);
							if (!piece.canDownload() || piece.priority() < priority)
								continue;
							if (peer.hasPiece(piece.index())) {
								if (piece.hasUnrequestedBlock()) {
									block = piece.getUnrequestedBlock();
									if (block != null) {
										requestedBlocks_.addElement(block);

										blocks.add(block);
										if (blocks.size() >= number)
											return blocks;
									}
								}
							}
						}
					}

					if (rarestPieces_.size() == 0 && downloadablePieces_.size() != 0)
						calculateRarestPieces();
					synchronized (rarestPieces_) {
						// Get a block from the rarest pieces
						for (int i = 0; i < rarestPieces_.size(); i++) {
							piece = rarestPieces_.elementAt(i);
							if (!piece.canDownload() || piece.priority() < priority)
								continue;
							if (peer.hasPiece(piece.index())) {
								if (piece.hasUnrequestedBlock()) {
									block = piece.getUnrequestedBlock();
									if (block != null) {
										downloadablePieces_.removeElement(piece);
										rarestPieces_.removeElement(piece);
										downloadingPieces_.addElement(piece);
										downloadingBitfield_.setBit(piece.index());
										requestedBlocks_.addElement(block);

										blocks.add(block);
										if (blocks.size() >= number)
											return blocks;
									}
								}
							}
						}
					}

					synchronized (downloadablePieces_) {
						Collections.shuffle(downloadablePieces_);
						// Get a block from the downloadable pieces
						for (int i = 0; i < downloadablePieces_.size(); i++) {
							piece = downloadablePieces_.elementAt(i);
							if (!piece.canDownload() || piece.priority() < priority)
								continue;
							if (peer.hasPiece(piece.index())) {
								if (piece.hasUnrequestedBlock()) {
									block = piece.getUnrequestedBlock();
									if (block != null) {
										downloadablePieces_.removeElement(piece);
										downloadingPieces_.addElement(piece);
										downloadingBitfield_.setBit(piece.index());
										requestedBlocks_.addElement(block);

										blocks.add(block);
										if (blocks.size() >= number)
											return blocks;
									}
								}
							}
						}
					}
				}
			}

		} else {
			// END GAME MODE
			isEndGame_ = true;
			synchronized (downloadingPieces_) {
				// Get a block from the downloading pieces
				for (int i = 0; i < downloadingPieces_.size(); i++) {
					piece = downloadingPieces_.elementAt(i);
					if (peer.hasPiece(piece.index())) {
						if (piece.hasUnrequestedBlock()) {
							block = piece.getUnrequestedBlock();
							if (block != null) {
								requestedBlocks_.addElement(block);

								blocks.add(block);
								if (blocks.size() >= number)
									return blocks;
							}
						}
					}
				}
			}

			synchronized (requestedBlocks_) {
				Collections.shuffle(requestedBlocks_);
				for (int i = 0; i < requestedBlocks_.size(); i++) {
					block = requestedBlocks_.elementAt(i);
					if (!block.isDownloaded() && !blocks.contains(block) && !peer.hasBlock(block) && peer.hasPiece(block.pieceIndex())) {

						blocks.add(block);
						if (blocks.size() >= number)
							return blocks;
					}
				}
			}
		}

		return blocks;
	}

	/** Cancels the downloading of a block. */
	public void cancelBlock(Block block) {
		if (!isEndGame_) {
			block.setNotRequested();
			Piece piece = getPiece(block.pieceIndex());
			piece.addBlockToRequest(block);
			requestedBlocks_.removeElement(block);
		}
	}

	/** Creates a new file in the file system and reserves space for it as well. */
	private int createFile(File file) {
		String filePath = file.getFullPath();
		long size = file.getSize();

		// Creating the file
		if (path_ != null && !path_.equals("")) {

			boolean existed = false;
			if (FileManager.fileExists(filePath))
				existed = true;
			
			if (FileManager.freeSize(path_) < size) {
				Log.v(LOG_TAG, "Not enough free space");
				return ERROR_NO_FREE_SIZE;
			}

			addFileToFileManager(file);
			
			if (existed) {
				return ERROR_ALREADY_EXISTS;
			}
			return ERROR_NONE;
		}

		return ERROR_WRONG_CONTENT;
	}

	/**
	 * Writes a block into the file in the given position. The offset and the
	 * length within the block is also given.
	 */
	public synchronized int writeFile(File file, long filePosition, byte[] block, int offset, int length) {
		return fileManager_.writeFile(file.getFullPath(), filePosition, block, offset, length);
	}

	/**
	 * Reads a block from the file. The position and the length within the file
	 * is also given.
	 */
	public synchronized byte[] read(String filepath, long position, int length) {
		return fileManager_.read(filepath, position, length);
	}

	/** Adds a new peer to the list of the peers of the torrent. */
	public int addPeer(String address, int port, String peerId) {
		// if (peers_.size() >= Preferences.getMaxConnections()) return
		// ERROR_OVERFLOW;
		if (hasPeer(address, port))
			return ERROR_ALREADY_EXISTS;

		Peer peer = new Peer(address, port, peerId, pieces_.size());
		peers_.addElement(peer);
		notConnectedPeers_.addElement(peer);
		// Log.v(LOG_TAG, "Number of peers: " + peers_.size());

		return ERROR_NONE;
	}
	
	/** Adds a new tracker to the list of the trackers. */
	public void addTracker(String url) {
		if (!hasTracker(url)) {
			if (url.startsWith("http://")) {
				if (URLUtil.isValidUrl(url)) {
					trackers_.addElement(new TrackerHttp(url, this));
				}
			} else if (url.startsWith("udp://")) {
				if (URLUtil.isValidUrl("http" + url.substring(3))) {
					trackers_.addElement(new TrackerUdp(url, this));
				}
			}
		}
	}

	/** Removes the given tracker from the tracker list. */
	public void removeTracker(int trackerId) {
		for (int i = 0; i < trackers_.size(); i++) {
			Tracker tracker = trackers_.elementAt(i);
			if (tracker.getId() == trackerId) {
				trackers_.removeElement(tracker);
				return;
			}
		}
	}
	
	/** Adds an incoming peer to the array of (connected) peers. */
	public void addIncomingPeer(Peer peer) {
		peers_.addElement(peer);
		connectedPeers_.addElement(peer);
	}

	/** Returns whether there is a peer with the given IP and port in the list of peers or not. */
	public boolean hasPeer(String address, int port) {
		Peer tempPeer;
		for (int i = 0; i < peers_.size(); i++) {
			tempPeer = peers_.elementAt(i);
			if (tempPeer.getAddress().equals(address) && tempPeer.getPort() == port)
				return true;
		}
		return false;
	}
	
	/** Returns whether there is a tracker with the given URL in the list of trackers or not. */
	public boolean hasTracker(String url) {
		Tracker tracker;
		for (int i = 0; i < trackers_.size(); i++) {
			tracker = trackers_.elementAt(i);
			if (tracker.getUrl().equals(url))
				return true;
		}
		return false;
	}

	/** Increments the number of peers having the given piece. */
	public void incNumberOfPeersHavingPiece(int index) {
		pieces_.elementAt(index).incNumberOfPeersHaveThis();
	}

	/** Decrements the number of peers having the given piece. */
	public void decNumberOfPeersHavingPiece(int index) {
		pieces_.elementAt(index).decNumberOfPeersHaveThis();
	}

	/** Calculates the rarest pieces. */
	public void calculateRarestPieces() {
		Piece piece = null;
		int n = Integer.MAX_VALUE;

		synchronized (downloadablePieces_) {
			for (int i = 0; i < downloadablePieces_.size(); i++) {
				piece = downloadablePieces_.elementAt(i);
				int k = piece.getNumberOfPeersHaveThis();
				if (k < n)
					n = k;
			}

			synchronized (rarestPieces_) {
				rarestPieces_.removeAllElements();
				for (int i = 0; i < downloadablePieces_.size(); i++) {
					piece = downloadablePieces_.elementAt(i);
					int k = piece.getNumberOfPeersHaveThis();
					if (k == n)
						rarestPieces_.add(piece);
				}

				Collections.shuffle(rarestPieces_);
			}
		}
	}

	/** Updates the downloaded bytes with the given amount of bytes. */
	public void updateBytesDownloaded(int bytes) {
		if (bytes >= 0) {
			downloadedSize_ += bytes;
		}
		
		activeDownloadedSize_ += bytes;
	}

	/** Updates the uploaded bytes. */
	public void updateBytesUploaded(int bytes) {
		uploadedSize_ += bytes;
	}

	/** Called after a block has been refused to download (disconnected, choked etc.). */
	public void blockNotRequested(Block block) {
		requestedBlocks_.removeElement(block);
	}

	/** Called after a block has been downloaded. */
	public void blockDownloaded(Block block) {
		requestedBlocks_.removeElement(block);
		if (isEndGame_) {
			Peer peer = null;
			for (int i = 0; i < connectedPeers_.size(); i++) {
				peer = connectedPeers_.elementAt(i);
				if (peer.hasBlock(block)) {
					peer.cancelBlock(block);
				}
			}
		}
	}

	/** Called after a piece has been downloaded. */
	public void pieceDownloaded(Piece piece, boolean calledBySavedTorrent) {
		if (calledBySavedTorrent)
			downloadablePieces_.removeElement(piece);
		else
			downloadingPieces_.removeElement(piece);

		bitfield_.setBit(piece.index());
		//downloadedPieceCount_++;

		if (calledBySavedTorrent) {
			activeDownloadedSize_ += piece.size(); //updateBytesDownloaded(piece.size());
			piece.addFilesDownloadedBytes(true);
		}

		// If the download is complete
		// if (downloadedPieceCount_ == pieces_.size()) {
		if (isComplete()) {
			activeDownloadedSize_ = activeSize_;	// download percent = 100%
			status_ = R.string.status_seeding;

			if (!calledBySavedTorrent) {
				Log.v(LOG_TAG, "DOWNLOAD COMPLETE");
				torrentManager_.showCompletedNotification(this);
				if (bitfield_.isFull()) {
					for (int i = 0; i < trackers_.size(); i++) {
						trackers_.elementAt(i).changeEvent(Tracker.EVENT_COMPLETED);
					}
				}

				// Disconnect from peers if not interested
				synchronized (connectedPeers_) {
					for (int i = 0; i < connectedPeers_.size(); i++) {
						// if (!connectedPeers_.elementAt(i).isIncommingPeer())
						if (!connectedPeers_.elementAt(i).isPeerInterested()) {
							connectedPeers_.elementAt(i).disconnect();
						}
					}
				}
			}
		}

		// Notify peers about having a new piece
		if (!calledBySavedTorrent) {
			synchronized (connectedPeers_) {
				for (int i = 0; i < connectedPeers_.size(); i++) {
					connectedPeers_.elementAt(i).notifyThatClientHavePiece(piece.index());
				}
			}
		}
	}

	/** Called when the downloaded piece is wrong. */
	public void pieceHashFailed(Piece piece) {
		synchronized (downloadingPieces_) {
			downloadingPieces_.removeElement(piece);
			downloadingBitfield_.unsetBit(piece.index());
			rarestPieces_.removeElement(piece);
			downloadablePieces_.addElement(piece);
		}
		updateBytesDownloaded(-piece.size());

		piece.addFilesDownloadedBytes(false);
	}

	/** Returns whether the downloading is complete or not. */
	public boolean isComplete() {
		return (downloadablePieces_.isEmpty() && downloadingPieces_.isEmpty());
	}

	/** Returns the index of the given piece. */
	public int indexOfPiece(Piece piece) {
		return pieces_.indexOf(piece);
	}

	/** Returns the length of pieces. */
	public int getPieceLength() {
		return pieceLength_;
	}
	
	/** Returns the number of pieces. */
	public int pieceCount() {
		return pieces_.size();
	}

	/** Returns the piece with the given index. */
	public Piece getPiece(int index) {
		return pieces_.elementAt(index);
	}

	/** Returns the active size of the torrent. */
	public long getActiveSize() {
		return activeSize_;
	}
	
	/** Returns the active downloaded size of the torrent. */
	public long getActiveDownloadedSize() {
		return activeDownloadedSize_;
	}

	/** Returns the number of bytes have to be downloaded. */
	public long getBytesLeft() {
		return activeSize_ - activeDownloadedSize_;
	}

	/** Returns the number of downloaded bytes. */
	public long getBytesDownloaded() {
		return downloadedSize_;
	}

	/** Returns the number of uploaded bytes. */
	public long getBytesUploaded() {
		return uploadedSize_;
	}

	/** Returns the download speed. */
	public int getDownloadSpeed() {
		return downloadSpeed_.getSpeed();
	}

	/** Returns the upload speed. */
	public int getUploadSpeed() {
		return uploadSpeed_.getSpeed();
	}

	/** Returns the percent of the downloaded data. */
	public double getProgressPercent() {
		if (activeSize_ == 0) return 100.0;
		if (status_ != R.string.status_hash_check) {
			return activeDownloadedSize_ / (activeSize_ / 100.0);
		}
		return checkedSize_ / (activeSize_ / 100.0);
	}

	/** Returns the id of the torrent. (Only used inside this program.) */
	public int getId() {
		return id_;
	}

	/** Returns the info hash as a string. */
	public String getInfoHash() {
		return this.infoHash_;
	}

	/** Returns the encoded info hash. */
	public String getInfoHashString() {
		return infoHashString_;
	}

	/** Returns the info hash. */
	public byte[] getInfoHashByteArray() {
		return infoHashByteArray_;
	}

	/** Returns the bitfield of the torrent. */
	public Bitfield getBitfield() {
		return bitfield_;
	}

	/** Returns the downloading bitfield of the torrent. */
	public Bitfield getDownloadingBitfield() {
		return downloadingBitfield_;
	}

	/** Returns the manager of the torrent. */
	public TorrentManager getTorrentManager() {
		return this.torrentManager_;
	}

	/** Returns the name of the torrent. */
	public String getName() {
		return name_;
	}

	/** Returns the status of the torrent. */
	public int getStatus() {
		return status_;
	}

	/** Returns whether the torrent is working or not (hash checking/downloading/seeding). */
	public boolean isWorking() {
		return status_ == R.string.status_downloading || status_ == R.string.status_seeding || status_ == R.string.status_hash_check;
	}
	
	/** Returns whether the torrent is connected or not. (downloading/seeding) */
	public boolean isConnected() {
		return status_ == R.string.status_downloading || status_ == R.string.status_seeding;
	}

	/** Returns the number of seeds. */
	public int getSeeds() {
		return seeds_;
	}

	/** Returns the number of leechers. */
	public int getLeechers() {
		return leechers_;
	}

	/** Sets the count of leechers & seeds. */
	public void setSeedsLeechers() {
		int seeds = 0;
		int leechers = 0;
		for (int i = 0; i < trackers_.size(); i++) {
			Tracker tracker = trackers_.elementAt(i);
			if (tracker.getComplete() > seeds)
				seeds = tracker.getComplete();
			if (tracker.getIncomplete() > leechers)
				leechers = tracker.getIncomplete();
		}
		seeds_ = seeds;
		leechers_ = leechers;
	}

	/** Returns the array of the connected peers. */
	public Vector<Peer> getConnectedPeers() {
		return connectedPeers_;
	}

	/** Returns the array of the files of the torrent. */
	public Vector<File> getFiles() {
		return files_;
	}

	/** Returns the array of the trackers of the torrent. */
	public Vector<Tracker> getTrackers() {
		return trackers_;
	}

	/** Returns the remaining time (ms) until the downloading ends. */
	public int getRemainingTime() {
		int speed = getDownloadSpeed();
		if (speed == 0)
			return -1;
		long remaining = activeSize_ - activeDownloadedSize_;
		double millis = ((double) remaining / (double) speed) * 1000.0;
		return (int) millis;
	}

	/** Returns the elapsed time (ms) since the torrent has been started. */
	public long getElapsedTime() {
		return elapsedTime_;
	}

	/** Return whether the bitfield has changed since the latest check or not. */
	public boolean isBitfieldChanged() {
		return bitfield_.isChanged() || downloadingBitfield_.isChanged();
	}

	/** Sets whether the bitfield has changed or not. */
	public void setBitfieldChanged(boolean isChanged) {
		bitfield_.setChanged(isChanged);
		downloadingBitfield_.setChanged(isChanged);
	}

	/** Returns information of the torrent in JSON. */
	public JSONObject getJSON() {
		JSONObject json = new JSONObject();

		try {
			json.put("InfoHash", getInfoHashString());

			JSONArray bitfield = new JSONArray();
			for (int i = 0; i < bitfield_.data().length; i++) {
				bitfield.put((int) bitfield_.data()[i]);
			}
			json.put("Bitfield", bitfield);

			JSONArray filePriorities = new JSONArray();
			for (File file : files_) {
				filePriorities.put(file.getPriority());
			}
			json.put("FilePriorities", filePriorities);
			
			json.put("Status", status_);
			
			json.put("ElapsedTime", elapsedTime_);
			json.put("Uploaded", uploadedSize_);
			json.put("Downloaded", downloadedSize_);
			json.put("IsFirstStart", isFirstStart_);
			
			JSONArray trackers = new JSONArray();
			for (Tracker tracker : trackers_) {
				trackers.put(tracker.getUrl());
			}
			json.put("Trackers", trackers);
		} catch (JSONException e) {
			Log.v(LOG_TAG, e.getMessage());
		}

		return json;
	}
	
	/** Sets the torrent from JSON. */
	public boolean setJSON(JSONObject json) {
		try {
			JSONArray filePriorities = json.getJSONArray("FilePriorities");
			if (filePriorities.length() == files_.size()) {
				for (int i = 0; i < filePriorities.length(); i++) {
					int priority = filePriorities.getInt(i);
					File file = files_.get(i);
					file.setPriority(priority);
					if (filePriorities.getInt(i) > File.PRIORITY_SKIP) {
						addFileToFileManager(file);
					}
				}
			}
			createFiles();
			setTorrentActivePieces();
			
			JSONArray bitfield = json.getJSONArray("Bitfield");
			ByteArrayBuffer bab = new ByteArrayBuffer(bitfield.length());
			for (int i = 0; i < bitfield.length(); i++) {
				bab.append((byte) bitfield.getInt(i));
			}
			bitfield_.set(bab.buffer());
			
			for (int i = 0; i < pieceCount(); i++) {
				if (bitfield_.isBitSet(i)) {
					Piece piece = pieces_.get(i);
					pieceDownloaded(piece, true);
					piece.setComplete();
					status_ = R.string.status_opening;
				}
			}
			
			elapsedTime_ = json.getLong("ElapsedTime");
			downloadedSize_ = json.getLong("Downloaded");
			uploadedSize_ = json.getLong("Uploaded");
			isFirstStart_ = json.getBoolean("IsFirstStart");
			status_ = json.getInt("Status");
			
			JSONArray trackers = json.getJSONArray("Trackers");
			trackers_ = new Vector<Tracker>();
			for (int i = 0; i < trackers.length(); i++) {
				String trackerUrl = trackers.getString(i);
				addTracker(trackerUrl);
			}
			
			downloadSpeed_ = new Speed();
			uploadSpeed_ = new Speed();
			
		} catch (JSONException e) {
			Log.v(LOG_TAG, e.getMessage());
			return false;
		}
		
		return true;
	}
}
