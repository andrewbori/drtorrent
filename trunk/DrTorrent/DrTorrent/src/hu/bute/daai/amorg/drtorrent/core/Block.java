package hu.bute.daai.amorg.drtorrent.core;

/** Class that represents the blocks of a piece. */
public class Block {

	final private int pieceIndex_;
	final private int begin_;
	final private int length_;
	
	private boolean isRequested_;	// True if a peer got this to request to download
	private boolean isDownloaded_;
	
	public Block(final int pieceIdex, final int blockBegin, final int blockLength) {
		pieceIndex_ = pieceIdex;
		begin_ = blockBegin;
		length_ = blockLength;
		isDownloaded_ = false;
		isRequested_ = false;
	}
	
	public void setRequested() {
		isRequested_ = true;
	}
	
	public void setNotRequested() {
		isRequested_ = false;
	}
	
	public boolean isRequested() {
		return isRequested_;
	}
	
	public void setDownloaded() {
		isDownloaded_ = true;
	}
	
	public boolean isDownloaded() {
		return isDownloaded_;
	}
	
	public int pieceIndex() {
		return pieceIndex_;
	}
	
	public int index() {
		return begin_ / Piece.DEFALT_BLOCK_LENGTH;
	}
	
	public int begin() {
		return begin_;
	}
	
	public int length() {
		return length_;
	}
}
