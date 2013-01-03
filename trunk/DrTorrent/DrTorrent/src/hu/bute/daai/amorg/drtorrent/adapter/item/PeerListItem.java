package hu.bute.daai.amorg.drtorrent.adapter.item;

import hu.bute.daai.amorg.drtorrent.Quantity;
import hu.bute.daai.amorg.drtorrent.torrentengine.Peer;

import java.io.Serializable;

import android.content.Context;

public class PeerListItem implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private int id_;
	private String address_;
	//private String requests_;
	private Quantity downloadSpeed_;
	private Quantity downloaded_;
	private Quantity uploadSpeed_;
	private Quantity uploaded_;
	private String percent_;
	private int state_;
	private boolean isChoked_;
	private boolean isPeerChoked_;
	
	public PeerListItem(Peer peer) {
		set(peer);
	}
	
	public PeerListItem(PeerListItem item) {
		set(item);
	}
	
	public void set(PeerListItem item) {
		id_ = item.id_;
		address_ = item.address_;
		//requests_ = item.requests_;
		downloadSpeed_ = item.downloadSpeed_;
		downloaded_ = item.downloaded_;
		uploadSpeed_ = item.uploadSpeed_;
		uploaded_ = item.uploaded_;
		percent_ = item.percent_;
		state_ = item.state_; 
		isChoked_ = item.isChoked_;
		isPeerChoked_ = item.isPeerChoked_;
	}
	
	public void set(Peer peer) {
		id_ = peer.getId();
		address_ = peer.getAddress();
		if (peer.getClientName() != null) {
			address_ += " (" + peer.getClientName() + ")";
		}
		//requests_ = peer.getRequestsCount() + "";
		downloadSpeed_ = new Quantity(peer.getDownloadSpeed(), Quantity.SPEED);
		downloaded_ = new Quantity(peer.getDownloaded(), Quantity.SIZE);
		uploadSpeed_ = new Quantity(peer.getUploadSpeed(), Quantity.SPEED);
		uploaded_ = new Quantity(peer.getUploaded(), Quantity.SIZE);
		percent_ = peer.getPercent() + " %";
		state_ = peer.getState();
		isChoked_ = peer.isChoked();
		isPeerChoked_ = peer.isPeerChocked();
	}
	
	public int getId() {
		return id_;
	}
	
	public String getAddress() {
		return address_;
	}
	
	/*public String getRequests() {
		return requests_;
	}*/
	
	public String getDownloadSpeed(final Context context) {
		return downloadSpeed_.toString(context);
	}
	
	public String getDownloaded(final Context context) {
		return downloaded_.toString(context);
	}
	
	public String getUploadSpeed(final Context context) {
		return uploadSpeed_.toString(context);
	}
	
	public String getUploaded(final Context context) {
		return uploaded_.toString(context);
	}
	
	public String getDownloadStatus(final Context context) {
		return downloaded_.toString(context).concat(" (").concat(downloadSpeed_.toString(context)).concat(")");
	}
	
	public String getUploadStatus(final Context context) {
		return uploaded_.toString(context).concat(" (").concat(uploadSpeed_.toString(context)).concat(")");
	}
	
	public String getPercent() {
		return percent_;
	}
	
	public int getState() {
		return state_;
	}
	
	public boolean isChoked() {
		return isChoked_;
	}
	
	public boolean isPeerChoked() {
		return isPeerChoked_;
	}
	
	@Override
	public boolean equals(Object other) {
		return this.id_ == ((PeerListItem) other).id_;
	}
}
