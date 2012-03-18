package hu.bute.daai.amorg.drtorrent.torrentengine;

public class File {
	public static final int STATUS_NOT_DOWNLOADED = 0;
    public static final int STATUS_DOWNLOADING    = 1;
    public static final int STATUS_DOWNLOADED     = 2;
    
    private String path_;	// Path relative to the torrent's parent directory.
    private int size_;
    private int downloadState_;
    private int fileNameLength_;
    
    public File(String path, int size) {
    	path_ = path;
    	size_ = size;
    }
    
    public String getPath() {
        return path_;
    }
    
    public int getSize() {
    	return size_;
    }
    
    public int getDownloadState() {
    	return downloadState_;
    }
    
    public void setDownloadState(int downloadState) {
    	downloadState_ = downloadState;
    }
}
