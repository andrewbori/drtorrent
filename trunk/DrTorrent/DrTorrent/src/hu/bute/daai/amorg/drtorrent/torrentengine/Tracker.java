package hu.bute.daai.amorg.drtorrent.torrentengine;

import hu.bute.daai.amorg.drtorrent.Preferences;
import hu.bute.daai.amorg.drtorrent.coding.bencode.Bencoded;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedDictionary;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedInteger;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedList;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedString;
import hu.bute.daai.amorg.drtorrent.network.HttpConnection;
import hu.bute.daai.amorg.drtorrent.network.UrlEncoder;
import android.util.Log;


/** Class representing the tracker */
public class Tracker {

	public final static int ERROR_NONE = 200;
	public final static int ERROR_WRONG_CONTENT = 401;
	
	private final static int DEFAULT_REQUEST_INTERVAL = 600;
	private final static int ERROR_REQUEST_INTERVAL = 180;
	
	private final static String LOG_TAG = "Tracker"; 
	
	public final static int STATUS_CONNECTING  = 100;
	public final static int STATUS_WORKING     = 200;
	public final static int STATUS_UNKNOWN     = 400;
	public final static int STATUS_FAILED      = 500;
	
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
	
	/** Processes the response of the tracker */
	public int processResponse(Bencoded responseBencoded) { 
        if (responseBencoded.type() != Bencoded.BencodedDictionary) {
        	return ERROR_WRONG_CONTENT;
        }

        BencodedDictionary response = (BencodedDictionary) responseBencoded;  
        Bencoded value = null;

        // failure reason
        value = response.entryValue("failure reason");
        if (value != null && (value.type() == Bencoded.BencodedString)) {
            Log.v(LOG_TAG, "[Tracker] Request failed, reason: " + ((BencodedString) value).getStringValue());
            return ERROR_WRONG_CONTENT;
        }
        
        // interval
        value = response.entryValue("interval");
        if (value != null && (value.type() == Bencoded.BencodedInteger)) {
            interval_ = ((BencodedInteger) value).getValue();
            if (!((interval_ > 0) && (interval_ < 12000))) {
            	interval_ = DEFAULT_REQUEST_INTERVAL;
            }
        }	

        // complete
        value = response.entryValue("complete");
        if (value != null && (value.type() == Bencoded.BencodedInteger)) {
            complete_ = ((BencodedInteger) value).getValue();
        }
        
        // incomplete
        value = response.entryValue("incomplete");
        if (value != null && (value.type() == Bencoded.BencodedInteger)) {
            incomplete_ = ((BencodedInteger) value).getValue();
        }
        
        Log.v(LOG_TAG, "seeds/leechers: " + complete_ + "/" + incomplete_);
        
        // tracker id
        value = response.entryValue("tracker id");
        if (value != null && (value.type() == Bencoded.BencodedString)) {
        	trackerId_ = ((BencodedString) value).getStringValue();            
        }

        // peers
        Log.v(LOG_TAG, "Local peer id: " + TorrentManager.getPeerID());

        value = response.entryValue("peers");
        // Normal tracker response
        if (value != null && (value.type() == Bencoded.BencodedList)) {
            BencodedList bencodedPeers = (BencodedList) value;

            Log.v(LOG_TAG, "Number of peers received: " + bencodedPeers.count());

            for (int i=0; i<bencodedPeers.count(); i++) {
                value = bencodedPeers.item(i);

                if (value.type() != Bencoded.BencodedDictionary) {
                    return ERROR_WRONG_CONTENT;
                }

                BencodedDictionary bencodedPeer = (BencodedDictionary) value;

                // peer id
                value = bencodedPeer.entryValue("peer id");
                if (value==null || (value.type() != Bencoded.BencodedString))
                    continue;
                String peerId = ((BencodedString) value).getStringValue();

                Log.v(LOG_TAG, "Processing peer id: " + peerId);
                if (peerId.length() != 20)
                    continue;

                // got back our own address
                if (peerId.equals(TorrentManager.getPeerID()))
                    continue;

                // ip
                value = bencodedPeer.entryValue("ip");
                if (value==null || (value.type() != Bencoded.BencodedString))
                    continue;
                String ip = ((BencodedString) value).getStringValue();

                // port
                value = bencodedPeer.entryValue("port");
                if (value==null || (value.type() != Bencoded.BencodedInteger))
                    continue;

                int port = ((BencodedInteger) value).getValue();

                Log.v(LOG_TAG, "Peer address: " + ip + ":" + port);                  

                /*if (Preferences.LocalAddress != null) {
                    if (Preferences.LocalAddress.equals(ip)) {
                        Log.v(LOG_TAG, "Got own IP address from tracker, throwing it away...");
                    }
                }*/

                torrent_.addPeer(ip, port, peerId);
            }
        }
        // likely a compact response
        else if (value != null && (value.type() == Bencoded.BencodedString)) {
            BencodedString bencodedPeers = (BencodedString) value;

            if ((bencodedPeers.getValue().length % 6) == 0) {
                byte[] ips = bencodedPeers.getValue();
                int[] peersRandomPos = new int[ips.length/6];
                for (int j=0; j<peersRandomPos.length; j++) {
                    peersRandomPos[j]=j;
                }
                //NetTools.shuffle(peersRandomPos);

                for (int i = 0; i < ips.length/6 /*&& peers.size()<MaxStoredPeers*/; i++) {
                    int pos = peersRandomPos[i] * 6;

                    int a, b, c, d;                         
                    if (ips[pos]<0)   a = 256 + ips[pos];
                    else              a = ips[pos];
                    if (ips[pos+1]<0) b = 256 + ips[pos+1];
                    else              b = ips[pos+1];
                    if (ips[pos+2]<0) c = 256 + ips[pos+2];
                    else              c = ips[pos+2];
                    if (ips[pos+3]<0) d = 256 + ips[pos+3];
                    else              d = ips[pos+3];                        
                    
                    String address = "" + a + "." + b + "." + c + "." + d;

                    int p1, p2;
                    if (ips[pos+4]<0) p1 = 256 + ips[pos+4] << 8;
                    else			  p1 = ips[pos+4] << 8;
                    if (ips[pos+5]<0) p2 = 256 + ips[pos+5];
                    else			  p2 = ips[pos+5];                                           
                    
                    int port = p1 + p2;

                    Log.v(LOG_TAG, address + ":" + port);
                    /*if (Preferences.LocalAddress != null) {
                        if (Preferences.LocalAddress.equals(addressBuffer) && port==Preferences.IncommingPort) {
                            Log.v(LOG_TAG, "Got own local address from tracker, throwing it away...");
                        }
                    }*/

                    torrent_.addPeer(address, port, null);
                }        
            }
            else
                Log.v(LOG_TAG, "[Tracker] Compact response invalid (length cannot be devided by 6 without remainder)");
        }        
        else
        	Log.v(LOG_TAG, "[Tracker] No peers list / peers list invalid");
        
        // Log.v(LOG_TAG, "Response procesed, peers: " + peers_.size());
        
        return ERROR_NONE;
    }
	
