package hu.bute.daai.amorg.drtorrent.util;

import java.util.Date;
import java.util.Random;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

public class Preferences {
	
	public final static String APP_VERSION_NAME = "1.2.6";	// TODO: Refresh!
	public final static int APP_VERSION_CODE = 10;			// TODO: Refresh!
	
	public static final String MY_AD_UNIT_ID = "a150b0b5a8dc405";
	
	private static final int[] downloadSpeedArray = {
		8 * 1024, 16 * 1024, 24 * 1024, 32 * 1024, 48 * 1024, 64 * 1024, 96 * 1024, 128 * 1024, 192 * 1024, 256 * 1024,
		384 * 1024, 512 * 1024, 768 * 1024, 1024 * 1024, 1536 * 1024, 2048 * 1024, 3072 * 1024, 4096 * 1024, Integer.MAX_VALUE
	};
	
	private static final int[] uploadSpeedArray = {
		2 * 1024, 4 * 1024, 8 * 1024, 16 * 1024, 24 * 1024, 32 * 1024, 48 * 1024, 64 * 1024, 96 * 1024, 128 * 1024,
		192 * 1024, 256 * 1024, 384 * 1024, 512 * 1024, 768 * 1024, 1024 * 1024, 1536 * 1024, 2048 * 1024, Integer.MAX_VALUE
	};
	
