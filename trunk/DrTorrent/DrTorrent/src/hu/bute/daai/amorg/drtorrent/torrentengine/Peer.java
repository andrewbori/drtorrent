package hu.bute.daai.amorg.drtorrent.torrentengine;

public class Peer {

	private String address_;
	private int port_;
	private String peerId_;
	private Bitfield bitfield_;
	
	private PeerConnection connection_ = null;
	
	public Peer(String address, int port, String peerId, int piecesCount) {
		address_ = address;
		port_ = port;
		peerId_ = peerId;
	}
	
	public void connect(Torrent torrent) {
		if (connection_ == null) connection_ = new PeerConnection(this, torrent, torrent.getTorrentManager());
		connection_.connect();
	}
	
	public void onTimer() {
		if (connection_ != null) {
			connection_.onTimer();
		}
	}
	
	public void havePieces(byte[] b, Torrent t) {
		// TODO
	}
	
	public void havePiece(int i, Torrent t) {
		// TODO
	}
	
	public void resetErrorCounter() {
		// TODO
	}
	
	public String getAddress() {
		return address_;
	}
	
	public int getPort() {
		return port_;
	}
	
	public String getPeerId() {
		return peerId_;
	}
	
	public Bitfield getBitfield() {
		return bitfield_;
	}
}
