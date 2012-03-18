package hu.bute.daai.amorg.drtorrent.torrentengine;


/** Peer */
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
		bitfield_ = new Bitfield(piecesCount, false);
	}
	
	public void connect(Torrent torrent) {
		if (connection_ == null) connection_ = new PeerConnection(this, torrent, torrent.getTorrentManager());
		connection_.connect();
	}
	
	public void disconnect() {
		if (connection_ != null) connection_.close("Peer has been disconnected...");
	}
	
	public void terminate() {
		if (connection_ != null) connection_.close(PeerConnection.EDeletePeer, "Invalid hash! Terminating connection.");
    }
	
	public void onTimer() {
		if (connection_ != null) {
			connection_.onTimer();
		}
	}
	
	public void havePieces(byte[] bitfield, Torrent torrent) {
		Bitfield tempBitfield = new Bitfield(bitfield);

		for (int i = 0; i < torrent.pieceCount(); i++) {
			if (tempBitfield.isBitSet(i) && (!bitfield_.isBitSet(i))) {
				torrent.incNumberOfPeersHavingPiece(i);
			}
		}
		bitfield_.set(bitfield);
	}
	
	public void havePiece(int index, Torrent torrent) {
		bitfield_.setBit(index);
		torrent.incNumberOfPeersHavingPiece(index);
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
