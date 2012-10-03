package hu.bute.daai.amorg.drtorrent.torrentengine;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Vector;

import android.os.SystemClock;
import android.util.Log;



/** Class representing the Peer. */
public class Peer {
	private final static String LOG_TAG = "Peer";
	private final static int TIMEOUT_RECONNECTION  = 1000 * 3 * 60;
	
	private static int ID = 0;
	
	private int id_;
	private String address_;
	private int port_;
	private String peerId_;
	private Bitfield bitfield_;
	
	private TorrentManager torrentManager_ = null;
	private Torrent torrent_ = null;
	
	private int failedPieceCount_ = 0;
	
	private boolean isIncomingConnection_ = false;
	private PeerConnection connection_ = null;
	
	private Vector<Block> blocksDownloading_;
	
	private long latestDownloaded_ = 0;
	private long downloaded_ = 0;
	private Speed downloadSpeed_ = null;
	
	private int tcpTimeoutCount_ = 0;
	private long disconnectionTime_ = 0;
	
	public Peer(String address, int port, String peerId, int piecesCount) {
		address_ = address;
		port_ = port;
		peerId_ = peerId;
		bitfield_ = new Bitfield(piecesCount, false);
        blocksDownloading_ = new Vector<Block>();
		
		id_ = ++ID;
	}
	
	public Peer(Socket socket, TorrentManager torrentManager) {
		address_ = socket.getInetAddress().getHostAddress();
		port_ = socket.getPort();
		torrentManager_ = torrentManager;
        blocksDownloading_ = new Vector<Block>();
		
		id_ = ++ID;
	}
	
	/** Connects to the peer. */
	public Runnable connect(Torrent torrent) {
		downloadSpeed_ = new Speed();
		torrent_ = torrent;
		if (connection_ == null) connection_ = new PeerConnection(this, torrent, false);
		return connection_.connect();
	}
	
	public void connect(Socket socket) {
		downloadSpeed_ = new Speed();
		if (connection_ == null) connection_ = new PeerConnection(this, null, true);
		connection_.connect(socket);
	}
	
	/** Disconnects the peer. */
	public void disconnect() {
		if (connection_ != null) connection_.close("Peer has been disconnected...");
	}
	
	public void disconnected() {
		torrent_.peerDisconnected(this);
		cancelBlocks();
		disconnectionTime_ = SystemClock.elapsedRealtime();
		downloadSpeed_ = null;
	}
	
	/** Schedules the connection. */
	public void onTimer() {
		if (connection_ != null) {
			connection_.onTimer();
		}
	}
	
	/** Issues the download requests. */
	public synchronized ArrayList<Block> issueDownload() {
		synchronized (blocksDownloading_) {
			//int number =  getLatestBlocksCount() + 5 - (blocksToDownload_.size() + blocksDownloading_.size());
			int number;
			/*if (getLatestBlocksCount() == 0 && (blocksToDownload_.isEmpty() && blocksDownloading_.isEmpty())) number = 1;
			else number =  (getLatestBlocksCount()) / 4 + 2 - (blocksToDownload_.size() + blocksDownloading_.size());*/
			if (getLatestBlocksCount() == 0 && (blocksDownloading_.isEmpty())) number = 3;
			else number =  (int) (getBlockSpeed() * 3.0) - blocksDownloading_.size();
			if (number < blocksDownloading_.size() * 0.2) return null;
			
			ArrayList<Block> blocksToDownload = torrent_.getBlockToDownload(this, number);
			/*for (int i = 0; i < blocksToDownload.size(); i++) {
				Block blockToDownload = blocksToDownload.get(i);
				Log.v(LOG_TAG, "BLOCK TO DOWNLOAD: " + blockToDownload.pieceIndex() + " - " + blockToDownload.begin());
			}*/

			if (!blocksToDownload.isEmpty()) {
				blocksDownloading_.addAll(blocksToDownload);
				return blocksToDownload;
			}
			return null;
		}
	}
	
	public void blockDownloaded(int index, int begin, final byte[] data) {
		downloaded_ += data.length;
		
		Block block = null;
		Piece piece = null;
		synchronized (blocksDownloading_) {
			for (int i = 0; i < blocksDownloading_.size(); i++) {
				block = blocksDownloading_.elementAt(i);
				if (block.pieceIndex() == index && block.begin() == begin) {
					if (block.isDownloaded()) {
						Log.v(LOG_TAG, "This is an already received block.");
						blocksDownloading_.removeElement(block);
						return;
					}
					piece = torrent_.getPiece(block.pieceIndex());
					break;
				}
			}
		}

		if (piece != null) {
			int appendResult = piece.appendBlock(data, block, this);
			
			blocksDownloading_.removeElement(block);
			
			block.setDownloaded();	// GOOD SET
			torrent_.blockDownloaded(block);
			
			if (appendResult != Torrent.ERROR_NONE) {
				Log.v(LOG_TAG, "Writing file failed!!!!");
				return;
			}
		} else {
			Log.v(LOG_TAG, "Warning: Unexpected block.");
		}
		connection_.issueDownload();
	}
	
	public void calculateSpeed(long time) {
		if (downloadSpeed_ == null) return;
		downloadSpeed_.addBytes(downloaded_ - latestDownloaded_, time);
		latestDownloaded_ = downloaded_;
	}
	
