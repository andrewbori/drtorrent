package hu.bute.daai.amorg.drtorrent;

import hu.bute.daai.amorg.drtorrent.torrentengine.Torrent;

import java.io.Serializable;

/** Item of the torrent list adapter */
public class TorrentListItem implements Serializable, Comparable<TorrentListItem> {
	private static final long serialVersionUID = 1L;

	private String infoHash_ = "";
	private String name_ = "";
	private int percent_ = 0;
	private int status_ = R.string.status_stopped;
	private String downloadSpeed_ = "0 kB/s";
	private String uploadSpeed_ = "0 kB/s";
	private String downloaded_ = "0 kB";
	private String uploaded_ = "0 kB";
	private String peers_ = "0/0";
	
	private String size_ = "0";
	private String elapsedTime_ = "0";
	private String remainingTime_ = "0";

	public TorrentListItem(String name) {
		this.name_ = name;
	}

	public TorrentListItem(TorrentListItem item) {
		set(item);
	}
	
	public TorrentListItem(Torrent torrent) {
		set(torrent);
	}

	public void set(TorrentListItem item) {
		this.infoHash_ = item.infoHash_;
		this.name_ = item.name_;
		this.percent_ = item.percent_;
		this.status_ = item.status_;
		this.downloadSpeed_ = item.downloadSpeed_;
		this.uploadSpeed_ = item.uploadSpeed_;
		this.downloaded_ = item.downloaded_;
		this.uploaded_ = item.uploaded_;
		this.peers_ = item.peers_;
		
		this.size_ = item.size_;
		this.remainingTime_ = item.remainingTime_;
		this.elapsedTime_ = item.elapsedTime_;
	}

	public void set(Torrent torrent) {
		this.infoHash_ = torrent.getInfoHash();
		this.name_ = torrent.getName();
		this.percent_ = (int) torrent.getDownloadPercent();
		this.status_ = torrent.getStatus();
		this.peers_ = torrent.getSeeds() + "/" + torrent.getLeechers();
		
		this.downloaded_ = DrTorrentTools.bytesToString(torrent.getBytesDownloaded());
		this.downloadSpeed_ = DrTorrentTools.bytesToString(torrent.getDownloadSpeed()) + "/s";
		
		this.uploaded_ = DrTorrentTools.bytesToString(torrent.getBytesUploaded());
		
		this.size_ = DrTorrentTools.bytesToString(torrent.getSize());
		this.remainingTime_ = DrTorrentTools.intToTime(torrent.getRemainingTime(), DrTorrentTools.MSEC, 2);
		this.elapsedTime_ = DrTorrentTools.intToTime(torrent.getElapsedTime(), DrTorrentTools.MSEC, 2);
	}

	public String getInfoHash() {
		return infoHash_;
	}
	
	public void setInfoHash(String infoHash) {
		this.infoHash_ = infoHash;
	}

	public String getName() {
		return name_;
	}

	public void setName(String name) {
		this.name_ = name;
	}

	public int getPercent() {
		return percent_;
	}

	public void setPercent(int percent) {
		this.percent_ = percent;
	}

	public int getStatus() {
		return status_;
	}

	public void setStatus(int status) {
		this.status_ = status;
	}

	public String getDownloadSpeed() {
		return downloadSpeed_;
	}

	public void setDownloadSpeed(String downloadSpeed) {
		this.downloadSpeed_ = downloadSpeed;
	}

	public String getUploadSpeed() {
		return uploadSpeed_;
	}

	public void setUploadSpeed(String uploadSpeed) {
		this.uploadSpeed_ = uploadSpeed;
	}

	public String getDownloaded() {
		return downloaded_;
	}

	public void setDownloaded(String downloaded) {
		this.downloaded_ = downloaded;
	}

	public String getUploaded() {
		return uploaded_;
	}

	public void setUploaded(String uploaded) {
		this.uploaded_ = uploaded;
	}
	
	public String getPeers() {
		return peers_;
	}

	public void setPeers(String peers) {
		this.peers_ = peers;
	}
	
	public String getSize() {
		return size_;
	}

	public String getElapsedTime() {
		return elapsedTime_;
	}
	
	public String getRemainingTime() {
		return remainingTime_;
	}
	
	public int compareTo(TorrentListItem another) {
		if (this.name_ != null)
			return this.infoHash_.toLowerCase().compareTo(another.getInfoHash().toLowerCase());
		else
			throw new IllegalArgumentException();
	}
}
