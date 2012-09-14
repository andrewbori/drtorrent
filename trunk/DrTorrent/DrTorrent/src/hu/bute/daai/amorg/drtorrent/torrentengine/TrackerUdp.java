package hu.bute.daai.amorg.drtorrent.torrentengine;

import hu.bute.daai.amorg.drtorrent.Tools;
import hu.bute.daai.amorg.drtorrent.Preferences;
import hu.bute.daai.amorg.drtorrent.network.UdpConnection;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Random;

import android.util.Log;

public class TrackerUdp extends Tracker {
	private static final String LOG_TAG = "TrackerTcp";
	
	private final static int ACTION_ID_CONNECT	= 0;
	private final static int ACTION_ID_ANNOUNCE = 1;
	private final static int ACTION_ID_SCRAPE	= 2;
	private final static int ACTION_ID_ERROR 	= 3;
	
	private String host_ = null;
	private int port_ = -1;
	
	private byte[] connectionId_ = null;	// length: 4 (64 bit)
	private byte[] transactionId_ = null;	// length: 4 (32 bit)
	
	/** Constructor of the tracker with its URL and running torrent. */
	public TrackerUdp(String url, Torrent torrent) {
		super(url, torrent);
		
		URI uri = URI.create(url);
		host_ = uri.getHost();
		port_ = uri.getPort();
		if (port_ == -1) port_ = 80;
	}
	
	/** Sends a connection request to the tracker and reads its response. */
	private byte[] connecting() {
		if (transactionId_ == null) {
			transactionId_ = new byte[4];
			Random r = new Random();
			r.nextBytes(transactionId_);
		}
		
		byte[] message = {
				0, 0, (byte) 0x04, (byte) 0x17, (byte) 0x27, (byte) 0x10, (byte) 0x19, (byte) 0x80,
				0, 0, 0, 0,
				transactionId_[0], transactionId_[1], transactionId_[2], transactionId_[3] 
		};
		
		/*ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			baos.write(Tools.int64ToByteArray(0x41727101980L));
			baos.write(Tools.int32ToByteArray(ACTION_ID_CONNECT));
			baos.write(transactionId_);
		} catch (Exception e) {}*/
		
		UdpConnection udpConnection = new UdpConnection(host_, port_, message, 16);
		return udpConnection.execute();
	}
	
	/** Sends a announcing request to the tracker and reads its response. */
	private byte[] announcing() {
		if (transactionId_ == null) {
			transactionId_ = new byte[4];
			Random r = new Random();
			r.nextBytes(transactionId_);
		}
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			baos.write(connectionId_);
			baos.write(Tools.int32ToByteArray(ACTION_ID_ANNOUNCE));
			baos.write(transactionId_);
			baos.write(torrent_.getInfoHashByteArray());
			baos.write(TorrentManager.getPeerID().getBytes());
			baos.write(Tools.int64ToByteArray(torrent_.getBytesDownloaded()));
			baos.write(Tools.int64ToByteArray(torrent_.getBytesLeft()));
			baos.write(Tools.int64ToByteArray(torrent_.getBytesUploaded()));
			baos.write(Tools.int32ToByteArray(event_));
			baos.write(Tools.int32ToByteArray(0));	// IP
			baos.write(Tools.int32ToByteArray(TorrentManager.getPeerKey()));
			baos.write(Tools.int32ToByteArray(-1));	// Number of peers (default)
			baos.write(Tools.int16ToByteArray(Preferences.getPort()));
		} catch (Exception e) {}
		
		UdpConnection udpConnection = new UdpConnection(host_, port_, baos.toByteArray(), 320);		// Response buffer for max 50 peers
		return udpConnection.execute();
	}
	
	/** Processes the response of the tracker */
	public int processResponse(byte[] response) {
		if (response.length < 8) return ERROR_WRONG_CONTENT;
		
		if ((response[0] == 0 && response[1] == 0 && response[2] == 0 && (response[3] >= 0 && response[3] <= 3)) &&
			(response[4] == transactionId_[0] && response[5] == transactionId_[1] &&
			 response[6] == transactionId_[2] && response[7] == transactionId_[3])) {
		
			switch (response[3]) {
				case ACTION_ID_CONNECT:
					if (response.length < 16) return ERROR_WRONG_CONTENT;
					
					connectionId_ = new byte[8];
					for (int i = 0; i < 8; i++) {
						connectionId_[i] = response[8 + i];
					}
					transactionId_ = null;
					return ERROR_NONE;
					
				case ACTION_ID_ANNOUNCE:
					if (response.length < 20) return ERROR_WRONG_CONTENT;
					
					interval_ = Tools.readInt32(response, 8);
					incomplete_ = Tools.readInt32(response, 12);
					complete_ = Tools.readInt32(response, 16);
					
					for (int pos = 20; pos + 6 <= response.length; pos += 6) {
	                    String address = Tools.readIp(response, pos);
	                    if (address == null) {
	                    	transactionId_ = null;
	                    	return ERROR_NONE;
	                    }
	                    int port = Tools.readInt16(response, pos + 4);

	                    Log.v(LOG_TAG, address + ":" + port);
	                    /*if (Preferences.LocalAddress != null) {
	                        if (Preferences.LocalAddress.equals(addressBuffer) && port==Preferences.IncommingPort) {
	                            Log.v(LOG_TAG, "Got own local address from tracker, throwing it away...");
	                        }
	                    }*/

	                    torrent_.addPeer(address, port, null);
	                }
					transactionId_ = null;
					return ERROR_NONE;
						
				case ACTION_ID_SCRAPE:
					return ERROR_WRONG_CONTENT;
					
				case ACTION_ID_ERROR:
					return ERROR_WRONG_CONTENT;
			}
			
		}
		
		return ERROR_WRONG_CONTENT;
	}
	
	@Override
	protected void doConnect() {
		byte[] response;
		if (connectionId_ == null) {
			response = connecting();
			if (response != null) {
				if (processResponse(response) != ERROR_NONE) {
					status_ = STATUS_FAILED;
					interval_ = ERROR_REQUEST_INTERVAL;
					return;
				}
			}
		}
		
		response = announcing();
		
		if (event_ != EVENT_STOPPED) {
			if (response != null) {
				status_ = STATUS_WORKING;
				Log.v(LOG_TAG, "Bencoded response processing...");
				if (processResponse(response) != ERROR_NONE) failCount_++;
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
