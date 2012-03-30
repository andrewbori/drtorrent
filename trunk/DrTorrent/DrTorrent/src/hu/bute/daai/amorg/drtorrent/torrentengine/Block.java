package hu.bute.daai.amorg.drtorrent.torrentengine;

public class Block {

	private int pieceIndex_;
	private int begin_;
	private int length_;
	
	private boolean isRequested_;
	
	public Block(int pieceIndex, int blockBegin, int blockLength) {
		pieceIndex_ = pieceIndex;
		begin_ = blockBegin;
		length_ = blockLength;
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
	
	public int pieceIndex() {
		return pieceIndex_;
	}
	
	public int begin() {
		return begin_;
	}
	
	public int length() {
		return length_;
	}
}