	/** Sets the bitfield of the peer. */
	public void peerHasPieces(byte[] bitfield) {
		Bitfield tempBitfield = new Bitfield(bitfield);

		for (int i = 0; i < torrent_.pieceCount(); i++) {
			if (tempBitfield.isBitSet(i) && (!bitfield_.isBitSet(i))) {
				torrent_.incNumberOfPeersHavingPiece(i);
			}
		}
		bitfield_.set(bitfield);
		
		torrent_.calculateRarestPieces();
	}
	
	/** Sets the given bit of the bitfield of the peer. */
	public void peerHasPiece(int index) {
		bitfield_.setBit(index);
		torrent_.incNumberOfPeersHavingPiece(index);
		
		torrent_.calculateRarestPieces();
	}
	
	/** Returns whether the peer has the given piece or not. */
	public boolean hasPiece(int index) {
		return bitfield_.isBitSet(index);
	}
	
	/** Returns whether the peer has requested the block or not. */
	public boolean hasBlock(Block block) {
		return blocksDownloading_.contains(block);
	}
	
	/** Cancels the request of the given block. */
	public void cancelBlock(Block block) {
		synchronized (blocksDownloading_) {
			if (blocksDownloading_.contains(block)) {
				if (connection_ != null) connection_.sendCancelMessage(block);
				blocksDownloading_.removeElement(block);
			}
		}
	}
	
	/** Notifies the torrent that the downloading blocks won't be received. */
	public void cancelBlocks() {
		synchronized (blocksDownloading_) {
			if (blocksDownloading_ != null && !blocksDownloading_.isEmpty()) {
				for (int i = 0; i < blocksDownloading_.size(); i++) {
					torrent_.cancelBlock(blocksDownloading_.elementAt(i));
				}
				blocksDownloading_.removeAllElements();
			}
		}
	}
	
	/** Notify the peer that the client have the given piece. */
	public void notifyThatClientHavePiece(int pieceIndex) {
		if (connection_ != null && connection_.isConnected()) connection_.sendHaveMessage(pieceIndex);
	}
	
	/** Gives the torrent manager the infoHash of the torrent to be attached to the peer. */
	public boolean attachTorrent(String infoHash) {
		boolean result = torrentManager_.attachPeerToTorrent(infoHash, this);
		torrentManager_ = null;
		return result;
	}
	
	/** Sets the torrent which this peer is sharing. */
	public void setTorrent(Torrent torrent) {
		bitfield_ = new Bitfield(torrent.pieceCount(), false);
		if (connection_ != null) connection_.setTorrent(torrent);
		torrent_ = torrent;
	}
	
	/** Sets the Peer ID. */
	public void setPeerId(String peerId) {
		peerId_ = peerId;
	}
	
	/** True if this is an incoming connection. */
	public boolean isIncomingConnection() {
		return isIncomingConnection_;
	}
	
	/** Sets our chocking state. */
	public void setChoking(boolean choking) {
		connection_.setChoking(choking);
	}
	
	/** Returns whether the peer is interested in us or not. */
	public boolean isPeerInterested() {
		return connection_.isInterested();
	}
	
	/** Resets the error counter. */
	public void resetErrorCounter() {
		// TODO
	}
	
	/** Returns the number of requests have been sent to the peer. */
	public int getRequestsCount() {
		return blocksDownloading_.size();
	}
	
	/** Returns the download speed. */
	public int getDownloadSpeed() {
		if (downloadSpeed_ == null) return 0;
		//return downloadSpeed_.getSpeed();
		return (int) (downloadSpeed_.getBytes() / Speed.TIME_INTERVAL);
	}
	
	/** Returns the block speed. */
	public double getBlockSpeed() {
		return (double) getDownloadSpeed() / (double) Piece.DEFALT_BLOCK_LENGTH;
	}
	
	/** Returns the count of the latest downloaded blocks. */
	public int getLatestBlocksCount() {
		if (downloadSpeed_ == null) return 0;
		return (int) (downloadSpeed_.getBytes() / Piece.DEFALT_BLOCK_LENGTH);
	}
	
	/** Returns the count of downloaded bytes. */
	public long getDownloaded() {
		return downloaded_;
	}
	
	/** Returns the peers ID. Only used inside this program... */
	public int getId() {
		return id_;
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
	
	/** Returns the percent of the peer's download progress. */
	public int getPercent() {
		return bitfield_.countOfSet() * 100 / bitfield_.getLengthInBits();
	}
	
	/** Returns whether we can reconnect to the previously disconnected peer or not. */
	public boolean canConnect() {
		if (failedPieceCount_ >= 5) return false;
		if ((SystemClock.elapsedRealtime() - disconnectionTime_) > TIMEOUT_RECONNECTION) return true;
		return false;
	}
	
	public void pieceHashCorrect(boolean isCorrect) {
		if (isCorrect) {
			failedPieceCount_ -= 2;
		} else {
			failedPieceCount_++;
			if (failedPieceCount_ >= 5) connection_.close("POOR CONNECTION!!!");
		}
	}
	
	public void incTcpTimeoutCount() {
		tcpTimeoutCount_++;
	}
	
	public void resetTcpTimeoutCount() {
		tcpTimeoutCount_ = 0;
		disconnectionTime_ = 0;
	}
	
	public int getTcpTimeoutCount() {
		return tcpTimeoutCount_;
	}
}
