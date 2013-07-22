package hu.bute.daai.amorg.drtorrent.activity;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.adapter.FileSettingsListAdapter;
import hu.bute.daai.amorg.drtorrent.adapter.item.FileListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.TorrentListItem;
import hu.bute.daai.amorg.drtorrent.service.TorrentClient;
import hu.bute.daai.amorg.drtorrent.service.TorrentService;
import hu.bute.daai.amorg.drtorrent.torrentengine.File;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class TorrentSettingsActivity extends Activity implements OnClickListener{

	private final static int RESULT_FOLDER_CHOOSER_ACTIVITY = 1;
	
	private int clientId_ = 0;
	private TorrentListItem torrent_ = null;
	private ArrayList<FileListItem> fileList_ = null;
	private ListView fileListView_ = null;
	private FileSettingsListAdapter<FileListItem> adapter_ = null;
	private ProgressBar progressBar_ = null;
	private TextView tvTorrentName_ = null;
	private TextView tvDownloadPath_ = null;
	
	private Messenger serviceMessenger_ = null;
	private final Messenger clientMessenger_ = new Messenger(new IncomingMessageHandler(this));
	private boolean isBound_ = false;
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.torrent_settings_files);
		getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		
		Intent intent = getIntent();
		if (intent != null) {
			Bundle bundle = intent.getExtras();
			if (bundle != null) {
				torrent_ = (TorrentListItem) bundle.getSerializable(TorrentService.MSG_KEY_TORRENT_ITEM);
				fileList_ = ((ArrayList<FileListItem>) bundle.getSerializable(TorrentService.MSG_KEY_FILE_LIST));
			}
		}
		
		((Button) findViewById(R.id.btnOk)).setOnClickListener(this);
		((Button) findViewById(R.id.btnCancel)).setOnClickListener(this);
		((ImageButton) findViewById(R.id.torrent_settings_btnDownloadFolder)).setOnClickListener(this);
		
		tvTorrentName_ = (TextView) findViewById(R.id.torrent_settings_tvTorrentName);
		tvDownloadPath_ = (TextView) findViewById(R.id.torrent_settings_tvDownloadPath);
		
		tvTorrentName_.setText(torrent_.getName());
		tvDownloadPath_.setText(torrent_.getDownloadFolder());
		
		progressBar_ = (ProgressBar) findViewById(R.id.torrent_settings_progressBar);
		fileListView_ = (ListView) findViewById(R.id.torrent_settings_lvFiles);
		refreshFileList(fileList_);
		
		((CheckBox) findViewById(R.id.cbALl)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (fileList_ != null && !fileList_.isEmpty()) {
					for (int i = 0; i < fileList_.size(); i++) {
						if (isChecked) fileList_.get(i).setPriority(File.PRIORITY_NORMAL);
						else fileList_.get(i).setPriority(File.PRIORITY_SKIP);
						
						fileListView_.invalidateViews();
					}
				}
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (fileList_ == null || fileList_.isEmpty()) {
			doBindService();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		doUnbindService();
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btnOk:
				Intent intent = new Intent();
				intent.putExtra(TorrentService.MSG_KEY_FILE_LIST, fileList_);
				intent.putExtra(TorrentService.MSG_KEY_TORRENT_ITEM, torrent_);
				setResult(RESULT_OK, intent);
				finish();
				break;
				
			case R.id.btnCancel:
				intent = new Intent();
				intent.putExtra(TorrentService.MSG_KEY_TORRENT_ITEM, torrent_);
				setResult(RESULT_CANCELED, intent);
				finish();
				break;
				
			case R.id.torrent_settings_btnDownloadFolder:
				intent = new Intent(this, FolderChooserActivity.class);
	        	intent.putExtra(FolderChooserActivity.KEY_PATH, torrent_.getDownloadFolder());
				this.startActivityForResult(intent, RESULT_FOLDER_CHOOSER_ACTIVITY);
				break;
			default:
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case RESULT_FOLDER_CHOOSER_ACTIVITY:
				if (resultCode == Activity.RESULT_OK) {
					final String path = data.getStringExtra(FolderChooserActivity.RESULT_KEY_PATH);
					torrent_.setDownloadFolder(path);
					tvDownloadPath_.setText(path);
				}
				break;
		}
	}
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent();
		intent.putExtra(TorrentService.MSG_KEY_TORRENT_ITEM, torrent_);
		setResult(RESULT_CANCELED, intent);
		finish();
	}
	
	/** Connection of the Torrent Service. */
	final protected ServiceConnection serviceConnection = new ServiceConnection() {
		
		public void onServiceConnected(ComponentName name, IBinder service) {
			serviceMessenger_ = new Messenger(service);
			isBound_ = true;

			Message msg = Message.obtain();
			msg.what = TorrentService.MSG_SUBSCRIBE_CLIENT;

			Bundle bundle = new Bundle();
			clientId_ = TorrentClient.generateId();
			bundle.putInt(TorrentService.MSG_KEY_CLIENT_ID, clientId_);
			bundle.putInt(TorrentService.MSG_KEY_TORRENT_ID, torrent_.getId());
			bundle.putInt(TorrentService.MSG_KEY_CLIENT_TYPE, TorrentClient.CLIENT_TYPE_SETTINGS);
			msg.setData(bundle);

			msg.replyTo = clientMessenger_;
			try {
				serviceMessenger_.send(msg);
			} catch (RemoteException e) {}
		}

		public void onServiceDisconnected(ComponentName name) {
			serviceMessenger_ = null;
			isBound_ = false;
		}
	};

	/** Bind to the Torrent Service. */
	protected void doBindService() {
		bindService(new Intent(this, TorrentService.class), serviceConnection, Context.BIND_AUTO_CREATE);
	}

	/** Unbind from the Torrent Service. */
	protected void doUnbindService() {
		if (isBound_) {
			if (serviceMessenger_ != null) {
				Message msg = Message.obtain();
				msg.what = TorrentService.MSG_UNSUBSCRIBE_CLIENT;

				Bundle bundle = new Bundle();
				bundle.putInt(TorrentService.MSG_KEY_CLIENT_ID, clientId_);
				bundle.putInt(TorrentService.MSG_KEY_TORRENT_ID, torrent_.getId());
				msg.setData(bundle);

				msg.replyTo = clientMessenger_;
				try {
					serviceMessenger_.send(msg);
				} catch (RemoteException e) {}
			}
			
			try {
				unbindService(serviceConnection);
			} catch (Exception e) {
				
			}
		}
	}
	
	/** Handler of incoming Messages. */
	private static class IncomingMessageHandler extends Handler {

		private final WeakReference<TorrentSettingsActivity> activity_; 

		IncomingMessageHandler(TorrentSettingsActivity activity) {
	    	activity_ = new WeakReference<TorrentSettingsActivity>(activity);
	    }
		
		@Override
		public void handleMessage(Message msg) {
			TorrentSettingsActivity activity = activity_.get();
			if (activity != null) {
				activity.handleMessage(msg);
			}
		}
	}
	
	/** Handling the incoming message. */
	public void handleMessage(Message msg) {
		Bundle bundle = msg.getData();
		
		switch (msg.what) {
			
			case TorrentService.MSG_SEND_TORRENT_ITEM:
				TorrentListItem item = (TorrentListItem) bundle.getSerializable(TorrentService.MSG_KEY_TORRENT_ITEM);
				refreshTorrentItem(item);
				break;
				
			case TorrentService.MSG_SEND_FILE_LIST:
				@SuppressWarnings("unchecked")
				ArrayList<FileListItem> files = ((ArrayList<FileListItem>) bundle.getSerializable(TorrentService.MSG_KEY_FILE_LIST));
				refreshFileList(files);
				break;
				
			default:
		}
	}
	
	private void refreshTorrentItem(TorrentListItem item) {
		if (torrent_.equals(item)) {
			String downloadFolder = torrent_.getDownloadFolder();
			torrent_ = item;
			torrent_.setDownloadFolder(downloadFolder);
			tvTorrentName_.setText(torrent_.getName());

		}
	}

	private void refreshFileList(ArrayList<FileListItem> fileList) {
		fileList_ = fileList; 
		adapter_ = new FileSettingsListAdapter<FileListItem>(this, fileList_);
		fileListView_.setAdapter(adapter_);
		
		if (fileList_ == null || fileList_.isEmpty()) {
			progressBar_.setVisibility(ProgressBar.VISIBLE);
			fileListView_.setVisibility(ListView.GONE);
		} else {
			progressBar_.setVisibility(ProgressBar.GONE);
			fileListView_.setVisibility(ListView.VISIBLE);
		}
	}
}
