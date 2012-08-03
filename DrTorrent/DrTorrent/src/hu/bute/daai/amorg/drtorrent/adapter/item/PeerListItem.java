package hu.bute.daai.amorg.drtorrent.adapter.item;

import hu.bute.daai.amorg.drtorrent.DrTorrentTools;
import hu.bute.daai.amorg.drtorrent.torrentengine.Peer;

import java.io.Serializable;

public class PeerListItem implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String address_;
	private String requests_;
	private String downloadSpeed_;
	private String downloaded_;
	private String percent_;
	
	public PeerListItem(String address, String requests, String downloadSpeed, String downloaded, String percent) {
		set(address, requests, downloadSpeed, downloaded, percent);
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
		percent_ = item.percent_;
	}
	
	public void set(Peer peer) {
		address_ = peer.getAddressPort();
		requests_ = peer.getRequestsCount() + "";
		downloadSpeed_ = DrTorrentTools.bytesToString(peer.getDownloadSpeed()) + "/s";
		downloaded_ = DrTorrentTools.bytesToString(peer.getDownloaded());
		percent_ = peer.getPercent() + " %";
	}
	
	public void set(String address, String requests, String downloadSpeed, String downloaded, String percent) {
		address_ = address;
		requests_ = requests;
		downloadSpeed_ = downloadSpeed;
		downloaded_ = downloaded;
		percent_ = percent;
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
	
	public String getPercent() {
		return percent_;
	}
	
	public void setPercent(String percent) {
		percent_ = percent;
	}

	@Override
	public boolean equals(Object other) {
		return address_.equals(((PeerListItem)other).address_);
	}
}