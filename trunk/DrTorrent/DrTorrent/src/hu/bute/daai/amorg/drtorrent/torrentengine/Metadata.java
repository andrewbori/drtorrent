package hu.bute.daai.amorg.drtorrent.torrentengine;

/** Represents the Metadata of a torrent. */
public class Metadata {
	public final static int BLOCK_SIZE = 16384;
	
	final private byte[] data_;
	final private boolean[] hasBlock_;
	final private boolean[] isRequested_;
	final private int size_;
	final private int count_;
	
	/** Constructs the Metadata with the given size. */
	public Metadata(final int size) {
		size_ = size;
		data_ = new byte[size];
		
		if (size_ % BLOCK_SIZE > 0) {
			count_ = (size_ / BLOCK_SIZE) + 1;
		} else {
			count_ = size_ / BLOCK_SIZE;
		}
		
		hasBlock_ = new boolean[count_];
		isRequested_ = new boolean[count_];
	}
	
	/** Adds the data at the index of block. */
	public void add(final int index, final byte[] data) {
		System.arraycopy(data, 0, data_, index * BLOCK_SIZE, data.length);
		hasBlock_[index] = true;
	}
	
	/** Returns the data. */
	public byte[] getData() {
		return data_;
	}
	
	/** Returns whether the block at the given index has been already downloaded or not. */
	public boolean hasBlock(final int index) {
		return hasBlock_[index];
	}
	
	/** Returns whether the block at the given index is already requested or not. */
	public boolean isRequested(final int index) {
		return isRequested_[index];
	}
	
	/** Sets the block at the given index isRequested. */
	public void setRequested(final int index, final boolean isRequested) {
		isRequested_[index] = isRequested;
	}
	
	/** Returns whether there is any unrequested block or not. */
	public boolean hasUnrequestedBlock() {
		for (int i = 0; i < isRequested_.length; i++) {
			if (isRequested_[i] == false) {
				return true;
			}
		}
		return false;
	}
	
	/** Returns whether the Metadata is complete or not. */
	public boolean isComplete() {
		for (int i = 0; i < hasBlock_.length; i++) {
			if (hasBlock_[i] == false) {
				return false;
			}
		}
		return true;
	}
	
	/** Returns the size of the metadata. */
	public int getSize() {
		return size_;
	}
	
	/** Returns the count of blocks. */
	public int getBlockCount() {
		return count_;
	}
}
