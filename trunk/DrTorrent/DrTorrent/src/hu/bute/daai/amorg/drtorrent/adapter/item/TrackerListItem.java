package hu.bute.daai.amorg.drtorrent.adapter.item;

import hu.bute.daai.amorg.drtorrent.Tools;
import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.torrentengine.Tracker;

import java.io.Serializable;

public class TrackerListItem implements Serializable  {

	private static final long serialVersionUID = 1L;

	private int id_;
	private String address_;
	private int status_;
	private String time_;
	private String peers_;
	
	public TrackerListItem(Tracker tracker) {
		set(tracker);
	}
	
	public TrackerListItem(TrackerListItem item) {
		set(item);
	}
	
	public void set(Tracker tracker) {
		id_ = tracker.getId();
		address_ = tracker.getUrl();
		switch (tracker.getStatus()) {
			case Tracker.STATUS_UPDATING:
				status_ = R.string.updating;
				time_ = "";
				peers_ = "";
				break;
			case Tracker.STATUS_WORKING:
				status_ = R.string.working; 
				time_ = Tools.timeToString(tracker.getRemainingTime(), Tools.SEC, 2);
				peers_ = "(" + tracker.getComplete() + "/" + tracker.getIncomplete() + ")";
				break;
			case Tracker.STATUS_FAILED:
				status_ = R.string.failed;
				time_ = Tools.timeToString(tracker.getRemainingTime(), Tools.SEC, 2);
				peers_ = "";
				break;
			default:
				status_ = R.string.empty_field;
				time_ = "";
				peers_ = "";
				break;
		}
	}
	
	public void set(TrackerListItem item) {
		this.id_ = item.id_;
		this.address_ = item.address_;
		this.status_ = item.status_;
		this.time_ = item.time_;
		this.peers_ = item.peers_;
	}

	public int getId() {
		return id_;
	}
	
	public String getAddress() {
		return address_;
	}

	public int getStatus() {
		return status_;
	}

	public String getTime() {
		return time_;
	}

	public String getPeers() {
		return peers_;
	}
	
	@Override
	public boolean equals(Object other) {
		return this.id_ == ((TrackerListItem) other).id_;
	}
}