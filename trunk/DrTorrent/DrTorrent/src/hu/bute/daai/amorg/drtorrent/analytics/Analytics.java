package hu.bute.daai.amorg.drtorrent.analytics;

import hu.bute.daai.amorg.drtorrent.Preferences;
import hu.bute.daai.amorg.drtorrent.network.HttpPostConnection;
import hu.bute.daai.amorg.drtorrent.torrentengine.Torrent;

import java.util.ArrayList;
import java.util.Vector;

import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

public class Analytics {
	
	private final static String SERVER_URL = "http://drtorrent-andrewbori.rhcloud.com/DrTorrent/Analytics";
	//private final static String SERVER_URL = "http://192.168.0.4:8080/DrTorrent/Analytics";
	
	private static AnalyticsOpenHelper db_;
	private final static Vector<DatabaseOperation> operations_ = new Vector<DatabaseOperation>();
	private static AnalyticsThread analyticsThread_ = null;
	
	
	public static void init(Context context) {
		db_ = new AnalyticsOpenHelper(context);	
	}
	
	public static void shutDown() {
		if (analyticsThread_ != null) {
			analyticsThread_.disable();
			analyticsThread_ = null;
		}
		db_ = null;
	}
	
	public static void newTorrent(Torrent torrent) {
		operations_.addElement(new DatabaseOperation(torrent, AnalyticsOpenHelper.TORRENT));
		startAnalyticsThread();
	}
	
	public static void changeTorrent(Torrent torrent) {
		operations_.addElement(new DatabaseOperation(torrent, AnalyticsOpenHelper.TORRENT_UPDATE));
		startAnalyticsThread();
	}
	
	public static void newBadPiece(Torrent torrent) {
		operations_.addElement(new DatabaseOperation(torrent, AnalyticsOpenHelper.BAD_PIECE));
		startAnalyticsThread();
	}
	
	public static void newGoodPiece(Torrent torrent) {
		operations_.addElement(new DatabaseOperation(torrent, AnalyticsOpenHelper.GOOD_PIECE));
		startAnalyticsThread();
	}

	public static void newFailedConnection(Torrent torrent) {
		operations_.addElement(new DatabaseOperation(torrent, AnalyticsOpenHelper.FAILED_CONNECTION));
		startAnalyticsThread();
	}
	
	public static void newTcpConnection(Torrent torrent) {
		operations_.addElement(new DatabaseOperation(torrent, AnalyticsOpenHelper.TCP_CONNECTION));
		startAnalyticsThread();
	}
	
	public static void newHandshake(Torrent torrent) {
		operations_.addElement(new DatabaseOperation(torrent, AnalyticsOpenHelper.HANDSHAKE));
		startAnalyticsThread();
	}
	
	private static void startAnalyticsThread() {
		if (analyticsThread_ == null) {
			analyticsThread_ = new AnalyticsThread();
			analyticsThread_.start();
		}
		synchronized (operations_) {
			operations_.notify();
		}
	}
	
	private static class AnalyticsThread extends Thread {
		private boolean isEnabled_ = true;
		 
		@Override
		public void run() {
			while (isEnabled_) {
				while (!operations_.isEmpty() && isEnabled_) {
					DatabaseOperation operation = operations_.get(0); 
					operations_.removeElement(operation);
					
					if (db_ != null && isEnabled_) {
						db_.doOperation(operation.torrent, operation.operationId);
					}
				}
				if (isEnabled_) {
					synchronized (operations_) {
						try {
							operations_.wait();
						} catch (Exception e) {
						}
					}
				}
			}
		}
		
		public void disable() {
			isEnabled_ = false;
			synchronized (operations_) {
				operations_.notify();
			}
		}
	}
	
	public static void onTimer(ArrayList<Torrent> openedTorrents) {
		(new ReportThread(openedTorrents)).start();
	}
	
	private static class ReportThread extends Thread {
		ArrayList<Torrent> openedTorrents_ = null;
		
		public ReportThread(ArrayList<Torrent> openedTorrents) {
			openedTorrents_ = openedTorrents;
		}
		
		@Override
		public void run() {
			try {
				JSONObject json = new JSONObject();
				
				String clientIdentifier = Preferences.getClientIdentifier();
				JSONArray torrents = db_.getTorentsInJSON();
				
				if (torrents.length() > 0) {
					
						json.put("clientIdentifier", clientIdentifier);
						json.put("torrents", torrents);
						
						String message = json.toString();
						Log.v("Analytics", "Sending report");
						
						StringEntity entity = new StringEntity(message);
						HttpPostConnection connection = new HttpPostConnection(SERVER_URL, entity);
						byte[] response = connection.execute();
						if (response != null) {
							String rStr = new String(response);
							Log.v("Analytics", "Response: " + rStr);
							
							if (rStr.equals("200")) {
								for (int i = 0; i < torrents.length(); i++) {
									JSONObject torrent = torrents.getJSONObject(i);
									String infoHash = torrent.getString("infoHash");
									
									boolean isFound = false;
									for (int j = 0; j < openedTorrents_.size(); j++) {
										String iHash = openedTorrents_.get(j).getInfoHashString();
										if (iHash != null && iHash.equals(infoHash)) {
											isFound = true;
											openedTorrents_.remove(j);
											break;
										}
									}
									
									if (!isFound) {
										db_.removeTorrent(infoHash);
									}
								}
							}
						}
				}
				
			} catch (Exception e) {
			}
		}
	}
	
	/** Class for representing the db operations. */
	private static class DatabaseOperation {
		Torrent torrent;
		int operationId; 
		
		DatabaseOperation(Torrent torrent, int operationId) {
			this.torrent = torrent;
			this.operationId = operationId;
		}
	}
}