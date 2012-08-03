package hu.bute.daai.amorg.drtorrent.torrentengine;

import hu.bute.daai.amorg.drtorrent.DrTorrentTools;
import hu.bute.daai.amorg.drtorrent.Preferences;
import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.coding.bencode.Bencoded;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedDictionary;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedInteger;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedList;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedString;
import hu.bute.daai.amorg.drtorrent.coding.sha1.SHA1;
import hu.bute.daai.amorg.drtorrent.file.FileManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import android.util.Log;

/** Class representing a torrent. */
public class Torrent {
	private final static String LOG_TAG = "Torrent";
	
	public final static int ERROR_NONE               =  0;
	public final static int ERROR_WRONG_CONTENT      = -1;
	public final static int ERROR_NO_FREE_SIZE       = -2;
	public final static int ERROR_ALREADY_EXISTS     = -3;
	public final static int ERROR_FILE_HANDLING      = -4;
	public final static int ERROR_OVERFLOW           = -5;
	public final static int ERROR_TORRENT_NOT_PARSED = -6;
	public final static int ERROR_NOT_ATTACHED       = -7;
	public final static int ERROR_GENERAL            = -8;

	private final int MAX_STORED_PEERS = 5;
	
	private TorrentManager torrentManager_;
	private FileManager fileManager_;

	private int status_ = R.string.status_stopped;
	
	private String announce_;
	private String infoHash_;
	private byte[] infoHashByteArray_;
	private boolean multiTrackerEnabled_ = false;

	private String torrentFilePath_;
	private String torrentFileName_;
	private String downloadFolder_;
	private String name_;
	private String path_; // downloadFolder + name
	private int pieceLength_;

	private String comment_;
	private String createdBy_;
	private int creationDate_;
	private boolean valid_ = false;

	private int bytesTotal_           = 0;
	private int bytesUploaded_        = 0;
	private int bytesDownloaded_      = 0;
	private Speed downloadSpeed_;
	private int uploadSpeed_;
	private int bytesLeft_            = 0;
	private double downloadPercent_   = 0;
	private int downloadedPieceCount_ = 0;
	private boolean complete_ = false;
	private Vector<File> files_;
	private Vector<Piece> pieces_;
	private Vector<Piece> activePieces_;
	private Vector<Piece> downloadablePieces_;
	private Vector<Piece> rarestPieces_;
	private Vector<Piece> downloadingPieces_;
	private Vector<Block> requestedBlocks_;
	private Bitfield bitfield_;
	private Bitfield downloadingBitfield_;
	
	private int elapsedTime_ = 0;
	private long lastTime_ = 0;

	private Vector<Peer> peers_;
	private Vector<Peer> connectedPeers_;
	private Vector<Peer> notConnectedPeers_;
	private Vector<Peer> interestedPeers_;
	private Tracker tracker_;
	private Vector<Vector<Tracker>> trackerList_;
	private Vector<Tracker> trackers_;
	private Vector<String> announceList_;
	private int trackerRequestInterval;
	
	private boolean isEndGame_ = false;
	
	/** Constructor with the manager and the torrent file as a parameter. */
	public Torrent(TorrentManager torrentManager, String filePath, String downloadPath) {
		torrentManager_ = torrentManager;
		fileManager_ = new FileManager();
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
		tracker_ = new Tracker();
		peers_ = new Vector<Peer>();
		connectedPeers_ = new Vector<Peer>();
		notConnectedPeers_ = new Vector<Peer>();
		interestedPeers_ = new Vector<Peer>();
		downloadSpeed_ = new Speed();
	}

