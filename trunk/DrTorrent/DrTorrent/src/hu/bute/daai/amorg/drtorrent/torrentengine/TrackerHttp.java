package hu.bute.daai.amorg.drtorrent.torrentengine;

import hu.bute.daai.amorg.drtorrent.Preferences;
import hu.bute.daai.amorg.drtorrent.Tools;
import hu.bute.daai.amorg.drtorrent.coding.bencode.Bencoded;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedDictionary;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedInteger;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedList;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedString;
import hu.bute.daai.amorg.drtorrent.network.HttpConnection;
import hu.bute.daai.amorg.drtorrent.network.UrlEncoder;
import android.util.Log;

public class TrackerHttp extends Tracker {
	private static final String LOG_TAG = "TrackerTcp";
	
	/** Constructor of the tracker with its URL and running torrent. */
	public TrackerHttp(String url, Torrent torrent) {
		super(url, torrent);
	}
	
	/** Processes the response of the tracker */
	public int processResponse(Bencoded responseBencoded) { 
        if (responseBencoded.type() != Bencoded.BENCODED_DICTIONARY) {
        	return Tracker.ERROR_WRONG_CONTENT;
        }

        BencodedDictionary response = (BencodedDictionary) responseBencoded;  
        Bencoded value = null;

        // failure reason
        value = response.entryValue("failure reason");
        if (value != null && (value.type() == Bencoded.BENCODED_STRING)) {
            Log.v(LOG_TAG, "[Tracker] Request failed, reason: " + ((BencodedString) value).getStringValue());
            return Tracker.ERROR_WRONG_CONTENT;
        }
        
        // interval
        value = response.entryValue("interval");
        if (value != null && (value.type() == Bencoded.BENCODED_INTEGER)) {
            interval_ = (int) ((BencodedInteger) value).getValue();
            if (!((interval_ > 0) && (interval_ < 12000))) {
            	interval_ = Tracker.DEFAULT_REQUEST_INTERVAL;
            }
        }	

        // complete
        value = response.entryValue("complete");
        if (value != null && (value.type() == Bencoded.BENCODED_INTEGER)) {
            complete_ = (int) ((BencodedInteger) value).getValue();
        }
        
        // incomplete
        value = response.entryValue("incomplete");
        if (value != null && (value.type() == Bencoded.BENCODED_INTEGER)) {
            incomplete_ = (int) ((BencodedInteger) value).getValue();
        }
        
        Log.v(LOG_TAG, "seeds/leechers: " + complete_ + "/" + incomplete_);
        
        // tracker id
        value = response.entryValue("tracker id");
        if (value != null && (value.type() == Bencoded.BENCODED_STRING)) {
        	trackerId_ = ((BencodedString) value).getStringValue();            
        }

        // peers
        Log.v(LOG_TAG, "Local peer id: " + TorrentManager.getPeerID());

        value = response.entryValue("peers");
        // Normal tracker response
        if (value != null && (value.type() == Bencoded.BENCODED_LIST)) {
            BencodedList bencodedPeers = (BencodedList) value;

            Log.v(LOG_TAG, "Number of peers received: " + bencodedPeers.count());

            for (int i=0; i<bencodedPeers.count(); i++) {
                value = bencodedPeers.item(i);

                if (value.type() != Bencoded.BENCODED_DICTIONARY) {
                    return Tracker.ERROR_WRONG_CONTENT;
                }

                BencodedDictionary bencodedPeer = (BencodedDictionary) value;

                String peerId = null;
                // peer id
                value = bencodedPeer.entryValue("peer id");
                if (value==null || (value.type() != Bencoded.BENCODED_STRING))
                {} else {
	                peerId = ((BencodedString) value).getStringValue();
	
	                Log.v(LOG_TAG, "Processing peer id: " + peerId);
	                if (peerId.length() != 20)
	                    continue;
	
	                // got back our own address
	                if (peerId.equals(TorrentManager.getPeerID()))
	                    continue;
                }

                // ip
                value = bencodedPeer.entryValue("ip");
                if (value==null || (value.type() != Bencoded.BENCODED_STRING))
                    continue;
                String ip = ((BencodedString) value).getStringValue();

                // port
                value = bencodedPeer.entryValue("port");
                if (value==null || (value.type() != Bencoded.BENCODED_INTEGER))
                    continue;
                int port = (int) ((BencodedInteger) value).getValue();

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
        else if (value != null && (value.type() == Bencoded.BENCODED_STRING)) {
            BencodedString bencodedPeers = (BencodedString) value;

            if ((bencodedPeers.getValue().length % 6) == 0) {
                byte[] ips = bencodedPeers.getValue();

                for (int pos = 0; pos + 6 <= ips.length; pos += 6) {
                    String address = Tools.readIp(ips, pos);
                    if (address == null) {
                    	break;
                    }
                    int port = Tools.readInt16(ips, pos + 4);

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
        
        return Tracker.ERROR_NONE;
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
        	//.append("&numwant=50")
        	.append("&compact=1");	// it seems that some trackers support only compact responses

        // if connection's event is specified
        if (event_ != Tracker.EVENT_NOT_SPECIFIED) {
            uriB.append("&event=");
            switch (event_) {
                case Tracker.EVENT_STARTED:
                    uriB.append("started");
                    break;
                case Tracker.EVENT_STOPPED:
                	uriB.append("stopped");
                	break;
                case Tracker.EVENT_COMPLETED:
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
	
	@Override
	protected void doConnect() {
		String fullUrl = url_ + createUri();
		
		HttpConnection conn = new HttpConnection(fullUrl);
		byte[] response = conn.execute();
		
		if (event_ != EVENT_STOPPED) {
			if (response != null) {
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
			} else {
				status_ = STATUS_FAILED;
				interval_ = ERROR_REQUEST_INTERVAL;
			}
		} else {
			status_ = STATUS_UNKNOWN;
		}
		
		torrent_.setSeedsLeechers();
	}
}
