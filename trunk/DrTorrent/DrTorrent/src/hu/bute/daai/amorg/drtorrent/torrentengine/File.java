package hu.bute.daai.amorg.drtorrent.torrentengine;

public class File {
	public static final int STATUS_NOT_DOWNLOADED = 0;
    public static final int STATUS_DOWNLOADING    = 1;
    public static final int STATUS_DOWNLOADED     = 2;
    
    private int size_;
    private String path_;		// Path relative to the torrent's parent directory.
    private int downloadState_;
    int fileNameLength_;
    
    public File(String path, int size) {
    	path_ = path;
    	size_ = size;
    }
    
    public int getSize() {
    	return size_;
    }
}
