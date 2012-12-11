package hu.bute.daai.amorg.drtorrent.activity;

import hu.bute.daai.amorg.drtorrent.Preferences;
import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.service.TorrentService;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {
	final private static int RESULT_FOLDER_CHOOSER_ACTIVITY = 1;
	private Activity context_ = this;
	
	protected Messenger serviceMessenger_ = null;
	
	private int port_;
	private boolean isIncomingConnectionsEnabled_;
	private boolean isWifiOnly_;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
		
		Preference folderPicker = findPreference("download_folder");
		folderPicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
	        @Override
	        public boolean onPreferenceClick(Preference preference) {
	        	Intent intent = new Intent(context_, FolderChooserActivity.class);
	        	intent.putExtra(FolderChooserActivity.KEY_PATH, Preferences.getDownloadFolder());
				context_.startActivityForResult(intent, RESULT_FOLDER_CHOOSER_ACTIVITY);
	            return false;
	        }
	    });
		
		port_ = Preferences.getPort();
		isIncomingConnectionsEnabled_ = Preferences.isIncomingConnectionsEnabled();
		isWifiOnly_ = Preferences.isWiFiOnly();
		
		update();
	}
	
	@Override
	protected void onStart() {
		bindService(new Intent(this, TorrentService.class), serviceConnection, Context.BIND_AUTO_CREATE);
		super.onStart();
	}
	
	@Override
	protected void onStop() {
		if (port_ != Preferences.getPort() || isIncomingConnectionsEnabled_ != Preferences.isIncomingConnectionsEnabled() ||
											  isWifiOnly_ != Preferences.isWiFiOnly()) {
			port_ = Preferences.getPort();
			isIncomingConnectionsEnabled_ = Preferences.isIncomingConnectionsEnabled();
			isWifiOnly_ = Preferences.isWiFiOnly();
			
			sendMessage(TorrentService.MSG_CONN_SETTINGS_CHANGED);
		}
		
		unbindService(serviceConnection);
		super.onStop();
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		
		if (hasFocus) {
			update();
		}		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case RESULT_FOLDER_CHOOSER_ACTIVITY:
				if (resultCode == Activity.RESULT_OK) {
					final String path = data.getStringExtra(FolderChooserActivity.RESULT_KEY_PATH);
					Preferences.setDownloadFolder(path);
				}
				break;
		}
	}
	
	protected void update() {
		findPreference("download_folder").setSummary(Preferences.getDownloadFolder());
		
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
	
	/** Sends a message to the torrent service. */
	public void sendMessage (int msgWhat) {
		if (serviceMessenger_ != null) {
			Message msg = Message.obtain();
			msg.what = msgWhat;

			try {
				serviceMessenger_.send(msg);
			} catch (RemoteException e) {}
		}
	}
	
	/** Connection of the Torrent Service. */
	final protected ServiceConnection serviceConnection = new ServiceConnection() {
		
		public void onServiceConnected(ComponentName name, IBinder service) {
			serviceMessenger_ = new Messenger(service);
		}

		public void onServiceDisconnected(ComponentName name) {
			serviceMessenger_ = null;
		}
	};
}
