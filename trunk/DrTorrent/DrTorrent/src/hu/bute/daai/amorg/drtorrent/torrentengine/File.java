package hu.bute.daai.amorg.drtorrent.torrentengine;

import hu.bute.daai.amorg.drtorrent.R;

public class File {
	public static final int STATUS_NOT_DOWNLOADED = 0;
    public static final int STATUS_DOWNLOADING    = 1;
    public static final int STATUS_DOWNLOADED     = 2;
    
    public static final int PRIORITY_SKIP 	= 0;
    public static final int PRIORITY_LOW  	= 1;
    public static final int PRIORITY_NORMAL = 2;
    public static final int PRIORITY_HIGH	= 3;
    
    private Torrent torrent_;
    private int index_;				// File index
    private int begin_;				// The index of the first piece that contains parts of the file
    private String path_;			// Torrent's path
    private String relativePath_;	// Path relative to the torrent's parent directory.
    
    private long size_;
    private long downloadedSize_ = 0;
    
    private int priority_ = PRIORITY_NORMAL;
    private int downloadState_;
    private boolean isChanged_ = false;
    
    public File(Torrent torrent, int index, int begin, String path, String relativePath, long size) {
    	torrent_ = torrent;
    	index_ = index;
    	begin_ = begin;
    	path_ = path;
    	relativePath_ = relativePath;
    	size_ = size;
    }
    
    /** Checks the hash of pieces that contain parts of the file. */
    public void checkHash(boolean shouldCheck) {
    	for (int i = begin_; i < torrent_.pieceCount(); i++) {
			Piece piece = torrent_.getPiece(i);
			if (piece.hasFile(this)) {
				if (!piece.isHashChecked()) {
					if (shouldCheck && piece.checkHash()) {
						torrent_.pieceDownloaded(piece, true);
					}
					torrent_.addCheckedBytes(piece.size());
				}
			} else {
				break;
			}
			if (torrent_.getStatus() == R.string.status_stopped) return;
		}
    }
    
    public String getFullPath() {
        return path_.concat(relativePath_);
    }
    
    public String getRelativePath() {
    	return relativePath_;
    }
    
    public long getSize() {
    	return size_;
    }
    
    public long getDownloadedSize() {
    	return downloadedSize_;
    }
    
    /** Adds the given bytes to the downloaded bytes. */
    public void addDownloadedBytes(long bytes) {
    	downloadedSize_ += bytes;
    	isChanged_ = true;
    }
    
    public int getPriority() {
    	return priority_;
    }
    
    public void setPriority(int priority) {
    	if (priority_ == PRIORITY_SKIP && priority > 0) checkHash(true);
    	priority_ = priority;
    	isChanged_ = true;
    }
    
    public int getDownloadState() {
    	return downloadState_;
    }
    
    public void setDownloadState(int downloadState) {
    	downloadState_ = downloadState;
    	isChanged_ = true;
    }
    
    public int index() {
    	return index_;
    }
    
    public boolean isChanged() {
    	if (isChanged_) {
    		isChanged_ = false;
    		return true;
    	}
    	return false;
    }
}
