package hu.bute.daai.amorg.drtorrent.activity;

import hu.bute.daai.amorg.drtorrent.Preferences;
import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.adapter.item.FileListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.PeerListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.TorrentListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.TrackerListItem;
import hu.bute.daai.amorg.drtorrent.fragment.TorrentDetailsFragment;
import hu.bute.daai.amorg.drtorrent.fragment.TorrentListFragment;
import hu.bute.daai.amorg.drtorrent.fragment.TorrentListFragment.TorrentItemInteractionListener;
import hu.bute.daai.amorg.drtorrent.service.TorrentClient;
import hu.bute.daai.amorg.drtorrent.service.TorrentService;
import hu.bute.daai.amorg.drtorrent.torrentengine.Bitfield;

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
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class MainActivity extends TorrentHostActivity implements OnNavigationListener, TorrentItemInteractionListener {
	
	private final static int VIEW_ALL 		  = 0;
	private final static int VIEW_DOWNLOADING = 1;
	private final static int VIEW_COMPLETED   = 2;
	private final static int VIEW_ACTIVE	  = 3;
	private final static int VIEW_INACTIVE    = 4;
	
	TorrentListFragment torrentListFragment_ = null;
	TorrentDetailsFragment torrentDetailsFragment_ = null;
	
	private ArrayList<TorrentListItem> torrents_ = null;
	private ArrayList<TorrentListItem> completedTorrents_ = null;
	private ArrayList<TorrentListItem> downloadingTorrents_ = null;
	private ArrayList<TorrentListItem> activeTorrents_ = null;
	private ArrayList<TorrentListItem> inactiveTorrents_ = null;
	private ArrayList<TorrentListItem> list_ = null;
	
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
		Preferences.set(getApplicationContext());
		
		if (!Preferences.wasAnalyticsAsked()) {
			LayoutInflater inflater = MainActivity.this.getLayoutInflater();
			ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.dialog_checkbox, null);
			((TextView) layout.findViewById(R.id.dialog_checkbox_message)).setText(R.string.analytics_message);
			final CheckBox cbSendStatistics = (CheckBox) layout.findViewById(R.id.dialog_checkbox_checkbox);
			cbSendStatistics.setText(R.string.analytics_checkbox);
			cbSendStatistics.setChecked(true);
			
			AlertDialog.Builder builder = new AlertDialog.Builder(context_);
			builder.setTitle(R.string.analytics_title).
			setView(layout).
			setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Preferences.setAnalitics(cbSendStatistics.isChecked());
					Preferences.setAnalyticsAsked(true);
					dialog.dismiss();
				}
			});
			dialog_ = builder.create();
			dialog_.show();
		}
	}
	
	private void init() {
		if (getResources().getBoolean(R.bool.has_two_panes)) {
			setContentView(R.layout.main_twopanes);
		} else {
			setContentView(R.layout.main_onepane);
		}
		
		getSupportActionBar().setDisplayShowTitleEnabled(false);

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
		
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		
		if (findViewById(R.id.torrent_list_fragment) != null) { 
			torrentListFragment_ = new TorrentListFragment();
			transaction.add(R.id.torrent_list_fragment, torrentListFragment_);
		}
		
		if (findViewById(R.id.torrent_deatils_fragment) != null) {
			torrentDetailsFragment_ = new TorrentDetailsFragment();
			
			transaction.add(R.id.torrent_deatils_fragment, torrentDetailsFragment_);
		}
		
		transaction.commitAllowingStateLoss();
		
		if (torrentListFragment_ != null) {
			torrentListFragment_.setTorrentList(list_);
			if (torrentDetailsFragment_ != null && torrentId_ != -1) {
				torrentListFragment_.setSelectedItem(torrentId_);
			}
		}
		
		clientType_ = TorrentClient.CLIENT_TYPE_ALL_AND_SINGLE;
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
		
		if (torrentListFragment_ != null && torrentListFragment_.isAdded()) {
			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
			transaction.remove(torrentListFragment_);
			transaction.commitAllowingStateLoss();
		}
		
		if (torrentDetailsFragment_ != null && torrentDetailsFragment_.isAdded()) {
			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
			transaction.remove(torrentDetailsFragment_);
			transaction.commitAllowingStateLoss();
		}
		
		torrentListFragment_ = null;
		torrentDetailsFragment_ = null;
		
		init();
		resetTorrentListAdapter();
		
		if (torrentDetailsFragment_ != null && torrentId_ != -1) {
			subscribeToTorrent(torrentId_);
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
			
			case RESULT_TORRENT_CREATOR:
				if (data != null && resultCode == Activity.RESULT_OK) {
					String torrentPath = data.getStringExtra(TorrentCreatorActivity.RESULT_KEY_TORRENT_PATH);
					String dataPath = data.getStringExtra(TorrentCreatorActivity.RESULT_KEY_DATA_PATH);
					
					openTorrent(torrentPath, dataPath);
				}
				
				break;
				
			default:
		}
	}
	
	/** Sends torrent file open request to the Torrent Service. */
	protected void openTorrent(String torrentPath, String dataPath) {
		fileToOpen_ = null;
		
		Message msg = Message.obtain();
		Bundle b = new Bundle();
		b.putString(TorrentService.MSG_KEY_FILEPATH, torrentPath);
		b.putString(TorrentService.MSG_KEY_DATAPATH, dataPath);
		msg.what = TorrentService.MSG_OPEN_TORRENT_AND_SEED;
		msg.setData(b);
		try {
			serviceMessenger_.send(msg);
		} catch (Exception e) {}
	}
		
	
	/** Sends torrent file open request to the Torrent Service. */
	@Override
	protected void openTorrent(Uri torrentUri) {
		
		fileToOpen_ = null;
		
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
				
				Intent intent = new Intent(getApplicationContext(), MainActivity.class);
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
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		    
		} catch (NoClassDefFoundError e) {
			menu.add(Menu.NONE, MENU_SEARCH_TORRENT, Menu.NONE, R.string.search)
				.setIcon(R.drawable.ic_menu_search)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	    }
		
		menu.add(Menu.NONE, MENU_ADD_TORRENT, Menu.NONE, R.string.menu_addtorrent)
			.setIcon(R.drawable.ic_menu_add2)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		
		menu.add(Menu.NONE, MENU_CREATE_TORRENT, Menu.NONE, R.string.create_torrent)
			//.setIcon(R.drawable.ic_menu_add2)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		
		menu.add(Menu.NONE, MENU_ADD_MAGNET_LINK, Menu.NONE, R.string.add_magnet_link)
			//.setIcon(R.drawable.ic_menu_magnet)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		
		menu.add(Menu.NONE, MENU_SETTINGS, Menu.NONE, R.string.settings)
			.setIcon(R.drawable.ic_menu_settings)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
		menu.add(Menu.NONE, MENU_SHARE_DRTORRENT, Menu.NONE, R.string.share)
			//.setIcon(R.drawable.ic_menu_email)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		
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
				intent.putExtra(FileBrowserActivity.KEY_EXTENSIONS, new String[] { ".torrent" });
				startActivityForResult(intent, RESULT_FILEBROWSER_ACTIVITY);
				break;
				
			case MENU_SEARCH_TORRENT:
				onSearchRequested();
				break;
				
			case MENU_SETTINGS:
				intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				break;
				
			case MENU_ADD_MAGNET_LINK:
				addMagnetLink();
				break;
				
			case MENU_CREATE_TORRENT:
				intent = new Intent(this, TorrentCreatorActivity.class);
				startActivityForResult(intent, RESULT_TORRENT_CREATOR);
				break;
				
			case MENU_SHARE_DRTORRENT:
				Intent sendIntent = new Intent();
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.putExtra(Intent.EXTRA_SUBJECT, "DrTorrent");
				sendIntent.putExtra(Intent.EXTRA_TEXT,  "https://play.google.com/store/apps/details?id=hu.bute.daai.amorg.drtorrent");
				sendIntent.setType("text/plain");
				startActivity(sendIntent);
				break;
				
			case MENU_ABOUT:
				LayoutInflater inflater = getLayoutInflater();
				View layout = inflater.inflate(R.layout.dialog_about, null);

				AlertDialog.Builder builder = new AlertDialog.Builder(context_);
				builder.setTitle(getString(R.string.about_drtorrent)).
				setView(layout);
				TextView tvFeedback = (TextView) layout.findViewById(R.id.dialog_about_tvFeedback);
				tvFeedback.setText(Html.fromHtml("<a href=\"mailto:andreasweiner@gmail.com\">" + getText(R.string.feedback) + "</a>"));
				tvFeedback.setMovementMethod(LinkMovementMethod.getInstance());
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
		if (item != null) {
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
						int index = -1;
						if (torrentId_ == tempItem.getId()) {
							index = list_.indexOf(tempItem);
						}
						
						torrents_.remove(tempItem);
						completedTorrents_.remove(tempItem);
						downloadingTorrents_.remove(tempItem);
						activeTorrents_.remove(tempItem);
						inactiveTorrents_.remove(tempItem);
						
						if (index != -1) {
							if (list_.size() > index) {
								torrentId_ = list_.get(index).getId();
							} else {
								torrentId_ = -1;
							}
							if (torrentDetailsFragment_ != null && torrentListFragment_ != null) {
								torrentDetailsFragment_.reset();
								torrentListFragment_.setSelectedItem(torrentId_);
								
								subscribeToTorrent(torrentId_);
							}
						}
					}
					break;
				}
			}
			if (!found && !isRemoved) {
				torrents_.add(item);
				addTorrent(item);
			}
			Collections.sort(list_);
	
			torrentListFragment_.onTorrentListChanged();		
			
			if (torrentId_ == item.getId() && torrentDetailsFragment_ != null) {
				torrentDetailsFragment_.refreshTorrentItem(item, isRemoved);
			}
		}
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
		
		if (torrentDetailsFragment_ != null && torrentId_ == -1 && !list_.isEmpty()) {
			torrentId_ = list_.get(0).getId();
			if (torrentListFragment_ != null) {
				torrentListFragment_.setSelectedItem(torrentId_);
			}
			torrentDetailsFragment_.reset();
			subscribeToTorrent(torrentId_);
		}

		torrentListFragment_.onTorrentListChanged();
	}

	private void addTorrent(TorrentListItem item) {
		if (item != null) {
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
	
	/** Resets the list adapter. */
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

		if (torrentId_ == -1 && !list_.isEmpty()) {
			torrentId_ = list_.get(0).getId();
		}
		if (torrentListFragment_ != null) {
			torrentListFragment_.setTorrentList(list_);
			if (torrentDetailsFragment_ != null && torrentId_ != -1) {
				torrentListFragment_.setSelectedItem(torrentId_);
			}
		}
	}

	/** Sends a change torrent message to the torrent service. */
	private void subscribeToTorrent(int torrentId) {
		Message msg = Message.obtain();
		Bundle b = new Bundle();
		b.putInt(TorrentService.MSG_KEY_CLIENT_ID, clientId_);
		b.putInt(TorrentService.MSG_KEY_TORRENT_ID, torrentId);
		
		msg.what = TorrentService.MSG_CHANGE_TORRENT;
		msg.replyTo = clientMessenger_;
		msg.setData(b);
		try {
			serviceMessenger_.send(msg);
		} catch (Exception e) {}
	}
	
	@Override
	public void onTorrentItemSelected(int torrentId) {
		torrentId_ = torrentId;
		
		if (torrentDetailsFragment_ != null) {
			torrentDetailsFragment_.reset();
			subscribeToTorrent(torrentId);
			
		} else {			
			Intent intent = new Intent(context_, TorrentDetailsActivity.class);
			intent.putExtra(KEY_TORRENT_ID, torrentId);
			startActivity(intent);
		}
	}

	@Override
	public void onTorrentItemLongClicked(final TorrentListItem item) {
		if (torrentDetailsFragment_ != null) {
			onTorrentItemSelected(item.getId());
		}
		
		CharSequence[] items = null;
		if (item.getStatus() == R.string.status_stopped || item.getStatus() == R.string.status_finished) {
			items = new CharSequence[] { getString(R.string.start), getString(R.string.remove), getString(R.string.menu_add_peer), getString(R.string.menu_add_tracker), getString(R.string.share_torrent), getString(R.string.share_magnet) };
		} else {
			items = new CharSequence[] { getString(R.string.stop), getString(R.string.remove), getString(R.string.menu_add_peer), getString(R.string.menu_add_tracker), getString(R.string.share_torrent), getString(R.string.share_magnet) };
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(context_);
		builder.setTitle(item.getName()).
		setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final Message msg = Message.obtain();
				final Bundle bundle = new Bundle();
				bundle.putInt(TorrentService.MSG_KEY_TORRENT_ID, item.getId());
				msg.setData(bundle);
				
				switch (which) {
					// Stop/Start
					case 0:
						if (item.getStatus() == R.string.status_stopped || item.getStatus() == R.string.status_finished) {
							msg.what = TorrentService.MSG_START_TORRENT;
						}
						else {
							msg.what = TorrentService.MSG_STOP_TORRENT;
						}
						try {
							serviceMessenger_.send(msg);
						} catch (RemoteException e) {}
						break;
						
					// Remove
					case 1:
						LayoutInflater inflater = MainActivity.this.getLayoutInflater();
						ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.dialog_checkbox, null);
						((TextView) layout.findViewById(R.id.dialog_checkbox_message)).setText(R.string.remove_dialog_title);
						final CheckBox cbDeleteFiles = (CheckBox) layout.findViewById(R.id.dialog_checkbox_checkbox);
						cbDeleteFiles.setText(R.string.delete_downloaded_files);
						
						AlertDialog.Builder builder = new AlertDialog.Builder(context_);
						builder.setTitle(item.getName()).
						setView(layout).
						setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								bundle.putBoolean(TorrentService.MSG_KEY_DELETE_FILES, cbDeleteFiles.isChecked());
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
					
					// Add peer
					case 2:
						addPeer(item.getId());
						break;
						
					// Add tracker
					case 3:
						addTracker(item.getId());
						break;
						
					case 4:
						sendShareMessage(item.getId(), true);
						break;
						
					case 5:
						sendShareMessage(item.getId(), false);
						break;
						
					default:
				}
			}
		});
		dialog_ = builder.create();
		dialog_.show();
	}
	
	@Override
	protected void refreshPeerList(ArrayList<PeerListItem> itemList) {
		if (torrentDetailsFragment_ != null) {
			torrentDetailsFragment_.refreshPeerList(itemList);
		}
	}
	
	@Override
	protected void refreshFileList(ArrayList<FileListItem> itemList) {
		if (torrentDetailsFragment_ != null) {
			torrentDetailsFragment_.refreshFileList(itemList);
		}
	}
	
	@Override
	protected void refreshTrackerList(ArrayList<TrackerListItem> itemList) {
		if (torrentDetailsFragment_ != null) {
			torrentDetailsFragment_.refreshTrackerList(itemList);
		}
	}
	
	@Override
	protected void refreshBitfield(Bitfield bitfield, Bitfield downloadingBitfield) {
		if (torrentDetailsFragment_ != null) {
			torrentDetailsFragment_.refreshBitfield(bitfield, downloadingBitfield);
		}
	}
}
