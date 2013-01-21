package hu.bute.daai.amorg.drtorrent.activity;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.activity.task.ApkInstallerTask;
import hu.bute.daai.amorg.drtorrent.adapter.TorrentListAdapter;
import hu.bute.daai.amorg.drtorrent.adapter.item.FileListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.TorrentListItem;
import hu.bute.daai.amorg.drtorrent.service.TorrentService;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class DrTorrentActivity extends TorrentHostActivity implements OnNavigationListener {
	
	private final static int VIEW_ALL 		  = 0;
	private final static int VIEW_DOWNLOADING = 1;
	private final static int VIEW_COMPLETED   = 2;
	private final static int VIEW_ACTIVE	  = 3;
	private final static int VIEW_INACTIVE    = 4;
	
	private ListView lvTorrent_ = null;
	private ArrayList<TorrentListItem> torrents_ = null;
	private ArrayList<TorrentListItem> completedTorrents_ = null;
	private ArrayList<TorrentListItem> downloadingTorrents_ = null;
	private ArrayList<TorrentListItem> activeTorrents_ = null;
	private ArrayList<TorrentListItem> inactiveTorrents_ = null;
	private ArrayList<TorrentListItem> list_ = null;
	private ArrayAdapter<TorrentListItem> adapter_ = null;
	
	private SearchView searchView_ = null;
	
	private int viewId_ = VIEW_ALL;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		list_ = torrents_ = new ArrayList<TorrentListItem>();
		completedTorrents_ = new ArrayList<TorrentListItem>();
		downloadingTorrents_ = new ArrayList<TorrentListItem>();
		activeTorrents_ = new ArrayList<TorrentListItem>();
		inactiveTorrents_ = new ArrayList<TorrentListItem>();
		
		init();
		
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
	}
	
	private void init() {
		setContentView(R.layout.main);
		getSupportActionBar().setDisplayShowTitleEnabled(false);
		//setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
		
		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		
		SpinnerAdapter spinnerAdapter = ArrayAdapter.createFromResource(this,
	            R.array.torrent_list_views,
	            R.layout.sherlock_spinner_dropdown_item);
		
		actionBar.setListNavigationCallbacks(spinnerAdapter, this);
		actionBar.setSelectedNavigationItem(viewId_);
		
		if (isShuttingDown_) {
			finish();
			return;
		}
		
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
				final TorrentListItem torrent = adapter_.getItem(position);
				
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
										dialog.dismiss();
									}
								});
								dialog_ = builder.create();
								dialog_.show();
								
								break;
						}
					}
				});
				dialog_ = builder.create();
				dialog_.show();
				
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
															     data.getScheme().equalsIgnoreCase("https") ||
																 data.getScheme().equalsIgnoreCase("magnet")) {
					fileToOpen_ = data;
				}
			}
		}
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		init();
		resetTorrentListAdapter();
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
	
	protected boolean canSearch() {
		boolean shouldInstall = false;
		int positive = R.string.update;
		int message = R.string.search_update_message;
		try {
			PackageInfo packageInfo = getPackageManager().getPackageInfo("org.transdroid.search", 0);
			if (packageInfo.versionCode < 12) {
				shouldInstall = true;
			}
		} catch (NameNotFoundException e1) {
			shouldInstall = true;
			positive = R.string.install;
			message = R.string.search_install_message;
		}
		
		if (shouldInstall) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.search_unavailable)
			.setMessage(message)
			.setPositiveButton(positive, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					new ApkInstallerTask(DrTorrentActivity.this).execute("http://transdroid-search.googlecode.com/files/transdroid-search-1.10.apk");
					dialog.dismiss();
				}
			})
			.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			dialog_ = builder.create();
			dialog_.show();
			
			return false;
		}
		return true;
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
						
						dialog.dismiss();
					} catch (Exception e) {
					}
				}
			}
		}).
		setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {	
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		dialog_ = builder.create();
		dialog_.show();
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
				dialog.dismiss();
			}
		});
		dialog_ = builder.create();
		dialog_.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		try {
			 // Get the SearchView and set the searchable configuration
			searchView_ = new SearchView(this);
			SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
	    	searchView_.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		    searchView_.setIconifiedByDefault(true); // Do not iconify the widget; expand it by default
		    searchView_.setQueryRefinementEnabled(true);
	    
		    menu.add(Menu.NONE, MENU_SEARCH_TORRENT2, Menu.NONE, R.string.search)
				.setIcon(R.drawable.ic_menu_search)
				.setActionView(searchView_)
				.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
					@Override
					public boolean onMenuItemActionExpand(MenuItem item) {
						if (!canSearch()) {
							return false;
						}
						return true;
					}
					
					@Override
					public boolean onMenuItemActionCollapse(MenuItem item) {
						return true;
					}
				})
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		    
		} catch (NoClassDefFoundError e) {
			menu.add(Menu.NONE, MENU_SEARCH_TORRENT, Menu.NONE, R.string.search)
				.setIcon(R.drawable.ic_menu_search)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	    }
		
		menu.add(Menu.NONE, MENU_ADD_TORRENT, Menu.NONE, R.string.menu_addtorrent)
			.setIcon(R.drawable.ic_menu_add2)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		
		menu.add(Menu.NONE, MENU_ADD_MAGNET_LINK, Menu.NONE, R.string.add_magnet_link)
			//.setIcon(R.drawable.ic_menu_magnet)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		
		menu.add(Menu.NONE, MENU_SETTINGS, Menu.NONE, R.string.settings)
			.setIcon(R.drawable.ic_menu_settings)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
		menu.add(Menu.NONE, MENU_RATE_APP, Menu.NONE, R.string.rate_app)
			//.setIcon(R.drawable.ic_menu_email)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		
		menu.add(Menu.NONE, MENU_FEEDBACK, Menu.NONE, R.string.feedback)
			//.setIcon(R.drawable.ic_menu_email)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		
		menu.add(Menu.NONE, MENU_ABOUT, Menu.NONE, R.string.about)
			//.setIcon(R.drawable.ic_menu_about)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		
		menu.add(Menu.NONE, MENU_SHUT_DOWN, Menu.NONE, R.string.shut_down)
			//.setIcon(R.drawable.ic_menu_close)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Intent intent = null;
		switch (item.getItemId()) {
			case MENU_ADD_TORRENT:
				intent = new Intent(this, FileBrowserActivity.class);
				startActivityForResult(intent, RESULT_FILEBROWSER_ACTIVITY);
				break;
				
			case MENU_SEARCH_TORRENT:
				if (canSearch()) {
					onSearchRequested();
				}
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
				setView(layout);
				dialog_ = builder.create();
				dialog_.show();
				break;
				
			case MENU_RATE_APP:
				final Uri uri = Uri.parse("market://details?id=" + getApplicationContext().getPackageName());
				final Intent rateAppIntent = new Intent(Intent.ACTION_VIEW, uri);

				if (getPackageManager().queryIntentActivities(rateAppIntent, 0).size() > 0) {
				    startActivity(rateAppIntent);
				}
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
				
			default:
				return super.onMenuItemSelected(featureId, item);
		}
		return true;
	}
	
	@Override
	protected void refreshTorrentItem(TorrentListItem item, boolean isRemoved) {
		boolean found = false;
		TorrentListItem tempItem;
		for (int i = 0; i < torrents_.size(); i++) {
			tempItem = torrents_.get(i);
			if (tempItem.equals(item)) {
				found = true;
				if (!isRemoved) {
					tempItem.set(item);
					addTorrent(tempItem);
				} else {
					torrents_.remove(tempItem);
					completedTorrents_.remove(tempItem);
					downloadingTorrents_.remove(tempItem);
					activeTorrents_.remove(tempItem);
					inactiveTorrents_.remove(tempItem);
				}
				break;
			}
		}
		if (!found && !isRemoved) {
			torrents_.add(item);
			addTorrent(item);
		}
		Collections.sort(list_);

		lvTorrent_.invalidateViews();
	}

	@Override
	protected void refreshTorrentList(ArrayList<TorrentListItem> itemList) {
		boolean foundOld = false;
		TorrentListItem tempItem;
		for (int i = 0; i < torrents_.size(); i++) {
			tempItem = torrents_.get(i);
			foundOld = false;
			for (int j = 0; j < itemList.size(); j++) {
				if (tempItem.equals(itemList.get(j))) {
					foundOld = true;
					tempItem.set(itemList.get(j));
					addTorrent(tempItem);
					itemList.remove(j);
				}
			}
			if (!foundOld) {
				torrents_.remove(tempItem);
				completedTorrents_.remove(tempItem);
				downloadingTorrents_.remove(tempItem);
				activeTorrents_.remove(tempItem);
				inactiveTorrents_.remove(tempItem);
				
				i--;
			}
		}

		for (int i = 0; i < itemList.size(); i++) {
			tempItem = itemList.get(i);
			
			torrents_.add(tempItem);
			addTorrent(tempItem);
		}
		Collections.sort(list_);

		lvTorrent_.invalidateViews();
	}

	private void addTorrent(TorrentListItem item) {
		if (isDownloading(item)) {
			if (!downloadingTorrents_.contains(item)) {
				downloadingTorrents_.add(item);
			}
			completedTorrents_.remove(item);
		} else {
			if (!completedTorrents_.contains(item)) {
				completedTorrents_.add(item);
			}
			downloadingTorrents_.remove(item);
		}
		if (isActive(item)) {
			if (!activeTorrents_.contains(item)) {
				activeTorrents_.add(item);
			}
			inactiveTorrents_.remove(item);
		} else {
			if (!inactiveTorrents_.contains(item)) {
				inactiveTorrents_.add(item);
			}
			activeTorrents_.remove(item);
		}
	}
	
	private boolean isDownloading(TorrentListItem item) {
		if (item.getStatus() == R.string.status_finished || item.getStatus() == R.string.status_seeding) {
			return false;
		}
		return true;
	}
	
	private boolean isActive(TorrentListItem item) {
		if (item.getStatus() == R.string.status_stopped || item.getStatus() == R.string.status_finished) {
			return false;
		}
		return true;
	}
	
	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		if (viewId_ != itemPosition) {
			viewId_ = itemPosition;
			resetTorrentListAdapter();
			return true;
		}
		return false;
	}
	
	private void resetTorrentListAdapter() {
		switch (viewId_) {
			case VIEW_COMPLETED:
				list_ = completedTorrents_;
				break;
			
			case VIEW_DOWNLOADING:
				list_ = downloadingTorrents_;
				break;
				
			case VIEW_ACTIVE:
				list_ = activeTorrents_;
				break;
			
			case VIEW_INACTIVE:
				list_ = inactiveTorrents_;
				break;

			default:
				list_ = torrents_;
				break;
		}
		Collections.sort(list_);
		adapter_ = new TorrentListAdapter<TorrentListItem>(this, list_);
		lvTorrent_.setAdapter(adapter_);
	}
}