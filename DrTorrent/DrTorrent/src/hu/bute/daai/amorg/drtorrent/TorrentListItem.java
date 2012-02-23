package hu.bute.daai.amorg.drtorrent;

import hu.bute.daai.amorg.drtorrent.torrentengine.Torrent;

import java.io.Serializable;

/** Item of the torrent list adapter */
public class TorrentListItem implements Serializable, Comparable<TorrentListItem> {
	private static final long serialVersionUID = 1L;

	private String infoHash_ = "";
	private String name_ = "";
	private int percent_ = 0;
	private int status_ = R.string.stopped;
	private String downloadSpeed_ = "0 kB/s";
	private String uploadSpeed_ = "0 kB/s";
	private String downloaded_ = "0 kB";
	private String uploaded_ = "0 kB";

	public TorrentListItem(String name) {
		this.name_ = name;
	}

	public TorrentListItem(TorrentListItem item) {
		this.name_ = item.name_;
		this.percent_ = item.percent_;
		this.status_ = item.status_;
		this.downloadSpeed_ = item.downloadSpeed_;
		this.uploadSpeed_ = item.uploadSpeed_;
		this.downloaded_ = item.downloaded_;
		this.uploaded_ = item.uploaded_;
	}
	
	public TorrentListItem(Torrent torrent) {
		this.infoHash_ = torrent.getInfoHash();
		this.name_ = torrent.getName();
	}

	public void set(String name, int percent, int status, String downloadSpeed, String uploadSpeed, String downloaded, String uploaded) {
		this.name_ = name;
		this.percent_ = percent;
		this.status_ = status;
		this.downloadSpeed_ = downloadSpeed;
		this.uploadSpeed_ = uploadSpeed;
		this.downloaded_ = downloaded;
		this.uploaded_ = uploaded;
	}

	public void set(TorrentListItem item) {
		this.name_ = item.name_;
		this.percent_ = item.percent_;
		this.status_ = item.status_;
		this.downloadSpeed_ = item.downloadSpeed_;
		this.uploadSpeed_ = item.uploadSpeed_;
		this.downloaded_ = item.downloaded_;
		this.uploaded_ = item.uploaded_;
	}

	public void set(Torrent torrent) {
		this.infoHash_ = torrent.getInfoHash();
		this.name_ = torrent.getName();
	}

	public String getInfoHash() {
		return infoHash_;
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

	public int compareTo(TorrentListItem another) {
		if (this.name_ != null)
			return this.infoHash_.toLowerCase().compareTo(another.getInfoHash().toLowerCase());
		else
			throw new IllegalArgumentException();
	}
}
