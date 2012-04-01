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
	public final static int ERROR_GENERAL            = -8;

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

	private int bytesTotal_           = 0;
	private int bytesUploaded_        = 0;
	private int bytesDownloaded_      = 0;
	private int downloadSpeed_;
	private int uploadSpeed_;
	private int bytesLeft_            = 0;
	private double downloadPercent_   = 0;
	private int downloadedPieceCount_ = 0;
	private boolean complete_ = false;
	private Vector<File> files_;
	private Vector<Piece> pieces_;
	private Vector<Piece> downloadablePieces_;
	private Vector<Piece> rarestPieces_;
	private Vector<Piece> downloadingPieces_;
	private Bitfield bitfield_;

	private Vector<Peer> peers_;
	private Vector<Peer> connectedPeers_;
	private Tracker tracker_;
	private Vector<Vector<Tracker>> trackerList_;
	private Vector<String> announceList_;
	private final int DefaultTrackerRequestInterval = 600;
	private int trackerRequestInterval;
	
	/** Constructor with the manager and the torrent file as a parameter. */
	public Torrent(TorrentManager torrentManager, String filePath, String downloadPath) {
		torrentManager_ = torrentManager;
		fileManager_ = new FileManager();
		downloadFolder_ = downloadPath;
		
		torrentFilePath_ = filePath;
		torrentFileName_ = filePath.substring(filePath.lastIndexOf('/') + 1, filePath.length());
		

		files_ = new Vector<File>();
		pieces_ = new Vector<Piece>();
		downloadablePieces_ = new Vector<Piece>();
		rarestPieces_ = new Vector<Piece>();
		downloadingPieces_ = new Vector<Piece>();
		tracker_ = new Tracker();
		peers_ = new Vector<Peer>();
		connectedPeers_ = new Vector<Peer>();
	}

	/** Class containing the path and the size of a file to be created. */
	class CreateFile {
		String filePath;
		int fileSize;

		CreateFile(String filePath, int fileSize) {
			this.filePath = filePath;
			this.fileSize = fileSize;
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

		byte[] hash = new byte[20];
		Piece piece;
		for (int i = 0; i < pieceCount; i++) {
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
	
	/** Stops the torrent. */
	public void stop() {
		tracker_.changeEvent(Tracker.EVENT_STOPPED);
		
		for (int i = 0; i < connectedPeers_.size(); i++) {
			connectedPeers_.get(i).disconnect();
		}
		status_ = R.string.status_stopped;
		torrentManager_.updateTorrent(this);
	}
	
	/** Connects to peers. */
	public void connectToPeers() {
		status_ = R.string.status_downloading;
		torrentManager_.updateTorrent(this);
		
		for (int i = 0; i < peers_.size() && i < MAX_STORED_PEERS; i++) {
			Peer peer = peers_.get(i);
			connectedPeers_.addElement(peer);
			peer.connect(this);
		}
	}
	
	/** Removes a disconnected peer from the array of connected peers. */
	public void peerDisconnected(Peer peer) {
		connectedPeers_.removeElement(peer);
	}
	
	private int latestBytesDownloaded_ = 0;
	/** Scheduler method. Schedules the peers and the trackers. */
	public void onTimer() {
		if (valid_) {
			for(int i = 0; i < connectedPeers_.size(); i++) {
				connectedPeers_.elementAt(i).onTimer();
            }
		}
		
		if (downloadSpeed_ != bytesDownloaded_ - latestBytesDownloaded_) {
			downloadSpeed_ = bytesDownloaded_ - latestBytesDownloaded_;
			torrentManager_.updateTorrent(this);
		}
		latestBytesDownloaded_ = bytesDownloaded_;
	}
	
	/** Returns a downloadable block for the given peer. */
	public synchronized Block getBlockToDownload(Peer peer) {
		Block block = null;
		Piece piece = null;
		
		/* *** EASY algoritmus, block-ok letöltése sorban
		for (int i = 0; i < pieces_.size(); i++) {
			if (peer.hasPiece(i)) {
				Piece piece = pieces_.elementAt(i);
				if (piece.hasUnrequestedBlock()) {
					blockToDownload = piece.getUnrequestedBlock();
					if (blockToDownload != null) return blockToDownload;
				}
			}
		}
		 * ***/
		
		if (downloadablePieces_.size() > 0) {
			// NORMAL MODE
			
			// Get a block from the downloading pieces
			for (int i = 0; i < downloadingPieces_.size(); i++) {
				piece = downloadingPieces_.elementAt(i);
				if (peer.hasPiece(piece.index())) {
					if (piece.hasUnrequestedBlock()) {
						block = piece.getUnrequestedBlock();
						if (block != null) return block;
					}
				}
			}
			
			block = null;
			piece = null;
			
			if (rarestPieces_.size() == 0 && downloadablePieces_.size() != 0) calculateRarestPieces();
			// Get a block from the rarest pieces
			for (int i = 0; i < rarestPieces_.size(); i++) {
				piece = rarestPieces_.elementAt(i);
				if (peer.hasPiece(piece.index())) {
					if (piece.hasUnrequestedBlock()) {
						block = piece.getUnrequestedBlock();
						if (block != null) {
							rarestPieces_.removeElement(piece);
							downloadingPieces_.add(piece);
							return block;
						}
					}
				}
			}
			
			block = null;
			piece = null;
			
			// Get a block from the downloadable pieces
			for (int i = 0; i < downloadablePieces_.size(); i++) {
				piece = downloadablePieces_.elementAt(i);
				if (peer.hasPiece(piece.index())) {
					if (piece.hasUnrequestedBlock()) {
						block = piece.getUnrequestedBlock();
						if (block != null) {
							downloadablePieces_.removeElement(piece);
							downloadingPieces_.add(piece);
							return block;
						}
					}
				}
			}
		
		} else {
			// END GAME MODE
			
			// Get a block from the downloading pieces
			for (int i = 0; i < downloadingPieces_.size(); i++) {
				piece = downloadingPieces_.elementAt(i);
				if (peer.hasPiece(piece.index())) {
					if (piece.hasUnrequestedBlock()) {
						block = piece.getUnrequestedBlock();
						if (block != null) return block;
					} else {
						Vector<Block> blocksToDownload = piece.getRequestedBlocks();
						for (int j = 0; j < blocksToDownload.size(); j++) {
							block = blocksToDownload.elementAt(j);
							if (!peer.hasBlock(block)) {
								return block;
							}
						}
					}
				}
			}
			
			
		}
		
		/*if (pieceToDownload == null) {
			for (int i = 0; i < pieces_.size(); i++) {
				if (peer.hasPiece(i)) {
					pieceToDownload = pieces_.elementAt(i);
					if (!pieceToDownload.isComplete() && downloadingPieces_.contains(pieceToDownload)) {
						if (pieceToDownload.)
						
						downloadingPieces_.addElement(pieceToDownload);
						break;
					}
					else {
						pieceToDownload = null;
					}
				}
			}
		}*/
		
		return null;
	}
	
	/** Cancels the downloading of a block. */
	public void cancelBlock(Block block) {
		block.setNotRequested();
		Piece piece = getPiece(block.pieceIndex());
		piece.addBlockToRequest(block);
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
	
	/** 
	 * Writes a block into the file in the given position.
	 * The offset and the length within the block is also given. 
	 */
	public synchronized int writeFile(File file, int filePosition, byte[] block, int offset, int length) {
		return fileManager_.writeFile(file.getPath(), filePosition, block, offset, length);
	}
	
	/** Adds a new peer to the list of the peers of the torrent. */
	public int addPeer(Peer peer) {
        //if (peers_.size() >= MAX_STORED_PEERS) return ERROR_OVERFLOW;
        if (hasPeer(peer.getAddress(), peer.getPort())) return ERROR_ALREADY_EXISTS;

        peers_.addElement(peer);
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
		for (int i = 0; i < downloadablePieces_.size(); i++) {
			piece = downloadablePieces_.elementAt(i);
			int k = piece.getNumberOfPeersHaveThis();
			if (k < n) n = k;
		}
		
		for (int i = 0; i < downloadablePieces_.size(); i++) {
			piece = downloadablePieces_.elementAt(i);
			int k = piece.getNumberOfPeersHaveThis();
			if (k == n) rarestPieces_.add(piece);
		}
	}
	
	/** Updates the downloaded bytes with the given amount of bytes. */
	public void updateBytesDownloaded(int bytes) {
		bytesDownloaded_ += bytes;
		bytesLeft_ -= bytes;
		downloadPercent_ = bytesDownloaded_ / (bytesTotal_ / 100.0);
		
		torrentManager_.updateTorrent(this);
		// TODO: Record the downloaded bytes for download speed calculation!
	}
	
	/** Called after a piece has been downloaded. */
	public void pieceDownloaded(Piece piece, boolean calledBySavedTorrent) {
		downloadingPieces_.removeElement(piece);

		bitfield_.setBit(piece.index());
		downloadedPieceCount_++;
		// TODO: notify the service that a new piece has been downloaded

		if (!calledBySavedTorrent) {
			autosave(false);
		}

		if (downloadedPieceCount_ == pieces_.size()) {
			complete_ = true;
			downloadPercent_ = 100;
			status_ = R.string.status_seeding;
			torrentManager_.updateTorrent(this);
			Log.v(LOG_TAG, "DOWNLOAD COMPLETE");

			tracker_.changeEvent(Tracker.EVENT_COMPLETED);

			if (!calledBySavedTorrent) {
				// Disconnect from peers if not incomming connection
				synchronized (connectedPeers_) {
					for (int i = 0; i < connectedPeers_.size(); i++) {
						//if (!connectedPeers_.elementAt(i).isIncommingPeer())
							connectedPeers_.elementAt(i).disconnect();
					}
				}
			}

			//avarageBytesPerSecond_ = 0;
			//bytesPerSecond_ = 0;
			//torrentManager.notifyTorrentObserverMain(this, MTTorrentObserver.EMTMainEventTorrentComplete);
			autosave(true);
			// Check and start end game if neccesary
			//endGameCheck();
		}

		synchronized (connectedPeers_) {
			for (int i = 0; i < connectedPeers_.size(); i++) {
				connectedPeers_.elementAt(i).notifyThatClientHavePiece(piece.index());
			}
		}
		
	}
	
	private void autosave(boolean forced) {
		/* TODO
		final long curTime = (new Date()).getTime();
		final long deltaTime = curTime - lastAutosaveTime_;
		downloadedSinceLastSave_ = downloadedPieceCount_ * pieceLength_;

		if (forced || deltaTime >= autosavePeriod_ || downloadedSinceLastSave_ >= autosaveSize_) {
			lastAutosaveTime_ = curTime;
			downloadedSinceLastSave_ = 0L;
		}*/
	}

	/** Called when the downloaded piece is wrong. */
	public void pieceHashFailed(Piece piece) {
		synchronized (downloadingPieces_) {
			downloadingPieces_.removeElement(piece);
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
		return downloadSpeed_;
	}
	
	/** Returns the upload speed. */
	public int getUploadSpeed() {
		return uploadSpeed_;
	}
	
	/** Returns the percent of the downloaded data. */
	public double getDownloadPercent() {
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
	
	/** Returns the number of seeds. */
	public int getSeeds() {
		return tracker_.getComplete();
	}
	
	/** Returns the number of leechers. */
	public int getLeechers() {
		return tracker_.getIncomplete();
	}
}
