package hu.bute.daai.amorg.drtorrent.activity;

import hu.bute.daai.amorg.drtorrent.Preferences;
import hu.bute.daai.amorg.drtorrent.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
		update();
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		
		if (hasFocus) {
			update();
		}		
	}
	
	protected void update() {
		findPreference("port").setSummary(Integer.toString(Preferences.getPort()));
		
		findPreference("connections").setSummary(Integer.toString(Preferences.getMaxConnectedPeers()));
		
		/*if (Preferences.getDownloadSpeedLimit() > 0) {
			findPreference("download_speed").setSummary(Preferences.getDownloadSpeedLimit() + " kB/s");
		} else {
			findPreference("download_speed").setSummary("Unlimited");
		}
		
		if (Preferences.getUploadSpeedLimit() > 0) {
			findPreference("upload_speed").setSummary(Preferences.getUploadSpeedLimit() + " kB/s");
		} else {
			findPreference("upload_speed").setSummary("Unlimited");
		}*/
	}
}
