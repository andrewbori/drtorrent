package hu.bute.daai.amorg.drtorrent;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

public class Preferences {
	
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
			return 0;
		}
	}
	
	/** Returns the number of the maximum connected peers per torrent. */
	public static int getMaxConnectedPeers() {
		try {
			return Integer.valueOf(preferences_.getString("connections", "10"));
		} catch (Exception e) {
			return 0;
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
	
	/** Sets the number of the default search site. */
	public static void setSearchSite(int searchSite) {
		SharedPreferences.Editor editor = preferences_.edit();
		editor.putInt("search_site", searchSite);
		editor.commit();
	}
	
	/** Returns the number of the default search site. */
	public static int getSearchSite() {
		return preferences_.getInt("search_site", 0);
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
