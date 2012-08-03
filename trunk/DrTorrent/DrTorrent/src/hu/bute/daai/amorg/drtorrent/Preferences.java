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
	
	public static boolean isStreamingMode() {
		return preferences_.getBoolean("streaming", false);
	}
	
	public static int getPort() {
		return Integer.valueOf(preferences_.getString("port", "34400"));
	}
	
	public static int getMaxConnectedPeers() {
		return Integer.valueOf(preferences_.getString("connections", "20"));
	}
}
