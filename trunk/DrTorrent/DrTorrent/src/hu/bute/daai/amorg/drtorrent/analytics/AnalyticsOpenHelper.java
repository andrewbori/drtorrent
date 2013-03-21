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

	private static final int DATABASE_VERSION = 1;
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
								  "FailedConnections INTEGER, TcpConnections INTEGER, Handshakes INTEGER);"	
		);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		
	}

	/** Inserts a new torrent to the Database. */
	public void insertTorrent(final Torrent torrent) {
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
			
			try {
				SQLiteDatabase db = this.getWritableDatabase();
				db.insert("Torrent", null, values);
				db.close();
			} catch (Exception e) {
			}
		}
	}
	
	/** Removes torrent with the given infoHash. */
	public void removeTorrent(final String infoHash) {
		try {
			SQLiteDatabase db = this.getWritableDatabase();
			db.delete("Torrent", "InfoHash = ?", new String[] { infoHash });
			db.close();
		} catch (Exception e) {
		}
	}
	
	/** Selects torrent with the given infoHash. */
	public boolean isTorrentExists(final Torrent torrent) {
		final String infoHash = torrent.getInfoHashString();
		final long addedOn = torrent.getAddedOn();
		
		boolean result = false;
		try {
			SQLiteDatabase db = this.getReadableDatabase();
			
			Cursor cursor = db.query("Torrent", null, "InfoHash = ? AND AddedOn = ?", new String[] { infoHash, Long.toString(addedOn) }, null, null, null);
			if (cursor.moveToFirst()) {
				result = true;
			}
			cursor.close();
			db.close();
		} catch (Exception e) {
		}
		
		return result;
	}
	
	/** Updates the size and the pieces count of the torrent. */
	public void updateTorrent(final Torrent torrent) {
		final String infoHash = torrent.getInfoHashString();
		final long addedOn = torrent.getAddedOn();
		final long size = torrent.getFullSize();
		final int pieceCount = torrent.pieceCount();
		
		ContentValues values = new ContentValues();
		values.put("Size", size);
		values.put("Pieces", pieceCount);
		
		try {
			SQLiteDatabase db = this.getWritableDatabase();
			db.update("Torrent", values, "InfoHash = ? AND AddedOn = ?", new String[] { infoHash, Long.toString(addedOn) });
			db.close();
		} catch (Exception e) {
		}
	}
	
	public final static int TORRENT 		  = 1;
	public final static int TORRENT_UPDATE    = 2;
	public final static int BAD_PIECE 		  = 3;
	public final static int GOOD_PIECE 		  = 4;
	public final static int FAILED_CONNECTION = 5;
	public final static int TCP_CONNECTION 	  = 6;
	public final static int HANDSHAKE 		  = 7;
	
	/** Increments the value of an optional column. */
	public void doOperation(final Torrent torrent, final int operationId) {
		String column = null;
		switch (operationId) {
			case TORRENT:
				insertTorrent(torrent);
				return;
			
			case TORRENT_UPDATE:
				updateTorrent(torrent);
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
	
			default:
				break;
		}
		
		if (column != null) {
			try {
				// Log.v("DB", "Inc " + column);
				
				final String infoHash = torrent.getInfoHashString();
				final long addedOn = torrent.getAddedOn();
				
				SQLiteDatabase db = this.getWritableDatabase();
				db.execSQL("UPDATE Torrent SET " + column + " = " + column + " + 1 WHERE InfoHash = ? AND AddedOn = ?", new String [] { infoHash, Long.toString(addedOn) });
				db.close();
			} catch (Exception e) {
				// Log.v("DB", "Inc error");
			}
		}
	}
	
	/** Returns the torrents in a JSONArray. */
	public JSONArray getTorentsInJSON() {
		JSONArray torrents = new JSONArray();
		try {
			SQLiteDatabase db = this.getReadableDatabase();
			Cursor cursor = db.query("Torrent", null, null, null, null, null, null);
			
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
							
							torrents.put(torrent);
						}
					} catch (Exception e) {
					}
				} while (cursor.moveToNext());
			}
			if (!cursor.isClosed()) {
				cursor.close();
			}
			db.close();
		} catch (Exception e) {
		}
		
		return torrents;
	}
}
