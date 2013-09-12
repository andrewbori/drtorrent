package hu.bute.daai.amorg.drtorrent.ui.adapter.item;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.core.tracker.Tracker;
import hu.bute.daai.amorg.drtorrent.core.tracker.TrackerInfo;
import hu.bute.daai.amorg.drtorrent.util.Time;
import hu.bute.daai.amorg.drtorrent.util.Tools;

import java.io.Serializable;

import android.content.Context;

public class TrackerListItem implements Serializable  {

	private static final long serialVersionUID = 1L;

	private int id_;
	private String address_;
	private int status_;
	private Time time_;
	private String peers_;
	
	public TrackerListItem(TrackerInfo tracker) {
		set(tracker);
	}
	
	public TrackerListItem(TrackerListItem item) {
		set(item);
	}
	
	public void set(TrackerInfo tracker) {
		id_ = tracker.getId();
		address_ = tracker.getUrl();
		switch (tracker.getStatus()) {
			case Tracker.STATUS_UPDATING:
				status_ = R.string.updating;
				time_ = new Time();
				peers_ = "";
				break;
			case Tracker.STATUS_WORKING:
				status_ = R.string.working; 
				time_ = new Time(tracker.getRemainingTime(), Tools.SEC, 2);
				peers_ = "(" + tracker.getComplete() + "/" + tracker.getIncomplete() + ")";
				break;
			case Tracker.STATUS_FAILED:
				status_ = R.string.failed;
				time_ = new Time(tracker.getRemainingTime(), Tools.SEC, 2);
				peers_ = "";
				break;
			default:
				status_ = R.string.empty_field;
				time_ = new Time();
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

	public String getTime(Context context) {
		return time_.toString(context);
	}

	public String getPeers() {
		return peers_;
	}
	
	@Override
	public boolean equals(Object other) {
		return this.id_ == ((TrackerListItem) other).id_;
	}
}