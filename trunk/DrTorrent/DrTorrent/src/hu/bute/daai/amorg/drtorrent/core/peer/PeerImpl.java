package hu.bute.daai.amorg.drtorrent.core.peer;

import hu.bute.daai.amorg.drtorrent.core.Bitfield;
import hu.bute.daai.amorg.drtorrent.core.Block;
import hu.bute.daai.amorg.drtorrent.core.Piece;
import hu.bute.daai.amorg.drtorrent.core.Speed;
import hu.bute.daai.amorg.drtorrent.core.exception.DrTorrentException;
import hu.bute.daai.amorg.drtorrent.core.torrent.Torrent;
import hu.bute.daai.amorg.drtorrent.core.torrent.TorrentManager;
import hu.bute.daai.amorg.drtorrent.util.Log;
import hu.bute.daai.amorg.drtorrent.util.analytics.Analytics;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Vector;

import android.os.SystemClock;

/** Class that represents the Peer. */
public class PeerImpl implements Peer, PeerConnectionObserver {
	private final static String LOG_TAG = "Peer";
	private final static int TIMEOUT_RECONNECTION      = 1000 * 2 * 60;
	private final static int TIMEOUT_RECONNECTION_PLUS = 1000 * 1 * 60;
	
	private static int ID = 0;
	
	final private int id_;
	final private String address_;
	final private int port_;
	private String peerId_;
	private String clientName_ = null;
	private Bitfield bitfield_ = null;
	
	private TorrentManager torrentManager_ = null;
	private Torrent torrent_ = null;
	
	private int failedPieceCount_ = 0;
	
	private PeerConnection connection_ = null;
	
	private final Vector<Block> blocksDownloading_;
	
	private long latestDownloaded_ = 0;
	private long downloaded_ = 0;
	private Speed downloadSpeed_ = null;
	private long latestUploaded_ = 0;
	private long uploaded_ = 0;
	private Speed uploadSpeed_ = null;
	
	private int tcpTimeoutCount_ = 0;
	private long disconnectionTime_ = 0;
	
	/** Constructor for peer with known address. */
	public PeerImpl(String address, int port, String peerId, int piecesCount) {
		address_ = address;
		port_ = port;
		peerId_ = peerId;
		bitfield_ = new Bitfield(piecesCount, false);
        blocksDownloading_ = new Vector<Block>();
		
		id_ = ++ID;
	}
	
	/** Constructor for incoming connections. */
	public PeerImpl(Socket socket, TorrentManager torrentManager) {
		address_ = socket.getInetAddress().getHostAddress();
		port_ = socket.getPort();
		torrentManager_ = torrentManager;
        blocksDownloading_ = new Vector<Block>();
		
		id_ = ++ID;
	}
	
	/** Connects to the peer. */
	@Override
	public void connect(Torrent torrent) {		
		torrent_ = torrent;
		downloadSpeed_ = new Speed();
		uploadSpeed_ = new Speed();
		if (connection_ == null) {
			connection_ = new PeerConnectionImpl(this, false);
		}
		connection_.connect();
	}
	
	/** Connects to the peer. (incoming connection) */
	@Override
	public void connect(Socket socket) {
		downloadSpeed_ = new Speed();
		uploadSpeed_ = new Speed();
		if (connection_ == null) {
			connection_ = new PeerConnectionImpl(this, true);
		}
		connection_.connect(socket);
	}
	
	/** Disconnects the peer. */
	@Override
	public void disconnect() {
		if (connection_ != null) {
			connection_.close("Peer has been disconnected...", true);
		}
	}
	
	/** Called when the disconnection is complete. */
	@Override
	public void onDisconnected() {
		if (torrent_ != null) {
			torrent_.peerDisconnected(this);
			
			if (torrent_.isValid() && bitfield_ != null) {
				for (int i = 0; i < torrent_.pieceCount(); i++) {
					if (bitfield_.isBitSet(i)) {
						torrent_.decNumberOfPeersHavingPiece(i);
					}
				}
			}
		}
		cancelBlocks();
		disconnectionTime_ = SystemClock.elapsedRealtime();
		downloadSpeed_ = null;
		uploadSpeed_ = null;
		connection_ = null;
	}
	
	/** Schedules the connection. */
	@Override
	public void onTimer() {
		if (connection_ != null) {
			connection_.onTimer();
		} else {
			onDisconnected();
		}
	}
	
