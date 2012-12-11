package hu.bute.daai.amorg.drtorrent.adapter.item;

import hu.bute.daai.amorg.drtorrent.Tools;
import hu.bute.daai.amorg.drtorrent.torrentengine.Peer;

import java.io.Serializable;

public class PeerListItem implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private int id_;
	private String address_;
	//private String requests_;
	private String downloadSpeed_;
	private String downloaded_;
	private String uploadSpeed_;
	private String uploaded_;
	private String percent_;
	
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
		uploadSpeed_ = item.getUploadSpeed();
		uploaded_ = item.getUploaded();
		percent_ = item.percent_;
	}
	
	public void set(Peer peer) {
		id_ = peer.getId();
		address_ = peer.getAddressPort();
		//requests_ = peer.getRequestsCount() + "";
		downloadSpeed_ = Tools.bytesToString(peer.getDownloadSpeed()) + "/s";
		downloaded_ = Tools.bytesToString(peer.getDownloaded());
		uploadSpeed_ = Tools.bytesToString(peer.getUploadSpeed()) + "/s";
		uploaded_ = Tools.bytesToString(peer.getUploaded());
		percent_ = peer.getPercent() + " %";
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
	
	public String getDownloadSpeed() {
		return downloadSpeed_;
	}
	
	public String getDownloaded() {
		return downloaded_;
	}
	
	public String getUploadSpeed() {
		return uploadSpeed_;
	}
	
	public String getUploaded() {
		return uploaded_;
	}
	
	public String getPercent() {
		return percent_;
	}
	
	@Override
	public boolean equals(Object other) {
		return this.id_ == ((PeerListItem) other).id_;
	}
}
