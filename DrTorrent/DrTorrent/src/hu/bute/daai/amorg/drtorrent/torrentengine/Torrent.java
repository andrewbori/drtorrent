package hu.bute.daai.amorg.drtorrent.torrentengine;

import hu.bute.daai.amorg.drtorrent.bencode.Bencoded;
import hu.bute.daai.amorg.drtorrent.bencode.BencodedDictionary;
import hu.bute.daai.amorg.drtorrent.bencode.BencodedInteger;
import hu.bute.daai.amorg.drtorrent.bencode.BencodedList;
import hu.bute.daai.amorg.drtorrent.bencode.BencodedString;
import hu.bute.daai.amorg.drtorrent.file.FileManager;
import hu.bute.daai.amorg.drtorrent.sha1.SHA1;

import java.util.Vector;

import android.util.Log;

/** Class representing a torrent. */
public class Torrent {
	public final static int ERROR_NONE               =  0;
	public final static int ERROR_WRONG_CONTENT      = -1;
	public final static int ERROR_NO_FREE_SIZE       = -2;
	public final static int ERROR_ALREADY_EXISTS     = -3;
	public final static int ERROR_FILE_HANDLING      = -4;
	public final static int ERROR_OVERFLOW           = -5;
	public final static int ERROR_TORRENT_NOT_PARSED = -6;
	public final static int ERROR_NOT_ATTACHED       = -7;

	private TorrentManager torrentManager_;
	private FileManager fileManager_;

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

	private int bytesDownloaded_;
	private int bytesLeft_;
	private Vector<File> files_;
	private Vector<Piece> pieces_;

	private Vector<Vector<Tracker>> trackerList_;
	private Vector<String> announceList_;

	/** Constructor with the torrent's manager as a parameter. */
	public Torrent(TorrentManager torrentManager) {
		this.torrentManager_ = torrentManager;
		this.fileManager_ = new FileManager();
		this.downloadFolder_ = "/sdcard/";

		files_ = new Vector<File>();
		pieces_ = new Vector<Piece>();
	}

	/** Class containing the path and the size of a file to be created. */
	class CreateFile {
		String filePath;
		int fileSize;

		CreateFile(String aFilePath, int aFileSize) {
			filePath = aFilePath;
			fileSize = aFileSize;
		}
	}

