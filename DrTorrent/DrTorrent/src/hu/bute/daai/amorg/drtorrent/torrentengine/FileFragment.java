package hu.bute.daai.amorg.drtorrent.torrentengine;


public class FileFragment {

	private File file_;
	private int offset_;
	private int length_;
	
	public FileFragment(File file, int offset, int length) {
		file_ = file;
		offset_ = offset;
		length_ = length;
	}
}