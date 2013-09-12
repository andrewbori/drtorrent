package hu.bute.daai.amorg.drtorrent.core;

/** This class represents a fragment of a file. */
public class FileFragment {

	final private File file_;
	final private long offset_;
	final private int length_;
	
	public FileFragment(final File file, final long offset, final int length) {
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
	
	public boolean isCreated() {
		return (file_.getCreatedSize() >= offset_ + length_);
	}
}