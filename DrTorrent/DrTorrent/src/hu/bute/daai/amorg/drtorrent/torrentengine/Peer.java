package hu.bute.daai.amorg.drtorrent.torrentengine;

import java.util.Vector;


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
		if (connection_ == null) connection_ = new PeerConnection(this, torrent);
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
	
	/** Returns whether the peer has requested the block or not. */
	public boolean hasBlock(Block block) {
		return connection_.hasBlock(block);
	}
	
	/** Cancels the request of the given block. */
	public void cancelBlock(Block block) {
		if (connection_ != null) connection_.cancelBlock(block);
	}
	
	/** Notify the peer that the client have the given piece. */
	public void notifyThatClientHavePiece(int pieceIndex) {
		connection_.sendHaveMessage(pieceIndex);
	}
	
	/** Sets our chocking state. */
	public void setChoking(boolean choking) {
		connection_.setChoking(choking);
	}
	
	/** Resets the error counter. */
	public void resetErrorCounter() {
		// TODO
	}
	
	/** Returns the number of requests have been sent to the peer. */
	public int getRequestsCount() {
		return ((connection_ != null) ? connection_.getRequestsCount() : 0);
	}
	
	/** Returns the number of requests have not been sent to the peer yet. */
	public int getRequestsToSendCount() {
		return ((connection_ != null) ? connection_.getRequestsToSendCount() : 0);
	}
	
	/** Returns the download speed. */
	public int getDownloadSpeed() {
		return ((connection_ != null) ? connection_.getDownloadSpeed() : 0);
	}
	
	/** Returns the count of downloaded bytes. */
	public int getDownloaded() {
		return ((connection_ != null) ? connection_.getDownloaded() : 0);
	}
	
	/** Returns the address and the port of the peer. Format: "address:port". */
	public String getAddressPort() {
		return address_ + ":" + port_;
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
