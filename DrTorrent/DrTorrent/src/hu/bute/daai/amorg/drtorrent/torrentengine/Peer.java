package hu.bute.daai.amorg.drtorrent.torrentengine;


/** Class representing the Peer. */
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
	
	/** Connects to the peer. */
	public void connect(Torrent torrent) {
		if (connection_ == null) connection_ = new PeerConnection(this, torrent, torrent.getTorrentManager());
		connection_.connect();
	}
	
	/** Disconnects the peer. */
	public void disconnect() {
		if (connection_ != null) connection_.close("Peer has been disconnected...");
	}
	
	/** Terminates the connection. */
	public void terminate() {
		if (connection_ != null) connection_.close(PeerConnection.ERROR_DELETE_PEER, "Terminating connection.");
    }
	
	/** Schedules the connection. */
	public void onTimer() {
		if (connection_ != null) {
			connection_.onTimer();
		}
	}
	
	/** Sets the bitfield of the peer. */
	public void peerHasPieces(byte[] bitfield, Torrent torrent) {
		Bitfield tempBitfield = new Bitfield(bitfield);

		for (int i = 0; i < torrent.pieceCount(); i++) {
			if (tempBitfield.isBitSet(i) && (!bitfield_.isBitSet(i))) {
				torrent.incNumberOfPeersHavingPiece(i);
			}
		}
		bitfield_.set(bitfield);
		
		torrent.calculateRarestPieces();
	}
	
	/** Sets the given bit of the bitfield of the peer. */
	public void peerHasPiece(int index, Torrent torrent) {
		bitfield_.setBit(index);
		torrent.incNumberOfPeersHavingPiece(index);
		
		torrent.calculateRarestPieces();
	}
	
	/** Returns whether the peer has the given piece or not. */
	public boolean hasPiece(int index) {
		return bitfield_.isBitSet(index);
	}
	
	/** Notify the peer that the client have the given piece. */
	public void notifyThatClientHavePiece(int pieceIndex) {
		connection_.sendHaveMessage(pieceIndex);
	}
	
	/** Resets the error counter. */
	public void resetErrorCounter() {
		// TODO
	}
	
	/** Returns the address of the peer. */
	public String getAddress() {
		return address_;
	}
	
	/** Returns the port of the peer. */
	public int getPort() {
		return port_;
	}
	
	/** Returns the ID of the peer. */
	public String getPeerId() {
		return peerId_;
	}
	
	/** Returns the bitfield. */
	public Bitfield getBitfield() {
		return bitfield_;
	}
}
