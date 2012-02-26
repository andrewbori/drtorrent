package hu.bute.daai.amorg.drtorrent.torrentengine;

import hu.bute.daai.amorg.drtorrent.bencode.Bencoded;
import hu.bute.daai.amorg.drtorrent.bencode.BencodedDictionary;
import hu.bute.daai.amorg.drtorrent.bencode.BencodedInteger;
import hu.bute.daai.amorg.drtorrent.bencode.BencodedString;
import hu.bute.daai.amorg.drtorrent.network.UrlEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Vector;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;
import android.util.Log;


/** Class reprenting the tracker */
public class Tracker {

	private final static String LOG_TAG = "Tracker"; 
	
	public final static int UNKNOWN 		 = 0;
	public final static int WORKING 		 = 1;
	public final static int FAILD_TO_CONNECT = 2;
	
	public static final int EVENT_NOT_SPECIFIED = 0;
    public static final int EVENT_STARTED       = 1;
    public static final int EVENT_STOPPED       = 2;
    public static final int EVENT_COMPLETED     = 3;
	
	private Torrent torrent_;
	private String url_;
	private int status_;
	private int interval_;
	private String trackerId;
	private int complete_;
	private int incomplete_;
	private Vector<Peer> peers_;
	private long lastRequest_;
	private int event_ = 1;

	public Tracker(String url, Torrent torrent) {
		this.url_ = url;
		this.torrent_ = torrent;
	}
	
	/** Sets the tracker from a bencoded source. */
	public void set(Bencoded bencode)
    {
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
        if(id != null) {
            trackerId = id.getStringValue();
        }

        lastRequest_ = (new Date()).getTime();
        status_ = WORKING;
    }
	
	/** Creates the query string from the curent state of the tracker */
	private String createUri()
    {
        //String localAddress = NetTools.getLocalAddress(torrent.getAnnounce());

        String uriBuf="?";
        uriBuf = uriBuf + "info_hash=" + UrlEncoder.encode(torrent_.getInfoHash());        
        uriBuf = uriBuf + "&peer_id=" + torrent_.getTorrentManager().getPeerID();
        //uriBuf = uriBuf + "&key=" + 503063430;//torrent_.getTorrentManager().getKey();
        
        uriBuf = uriBuf + "&port=" + 6882; //Preferences.IncommingPort;

        /*if (localAddress != null)
           uriBuf = uriBuf + Preferences.IncommingPort;  // localAddress.port needed!!!!
        else
           uriBuf = uriBuf + Preferences.IncommingPort;*/                    

        uriBuf = uriBuf + "&uploaded=0";
        uriBuf = uriBuf + "&downloaded=0";
        uriBuf = uriBuf + "&left=" + torrent_.getBytesLeft();            

        // it seems that some trackers support only compact responses
        uriBuf = uriBuf + "&compact=1";            

        if (event_ != EVENT_NOT_SPECIFIED) // if not specificed
        {
            uriBuf = uriBuf + "&event=";

            switch (event_)
            {
                case EVENT_STARTED:
                    uriBuf = uriBuf + "started";
                    break;

                case EVENT_STOPPED:
                	uriBuf = uriBuf + "stopped";
                break;
                
                case EVENT_COMPLETED:
                    uriBuf = uriBuf + "completed";
                    break;

                default:
                	break;
            }
        }

        return uriBuf;
    }
	
	/** Connects to the tracker. */
	public void connect() {
		(new TrackerConnectionAsyncTask()).execute();
	}
	
	/** Async task to connect to the tracker. */ 
	private class TrackerConnectionAsyncTask extends AsyncTask<String, Integer, Boolean> {

		@Override
		protected Boolean doInBackground(String... params) {
			
			String str = url_ + createUri();
			
			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet(str);
			
			try {
				Log.v(LOG_TAG, "Connecting to the tracker: " + str);
				HttpResponse response = client.execute(get);
				Log.v(LOG_TAG, "Reading the tracker's respones: " + str);
				
				InputStream is = response.getEntity().getContent();
				ByteArrayOutputStream bf = new ByteArrayOutputStream();
				int ch;
	            while((ch = is.read()) != -1)
	            {
	                bf.write(ch);
	            }
				
				Log.v(LOG_TAG, bf.toString());
				
				Bencoded bencoded = Bencoded.parse(bf.toByteArray());
				if (bencoded == null) {
					Log.v(LOG_TAG, "Unsuccessful bencoding tracker's response.");
					return false;
				}
				Log.v(LOG_TAG, "Process response");
				set(bencoded);
				torrent_.processTrackerResponse(bencoded);
				
				return true;
				
			} catch (IOException e) {
				Log.v(LOG_TAG, e.getMessage());
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
		}
	}
	
	public int getComplete() {
		return complete_;
	}
	
	public int getIncomplete() {
		return incomplete_;
	}
}