package hu.bute.daai.amorg.drtorrent;

import hu.bute.daai.amorg.drtorrent.torrentengine.Peer;

import java.io.Serializable;

public class PeerListItem implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String address_;
	private String requests_;
	private String downloadSpeed_;
	private String downloaded_;
	
	public PeerListItem(String address, String requests, String downloadSpeed, String downloaded) {
		set(address, requests, downloadSpeed, downloaded);
	}
	
	public PeerListItem(Peer peer) {
		set(peer);
	}
	
	public PeerListItem(PeerListItem item) {
		set(item);
	}
	
	public void set(PeerListItem item) {
		address_ = item.address_;
		requests_ = item.requests_;
		downloadSpeed_ = item.downloadSpeed_;
		downloaded_ = item.downloaded_;
	}
	
	public void set(Peer peer) {
		address_ = peer.getAddressPort();
		requests_ = peer.getRequestsCount() + "";
		downloadSpeed_ = DrTorrentTools.bytesToString(peer.getDownloadSpeed()) + "/s";
		downloaded_ = DrTorrentTools.bytesToString(peer.getDownloaded());
	}
	
	public void set(String address, String requests, String downloadSpeed, String downloaded) {
		address_ = address;
		requests_ = requests;
		downloadSpeed_ = downloadSpeed;
		downloaded_ = downloaded;
	}
	
	public String getAddress() {
		return address_;
	}
	
	public void setAddress(String address) {
		address_ = address;
	}
	
	public String getRequests() {
		return requests_;
	}
	
	public void setRequests(String requests) {
		requests_ = requests;
	}
	
	public String getDownloadSpeed() {
		return downloadSpeed_;
	}
	
	public void setDownloadSpeed(String downloadSpeed) {
		downloadSpeed_ = downloadSpeed;
	}
	
	public String getDownloaded() {
		return downloaded_;
	}
	
	public void setDownloaded(String downloaded) {
		downloaded_ = downloaded;
	}

	@Override
	public boolean equals(Object other) {
		return address_.equals(((PeerListItem)other).address_);
	}
}
