package hu.bute.daai.amorg.drtorrent.analytics;

import hu.bute.daai.amorg.drtorrent.torrentengine.Torrent;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AnalyticsOpenHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 2;
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
            	
            default:
        }
	}

	/** Inserts a new torrent to the Database. */
	public synchronized void insertTorrent(final Torrent torrent) {
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
	private synchronized boolean isTorrentExists(final Torrent torrent) {
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
	private synchronized void updateTorrent(final Torrent torrent) {
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
	private synchronized void updateTorrentSizeAndTime(final Torrent torrent) {
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
	private synchronized void updateTorrentColumn(final Torrent torrent, final String column, final long value) {
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
	public final static int REMOVE_TORRENT	  = 666;
	
	/** Increments the value of an optional column. */
	public synchronized void doOperation(final Torrent torrent, final int operationId) {
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
}
