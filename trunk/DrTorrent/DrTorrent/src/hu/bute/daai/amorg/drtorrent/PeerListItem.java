package hu.bute.daai.amorg.drtorrent;

import hu.bute.daai.amorg.drtorrent.torrentengine.Peer;

import java.io.Serializable;

public class PeerListItem implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String address_;
	private String requests_;
	
	public PeerListItem(String address, String requests) {
		set(address, requests);
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
	}
	
	public void set(Peer peer) {
		address_ = peer.getAddressPort();
		requests_ = peer.getRequestsCount() + " + " + peer.getRequestsToSendCount();
	}
	
	public void set(String address, String requests) {
		address_ = address;
		requests_ = requests;
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

	@Override
	public boolean equals(Object other) {
		return address_.equals(((PeerListItem)other).address_);
	}
}
