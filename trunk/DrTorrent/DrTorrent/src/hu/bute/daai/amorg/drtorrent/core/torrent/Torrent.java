package hu.bute.daai.amorg.drtorrent.core.torrent;

import hu.bute.daai.amorg.drtorrent.core.Bitfield;
import hu.bute.daai.amorg.drtorrent.core.Block;
import hu.bute.daai.amorg.drtorrent.core.DownloadManager;
import hu.bute.daai.amorg.drtorrent.core.File;
import hu.bute.daai.amorg.drtorrent.core.Metadata;
import hu.bute.daai.amorg.drtorrent.core.Piece;
import hu.bute.daai.amorg.drtorrent.core.Speed;
import hu.bute.daai.amorg.drtorrent.core.UploadManager;
import hu.bute.daai.amorg.drtorrent.core.peer.Peer;
import hu.bute.daai.amorg.drtorrent.core.peer.PeerImpl;
import hu.bute.daai.amorg.drtorrent.core.peer.PeerInfo;
import hu.bute.daai.amorg.drtorrent.core.tracker.Tracker;
import hu.bute.daai.amorg.drtorrent.core.tracker.TrackerHttp;
import hu.bute.daai.amorg.drtorrent.core.tracker.TrackerInfo;
import hu.bute.daai.amorg.drtorrent.core.tracker.TrackerObserver;
import hu.bute.daai.amorg.drtorrent.core.tracker.TrackerUdp;
import hu.bute.daai.amorg.drtorrent.file.FileManager;
import hu.bute.daai.amorg.drtorrent.network.MagnetUri;
import hu.bute.daai.amorg.drtorrent.util.Log;
import hu.bute.daai.amorg.drtorrent.util.Preferences;
import hu.bute.daai.amorg.drtorrent.util.Quantity;
import hu.bute.daai.amorg.drtorrent.util.Tools;
import hu.bute.daai.amorg.drtorrent.util.analytics.Analytics;
import hu.bute.daai.amorg.drtorrent.util.bencode.Bencoded;
import hu.bute.daai.amorg.drtorrent.util.bencode.BencodedDictionary;
import hu.bute.daai.amorg.drtorrent.util.bencode.BencodedInteger;
import hu.bute.daai.amorg.drtorrent.util.bencode.BencodedList;
import hu.bute.daai.amorg.drtorrent.util.bencode.BencodedString;
import hu.bute.daai.amorg.drtorrent.util.sha1.SHA1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.SystemClock;
import android.webkit.URLUtil;

/** Class representing a torrent. */
public class Torrent implements TorrentInfo, Comparable<Torrent>, TrackerObserver {
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

	public final static int STATUS_WRONG_FILE 			= 0;
	public final static int STATUS_STOPPED 				= 10;
	public final static int STATUS_FINISHED 			= 11;
	public final static int STATUS_STOPPED_CREATING		= 12;
	public final static int STATUS_CREATING 			= 20;
	public final static int STATUS_STARTED 				= 21;
	public final static int STATUS_OPENING 				= 22;
	public final static int STATUS_CHECKING_HASH 		= 23;
	public final static int STATUS_DOWNLOADING_METADATA = 30;
	public final static int STATUS_DOWNLOADING 			= 31;
	public final static int STATUS_SEEDING 				= 32;

	private static int ID = 0;
	final protected int id_;

	final protected TorrentManager torrentManager_;
	final private FileManager fileManager_;
	final protected UploadManager uploadManager_;
	final protected DownloadManager downloadManager_;

	protected int status_ = STATUS_STOPPED;
	private boolean isFirstStart_ = true;
	private boolean valid_ = false;

	private String infoHash_;
	private String infoHashString_;
	private byte[] infoHashByteArray_;
	
	private Metadata metadata_ = null;
	private int metadataSize_ = 0;
	protected boolean shouldStart_ = false;

	private boolean isSingleFile_ = true;
	protected String downloadFolder_;
	protected String name_;
	private String path_; // downloadFolder + name
	private int pieceLength_;

	private String comment_ = "";
	private String createdBy_ = "";
	private long creationDate_ = 0;
	
	private long addedOn_ = 0;
	private long completedOn_ = 0;
	private long removedOn_ = 0;

	private int seeds_ = 0;
	private int leechers_ = 0;

	private long fullSize_ = 0; 			// Size of the full torrent
	protected long activeSize_ = 0; 			// Size of the selected files (pieces)
	protected long activeDownloadedSize_ = 0; // Size of the selected downloaded files (pieces)
	private long checkedSize_ = 0; 			// Size of the checked bytes (during hash check)
	private long downloadedSize_ = 0; 		// Size of the downloaded bytes
	private long uploadedSize_ = 0; 		// Size of the uploaded bytes
	private Speed downloadSpeed_;
	private Speed uploadSpeed_;
	final private Vector<File> files_;
	final private Vector<Piece> pieces_;
	final private Vector<Piece> downloadablePieces_;
	final private Vector<Piece> rarestPieces_;
	final private Vector<Piece> downloadingPieces_;
	final private Vector<Block> requestedBlocks_;
	private Bitfield bitfield_;
	private Bitfield downloadingBitfield_;
	private Bitfield activeBitfield_;

	private boolean wasStreamingMode_ = true;
	private long elapsedTime_ = 0;
	private long downloadingTime_ = 0;
	private long seedingTime_ = 0;
	private long lastTime_ = 0;

	final private Vector<Peer> peers_;
	final private Vector<Peer> connectedPeers_;
	final private Vector<Peer> notConnectedPeers_;
	final private Vector<Peer> interestedPeers_;
	final private Vector<Tracker> trackers_;

	private boolean isEndGame_ = false;
	
	private ThreadPoolExecutor peerTPE_ = null;

	/** Constructor with the manager and the torrent file as a parameter. */
	public Torrent(final TorrentManager torrentManager, final UploadManager uploadManager, final DownloadManager downloadManager, final String filePath, final String downloadPath) {
		torrentManager_ = torrentManager;
		uploadManager_ = uploadManager;
		downloadManager_ = downloadManager;
		fileManager_ = new FileManager(this);
		downloadFolder_ = downloadPath;
		if (downloadFolder_.equals("/")) {
			downloadFolder_ = "";
		}

		files_ = new Vector<File>();
		pieces_ = new Vector<Piece>();
		downloadablePieces_ = new Vector<Piece>();
		rarestPieces_ = new Vector<Piece>();
		downloadingPieces_ = new Vector<Piece>();
		requestedBlocks_ = new Vector<Block>();
		peers_ = new Vector<Peer>();
		connectedPeers_ = new Vector<Peer>();
		notConnectedPeers_ = new Vector<Peer>();
		interestedPeers_ = new Vector<Peer>();
		trackers_ = new Vector<Tracker>();
		downloadSpeed_ = new Speed();
		uploadSpeed_ = new Speed();
		
		addedOn_ = System.currentTimeMillis();

		id_ = ++ID;
	}
	
