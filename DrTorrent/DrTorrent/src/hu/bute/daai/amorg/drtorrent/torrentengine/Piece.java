package hu.bute.daai.amorg.drtorrent.torrentengine;

import java.util.Vector;

public class Piece {
	
	private Torrent torrent_;
	private byte[] hash_;
	private int length_;
	private Vector<FileFragment> fragments_;
	
	public Piece(Torrent torrent, byte[] hash, int length) {
		torrent_ = torrent;
		hash_ = hash;
		length_ = length;
		fragments_ = new Vector<FileFragment>();
	}
	
	public void addFileFragment(File file, int offset, int length) {
        fragments_.addElement(new FileFragment(file, offset, length));
    }  
	
	public int index() {
        return torrent_.indexOfPiece(this);
    } 
	
	public int getLength() {
		return length_;
	}
	
	public byte[] getBlock(int position, int length) {
		// TODO
		return null;
	}
	
	public int getDownloadedSize() {
        // TODO
		return 0; //downloadedSize_;
    }
}
