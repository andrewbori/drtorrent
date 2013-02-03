package hu.bute.daai.amorg.drtorrent.torrentengine;

import android.os.SystemClock;

public class Block {

	final private Piece piece_;
	final private int begin_;
	final private int length_;
	
	private boolean isRequested_;	// True if a peer got this to request to download
	private boolean isDownloaded_;
	private long requestTime_;
	
	public Block(final Piece piece, final int blockBegin, final int blockLength) {
		piece_ = piece;
		begin_ = blockBegin;
		length_ = blockLength;
		isDownloaded_ = false;
		isRequested_ = false;
		requestTime_ = -1;
	}
	
	public void setRequested() {
		isRequested_ = true;
		requestTime_ = SystemClock.elapsedRealtime();
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
	
	public void setRequestTime(final long requestTime) {
		requestTime_ = requestTime;
	}
	
	public long getRequestTime() {
		return requestTime_;
	}
	
	public long getRequestDelay() {
		return SystemClock.elapsedRealtime() - requestTime_;
	}
	
	public int pieceIndex() {
		return piece_.index();
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
