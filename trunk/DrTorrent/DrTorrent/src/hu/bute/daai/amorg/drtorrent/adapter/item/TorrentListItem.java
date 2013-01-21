package hu.bute.daai.amorg.drtorrent.adapter.item;

import hu.bute.daai.amorg.drtorrent.Quantity;
import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.Time;
import hu.bute.daai.amorg.drtorrent.Tools;
import hu.bute.daai.amorg.drtorrent.torrentengine.Torrent;

import java.io.Serializable;

import android.content.Context;

/** Item of the torrent list adapter */
public class TorrentListItem implements Serializable, Comparable<TorrentListItem> {
	private static final long serialVersionUID = 1L;

	private int id_;
	private String infoHash_ = "";
	private String downloadFolder_ = "";
	private String name_ = "";
	private double percent_ = 0;
	private int status_ = R.string.status_stopped;
	private Quantity size_;
	private Quantity ready_;
	private Quantity downloaded_;
	private Quantity downloadSpeed_;
	private Quantity uploaded_;
	private Quantity uploadSpeed_;
	private String peers_ = "0/0";
	private Time elapsedTime_;
	private Time remainingTime_;

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
		this.downloadFolder_ = item.downloadFolder_;
		this.name_ = item.name_;
		this.percent_ = item.percent_;
		this.status_ = item.status_;
		this.size_ = item.size_;
		this.ready_ = item.ready_;
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
		this.downloadFolder_ = torrent.getDownloadFolder();
		this.name_ = torrent.getName();
		this.percent_ = torrent.getProgressPercent();
		if (((int) this.percent_) == 100 && torrent.getStatus() == R.string.status_stopped) {
			status_ = R.string.status_finished;
		} else {
			this.status_ = torrent.getStatus();
		}
		this.peers_ = torrent.getSeeds() + "/" + torrent.getLeechers();
		
		this.size_ = new Quantity(torrent.getActiveSize(), Quantity.SIZE);
		this.ready_ = new Quantity(torrent.getActiveDownloadedSize(), Quantity.SIZE);
		
		this.downloaded_ = new Quantity(torrent.getBytesDownloaded(), Quantity.SIZE);
		this.downloadSpeed_ = new Quantity(torrent.getDownloadSpeed(), Quantity.SPEED);
		
		this.uploaded_ = new Quantity(torrent.getBytesUploaded(), Quantity.SIZE);
		this.uploadSpeed_ = new Quantity(torrent.getUploadSpeed(), Quantity.SPEED);
		
		this.remainingTime_ = new Time(torrent.getRemainingTime(), Tools.MSEC, 2);
		this.elapsedTime_ = new Time(torrent.getElapsedTime(), Tools.MSEC, 2);
	}

	public int getId() {
		return id_;
	}
	
	public String getInfoHash() {
		return infoHash_;
	}

	public String getDownloadFolder() {
		return downloadFolder_;
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
	
	public String getSize(Context context) {
		return size_.toString(context);
	}

	public String getReady(Context context) {
		return ready_.toString(context);
	}
	
	public String getReadySize(Context context) {
		return ready_.toString(context, size_.getUnit());
	}
	
	public String getDownloaded(Context context) {
		return downloaded_.toString(context);
	}

	public String getDownloadSpeed(Context context) {
		return downloadSpeed_.toString(context);
	}

	public String getUploaded(Context context) {
		return uploaded_.toString(context);
	}

	public String getUploadSpeed(Context context) {
		return uploadSpeed_.toString(context);
	}

	public String getPeers() {
		return peers_;
	}

	public String getElapsedTime(Context context) {
		return elapsedTime_.toString(context);
	}
	
	public String getRemainingTime(Context context) {
		return remainingTime_.toString(context);
	}
	
	public void setDownloadFolder(String downloadFolder) {
		downloadFolder_ = downloadFolder;
	}
	
	@Override
	public boolean equals(Object other) {
		return this.id_ == ((TorrentListItem) other).id_;
	}

	@Override
	public int compareTo(TorrentListItem another) {
		int thisOrder = TorrentListItem.getOrder(this.status_);
		int anotherOrder = TorrentListItem.getOrder(another.status_);
		if (thisOrder < anotherOrder) {
			return -1;
		}
		if (thisOrder > anotherOrder) {
			return 1;
		}
		return this.name_.compareToIgnoreCase(another.name_);
	}
	
	private static int getOrder(int status) {
		switch (status) {
			case R.string.status_finished:
			case R.string.status_seeding:
				return 1;
			
			default:
				return 0;
		}		
	}
}
