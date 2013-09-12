package hu.bute.daai.amorg.drtorrent.core.torrent;

import hu.bute.daai.amorg.drtorrent.core.Bitfield;
import hu.bute.daai.amorg.drtorrent.core.File;
import hu.bute.daai.amorg.drtorrent.core.peer.PeerInfo;
import hu.bute.daai.amorg.drtorrent.core.tracker.TrackerInfo;

import java.util.ArrayList;
import java.util.Vector;

/** Represents a readable interface of the state of the torrent. */
public interface TorrentInfo {

	/** Returns the id of the torrent. (Only used inside of this program.) */
	public int getId();
	
	/** Returns the encoded info hash. */
	public String getInfoHashString();
	
	/** Returns the magnet link of the torrent. */
	public String getMagnetLink();
	
	/** Returns the full download path of the torrent. */
	public String getPath();
	
	/** Returns the download folder of the torrent. */
	public String getDownloadFolder();
	
	/** Returns the name of the torrent. */
	public String getName();
	
	/** Returns whether the torrent is valid or not. */
	public boolean isValid();
	
	/** Returns the status of the torrent. */
	public int getStatus();
	
	/** Returns whether the torrent is about to be created or not. */
	public boolean isCreating();
	
	/** Returns whether the torrent is working or not (hash checking/downloading/seeding/metadata downloading). */
	public boolean isWorking();
	
	/** Returns whether the torrent is connected or not. (downloading/seeding/metadata downloading) */
	public boolean isConnected();
	
	/** Returns whether the torrent is downloading data or not. (downloading/metadata downloading) */
	public boolean isDownloadingData();
	
	/** Returns the bitfield of the torrent. */
	public Bitfield getBitfield();

	/** Returns the downloading bitfield of the torrent. */
	public Bitfield getDownloadingBitfield();
	
	/** Returns the active bitfield of the torrent. */
	public Bitfield getActiveBitfield();
	
	/** Returns the array of the files of the torrent. */
	public Vector<File> getFiles();

	/** Returns the array of the trackers of the torrent. */
	public ArrayList<TrackerInfo> getTrackers();
	
	/** Returns the array of the connected peers. */
	public PeerInfo[] getConnectedPeers();
	
	/** Returns the percent of the downloaded data. */
	public double getProgressPercent();
	
	/** Returns the length of pieces. */
	public int getPieceLength();
	
	/** Returns the number of pieces. */
	public int pieceCount();
	
	/** Returns the active size of the torrent. */
	public long getActiveSize();
	
	/** Returns the full size of the torrent. */
	public long getFullSize();
	
	/** Returns the size of completed files. */
	public long getCompletedFilesSize();
	
	/** Returns the active downloaded size of the torrent. */
	public long getActiveDownloadedSize();

	/** Returns the number of bytes have to be downloaded. */
	public long getBytesLeft();

	/** Returns the number of downloaded bytes. */
	public long getBytesDownloaded();

	/** Returns the number of uploaded bytes. */
	public long getBytesUploaded();

	/** Returns the download speed. */
	public int getDownloadSpeed();

	/** Returns the upload speed. */
	public int getUploadSpeed();
	
	/** Returns the remaining time (ms) until the downloading ends. */
	public int getRemainingTime();
	
	/** Returns the date (in ms since 1970) when the torrent was added to the list. */
	public long getAddedOn();
	
	/** Returns the date (in ms since 1970) when the torrent was completed. */
	public long getCompletedOn();
	
	/** Returns the date (in ms since 1970) when the torrent was removed from the list. */
	public long getRemovedOn();
	
	/** Returns the elapsed time (ms) since the torrent has been started. */
	public long getElapsedTime();
	
	/** Returns the downloading time (ms). */
	public long getDownloadingTime();
	
	/** Returns the seeding time (ms). */
	public long getSeedingTime();
	
	/** Returns the number of seeds. */
	public int getSeeds();

	/** Returns the number of leechers. */
	public int getLeechers();
}
