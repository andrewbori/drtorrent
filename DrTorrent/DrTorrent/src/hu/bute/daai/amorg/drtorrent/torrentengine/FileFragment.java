package hu.bute.daai.amorg.drtorrent.torrentengine;

/** This class represents a fragment of a file. */
public class FileFragment {

	private File file_;
	private long offset_;
	private int length_;
	
	public FileFragment(File file, int offset, int length) {
		file_ = file;
		offset_ = offset;
		length_ = length;
	}
	
	public File file() {
		return  file_;
	}
	
	public long offset() {
		return  offset_;
	}
	
	public int length() {
		return  length_;
	}
}