package hu.bute.daai.amorg.drtorrent.util.analytics;

import hu.bute.daai.amorg.drtorrent.core.torrent.TorrentInfo;
import hu.bute.daai.amorg.drtorrent.network.HttpPostConnection;
import hu.bute.daai.amorg.drtorrent.util.Log;
import hu.bute.daai.amorg.drtorrent.util.Preferences;

import java.util.ArrayList;
import java.util.Vector;

import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;

public class Analytics {
	
	private final static String SERVER_URL = "http://drtorrent-andrewbori.rhcloud.com/DrTorrent/AnalyticsV3";
	//private final static String SERVER_URL = "http://192.168.0.3:8084/DrTorrent/AnalyticsV3";
	
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
	
	public static void newTorrent(TorrentInfo torrent) {
		operations_.addElement(new DatabaseOperation(torrent, AnalyticsOpenHelper.TORRENT));
		startAnalyticsThread();
	}
	
	public static void removeTorrent(TorrentInfo torrent) {
		operations_.addElement(new DatabaseOperation(torrent, AnalyticsOpenHelper.REMOVE_TORRENT));
		startAnalyticsThread();
	}
	
	public static void changeTorrent(TorrentInfo torrent) {
		operations_.addElement(new DatabaseOperation(torrent, AnalyticsOpenHelper.TORRENT_UPDATE));
		startAnalyticsThread();
	}
	
	public static void saveSizeAndTimeInfo(TorrentInfo torrent) {
		operations_.addElement(new DatabaseOperation(torrent, AnalyticsOpenHelper.TORRENT_SIZE_AND_TIME_UPDATE));
		startAnalyticsThread();
	}
	
	public static void saveCompletedOn(TorrentInfo torrent) {
		operations_.addElement(new DatabaseOperation(torrent, AnalyticsOpenHelper.COMPLETED_ON));
		startAnalyticsThread();
	}
	
	public static void saveRemovedOn(TorrentInfo torrent) {
		operations_.addElement(new DatabaseOperation(torrent, AnalyticsOpenHelper.REMOVED_ON));
		startAnalyticsThread();
	}
	
	public static void newBadPiece(TorrentInfo torrent) {
		operations_.addElement(new DatabaseOperation(torrent, AnalyticsOpenHelper.BAD_PIECE));
		startAnalyticsThread();
	}
	
	public static void newGoodPiece(TorrentInfo torrent) {
		operations_.addElement(new DatabaseOperation(torrent, AnalyticsOpenHelper.GOOD_PIECE));
		startAnalyticsThread();
	}

	public static void newFailedConnection(TorrentInfo torrent) {
		operations_.addElement(new DatabaseOperation(torrent, AnalyticsOpenHelper.FAILED_CONNECTION));
		startAnalyticsThread();
	}
	
	public static void newTcpConnection(TorrentInfo torrent) {
		operations_.addElement(new DatabaseOperation(torrent, AnalyticsOpenHelper.TCP_CONNECTION));
		startAnalyticsThread();
	}
	
	public static void newHandshake(TorrentInfo torrent) {
		operations_.addElement(new DatabaseOperation(torrent, AnalyticsOpenHelper.HANDSHAKE));
		startAnalyticsThread();
	}
	
	public static void newNetworkConnection(long from, long to, int networkType) {
		ClientInfo clientInfo = new ClientInfo(from, to, networkType);
		operations_.addElement(new DatabaseOperation(clientInfo, AnalyticsOpenHelper.NEW_NETWORK_CONNECTION));
		startAnalyticsThread();
	}
	
	public static void updateNetworkConnection(long from, long to, int networkType) {
		ClientInfo clientInfo = new ClientInfo(from, to, networkType);
		operations_.addElement(new DatabaseOperation(clientInfo, AnalyticsOpenHelper.SET_NETWORK_CONNECTION));
		startAnalyticsThread();
	}
	
	public static void newPowerConnection(long from, long to, boolean isChargerPlugged) {
		ClientInfo clientInfo = new ClientInfo(from, to, isChargerPlugged);
		operations_.addElement(new DatabaseOperation(clientInfo, AnalyticsOpenHelper.NEW_POWER_CONNECTION));
		startAnalyticsThread();
	}
	
	public static void updatePowerConnection(long from, long to, boolean isChargerPlugged) {
		ClientInfo clientInfo = new ClientInfo(from, to, isChargerPlugged);
		operations_.addElement(new DatabaseOperation(clientInfo, AnalyticsOpenHelper.SET_POWER_CONNECTION));
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
				while (isEnabled_) {
					DatabaseOperation operation = null;
					synchronized (operations_) {
						if (!operations_.isEmpty()) {
							operation = operations_.firstElement(); 
							operations_.removeElement(operation);
						} else {
							break;
						}
					}
					
					if (db_ != null && isEnabled_) {
						db_.doOperation(operation.torrent, operation.clientInfo, operation.operationId);
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
	
	public static void onTimer(ArrayList<TorrentInfo> openedTorrents, long[] lastTimestamps) {
		(new ReportThread(openedTorrents, lastTimestamps)).start();
	}
	
	private static class ReportThread extends Thread {
		final ArrayList<TorrentInfo> openedTorrents_;
		final long[] lastTimestamps_;
		
		public ReportThread(ArrayList<TorrentInfo> openedTorrents, long[] lastTimestamps) {
			openedTorrents_ = openedTorrents;
			lastTimestamps_ = lastTimestamps;
		}
		
		@Override
		public void run() {
			try {
				JSONObject json = new JSONObject();
				
				String clientIdentifier = Preferences.getClientIdentifier();
				JSONArray torrents = db_.getTorentsInJSON();
				JSONArray networkInfo = db_.getNetworkInfoInJSON();
				JSONArray powerInfo = db_.getPowerInfoInJSON();
				
				if (torrents.length() > 0) {
					
						json.put("clientIdentifier", clientIdentifier);
						json.put("torrents", torrents);
						json.put("networkInfo", networkInfo);
						json.put("powerInfo", powerInfo);
						
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
								
								db_.removePowerInfo(lastTimestamps_[0]);
								db_.removeNetworkInfo(lastTimestamps_[1], 1);
								db_.removeNetworkInfo(lastTimestamps_[2], 2);
								db_.removeNetworkInfo(lastTimestamps_[3], 3);
								db_.removeNetworkInfo(lastTimestamps_[4], 4);
							}
						}
				}
				
			} catch (Exception e) {
			}
		}
	}
	
	/** Class for representing the db operations. */
	private static class DatabaseOperation {
		TorrentInfo torrent = null;
		ClientInfo clientInfo = null;
		int operationId; 
		
		DatabaseOperation(TorrentInfo torrent, int operationId) {
			this.torrent = torrent;
			this.operationId = operationId;
		}
		
		DatabaseOperation(ClientInfo clientInfo, int operationId) {
			this.clientInfo = clientInfo;
			this.operationId = operationId;
		}
	}
	
	static class ClientInfo {
		long from;
		long to;
		int networkType;
		boolean isChargerPlugged;
		
		ClientInfo(long from, long to, int networkType) {
			this.from = from;
			this.to = to;
			this.networkType = networkType;
		}
		
		ClientInfo(long from, long to, boolean isChargerPlugged) {
			this.from = from;
			this.to = to;
			this.isChargerPlugged = isChargerPlugged;
		}
	}
}