	/** Issues the download requests. */
	@Override
	public ArrayList<Block> issueDownload() {
		int number;
		final int size = blocksDownloading_.size();
		if (getLatestBlocksCount() == 0 && size == 0) {
			number = 3;
		} else {
			number = (int) (getBlockSpeed() * 3.0) - size;
		}
		if (number < size * 0.2) {
			return null;
		}
		
		Log.v(LOG_TAG, "Number of blocks to request: " + number);
		ArrayList<Block> blocksToDownload = torrent_.getBlocksToDownload(this, number);
		Log.v(LOG_TAG, "Number of blocks got: " + blocksToDownload.size());
		
		if (!blocksToDownload.isEmpty()) {
			blocksDownloading_.addAll(blocksToDownload);
			return blocksToDownload;
		}
		return null;
	}
	
	/** Called when a new block was received. */
	@Override
	public void onBlockDownloaded(int index, int begin, final byte[] data) {
		downloaded_ += data.length;
		
		Block block = null;
		Piece piece = null;

		if (!blocksDownloading_.isEmpty()) {
			final Block[] blocks;
			synchronized (blocksDownloading_) {
				blocks = new Block[blocksDownloading_.size()];
				blocksDownloading_.copyInto(blocks);
			}
			for (int i = 0; i < blocks.length; i++) {
				block = blocks[i];
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
			try {
				piece.appendBlock(data, block, this);
			} catch (DrTorrentException e) {
				Log.v(LOG_TAG, "Writing file failed!");
				return;
			} finally {
				// TODO: Re-think failed writing...
				blocksDownloading_.removeElement(block);
				block.setDownloaded();	// GOOD SET
				torrent_.onBlockDownloaded(block);
			}
		} else {
			Log.v(LOG_TAG, "Warning: Unexpected block.");
		}
		connection_.issueDownload();
	}
	
	@Override
	public boolean issueUpload(Block block) {
		if (connection_ != null) {
			return connection_.issueUpload(block);
		}
		return false;
	}
	
	/** Called when a new block was uploaded. */
	@Override
	public void onBlockUploaded(int size) {
		uploaded_ += size; 
	}
	
	/** Calculates the download and upload speed. */
	@Override
	public void calculateSpeed(long time) {
		if (downloadSpeed_ != null) {
			downloadSpeed_.addBytes(downloaded_ - latestDownloaded_, time);
			latestDownloaded_ = downloaded_;
		}
		if (uploadSpeed_ != null) {
			uploadSpeed_.addBytes(uploaded_ - latestUploaded_, time);
			latestUploaded_ = uploaded_;
		}
	}
	
	/** Sets the bitfield. */
	@Override
	public void setBitfield(Bitfield bitfield) {
		bitfield_ = bitfield;
	}
	
	/** Sets the bitfield of the peer. */
	@Override
	public void peerHasPieces(byte[] bitfield) {
		Bitfield tempBitfield = new Bitfield(bitfield);

		for (int i = 0; i < torrent_.pieceCount(); i++) {
			if (tempBitfield.isBitSet(i) && (!bitfield_.isBitSet(i))) {
				torrent_.incNumberOfPeersHavingPiece(i);
			}
		}
		bitfield_.set(bitfield);
	}
	
	/** Sets the given bit of the bitfield of the peer. */
	@Override
	public void peerHasPiece(int index) {
		bitfield_.setBit(index);
		torrent_.incNumberOfPeersHavingPiece(index);
	}
	
	/** Returns whether the peer has the given piece or not. */
	//
	@Override
	public boolean hasPiece(int index) {
		return bitfield_.isBitSet(index);
	}
	
	/** Returns whether the peer has requested the block or not. */
	//
	@Override
	public boolean hasBlock(Block block) {
		return blocksDownloading_.contains(block);
	}
	
	/** Cancels the request of the given block. */
	//
	@Override
	public void cancelBlock(final Block block) {
		boolean hasBlock = false;
		synchronized (blocksDownloading_) {
			if (blocksDownloading_.contains(block)) {
				blocksDownloading_.removeElement(block);
				hasBlock = true;
			}
		}
		
		if (connection_ != null && hasBlock) {
			connection_.sendCancelMessage(block);
		}
	}
	
	/** Notifies the torrent that the downloading blocks won't be received. */
	@Override
	public void cancelBlocks() {
		Block[] blocks = null;
		synchronized (blocksDownloading_) {
			if (!blocksDownloading_.isEmpty()) {
				blocks = new Block[blocksDownloading_.size()];
				blocksDownloading_.copyInto(blocks);
				blocksDownloading_.removeAllElements();
			}
		}
		
		if (blocks != null) {
			for (int i = 0; i < blocks.length; i++) {
				torrent_.cancelBlock(blocks[i]);
			}
		}
	}
	
	/** Notify the peer that the client have the given piece. */
	//
	@Override
	public void notifyThatClientHavePiece(int pieceIndex) {
		if (connection_ != null && connection_.isConnected()) {
			connection_.sendHaveMessage(pieceIndex);
		}
	}
	
	/** Gives the torrent manager the infoHash of the torrent to be attached to the peer. */
	@Override
	public boolean attachTorrent(String infoHash) {
		boolean result = torrentManager_.attachPeerToTorrent(infoHash, this);
		torrentManager_ = null;
		return result;
	}
	
	/** Sets the torrent which this peer is sharing. */
	//
	@Override
	public void setTorrent(Torrent torrent) {
		bitfield_ = new Bitfield(torrent.pieceCount(), false);
		torrent_ = torrent;
	}
	
	/** Sets the Peer ID. */
	@Override
	public void setPeerId(String peerId) {
		peerId_ = peerId;
	}
	
	/** Sets our chocking state. */
	//
	@Override
	public void setChoking(boolean choking) {
		if (connection_ != null) {
			connection_.setChoking(choking);
		}
	}
	
	/** Resets the error counter. */
	@Override
	public void resetErrorCounter() {
		// TODO
	}
	
	/** Returns the number of requests have been sent to the peer. */
	@Override
	public boolean hasRequest() {
		if (blocksDownloading_.size() > 0) {
			return true;
		}
		return false;
	}
	
	/** Returns the download speed. */
	//
	@Override
	public int getDownloadSpeed() {
		if (downloadSpeed_ == null) {
			return 0;
		}
		//return downloadSpeed_.getSpeed();
		return (int) (downloadSpeed_.getBytes() / Speed.TIME_INTERVAL);
	}
	
	/** Returns the upload speed. */
	//
	@Override
	public int getUploadSpeed() {
		if (uploadSpeed_ == null) {
			return 0;
		}

		return (int) (uploadSpeed_.getBytes() / Speed.TIME_INTERVAL);
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
	//
	@Override
	public long getDownloaded() {
		return downloaded_;
	}
	
	/** Returns the count of uploaded bytes. */
	//
	@Override
	public long getUploaded() {
		return uploaded_;
	}
	
	/** Returns the peers ID. Only used inside this program... */
	//
	@Override
	public int getId() {
		return id_;
	}
	
	/** Returns the address and the port of the peer. Format: "address:port". */
	@Override
	public String getAddressPort() {
		return address_ + ":" + port_;
	}
	
	/** Returns the address of the peer. */
	//
	@Override
	public String getAddress() {
		return address_;
	}
	
	/** Returns the port of the peer. */
	//
	@Override
	public int getPort() {
		return port_;
	}
	
	/** Returns the ID of the peer. */
	@Override
	public String getPeerId() {
		return peerId_;
	}
	
	/** Sets the client's name. */
	@Override
	public void setClientName(String clientName) {
		if (clientName != null && !clientName.equals("")) {
			clientName_ = clientName;
		}
	}
	
	/** Returns the client's name. */
	//
	@Override
	public String getClientName() {
		return clientName_;
	}
	
	/** Returns the bitfield. */
	@Override
	public Bitfield getBitfield() {
		return bitfield_;
	}
	
	/** Returns the percent of the peer's download progress. */
	//
	@Override
	public int getPercent() {
		if (torrent_.getBitfield() == null || torrent_.pieceCount() == 0) {
			return 0;
		}
		
		return bitfield_.countOfSet() * 100 / torrent_.pieceCount();
	}
	
	/** Returns whether we can reconnect to the previously disconnected peer or not. */
	//
	@Override
	public boolean canConnect() {
		if (failedPieceCount_ >= 5) {
			return false;
		}
		
		if ((SystemClock.elapsedRealtime() - disconnectionTime_) > TIMEOUT_RECONNECTION + (tcpTimeoutCount_ * TIMEOUT_RECONNECTION_PLUS)) {
			return true;
		}
		
		return false;
	}
	
	//
	@Override
	public void onPieceHashChecked(boolean isCorrect) {
		if (isCorrect) {
			failedPieceCount_ -= 2;
		} else {
			failedPieceCount_++;
			if (failedPieceCount_ >= 5) {
				connection_.close("Poor connection! Too many corrupt pieces.");
			}
		}
	}
	
	@Override
	public void incTcpTimeoutCount() {
		tcpTimeoutCount_++;
	}
	
	//
	@Override
	public void resetTcpTimeoutCount() {
		tcpTimeoutCount_ = 0;
		disconnectionTime_ = 0;
	}
	
	//
	@Override
	public int getTcpTimeoutCount() {
		return tcpTimeoutCount_;
	}

	/** Returns the state of the connection of the peer. */
	//
	@Override
	public int getState() {
		return connection_ != null ? connection_.getState() : PeerConnectionImpl.STATE_NOT_CONNECTED;
	}

	//
	@Override
	public boolean isMetadataBlockRejected(int index) {
		return connection_ != null ? connection_.isMetadataBlockRejected(index) : true;
	}

	@Override
	public void cancelMetadataBlock(final int index) {
		torrent_.cancelMetadataBlock(index);
	}

	@Override
	public void addBlockToMetadata(final int piece, final byte[] left) {
		torrent_.addBlockToMetadata(piece, left);
	}

	@Override
	public byte[] readMetadataBlock(int piece) {
		return torrent_.readMetadataBlock(piece);
	}

	@Override
	public void setMetadataSize(int size) {
		torrent_.setMetadataSize(size);
	}
	
	@Override
	public int getMetadataSize() {
		return torrent_.getMetadataSize();
	}
	
	/** Returns whether we are choked (the peer is choking us) or not. */ 
	//
	@Override
	public boolean isChoked() {
		return connection_ != null ? connection_.isPeerChoking() : true;
	}
	
	/** Returns whether the peer is choked (we are choking the peer) or not. */
	//
	@Override
	public boolean isPeerChocked() {
		return connection_ != null ? connection_.isChoking() : true;
	}

	/** Returns whether the peer has blocks we don't have. */
	@Override
	public boolean hasInterestingBlock() {
		Bitfield tempBitfield = bitfield_.getBitfieldAnd(torrent_.getActiveBitfield());
		if (tempBitfield.hasSettedCompearedTo(torrent_.getBitfield())) {
			return true;
		}
		return false;
	}
	
	//// PeerConnection methods:
	
	@Override
	public boolean hasTorrent() {
		return torrent_ != null;
	}

	@Override
	public boolean isTorrentValid() {
		return torrent_ != null ? torrent_.isValid() : false; 
	}

	@Override
	public int getTorrentStatus() {
		return torrent_ != null ? torrent_.getStatus() : Torrent.STATUS_OPENING;
	}

	@Override
	public boolean isTorrentConnected() {
		return torrent_ != null ? torrent_.isConnected() : false;
	}

	@Override
	public int getMetadataBlockToDownload() {
		return torrent_ != null ? torrent_.getMetadataBlockToDownload(this) : -1;
	}

	@Override
	public byte[] getTorrentInfoHashByteArray() {
		return torrent_.getInfoHashByteArray();
	}

	@Override
	public Bitfield getTorrentBitfield() {
		return torrent_.getBitfield(); 
	}

	@Override
	public int getTorrentPieceCount() {
		return torrent_.pieceCount();
	}

	@Override
	public boolean isTorrentDownloadingData() {
		return torrent_ != null ? torrent_.isDownloadingData() : false;
	}

	@Override
	public void addBlockToUpload(Block block) {
		torrent_.addBlockToUpload(block, this);
	}

	@Override
	public Piece getPiece(int index) {
		return torrent_.getPiece(index);
	}

	@Override
	public void peerInterested(final boolean isInterested) {
		if (isInterested) {
			torrent_.onPeerInterested(this);
		} else {
			torrent_.onPeerNotInterested(this);
		}
	}

	@Override
	public void updateBytesUploaded(int bytes) {
		torrent_.updateBytesUploaded(bytes);
	}

	@Override
	public void analyticsNewHandshake() {
		Analytics.newHandshake(torrent_);
	}

	@Override
	public void analyticsNewFailedConnection() {
		Analytics.newFailedConnection(torrent_);
	}

	@Override
	public void analyticsNewTcpConnection() {
		Analytics.newTcpConnection(torrent_);
	}
}
