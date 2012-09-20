package hu.bute.daai.amorg.drtorrent;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {
	
	private static SharedPreferences preferences_ = null;
	
	private Preferences() {}
	
	public static void set(Context context) {
		preferences_ = PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	/** Returns whether the streaming is enabled or not. */
	public static boolean isStreamingMode() {
		return preferences_.getBoolean("streaming", false);
	}
	
	/** Returns whether the download is enabled only over WiFi or not. */
	public static boolean isOnlyWiFi() {
		return preferences_.getBoolean("only_wifi", true);
	}
	
	/** Returns the P2P port. */
	public static int getPort() {
		try {
			return Integer.valueOf(preferences_.getString("port", "34400"));
		} catch (Exception e) {
			return 0;
		}
	}
	
	/** Returns the number of the maximum connected peers per torrent. */
	public static int getMaxConnectedPeers() {
		try {
			return Integer.valueOf(preferences_.getString("connections", "20"));
		} catch (Exception e) {
			return 0;
		}
	}
	
	/** Returns the download speed limit. */
	public static int getDownloadSpeedLimit() {
		try {
			return Integer.valueOf(preferences_.getString("download_speed",  "0"));
		} catch (Exception e) {
			return 0;
		}
	}
	
	/** Returns the upload speed limit. */
	public static int getUploadSpeedLimit() {
		try {
			return Integer.valueOf(preferences_.getString("upload_speed",  "0"));
		} catch (Exception e) {
			return 0;
		}
	}
}
