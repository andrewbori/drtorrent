package hu.bute.daai.amorg.drtorrent.adapter.item;

import hu.bute.daai.amorg.drtorrent.torrentengine.Tracker;

import java.io.Serializable;

public class TrackerListItem implements Serializable {

	private static final long serialVersionUID = 1L;

	private String address_;
	
	public TrackerListItem(Tracker tracker) {
		set(tracker);
	}
	
	public TrackerListItem(TrackerListItem item) {
		set(item);
	}
	
	public void set(Tracker tracker) {
		address_ = tracker.getUrl();
	}
	
	public void set(TrackerListItem item) {
		this.address_ = item.address_;
	}
	
	public String getAddress() {
		return address_;
	}
	
	public void setAddress(String address) {
		this.address_ = address;
	}
	
}