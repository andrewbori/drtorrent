package hu.bute.daai.amorg.drtorrent.torrentengine;

public class Block {

	private Piece piece_;
	private int begin_;
	private int length_;
	
	private boolean isRequested_;
	private boolean isDownloaded_;
	private long requestTime_;
	
	public Block(Piece piece, int blockBegin, int blockLength) {
		piece_ = piece;
		begin_ = blockBegin;
		length_ = blockLength;
		isDownloaded_ = false;
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
	
	public void setDownloaded() {
		isDownloaded_ = true;
	}
	
	public boolean isDownloaded() {
		return isDownloaded_;
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
		return piece_.index();
	}
	
	public int begin() {
		return begin_;
	}
	
	public int length() {
		return length_;
	}
}
