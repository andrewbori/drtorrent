package hu.bute.daai.amorg.drtorrent.core.peer;

import hu.bute.daai.amorg.drtorrent.core.Bitfield;
import hu.bute.daai.amorg.drtorrent.core.Block;
import hu.bute.daai.amorg.drtorrent.core.Piece;

import java.util.ArrayList;

/** Represents the observer for peer connection. */
public interface PeerConnectionObserver {
	
	/** Called when the disconnection is complete. */
	public void onDisconnected();
	
	public void incTcpTimeoutCount();
	
	public void resetTcpTimeoutCount();
	
	/** Resets the error counter. */
	public void resetErrorCounter();
	
	/** Returns the address of the peer. */
	public String getAddress();

	/** Returns the port of the peer. */
	public int getPort();
	
	/** Returns the address and the port of the peer. Format: "address:port". */
	public String getAddressPort();
	
	/** Returns the ID of the peer. */
	public String getPeerId();
	
	/** Sets the Peer ID. */
	public void setPeerId(String peerId);
	
	/** Sets the client's name. */
	public void setClientName(String clientName);
	
	
	/** Called when a new block was received. */
	public void onBlockDownloaded(int index, int begin, final byte[] data);
	
	/** Sets the given bit of the bitfield of the peer. */
	public void peerHasPiece(int index);
	
	/** Sets the bitfield of the peer. */
	public void peerHasPieces(byte[] bitfield);
	
	/** Returns whether the peer has blocks we don't have. */
	public boolean hasInterestingBlock();
	
	/** Sets the bitfield. */
	public void setBitfield(Bitfield bitfield);
	
	/** Returns the bitfield. */
	public Bitfield getBitfield();
	
	/** Notifies the torrent that the downloading blocks won't be received. */
	public void cancelBlocks();
	
	/** Gives the torrent manager the infoHash of the torrent to be attached to the peer. */
	public boolean attachTorrent(String infoHash);
	
	/** Issues the download requests. */
	public ArrayList<Block> issueDownload();
	
	/** Returns the number of requests have been sent to the peer. */
	public boolean hasRequest();
	
	/** Calculates the download and upload speed. */
	public void calculateSpeed(long time);
	
	
	public boolean hasTorrent();
	
	public boolean isTorrentValid();
	
	public int getTorrentStatus();
	
	public boolean isTorrentConnected();
	
	public void cancelMetadataBlock(final int index);
	
	public void addBlockToMetadata(final int piece, final byte[] left);
	
	public byte[] readMetadataBlock(int piece);
	
	public void setMetadataSize(int size);
	
	public int getMetadataSize();
	
	public int getMetadataBlockToDownload();
	
	public byte[] getTorrentInfoHashByteArray();
	
	public Bitfield getTorrentBitfield();
	
	public int getTorrentPieceCount();
	
	public boolean isTorrentDownloadingData();
	
	public void addBlockToUpload(Block block);
	
	public Piece getPiece(int index);
	
	public void peerInterested(final boolean isInterested);
	
	public void updateBytesUploaded(int bytes);
	
	public void onBlockUploaded(int size);
	
	public void analyticsNewHandshake();
	
	public void analyticsNewFailedConnection();
	
	public void analyticsNewTcpConnection();
}
