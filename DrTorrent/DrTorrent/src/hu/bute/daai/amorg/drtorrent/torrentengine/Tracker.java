package hu.bute.daai.amorg.drtorrent.torrentengine;

import hu.bute.daai.amorg.drtorrent.coding.bencode.Bencoded;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedDictionary;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedInteger;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedString;
import hu.bute.daai.amorg.drtorrent.network.HttpConnection;
import hu.bute.daai.amorg.drtorrent.network.UrlEncoder;

import java.util.Date;
import java.util.Vector;

import android.util.Log;


/** Class representing the tracker */
public class Tracker {

	private final static String LOG_TAG = "Tracker"; 
	
	public final static int STATUS_UNKNOWN = 0;
	public final static int STATUS_WORKING = 1;
	public final static int STATUS_FAILED  = 2;
	
	public static final int EVENT_NOT_SPECIFIED = 0;
    public static final int EVENT_STARTED       = 1;
    public static final int EVENT_STOPPED       = 2;
    public static final int EVENT_COMPLETED     = 3;
	
	private Torrent torrent_;
	private String url_;
	private int status_;
	private int interval_;
	private String trackerId_ = null;
	private int complete_;
	private int incomplete_;
	private Vector<Peer> peers_;
	private long lastRequest_;
	private int event_ = EVENT_STARTED;
	
	private int failCount_;

	public Tracker() {}
	
	public Tracker(String url, Torrent torrent) {
		url_ = url;
		torrent_ = torrent;
		status_ = STATUS_UNKNOWN;
		failCount_ = 0;
	}
	
	/** Sets the tracker from a bencoded source. */
	public void set(Bencoded bencode) {
        if(bencode.type() != Bencoded.BencodedDictionary) return;

        final BencodedDictionary mainDict = (BencodedDictionary) bencode;

        if(mainDict.entryValue("failure reason") != null) return;

        try
        {
        	interval_ = ((BencodedInteger) mainDict.entryValue("interval")).getValue();
        	complete_ = ((BencodedInteger) mainDict.entryValue("complete")).getValue();
        	incomplete_ = ((BencodedInteger) mainDict.entryValue("incomplete")).getValue();
        } catch(Exception e) { }

        Log.v(LOG_TAG, "seeds/leechers: " + complete_ + "/" + incomplete_);
        
        BencodedString id = (BencodedString) mainDict.entryValue("tracker id");
        if (id != null) {
            trackerId_ = id.getStringValue();
        }

        lastRequest_ = (new Date()).getTime();
        status_ = STATUS_WORKING;
    }
	
	/** Creates the query string from the current state of the tracker. */
	private String createUri() {
        //String localAddress = NetTools.getLocalAddress(torrent.getAnnounce());
		StringBuilder uriB = new StringBuilder("?");
		uriB.append("info_hash=").append(UrlEncoder.encode(torrent_.getInfoHash()))        
        	.append("&peer_id=").append(torrent_.getTorrentManager().getPeerID())
        	.append("&key=").append(torrent_.getTorrentManager().getPeerKey())
        	.append("&port=").append("36405") 	// Preferences.IncommingPort;

	        /*if (localAddress != null)
	           uriBuf = uriBuf + Preferences.IncommingPort;  // localAddress.port needed!!!!
	        else
	           uriBuf = uriBuf + Preferences.IncommingPort;*/                    

        	.append("&uploaded=").append(torrent_.getBytesUploaded())
        	.append("&downloaded=").append(torrent_.getBytesDownloaded())
        	.append("&left=").append(torrent_.getBytesLeft())            
        	.append("&compact=1");	// it seems that some trackers support only compact responses

        // if connection's event is specified
        if (event_ != EVENT_NOT_SPECIFIED) {
            uriB.append("&event=");
            switch (event_) {
                case EVENT_STARTED:
                    uriB.append("started");
                    break;
                case EVENT_STOPPED:
                	uriB.append("stopped");
                	break;
                case EVENT_COMPLETED:
                    uriB.append("completed");
                    break;     
                default:
            }
        }
        
        if (trackerId_ != null) {
        	uriB.append(trackerId_);
        }
        
        return uriB.toString();
    }
	
	/** Connects to the tracker. */
	public void connect() {
		(new TrackerConnectionThread()).start();
	}
	
	public void changeEvent(int event) {
		event_ = event;
		connect();
	}
	
	/** Async task to connect to the tracker. */ 
	private class TrackerConnectionThread extends Thread {

		@Override
		public void run() {
			String fullUrl = url_ + createUri();
			
			HttpConnection conn = new HttpConnection(fullUrl);
			byte[] response = conn.execute();
			
			Bencoded bencoded = Bencoded.parse(response);
			if (bencoded == null) {
				status_ = STATUS_FAILED;
				Log.v(LOG_TAG, "Faild to bencode the response of the tracker.");
				return;
			}
			status_ = STATUS_WORKING;
			Log.v(LOG_TAG, "Bencoded response processing...");
			set(bencoded);
			if (event_ != EVENT_STOPPED) torrent_.processTrackerResponse(bencoded);
		}
	}
	
	public int getStatus() {
		return status_;
	}
	
	public int getInterval() {
		return interval_;
	}
	
	public int getComplete() {
		return complete_;
	}
	
	public int getIncomplete() {
		return incomplete_;
	}
}