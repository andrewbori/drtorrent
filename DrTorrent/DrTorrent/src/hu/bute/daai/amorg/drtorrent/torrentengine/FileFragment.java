package hu.bute.daai.amorg.drtorrent.torrentengine;

/** This class represents a fragment of a file. */
public class FileFragment {

	private File file_;
	private int offset_;
	private int length_;
	
	public FileFragment(File file, int offset, int length) {
		file_ = file;
		offset_ = offset;
		length_ = length;
	}
	
	public File getFile() {
		return  file_;
	}
	
	public int getOffset() {
		return  offset_;
	}
	
	public int getLength() {
		return  length_;
	}
}