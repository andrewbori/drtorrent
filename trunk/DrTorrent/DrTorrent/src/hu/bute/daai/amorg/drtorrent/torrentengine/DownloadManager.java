package hu.bute.daai.amorg.drtorrent.torrentengine;

public class DownloadManager {

	private final Speed downloadSpeed_;
	private int latestDownloadedBytes_ = 0;
	
	public DownloadManager() {
		downloadSpeed_ = new Speed();
	}

	public void onTimer(long time) {
		synchronized (downloadSpeed_) {
			downloadSpeed_.addBytes(latestDownloadedBytes_, time);
			latestDownloadedBytes_ = 0;
		}
	}
	
	public void addBytes(int bytes) {
		synchronized(downloadSpeed_) {
			latestDownloadedBytes_ += bytes;
		}
	}
	
	public int getDownloadSpeed() {
		return downloadSpeed_.getSpeed();
	}

	public int getLatestDownloadedBytes() {
		return latestDownloadedBytes_;
	}
}
