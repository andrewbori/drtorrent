package hu.bute.daai.amorg.drtorrent.activity;

import hu.bute.daai.amorg.drtorrent.Preferences;
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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

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
		getSupportActionBar().setDisplayShowTitleEnabled(false);
		
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
				if (torrent.getStatus() == R.string.status_stopped || torrent.getStatus() == R.string.status_finished) {
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
								if (torrent.getStatus() == R.string.status_stopped || torrent.getStatus() == R.string.status_finished) {
									msg.what = TorrentService.MSG_START_TORRENT;
								}
								else {
									msg.what = TorrentService.MSG_STOP_TORRENT;
								}
								try {
									serviceMessenger_.send(msg);
								} catch (RemoteException e) {}
								break;
								
							case 1:
								AlertDialog.Builder builder = new AlertDialog.Builder(context_);
								builder.setTitle(torrent.getName()).
								setMessage(getString(R.string.remove_dialog_title)).
								setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										msg.what = TorrentService.MSG_CLOSE_TORRENT;
										try {
											serviceMessenger_.send(msg);
										} catch (RemoteException e) {}
									}
								}).
								setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
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
		final Intent intent = getIntent();
		if (intent != null) {
			Uri data = intent.getData();
			intent.setData(Uri.parse("nothing://"));
			
			if (data != null) {
				if (data.getScheme().equalsIgnoreCase("file") || data.getScheme().equalsIgnoreCase("http") || 
																 data.getScheme().equalsIgnoreCase("magnet")) {
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
				if (resultCode == Activity.RESULT_OK && data != null) {
					// data contains the full path of the torrent
					final String filePath = data.getStringExtra(FileBrowserActivity.RESULT_KEY_FILEPATH);
					openTorrent(Uri.fromFile(new File(filePath)));
				}
				break;
				
			case RESULT_TORRENT_SETTINGS:
				if (data == null) {
					return;
				}
				
				Message msg = Message.obtain();
				Bundle b = new Bundle();
				if (resultCode == Activity.RESULT_OK) {
					@SuppressWarnings("unchecked")
					ArrayList<FileListItem> fileList = ((ArrayList<FileListItem>) data.getSerializableExtra(TorrentService.MSG_KEY_FILE_LIST));
					
					b.putSerializable(TorrentService.MSG_KEY_FILE_LIST, fileList);
					b.putSerializable(TorrentService.MSG_KEY_IS_REMOVED, false);
				} else {
					b.putSerializable(TorrentService.MSG_KEY_IS_REMOVED, true);
				}
				
				TorrentListItem torrent = (TorrentListItem) data.getSerializableExtra(TorrentService.MSG_KEY_TORRENT_ITEM);
				b.putSerializable(TorrentService.MSG_KEY_TORRENT_ITEM, torrent);
				
				msg.what = TorrentService.MSG_SEND_TORRENT_SETTINGS;
				msg.setData(b);
				try {
					serviceMessenger_.send(msg);
				} catch (Exception e) {}
				break;
			
			default:
		}
	}
	
	/** Sends torrent file open request to the Torrent Service. */
	@Override
	protected void openTorrent(Uri torrentUri) {
		fileToOpen_ = null;
		
		//Log.v("", torrentUri.getHost() + torrentUri.getPath());
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
	
	/** Shows the search dialog. */
	protected void search() {
		AlertDialog.Builder builder = new AlertDialog.Builder(context_);
		builder.setTitle(R.string.search);

	    LayoutInflater inflater = context_.getLayoutInflater();
	    View dialogView = inflater.inflate(R.layout.dialog_search, null);
	    builder.setView(dialogView);
	    
	    final Spinner spinnerSite = (Spinner) dialogView.findViewById(R.id.dialog_search_spinnerSite);
	    final EditText etSearch = (EditText) dialogView.findViewById(R.id.dialog_search_etSearch);
	    
	    if (spinnerSite.getAdapter().getCount() > Preferences.getSearchSite()) {
	    	spinnerSite.setSelection(Preferences.getSearchSite());
	    }
		
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {	
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Preferences.setSearchSite(spinnerSite.getSelectedItemPosition());
				String searchStr = etSearch.getText().toString();
				String searchUrlStr = "";
				switch (spinnerSite.getSelectedItemPosition()) {
					case 0:
						searchUrlStr = "http://www.google.hu/search?q=" + searchStr + " .torrent";
						break;
					case 1:
						searchUrlStr = "http://1337x.org/search/" + searchStr + "/0/";
						break;
					case 2:
						searchUrlStr = "http://bitsnoop.com/search/all/" + searchStr;
						break;
					case 3:
						searchUrlStr = "http://extratorrent.com/search/?search=" + searchStr;
						break;
					case 4:
						searchUrlStr = "http://h33t.com/search/" + searchStr;
						break;
					case 5:
						searchUrlStr = "http://isohunt.com/torrents/?ihq=" + searchStr;
						break;
					case 6:
						searchUrlStr = "http://kat.ph/usearch/" + searchStr + "/";
						break;
					case 7:
						searchUrlStr = "http://www.mininova.org/search/?search=" + searchStr;
						break;
					case 8:
						searchUrlStr = "http://torrentz.eu/search?f=" + searchStr;
						break;
					case 9:
						searchUrlStr = "http://thepiratebay.org/search/" + searchStr;
						break;
					case 10:
						searchUrlStr = "http://www.torrentreactor.net/torrent-search/" + searchStr;
						break;
					case 11:
						searchUrlStr = "http://rarbg.com/torrents.php?search=" + searchStr;
						break;
				};
				
				try {
					Uri uri = Uri.parse(searchUrlStr);
					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
					startActivity(intent);
					
					dialog.cancel();
				} catch (Exception e) {
				}
			}
		}).
		setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {	
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		}).
		create().show();
	}
	
	/** Shows the "Add magnet link" dialog. */
	public void addMagnetLink() {
		AlertDialog.Builder builder = new AlertDialog.Builder(context_);
		builder.setTitle(R.string.add_magnet_link);

	    final EditText etMagnetLink = new EditText(context_);
	    etMagnetLink.setHint("magnet:?xt=urn:btih:...");
	    etMagnetLink.setSingleLine(true);
	    etMagnetLink.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
	    builder.setView(etMagnetLink).
	    
	    setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {	
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String magnetLinkStr = etMagnetLink.getText().toString();
				if (magnetLinkStr.startsWith("magnet")) {
					try {
						Uri uri = Uri.parse(magnetLinkStr);
						openTorrent(uri);
						
						dialog.cancel();
					} catch (Exception e) {
					}
				}
			}
		}).
		setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {	
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		}).
		create().show();
	}
	
	/** Shuts down the application. */
	protected void shutDown() {
		AlertDialog.Builder builder = new AlertDialog.Builder(context_);
		builder.setMessage(getString(R.string.exit_dialog_title)).
		setTitle(getString(R.string.shut_down)).
		setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
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
		setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		}).
		create().show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_SETTINGS, Menu.NONE, R.string.settings)
			.setIcon(R.drawable.ic_menu_settings)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		menu.add(Menu.NONE, MENU_ADD_TORRENT, Menu.NONE, R.string.menu_addtorrent)
			.setIcon(R.drawable.ic_menu_add2)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		menu.add(Menu.NONE, MENU_SEARCH_TORRENT, Menu.NONE, R.string.search)
			.setIcon(R.drawable.ic_menu_search)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		menu.add(Menu.NONE, MENU_ADD_MAGNET_LINK, Menu.NONE, R.string.add_magnet_link)
			//.setIcon(R.drawable.ic_menu_magnet)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		menu.add(Menu.NONE, MENU_ABOUT, Menu.NONE, R.string.about)
			//.setIcon(R.drawable.ic_menu_about)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		menu.add(Menu.NONE, MENU_FEEDBACK, Menu.NONE, R.string.feedback)
			//.setIcon(R.drawable.ic_menu_email)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		menu.add(Menu.NONE, MENU_SHUT_DOWN, Menu.NONE, R.string.shut_down)
			//.setIcon(R.drawable.ic_menu_close)
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
				
			case MENU_SEARCH_TORRENT:
				search();
				break;
				
			case MENU_SETTINGS:
				intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				break;
				
			case MENU_ADD_MAGNET_LINK:
				addMagnetLink();
				break;
				
			case MENU_ABOUT:
				LayoutInflater inflater = getLayoutInflater();
				View layout = inflater.inflate(R.layout.dialog_about, null);

				AlertDialog.Builder builder = new AlertDialog.Builder(context_);
				builder.setTitle(getString(R.string.about_drtorrent)).
				setView(layout).
				create().show();
				break;
				
			case MENU_FEEDBACK:
				String ver = "";
				try {
					PackageManager manager = context_.getPackageManager();
					PackageInfo info = manager.getPackageInfo(context_.getPackageName(), 0);
					ver = " (v" + info.versionName + ")";
				} catch (Exception e) {
				}
				
				intent = new Intent(Intent.ACTION_SENDTO);
				intent.setType("text/plain");
				intent.setData(Uri.parse("mailto:andreasweiner@gmail.com"));
				intent.putExtra(Intent.EXTRA_SUBJECT, "DrTorrent feedback" + ver);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // when return, DrTorrent is displayed, instead of the email client
				try {
				    startActivity(intent);
				} catch (android.content.ActivityNotFoundException ex) {
				    Toast.makeText(context_, context_.getString(R.string.no_email_client), Toast.LENGTH_SHORT).show();
				}
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