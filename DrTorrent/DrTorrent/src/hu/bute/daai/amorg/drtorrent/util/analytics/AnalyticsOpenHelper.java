package hu.bute.daai.amorg.drtorrent.util.analytics;

import hu.bute.daai.amorg.drtorrent.core.torrent.TorrentInfo;
import hu.bute.daai.amorg.drtorrent.util.analytics.Analytics.ClientInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AnalyticsOpenHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 3;
	private static final String DATABASE_NAME = "DrTorrentAnalyticsDB";

	AnalyticsOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(
			"CREATE TABLE Torrent (Id INTEGER PRIMARY KEY, InfoHash TEXT, " +
								  "AddedOn INTEGER, " +
								  "Size INTEGER, Pieces INTEGER, " +
								  "GoodPieces INTEGER, BadPieces INTEGER, " +
								  "FailedConnections INTEGER, TcpConnections INTEGER, Handshakes INTEGER, " +
								  "Downloaded INTEGER DEFAULT -1, " +
								  "Completed INTEGER DEFAULT -1, " +
					        	  "Uploaded INTEGER DEFAULT -1, " +
					        	  "DownloadingTime INTEGER DEFAULT -1, " +
					        	  "SeedingTime INTEGER DEFAULT -1, " +
					        	  "CompletedOn INTEGER DEFAULT -1, " +
					        	  "RemovedOn INTEGER DEFAULT -1);"	
		);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch(oldVersion) {
            case 1:
            	db.execSQL("ALTER TABLE Torrent ADD Downloaded INTEGER DEFAULT -1");
            	db.execSQL("ALTER TABLE Torrent ADD Completed INTEGER DEFAULT -1");
            	db.execSQL("ALTER TABLE Torrent ADD Uploaded INTEGER DEFAULT -1");
            	db.execSQL("ALTER TABLE Torrent ADD DownloadingTime INTEGER DEFAULT -1");
            	db.execSQL("ALTER TABLE Torrent ADD SeedingTime INTEGER DEFAULT -1");
            	db.execSQL("ALTER TABLE Torrent ADD CompletedOn INTEGER DEFAULT -1");
            	db.execSQL("ALTER TABLE Torrent ADD RemovedOn INTEGER DEFAULT -1");
            	
            case 2:
            	db.execSQL(
        			"CREATE TABLE NetworkInfo (Id INTEGER PRIMARY KEY, " + 
        									  "FromTicks INTEGER, " +
        									  "ToTicks INTEGER, " +
        									  "Type INTEGER)");
            	db.execSQL(
        			"CREATE TABLE PowerInfo (Id INTEGER PRIMARY KEY, " + 
        									"FromTicks INTEGER, " +
        									"ToTicks INTEGER, " +
        									"IsPlugged INTEGER)");
            	
            default:
        }
	}

	/** Inserts a new torrent to the Database. */
	public synchronized void insertTorrent(final TorrentInfo torrent) {
		if (!isTorrentExists(torrent)) {
			final String infoHash = torrent.getInfoHashString();
			final long addedOn = torrent.getAddedOn();
			final long size = torrent.getFullSize();
			final int pieceCount = torrent.pieceCount();
			
			ContentValues values = new ContentValues();
			values.put("InfoHash", infoHash);
			values.put("AddedOn", addedOn);
			values.put("Size", size);
			values.put("Pieces", pieceCount);
			values.put("GoodPieces", 0);
			values.put("BadPieces", 0);
			values.put("FailedConnections", 0);
			values.put("TcpConnections", 0);
			values.put("Handshakes", 0);
			
			SQLiteDatabase db = null;
			try {
				db = this.getWritableDatabase();
				db.insert("Torrent", null, values);
			} catch (Exception e) {
			}
			try {
				db.close();
			} catch (Exception e) {}
		}
	}
	
	/** Removes torrent with the given infoHash. */
	public synchronized void removeTorrent(final String infoHash) {
		
		SQLiteDatabase db = null;
		try {
			db = this.getWritableDatabase();
			db.delete("Torrent", "InfoHash = ?", new String[] { infoHash });
		} catch (Exception e) {
		}
		try {
			db.close();
		} catch (Exception e) {}
	}
	
	/** Selects torrent with the given infoHash. */
	private synchronized boolean isTorrentExists(final TorrentInfo torrent) {
		final String infoHash = torrent.getInfoHashString();
		final long addedOn = torrent.getAddedOn();
		
		boolean result = false;
		SQLiteDatabase db = null;
		Cursor cursor = null;
		try {
			db = this.getReadableDatabase();
			
			cursor = db.query("Torrent", null, "InfoHash = ? AND AddedOn = ?", new String[] { infoHash, Long.toString(addedOn) }, null, null, null);
			if (cursor.moveToFirst()) {
				result = true;
			}
		} catch (Exception e) {
		}
		try {
			cursor.close();
		} catch (Exception e) {}
		try {
			db.close();
		} catch (Exception e) {}
		
		return result;
	}
	
	/** Updates the size and the pieces count of the torrent. */
	private synchronized void updateTorrent(final TorrentInfo torrent) {
		final String infoHash = torrent.getInfoHashString();
		final long addedOn = torrent.getAddedOn();
		final long size = torrent.getFullSize();
		final int pieceCount = torrent.pieceCount();
		
		ContentValues values = new ContentValues();
		values.put("Size", size);
		values.put("Pieces", pieceCount);
		
		SQLiteDatabase db = null;
		try {
			db = this.getWritableDatabase();
			db.update("Torrent", values, "InfoHash = ? AND AddedOn = ?", new String[] { infoHash, Long.toString(addedOn) });
		} catch (Exception e) {
		}
		
		try {
			db.close();
		} catch (Exception e) {}
	}
	
	/** Updates the completed/downloaded/uploaded size and the downloading/seeding time of the torrent. */
	private synchronized void updateTorrentSizeAndTime(final TorrentInfo torrent) {
		final String infoHash = torrent.getInfoHashString();
		final long addedOn = torrent.getAddedOn();
		
		final long downloaded = torrent.getBytesDownloaded();
		final long completed = torrent.getCompletedFilesSize();
		final long uploaded = torrent.getBytesUploaded();
		final long downloadingTime = torrent.getDownloadingTime();
		final long seedingTime = torrent.getSeedingTime();
		
		ContentValues values = new ContentValues();
		values.put("Downloaded", downloaded);
		values.put("Completed", completed);
		values.put("Uploaded", uploaded);
		values.put("DownloadingTime", downloadingTime);
		values.put("SeedingTime", seedingTime);
		
		SQLiteDatabase db = null;
		try {
			db = this.getWritableDatabase();
			db.update("Torrent", values, "InfoHash = ? AND AddedOn = ?", new String[] { infoHash, Long.toString(addedOn) });
		} catch (Exception e) {
		}
		try {
			db.close();
		} catch (Exception e) {}
	}
	
	/** Updates an optional column of a torrent. */
	private synchronized void updateTorrentColumn(final TorrentInfo torrent, final String column, final long value) {
		final String infoHash = torrent.getInfoHashString();
		final long addedOn = torrent.getAddedOn();
		
		ContentValues values = new ContentValues();
		values.put(column, value);

		SQLiteDatabase db = null;
		try {
			db = this.getWritableDatabase();
			db.update("Torrent", values, "InfoHash = ? AND AddedOn = ?", new String[] { infoHash, Long.toString(addedOn) });
		} catch (Exception e) {
		}
		try {
			db.close();
		} catch (Exception e) {}
	}
	
	/** Inserts a new network info to the Database. */
	private synchronized void insertNetworkInfo(final ClientInfo clientInfo) {
		ContentValues values = new ContentValues();
		values.put("FromTicks", clientInfo.from);
		values.put("ToTicks", clientInfo.to);
		values.put("Type", clientInfo.networkType);
		
		SQLiteDatabase db = null;
		try {
			db = this.getWritableDatabase();
			db.insert("NetworkInfo", null, values);
		} catch (Exception e) {
		}
		try {
			db.close();
		} catch (Exception e) {}
	}
	
	/** Updates a network info. */
	private synchronized void updateNetworkInfo(final ClientInfo clientInfo) {
		ContentValues values = new ContentValues();
		values.put("ToTicks", clientInfo.to);

		SQLiteDatabase db = null;
		try {
			db = this.getWritableDatabase();
			db.update("NetworkInfo", values, "FromTicks = ? AND Type = ?", new String[] { Long.toString(clientInfo.from), Long.toString(clientInfo.networkType) });
		} catch (Exception e) {
		}
		try {
			db.close();
		} catch (Exception e) {}
	}
	
	public synchronized void removeNetworkInfo(long from, int networkType) {
		SQLiteDatabase db = null;
		try {
			db = this.getWritableDatabase();
			db.delete("NetworkInfo", "FromTicks < ? AND Type = ?", new String[] { Long.toString(from), Long.toString(networkType) });
		} catch (Exception e) {
		}
		try {
			db.close();
		} catch (Exception e) {}
	}
	
	/** Inserts a new network info to the Database. */
	private synchronized void insertPowerInfo(final ClientInfo clientInfo) {
		ContentValues values = new ContentValues();
		values.put("FromTicks", clientInfo.from);
		values.put("ToTicks", clientInfo.to);
		values.put("IsPlugged", clientInfo.isChargerPlugged ? 1 : 0);
		
		SQLiteDatabase db = null;
		try {
			db = this.getWritableDatabase();
			db.insert("PowerInfo", null, values);
		} catch (Exception e) {
		}
		try {
			db.close();
		} catch (Exception e) {}
	}
	
	/** Updates a network info. */
	private synchronized void updatePowerInfo(final ClientInfo clientInfo) {
		ContentValues values = new ContentValues();
		values.put("ToTicks", clientInfo.to);

		SQLiteDatabase db = null;
		try {
			db = this.getWritableDatabase();
			db.update("PowerInfo", values, "FromTicks = ? AND IsPlugged = ?", new String[] { Long.toString(clientInfo.from), Integer.toString(clientInfo.isChargerPlugged ? 1 : 0) });
		} catch (Exception e) {
		}
		try {
			db.close();
		} catch (Exception e) {}
	}

	public synchronized void removePowerInfo(long from) {
		SQLiteDatabase db = null;
		try {
			db = this.getWritableDatabase();
			db.delete("PowerInfo", "FromTicks < ?", new String[] { Long.toString(from) });
		} catch (Exception e) {
		}
		try {
			db.close();
		} catch (Exception e) {}
	}
	
	public final static int TORRENT 		  = 1;
	public final static int TORRENT_UPDATE    = 2;
	public final static int BAD_PIECE 		  = 3;
	public final static int GOOD_PIECE 		  = 4;
	public final static int FAILED_CONNECTION = 5;
	public final static int TCP_CONNECTION 	  = 6;
	public final static int HANDSHAKE 		  = 7;
	public final static int TORRENT_SIZE_AND_TIME_UPDATE = 8;
	public final static int COMPLETED_ON	  = 9;
	public final static int REMOVED_ON	  	  = 10;
	public final static int NEW_NETWORK_CONNECTION = 11;
	public final static int SET_NETWORK_CONNECTION = 12;
	public final static int NEW_POWER_CONNECTION   = 13;
	public final static int SET_POWER_CONNECTION   = 14;
	public final static int REMOVE_TORRENT	  = 666;
	
	/** Increments the value of an optional column. */
	public synchronized void doOperation(final TorrentInfo torrent, final ClientInfo clientInfo, final int operationId) {
		String column = null;
		switch (operationId) {
			case TORRENT:
				insertTorrent(torrent);
				return;
			
			case TORRENT_UPDATE:
				updateTorrent(torrent);
				return;
				
			case TORRENT_SIZE_AND_TIME_UPDATE:
				updateTorrentSizeAndTime(torrent);
				return;
		
			case BAD_PIECE:
				column = "BadPieces";
				break;
				
			case GOOD_PIECE:
				column = "GoodPieces";
				break;
				
			case FAILED_CONNECTION:
				column = "FailedConnections";
				break;
				
			case TCP_CONNECTION:
				column = "TcpConnections";
				break;
				
			case HANDSHAKE:
				column = "Handshakes";
				break;
				
			case COMPLETED_ON:
				column = "CompletedOn";
				updateTorrentColumn(torrent, column, torrent.getCompletedOn());
				return;
	
			case REMOVED_ON:
				column = "RemovedOn";
				updateTorrentColumn(torrent, column, torrent.getRemovedOn());
				return;
				
			case REMOVE_TORRENT:
				removeTorrent(torrent.getInfoHashString());
				return;
			
			case NEW_NETWORK_CONNECTION:
				insertNetworkInfo(clientInfo);
				return;
				
			case SET_NETWORK_CONNECTION:
				updateNetworkInfo(clientInfo);
				return;
				
			case NEW_POWER_CONNECTION:
				insertPowerInfo(clientInfo);
				return;
				
			case SET_POWER_CONNECTION:
				updatePowerInfo(clientInfo);
				return;
				
			default:
				break;
		}
		
		if (column != null) {
			SQLiteDatabase db = null;
			try {
				// Log.v("DB", "Inc " + column);
				
				final String infoHash = torrent.getInfoHashString();
				final long addedOn = torrent.getAddedOn();
				
				db = this.getWritableDatabase();
				db.execSQL("UPDATE Torrent SET " + column + " = " + column + " + 1 WHERE InfoHash = ? AND AddedOn = ?", new String [] { infoHash, Long.toString(addedOn) });
			} catch (Exception e) {
				// Log.v("DB", "Inc error");
			}
			try {
				db.close();
			} catch (Exception e) {}
		}
	}
	
	/** Returns the torrents in a JSONArray. */
	public synchronized JSONArray getTorentsInJSON() {
		JSONArray torrents = new JSONArray();
		SQLiteDatabase db = null;
		Cursor cursor = null;
		try {
			db = this.getReadableDatabase();
			cursor = db.query("Torrent", null, null, null, null, null, null);
			
			JSONObject torrent;
			if (cursor.moveToFirst()) {
				do {
					try {
						if (cursor.getInt(2) != 0 && cursor.getInt(3) != 0) {
							torrent = new JSONObject();
							torrent.put("infoHash", cursor.getString(1));
							torrent.put("addedOn", cursor.getLong(2));
							torrent.put("size", cursor.getLong(3));
							torrent.put("pieces", cursor.getInt(4));
							torrent.put("goodPieces", cursor.getInt(5));
							torrent.put("badPieces", cursor.getInt(6));
							torrent.put("failedConnections", cursor.getInt(7));
							torrent.put("tcpConnections", cursor.getInt(8));
							torrent.put("handshakes", cursor.getInt(9));
							
							torrent.put("downloaded", cursor.getLong(10));
							torrent.put("completed", cursor.getLong(11));
							torrent.put("uploaded", cursor.getLong(12));
							torrent.put("downloadingTime", cursor.getLong(13));
							torrent.put("seedingTime", cursor.getLong(14));
							torrent.put("completedOn", cursor.getLong(15));
							torrent.put("removedOn", cursor.getLong(16));
							
							torrents.put(torrent);
						}
					} catch (Exception e) {
						//Log.v("DB", e.toString());
					}
				} while (cursor.moveToNext());
			}

		} catch (Exception e) {
			//Log.v("DB2", e.toString());
		}
		
		try {
			cursor.close();
		} catch (Exception e) {}
		try {
			db.close();
		} catch (Exception e) {}
		
		return torrents;
	}
	
	/** Returns the torrents in a JSONArray. */
	public synchronized JSONArray getNetworkInfoInJSON() {
		JSONArray networkInfoArray = new JSONArray();
		SQLiteDatabase db = null;
		Cursor cursor = null;
		try {
			db = this.getReadableDatabase();
			cursor = db.query("NetworkInfo", null, null, null, null, null, null);
			
			JSONObject networkInfo;
			if (cursor.moveToFirst()) {
				do {
					try {
						networkInfo = new JSONObject();
						networkInfo.put("from", cursor.getLong(1));
						networkInfo.put("to", cursor.getLong(2));
						networkInfo.put("type", cursor.getInt(3));
							
						networkInfoArray.put(networkInfo);
					} catch (Exception e) {
						//Log.v("DB", e.toString());
					}
				} while (cursor.moveToNext());
			}

		} catch (Exception e) {
			//Log.v("DB2", e.toString());
		}
		
		try {
			cursor.close();
		} catch (Exception e) {}
		try {
			db.close();
		} catch (Exception e) {}
		
		return networkInfoArray;
	}
	
	/** Returns the torrents in a JSONArray. */
	public synchronized JSONArray getPowerInfoInJSON() {
		JSONArray powerInfoArray = new JSONArray();
		SQLiteDatabase db = null;
		Cursor cursor = null;
		try {
			db = this.getReadableDatabase();
			cursor = db.query("PowerInfo", null, null, null, null, null, null);
			
			JSONObject powerInfo;
			if (cursor.moveToFirst()) {
				do {
					try {
						powerInfo = new JSONObject();
						powerInfo.put("from", cursor.getLong(1));
						powerInfo.put("to", cursor.getLong(2));
						powerInfo.put("isPlugged", (cursor.getInt(3) != 0));
							
						powerInfoArray.put(powerInfo);
					} catch (Exception e) {
						//Log.v("DB", e.toString());
					}
				} while (cursor.moveToNext());
			}

		} catch (Exception e) {
			//Log.v("DB2", e.toString());
		}
		
		try {
			cursor.close();
		} catch (Exception e) {}
		try {
			db.close();
		} catch (Exception e) {}
		
		return powerInfoArray;
	}
}
