package hu.bute.daai.amorg.drtorrent.torrentengine;

import java.util.Vector;

public class Piece {
	
	private Torrent torrent_;
	private byte[] hash_;
	private int length_;	
	private Vector<FileFragment> fragments_;
	private int numberOfPeersHaveThis_;
	
	public Piece(Torrent torrent, byte[] hash, int length) {
		torrent_ = torrent;
		hash_ = hash;
		length_ = length;
		fragments_ = new Vector<FileFragment>();
		numberOfPeersHaveThis_ = 0;
	}
	
	public void addFileFragment(File file, int offset, int length) {
        fragments_.addElement(new FileFragment(file, offset, length));
    }
	
	public int appendBlock(byte[] block, int i, Peer peer) {
		// TODO
		return Torrent.ERROR_NONE;
	}
	
	public void incNumberOfPeersHaveThis() {
		numberOfPeersHaveThis_++;
	}
	
	public void decNumberOfPeersHaveThis() {
		numberOfPeersHaveThis_--;
	}
	
	public int getNumberOfPeersHaveThis() {
		return numberOfPeersHaveThis_;
	}
	
	
	public byte[] getBlock(int position, int length) {
		// TODO
		return null;
	}
	
	/** The index of piece in the torrent. */
	public int index() {
        return torrent_.indexOfPiece(this);
    } 
	
	/** The length of piece in bytes. */
	public int length() {
		return length_;
	}
	
	/** The downloaded fragments in bytes. */
	public int downloaded() {
        // TODO
		return 0; //downloadedSize_;
    }
	
	/** The remaining fragments in bytes. */
	public int remaining() {
		// TODO
		return length_;
	}
}
