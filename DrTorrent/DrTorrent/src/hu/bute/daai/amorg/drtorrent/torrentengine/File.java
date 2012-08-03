package hu.bute.daai.amorg.drtorrent.torrentengine;

public class File {
	public static final int STATUS_NOT_DOWNLOADED = 0;
    public static final int STATUS_DOWNLOADING    = 1;
    public static final int STATUS_DOWNLOADED     = 2;
    
    public static final int PRIORITY_SKIP 	= 0;
    public static final int PRIORITY_LOW  	= 1;
    public static final int PRIORITY_NORMAL = 2;
    public static final int PRIORITY_HIGH	= 3;
    
    private String path_;	// Path relative to the torrent's parent directory.
    private String relativePath_;
    private int size_;
    private int priority_ = PRIORITY_NORMAL;
    private int downloadState_;
    
    public File(String path, String relativePath, int size) {
    	path_ = path;
    	relativePath_ = relativePath;
    	size_ = size;
    }
    
    public String getPath() {
        return path_.concat(relativePath_);
    }
    
    public String getRelativePath() {
    	return relativePath_;
    }
    
    public int getSize() {
    	return size_;
    }
    
    public int getPriority() {
    	return priority_;
    }
    
    public void setPriority(int priority) {
    	priority_ = priority;
    }
    
    public int getDownloadState() {
    	return downloadState_;
    }
    
    public void setDownloadState(int downloadState) {
    	downloadState_ = downloadState;
    }
}
