package hu.bute.daai.amorg.drtorrent.core;

import hu.bute.daai.amorg.drtorrent.core.peer.Peer;
import hu.bute.daai.amorg.drtorrent.core.torrent.Torrent;
import hu.bute.daai.amorg.drtorrent.file.FileManager;
import hu.bute.daai.amorg.drtorrent.util.Log;
import hu.bute.daai.amorg.drtorrent.util.Tools;
import hu.bute.daai.amorg.drtorrent.util.sha1.SHA1;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Vector;

/** This class represents a piece of a torrent. */
public class Piece {
	private final static String LOG_TAG = "Piece";
	
	public final static int DEFALT_BLOCK_LENGTH = 16384;	// default download block size 16kB (2^14)
	public final static int MAX_PIECE_SIZE_TO_READ_AT_ONCE = 16384; // 16 kB
	
	final private Torrent torrent_;
	final private byte[] hash_;
	final private int index_;
	final private int size_;
	private int downloaded_;
	final private ArrayList<FileFragment> fragments_;
	private int priority_ = File.PRIORITY_NORMAL;
	private int numberOfPeersHaveThis_ = 0;
	private boolean isRequested_ = false;
	private boolean isComplete_ = false;
	
	private Vector<Block> downloadingBlocks_ = null;
	private Bitfield unrequestedBitfield_ = null;
	private Bitfield downloadedBitfield_ = null;
	
	private Vector<Peer> peers_ = null;
	
	private boolean isHashChecked_ = false;
	
	/** 
	 * Constructor of the piece.
	 * 
	 * @param torrent The torrent that the piece belongs to.
	 * @param hash    The hash value of the piece.
	 * @param length  The length of the piece.
	 */
	public Piece(final Torrent torrent, final byte[] hash, final int index, int length) {
		torrent_ = torrent;
		hash_ = hash;
		index_ = index;
		size_ = length;
		downloaded_ = 0;
		fragments_ = new ArrayList<FileFragment>();
	}
	
	/** Calculates the blocks of the piece. - initialize the piece for download... */
	public void calculateBlocks() {		
		isRequested_ = true;
		Log.v(LOG_TAG, "Calculate blocks.");
		
		int blockCount = size_ / DEFALT_BLOCK_LENGTH;
		if (size_ % DEFALT_BLOCK_LENGTH > 0) blockCount++;
		
		unrequestedBitfield_ = new Bitfield(blockCount, true);
		downloadedBitfield_ = new Bitfield(blockCount, false);
		downloadingBlocks_ = new Vector<Block>();
		
		peers_ = new Vector<Peer>();
	}
	
	/** 
	 * Adds a new file fragment to the piece.
	 * 
	 * @param file   The file which the file fragment belongs to.
	 * @param offset The position of the file fragment within the file.
	 * @param length The length of the file fragment.
	 */
	public void addFileFragment(final File file, final long offset, final int length) {
        synchronized (fragments_) {
        	fragments_.add(new FileFragment(file, offset, length));
		}
    }
	
	/** 
	 * Appends the given block to its file(s).
	 * 
	 * @param data  The block that has to be appended.
	 * @param offset The position of the block within the piece.
	 * @param peer   The peer which the block was received from.
	 * @return Torrent error code.
	 */
	public int appendBlock(final byte[] data, final Block block, final Peer peer) {
		// If received an already received part.
		if (downloadingBlocks_ == null || !downloadingBlocks_.contains(block)) {
			Log.v(LOG_TAG, "Wrong block has been received: " + block.begin());
			return Torrent.ERROR_NONE;
		}
		downloadingBlocks_.removeElement(block);

		int blockPosition = 0;
		while (blockPosition < data.length) {
			
			// Calculating the file and the position within the file...
			File file = null;
			long filePosition = 0;
			long pos = 0;
			for (int i = 0; i < fragments_.size(); i++) {
				FileFragment fragment = (FileFragment) fragments_.get(i);
				// If the already downloaded part doesn't extend beyond the current fragment...
				// (...which means that a part of the current fragment was received.)
				if (block.begin() + blockPosition < (pos + fragment.length())) {
					file = fragment.file();
					filePosition = fragment.offset() + ((block.begin() + blockPosition) - pos);
					break;
				} else {
					pos += fragment.length();
				}
			}

			if (file != null) {
				int res = Torrent.ERROR_NONE;
				long fileRemainingLength = file.getSize() - filePosition; // Remaining length from the position

				Log.v(LOG_TAG, "Appending block to " + file.getFullPath());

				int rightSize;
				// If the file is not finished yet
				if (fileRemainingLength > (data.length - blockPosition)) {
					rightSize = data.length - blockPosition;
				}
				// If the end of a file was reached...
				else {
					rightSize = (int) fileRemainingLength;
				}

				res = FileManager.write(file.getFullPath(), filePosition, data, blockPosition, rightSize);
				if (res != Torrent.ERROR_NONE) return res;
				
				file.addDownloadedBytes(rightSize);

				blockPosition += rightSize;
			} else {
				return Torrent.ERROR_GENERAL;
			}
		}
		
		addPeer(peer);
		
		synchronized (this) {
			if (downloadedBitfield_ != null && !downloadedBitfield_.isBitSet(block.index())) {
				downloadedBitfield_.setBit(block.index());
				torrent_.updateBytesDownloaded(data.length);
				downloaded_ += data.length;
			}
	
			// If the piece is complete...
			if (downloadedBitfield_ != null && downloadedBitfield_.isFull()) {
				// Hash check
				boolean isHashCorrect = checkHash();
				if (isHashCorrect) {
					isComplete_ = true;
					//setFilesDownloaded();
					torrent_.onPieceDownloaded(this, false);
					
					downloadingBlocks_ = null;
					downloadedBitfield_ = null;
					unrequestedBitfield_ = null;
					
				} else {
					isRequested_ = false;
					downloadingBlocks_ = null;
					downloadedBitfield_ = null;
					unrequestedBitfield_ = null;
						
					torrent_.onPieceHashFailed(this);
				}
				
				for (int i = 0; i < peers_.size(); i++) {
					peers_.elementAt(i).onPieceHashChecked(isHashCorrect);
				}
				peers_ = null;
			}
		}
			
		return Torrent.ERROR_NONE;
	}
	
