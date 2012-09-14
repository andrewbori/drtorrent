package hu.bute.daai.amorg.drtorrent.adapter.item;

import hu.bute.daai.amorg.drtorrent.Tools;
import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.torrentengine.Torrent;

import java.io.Serializable;

/** Item of the torrent list adapter */
public class TorrentListItem implements Serializable {
	private static final long serialVersionUID = 1L;

	private int id_;
	private String infoHash_ = "";
	private String name_ = "";
	private double percent_ = 0;
	private int status_ = R.string.status_stopped;
	private String size_ = "0 B";
	private String downloaded_ = "0 B";
	private String downloadSpeed_ = "0 B/s";
	private String uploaded_ = "0 B";
	private String uploadSpeed_ = "0 B/s";
	private String peers_ = "0/0";
	private String elapsedTime_ = "0 s";
	private String remainingTime_ = "0 s";

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
		this.id_ = item.id_;
		this.infoHash_ = item.infoHash_;
		this.name_ = item.name_;
		this.percent_ = item.percent_;
		this.status_ = item.status_;
		this.size_ = item.size_;
		this.downloaded_ = item.downloaded_;
		this.downloadSpeed_ = item.downloadSpeed_;
		this.uploaded_ = item.uploaded_;
		this.uploadSpeed_ = item.uploadSpeed_;
		this.peers_ = item.peers_;
		this.remainingTime_ = item.remainingTime_;
		this.elapsedTime_ = item.elapsedTime_;
	}

	public void set(Torrent torrent) {
		this.id_ = torrent.getId();
		this.infoHash_ = torrent.getInfoHashString();
		this.name_ = torrent.getName();
		this.percent_ = torrent.getProgressPercent();
		this.status_ = torrent.getStatus();
		this.peers_ = torrent.getSeeds() + "/" + torrent.getLeechers();
		
		this.size_ = Tools.bytesToString(torrent.getActiveSize());
		
		this.downloaded_ = Tools.bytesToString(torrent.getBytesDownloaded());
		this.downloadSpeed_ = Tools.bytesToString(torrent.getDownloadSpeed()).concat("/s");
		
		this.uploaded_ = Tools.bytesToString(torrent.getBytesUploaded());
		this.uploadSpeed_ = Tools.bytesToString(torrent.getUploadSpeed()).concat("/s");
		
		this.remainingTime_ = Tools.timeToString(torrent.getRemainingTime(), Tools.MSEC, 2);
		this.elapsedTime_ = Tools.timeToString(torrent.getElapsedTime(), Tools.MSEC, 2);
	}

	public int getId() {
		return id_;
	}
	
	public String getInfoHash() {
		return infoHash_;
	}

	public String getName() {
		return name_;
	}

	public double getPercent() {
		return percent_;
	}

	public int getStatus() {
		return status_;
	}
	
	public String getSize() {
		return size_;
	}

	public String getDownloaded() {
		return downloaded_;
	}

	public String getDownloadSpeed() {
		return downloadSpeed_;
	}

	public String getUploaded() {
		return uploaded_;
	}

	public String getUploadSpeed() {
		return uploadSpeed_;
	}

	public String getPeers() {
		return peers_;
	}

	public String getElapsedTime() {
		return elapsedTime_;
	}
	
	public String getRemainingTime() {
		return remainingTime_;
	}
	
	@Override
	public boolean equals(Object other) {
		return this.id_ == ((TorrentListItem) other).id_;
	}
}
