package hu.bute.daai.amorg.drtorrent.torrentengine;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.coding.bencode.Bencoded;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedDictionary;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedInteger;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedList;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedString;
import hu.bute.daai.amorg.drtorrent.coding.sha1.SHA1;
import hu.bute.daai.amorg.drtorrent.file.FileManager;

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

	private final int MAX_STORED_PEERS = 50;
	
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

	private int bytesDownloaded_;
	private int bytesLeft_;
	private Vector<File> files_;
	private Vector<Piece> pieces_;
	private Bitfield bitfield_;

	private Vector<Peer> peers_;
	private Tracker tracker_;
	private Vector<Vector<Tracker>> trackerList_;
	private Vector<String> announceList_;
	private final int DefaultTrackerRequestInterval = 600;
	private int trackerRequestInterval;
	
	/** Constructor with the manager and the torrent file as a parameter. */
	public Torrent(TorrentManager torrentManager, String filePath, String downloadPath) {
		this.torrentManager_ = torrentManager;
		this.fileManager_ = new FileManager();
		this.downloadFolder_ = downloadPath;
		
		this.torrentFilePath_ = filePath;
		this.torrentFileName_ = filePath.substring(filePath.lastIndexOf('/') + 1, filePath.length());
		

		files_ = new Vector<File>();
		pieces_ = new Vector<Piece>();
		tracker_ = new Tracker();
		peers_ = new Vector<Peer>();
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
	 * Processes the bencoded torrent.
	 * 
	 * @param torrentBencoded The torrent file's bencoded content.
	 * @return Error code.
	 */
	public int processBencodedTorrent(Bencoded torrentBencoded) {
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
		trackerList_ = new Vector<Vector<Tracker>>();
		Vector<Tracker> tempTrackers = new Vector<Tracker>();
		trackerList_.addElement(tempTrackers);
		tempTrackers.addElement(new Tracker(announce_, this));

		// announce-list
		/*tempBencoded = torrent.entryValue("announce-list");
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
								tempTrackers.addElement(new Tracker(tempAnnounceURL, this));
							}
						}
					}
					// VectorSorter.shuffle(tmp);
				}
			}
		}*/
		
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

		torrentManager_.hideProgress();
		if (torrentManager_.addTorrent(this)) {
			torrentManager_.updateTorrent(this);
		} else {
			return ERROR_ALREADY_EXISTS;
		}

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
			
			torrentManager_.updateTorrent(this);

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
				//if (!isSaved) {
					createFiles.addElement(new CreateFile(pathBuf, length));
				/*} else {
					addFile(pathBuf, length, true, isSaved);
					bytesLeft_ += length;
				}*/

				sumFileSizes += length;
			}

			//if (!isSaved) {
				// Check free size
				if (FileManager.freeSize(path_) >= sumFileSizes) {
					// Create files
					CreateFile tempCreateFile;
					for (int i = 0; i < createFiles.size(); i++) {
						tempCreateFile = (CreateFile) createFiles.elementAt(i);
						int result = addFile(tempCreateFile.filePath, tempCreateFile.fileSize, false, false);
						if (result != ERROR_NONE)
							return result;

						bytesLeft_ += tempCreateFile.fileSize;
					}
				} else {
					Log.v(LOG_TAG, "Not enough free place for the torrent");
					return ERROR_NO_FREE_SIZE;
				}
			//}
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
			path_ = downloadFolder_ + name_ + "/";

			torrentManager_.updateTorrent(this);
			
			int result = addFile(name_, length, true, false);
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
				piece = new Piece(this, hash, getSize() - processedLength);
			}

			pieces_.addElement(piece);
		}

		bitfield_ = new Bitfield(pieces_.size(), false);

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
		
		return ERROR_NONE;
	}
	
	/** Calculates the fragments of the file(s). */
    private void calculateFileFragments() {
        int pieceIndex = 0;
        Piece piece = (Piece) pieces_.elementAt(pieceIndex);
        int piecePos = 0;

        for (int i=0; i<files_.size(); i++) {
        	File file = (File) files_.elementAt(i);

            if (piecePos + file.getSize() <= piece.length()) {
                piece.addFileFragment(file, 0, file.getSize());
                piecePos += file.getSize();

                if (piecePos + file.getSize() == piece.length()) {
                    piece = (Piece) pieces_.elementAt(++pieceIndex);
                    piecePos = 0;
                }
            } else {
                int filePos = 0;

                while (filePos < file.getSize()) {
                    if ((file.getSize() - filePos + piecePos) > piece.length()) {
                        piece.addFileFragment(file, filePos, piece.length() - piecePos);
                        filePos += piece.length() - piecePos;

                        piece = (Piece) pieces_.elementAt(++pieceIndex);
                        piecePos = 0;
                    } else {
                        piece.addFileFragment(file, filePos, file.getSize() - filePos);
                        piecePos += file.getSize() - filePos;

                        if (piecePos == piece.length()) {
                            if (pieceIndex < (pieces_.size() - 1)) {
                                piece = (Piece) pieces_.elementAt(++pieceIndex);
                                piecePos = 0;
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

	/** Processes the response of the tracker */
	public int processTrackerResponse(Bencoded responseBencoded) { 
        if (responseBencoded.type() != Bencoded.BencodedDictionary)
        	return ERROR_WRONG_CONTENT;

        BencodedDictionary response = (BencodedDictionary) responseBencoded;  
        Bencoded value = null;

        // failure reason
        value = response.entryValue("failure reason");
        if (value != null && (value.type() == Bencoded.BencodedString)) {
            Log.v(LOG_TAG, "[Tracker] Request failed, reason: " + ((BencodedString) value).getStringValue());
            return ERROR_WRONG_CONTENT;
        }
        
        // interval
        value = response.entryValue("interval");
        if (value != null && (value.type() == Bencoded.BencodedInteger)) {
            int interval = ((BencodedInteger)value).getValue();
            if ((interval > 0) && (interval < 12000))
            	trackerRequestInterval = interval;
            else
            	trackerRequestInterval = DefaultTrackerRequestInterval;				
        }	

        // complete

        // incomplete

        // peers
        Log.v(LOG_TAG, "Local peer id: " + torrentManager_.getPeerID());

        value = response.entryValue("peers");
        // Normal tracker response
        if (value != null && (value.type() == Bencoded.BencodedList)) {
            BencodedList bencodedPeers = (BencodedList) value;

            Log.v(LOG_TAG, "Number of peers received: " + bencodedPeers.count());

            for (int i=0; i<bencodedPeers.count(); i++) {
                value = bencodedPeers.item(i);

                if (value.type() != Bencoded.BencodedDictionary) {
                    return ERROR_WRONG_CONTENT;
                }

                BencodedDictionary bencodedPeer = (BencodedDictionary) value;

                // peer id
                value = bencodedPeer.entryValue("peer id");
                if (value==null || (value.type() != Bencoded.BencodedString))
                    continue;
                String peerId = ((BencodedString) value).getStringValue();

                Log.v(LOG_TAG, "Processing peer id: " + peerId);
                if (peerId.length() != 20)
                    continue;

                // got back our own address
                if (peerId.equals(torrentManager_.getPeerID()))
                    continue;

                // ip
                value = bencodedPeer.entryValue("ip");
                if (value==null || (value.type() != Bencoded.BencodedString))
                    continue;
                String ip = ((BencodedString) value).getStringValue();

                // port
                value = bencodedPeer.entryValue("port");
                if (value==null || (value.type() != Bencoded.BencodedInteger))
                    continue;

                int port = ((BencodedInteger) value).getValue();

                Log.v(LOG_TAG, "Peer address: " + ip + ":" + port);                  

                /*if (Preferences.LocalAddress != null) {
                    if (Preferences.LocalAddress.equals(ip)) {
                        MTLogger.writeLine("Got own local address from tracker, throwing it away...");
                        continue;
                    }
                    else MTLogger.writeLine("");
                }
                else MTLogger.writeLine("");
                */

                Peer peer = new Peer(ip, port, peerId, pieces_.size());
                addPeer(peer);
            }
        }
        // likely a compact response
        else if (value != null && (value.type() == Bencoded.BencodedString)) {
            BencodedString bencodedPeers = (BencodedString) value;

            if ((bencodedPeers.getValue().length % 6) == 0) {
                byte[] ips = bencodedPeers.getValue();
                int[] peersRandomPos = new int[ips.length/6];
                for (int j=0;j<peersRandomPos.length;j++) {
                    peersRandomPos[j]=j;
                }
                //NetTools.shuffle(peersRandomPos);

                for (int i = 0; i < ips.length/6 /*&& peers.size()<MaxStoredPeers*/; i++) {
                    int pos = peersRandomPos[i] * 6;

                    int a, b, c, d;                         
                    if (ips[pos]<0)   a = 256 + ips[pos];
                    else              a = ips[pos];
                    if (ips[pos+1]<0) b = 256 + ips[pos+1];
                    else              b = ips[pos+1];
                    if (ips[pos+2]<0) c = 256 + ips[pos+2];
                    else              c = ips[pos+2];
                    if (ips[pos+3]<0) d = 256 + ips[pos+3];
                    else              d = ips[pos+3];                        
                    
                    String address = "" + a + "." + b + "." + c + "." + d;

                    int p1, p2;
                    if (ips[pos+4]<0) p1 = 256 + ips[pos+4] << 8;
                    else			  p1 = ips[pos+4] << 8;
                    if (ips[pos+5]<0) p2 = 256 + ips[pos+5];
                    else			  p2 = ips[pos+5];                                           
                    
                    int port = p1 + p2;

                    Log.v(LOG_TAG, address + ":" + port);
                    /*if (Preferences.LocalAddress != null) {
                        if (Preferences.LocalAddress.equals(addressBuffer) && port==Preferences.IncommingPort) {
                            MTLogger.writeLine("Got own local address from tracker, throwing it away...");
                            continue;
                        }
                    }*/

                    Peer peer = new Peer(address, port, null, pieces_.size());
                    //int result = 
                    addPeer(peer);
                    /*if (result != ERROR_NONE)
                        MTLogger.writeLine("Peer not added, error code: " + result);
                	*/
                }        
            }
            else
                Log.v(LOG_TAG, "[Tracker] Compact response invalid (length cannot be devided by 6 without remainder)");
        }        
        else
        	Log.v(LOG_TAG, "[Tracker] No peers list / peers list invalid");
        
        Log.v(LOG_TAG, "Response procesed, peers: " + peers_.size()); 

        torrentManager_.updateTorrent(this);
        
        connectToPeers();
        
        return ERROR_NONE;
    }
	
	/** Starts the torrent. */
	public void start() {
		if (valid_) {
			status_ = R.string.status_connecting;
			torrentManager_.updateTorrent(this);
			
			tracker_.connect();
		}
	}
	
	public void connectToPeers() {
		status_ = R.string.status_downloading;
		torrentManager_.updateTorrent(this);
		
		for (int i = 0; i < peers_.size(); i++) {
			peers_.get(i).connect(this);
		}
	}
	
	public void onTimer() {
		if (valid_) {
			for(int i = 0; i < peers_.size(); i++) {
				peers_.elementAt(i).onTimer();
            }
		}
	}
	
	public Piece getPieceToDownload(Peer peer)
    {        
        Piece pieceToDownload = null;
        pieceToDownload = pieces_.elementAt(0);
        return pieceToDownload;
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
					Log.v(LOG_TAG, "Not enough free space");
					return ERROR_NO_FREE_SIZE;
				}
			}
			// Create file
			files_.addElement(new File(filePath, size));
			if (FileManager.createFile(filePath, size))
				return ERROR_NONE;
			else
				return ERROR_FILE_HANDLING;
		} else
			return ERROR_WRONG_CONTENT;
	}
	
	/** Adds a new peer to the list of the peers of the torrent. */
	public int addPeer(Peer peer)
    {
        if (peers_.size() >= MAX_STORED_PEERS)
            return ERROR_OVERFLOW;
        if (hasPeer(peer.getAddress(), peer.getPort()))
            return ERROR_ALREADY_EXISTS;

        peers_.addElement(peer);
        
        Log.v(LOG_TAG, "Number of peers: " + peers_.size());

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
		((Piece) pieces_.elementAt(index)).incNumberOfPeersHaveThis();
	}

	/** Decrements the number of peers having the given piece. */
	public void decNumberOfPeersHavingPiece(int index) {
		((Piece) pieces_.elementAt(index)).decNumberOfPeersHaveThis();
	}
	
	public int indexOfPiece(Piece piece) {
        return pieces_.indexOf(piece);
    }
	
	public int pieceCount() {
        return pieces_.size();
    }
	
	public Piece getPiece(int index) {
		return pieces_.elementAt(index);
	}
	
	/** Returns the size of the torrent. */
	public int getSize() {
		return bytesDownloaded_ + bytesLeft_;
	}

	public int getBytesLeft() {
        return bytesLeft_;
    }
	
	public String getInfoHash() {
		return this.infoHash_;
	}
	
	public byte[] getInfoHashByteArray() {
        return infoHashByteArray_;
    }

	public Bitfield getBitfield() {
		return bitfield_;
	}
	
	public TorrentManager getTorrentManager() {
		return this.torrentManager_;
	}
	
	public String getName() {
		return this.name_;
	}
	
	public int getStatus() {
		return this.status_;
	}
	
	public int getSeeds() {
		return tracker_.getComplete();
	}
	
	public int getLeechers() {
		return tracker_.getIncomplete();
	}
}