	private static boolean isTesting_ = false;
	private static int testPort_ = 6881;
	
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
		return preferences_.getString("download_folder", Environment.getExternalStorageDirectory().getPath() + "/Download");
	}
	
	/** [SDCARD]/Android/data/hu.bute.daai.amorg.drtorrent/files */
	public static String getExternalFilesDir() {
		return Environment.getExternalStorageDirectory().getPath() + "/Android/data/hu.bute.daai.amorg.drtorrent/files";
	}
	
	/** [SDCARD]/Android/data/hu.bute.daai.amorg.drtorrent/cache */
	public static String getExternalCacheDir() {
		return Environment.getExternalStorageDirectory().getPath() + "/Android/data/hu.bute.daai.amorg.drtorrent/cache";
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
	
	/** Returns whether UPnP port mapping is enabled or not. */
	public static boolean isUpnpEnabled() {
		return preferences_.getBoolean("upnp", true);
	}
	
	/** Returns whether upload is enabled. */
	public static boolean isUploadEnabled() {
		return preferences_.getBoolean("upload", true);
	}
	
	/** Sets the P2P port. */
	public static void setPort(int port) {
		if (!isTesting_) {
			SharedPreferences.Editor editor = preferences_.edit();
			editor.putInt("port", port);
			editor.commit();
		} else {
			testPort_ = port;
		}
	}
	
	/** Returns the P2P port. */
	public static int getPort() {
		if (!isTesting_) {
			try {
				return Integer.valueOf(preferences_.getString("port", "6886"));
			} catch (Exception e) {
				return 6886;
			}
		}
		return testPort_;
	}
	
	/** Returns the number of the maximum connected peers per torrent. */
	public static int getMaxConnectedPeers() {
		try {
			return Integer.valueOf(preferences_.getString("connections", "20"));
		} catch (Exception e) {
			return 10;
		}
	}
	
	/** Returns the download speed limit. */
	public static int getDownloadSpeedLimit() {
		try {
			int selected = preferences_.getInt("download_speed",  18);
			return downloadSpeedArray[selected];
		} catch (Exception e) {
			return Integer.MAX_VALUE;
		}
	}
	
	/** Returns the upload speed limit. */
	public static int getUploadSpeedLimit() {
		try {
			int selected = preferences_.getInt("upload_speed",  18);
			return uploadSpeedArray[selected];
		} catch (Exception e) {
			return Integer.MAX_VALUE;
		}
	}

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
		
		} else if (searchEngine.equals("Fenopy")) {
			return "http://fenopy.se/?keyword=%1$s";
			
		} else if (searchEngine.equals("Isohunt")) {
			return "http://isohunt.to/torrents/?ihq=%1$s";
			
		} else if (searchEngine.equals("KickassTorents")) {
			return "http://kickass.to/usearch/%1$s/";
			
		} else if (searchEngine.equals("LimeTorrents")) {
			return "http://www.limetorrents.com/search/all/%1$s/";
			
		} else if (searchEngine.equals("Mininova")) {
			return "http://www.mininova.org/search/?search=%1$s";
			
		} else if (searchEngine.equals("Monova")) {
			return "http://www.monova.org/search.php?term=%1$s";
			
		} else if (searchEngine.equals("ThePirateBay")) {
			return "http://thepiratebay.se/search/%1$s";
			
		} else if (searchEngine.equals("ThePirateBayMirror")) {
			return "http://pirateproxy.net/search/%1$s";
			
		} else if (searchEngine.equals("TorrentDownloads")) {
			return "http://www.torrentdownloads.me/search/?search=%1$s";
					
		} else if (searchEngine.equals("TorrentReactor")) {
			return "http://www.torrentreactor.net/torrent-search/%1$s";					
		}
		
		return "http://www.vertor.com/index.php?mod=search&search=&cid=0&words=%1$s";
	}
	
	/** Sets the default trackers. */
	public static void setDefaultTrackers(String defaultTrackers) {
		SharedPreferences.Editor editor = preferences_.edit();
		editor.putString("default_trackers", defaultTrackers);
		editor.commit();
	}
	
	/** Returns the default trackers. */
	public static String getDefaultTrackers() {
		String system_default =
				"udp://tracker.openbittorrent.com:80\n\n" + 
				"udp://tracker.publicbt.com:80\n\n" +
				"udp://tracker.istole.it:6969\n\n" +
				"udp://tracker.ccc.de:80";
		return preferences_.getString("default_trackers", system_default);
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
	
	/** Returns the peer's identifier. */
	public static String getClientIdentifier() {
		if (preferences_.contains("client_identifier")) {
			return preferences_.getString("client_identifier", "undefined0undefined0");
		} else {
			Date d = new Date();
	        long seed = d.getTime();   
	        Random r = new Random();
	        r.setSeed(seed);
	        
	        String clientIdentifier = "";
	        for (int i = 0; i < 20; i++) {
	        	clientIdentifier += (char)(Math.abs(r.nextInt()) % 25 + 97); // random lower case alphabetic characters ('a' - 'z')
	        }
	        
	        SharedPreferences.Editor editor = preferences_.edit();
			editor.putString("client_identifier", clientIdentifier);
			editor.commit();
			
			return clientIdentifier;
		}
		/*
		String clientIdentifier = "aaaaaaaaaaaaaaaaaaaa";
		SharedPreferences.Editor editor = preferences_.edit();
		editor.putString("client_identifier", clientIdentifier);
		editor.commit();
		return clientIdentifier;*/
	}

	/** Returns whether the question about sending statistics was asked or not. */
	public static boolean wasAnalyticsAsked() {
		return preferences_.getBoolean("analytics_asked", false);
	}
	
	/** Sets whether the question about sending statistics was asked or not. */
	public static void setAnalyticsAsked(boolean wasAnalyticsAsked) {
		SharedPreferences.Editor editor = preferences_.edit();
		editor.putBoolean("analytics_asked", wasAnalyticsAsked);
		editor.commit();
	}
	
	/** Returns whether sending statistics is enabled or not. */
	public static boolean isAnalyticsEnabled() {
		return preferences_.getBoolean("analytics", false);
	}
	
	/** Sets whether sending statistics is enabled or not. */
	public static void setAnalitics(boolean enabled) {
		SharedPreferences.Editor editor = preferences_.edit();
		editor.putBoolean("analytics", enabled);
		editor.commit();
	}
	
	/** Sets whether it is a test or not. */
	public static void setTesting(boolean isTesting) {
		isTesting_ = isTesting;
	}
}
