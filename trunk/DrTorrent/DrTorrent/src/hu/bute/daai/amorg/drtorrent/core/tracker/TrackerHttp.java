package hu.bute.daai.amorg.drtorrent.core.tracker;

import hu.bute.daai.amorg.drtorrent.core.exception.DrTorrentException;
import hu.bute.daai.amorg.drtorrent.core.torrent.TorrentManager;
import hu.bute.daai.amorg.drtorrent.network.HttpConnection;
import hu.bute.daai.amorg.drtorrent.network.UrlEncoder;
import hu.bute.daai.amorg.drtorrent.util.Log;
import hu.bute.daai.amorg.drtorrent.util.Preferences;
import hu.bute.daai.amorg.drtorrent.util.Tools;
import hu.bute.daai.amorg.drtorrent.util.bencode.Bencoded;
import hu.bute.daai.amorg.drtorrent.util.bencode.BencodedDictionary;
import hu.bute.daai.amorg.drtorrent.util.bencode.BencodedInteger;
import hu.bute.daai.amorg.drtorrent.util.bencode.BencodedList;
import hu.bute.daai.amorg.drtorrent.util.bencode.BencodedString;

/** Class that represents HTTP trackers. */
public class TrackerHttp extends Tracker {
	private static final String LOG_TAG = "TrackerHttp";
	
	/** Constructor of the tracker with its URL and running torrent. */
	public TrackerHttp(String url, TrackerObserver torrent) {
		super(url, torrent);
	}
	
	/** Processes the response of the tracker */
	public void processResponse(Bencoded responseBencoded) throws DrTorrentException { 
        if (responseBencoded.type() != Bencoded.BENCODED_DICTIONARY) {
        	throw new DrTorrentException("The response of the tracker is not a becoded dictionary.");
        }

        final BencodedDictionary response = (BencodedDictionary) responseBencoded;  
        Bencoded value = null;

        // failure reason
        value = response.entryValue("failure reason");
        if (value != null && (value.type() == Bencoded.BENCODED_STRING)) {
        	final String reason = ((BencodedString) value).getStringValue();
            throw new DrTorrentException("The tracker failed. Reason: " + reason);
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
        value = response.entryValue("peers");
        // Normal tracker response
        if (value != null && (value.type() == Bencoded.BENCODED_LIST)) {
            final BencodedList bencodedPeers = (BencodedList) value;

            Log.v(LOG_TAG, "Number of peers received: " + bencodedPeers.count());

            for (int i = 0; i < bencodedPeers.count(); i++) {
                value = bencodedPeers.item(i);

                if (value.type() != Bencoded.BENCODED_DICTIONARY) {
                	continue;
                	//throw new DrTorrentException("An item of the peer list is not a bencoded dictionary.");
                }

                final BencodedDictionary bencodedPeer = (BencodedDictionary) value;

                String peerId = null;
                // peer id
                value = bencodedPeer.entryValue("peer id");
                if (value != null && (value.type() == Bencoded.BENCODED_STRING))
                {
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
                if (value == null || (value.type() != Bencoded.BENCODED_STRING)) {
                    continue;
                }
                String ip = ((BencodedString) value).getStringValue();

                // port
                value = bencodedPeer.entryValue("port");
                if (value == null || (value.type() != Bencoded.BENCODED_INTEGER)) {
                    continue;
                }
                int port = (int) ((BencodedInteger) value).getValue();

                Log.v(LOG_TAG, "Peer address: " + ip + ":" + port);                  

                /*if (Preferences.LocalAddress != null) {
                    if (Preferences.LocalAddress.equals(ip)) {
                        Log.v(LOG_TAG, "Got own IP address from tracker, throwing it away...");
                    }
                }*/

                // Add peer to torrent
                torrent_.addPeer(ip, port, peerId);
            }
        }
        // likely a compact response
        else if (value != null && (value.type() == Bencoded.BENCODED_STRING)) {
            final BencodedString bencodedPeers = (BencodedString) value;

            if ((bencodedPeers.getValue().length % 6) == 0) {
                final byte[] ips = bencodedPeers.getValue();

                for (int pos = 0; pos + 6 <= ips.length; pos += 6) {
                    final String address = Tools.readIp(ips, pos);
                    if (address == null) {
                    	break;
                    }
                    final int port = Tools.readInt16(ips, pos + 4);

                    Log.v(LOG_TAG, address + ":" + port);
                    /*if (Preferences.LocalAddress != null) {
                        if (Preferences.LocalAddress.equals(addressBuffer) && port==Preferences.IncommingPort) {
                            Log.v(LOG_TAG, "Got own local address from tracker, throwing it away...");
                        }
                    }*/

                    torrent_.addPeer(address, port, null);
                }        
            } else {
            	throw new DrTorrentException("The compact response is invalid, its length cannot be divided by 6 without remainder.");
            }
        } else {
        	throw new DrTorrentException("Peer list cannot be found or peer list is invalid.");
        }
        
        // Log.v(LOG_TAG, "Response procesed, peers: " + peers_.size());
    }
	
	/** Creates the query string from the current state of the tracker. */
	private String createUri() {
        //String localAddress = NetTools.getLocalAddress(torrent.getAnnounce());
		final StringBuilder uriB = new StringBuilder("?");
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
	
	/** Connects to the tracker over HTTP protocol. */
	@Override
	protected void connect() {
		final String fullUrl = url_ + createUri();
		
		final HttpConnection conn = new HttpConnection(fullUrl);
		final byte[] response = conn.execute();
		
		if (event_ != EVENT_STOPPED) {
			if (response != null) {
				final Bencoded bencoded = Bencoded.parse(response);
			
				if (bencoded == null) {
					status_ = STATUS_FAILED;
					failCount_++;
					Log.v(LOG_TAG, "Failed to bencode the response of the tracker.");
					return;
				}
				status_ = STATUS_WORKING;
				Log.v(LOG_TAG, "Bencoded response processing...");
				try {
					processResponse(bencoded);
				} catch (DrTorrentException e) {
					failCount_++;
				}
			} else {
				status_ = STATUS_FAILED;
				interval_ = ERROR_REQUEST_INTERVAL;
			}
		} else {
			status_ = STATUS_UNKNOWN;
		}
		
		torrent_.refreshSeedsAndLeechersCount();
	}
}
