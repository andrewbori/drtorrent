package hu.bute.daai.amorg.drtorrent.torrentengine;

import java.util.Vector;

public class Piece {
	
	private Torrent torrent_;
	private byte[] hash_;
	private int length_;
	private Vector<FileFragment> fragments_;
	
	public Piece(Torrent torrent, byte[] hash, int length) {
		this.torrent_ = torrent;
		this.hash_ = hash;
		this.length_ = length;
		fragments_ = new Vector<FileFragment>();
	}
	
	public void addFileFragment(File file, int offset, int length) {
        fragments_.addElement(new FileFragment(file, offset, length));
    }  
	
	public int getLength() {
		return length_;
	}
}
