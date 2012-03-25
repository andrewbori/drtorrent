package hu.bute.daai.amorg.drtorrent.torrentengine;

import hu.bute.daai.amorg.drtorrent.coding.sha1.SHA1;

import java.util.Vector;

import android.util.Log;

/** This class represents a piece of a torrent. */
public class Piece {
	private final static String LOG_TAG = "Piece";
	
	private Torrent torrent_;
	private byte[] hash_;
	private int size_;
	private int downloadedSize_;
	private Vector<FileFragment> fragments_;
	private int numberOfPeersHaveThis_;
	private boolean hasPendingIntent_ = false;
	
	private SHA1 incommingHash_ = null;
	
	/** 
	 * Constructor of the piece.
	 * 
	 * @param torrent The torrent that the piece belongs to.
	 * @param hash    The hash value of the piece.
	 * @param length  The length of the piece.
	 */
	public Piece(Torrent torrent, byte[] hash, int length) {
		torrent_ = torrent;
		hash_ = hash;
		size_ = length;
		downloadedSize_ = 0;
		fragments_ = new Vector<FileFragment>();
		numberOfPeersHaveThis_ = 0;
	}
	
	/** 
	 * Adds a new file fragment to the piece.
	 * 
	 * @param file   The file which the file fragment belongs to.
	 * @param offset The position of the file fragment within the file.
	 * @param length The length of the file fragment.
	 */
	public void addFileFragment(File file, int offset, int length) {
        fragments_.addElement(new FileFragment(file, offset, length));
    }
	
	/** 
	 * Appends the given block to its file.
	 * 
	 * @param block  The block that has to be appended.
	 * @param offset The position of the block within the piece.
	 * @param peer   The peer which the block was received from.
	 * @return Torrent error code.
	 */
	public int appendBlock(byte[] block, int offset, Peer peer) {
		// If received an already received part.
		if (downloadedSize_ != offset) {
			Log.v(LOG_TAG, "Wrong block received: " + offset + " excepted: " + downloadedSize_);
			return Torrent.ERROR_NONE;
		}

		synchronized (this) {
			// Check again, in the synchronized block, this way wrong blocks do not have to wait, until write finishes.
			if (downloadedSize_ != offset) {
				Log.v(LOG_TAG, "Wrong block received: " + offset + " excepted: " + downloadedSize_);
				return Torrent.ERROR_NONE;
			}

			if (incommingHash_ == null) {
				incommingHash_ = new SHA1();
			}

			int blockPosition = 0;
			while (blockPosition < block.length) {
				
				// Calculating the file and the position within the file...
				File file = null;
				int filePosition = 0;
				int pos = 0;
				for (int i = 0; i < fragments_.size(); i++) {
					// If the already downloaded part doesn't extend beyond the current fragment...
					// (...which means that a part of the current fragment was received.)
					if (downloadedSize_ < (pos + fragments_.elementAt(i).getLength())) {
						file = fragments_.elementAt(i).getFile();
						filePosition = fragments_.elementAt(i).getOffset() + (downloadedSize_ - pos);
						i = fragments_.size(); // break
					} else {
						pos += fragments_.elementAt(i).getLength();
					}
				}

				// set the file to downloading state
				/**
				 * TODO: what if file is set not to be downloaded?
				 */
				if (file.getDownloadState() != File.STATUS_DOWNLOADING) {
					file.setDownloadState(File.STATUS_DOWNLOADING);
					// TODO: TorrentManager notify "Torrent Files State Changed".
				}

				if (file != null) {
					int res = Torrent.ERROR_NONE;
					int fileBlockLength = file.getSize() - filePosition; // Remaining length from the position

					Log.v(LOG_TAG, "Appending block to " + file.getPath());

					 // If the end of a file was reached...
					if (fileBlockLength <= (block.length - blockPosition)) {
						//Log.v(LOG_TAG, "File complete: " + file.getPath());

						res = torrent_.writeFile(file, filePosition, block, blockPosition, fileBlockLength);
						if (res != Torrent.ERROR_NONE) return res;

						blockPosition += fileBlockLength;
						downloadedSize_ += fileBlockLength;
						
					// ...if the file isn't finished yet
					} else {
						int rightSize = block.length - blockPosition;

						//Log.v(LOG_TAG, "---PIECE BLOCK SAVE FROM---: " + peer.getAddress() + ":" + peer.getPort());
						res = torrent_.writeFile(file, filePosition, block, block.length - rightSize, rightSize);
						if (res != Torrent.ERROR_NONE) return res;

						downloadedSize_ += rightSize;
						break;
					}
				} else {
					return Torrent.ERROR_GENERAL;
				}
			}
			
			torrent_.updateBytesDownloaded(block.length);
			incommingHash_.update(block);

			if (remaining() == 0) {
				//Logger.writeLine("[Piece] Downloading piece completed!");
				// torrent.getHashChecker().addPieceToCheck(this);

				if (checkHash()) {
					Log.v(LOG_TAG, "Hash OK!");
					setFilesDownloaded();
					torrent_.pieceDownloaded(this, false);
				} else {
					Log.v(LOG_TAG, "Hash FAILED!");
					downloadedSize_ = 0;
					torrent_.pieceHashFailed(this);
					// ask this piece from an other peer
					// peer_.cancelPieceRequest(this);
					peer.terminate(); // TODO: 
				}

				// we no longer need the hash
				incommingHash_ = null;
			} else if (remaining() < 0) return Torrent.ERROR_GENERAL;

			return Torrent.ERROR_NONE;
		}
	}
	
	public boolean checkHash() {
		// TODO
		return true;
	}
	
	public void setFilesDownloaded() {
		// TODO
	}
	
	public void incNumberOfPeersHaveThis() {
		numberOfPeersHaveThis_++;
	}
	
	public void decNumberOfPeersHaveThis() {
		numberOfPeersHaveThis_--;
	}
	
	public void setHasPendingIntent(boolean has) {
		hasPendingIntent_ = has;
	}
	
	public int getNumberOfPeersHaveThis() {
		return numberOfPeersHaveThis_;
	}
	
	public byte[] getBlock(int position, int length) {
		// TODO
		return null;
	}
	
	/** The index of piece in the torrent. */
	public int index() {
        return torrent_.indexOfPiece(this);
    } 
	
	/** The length of piece in bytes. */
	public int size() {
		return size_;
	}
	
	/** The downloaded fragments in bytes. */
	public int downloaded() {
		return downloadedSize_;
    }
	
	/** The remaining fragments in bytes. */
	public int remaining() {
		return size_ - downloadedSize_;
	}
	
	public boolean isComplete() {
		return (size_ == downloadedSize_); 
	}
	
	public boolean hasPendingIntent() {
		return hasPendingIntent_;
	}
}