	/**
	 * Processes the bencoded torrent.
	 * 
	 * @param torrentBencoded The torrent file's bencoded content.
	 * @return Error code.
	 */
	public int processBencodedTorrent(Bencoded torrentBencoded) {
		long time = System.currentTimeMillis();
		
		status_ = R.string.status_opening;
		
		if (torrentBencoded.type() != Bencoded.BencodedDictionary)
			return ERROR_WRONG_CONTENT;	// bad bencoded torrent

		BencodedDictionary torrent = (BencodedDictionary) torrentBencoded;
		Bencoded tempBencoded = null;

		// announce
		tempBencoded = torrent.entryValue("announce");
		if (tempBencoded == null || tempBencoded.type() != Bencoded.BencodedString)
			return ERROR_WRONG_CONTENT;

		announce_ = ((BencodedString) tempBencoded).getStringValue();
		tracker_ = new Tracker(announce_, this);
		//trackerList_ = new Vector<Vector<Tracker>>();
		trackers_ = new Vector<Tracker>();
		//Vector<Tracker> tempTrackers = new Vector<Tracker>();
		//trackerList_.addElement(tempTrackers);
		//tempTrackers.addElement(new Tracker(announce_, this));
		trackers_.addElement(tracker_);

		// announce-list
		tempBencoded = torrent.entryValue("announce-list");
		if (tempBencoded != null && tempBencoded.type() == Bencoded.BencodedList) {
			//announceList_ = new Vector<String>();
			//trackerList_.removeAllElements();
			trackers_.removeAllElements();
			multiTrackerEnabled_ = true;
			BencodedList parseAnnounceLists = (BencodedList) tempBencoded;
			for (int i = 0; i < parseAnnounceLists.count(); i++) {
				tempBencoded = parseAnnounceLists.item(i);
				if (tempBencoded != null && tempBencoded.type() == Bencoded.BencodedList) {
					BencodedList announces = (BencodedList) tempBencoded;
					//tempTrackers = new Vector<Tracker>();
					//trackerList_.addElement(tempTrackers);
					for (int j = 0; j < announces.count(); j++) {
						tempBencoded = announces.item(j);
						if (tempBencoded != null && tempBencoded.type() == Bencoded.BencodedString) {
							String tempAnnounceURL = ((BencodedString) tempBencoded).getStringValue();
							Log.v(LOG_TAG, tempAnnounceURL);
							if (!tempAnnounceURL.startsWith("udp")) {
								//announceList_.addElement(tempAnnounceURL);
								//tempTrackers.addElement(new Tracker(tempAnnounceURL, this));
								trackers_.addElement(new Tracker(tempAnnounceURL, this));
							}
						}
					}
					Log.v(LOG_TAG, "----------------------");
					// VectorSorter.shuffle(tmp);
				}
			}
		}
		
		// info
		tempBencoded = torrent.entryValue("info");
		if (tempBencoded == null || tempBencoded.type() != Bencoded.BencodedDictionary)
			return ERROR_WRONG_CONTENT;

		BencodedDictionary info = (BencodedDictionary) tempBencoded;

		SHA1 sha1 = new SHA1();
		sha1.update(info.Bencode());
		int[] hashResult = sha1.digest();
		infoHash_ = SHA1.resultToByteString(hashResult);
		infoHashByteArray_ = SHA1.resultToByte(hashResult);
		Log.v(LOG_TAG, "Infohash of the torrrent(hex): " + SHA1.resultToString(hashResult));
		hashResult = null;

		// info/piece lenght
		tempBencoded = info.entryValue("piece length");
		if (tempBencoded == null || tempBencoded.type() != Bencoded.BencodedInteger)
			return ERROR_WRONG_CONTENT;

		pieceLength_ = ((BencodedInteger) tempBencoded).getValue();

		// file/files

		tempBencoded = info.entryValue("files");
		if (tempBencoded != null) {
		// multi-file torrent
			if (tempBencoded.type() != Bencoded.BencodedList)
				return ERROR_WRONG_CONTENT;

			BencodedList files = (BencodedList) tempBencoded;

			// info/name

			tempBencoded = info.entryValue("name");
			if (tempBencoded == null || tempBencoded.type() != Bencoded.BencodedString)
				return ERROR_WRONG_CONTENT;

			name_ = ((BencodedString) tempBencoded).getStringValue();
			path_ = downloadFolder_ + name_ + "/";

			for (int i = 0; i < files.count(); i++) {
				tempBencoded = files.item(i);
				if (tempBencoded.type() != Bencoded.BencodedDictionary)
					return ERROR_WRONG_CONTENT;

				BencodedDictionary file = (BencodedDictionary) tempBencoded;

				tempBencoded = file.entryValue("path");
				if (tempBencoded == null || tempBencoded.type() != Bencoded.BencodedList)
					return ERROR_WRONG_CONTENT;

				BencodedList inPath = (BencodedList) tempBencoded;

				String pathBuf = "";

				for (int j = 0; j < inPath.count(); j++) {
					tempBencoded = inPath.item(j);
					if (tempBencoded.type() != Bencoded.BencodedString)
						return ERROR_WRONG_CONTENT;
					
					pathBuf = pathBuf + ((BencodedString) tempBencoded).getStringValue();

					if (j != (inPath.count() - 1))
						pathBuf = pathBuf + "/";
				}

				tempBencoded = file.entryValue("length");
				if (tempBencoded == null || tempBencoded.type() != Bencoded.BencodedInteger)
					return ERROR_WRONG_CONTENT;

				int length = ((BencodedInteger) tempBencoded).getValue();
				bytesLeft_ += length;
				
				files_.addElement(new File(path_, pathBuf, length));
			}
		} else {
		// single-file torrent
			int length = 0;

			// info/length
			tempBencoded = info.entryValue("length");
			if (tempBencoded == null || tempBencoded.type() != Bencoded.BencodedInteger)
				return ERROR_WRONG_CONTENT;

			length = ((BencodedInteger) tempBencoded).getValue();
			bytesLeft_ = length;

			// info/name
			tempBencoded = info.entryValue("name");
			if (tempBencoded == null || tempBencoded.type() != Bencoded.BencodedString)
				return ERROR_WRONG_CONTENT;

			name_ = ((BencodedString) tempBencoded).getStringValue();
			path_ = downloadFolder_; //+ name_ + "/";
			
			files_.addElement(new File(path_, name_, length));
		}
		bytesTotal_ = bytesLeft_;

		// info/pieces

		tempBencoded = info.entryValue("pieces");

		if (tempBencoded == null || tempBencoded.type() != Bencoded.BencodedString)
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
				piece = new Piece(this, hash, i, getSize() - processedLength);
			}