	/**
	 * Sets the torrent by a torrent file's bencoded content.
	 * 
	 * @param torrentBencoded The torrent file's bencoded content.
	 * @param isSaved True if the torrent was opened before.
	 * @return Error code.
	 */
	public int set(Bencoded torrentBencoded, boolean isSaved) {
		if (torrentBencoded.type() != Bencoded.BencodedDictionary)
			return ERROR_WRONG_CONTENT; // bad bencoded torrent

		BencodedDictionary torrent = (BencodedDictionary) torrentBencoded;

		Bencoded tempBencoded = null;

		// announce
		tempBencoded = torrent.entryValue("announce");
		if (tempBencoded == null || tempBencoded.type() != Bencoded.BencodedString)
			return ERROR_WRONG_CONTENT;

		announce_ = ((BencodedString) tempBencoded).getStringValue();
		trackerList_ = new Vector<Vector<Tracker>>();
		Vector<Tracker> tempTrackers = new Vector<Tracker>();
		trackerList_.addElement(tempTrackers);
		tempTrackers.addElement(new Tracker(announce_));

		// announce-list
		tempBencoded = torrent.entryValue("announce-list");
		if (tempBencoded != null && tempBencoded.type() == Bencoded.BencodedList) {
			announceList_ = new Vector<String>();
			trackerList_.removeAllElements();
			multiTrackerEnabled_ = true;
			BencodedList parseAnnounceLists = (BencodedList) tempBencoded;
			for (int i = 0; i < parseAnnounceLists.count(); i++) {
				tempBencoded = parseAnnounceLists.item(i);
				if (tempBencoded != null && tempBencoded.type() == Bencoded.BencodedList) {
					BencodedList announces = (BencodedList) tempBencoded;
					tempTrackers = new Vector<Tracker>();
					trackerList_.addElement(tempTrackers);
					for (int j = 0; j < announces.count(); j++) {
						tempBencoded = announces.item(j);
						if (tempBencoded != null && tempBencoded.type() == Bencoded.BencodedString) {
							String tempAnnounceURL = ((BencodedString) tempBencoded).getStringValue();
							if (!tempAnnounceURL.startsWith("udp")) {
								announceList_.addElement(tempAnnounceURL);
								tempTrackers.addElement(new Tracker(tempAnnounceURL));
							}
						}
					}
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
		// Logger.writeLine("Infohash of the torrrent(hex): " + SHA1.resultToString(hashResult));
		Log.v("Torrent", "Infohash of the torrrent(hex): " + SHA1.resultToString(hashResult));
		hashResult = null;

		if (!isSaved) {
			if (torrentManager_.hasTorrent(this))
				return ERROR_ALREADY_EXISTS;
		}

		// torrentManager.notifyTorrentObserverMain(getThisTorrent(), MTTorrentObserver.EMTMainEventTorrentNotDuplicated);

		// info/piece lenght
		tempBencoded = info.entryValue("piece length");
		if (tempBencoded == null || tempBencoded.type() != Bencoded.BencodedInteger)
			return ERROR_WRONG_CONTENT;

		pieceLength_ = ((BencodedInteger) tempBencoded).getValue();

		// file/files

		tempBencoded = info.entryValue("files");
		if (tempBencoded != null) { // multi-file torrent
			if (tempBencoded.type() != Bencoded.BencodedList)
				return ERROR_WRONG_CONTENT;

			BencodedList files = (BencodedList) tempBencoded;

			// info/name

			tempBencoded = info.entryValue("name");
			if (tempBencoded == null || tempBencoded.type() != Bencoded.BencodedString)
				return ERROR_WRONG_CONTENT;

			name_ = ((BencodedString) tempBencoded).getStringValue();
			path_ = downloadFolder_ + name_ + "/";

			Vector<CreateFile> createFiles = new Vector<CreateFile>();
			int sumFileSizes = 0;

			for (int i = 0; i < files.count(); i++) {
				tempBencoded = files.item(i);
				if (tempBencoded.type() != Bencoded.BencodedDictionary)
					return ERROR_WRONG_CONTENT;

				BencodedDictionary file = (BencodedDictionary) tempBencoded;

				tempBencoded = file.entryValue("path");
				if (tempBencoded == null || tempBencoded.type() != Bencoded.BencodedList)
					return ERROR_WRONG_CONTENT;

				BencodedList inPath = (BencodedList) tempBencoded;

				int pathLength = 0;
				int j = 0;
				for (j = 0; j < inPath.count(); j++) {
					tempBencoded = inPath.item(j);
					if (tempBencoded.type() != Bencoded.BencodedString)
						return ERROR_WRONG_CONTENT;

					pathLength += ((BencodedString) tempBencoded).getStringValue().length() + 1;
				}

				String pathBuf = "";

				for (j = 0; j < inPath.count(); j++) {
					tempBencoded = inPath.item(j);
					pathBuf = pathBuf + ((BencodedString) tempBencoded).getStringValue();

					if (j != (inPath.count() - 1))
						pathBuf = pathBuf + "/";
				}

				tempBencoded = file.entryValue("length");
				if (tempBencoded == null || tempBencoded.type() != Bencoded.BencodedInteger)
					return ERROR_WRONG_CONTENT;

				int length = ((BencodedInteger) tempBencoded).getValue();

				// If we open a saved torrent then we have to check whether this file is already added
				if (!isSaved) {
					createFiles.addElement(new CreateFile(pathBuf, length));
				} else {
					addFile(pathBuf, length, true, isSaved);
					bytesLeft_ += length;
				}

				sumFileSizes += length;
			}

			if (!isSaved) {
				// Check free size
				if (FileManager.freeSize(path_) >= sumFileSizes) {
					// Create files
					CreateFile tempCreateFile;
					for (int i = 0; i < createFiles.size(); i++) {
						tempCreateFile = (CreateFile) createFiles.elementAt(i);
						int result = addFile(tempCreateFile.filePath, tempCreateFile.fileSize, false, isSaved);
						if (result != ERROR_NONE)
							return result;

						bytesLeft_ += tempCreateFile.fileSize;
					}
				} else {
					Log.v("Torrent", "Not enough free place for the torrent");
					return ERROR_NO_FREE_SIZE;
				}
			}
		} else // single-file torrent
		{
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
			path_ = downloadFolder_ + name_ + "/";

			int result = addFile(name_, length, true, isSaved);
			if (result != ERROR_NONE)
				return result;
		}

		// info/pieces

		tempBencoded = info.entryValue("pieces");

		if (tempBencoded == null || tempBencoded.type() != Bencoded.BencodedString)
			return ERROR_WRONG_CONTENT;

		byte[] piecesArray = ((BencodedString) tempBencoded).getValue();

		if ((piecesArray.length % 20) != 0)
			return ERROR_WRONG_CONTENT;

		int processedLength = 0;
		int pieceCount = piecesArray.length / 20;

		byte[] hash = new byte[20];
		Piece piece;
		for (int i = 0; i < pieceCount; i++) {
			System.arraycopy(piecesArray, i * 20, hash, 0, 20);

			if (i != pieceCount - 1) {
				processedLength += pieceLength_;
				piece = new Piece(this, hash, pieceLength_);
			} else {
				piece = new Piece(this, hash, size() - processedLength);
			}

			pieces_.addElement(piece);
		}

		// bitField = new MTBitfield(torrentPieces.size(), false);

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

		// calculateFileFragments();
		valid_ = true;
		// torrentManager.notifyOpenTorrentStateChanged(MTTorrentObserver.EMTOpenTorrentEventCreatingFilesFinished,this,null,0);

		return ERROR_NONE;
	}

	/**
	 * Adds a new file to the files_ array and creates it in the file system
	 * with reserving space for it as well.
	 * 
	 * @param relativePath The path of the file relative to the torrent's download path.
	 * @param size The size of the file.
	 * @param shouldCheckFreeSpace True if free space should be checked.
	 * @param isSaved True if the file probably exists.
	 * @return Error code.
	 */
	private int addFile(String relativePath, int size, boolean shouldCheckFreeSpace, boolean isSaved) {
		String filePath = path_ + relativePath;

		// If this is a saved torrent then if the file exists we do not have to create it again
		if (isSaved) {
			if (FileManager.fileExists(filePath)) {
				files_.addElement(new File(filePath, size));
				return ERROR_NONE;
			}
		}

		// Creating the file
		if (path_ != null && !path_.equals("")) {
			// If single file torrent then we have to check the free size
			if (shouldCheckFreeSpace) {
				if (FileManager.freeSize(path_) < size) {
					Log.v("Torrent", "Not enough free space");
					return ERROR_NO_FREE_SIZE;
				}
			}

			// Create file
			files_.addElement(new File(filePath, size));
			// torrentManager.notifyOpenTorrentStateChanged(MTTorrentObserver.EMTOpenTorrentEventCreatingFileStatus,this,aRelativePath,0);
			if (FileManager.createFile(filePath, size))
				return ERROR_NONE;
			else
				return ERROR_FILE_HANDLING;
		} else
			return ERROR_WRONG_CONTENT;
	}

	/** Returns the size of the torrent. */
	public int size() {
		return bytesDownloaded_ + bytesLeft_;
	}

	public String getInfoHash() {
		return this.infoHash_;
	}

	public String getName() {
		return this.name_;
	}

}
