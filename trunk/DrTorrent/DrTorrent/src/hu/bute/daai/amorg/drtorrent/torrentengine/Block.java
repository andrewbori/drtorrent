package hu.bute.daai.amorg.drtorrent.torrentengine;

public class Block {

	private int pieceIndex_;
	private int begin_;
	private int length_;
	
	private boolean isRequested_;
	private long requestTime_;
	
	public Block(int pieceIndex, int blockBegin, int blockLength) {
		pieceIndex_ = pieceIndex;
		begin_ = blockBegin;
		length_ = blockLength;
		isRequested_ = false;
		requestTime_ = -1;
	}
	
	public void setRequested() {
		isRequested_ = true;
		requestTime_ = System.currentTimeMillis();
	}
	
	public void setNotRequested() {
		isRequested_ = false;
		requestTime_ = -1;
	}
	
	public boolean isRequested() {
		return isRequested_;
	}
	
	public void setRequestTime(long requestTime) {
		requestTime_ = requestTime;
	}
	
	public long getRequestTime() {
		return requestTime_;
	}
	
	public long getRequestDelay() {
		return System.currentTimeMillis() - requestTime_;
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
