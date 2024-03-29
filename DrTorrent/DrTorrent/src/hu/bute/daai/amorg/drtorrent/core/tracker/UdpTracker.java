package hu.bute.daai.amorg.drtorrent.core.tracker;

import hu.bute.daai.amorg.drtorrent.core.exception.DrTorrentException;
import hu.bute.daai.amorg.drtorrent.core.torrent.TorrentManager;
import hu.bute.daai.amorg.drtorrent.network.UdpConnection;
import hu.bute.daai.amorg.drtorrent.util.Log;
import hu.bute.daai.amorg.drtorrent.util.Preferences;
import hu.bute.daai.amorg.drtorrent.util.Tools;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Random;

/** Class that represents UDP trackers. */
public class UdpTracker extends Tracker {
	private static final String LOG_TAG = "TrackerUdp";
	
	private final static int ACTION_ID_CONNECT	= 0;
	private final static int ACTION_ID_ANNOUNCE = 1;
	private final static int ACTION_ID_SCRAPE	= 2;
	private final static int ACTION_ID_ERROR 	= 3;
	
	private String host_ = null;
	private int port_ = -1;
	
	private byte[] connectionId_ = null;	// length: 4 (64 bit)
	private byte[] transactionId_ = null;	// length: 4 (32 bit)
	
	/** Constructor of the tracker with its URL and running torrent. */
	public UdpTracker(String url, TrackerObserver torrent) {
		super(url, torrent);
		
		try {
			URI uri = URI.create(url);
			host_ = uri.getHost();
			port_ = uri.getPort();
			if (port_ == -1) {
				port_ = 80;
			}
		} catch (Exception e) {
			host_ = null;
			Log.v(LOG_TAG, "Invalid path: " + e.getMessage());
		}
	}
	
	/** Sends a connection request to the tracker and reads its response. */
	private byte[] connecting() {
		if (host_ == null) {
			return null;
		}
		
		if (transactionId_ == null) {
			transactionId_ = new byte[4];
			Random r = new Random();
			r.nextBytes(transactionId_);
		}
		
		final byte[] message = {
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
		if (host_ == null) {
			return null;
		}
		
		if (transactionId_ == null) {
			transactionId_ = new byte[4];
			Random r = new Random();
			r.nextBytes(transactionId_);
		}
		
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
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
		
			UdpConnection udpConnection = new UdpConnection(host_, port_, baos.toByteArray(), 320);		// Response buffer for max 50 peers
			return udpConnection.execute();
		} catch (Exception e) {
			return null;
		} finally {
			try {
				if (baos != null) {
					baos.close();
				}
			} catch (Exception e) {}
		}
	}
	
	/** Processes the response of the tracker */
	public void processResponse(final byte[] response) throws DrTorrentException {
		if (response == null || response.length < 8) {
			throw new DrTorrentException("Transaction ID cannot be found. The length of the response is less than 8.");
		}
		
		if ((response[0] == 0 && response[1] == 0 && response[2] == 0 && (response[3] >= 0 && response[3] <= 3)) &&
			(response[4] == transactionId_[0] && response[5] == transactionId_[1] &&
			 response[6] == transactionId_[2] && response[7] == transactionId_[3])) {
		
			switch (response[3]) {
				case ACTION_ID_CONNECT:
					if (response.length < 16) {
						throw new DrTorrentException("Connection ID cannot be found. The length of the response is less than 16. ");
					}
					
					connectionId_ = new byte[8];
					for (int i = 0; i < 8; i++) {
						connectionId_[i] = response[8 + i];
					}
					transactionId_ = null;
					return;
					
				case ACTION_ID_ANNOUNCE:
					if (response.length < 20) {
						throw new DrTorrentException("");
					}
					
					interval_ = Tools.readInt32(response, 8);
					incomplete_ = Tools.readInt32(response, 12);
					complete_ = Tools.readInt32(response, 16);
					
					for (int pos = 20; pos + 6 <= response.length; pos += 6) {
	                    String address = Tools.readIp(response, pos);
	                    if (address == null) {
	                    	transactionId_ = null;
	                    	return;
	                    }
	                    int port = Tools.readInt16(response, pos + 4);

	                    Log.v(LOG_TAG, address + ":" + port);
	                    /*if (Preferences.LocalAddress != null) {
	                        if (Preferences.LocalAddress.equals(addressBuffer) && port==Preferences.IncommingPort) {
	                            Log.v(LOG_TAG, "Got own local address from tracker, throwing it away...");
	                        }
	                    }*/

	                    // Add peer to the torrent
	                    torrent_.addPeer(address, port, null);
	                }
					transactionId_ = null;
					return;
						
				case ACTION_ID_SCRAPE:
					throw new DrTorrentException("Not supported action: scrape");
					
				case ACTION_ID_ERROR:
					throw new DrTorrentException("Not supported action: error");
			}
			
		}
		
		throw new DrTorrentException("Invalid response. Non-supported action or invalid transaction ID.");
	}
	
	/** Connects to the tracker over UDP protocol. */
	@Override
	protected void connect() {
		byte[] response;
		if (connectionId_ == null) {
			response = connecting();
			if (response != null) {
				try {
					processResponse(response);
				} catch (DrTorrentException e) {
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
				try {
					processResponse(response);
				} catch(DrTorrentException e) {
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
