package hu.bute.daai.amorg.drtorrent.activity;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.TorrentListAdapter;
import hu.bute.daai.amorg.drtorrent.TorrentListItem;
import hu.bute.daai.amorg.drtorrent.service.TorrentService;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class DrTorrentActivity extends Activity {

	private static final int MENU_ADDTORRENT = 101;
	private static final int RESULT_FILEBROWSER_ACTIVITY = 201;

	private Messenger serviceMessenger_ = null;
	private final Messenger clientMessenger_ = new Messenger(new IncomingMessageHandler());
	private boolean isBound_ = false;

	private ListView lvTorrent_;
	private ArrayList<TorrentListItem> torrents_;
	private ArrayAdapter<TorrentListItem> adapter_;
	
	private AlertDialog dialog_;
	private ProgressDialog progressDialog_;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Intent i = new Intent(getApplicationContext(), TorrentService.class);
		startService(i);

		AlertDialog.Builder builder = new AlertDialog.Builder(DrTorrentActivity.this);
		builder.setCancelable(false)
		       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dialog.cancel();
		           }
		       });
		dialog_ = builder.create();

		progressDialog_ = new ProgressDialog(DrTorrentActivity.this);
		torrents_ = new ArrayList<TorrentListItem>();
		lvTorrent_ = (ListView) findViewById(R.id.main_lvTorrent);
		adapter_ = new TorrentListAdapter<TorrentListItem>(this, torrents_);
		lvTorrent_.setAdapter(adapter_);
	}

	@Override
	protected void onStart() {
		super.onStart();
		doBindService();
	}

	@Override
	protected void onStop() {
		super.onStop();
		doUnbindService();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == RESULT_FILEBROWSER_ACTIVITY) {
			if (resultCode == Activity.RESULT_OK) {
				// data contains the full path of the torrent
				String filePath = data.getStringExtra(FileBrowserActivity.RESULT_KEY_FILEPATH);
				
				Message msg = Message.obtain();
				Bundle b = new Bundle();
				b.putString(TorrentService.MSG_KEY_FILEPATH, filePath);
				msg.what = TorrentService.MSG_OPEN_TORRENT;
				msg.setData(b);
				try {
					serviceMessenger_.send(msg);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(Menu.NONE, MENU_ADDTORRENT, Menu.NONE, R.string.menu_addtorrent);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		super.onMenuItemSelected(featureId, item);
		switch (item.getItemId()) {
			case MENU_ADDTORRENT:
				Intent i = new Intent(this, FileBrowserActivity.class);
				startActivityForResult(i, RESULT_FILEBROWSER_ACTIVITY);
				break;
		}
		return true;
	}

	/** Connection of the Torrent Service. */
	private ServiceConnection serviceConnection = new ServiceConnection() {
		
		public void onServiceConnected(ComponentName name, IBinder service) {
			serviceMessenger_ = new Messenger(service);
			isBound_ = true;

			Message msg = Message.obtain();
			msg.what = TorrentService.MSG_SUBSCRIBE_CLIENT;
			msg.replyTo = clientMessenger_;
			try {
				serviceMessenger_.send(msg);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void onServiceDisconnected(ComponentName name) {
			Message msg = Message.obtain();
			msg.what = TorrentService.MSG_UNSUBSCRIBE_CLIENT;
			msg.replyTo = clientMessenger_;
			try {
				serviceMessenger_.send(msg);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			serviceMessenger_ = null;
			isBound_ = false;
		}
	};

	/** Bind to the Torrent Service. */
	private void doBindService() {
		bindService(new Intent(this, TorrentService.class), serviceConnection, Context.BIND_AUTO_CREATE);
	}

	/** Unbind from the Torrent Service. */
	private void doUnbindService() {
		if (isBound_) {
			if (serviceMessenger_ != null) {
				try {
					Message msg = Message.obtain();
					msg.what = TorrentService.MSG_UNSUBSCRIBE_CLIENT;
					msg.replyTo = clientMessenger_;
					serviceMessenger_.send(msg);
				} catch (RemoteException e) {
				}
			}

			unbindService(serviceConnection);
		}
	}

	/** Handler of incoming Messages. */
	private class IncomingMessageHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			String message;
			
			switch (msg.what) {
				case TorrentService.MSG_SHOW_TOAST:
					message = bundle.getString(TorrentService.MSG_KEY_MESSAGE);
					Toast.makeText(DrTorrentActivity.this, message, Toast.LENGTH_SHORT).show();
					break;
					
				case TorrentService.MSG_SHOW_DIALOG:
					message = bundle.getString(TorrentService.MSG_KEY_MESSAGE);
					dialog_.setMessage(message);
					dialog_.show();
					break;
				
				case TorrentService.MSG_SHOW_PROGRESS:
					message = bundle.getString(TorrentService.MSG_KEY_MESSAGE);
					progressDialog_.setMessage(message);
					progressDialog_.show();
					break;
					
				case TorrentService.MSG_HIDE_PROGRESS:
					progressDialog_.hide();
					break;
					
				case TorrentService.MSG_SEND_TORRENT_ITEM:
					TorrentListItem item = (TorrentListItem) bundle.getSerializable(TorrentService.MSG_KEY_TORRENT_ITEM);
					String infoHash = item.getInfoHash();
					boolean found = false;
					TorrentListItem tempItem;
					for (int i = 0; i < adapter_.getCount(); i++) {
						tempItem = adapter_.getItem(i);
						if (tempItem.getInfoHash().equals(infoHash)) {
							found = true;
							adapter_.getItem(i).set(item);
							break;
						}
					}
					if (!found) {
						adapter_.add(item);
					}

					lvTorrent_.invalidateViews();
					break;

				case TorrentService.MSG_SEND_TORRENT_LIST:
					@SuppressWarnings("unchecked")
					ArrayList<TorrentListItem> t = ((ArrayList<TorrentListItem>) bundle.getSerializable(TorrentService.MSG_KEY_TORRENT_LIST));
					boolean foundOld = false;
					for (int i = 0; i < adapter_.getCount(); i++) {
						foundOld = false;
						for (int j = 0; j < t.size(); j++) {
							if (adapter_.getItem(i).compareTo(t.get(j)) == 0) {
								foundOld = true;
								adapter_.getItem(i).set(t.get(j));
								t.remove(j);
							}
						}
						if (!foundOld) {
							adapter_.remove(adapter_.getItem(i));
							i--;
						}
					}

					for (int i = 0; i < t.size(); i++) {
						adapter_.add(t.get(i));
					}

					lvTorrent_.invalidateViews();
					break;
					
				default:
					super.handleMessage(msg);
			}
		}
	}
}