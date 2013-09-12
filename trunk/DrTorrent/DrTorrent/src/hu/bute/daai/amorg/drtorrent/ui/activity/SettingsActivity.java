package hu.bute.daai.amorg.drtorrent.ui.activity;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.ui.service.TorrentService;
import hu.bute.daai.amorg.drtorrent.util.Preferences;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	final private static int RESULT_FOLDER_CHOOSER_ACTIVITY = 1;
	private Activity context_ = this;
	
	protected Messenger serviceMessenger_ = null;
	
	private int port_;
	private boolean isIncomingConnectionsEnabled_;
	private boolean isUpnpEnabled_;
	private boolean isWifiOnly_;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
		
		try {
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
		} catch (Error e) {
			
		}
		
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
		isUpnpEnabled_ = Preferences.isUpnpEnabled();
		isWifiOnly_ = Preferences.isWiFiOnly();
		
		findPreference("download_folder").setSummary(Preferences.getDownloadFolder());
		findPreference("port").setSummary(Integer.toString(Preferences.getPort()));
		findPreference("connections").setSummary(Integer.toString(Preferences.getMaxConnectedPeers()));
		findPreference("search_engine").setSummary(getSearchEngine());
		
		findPreference("upnp").setEnabled(Preferences.isIncomingConnectionsEnabled());
		findPreference("upload_speed").setEnabled(Preferences.isUploadEnabled());
	}
	
	@Override
	public boolean onOptionsItemSelected(android.view.MenuItem item) {
		switch (item.getItemId()) {
	        case android.R.id.home:
	            // app icon in action bar clicked; go home
	        	onBackPressed();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
		
	@Override
	protected void onStart() {
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		bindService(new Intent(this, TorrentService.class), serviceConnection, Context.BIND_AUTO_CREATE);
		super.onStart();
	}
	
	@Override
	protected void onStop() {
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		
		if (port_ != Preferences.getPort() || isIncomingConnectionsEnabled_ != Preferences.isIncomingConnectionsEnabled() ||
				  isWifiOnly_ != Preferences.isWiFiOnly() || isUpnpEnabled_ != Preferences.isUpnpEnabled()) {
			
			if (serviceMessenger_ != null) {
				Message msg = Message.obtain();
				Bundle b = new Bundle();
				b.putBoolean(TorrentService.MSG_KEY_PORT_CHANGED, 	  port_ != Preferences.getPort());
				b.putBoolean(TorrentService.MSG_KEY_INCOMING_CHANGED, isIncomingConnectionsEnabled_ != Preferences.isIncomingConnectionsEnabled());
				b.putBoolean(TorrentService.MSG_KEY_WIFIONLY_CHANGED, isWifiOnly_ != Preferences.isWiFiOnly());
				b.putBoolean(TorrentService.MSG_KEY_UPNP_CHANGED, 	  isUpnpEnabled_ != Preferences.isUpnpEnabled());
				msg.what = TorrentService.MSG_CONN_SETTINGS_CHANGED;
				msg.setData(b);

				try {
					serviceMessenger_.send(msg);
				} catch (RemoteException e) {}
			
				port_ = Preferences.getPort();
				isIncomingConnectionsEnabled_ = Preferences.isIncomingConnectionsEnabled();
				isWifiOnly_ = Preferences.isWiFiOnly();
				isUpnpEnabled_ = Preferences.isUpnpEnabled();
			}
		}
		
		unbindService(serviceConnection);
		super.onStop();
	}
	
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("port")) {
			findPreference(key).setSummary(Integer.toString(Preferences.getPort()));
			
		} else if (key.equals("connections")) {
			findPreference(key).setSummary(Integer.toString(Preferences.getMaxConnectedPeers()));
			
		} else if (key.equals("search_engine")) {
			findPreference(key).setSummary(getSearchEngine());
			
		} else if (key.equals("incoming_connections")) {
			findPreference("upnp").setEnabled(Preferences.isIncomingConnectionsEnabled());
			
		} else if (key.equals("upload")) {
			findPreference("upload_speed").setEnabled(Preferences.isUploadEnabled());
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
					
					findPreference("download_folder").setSummary(path);
				}
				break;
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
	
	/** Returns the human readable name of the search engine. */
	public String getSearchEngine() {
		String[] values = getResources().getStringArray(R.array.search_engine_values);
		String value = Preferences.getSearchEngine(this);
		int i = 0;
		for(;i < values.length - 1; i++) {
			if (values[i].equals(value)) {
				break;
			}
		}
		values = getResources().getStringArray(R.array.search_engine_names);
		if (i < values.length) {
			return values[i];
		}
		return "";
	}
}