			pieces_.addElement(piece);
			downloadablePieces_.addElement(piece);
		}
		Log.v(LOG_TAG, "Pieces: " + pieceCount + " size: " + DrTorrentTools.bytesToString(pieceLength_));

		bitfield_ = new Bitfield(pieces_.size(), false);
		downloadingBitfield_ = new Bitfield(pieces_.size(), false);

		// comment
		tempBencoded = torrent.entryValue("comment");
		if (tempBencoded != null && tempBencoded.type() == Bencoded.BencodedString)
			comment_ = ((BencodedString) tempBencoded).getStringValue();

		// created by
		tempBencoded = torrent.entryValue("created by");
		if (tempBencoded != null && tempBencoded.type() == Bencoded.BencodedString)
			createdBy_ = ((BencodedString) tempBencoded).getStringValue();

		// creation date
		tempBencoded = torrent.entryValue("creation date");
		if (tempBencoded != null && tempBencoded.type() == Bencoded.BencodedInteger)
			creationDate_ = ((BencodedInteger) tempBencoded).getValue();

		calculateFileFragments();
		valid_ = true;
		
		/*DecimalFormat dec = new DecimalFormat("###.##");
		float filesize = (float) (getSize() / 1048576.0);
		name_ += " (" + dec.format(filesize) + " MB)";*/
		
		Log.v(LOG_TAG, "Bencode processing over. " + (float) (System.currentTimeMillis() - time) / 1000.0 + " seconds.");
		
		//checkHash();
		
		return ERROR_NONE;
	}
	
	/** Sets the active pieces according to the selected file list of the torrent. */
	public void setTorrentActivePieces() {
		activePieces_ = new Vector<Piece>();
		for (int i = 0; i < pieces_.size(); i++) {
			Piece piece = pieces_.get(i);
			Vector<FileFragment> fragments = piece.getFragments();
			boolean hasToDownload = false;
			for (int j = 0; j < fragments.size(); j++) {
				if (fragments.get(j).file().getPriority() != File.PRIORITY_SKIP) {
					hasToDownload = true;
					break;
				}
			}
			if (hasToDownload) {
				activePieces_.addElement(piece);
				if (!bitfield_.isBitSet(i)) {
					if (!downloadablePieces_.contains(piece) && !downloadingPieces_.contains(piece)) {
						downloadablePieces_.addElement(piece);
					}
				}
			} else {
				downloadablePieces_.removeElement(piece);
				downloadingPieces_.removeElement(piece);
			}
		}
	}
	
	/** Checks the pieces of the torrent. */
	public void checkHash() {
		status_ = R.string.status_hash_check;
		downloadPercent_ = 0.0;
		int bytesProcessed = 0;
		
		ArrayList<File> checkedFiles = new ArrayList<File>();
		boolean shouldCheck = false;
		for (int i = 0; i < activePieces_.size(); i++) {
			downloadPercent_ = ((double) bytesProcessed * 100.0) / (double) bytesTotal_;
			Piece piece = activePieces_.get(i);
			Vector<FileFragment> fragments = piece.getFragments();
			
			for (int j = 0; j < fragments.size(); j++) {
				File file = fragments.get(j).file();
				if (!checkedFiles.contains(file)) {
					checkedFiles.add(file);
					int result = createFile(file);
					if (result == ERROR_ALREADY_EXISTS) shouldCheck = true;
					else shouldCheck = false;
					Log.v(LOG_TAG, file.getRelativePath() + " " + shouldCheck);
				}
			}
			
			bytesProcessed += piece.size();
			
			if (shouldCheck) {
				if (piece.checkHash()) {
					Log.v(LOG_TAG, "Hash check " + i + " OK");
					piece.setFilesDownloaded();
					pieceDownloaded(piece, true);
					bytesDownloaded_ += piece.size();
					downloadablePieces_.removeElement(piece);
				}
				else Log.v(LOG_TAG, "Hash check " + i + " X");
			}
		}
		downloadPercent_ = bytesDownloaded_ / (bytesTotal_ / 100.0);
		status_ = R.string.status_started;
	}
	
	/** 
	 * Calculates the file fragments of pieces.
	 * This is important because a torrent can contain multiple files,
	 * so when saving the received blocks of pieces we have to know the file boundaries.
	 * If a torrent contains only one file then "piece" ~ "file fragment".
	 */
	private void calculateFileFragments() {
		int pieceIndex = 0;
		Piece piece = pieces_.elementAt(pieceIndex);
		int piecePos = 0;

		for (int i = 0; i < files_.size(); i++) {
			File file = (File) files_.elementAt(i);
			// If the file is NOT BIGGER than the remaining part of the piece...
			if (piecePos + file.getSize() <= piece.size()) {
				piece.addFileFragment(file, 0, file.getSize());
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
					// If the remaining part of the file is BIGGER than the remaining part of the piece...
					if ((file.getSize() - filePos + piecePos) > piece.size()) {
						piece.addFileFragment(file, filePos, piece.size() - piecePos);
						filePos += piece.size() - piecePos;

						if (pieces_.size() > pieceIndex + 1) {
							piece = pieces_.elementAt(++pieceIndex);
							piecePos = 0;
						}
					// ...if SMALLER or EQUALS
					} else {
						piece.addFileFragment(file, filePos, file.getSize() - filePos);
						piecePos += file.getSize() - filePos;
						
						// If EQUALS than get the iterates to the next piece
						if (piecePos == piece.size()) {
							if (pieces_.size() > pieceIndex + 1) {
								piece = pieces_.elementAt(++pieceIndex);
								piecePos = 0;
							}
						}
						break;	// Iterates to the next file
					}
				}
			}
		}
	}
	
	/** Starts the torrent. */
	public void start() {
		if (valid_) {
			lastTime_ = System.currentTimeMillis();
			lastOptimisticUnchokingTime_ = lastUnchokingTime_ = 0;
			
			notConnectedPeers_ = new Vector<Peer>();
			for (int i = 0; i < peers_.size(); i++) {
				notConnectedPeers_.add(peers_.elementAt(i));
			}
			
			status_ = R.string.status_downloading;
			torrentManager_.updateTorrent(this);
			
			//tracker_.changeEvent(Tracker.EVENT_STARTED);
			for (int i = 0; i < trackers_.size(); i++) {
				trackers_.elementAt(i).changeEvent(Tracker.EVENT_STARTED);
			}
		}
	}
	
	/** Stops the torrent. */
	public void stop() {
		if (status_ != R.string.status_stopped) {
			lastTime_ = 0;
			
			//tracker_.changeEvent(Tracker.EVENT_STOPPED);
			for (int i = 0; i < trackers_.size(); i++) {
				trackers_.elementAt(i).changeEvent(Tracker.EVENT_STOPPED);
			}
			
			for (int i = 0; i < connectedPeers_.size(); i++) {
				connectedPeers_.get(i).disconnect();
			}
			connectedPeers_ = new Vector<Peer>();
			notConnectedPeers_ = new Vector<Peer>();
			interestedPeers_ = new Vector<Peer>();
			
			status_ = R.string.status_stopped;
			torrentManager_.updateTorrent(this);
		}
	}
	
	/** Connects to peers. */
	public void connectToPeers() {
		if (notConnectedPeers_.isEmpty()) {
			//tracker_.changeEvent(Tracker.EVENT_NOT_SPECIFIED);
			for (int i = 0; i < trackers_.size(); i++) {
				trackers_.elementAt(i).changeEvent(Tracker.EVENT_NOT_SPECIFIED);
			}
		}
		
		for (int i = 0; i < notConnectedPeers_.size() && connectedPeers_.size() < Preferences.getMaxConnectedPeers(); i++) {
			Peer peer = notConnectedPeers_.elementAt(i);
			connectedPeers_.addElement(peer);
			notConnectedPeers_.removeElement(peer);
			i--;
			peer.connect(this);
		}
	}
	
	/** Removes a disconnected peer from the array of connected peers. */
	public void peerDisconnected(Peer peer) {
		connectedPeers_.removeElement(peer);
		interestedPeers_.removeElement(peer);
	}
	
	/** Adds the given peer to the interested peers. */
	public void peerInterested(Peer peer) {
		if (!interestedPeers_.contains(peer)) interestedPeers_.addElement(peer);
	}
	
	/** Removes the given peer from the interested peers. */
	public void peerNotInterested(Peer peer) {
		interestedPeers_.removeElement(peer);
	}
	
	private int latestBytesDownloaded_ = 0;
	
	/** Scheduler method. Schedules the peers and the trackers. */
	public void onTimer() {
		if (lastTime_ != 0) {
			long currentTime = System.currentTimeMillis();
			elapsedTime_ += (int) (currentTime - lastTime_);
			lastTime_ = currentTime;
		}
		downloadSpeed_.addBytes(bytesDownloaded_ - latestBytesDownloaded_, lastTime_);
		latestBytesDownloaded_ = bytesDownloaded_;
		
		torrentManager_.updateTorrent(this);
		
		if (status_ == R.string.status_downloading || status_ == R.string.status_seeding) {
			if (valid_) {
				Log.v(LOG_TAG, "peers: " + connectedPeers_.size() + " Blocks: " + requestedBlocks_.size() + " Downloadable: " + downloadablePieces_.size() + " Downloading: " + downloadingPieces_.size());
				
				for (int i = 0; i < connectedPeers_.size(); i++) {
					connectedPeers_.elementAt(i).onTimer();
	            }
				
				chokeAlgorithm();
				
				if (connectedPeers_.size() < Preferences.getMaxConnectedPeers()) connectToPeers();
			}
		}
	}
	
	private long lastUnchokingTime_ = 0;
	private Peer optimisticUnchokedPeer_ = null;
	private long lastOptimisticUnchokingTime_ = 0;
	
	/** Selects the unchoked peers and unchokes them. */
	public void chokeAlgorithm() {
		// Choking algorithm runs in every 10 seconds
		long currentTime = System.currentTimeMillis();
		if (!(lastUnchokingTime_ + 10000.0 < currentTime || lastOptimisticUnchokingTime_ + 30000.0 < currentTime)) return;
		
		if (interestedPeers_.size() > 0) {			
			Vector<Peer> peersToUnchoke = new Vector<Peer>();
			
			synchronized (interestedPeers_) {
				// Choosing the optimistic unchoked peer (a random peer) in every 30 seconds
				if (lastOptimisticUnchokingTime_ + 30000.0 < currentTime) {
					lastOptimisticUnchokingTime_ = currentTime;
					Collections.shuffle(interestedPeers_);	// shuffle to select a random peer
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
							if (!isFound) peersToUnchoke.add(peer);
						}
					}
					
					// Only keeps 3 peers and the optimistic unchoked peer
					for (int i = 3; i < peersToUnchoke.size(); i++) {
						peersToUnchoke.remove(i);
						i--;
					}
					if (optimisticUnchokedPeer_ != null) peersToUnchoke.add(optimisticUnchokedPeer_);
					
					
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
		} else optimisticUnchokedPeer_ = null;
	}
	
	/** Returns a downloadable block for the given peer. */
	public synchronized ArrayList<Block> getBlockToDownload(Peer peer, int number) {
		ArrayList<Block> blocks = new ArrayList<Block>();
		Block block = null;
		Piece piece = null;
		
		if (downloadablePieces_.size() > 0) {
			// NORMAL MODE
			
			if (Preferences.isStreamingMode()) {
				// STREAMING MODE
				
				for (int i = 0; i < activePieces_.size(); i++) {
					piece = activePieces_.elementAt(i);
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
									if (blocks.size() >= number) return blocks;
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
						if (peer.hasPiece(piece.index())) {
							if (piece.hasUnrequestedBlock()) {
								block = piece.getUnrequestedBlock();
								if (block != null) {
									requestedBlocks_.addElement(block);
									
									blocks.add(block);
									if (blocks.size() >= number) return blocks;
								}
							}
						}
					}
				}
				
				if (rarestPieces_.size() == 0 && downloadablePieces_.size() != 0) calculateRarestPieces();
				synchronized (rarestPieces_) {
					// Get a block from the rarest pieces
					for (int i = 0; i < rarestPieces_.size(); i++) {
						piece = rarestPieces_.elementAt(i);
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
									if (blocks.size() >= number) return blocks;
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
						if (peer.hasPiece(piece.index())) {
							if (piece.hasUnrequestedBlock()) {
								block = piece.getUnrequestedBlock();
								if (block != null) {
									downloadablePieces_.removeElement(piece);
									downloadingPieces_.addElement(piece);
									downloadingBitfield_.setBit(piece.index());
									requestedBlocks_.addElement(block);
									
									blocks.add(block);
									if (blocks.size() >= number) return blocks;
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
								if (blocks.size() >= number) return blocks;
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
						if (blocks.size() >= number) return blocks;
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
		String filePath = file.getPath();
		int size = file.getSize();
		
		// Creating the file
		if (path_ != null && !path_.equals("")) {
			
			boolean existed = false;
			if (FileManager.fileExists(filePath)) existed = true;
			
			if (FileManager.freeSize(path_) < size) {
				Log.v(LOG_TAG, "Not enough free space");
				return ERROR_NO_FREE_SIZE;
			}
			
			if (FileManager.createFile(filePath, size)) {
				if (existed) return ERROR_ALREADY_EXISTS;
				return ERROR_NONE;
			}
			
			return ERROR_FILE_HANDLING;
		}
		
		return ERROR_WRONG_CONTENT;
	}
	
	/** 
	 * Writes a block into the file in the given position.
	 * The offset and the length within the block is also given. 
	 */
	public synchronized int writeFile(File file, int filePosition, byte[] block, int offset, int length) {
		return fileManager_.writeFile(file.getPath(), filePosition, block, offset, length);
	}
	
	/** 
	 * Reads a block from the file.
	 * The position and the length within the file is also given. 
	 */
	public synchronized byte[] read(String filepath, int position, int length) {
		return fileManager_.read(filepath, position, length);
	}
	
	/** Adds a new peer to the list of the peers of the torrent. */
	public int addPeer(String address, int port, String peerId) {
        //if (peers_.size() >= Preferences.getMaxConnections()) return ERROR_OVERFLOW;
        if (hasPeer(address, port)) return ERROR_ALREADY_EXISTS;

        Peer peer = new Peer(address, port, peerId, pieces_.size());
        peers_.addElement(peer);
        notConnectedPeers_.addElement(peer);
        //Log.v(LOG_TAG, "Number of peers: " + peers_.size());

        return ERROR_NONE;
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
				if (k < n) n = k;
			}
				
			synchronized (rarestPieces_) {
				rarestPieces_.removeAllElements();
				for (int i = 0; i < downloadablePieces_.size(); i++) {
					piece = downloadablePieces_.elementAt(i);
					int k = piece.getNumberOfPeersHaveThis();
					if (k == n) rarestPieces_.add(piece);
				}
				
				Collections.shuffle(rarestPieces_);
			}
		}
	}
	
	/** Updates the downloaded bytes with the given amount of bytes. */
	public void updateBytesDownloaded(int bytes) {
		bytesDownloaded_ += bytes;
		if (bytes < 0) latestBytesDownloaded_ += bytes;
		bytesLeft_ -= bytes;
		downloadPercent_ = bytesDownloaded_ / (bytesTotal_ / 100.0);
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
		downloadingPieces_.removeElement(piece);

		bitfield_.setBit(piece.index());
		downloadedPieceCount_++;

		// If the download is complete
		//if (downloadedPieceCount_ == pieces_.size()) {
		if (downloadablePieces_.isEmpty() && downloadingPieces_.isEmpty()) {
			complete_ = true;
			//downloadPercent_ = 100;
			status_ = R.string.status_seeding;
			
			if (!calledBySavedTorrent) {
				Log.v(LOG_TAG, "DOWNLOAD COMPLETE");
				if (bitfield_.isFull()) {
					for (int i = 0; i < trackers_.size(); i++) {
						trackers_.elementAt(i).changeEvent(Tracker.EVENT_COMPLETED);
					}
				}
				
				// Disconnect from peers if not interested
				synchronized (connectedPeers_) {
					for (int i = 0; i < connectedPeers_.size(); i++) {
						//if (!connectedPeers_.elementAt(i).isIncommingPeer())
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
	} 
	
	/** Returns the index of the given piece. */
	public int indexOfPiece(Piece piece) {
        return pieces_.indexOf(piece);
    }
	
	/** Returns the number of pieces. */
	public int pieceCount() {
        return pieces_.size();
    }
	
	/** Returns the piece with the given index. */
	public Piece getPiece(int index) {
		return pieces_.elementAt(index);
	}
	
	/** Returns the size of the torrent. */
	public int getSize() {
		return bytesTotal_;
	}

	/** Returns the number of bytes have to be downloaded. */
	public int getBytesLeft() {
        return bytesLeft_;
    }
	
	/** Returns the number of downloaded bytes. */
	public int getBytesDownloaded() {
		return bytesDownloaded_;
	}
	
	/** Returns the number of uploaded bytes. */
	public int getBytesUploaded() {
		return bytesUploaded_;
	}
	
	/** Returns the download speed. */
	public int getDownloadSpeed() {
		return downloadSpeed_.getSpeed();
	}
	
	/** Returns the upload speed. */
	public int getUploadSpeed() {
		return uploadSpeed_;
	}
	
	/** Returns the percent of the downloaded data. */
	public double getProgressPercent() {
		if (status_ != R.string.status_hash_check) {
			return downloadPercent_;
		}
		return downloadPercent_;
	}
	
	/** Returns the info hash as a string. */
	public String getInfoHash() {
		return this.infoHash_;
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
	
	/** Returns whether the torrent is working or not. */ 
	public boolean isWorking() {
		return status_ == R.string.status_downloading || status_ == R.string.status_seeding || status_ == R.string.status_hash_check;
	}
	
	/** Returns the number of seeds. */
	public int getSeeds() {
		return tracker_.getComplete();
	}
	
	/** Returns the number of leechers. */
	public int getLeechers() {
		return tracker_.getIncomplete();
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
		/*Vector<Tracker> trackers = new Vector<Tracker>();
		trackers.add(tracker_);
		for (int i = 0; i < trackerList_.size(); i++) {
			trackers.addAll(trackerList_.get(i));
		}*/
		return trackers_;
	}
		
	/** Returns the remaining time (ms) until the downloading ends. */
	public int getRemainingTime() {
		int speed = getDownloadSpeed();
		if (speed == 0) return -1;
		int remaining = getSize() - getBytesDownloaded();
		double millis = ((double) remaining / (double) speed) * 1000.0; 
		return (int) millis;
	}

	/** Returns the elapsed time (ms) since the torrent has been started. */
	public int getElapsedTime() {
		return elapsedTime_;
	}

	public void updateBytesUploaded(int length) {
		bytesUploaded_ += length;
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
}