	/** Constructor with creating torrent as a parameter. */
	public Torrent(CreatingTorrent creatingTorrent) {
		torrentManager_ = creatingTorrent.torrentManager_;
		uploadManager_ = creatingTorrent.uploadManager_;
		downloadManager_ = creatingTorrent.downloadManager_;
		fileManager_ = new FileManager(this);
		downloadFolder_ = creatingTorrent.downloadFolder_;
		if (downloadFolder_.equals("/")) {
			downloadFolder_ = "";
		}

		files_ = new Vector<File>();
		pieces_ = new Vector<Piece>();
		downloadablePieces_ = new Vector<Piece>();
		rarestPieces_ = new Vector<Piece>();
		downloadingPieces_ = new Vector<Piece>();
		requestedBlocks_ = new Vector<Block>();
		peers_ = new Vector<Peer>();
		connectedPeers_ = new Vector<Peer>();
		notConnectedPeers_ = new Vector<Peer>();
		interestedPeers_ = new Vector<Peer>();
		trackers_ = new Vector<Tracker>();
		downloadSpeed_ = new Speed();
		uploadSpeed_ = new Speed();
		
		addedOn_ = System.currentTimeMillis();

		id_ = creatingTorrent.id_;
	}

	/**
	 * Processes the bencoded torrent.
	 * 
	 * @param torrentBencoded The torrent file's bencoded content.
	 * @return Error code.
	 */
	public int processBencodedTorrent(Bencoded torrentBencoded) {
		status_ = STATUS_OPENING;

		if (torrentBencoded == null || torrentBencoded.type() != Bencoded.BENCODED_DICTIONARY) {
			return ERROR_WRONG_CONTENT; // bad bencoded torrent
		}

		BencodedDictionary torrent = (BencodedDictionary) torrentBencoded;
		Bencoded tempBencoded = null;

		trackers_.removeAllElements();
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

		// comment
		tempBencoded = torrent.entryValue("comment");
		if (tempBencoded != null && tempBencoded.type() == Bencoded.BENCODED_STRING) {
			comment_ = ((BencodedString) tempBencoded).getStringValue();
		}

		// created by
		tempBencoded = torrent.entryValue("created by");
		if (tempBencoded != null && tempBencoded.type() == Bencoded.BENCODED_STRING) {
			createdBy_ = ((BencodedString) tempBencoded).getStringValue();
		}

		// creation date
		tempBencoded = torrent.entryValue("creation date");
		if (tempBencoded != null && tempBencoded.type() == Bencoded.BENCODED_INTEGER) {
			creationDate_ = ((BencodedInteger) tempBencoded).getValue();
		}
		
		// info
		tempBencoded = torrent.entryValue("info");
		if (tempBencoded == null || tempBencoded.type() != Bencoded.BENCODED_DICTIONARY) {
			return ERROR_WRONG_CONTENT;
		}

		BencodedDictionary info = (BencodedDictionary) tempBencoded;
		
		SHA1 sha1 = new SHA1();
		byte[] metadata = info.Bencode();
		metadataSize_ = metadata.length;
		Log.v(LOG_TAG, "metadata size: " + Tools.bytesToString(metadata.length) + " (" + metadata.length + ")");
		sha1.update(metadata);
		int[] hashResult = sha1.digest();
		infoHash_ = SHA1.resultToByteString(hashResult);

		infoHashByteArray_ = SHA1.resultToByte(hashResult);
		infoHashString_ = SHA1.resultToString(hashResult);
		Log.v(LOG_TAG, "Infohash of the torrrent: " + infoHashString_);
		hashResult = null;
		
		if (torrentManager_.hasTorrent(infoHash_)) {
			return ERROR_ALREADY_EXISTS;
		}
		
		return processBencodedMetadata(info);
	}
	
	
	/** Processes the Magnet Link. */
	public int processMagnetTorrent(MagnetUri magnetUri) {
		status_ = STATUS_OPENING;
		
		infoHashString_ = magnetUri.getInfoHash();
		if (infoHashString_ == null || infoHashString_.length() != 40) {
			return ERROR_WRONG_CONTENT;
		}
		
		int[] hashResult = SHA1.stringToIntArray(infoHashString_);
		infoHash_ = SHA1.resultToByteString(hashResult);
		
		infoHashByteArray_ = SHA1.resultToByte(hashResult);
		//hashResult = null;
		
		infoHashString_ = infoHashString_.toUpperCase(Locale.ENGLISH);
		Log.v(LOG_TAG, "xt = " + infoHashString_);
		Log.v(LOG_TAG, "xt = " + SHA1.resultToString(hashResult));
		
		name_ = magnetUri.getName();
		if (name_ != null && !name_.equals("")) {
			Log.v(LOG_TAG, "dn = " + name_);
		} else {
			name_ = infoHashString_;
		}
		
		List<String> trackers = magnetUri.getTrackers();
		trackers_.removeAllElements();
		for (String announceURL : trackers) {
			Log.v(LOG_TAG, "tr = " + announceURL);
			addTracker(announceURL);
		}
		
		if (torrentManager_.hasTorrent(infoHash_)) {
			return ERROR_ALREADY_EXISTS;
		}
		
		return ERROR_NONE;
	}
	
	/** Processes the metadata from external source. */
	public int processMetadata(byte[] metadata) {
		metadataSize_ = metadata.length;
		Bencoded info = null;
		try {
			info = Bencoded.parse(metadata);
		} catch (Exception e) {
			Log.v(LOG_TAG, "Error occured while processing metadata " + e.getMessage());
			return ERROR_WRONG_CONTENT;
		}
		
		if (info == null || info.type() != Bencoded.BENCODED_DICTIONARY) {
			return ERROR_WRONG_CONTENT;
		}
		
		return processBencodedMetadata((BencodedDictionary) info);
	}
	
	/** Processes the metadata. */
	public int processBencodedMetadata(BencodedDictionary info) {
		status_ = STATUS_OPENING;
		
		Bencoded tempBencoded = null;
		
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

			isSingleFile_ = false;
			name_ = ((BencodedString) tempBencoded).getStringValue();
			path_ = downloadFolder_ + "/" + name_ + "/";

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

				files_.addElement(new File(this, i, (int) (fileBegin / pieceLength_), /*path_,*/ pathBuf, length));
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
			path_ = downloadFolder_ + "/";

			files_.addElement(new File(this, 0, 0, /*path_,*/ name_, length));
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
		}
		
		Quantity pieceLength = new Quantity(pieceLength_, Quantity.SIZE);
		Log.v(LOG_TAG, "Pieces: " + pieceCount + " size: " + pieceLength);

		bitfield_ = new Bitfield(pieces_.size(), false);
		downloadingBitfield_ = new Bitfield(pieces_.size(), false);
		activeBitfield_ = new Bitfield(pieces_.size(), false);

		calculateFileFragments();
		valid_ = true;

