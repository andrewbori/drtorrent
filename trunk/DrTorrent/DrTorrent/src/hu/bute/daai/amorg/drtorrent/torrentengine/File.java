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
    private boolean isCreated_ = false;
    private int downloadState_;
    private boolean isChanged_ = false;
    
    /** Constructor with the torrent and the file's properties. */
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
    
    /** Returns the full path of the file. */
    public String getFullPath() {
        return path_.concat(relativePath_);
    }
    
    /** Returns the file's path relative to the torrent. */
    public String getRelativePath() {
    	return relativePath_;
    }
    
    /** Returns the size (in bytes) of the file. */
    public long getSize() {
    	return size_;
    }
    
    /** Returns the downloaded size of the file. */
    public long getDownloadedSize() {
    	return downloadedSize_;
    }
    
    /** Adds the given bytes to the downloaded bytes. */
    public void addDownloadedBytes(long bytes) {
    	downloadedSize_ += bytes;
    	isChanged_ = true;
    }
    
    /** Returns the priority of the file.<br><br>
     * 
     *  @return	
     *  0 = PRIORITY_SKIP<br>
     *	1 = PRIORITY_LOW<br>
     *	2 = PRIORITY_NORMAL<br>
     *	3 = PRIORITY_HIGH
     */
    public int getPriority() {
    	return priority_;
    }
    
    /** Sets the priority of the file.<br><br>
     * 
     * 	@param priority priotity of the file<br>
     *  0 = PRIORITY_SKIP<br>
     *	1 = PRIORITY_LOW<br>
     *	2 = PRIORITY_NORMAL<br>
     *	3 = PRIORITY_HIGH
     */
    public void setPriority(int priority) {
    	if (priority_ == PRIORITY_SKIP && priority > 0) checkHash(true);
    	priority_ = priority;
    	isChanged_ = true;
    }
    
    /** Returns the download status of the file. */
    public int getDownloadState() {
    	return downloadState_;
    }
    
    /** Sets the download status of the file. */
    public void setDownloadState(int downloadState) {
    	downloadState_ = downloadState;
    	isChanged_ = true;
    }
    
    /** Returns the index of the file. */
    public int index() {
    	return index_;
    }
    
    /** Sets the file created. */
    public void setCreated(boolean isCreated) {
    	isCreated_ = isCreated;
    }
    
    /** Returns whether the file is created or not. */
    public boolean isCreated() {
    	return isCreated_;
    }
    
    /** Returns whether the file is changed or not. */
    public boolean isChanged() {
    	if (isChanged_) {
    		isChanged_ = false;
    		return true;
    	}
    	return false;
    }
}
