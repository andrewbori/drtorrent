package hu.bute.daai.amorg.drtorrent.torrentengine;

import hu.bute.daai.amorg.drtorrent.coding.sha1.SHA1;

import java.io.ByteArrayOutputStream;
import java.util.Vector;

import android.util.Log;

/** This class represents a piece of a torrent. */
public class Piece {
	private final static String LOG_TAG = "Piece";
	
	public final static int DEFALT_BLOCK_LENGTH = 16384;	// default download block size 16kB (2^14)
	
	private Torrent torrent_;
	private byte[] hash_;
	private int index_;
	private int size_;
	private int downloadedSize_;
	private Vector<FileFragment> fragments_;
	private int numberOfPeersHaveThis_;
	private boolean isRequested_ = false;
	
	private Vector<Block> blocksToDownload_ = null;
	private Vector<Block> blocksToRequest_ = null;
	
	private SHA1 incommingHash_ = null;
	
	/** 
	 * Constructor of the piece.
	 * 
	 * @param torrent The torrent that the piece belongs to.
	 * @param hash    The hash value of the piece.
	 * @param length  The length of the piece.
	 */
	public Piece(Torrent torrent, byte[] hash, int index, int length) {
		torrent_ = torrent;
		hash_ = hash;
		index_ = index;
		size_ = length;
		downloadedSize_ = 0;
		fragments_ = new Vector<FileFragment>();
		numberOfPeersHaveThis_ = 0;
	}
	
