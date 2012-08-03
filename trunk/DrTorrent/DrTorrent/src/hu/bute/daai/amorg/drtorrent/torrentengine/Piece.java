package hu.bute.daai.amorg.drtorrent.torrentengine;

import hu.bute.daai.amorg.drtorrent.DrTorrentTools;
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
	private Vector<FileFragment> fragments_;
	private int numberOfPeersHaveThis_ = 0;
	private boolean isRequested_ = false;
	private boolean isComplete_ = false;
	
	private Vector<Block> blocks_ = null;
	private Vector<Block> unrequestedBlocks_ = null;
	private Vector<Block> downloadedBlocks_ = null;
	
	private Vector<Peer> peers_ = null;
	
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
		fragments_ = new Vector<FileFragment>();
	}
	
	/** Calculates the blocks of the piece. */
	public void calculateBlocks() {		
		isRequested_ = true;
		Log.v(LOG_TAG, "Calculate blocks.");
		blocks_ = new Vector<Block>();
		unrequestedBlocks_ = new Vector<Block>();
		downloadedBlocks_ = new Vector<Block>();
		
		int blockCount = size_ / DEFALT_BLOCK_LENGTH;
		if (size_ % DEFALT_BLOCK_LENGTH > 0) blockCount++;
		for (int i = 0; i < blockCount; i++) {
			Block block = null;
			if (i + 1 < blockCount) {
				block = new Block(this, i * DEFALT_BLOCK_LENGTH, DEFALT_BLOCK_LENGTH);
			} else {
				block = new Block(this, i * DEFALT_BLOCK_LENGTH, size_ - (i * DEFALT_BLOCK_LENGTH));
			}
			blocks_.addElement(block);
			unrequestedBlocks_.addElement(block);
		}
		
		peers_ = new Vector<Peer>();
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
		if (blocks_ == null || !blocks_.contains(block)) {
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
					if (block.begin() + blockPosition < (pos + fragment.length())) {
						file = fragment.file();
						filePosition = fragment.offset() + ((block.begin() + blockPosition) - pos);//- pos);
						i = fragments_.size(); // break
					} else {
						pos += fragment.length();
					}
				}

				if (file != null) {
					// set the file to downloading state
					/**
					 * TODO: what if file is set not to be downloaded?
					 */
					if (file.getDownloadState() != File.STATUS_DOWNLOADING) {
						file.setDownloadState(File.STATUS_DOWNLOADING);
					}

					int res = Torrent.ERROR_NONE;
					int fileRemainingLength = file.getSize() - filePosition; // Remaining length from the position

					Log.v(LOG_TAG, "Appending block to " + file.getPath());

					int rightSize;
					// If the file is not finished yet
					if (fileRemainingLength > (data.length - blockPosition)) {
						rightSize = data.length - blockPosition;
					}
					// If the end of a file was reached...
					else {
						rightSize = fileRemainingLength;
					}

					res = torrent_.writeFile(file, filePosition, data, blockPosition, rightSize);
					if (res != Torrent.ERROR_NONE) return res;

					blockPosition += rightSize;
				} else {
					return Torrent.ERROR_GENERAL;
				}
			}
			
			addPeer(peer);
			
			if (!downloadedBlocks_.contains(block)) {
				downloadedBlocks_.addElement(block);
				torrent_.updateBytesDownloaded(data.length);
			}

			// If the piece is complete...
			if (downloadedBlocks_.size() >= blocks_.size()) {
				// Hash check
				boolean isHashCorrect = checkHash();
				if (isHashCorrect) {
					Log.v(LOG_TAG, "Hash OK!");
					isComplete_ = true;
					setFilesDownloaded();
					torrent_.pieceDownloaded(this, false);
					
					blocks_ = null;
					downloadedBlocks_ = null;
				} else {
					Log.v(LOG_TAG, "Hash FAILED!");

					isRequested_ = false;
					blocks_ = null;
					downloadedBlocks_ = null;
					unrequestedBlocks_ = null;
						
					torrent_.pieceHashFailed(this);
				}
				
				for (int i = 0; i < peers_.size(); i++) {
					peers_.elementAt(i).pieceHashCorrect(isHashCorrect);
				}
			}

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
	
	/** Returns whether the piece is correct or not by checking its hash. */
	public boolean checkHash() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			for (int i = 0; i < fragments_.size(); i++) {
				FileFragment fragment = fragments_.get(i);
				byte[] content = torrent_.read(fragment.file().getPath(), fragment.offset(), fragment.length());
				baos.write(content);
				Log.v(LOG_TAG, fragment.file().getRelativePath() + " " + fragment.offset() + " " + fragment.length());
			}
			
			SHA1 sha1 = new SHA1();
			sha1.update(baos.toByteArray());
			byte[] hash = SHA1.resultToByte(sha1.digest());
			
			Log.v(LOG_TAG, SHA1.resultToString(hash_));
			Log.v(LOG_TAG, SHA1.resultToString(hash));
			if (DrTorrentTools.byteArrayEqual(hash_, hash)) return true;
		} catch (Exception e) {
		} finally {
			try {
				baos.flush();
				baos.close();
			} catch (Exception e) {
			}
		}
		
		return false;
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
	
	/** Returns the length of the block in the given position. */
	public int getBlockLength(int index) {
		int lengthFromIndex = size_ - (index * DEFALT_BLOCK_LENGTH);
		if (lengthFromIndex > DEFALT_BLOCK_LENGTH) return DEFALT_BLOCK_LENGTH;
		
		return lengthFromIndex;
	}
	
	/** Returns whether the piece has unrequested block(s). */
	public boolean hasUnrequestedBlock() {
		if (isRequested_) {
			if (unrequestedBlocks_.size() > 0) return true;
		} else {
			if (!isComplete_) return true;
		}
		
		return false;
	}
	
	/** Returns an unrequested block. */
	public Block getUnrequestedBlock() {
		if (!isRequested_) {
			if (!isComplete_) calculateBlocks();
			else return null;
		}
		synchronized(unrequestedBlocks_) {
			if (unrequestedBlocks_.size() > 0) {
				Block block = unrequestedBlocks_.elementAt(0);
				unrequestedBlocks_.removeElementAt(0);
				return block;
			}
		}
		
		return null;
	}
	
	/** Adds a block to the the array of blocks to be requested. */
	public void addBlockToRequest(Block block) {
		unrequestedBlocks_.addElement(block);
	}
	
	/** Returns the file fragments of the piece. */
	public Vector<FileFragment> getFragments() {
		return fragments_;
	}
	
	/** The index of piece in the torrent. */
	public int index() {
        return index_;
    } 
	
	/** The length of piece in bytes. */
	public int size() {
		return size_;
	}
	
	/** Returns whether the piece has been requested or not. */
	public boolean isRequested() {
		return isRequested_;
	}
	
	/** Adds a peer to the list of the peers that gave the blocks of this piece. */
	public void addPeer(Peer peer) {
		if (!peers_.contains(peer)) peers_.addElement(peer);
	}
}
