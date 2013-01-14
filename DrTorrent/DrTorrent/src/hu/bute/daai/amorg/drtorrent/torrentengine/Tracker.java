package hu.bute.daai.amorg.drtorrent.torrentengine;

import android.os.SystemClock;


/** Class representing the tracker */
public abstract class Tracker {

	public final static int ERROR_NONE = 200;
	public final static int ERROR_WRONG_CONTENT = 401;
	
	protected final static int DEFAULT_REQUEST_INTERVAL = 600;
	protected final static int ERROR_REQUEST_INTERVAL = 300;
	
	protected final static String LOG_TAG = "Tracker"; 
	
	public final static int STATUS_UNKNOWN	   = 0;
	public final static int STATUS_UPDATING    = 100;
	public final static int STATUS_WORKING     = 200;
	public final static int STATUS_FAILED      = 400;
	
	public static final int EVENT_NOT_SPECIFIED = 0;
    public static final int EVENT_STARTED       = 1;
    public static final int EVENT_STOPPED       = 2;
    public static final int EVENT_COMPLETED     = 3;
	
    protected static int ID = 0;
    
    final protected int id_;
    final protected Torrent torrent_;
    final protected String url_;
    protected int status_ = STATUS_UNKNOWN;
    protected int interval_ = DEFAULT_REQUEST_INTERVAL;
    protected String trackerId_ = null;
    protected int complete_;
    protected int incomplete_;
    protected long lastRequest_;
    protected int event_ = EVENT_STARTED;
	
    protected int failCount_ = 0;
	
    /** Constructor of the tracker with its URL and running torrent. */
	public Tracker(String url, Torrent torrent) {
		url_ = url;
		torrent_ = torrent;
		
		id_ = ++ID;
	}
	
	/** Connects to the tracker. */
	public void connect() {
		lastRequest_ = SystemClock.elapsedRealtime();
		status_ = STATUS_UPDATING;
		(new TrackerConnectionThread()).start();
	}
	
	/** Changes the event and notifies the tracker by connecting to it. */
	public void changeEvent(int event) {
		event_ = event;
		connect();
	}
	
	/** onTimer */
	public void onTimer() {
		if (status_ != STATUS_UPDATING && getRemainingTime() < 0) {
			changeEvent(EVENT_NOT_SPECIFIED);
		}
	}
	
	/** Does the connection stuff. This is a protocol specific method. */
	protected abstract void doConnect();
	
	/** Thread that is connecting to the tracker. */ 
	private class TrackerConnectionThread extends Thread {
		@Override
		public void run() {
			doConnect();
		}
	}
	
	/** Returns the ID of the tracker. Only used in this application. */
	public int getId() {
		return id_;
	}
	
	/** Returns the URL of the tracker. */
	public String getUrl() {
		return url_;
	}
	
	/** Returns the status of the tracker. */
	public int getStatus() {
		return status_;
	}
	
	/** Returns the interval (in seconds) of the tracker. */ 
	public int getInterval() {
		return interval_;
	}
	
	/** Returns the number of complete peers (seeds). */
	public int getComplete() {
		return complete_;
	}
	
	/** Returns the number of incomplete peers (leechers). */
	public int getIncomplete() {
		return incomplete_;
	}
	
	/** Returns the time of the last request (announce). */
	public long getLastRequest() {
		return lastRequest_;
	}
	
	/** Resets the time of the last request. */
	public void resetLastRequest() {
		if (status_ != STATUS_UPDATING) {
			interval_ = 0;
			lastRequest_ = 0;
		}
	}
	
	/** Returns the remaining time (in seconds) until the next announce. */
	public int getRemainingTime() {
		return interval_ - (int) (SystemClock.elapsedRealtime() - lastRequest_) / 1000;
	}
	
	/** Returns the number of failed announces. */
	public int getFailCount() {
		return failCount_;
	}
}