	/** Reads a block from its file(s). */
	public byte[] readBlock(final Block block) {
		int length = block.length();
		
		final ByteArrayOutputStream blockByteArray = new ByteArrayOutputStream();

		File file = null;
		long filePosition = 0;
		
		int i = 0;
		long pos = 0;
		for (; i < fragments_.size(); i++) {
			final FileFragment fragment = (FileFragment) fragments_.get(i);
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
				byte[] buffer = FileManager.read(file.getFullPath(), filePosition, (int) (file.getSize() - filePosition));
				if (buffer != null) {
					blockByteArray.write(buffer, 0, buffer.length);
					buffer = null;
					length -= (file.getSize() - filePosition);
				} else
					break;
			} else {
				byte[] buf = FileManager.read(file.getFullPath(), filePosition, length);
				if (buf != null) {
					blockByteArray.write(buf, 0, buf.length);
					length = 0;
					buf = null;
				}
				break;
			}

			if (i >= fragments_.size() - 1)
				break;

			file = ((FileFragment) fragments_.get(++i)).file();
			filePosition = 0;
		}

		if (length != 0) {
			return null;
		}

		return blockByteArray.toByteArray();
	}
	
	/** Returns whether the piece is correct or not by checking its hash. */
	public boolean checkHash() {
		isHashChecked_ = true;
		
		try {
			final SHA1 sha1 = new SHA1();
			for (int i = 0; i < fragments_.size(); i++) {
				final FileFragment fragment = fragments_.get(i);
				Log.v(LOG_TAG, "Checking hash: " + fragment.file().getRelativePath() + " " + fragment.offset() + " " +  fragment.length());
				
				FileManager.read(fragment.file().getFullPath(), fragment.offset(), fragment.length(), sha1);
			}
			
			final byte[] hash = SHA1.resultToByte(sha1.digest());
			
			//Log.v(LOG_TAG, SHA1.resultToString(hash_));
			//Log.v(LOG_TAG, SHA1.resultToString(hash));
			if (Tools.byteArrayEqual(hash_, hash)) {
				downloaded_ = size_;
				Log.v(LOG_TAG, "Hash OK!");
				return true;
			}
		} catch (Exception e) {
		}
		
		downloaded_ = 0;
		Log.v(LOG_TAG, "Hash FAILED!");
		return false;
	}

	/** Increments the number of peers having this piece. */
	public void incNumberOfPeersHaveThis() {
		numberOfPeersHaveThis_++;
	}
	
	/** Decrements the number of peers having this piece. */
	public void decNumberOfPeersHaveThis() {
		numberOfPeersHaveThis_--;
		if (numberOfPeersHaveThis_ < 0) {
			numberOfPeersHaveThis_ = 0;
		}
	}
	
	/** Returns the number of peers having this piece. */
	public int getNumberOfPeersHaveThis() {
		return numberOfPeersHaveThis_;
	}
	
	/** Returns the length of the block in the given position. */
	public int getBlockLength(final int index) {
		int lengthFromIndex = size_ - (index * DEFALT_BLOCK_LENGTH);
		if (lengthFromIndex > DEFALT_BLOCK_LENGTH) {
			return DEFALT_BLOCK_LENGTH;
		}
		
		return lengthFromIndex;
	}
	
	/** Returns whether the piece has unrequested block(s). */
	public boolean hasUnrequestedBlock() {
		if (isRequested_) {
			if (unrequestedBitfield_ != null) {
				return !unrequestedBitfield_.isNull();
			}
		} else {
			return !isComplete_;
		}
		
		return false;
	}
	
	/** Returns an unrequested block. */
	public Block getUnrequestedBlock() {
		if (!isRequested_) {
			if (!isComplete_) {
				calculateBlocks();
			} else {
				return null;
			}
		}
		
		int index = -1;
		if (unrequestedBitfield_ != null) {
			synchronized(unrequestedBitfield_) {
			
				index = unrequestedBitfield_.indexOfFirstSet();
				if (index != -1) {
					unrequestedBitfield_.unsetBit(index);
				}
			}
		}
				
		if (index != -1) {
			int length = DEFALT_BLOCK_LENGTH;
			if (index + 1 == unrequestedBitfield_.getLengthInBits()) {
				length = size_ - (index * DEFALT_BLOCK_LENGTH);
			}
			final Block block = new Block(index_, index * DEFALT_BLOCK_LENGTH, length);
			downloadingBlocks_.addElement(block);
			
			return block;
		}
		
		return null;
	}
	
	/** Adds a block to the the array of blocks to be requested. */
	public void addBlockToRequest(final Block block) {
		if (unrequestedBitfield_ != null) {
			synchronized (unrequestedBitfield_) {
				unrequestedBitfield_.setBit(block.index());
			}
		}
	}
	
	/** Returns whether the piece contains parts of the given file or not. */
	public boolean hasFile(final File file) {
		for (int i = 0; i < fragments_.size(); i++) {
			if (fragments_.get(i).file() == file) {
				return true;
			}
		}
		return false;
	}
	
	public void addFilesDownloadedBytes(final boolean isPositive) {
		for (int j = 0; j < fragments_.size(); j++) {
			final FileFragment fragment = fragments_.get(j);
			
			if (isPositive) {
				fragment.file().addDownloadedBytes(fragment.length());
			} else {
				fragment.file().addDownloadedBytes(-fragment.length());
			}
		}
	}
	
	/** Calculates the priority of the piece. */
	public void calculatePriority() {
		priority_ = 0;
		for (int i = 0; i < fragments_.size(); i++) {
			final FileFragment fragment = fragments_.get(i);
			int priority = fragment.file().getPriority();
			if (priority > priority_) priority_ = priority; 
		}
	}
	
	/** The priority of the piece. */
	public int priority() {
		return priority_;
	}
	
	/** The index of piece in the torrent. */
	public int index() {
        return index_;
    } 
	
	/** The length of piece in bytes. */
	public int size() {
		return size_;
	}
	
	/** Returns the downloaded size. */
	public int downloaded() {
		return downloaded_;
	}
	
	/** Returns whether the piece can be downloaded or not. It depends on whether the associated files are created or not. */
	public boolean canDownload() {
		for (FileFragment fragment : fragments_) {
			if (!fragment.isCreated()) {
				return false;
			}
		}
		return true;
	}
	
	/** Returns whether the piece has been requested or not. */
	public boolean isRequested() {
		return isRequested_;
	}
	
	/** Returns whether the piece is complete or not. */
	public boolean isComplete() {
		return isComplete_;
	}
	
	/** Sets the piece complete. */
	public void setComplete() {
		isComplete_ = true;
		isHashChecked_ = true;
		downloaded_ = size_;
	}
	
	/** Returns whether the hash is already checked or not. */
	public boolean isHashChecked() {
		return isHashChecked_;
	}
	
	/** Adds a peer to the list of the peers that gave the blocks of this piece. */
	public void addPeer(final Peer peer) {
		if (peers_ != null && !peers_.contains(peer)) {
			peers_.addElement(peer);
		}
	}
	
	/** Compares two pieces by their priority. */
	public static class PiecePriorityComparator implements Comparator<Piece> {
		@Override
		public int compare(Piece lhs, Piece rhs) {
			if (lhs.priority_ > rhs.priority_) {
				return -1;
			} else if (lhs.priority_ == rhs.priority_) {
				return 0;
			}
			return 1;
		}
	}
	
	/** Compares two pieces by their index and priority. */
	public static class PieceSequenceAndPriorityComparator implements Comparator<Piece> {
		@Override
		public int compare(Piece lhs, Piece rhs) {
			if (lhs.priority_ > rhs.priority_) {
				return -1;
			} else if (lhs.priority_ == rhs.priority_) {
				if (lhs.index_ < rhs.index_) {
					return -1;
				} else if (lhs.index_ == rhs.index_) {
					return 0;
				}
			}
			return 1;
		}
	}
}
