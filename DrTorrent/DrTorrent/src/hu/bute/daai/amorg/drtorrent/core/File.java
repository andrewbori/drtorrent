package hu.bute.daai.amorg.drtorrent.core;

import hu.bute.daai.amorg.drtorrent.core.torrent.Torrent;

/** Class that represent the files of a torrent. */
public class File {    
    public static final int PRIORITY_SKIP 	= 0;
    public static final int PRIORITY_LOW  	= 1;
    public static final int PRIORITY_NORMAL = 2;
    public static final int PRIORITY_HIGH	= 3;
    
    final private Torrent torrent_;
    final private int index_;			// File index
    final private int begin_;			// The index of the first piece that contains parts of the file
    private String relativePath_;		// Path relative to the torrent's parent directory.
    
    final private long size_;
    private long createdSize_ = 0;
    private long downloadedSize_ = 0;
    
    private int priority_ = PRIORITY_NORMAL;
    private boolean isChanged_ = true;
    
    /** Constructor with the torrent and the file's properties. */
    public File(final Torrent torrent, final int index, final int begin, /*final String path,*/ final String relativePath, final long size) {
    	torrent_ = torrent;
    	index_ = index;
    	begin_ = begin;
    	relativePath_ = relativePath;
    	size_ = size;
    }
    
    /** Checks the hash of pieces that contain parts of the file. */
    public void checkHash(final boolean shouldCheck) {
    	for (int i = begin_; i < torrent_.pieceCount(); i++) {
			final Piece piece = torrent_.getPiece(i);
			if (piece.hasFile(this)) {
				if (!piece.isHashChecked()) {
					if (shouldCheck && piece.checkHash()) {
						torrent_.onPieceDownloaded(piece, true);
					}
					torrent_.addCheckedBytes(piece.size());
				}
			} else {
				break;
			}
			if (torrent_.getStatus() == Torrent.STATUS_STOPPED) {
				return;
			}
		}
    }
    
    /** Returns the full path of the file. */
    public String getFullPath() {
        return torrent_.getPath().concat(relativePath_);
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
    public synchronized void addDownloadedBytes(final long bytes) {
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
    public void setPriority(final int priority) {
    	if (priority_ == PRIORITY_SKIP && priority > 0) {
    		checkHash(true);
    	}
    	priority_ = priority;
    	isChanged_ = true;
    }
    
    /** Returns the index of the file. */
    public int index() {
    	return index_;
    }
    
    /** Returns whether the file is changed or not. */
    public boolean isChanged() {
    	if (isChanged_) {
    		isChanged_ = false;
    		return true;
    	}
    	return false;
    }

    /** Sets the created size of the file. */
	public void setCreatedSize(final long createdSize) {
		createdSize_ = createdSize;
	}
	
	/** Returns the created size of the file. */
	public long getCreatedSize() {
		return createdSize_;
	}

	/** Returns the index of the first piece that contains a fragment of the file. */
	public int getIndexOfFirstPiece() {
		return begin_;
	}

	public boolean isComplete() {
		return downloadedSize_ >= size_;
	}
}