		return ERROR_NONE;
	}

	/** Sets the active pieces according to the selected file list of the torrent. */
	protected void setTorrentActivePieces() {
		synchronized (downloadablePieces_) {
			synchronized (downloadingPieces_) {
				activeSize_ = 0;
				activeDownloadedSize_ = 0;
				activeBitfield_ = new Bitfield(pieces_.size(), false);
				Piece piece;
				for (int i = 0; i < pieces_.size(); i++) {
					piece = pieces_.get(i);
					piece.calculatePriority();
					if (piece.priority() > File.PRIORITY_SKIP) {
						activeSize_ += piece.size();
						activeDownloadedSize_ += piece.downloaded();
						if (!bitfield_.isBitSet(i)) {
							activeBitfield_.setBit(piece.index());
							if (!downloadablePieces_.contains(piece) && !downloadingPieces_.contains(piece)) {
								downloadablePieces_.addElement(piece);

								if (status_ == STATUS_SEEDING) {
									status_ = STATUS_DOWNLOADING;
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
		wasStreamingMode_ = Preferences.isStreamingMode();
		if (wasStreamingMode_) {
			Collections.sort(downloadablePieces_, new Piece.PieceSequenceAndPriorityComparator());
			rarestPieces_.removeAllElements();
		} else {
			Collections.shuffle(downloadablePieces_);
			Collections.sort(downloadablePieces_, new Piece.PiecePriorityComparator());
		}

		if (isComplete()) {
			if (status_ == STATUS_DOWNLOADING)
				status_ = STATUS_SEEDING;
		} else {
			if (status_ == STATUS_SEEDING)
				status_ = STATUS_DOWNLOADING;
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
			if (!lastPiece.hasFile(file) && lastPiece.index() > 0) {
				lastPiece = pieces_.elementAt(lastPiece.index() - 1);
			}
		}
		
		for (int j = file.index() - 1; j >= 0; j--) {
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
		status_ = STATUS_CHECKING_HASH;

		for (final File file : files_) {
			if (file.getPriority() > 0) {
				int result = createFile(file);
				if (result == ERROR_ALREADY_EXISTS) {
					file.checkHash(true);
				} else {
					file.checkHash(false);
				}
			}
			if (status_ == STATUS_STOPPED) {
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
		if (pieces_.isEmpty()) {
			return;
		}
		int pieceIndex = 0;
		Piece piece = pieces_.elementAt(pieceIndex);
		int piecePos = 0;

		File file;
		for (int i = 0; i < files_.size(); i++) {
			file = (File) files_.elementAt(i);
			// If the file is NOT BIGGER than the remaining part of the piece...
			if ((long) piecePos + file.getSize() <= piece.size()) {
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
				long filePos = 0;
				while (filePos < file.getSize()) {
					// If the remaining part of the file is BIGGER than the
					// remaining part of the piece...
					if ((file.getSize() - filePos + (long) piecePos) > piece.size()) {
						piece.addFileFragment(file, filePos, piece.size() - piecePos);
						filePos += piece.size() - (long) piecePos;

						if (pieces_.size() > pieceIndex + 1) {
							piece = pieces_.elementAt(++pieceIndex);
							piecePos = 0;
						}
						// ...if SMALLER or EQUALS
					} else {
						piece.addFileFragment(file, filePos, (int) (file.getSize() - filePos));
						piecePos += (int) (file.getSize() - filePos);

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

	public void startSeeding() {
		setTorrentActivePieces();
	
		if (valid_) {
			bitfield_ = new Bitfield(bitfield_.getLengthInBits(), true);
			for (int i = 0; i < pieceCount(); i++) {
				Piece piece = pieces_.get(i);
				onPieceDownloaded(piece, true);
				piece.setComplete();
			}
			
			isFirstStart_ = false;
			if (shouldStart_) {
				status_ = STATUS_SEEDING;
			} else {
				status_ = STATUS_STOPPED;
			}
		}
		
		if (shouldStart_) {
			start();
		}
	}
	
	/** Starts the torrent. */
	public void start() {
		lastTime_ = SystemClock.elapsedRealtime();

		if (valid_) {
			if (isFirstStart_) {
				setTorrentActivePieces();
				checkHash();

				if (status_ == STATUS_STOPPED) {
					return;
				}
				
				isFirstStart_ = false;
			}
			createFiles();
		}

		if (peerTPE_ == null) {
			downloadSpeed_ = new Speed();
			uploadSpeed_ = new Speed();
			
			lastOptimisticUnchokingTime_ = lastUnchokingTime_ = 0;

			peerTPE_ = new ThreadPoolExecutor(
					Preferences.getMaxConnectedPeers(), Preferences.getMaxConnectedPeers(),
					30, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(5)
			);
			
			notConnectedPeers_.removeAllElements();

			Peer peer;
			for (int i = 0; i < peers_.size(); i++) {
				peer = peers_.elementAt(i);
				peer.resetTcpTimeoutCount();
				notConnectedPeers_.addElement(peer);
			}
		}

		if (valid_) {
			if (isComplete()) {
				status_ = STATUS_SEEDING;
			} else {
				status_ = STATUS_DOWNLOADING;
			}
		} else {
			status_ = STATUS_DOWNLOADING_METADATA;
		}
		
		if (torrentManager_.isEnabled()) {
			Tracker tracker;
			for (int i = 0; i < trackers_.size(); i++) {
				tracker = trackers_.elementAt(i);
				if (tracker.getStatus() == Tracker.STATUS_FAILED || tracker.getStatus() == Tracker.STATUS_UNKNOWN) {
					tracker.changeEvent(Tracker.EVENT_STARTED);
				}
			}
		}
	}
	
	/** Resumes the torrent when network connection is established. */
	public void resume() {
		Log.v(LOG_TAG, "Resuming...");
		Peer peer;
		for (int i = 0; i < notConnectedPeers_.size(); i++) {
			peer = notConnectedPeers_.elementAt(i);
			peer.resetTcpTimeoutCount();
		}
		peer = null;
		
		lastTime_ = SystemClock.elapsedRealtime();
		
		Tracker tracker;
		for (int i = 0; i < trackers_.size(); i++) {
			tracker = trackers_.elementAt(i);
			if (tracker.getStatus() == Tracker.STATUS_FAILED || tracker.getStatus() == Tracker.STATUS_UNKNOWN) {
				tracker.resetLastRequest();
			}
		}
		tracker = null;
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
				connectedPeers_.elementAt(i).disconnect();
			}
			connectedPeers_.removeAllElements();
			notConnectedPeers_.removeAllElements();
			interestedPeers_.removeAllElements();
			
			if (peerTPE_ != null) {
				peerTPE_.shutdown();
				peerTPE_ = null;
			}
		}
		lastTime_ = 0;
		status_ = STATUS_STOPPED;
	}

	/** Removes a disconnected peer from the array of connected peers. */
	public void peerDisconnected(Peer peer) {
		connectedPeers_.removeElement(peer);
		interestedPeers_.removeElement(peer);
		if (peer.getTcpTimeoutCount() >= 3) {
			peers_.removeElement(peer);
		} else {
			if (!notConnectedPeers_.contains(peer)) {
				notConnectedPeers_.addElement(peer);
			}
		}
	}

	/** Adds the given peer to the interested peers. */
	public void onPeerInterested(Peer peer) {
		if (!interestedPeers_.contains(peer))
			interestedPeers_.addElement(peer);
	}

	/** Removes the given peer from the interested peers. */
	public void onPeerNotInterested(Peer peer) {
		interestedPeers_.removeElement(peer);
	}

	private long latestBytesDownloaded_ = 0;
	private long latestBytesUploaded_ = 0;

	/** Scheduler method. Schedules the peers and the trackers. */
	public void onTimer() {
		if (lastTime_ != 0) {
			long currentTime = SystemClock.elapsedRealtime();
			
			if (torrentManager_.isEnabled()) {
				int diff = (int) (currentTime - lastTime_);
			
				if (downloadingTime_ > -1) {
					if (status_ == STATUS_DOWNLOADING) {
						downloadingTime_ += diff;
					} else if (status_ == STATUS_SEEDING) {
						seedingTime_ += diff;
					}
				}
			
				elapsedTime_ += diff;
			}
			
			lastTime_ = currentTime;
		}
		downloadSpeed_.addBytes(downloadedSize_ - latestBytesDownloaded_, lastTime_);
		uploadSpeed_.addBytes(uploadedSize_ - latestBytesUploaded_, lastTime_);
		latestBytesDownloaded_ = downloadedSize_;
		latestBytesUploaded_ = uploadedSize_;

		if (isConnected() && torrentManager_.isEnabled()) {
			canCalculateRarestPieces_ = true;
			/*Log.v(LOG_TAG, name_ +  
					" - peers: " + connectedPeers_.size() + " Blocks: " + requestedBlocks_.size() + " Downloadable: " + downloadablePieces_.size()
							+ " Downloading: " + downloadingPieces_.size());*/

			//Log.v(LOG_TAG, "onTimer: are there peers?");
			// Updates the peers
			if (!connectedPeers_.isEmpty()) {
				final Peer[] peers;
				//Log.v(LOG_TAG, "onTimer: get peersx");
				synchronized (connectedPeers_) {
					peers = new Peer[connectedPeers_.size()];
					connectedPeers_.copyInto(peers);
				}
				//Log.v(LOG_TAG, "onTimer: iterate peers");
				for (int i = 0; i < peers.length; i++) {
					peers[i].onTimer();
				}
				//Log.v(LOG_TAG, "onTimer: finished with peers");
			}
				
			if (valid_) {
				if (Preferences.isUploadEnabled()) {
					chokeAlgorithm();
				}
			}

			// Updates the trackers
			for (int i = 0; i < trackers_.size(); i++) {
				trackers_.elementAt(i).onTimer();
			}

			// Connects to peers
			if (status_ == STATUS_DOWNLOADING || status_ == STATUS_DOWNLOADING_METADATA) {
				try {
					int activeCount = peerTPE_.getActiveCount();
					
					for (int i = 0; i < notConnectedPeers_.size() && peerTPE_.getActiveCount() < peerTPE_.getCorePoolSize() && activeCount < peerTPE_.getCorePoolSize() && connectedPeers_.size() < peerTPE_.getCorePoolSize(); i++) {
						final Peer peer = notConnectedPeers_.elementAt(i);
						if (peer.canConnect()) {
							connectedPeers_.addElement(peer);
							notConnectedPeers_.removeElement(peer);
							
							Runnable command = new Runnable() {
								@Override
								public void run() {
									peer.connect(Torrent.this);
								}
							};
							peerTPE_.execute(command);
							i--;
							activeCount++;
						}
					}
				} catch (Exception e) {}
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
		if (!(lastUnchokingTime_ + 10000.0 < currentTime || lastOptimisticUnchokingTime_ + 30000.0 < currentTime)) {
			return;
		}

		if (interestedPeers_.size() > 0) {
			final ArrayList<Peer> peersToUnchoke = new ArrayList<Peer>();

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
					Peer peer;
					for (int i = 0; i < interestedPeers_.size(); i++) {
						peer = interestedPeers_.elementAt(i);

						int speed = peer.getDownloadSpeed();
						boolean isFound = false;
						if (speed > 0 && peer != optimisticUnchokedPeer_) {
							for (int j = 0; j < peersToUnchoke.size(); j++) {
								if (peersToUnchoke.get(j).getDownloadSpeed() > speed) {
									peersToUnchoke.add(j, peer);
									isFound = true;
									break;
								}
							}
							if (!isFound) {
								peersToUnchoke.add(peer);
							}
						}
					}
					peer = null;

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
					peer = null;
				}
			}
		} else {
			optimisticUnchokedPeer_ = null;
		}
	}

	/** Returns a downloadable metadata block for the given peer. */
	public int getMetadataBlockToDownload(Peer peer) {
		if (metadata_ != null) {
			synchronized (metadata_) {
				if (metadata_ != null && metadata_.hasUnrequestedBlock()) {
					for (int i = 0; i < metadata_.getBlockCount(); i++) {
						if (!metadata_.isRequested(i) && !peer.isMetadataBlockRejected(i)) {
							metadata_.setRequested(i, true);
							return i;
						}
					}
				}
			}
		}
		
		return -1;
	}
	
	/** Returns a downloadable block for the given peer. */
	public synchronized ArrayList<Block> getBlocksToDownload(final Peer peer, int number) {
		final ArrayList<Block> blocks = new ArrayList<Block>();
		if (downloadManager_.getDownloadSpeed() >= Preferences.getDownloadSpeedLimit() ||
			downloadManager_.getLatestDownloadedBytes() >= Preferences.getDownloadSpeedLimit()) {
			
			///Log.v(LOG_TAG, "Can't request: " + downloadManager_.getDownloadSpeed() + " " + downloadManager_.getLatestDownloadedBytes() + " " + Preferences.getDownloadSpeedLimit());
			return blocks;
		}
		
		Block block = null;
		Piece piece = null;

		if (downloadablePieces_.size() > 0) {
			// NORMAL MODE

			if (Preferences.isStreamingMode()) {
				// STREAMING MODE
				if (!wasStreamingMode_) {
					Collections.sort(downloadablePieces_, new Piece.PieceSequenceAndPriorityComparator());
					wasStreamingMode_ = true;
					rarestPieces_.removeAllElements();
				}
			} else {
				// RAREST/RANDOM PIECE FIRST MODE
				if (wasStreamingMode_) {
					Collections.shuffle(downloadablePieces_);
					Collections.sort(downloadablePieces_, new Piece.PiecePriorityComparator());
					wasStreamingMode_ = true;
				}
			}
				
			synchronized (downloadingPieces_) {
				// Get a block from the downloading pieces
				for (int i = 0; i < downloadingPieces_.size(); i++) {
					piece = downloadingPieces_.elementAt(i);
					if (!piece.canDownload()) {
						continue;
					}
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

			if (!wasStreamingMode_) {
				if ((rarestPieces_.size() == 0 && downloadablePieces_.size() != 0) ||
					(isChangedPeersHavingPiece_ && canCalculateRarestPieces_)) {
					calculateRarestPieces();
				}
				synchronized (rarestPieces_) {
					// Get a block from the rarest pieces
					for (int i = 0; i < rarestPieces_.size(); i++) {
						piece = rarestPieces_.elementAt(i);
						if (!piece.canDownload()) {
							//Log.v(LOG_TAG, "0Cant download");
							continue;
						}
						//Log.v(LOG_TAG, "0Can download");
						if (peer.hasPiece(piece.index())) {
							//Log.v(LOG_TAG, "0Can download has peer");
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
						} /*else {
							Log.v(LOG_TAG, "0Can't download hasnt peer");
						}*/
					}
				}
			}

			synchronized (downloadablePieces_) {
				// Get a block from the downloadable pieces
				for (int i = 0; i < downloadablePieces_.size(); i++) {
					piece = downloadablePieces_.elementAt(i);
					if (!piece.canDownload()) {
						//Log.v(LOG_TAG, "Cant download");
						continue;
					}
					//Log.v(LOG_TAG, "Can download");
					if (peer.hasPiece(piece.index())) {
						//Log.v(LOG_TAG, "Can download has peer");
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
					} /*else {
						Log.v(LOG_TAG, "Can't download hasnt peer");
					}*/
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
	private int createFile(final File file) {
		final String filePath = file.getFullPath();
		final long size = file.getSize();

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
	
	/** Returns the metadata block at the given position. */
	public byte[] readMetadataBlock(final int piece) {
		try {
			byte[] torrentContent = torrentManager_.readTorrentContent(infoHashString_);
			Bencoded bencoded = Bencoded.parse(torrentContent);
			torrentContent = null;
			if (bencoded != null && bencoded.type() == Bencoded.BENCODED_DICTIONARY) {
				bencoded = ((BencodedDictionary) bencoded).entryValue("info");
				if (bencoded != null && bencoded.type() == Bencoded.BENCODED_DICTIONARY) {
					final byte[] metadata = ((BencodedDictionary) bencoded).Bencode();
					int size = Metadata.BLOCK_SIZE;
					if ((piece + 1) * Metadata.BLOCK_SIZE > metadata.length) {
						size = metadata.length - piece * Metadata.BLOCK_SIZE;
					}
					final byte[] data = new byte[size];
					System.arraycopy(metadata, piece * Metadata.BLOCK_SIZE, data, 0, data.length);
					return data;
				}
			}
		} catch (Exception e) {
		}
		return null;
	}

	/** Adds a new peer to the list of the peers of the torrent. <br> 
	 * (Used by the Torrent Manager & trackers.) */
	@Override
	public int addPeer(final String address, final int port, final String peerId) {
		// if (peers_.size() >= Preferences.getMaxConnections()) return
		// ERROR_OVERFLOW;
		if (hasPeer(address, port))
			return ERROR_ALREADY_EXISTS;

		Peer peer = new PeerImpl(address, port, peerId, pieces_.size());
		peers_.addElement(peer);
		notConnectedPeers_.addElement(peer);
		// Log.v(LOG_TAG, "Number of peers: " + peers_.size());

		return ERROR_NONE;
	}
	
	/** Adds a new tracker to the list of the trackers. */
	public void addTracker(final String url) {
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

	/** Updates the given tracker from the tracker list. */
	public void updateTracker(final int trackerId) {
		Tracker tracker;
		for (int i = 0; i < trackers_.size(); i++) {
			tracker = trackers_.elementAt(i);
			if (tracker.getId() == trackerId) {
				tracker.resetLastRequest();
				return;
			}
		}
	}
	
	/** Removes the given tracker from the tracker list. */
	public void removeTracker(final int trackerId) {
		Tracker tracker;
		for (int i = 0; i < trackers_.size(); i++) {
			tracker = trackers_.elementAt(i);
			if (tracker.getId() == trackerId) {
				trackers_.removeElement(tracker);
				return;
			}
		}
	}
	
	/** Adds an incoming peer to the array of (connected) peers. */
	public void addIncomingPeer(final Peer peer) {
		peers_.addElement(peer);
		if (peerTPE_ != null && connectedPeers_.size() < ((double) peerTPE_.getCorePoolSize()) * 1.2) {
			connectedPeers_.addElement(peer);
		} else {
			peer.disconnect();
		}
	}

	/** Returns whether there is a peer with the given IP and port in the list of peers or not. */
	public boolean hasPeer(final String address, final int port) {
		Peer tempPeer;
		for (int i = 0; i < peers_.size(); i++) {
			tempPeer = peers_.elementAt(i);
			if (tempPeer.getAddress().equals(address) && tempPeer.getPort() == port)
				return true;
		}
		return false;
	}
	
	/** Returns whether there is a tracker with the given URL in the list of trackers or not. */
	public boolean hasTracker(final String url) {
		Tracker tracker;
		for (int i = 0; i < trackers_.size(); i++) {
			tracker = trackers_.elementAt(i);
			if (tracker.getUrl().equals(url)) {
				return true;
			}
		}
		return false;
	}

	boolean isChangedPeersHavingPiece_ = false;
	boolean canCalculateRarestPieces_ = false;
	
	/** Increments the number of peers having the given piece. */
	public void incNumberOfPeersHavingPiece(final int index) {
		pieces_.elementAt(index).incNumberOfPeersHaveThis();
		
		isChangedPeersHavingPiece_ = true;
	}

	/** Decrements the number of peers having the given piece. */
	public void decNumberOfPeersHavingPiece(final int index) {
		pieces_.elementAt(index).decNumberOfPeersHaveThis();
		
		isChangedPeersHavingPiece_ = true;
	}

	/** Calculates the rarest pieces. */
	public void calculateRarestPieces() {
		canCalculateRarestPieces_ = false;
		isChangedPeersHavingPiece_ = false;
		
		Piece piece = null;
		int n = Integer.MAX_VALUE;

		synchronized (rarestPieces_) {
			synchronized (downloadablePieces_) {
				for (int i = 0; i < downloadablePieces_.size(); i++) {
					piece = downloadablePieces_.elementAt(i);
					int k = piece.getNumberOfPeersHaveThis();
					if (k < n && k > 0) {
						n = k;
					}
				}

				rarestPieces_.removeAllElements();
				for (int i = 0; i < downloadablePieces_.size(); i++) {
					piece = downloadablePieces_.elementAt(i);
					int k = piece.getNumberOfPeersHaveThis();
					if (k == n) {
						rarestPieces_.add(piece);
					}
				}
			}
		}
	}

	/** Updates the downloaded bytes with the given amount of bytes. */
	public void updateBytesDownloaded(final int bytes) {
		if (bytes >= 0) {
			downloadedSize_ += bytes;
			downloadManager_.addBytes(bytes);
		}
		
		activeDownloadedSize_ += bytes;
	}

	/** Updates the uploaded bytes. */
	public void updateBytesUploaded(final int bytes) {
		uploadedSize_ += bytes;
	}
	
	/** Adds a block to the Upload Manager to upload. */
	public void addBlockToUpload(final Block block, final Peer peer) {
		uploadManager_.addBlock(block, peer);
	}

	/** Called after a block has been refused to download (disconnected, choked etc.). */
	public void blockNotRequested(final Block block) {
		requestedBlocks_.removeElement(block);
	}

	/** Called after a block has been downloaded. */
	public void onBlockDownloaded(final Block block) {
		requestedBlocks_.removeElement(block);
		if (isEndGame_) {
			if (!connectedPeers_.isEmpty()) {
				final Peer[] peers;
				synchronized (connectedPeers_) {
					peers = new Peer[connectedPeers_.size()];
					connectedPeers_.copyInto(peers);
				}
				for (int i = 0; i < peers.length; i++) {
					if (peers[i].hasBlock(block)) {
						peers[i].cancelBlock(block);
					}
				}
			}
		}
	}

	/** Called after a piece has been downloaded. */
	public synchronized void onPieceDownloaded(final Piece piece, final boolean calledBySavedTorrent) {
		if (calledBySavedTorrent) {
			downloadablePieces_.removeElement(piece);
		} else {
			downloadingPieces_.removeElement(piece);
			Analytics.newGoodPiece(this);
		}

		bitfield_.setBit(piece.index());
		activeBitfield_.unsetBit(piece.index());
		//downloadedPieceCount_++;

		if (calledBySavedTorrent) {
			activeDownloadedSize_ += piece.size(); //updateBytesDownloaded(piece.size());
			piece.addFilesDownloadedBytes(true);
		}

		// If the download is complete
		// if (downloadedPieceCount_ == pieces_.size()) {
		if (isComplete()) {
			activeDownloadedSize_ = activeSize_;	// download percent = 100%
			status_ = STATUS_SEEDING;

			if (!calledBySavedTorrent) {
				Log.v(LOG_TAG, "DOWNLOAD COMPLETE");
				
				completedOn_ = System.currentTimeMillis();
				Analytics.saveCompletedOn(this);
				Analytics.saveSizeAndTimeInfo(this);
				
				torrentManager_.showCompletedNotification(this);
				if (bitfield_.isFull()) {
					for (int i = 0; i < trackers_.size(); i++) {
						trackers_.elementAt(i).changeEvent(Tracker.EVENT_COMPLETED);
					}
				}
			}
		}

		// Notify peers about having a new piece
		if (!calledBySavedTorrent) {
			if (!connectedPeers_.isEmpty()) {
				final Peer[] peers;
				synchronized (connectedPeers_) {
					peers = new Peer[connectedPeers_.size()];
					connectedPeers_.copyInto(peers);
				}
				for (int i = 0; i < peers.length; i++) {
					peers[i].notifyThatClientHavePiece(piece.index());
				}
			}
		}
	}

	/** Called when the downloaded piece is wrong. */
	public void onPieceHashFailed(final Piece piece) {
		/*synchronized (downloadingPieces_) {*/
			downloadingPieces_.removeElement(piece);
			downloadingBitfield_.unsetBit(piece.index());
			rarestPieces_.removeElement(piece);
			downloadablePieces_.addElement(piece);
		/*}*/
		updateBytesDownloaded(-piece.size());

		piece.addFilesDownloadedBytes(false);
		
		Analytics.newBadPiece(this);
	}

	/** Returns whether the downloading is complete or not. */
	public boolean isComplete() {
		return (downloadablePieces_.isEmpty() && downloadingPieces_.isEmpty());
	}

	/** Returns the index of the given piece. */
	public int indexOfPiece(final Piece piece) {
		return pieces_.indexOf(piece);
	}

	/** Returns the piece with the given index. */
	public Piece getPiece(final int index) {
		return pieces_.elementAt(index);
	}

	/** Returns the info hash as a string. */
	@Override
	public String getInfoHash() {
		return this.infoHash_;
	}

	/** Returns the info hash. */
	@Override
	public byte[] getInfoHashByteArray() {
		return infoHashByteArray_;
	}

	/** Returns the manager of the torrent. */
	public TorrentManager getTorrentManager() {
		return this.torrentManager_;
	}

	/** Refreshes the count of leechers & seeds. */
	@Override
	public void refreshSeedsAndLeechersCount() {
		int seeds = 0;
		int leechers = 0;
		Tracker tracker;
		for (int i = 0; i < trackers_.size(); i++) {
			tracker = trackers_.elementAt(i);
			if (tracker.getComplete() > seeds) {
				seeds = tracker.getComplete();
			}
			if (tracker.getIncomplete() > leechers) {
				leechers = tracker.getIncomplete();
			}
		}
		seeds_ = seeds;
		leechers_ = leechers;
	}

	public void setRemovedOn() {
		removedOn_ = System.currentTimeMillis();
	}

	/** Return whether the bitfield has changed since the latest check or not. */
	public boolean isBitfieldChanged() {
		return bitfield_ != null && (bitfield_.isChanged() || downloadingBitfield_.isChanged());
	}

	/** Sets whether the bitfield has changed or not. */
	public void setBitfieldChanged(final boolean isChanged) {
		bitfield_.setChanged(isChanged);
		downloadingBitfield_.setChanged(isChanged);
	}

	/** Returns the size of the metadata. */
	public int getMetadataSize() {
		return metadataSize_;
	}
	
	/** Sets the size of the metadata. */
	public void setMetadataSize(final int size) {
		if (metadata_ == null) {
			metadata_ = new Metadata(size);
		}
	}
	
	/** Adds the downloaded block to the metadata. */
	public void addBlockToMetadata(final int index, final byte[] data) {
		synchronized (metadata_) {
			String filePath = Preferences.getExternalFilesDir() + "/" + infoHashString_ + ".dat";
			FileManager.write(filePath, index * Metadata.BLOCK_SIZE, data);
			metadata_.add(index/*, data*/);
			if (metadata_.isComplete()) {
				final SHA1 sha1 = new SHA1();
				byte[] content = null;
				for (int processedLength = 0; processedLength < metadata_.getSize();) {
					int length = Metadata.BLOCK_SIZE;
					if (processedLength + Metadata.BLOCK_SIZE > metadata_.getSize()) {
						length = metadata_.getSize() - processedLength;
					}
					if (content == null || content.length != length) {
						content = new byte[length];
					}
					FileManager.read(filePath, processedLength, content);
					sha1.update(content);
					processedLength += length;
				}
				content = null;
				final String infoHash = SHA1.resultToByteString(sha1.digest());
				metadataSize_ = metadata_.getSize();
	
				if (!infoHash.equals(infoHash_)) {
					metadata_ = new Metadata(metadata_.getSize());
					return;
				}
				
				content = new byte[metadata_.getSize()];
				FileManager.read(filePath, 0, content);
				final Bencoded bencoded = Bencoded.parse(content);
				if (bencoded != null && bencoded.type() == Bencoded.BENCODED_DICTIONARY) {
					final BencodedDictionary info = (BencodedDictionary) bencoded;
					processBencodedMetadata(info);
					if (shouldStart_) {
						Analytics.changeTorrent(this);
						start();
					} else {
						torrentManager_.updateMetadata(this);
					}
				}
				
				torrentManager_.saveTorrentMetadata(this, content);
				FileManager.removeFile(filePath);
				metadata_ = null;
			}
		}
	}
	
	/** The torrent should start after getting the metadata. */
	public void shouldStart() {
		shouldStart_ = true;
	}
	
	/** Cancels the requested block of the metadata. */
	public void cancelMetadataBlock(final int index) {
		metadata_.setRequested(index, false);
	}
	
	public void setDownloadFolder(String downloadFolder) {
		downloadFolder_ = downloadFolder;
		
		if (downloadFolder_.equals("/")) {
			downloadFolder_ = "";
		}
		
		if (!isSingleFile_) {
			path_ = downloadFolder_ + "/" + name_ + "/";
		} else {
			path_ = downloadFolder_ + "/";
		}
	}
	
	/** Removes the downloaded files of the torrent. (Call only when closing torrent and content should be removed!!!) */
	public void removeFiles() {
		if (valid_) {
			for (File file : files_) {
				FileManager.removeFile(file.getFullPath());
			}
			if (downloadFolder_ != null && name_ != null && !name_.equals("")) {
				String uniquePath = downloadFolder_.concat("/").concat(name_);
				FileManager.removeDirectories(new java.io.File(uniquePath));
			}
		}
	}
	
	/** Returns the id of the torrent. (Only used inside of this program.) */
	@Override
	public int getId() {
		return id_;
	}

	/** Returns the encoded info hash. */
	@Override
	public String getInfoHashString() {
		return infoHashString_;
	}
	
	/** Returns the magnet link of the torrent. */
	@Override
	public String getMagnetLink() {
		String magnet = "magnet:?xt=urn:btih:" + infoHashString_ + "&dn=" + name_;
		for (int i = 0; i < trackers_.size(); i++) {
			magnet += "&tr=" + trackers_.elementAt(i).getUrl();
		}
		return magnet;
	}
	
	/** Returns the full download path of the torrent. */
	@Override
	public String getPath() {
		return path_;
	}
	
	/** Returns the download folder of the torrent. */
	@Override
	public String getDownloadFolder() {
		if (downloadFolder_ == null || downloadFolder_.equals("")) {
			return "/";
		}
		return downloadFolder_;
	}
	
	/** Returns the name of the torrent. */
	@Override
	public String getName() {
		return name_;
	}
	
	/** Returns whether the torrent is valid or not. */
	@Override
	public boolean isValid() {
		return valid_;
	}
		
	/** Returns the status of the torrent. */
	@Override
	public int getStatus() {
		return status_;
	}
	
	/** Returns whether the torrent is about to be created or not. */
	@Override
	public boolean isCreating() {
		return status_ == STATUS_CREATING || status_ == STATUS_STOPPED_CREATING;
	}
	
	/** Returns whether the torrent is working or not (hash checking/downloading/seeding/metadata downloading). */
	@Override
	public boolean isWorking() {
		return status_ == STATUS_DOWNLOADING || status_ == STATUS_SEEDING || 
			   status_ == STATUS_CHECKING_HASH  || status_ == STATUS_DOWNLOADING_METADATA;
	}
	
	/** Returns whether the torrent is connected or not. (downloading/seeding/metadata downloading) */
	@Override
	public boolean isConnected() {
		return status_ == STATUS_DOWNLOADING || status_ == STATUS_SEEDING || status_ == STATUS_DOWNLOADING_METADATA;
	}
	
	/** Returns whether the torrent is downloading data or not. (downloading/metadata downloading) */
	@Override
	public boolean isDownloadingData() {
		return status_ == STATUS_DOWNLOADING || status_ == STATUS_DOWNLOADING_METADATA;
	}
	
	/** Returns the bitfield of the torrent. */
	@Override
	public Bitfield getBitfield() {
		return bitfield_;
	}

	/** Returns the downloading bitfield of the torrent. */
	@Override
	public Bitfield getDownloadingBitfield() {
		return downloadingBitfield_;
	}
	
	/** Returns the active bitfield of the torrent. */
	@Override
	public Bitfield getActiveBitfield() {
		return activeBitfield_;
	}

	/** Returns the array of the files of the torrent. */
	@Override
	public Vector<File> getFiles() {
		return files_;
	}

	/** Returns the array of the trackers of the torrent. */
	@Override
	public ArrayList<TrackerInfo> getTrackers() {
		return new ArrayList<TrackerInfo>(trackers_);
	}
	
	/** Returns the array of the connected peers. */
	@Override
	public PeerInfo[] getConnectedPeers() {
		if (!connectedPeers_.isEmpty()) {
			synchronized (connectedPeers_) {
				final PeerInfo[] peers = new PeerInfo[connectedPeers_.size()];
				connectedPeers_.copyInto(peers);
				return peers;
			}
		}
		return new PeerInfo[0];
	}
	
	/** Returns the percent of the downloaded data. */
	@Override
	public double getProgressPercent() {
		if (status_ == STATUS_DOWNLOADING_METADATA) {
			return 0;
		}
		if (activeSize_ == 0) return 100.0;
		if (status_ != STATUS_CHECKING_HASH) {
			return activeDownloadedSize_ / (activeSize_ / 100.0);
		}
		return checkedSize_ / (activeSize_ / 100.0);
	}
	
	/** Returns the length of pieces. */
	@Override
	public int getPieceLength() {
		return pieceLength_;
	}
	
	/** Returns the number of pieces. */
	@Override
	public int pieceCount() {
		return pieces_.size();
	}
	
	/** Returns the active size of the torrent. */
	@Override
	public long getActiveSize() {
		return activeSize_;
	}
	
	/** Returns the full size of the torrent. */
	@Override
	public long getFullSize() {
		return fullSize_;
	}
	
	/** Returns the size of completed files. */
	@Override
	public long getCompletedFilesSize() {
		long size = 0;
		for (int i = 0; i < files_.size(); i++) {
			File file = files_.get(i);
			if (file.isComplete()) {
				size += file.getSize();
			}
		}
		return size;
	}
	
	/** Returns the active downloaded size of the torrent. */
	@Override
	public long getActiveDownloadedSize() {
		return activeDownloadedSize_;
	}

	/** Returns the number of bytes have to be downloaded. */
	@Override
	public long getBytesLeft() {
		return activeSize_ - activeDownloadedSize_;
	}

	/** Returns the number of downloaded bytes. */
	@Override
	public long getBytesDownloaded() {
		return downloadedSize_;
	}

	/** Returns the number of uploaded bytes. */
	@Override
	public long getBytesUploaded() {
		return uploadedSize_;
	}

	/** Returns the download speed. */
	@Override
	public int getDownloadSpeed() {
		return downloadSpeed_.getSpeed();
	}

	/** Returns the upload speed. */
	@Override
	public int getUploadSpeed() {
		return uploadSpeed_.getSpeed();
	}
	
	/** Returns the remaining time (ms) until the downloading ends. */
	@Override
	public int getRemainingTime() {
		int speed = getDownloadSpeed();
		if (speed == 0) {
			return -1;
		}
		long remaining = activeSize_ - activeDownloadedSize_;
		double millis = ((double) remaining / (double) speed) * 1000.0;
		return (int) millis;
	}
	
	/** Returns the date (in ms since 1970) when the torrent was added to the list. */
	@Override
	public long getAddedOn() {
		return addedOn_;
	}
	
	/** Returns the date (in ms since 1970) when the torrent was completed. */
	@Override
	public long getCompletedOn() {
		return completedOn_;
	}
	
	/** Returns the date (in ms since 1970) when the torrent was removed from the list. */
	@Override
	public long getRemovedOn() {
		return removedOn_;
	}
	
	/** Returns the elapsed time (ms) since the torrent has been started. */
	@Override
	public long getElapsedTime() {
		return elapsedTime_;
	}
	
	/** Returns the downloading time (ms). */
	@Override
	public long getDownloadingTime() {
		return downloadingTime_;
	}
	
	/** Returns the seeding time (ms). */
	@Override
	public long getSeedingTime() {
		return seedingTime_;
	}
	
	/** Returns the number of seeds. */
	@Override
	public int getSeeds() {
		return seeds_;
	}

	/** Returns the number of leechers. */
	 @Override
	public int getLeechers() {
		return leechers_;
	}
	
	/** Returns information of the torrent in JSON. */
	public JSONObject getStateInJSON() {
		final JSONObject json = new JSONObject();

		try {
			json.put("Version", Preferences.APP_VERSION_CODE);
			
			json.put("InfoHash", getInfoHashString());
			
			json.put("Name", getName());

			json.put("DownloadFolder", downloadFolder_);
			
			json.put("AddedOn", addedOn_);
			json.put("CompletedOn", completedOn_);
			
			if (bitfield_ != null && !bitfield_.isNull()) {
				JSONArray bitfield = new JSONArray();
				for (int i = 0; i < bitfield_.data().length; i++) {
					bitfield.put((int) bitfield_.data()[i]);
				}
				json.put("Bitfield", bitfield);
			}

			JSONArray filePriorities = new JSONArray();
			for (File file : files_) {
				filePriorities.put(file.getPriority());
			}
			json.put("FilePriorities", filePriorities);
			
			json.put("Status", status_);
			
			json.put("ElapsedTime", elapsedTime_);
			json.put("DownloadingTime", downloadingTime_);
			json.put("SeedingTime", seedingTime_);
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
	public boolean setStateFromJSON(final JSONObject json) {
		try {
			int version = 0;
			if (json.has("Version")) {
				version = json.getInt("Version");
			}
			
			if (json.has("Name") && (name_ == null || name_.equals(""))) {
				name_ = json.getString("Name");
			}
			
			if (infoHashString_ == null) {
				infoHashString_ = json.getString("InfoHash");
				int[] hashResult = SHA1.stringToIntArray(infoHashString_);
				infoHash_ = SHA1.resultToByteString(hashResult);
				infoHashByteArray_ = SHA1.resultToByte(hashResult);
				shouldStart_ = true;
			}
			
			if (json.has("AddedOn")) {
				addedOn_ = json.getLong("AddedOn");
			} else {
				addedOn_ = 0;
			}
			
			if (json.has("CompletedOn")) {
				completedOn_ = json.getLong("CompletedOn");
			}
			
			if (json.has("DownloadingTime")) {
				downloadingTime_ = json.getLong("DownloadingTime");
			} else {
				downloadingTime_ = -1;
			}
			
			if (json.has("SeedingTime")) {
				seedingTime_ = json.getLong("SeedingTime");
			} else {
				seedingTime_ = -1;
			}
			
			if (valid_) {
				JSONArray filePriorities = json.getJSONArray("FilePriorities");
				File file;
				if (filePriorities.length() == files_.size()) {
					for (int i = 0; i < filePriorities.length(); i++) {
						int priority = filePriorities.getInt(i);
						file = files_.get(i);
						file.setPriority(priority);
						if (filePriorities.getInt(i) > File.PRIORITY_SKIP) {
							addFileToFileManager(file);
						}
					}
				}
				createFiles();
				setTorrentActivePieces();
			}
			
			if (valid_) {
				if (json.has("Bitfield")) {
					JSONArray bitfield = json.getJSONArray("Bitfield");
					ByteArrayBuffer bab = new ByteArrayBuffer(bitfield.length());
					for (int i = 0; i < bitfield.length(); i++) {
						bab.append((byte) bitfield.getInt(i));
					}
					bitfield_.set(bab.buffer());
				
					for (int i = 0; i < pieceCount(); i++) {
						if (bitfield_.isBitSet(i)) {
							Piece piece = pieces_.get(i);
							onPieceDownloaded(piece, true);
							piece.setComplete();
							status_ = STATUS_OPENING;
						}
					}
				}
				
				isFirstStart_ = json.getBoolean("IsFirstStart");
			}
			
			checkedSize_ = activeDownloadedSize_;
			
			downloadedSize_ = json.getLong("Downloaded");
			latestBytesDownloaded_ = downloadedSize_;
			uploadedSize_ = json.getLong("Uploaded");
			latestBytesUploaded_ = uploadedSize_;
			elapsedTime_ = json.getLong("ElapsedTime");
			
			// Before the 10th version status codes were R.string.status_XXX...
			// They were UI specific therefore I replaced them with final static int variables
			// For backward compatibility the default status is STOPPED
			if (version < 10) {
				status_ = STATUS_STOPPED;
			} else {
				status_ = json.getInt("Status");
				if (!valid_ && (status_ == STATUS_DOWNLOADING || status_ == STATUS_CHECKING_HASH || status_ == STATUS_SEEDING)) {
					status_ = STATUS_DOWNLOADING_METADATA;
				}
			}
			
			JSONArray trackers = json.getJSONArray("Trackers");
			trackers_.removeAllElements();
			String trackerUrl;
			for (int i = 0; i < trackers.length(); i++) {
				trackerUrl = trackers.getString(i);
				addTracker(trackerUrl);
			}
			trackerUrl = null;
			
			downloadSpeed_ = new Speed();
			uploadSpeed_ = new Speed();
			
		} catch (JSONException e) {
			Log.v(LOG_TAG, e.getMessage());
			return false;
		}
		
		return true;
	}

	@Override
	public int compareTo(Torrent another) {
		if (name_ == null) {
			return -1;
		} else if (another.name_ == null) {
			return 1;
		}
		return name_.compareTo(another.name_);
	}
}
