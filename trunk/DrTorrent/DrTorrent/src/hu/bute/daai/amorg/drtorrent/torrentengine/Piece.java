package hu.bute.daai.amorg.drtorrent.torrentengine;

public class Piece {
	
	private Torrent torrent;
	private byte[] hash;
	private int length;
	
	public Piece(Torrent torrent, byte[] hash, int length) {
		this.torrent = torrent;
		this.hash = hash;
		this.length = length;
	}
}
