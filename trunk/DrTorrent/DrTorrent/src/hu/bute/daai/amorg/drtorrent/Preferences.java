package hu.bute.daai.amorg.drtorrent;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

public class Preferences {
	
	public static final String MY_AD_UNIT_ID = "a150b0b5a8dc405";
	
	private static SharedPreferences preferences_ = null;
	
	private Preferences() {}
	
	public static void set(Context context) {
		preferences_ = PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	/** Sets the download folder. */
	public static void setDownloadFolder(String path) {
		SharedPreferences.Editor editor = preferences_.edit();
		editor.putString("download_folder", path);
		editor.commit();
	}

	/** Returns the download folder. */
	public static String getDownloadFolder() {
		return preferences_.getString("download_folder", Environment.getExternalStorageDirectory().getPath() + "/download");
	}
	
	/** Returns whether the streaming is enabled or not. */
	public static boolean isStreamingMode() {
		return preferences_.getBoolean("streaming", false);
	}
	
	/** Returns whether the download is enabled only over WiFi or not. */
	public static boolean isWiFiOnly() {
		return preferences_.getBoolean("wifi_only", true);
	}
	
	/** Returns whether incoming connections are enabled or not. */
	public static boolean isIncomingConnectionsEnabled() {
		return preferences_.getBoolean("incoming_connections", true);
	}
	
	/** Returns whether upload is enabled. */
	public static boolean isUploadEnabled() {
		return preferences_.getBoolean("upload", true);
	}
	
	/** Returns the P2P port. */
	public static int getPort() {
		try {
			return Integer.valueOf(preferences_.getString("port", "6886"));
		} catch (Exception e) {
			return 6886;
		}
	}
	
	/** Returns the number of the maximum connected peers per torrent. */
	public static int getMaxConnectedPeers() {
		try {
			return Integer.valueOf(preferences_.getString("connections", "10"));
		} catch (Exception e) {
			return 10;
		}
	}
	
	/** Returns the download speed limit. */
	/*public static int getDownloadSpeedLimit() {
		try {
			return Integer.valueOf(preferences_.getString("download_speed",  "0"));
		} catch (Exception e) {
			return 0;
		}
	}*/
	
	/** Returns the upload speed limit. */
	/*public static int getUploadSpeedLimit() {
		try {
			return Integer.valueOf(preferences_.getString("upload_speed",  "0"));
		} catch (Exception e) {
			return 0;
		}
	}*/

	/** Returns the siteCode of the search engine. */
	public static String getSearchEngine(Context context) {
		try {
			return preferences_.getString("search_engine", "KickassTorents");
		} catch (NullPointerException e) {
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			return preferences.getString("search_engine", "KickassTorents");
		}
	}
	
	/** Returns the url of the search engine. */
	public static String getSearchEngineUrl(Context context) {
		String searchEngine;
		try {
			searchEngine = preferences_.getString("search_engine", "KickassTorents");
		} catch (NullPointerException e) {
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			searchEngine = preferences.getString("search_engine", "KickassTorents");
		}
		
		if (searchEngine.equals("BitSnoop")) {
			return "http://bitsnoop.com/search/all/%1$s";
			
		} else if (searchEngine.equals("ExtraTorrent")) {
			return "http://extratorrent.com/search/?search=%1$s";
			
		} else if (searchEngine.equals("Isohunt")) {
			return "http://isohunt.com/torrents/?ihq=%1$s";
			
		} else if (searchEngine.equals("KickassTorents")) {
			return "http://kat.ph/usearch/%1$s/";
			
		} else if (searchEngine.equals("LimeTorrents")) {
			return "http://www.limetorrents.com/search/all/%1$s/";
			
		} else if (searchEngine.equals("Mininova")) {
			return "http://www.mininova.org/search/?search=%1$s";
			
		} else if (searchEngine.equals("Monova")) {
			return "http://www.monova.org/search.php?term=%1$s";
			
		} else if (searchEngine.equals("ThePirateBay")) {
			return "http://thepiratebay.se/search/%1$s";
					
		} else if (searchEngine.equals("TorrentDownloads")) {
			return "http://www.torrentdownloads.me/search/?search=%1$s";
					
		} else if (searchEngine.equals("TorrentReactor")) {
			return "http://www.torrentreactor.net/torrent-search/%1$s";					
		}
		
		return "http://www.vertor.com/index.php?mod=search&search=&cid=0&words=%1$s";
	}
	
	/** Sets the number of the latest version. */
	public static void setLatestVersion(int latestVersion) {
		SharedPreferences.Editor editor = preferences_.edit();
		editor.putInt("latest_version", latestVersion);
		editor.commit();
	}
	
	/** Returns the number of the latest version. */
	public static int getLatestVersion() {
		return preferences_.getInt("latest_version", 0);
	}
}
