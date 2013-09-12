package hu.bute.daai.amorg.drtorrent.core.tracker;

/** Represents a readable interface of the state of the tracker. */
public interface TrackerInfo {

	/** Returns the ID of the tracker. Only used in this application. */
	public int getId();
	
	/** Returns the URL of the tracker. */
	public String getUrl();
	
	/** Returns the status of the tracker. */
	public int getStatus();
	
	/** Returns the number of complete peers (seeds). */
	public int getComplete();
	
	/** Returns the number of incomplete peers (leechers). */
	public int getIncomplete();
	
	/** Returns the remaining time (in seconds) until the next announce. */
	public int getRemainingTime();
}