	/** Calculates the blocks of the piece. */
	public void calculateBlocks() {
		isRequested_ = true;
		Log.v(LOG_TAG, "Calculate blocks.");
		blocksToDownload_ = new Vector<Block>();
		blocksToRequest_ = new Vector<Block>();
		
		int blockCount = size_ / DEFALT_BLOCK_LENGTH;
		if (size_ % DEFALT_BLOCK_LENGTH > 0) blockCount++;
		for (int i = 0; i < blockCount; i++) {
			Block block = null;
			if (i + 1 < blockCount) {
				block = new Block(this, i * DEFALT_BLOCK_LENGTH, DEFALT_BLOCK_LENGTH);
			} else {
				block = new Block(this, i * DEFALT_BLOCK_LENGTH, size_ - (i * DEFALT_BLOCK_LENGTH));
			}
			blocksToDownload_.addElement(block);
			blocksToRequest_.addElement(block);
		}
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
	 * Appends the given block to its file(s).
	 * 
	 * @param data  The block that has to be appended.
	 * @param offset The position of the block within the piece.
	 * @param peer   The peer which the block was received from.
	 * @return Torrent error code.
	 */
	public int appendBlock(byte[] data, Block block, Peer peer) {
		// If received an already received part.
		if (!blocksToDownload_.contains(block)) {
			Log.v(LOG_TAG, "Wrong block has been received: " + block.begin());
			return Torrent.ERROR_NONE;
		}

		synchronized (this) {
			int blockPosition = 0;
			while (blockPosition < data.length) {
				
				// Calculating the file and the position within the file...
				File file = null;
				int filePosition = 0;
				int pos = 0;
				for (int i = 0; i < fragments_.size(); i++) {
					FileFragment fragment = (FileFragment) fragments_.elementAt(i);
					// If the already downloaded part doesn't extend beyond the current fragment...
					// (...which means that a part of the current fragment was received.)
					if (block.begin() < (pos + fragment.length())) {
						file = fragment.file();
						filePosition = fragment.offset() + (block.begin() - pos);
						i = fragments_.size(); // break
					} else {
						pos += fragment.length();
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
					if (fileBlockLength <= (data.length - blockPosition)) {
						//Log.v(LOG_TAG, "File complete: " + file.getPath());

						res = torrent_.writeFile(file, filePosition, data, blockPosition, fileBlockLength);
						if (res != Torrent.ERROR_NONE) return res;

						blockPosition += fileBlockLength;
						downloadedSize_ += fileBlockLength;
						
					// ...if the file isn't finished yet
					} else {
						int rightSize = data.length - blockPosition;

						//Log.v(LOG_TAG, "Block has been saved from: " + peer.getAddress() + ":" + peer.getPort());
						res = torrent_.writeFile(file, filePosition, data, data.length - rightSize, rightSize);
						if (res != Torrent.ERROR_NONE) return res;

						downloadedSize_ += rightSize;
						break;
					}
				} else {
					return Torrent.ERROR_GENERAL;
				}
			}
			
			torrent_.updateBytesDownloaded(data.length);

			if (remaining() == 0) {
				//Log.v(LOG_TAG, "Downloading piece completed!");
				// torrent.getHashChecker().addPieceToCheck(this);
				// TODO: HASH check
				if (checkHash()) {
					Log.v(LOG_TAG, "Hash OK!");
					setFilesDownloaded();
					torrent_.pieceDownloaded(this, false);
				} else {
					Log.v(LOG_TAG, "Hash FAILED!");
					downloadedSize_ = 0;
					calculateBlocks();
					torrent_.pieceHashFailed(this);
					// ask this piece from an other peer
					// peer_.cancelPieceRequest(this);
					// peer.terminate(); // TODO: 
				}

				// we no longer need the hash
				incommingHash_ = null;
			} else if (remaining() < 0) return Torrent.ERROR_GENERAL;

			return Torrent.ERROR_NONE;
		}
	}
	
	/** Reads a block from its file(s). */
	public byte[] readBlock(Block block) {
		int length = block.length();
		
		ByteArrayOutputStream blockByteArray = new ByteArrayOutputStream();

		File file = null;
		int filePosition = 0;
		
		int i = 0;
		int pos = 0;
		for (; i < fragments_.size(); i++) {
			FileFragment fragment = (FileFragment) fragments_.elementAt(i);
			if (block.begin() < (pos + fragment.length())) {
				file = fragment.file();
				filePosition = fragment.offset() + (block.begin() - pos);
				break;
			} else {
				pos += fragment.length();
			}
		}

		if (file == null) {
			return null;
		}

		while (length > 0) {
			if (length >= (file.getSize() - filePosition)) {
				byte[] buffer = torrent_.read(file.getPath(), filePosition, file.getSize() - filePosition);
				if (buffer != null) {
					blockByteArray.write(buffer, 0, buffer.length);
					buffer = null;
					length -= (file.getSize() - filePosition);
				} else
					break;
			} else {
				byte[] buf = torrent_.read(file.getPath(), filePosition, length);
				if (buf != null) {
					blockByteArray.write(buf, 0, buf.length);
					length = 0;
					buf = null;
				}

				break;
			}

			if (i >= fragments_.size() - 1)
				break;

			file = ((FileFragment) fragments_.elementAt(++i)).file();
			filePosition = 0;
		}

		if (length != 0) {
			return null;
		}

		return blockByteArray.toByteArray();
	}
	
	/** Returns whether the piece equals its hash or not. */
	public boolean checkHash() {
		// TODO
		return true;
	}
	
	public void setFilesDownloaded() {
		// TODO
	}
	
	/** Increments the number of peers having this piece. */
	public void incNumberOfPeersHaveThis() {
		numberOfPeersHaveThis_++;
	}
	
	/** Decrements the number of peers having this piece. */
	public void decNumberOfPeersHaveThis() {
		numberOfPeersHaveThis_--;
	}
	
	/** Returns the number of peers having this piece. */
	public int getNumberOfPeersHaveThis() {
		return numberOfPeersHaveThis_;
	}
	
	/** Returns a block of this piece. */
	public byte[] getBlock(int position, int length) {
		// TODO
		return null;
	}
	
	/** Returns the length of the block in the given position. */
	public int getBlockLength(int index) {
		int lengthFromIndex = size_ - (index * DEFALT_BLOCK_LENGTH);
		if (lengthFromIndex > DEFALT_BLOCK_LENGTH) return DEFALT_BLOCK_LENGTH;
		
		return lengthFromIndex;
	}
	
	/** Returns whether the piece has unrequested block(s). */
	public boolean hasUnrequestedBlock() {
		if (isRequested_) {
			if (blocksToRequest_.size() > 0) return true;
		} else {
			if (!isComplete()) return true;
		}
		
		return false;
	}
	
	/** Returns an unrequested block. */
	public Block getUnrequestedBlock() {
		if (!isRequested_) {
			if (!isComplete()) calculateBlocks();
			else return null;
		}
		synchronized(blocksToRequest_) {
			if (blocksToRequest_.size() > 0) {
				Block block = blocksToRequest_.elementAt(0);
				blocksToRequest_.removeElementAt(0);
				return block;
			}
		}
		
		return null;
	}
	
	/** Returns the requested blocks not downloaded yet. */
	public Vector<Block> getRequestedBlocks() {
		return blocksToDownload_;
	}
	
	/** Adds a block to the the array of blocks to be requested. */
	public void addBlockToRequest(Block block) {
		blocksToRequest_.addElement(block);
	}
	
	/** The index of piece in the torrent. */
	public int index() {
        return index_;
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
	
	/** Returns whether the piece is complete or not. */
	public boolean isComplete() {
		return (size_ == downloadedSize_); 
	}
	
	/** Returns whether the piece has been requested or not. */
	public boolean isRequested() {
		return isRequested_;
	}
}
