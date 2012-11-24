package hu.bute.daai.amorg.drtorrent.activity;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.adapter.TorrentListAdapter;
import hu.bute.daai.amorg.drtorrent.adapter.item.FileListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.TorrentListItem;
import hu.bute.daai.amorg.drtorrent.service.TorrentService;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class DrTorrentActivity extends TorrentHostActivity {
	
	private ListView lvTorrent_;
	private ArrayList<TorrentListItem> torrents_;
	private ArrayAdapter<TorrentListItem> adapter_;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		if (isShuttingDown_) {
			finish();
			return;
		}
		
		torrents_ = new ArrayList<TorrentListItem>();
		lvTorrent_ = (ListView) findViewById(R.id.main_lvTorrent);
		adapter_ = new TorrentListAdapter<TorrentListItem>(this, torrents_);
		lvTorrent_.setAdapter(adapter_);
		
		lvTorrent_.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				TorrentListItem item = adapter_.getItem(position);
				int torrentId = item.getId();
				
				Intent intent = new Intent(context_, TorrentDetailsActivity.class);
				intent.putExtra(KEY_TORRENT_ID, torrentId);
				startActivity(intent);
			}
		});
		
		lvTorrent_.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				final TorrentListItem torrent = torrents_.get(position);
				
				CharSequence[] items = null;
				if (torrent.getStatus() == R.string.status_stopped) {
					items = new CharSequence[] { getString(R.string.start), getString(R.string.remove) };
				} else {
					items = new CharSequence[] { getString(R.string.stop), getString(R.string.remove) };
				}
				
				AlertDialog.Builder builder = new AlertDialog.Builder(context_);
				builder.setTitle(torrent.getName()).
				setItems(items, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final Message msg = Message.obtain();
						Bundle bundle = new Bundle();
						bundle.putInt(TorrentService.MSG_KEY_TORRENT_ID, torrent.getId());
						msg.setData(bundle);
						
						switch (which) {
							case 0:
								if (torrent.getStatus() == R.string.status_stopped) msg.what = TorrentService.MSG_START_TORRENT;
								else msg.what = TorrentService.MSG_STOP_TORRENT;
								try {
									serviceMessenger_.send(msg);
								} catch (RemoteException e) {}
								break;
								
							case 1:
								AlertDialog.Builder builder = new AlertDialog.Builder(context_);
								builder.setTitle(torrent.getName()).
								setMessage(getString(R.string.remove_dialog_title)).
								setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										msg.what = TorrentService.MSG_CLOSE_TORRENT;
										try {
											serviceMessenger_.send(msg);
										} catch (RemoteException e) {}
									}
								}).
								setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.cancel();
									}
								}).
								create().show();
								
								break;
						}
						
					}
				}).
				create().show();
				
				return true;
			}
			
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		final Intent intent = getIntent ();
		if (intent != null) {
			final Uri data = intent.getData();
			setIntent(null);
			if (data != null) {
				if (data.getScheme().equalsIgnoreCase("file") || data.getScheme().equalsIgnoreCase("http")) {
					fileToOpen_ = data;
				}
			}
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case RESULT_FILEBROWSER_ACTIVITY:
				if (resultCode == Activity.RESULT_OK) {
					// data contains the full path of the torrent
					final String filePath = data.getStringExtra(FileBrowserActivity.RESULT_KEY_FILEPATH);
					openTorrent(Uri.fromFile(new File(filePath)));
				}
				break;
				
			case RESULT_TORRENT_SETTINGS:
				if (resultCode == Activity.RESULT_OK) {
					TorrentListItem torrent = (TorrentListItem) data.getSerializableExtra(TorrentService.MSG_KEY_TORRENT_ITEM);
					@SuppressWarnings("unchecked")
					ArrayList<FileListItem> fileList = ((ArrayList<FileListItem>) data.getSerializableExtra(TorrentService.MSG_KEY_FILE_LIST));
					
					Message msg = Message.obtain();
					Bundle b = new Bundle();
					b.putSerializable(TorrentService.MSG_KEY_TORRENT_ITEM, torrent);
					b.putSerializable(TorrentService.MSG_KEY_FILE_LIST, fileList);
					msg.what = TorrentService.MSG_SEND_TORRENT_SETTINGS;
					msg.setData(b);
					try {
						serviceMessenger_.send(msg);
					} catch (Exception e) {}
				}
				break;
		}
	}
	
	/** Sends torrent file open request to the Torrent Service. */
	@Override
	protected void openTorrent(Uri torrentUri) {
		fileToOpen_ = null;
		Log.v("", torrentUri.getHost() + torrentUri.getPath());
		Message msg = Message.obtain();
		Bundle b = new Bundle();
		b.putParcelable(TorrentService.MSG_KEY_FILEPATH, torrentUri);
		msg.what = TorrentService.MSG_OPEN_TORRENT;
		msg.setData(b);
		try {
			serviceMessenger_.send(msg);
		} catch (Exception e) {}
	}
	
	/** Shows the Torrent Settings (before the first start of the torrent). */
	@Override
	protected void showTorrentSettings(TorrentListItem torrent, ArrayList<FileListItem> fileList) {
		Intent intent = new Intent(this, TorrentSettingsActivity.class);
		intent.putExtra(TorrentService.MSG_KEY_TORRENT_ITEM, torrent);
		intent.putExtra(TorrentService.MSG_KEY_FILE_LIST, fileList);
		startActivityForResult(intent, RESULT_TORRENT_SETTINGS);
	}
	
	/** Shuts down the application. */
	protected void shutDown() {
		AlertDialog.Builder builder = new AlertDialog.Builder(context_);
		builder.setMessage(getString(R.string.exit_dialog_title)).
		setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Message msg = Message.obtain();
				msg.what = TorrentService.MSG_SHUT_DOWN;
				try {
					serviceMessenger_.send(msg);
				} catch (Exception e) {}
				
				Intent intent = new Intent(getApplicationContext(), DrTorrentActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
				intent.putExtra(SHUT_DOWN, true);
				startActivity(intent);
				finish();
			}
		}).
		setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		}).
		create().show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_SETTINGS, Menu.NONE, "Settings")
			.setIcon(R.drawable.ic_settings)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(Menu.NONE, MENU_ADD_TORRENT, Menu.NONE, R.string.menu_addtorrent)
			.setIcon(R.drawable.ic_add)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(Menu.NONE, MENU_SHUT_DOWN, Menu.NONE, "Shut down")
			.setIcon(android.R.drawable.ic_menu_close_clear_cancel)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		super.onMenuItemSelected(featureId, item);
		Intent intent = null;
		switch (item.getItemId()) {
			case MENU_ADD_TORRENT:
				intent = new Intent(this, FileBrowserActivity.class);
				startActivityForResult(intent, RESULT_FILEBROWSER_ACTIVITY);
				break;
				
			case MENU_SETTINGS:
				intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				break;
				
			case MENU_SHUT_DOWN:
				shutDown();
				break;
		}
		return true;
	}
	
	@Override
	protected void refreshTorrentItem(TorrentListItem item, boolean isRemoved) {
		boolean found = false;
		TorrentListItem tempItem;
		for (int i = 0; i < adapter_.getCount(); i++) {
			tempItem = adapter_.getItem(i);
			if (tempItem.equals(item)) {
				found = true;
				if (!isRemoved) {
					adapter_.getItem(i).set(item);
				} else {
					adapter_.remove(tempItem);
				}
				break;
			}
		}
		if (!found && !isRemoved) {
			adapter_.add(item);
		}

		lvTorrent_.invalidateViews();
	}

	@Override
	protected void refreshTorrentList(ArrayList<TorrentListItem> itemList) {
		boolean foundOld = false;
		for (int i = 0; i < adapter_.getCount(); i++) {
			foundOld = false;
			for (int j = 0; j < itemList.size(); j++) {
				if (adapter_.getItem(i).equals(itemList.get(j))) {
					foundOld = true;
					adapter_.getItem(i).set(itemList.get(j));
					itemList.remove(j);
				}
			}
			if (!foundOld) {
				adapter_.remove(adapter_.getItem(i));
				i--;
			}
		}

		for (int i = 0; i < itemList.size(); i++) {
			adapter_.add(itemList.get(i));
		}

		lvTorrent_.invalidateViews();
	}
}