	/** Creates the query string from the current state of the tracker. */
	private String createUri() {
        //String localAddress = NetTools.getLocalAddress(torrent.getAnnounce());
		StringBuilder uriB = new StringBuilder("?");
		uriB.append("info_hash=").append(UrlEncoder.encode(torrent_.getInfoHash()))        
        	.append("&peer_id=").append(TorrentManager.getPeerID())
        	.append("&key=").append(TorrentManager.getPeerKey())
        	.append("&port=").append(Preferences.getPort()) 	// Preferences.IncommingPort;

	        /*if (localAddress != null)
	           uriBuf = uriBuf + Preferences.IncommingPort;  // localAddress.port needed!!!!
	        else
	           uriBuf = uriBuf + Preferences.IncommingPort;*/                    

        	.append("&uploaded=").append(torrent_.getBytesUploaded())
        	.append("&downloaded=").append(torrent_.getBytesDownloaded())
        	.append("&left=").append(torrent_.getBytesLeft())            
        	.append("&numwant=50")
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
		lastRequest_ = System.currentTimeMillis();
		status_ = STATUS_CONNECTING;
		(new TrackerConnectionThread()).start();
	}
	
	/** Changes the event and notifies the tracker by connecting to it. */
	public void changeEvent(int event) {
		if ((System.currentTimeMillis() - lastRequest_) < 120000 && event == EVENT_NOT_SPECIFIED) return;
		
		event_ = event;
		connect();
	}
	
	/** Thread that is connecting to the tracker. */ 
	private class TrackerConnectionThread extends Thread {

		@Override
		public void run() {
			String fullUrl = url_ + createUri();
			
			HttpConnection conn = new HttpConnection(fullUrl);
			byte[] response = conn.execute();
			
			if (event_ != EVENT_STOPPED && response != null) {
				Bencoded bencoded = Bencoded.parse(response);
			
				if (bencoded == null) {
					status_ = STATUS_FAILED;
					failCount_++;
					Log.v(LOG_TAG, "Faild to bencode the response of the tracker.");
					return;
				}
				status_ = STATUS_WORKING;
				Log.v(LOG_TAG, "Bencoded response processing...");
				if (processResponse(bencoded) != ERROR_NONE) failCount_++;
			}
		}
	}
	
	public String getUrl() {
		return url_;
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
	
	public long getLastRequest() {
		return lastRequest_;
	}
	
	public int getFailCount() {
		return failCount_;
	}
}