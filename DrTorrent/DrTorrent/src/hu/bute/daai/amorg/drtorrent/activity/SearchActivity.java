package hu.bute.daai.amorg.drtorrent.activity;

import hu.bute.daai.amorg.drtorrent.Preferences;
import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.adapter.SearchResultListAdapter;
import hu.bute.daai.amorg.drtorrent.adapter.item.SearchResultListItem;
import hu.bute.daai.amorg.drtorrent.network.HttpConnection;
import hu.bute.daai.amorg.drtorrent.provider.TorrentSuggestionProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ContentProviderClient;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.SearchRecentSuggestions;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class SearchActivity extends SherlockActivity {
	
	private final int MENU_SETTINGS		  = 1;
	private final int MENU_SEARCH_TORRENT = 2;
	private final int MENU_CLEAR_HISTORY  = 3;
	
	private String query_ = null;
	private ListView lvResults_ = null;
	private ProgressBar progress_ = null;
	private TextView tvMessage_ = null;
	private ProgressDialog progressDialog_ = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.Theme_Sherlock_ForceOverflow);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		lvResults_ = (ListView) findViewById(R.id.search_lvResults);
		tvMessage_ = (TextView) findViewById(R.id.search_tvMessage); 
		progress_ = (ProgressBar) findViewById(R.id.search_progress);
		
		lvResults_.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				
				final SearchResultListItem item = (SearchResultListItem) lvResults_.getItemAtPosition(position);
				
				final CharSequence[] items = { "Details", "Download" };
				
				AlertDialog.Builder builder = new AlertDialog.Builder(SearchActivity.this);
				builder.setTitle(item.getName()).
				setItems(items, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
							case 0:
								Uri uri = Uri.parse(item.getDetailUrl());
								Intent intent = new Intent(Intent.ACTION_VIEW, uri);
								startActivity(intent);
								break;
								
							case 1:
								downloadTorrent(item.getTorrentUrl());
								break;
								
							default:
								break;
						}
					}
				}).
				create().show();
				
				return false;
			}
		});
		
		lvResults_.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final SearchResultListItem item = (SearchResultListItem) lvResults_.getItemAtPosition(position);
				
				downloadTorrent(item.getTorrentUrl());
			}
		});
		
		handleIntent(getIntent());
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		handleIntent(intent);
	}
	
	/** Handles the incoming intent. */
	private void handleIntent(Intent intent) {
		if (intent != null && Intent.ACTION_SEARCH.equals(intent.getAction())) {
	    	query_ = intent.getStringExtra(SearchManager.QUERY);
	    	
	    	SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, TorrentSuggestionProvider.AUTHORITY, TorrentSuggestionProvider.MODE);
	    	suggestions.saveRecentQuery(query_, null);
	    	
	    	new SearchTask().execute(query_, Preferences.getSearchEngine());
	    }
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        
		menu.add(Menu.NONE, MENU_SETTINGS, Menu.NONE, R.string.settings)
			.setIcon(R.drawable.ic_menu_settings)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		
		menu.add(Menu.NONE, MENU_CLEAR_HISTORY, Menu.NONE, R.string.clear_history)
			.setIcon(R.drawable.ic_menu_delete)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
		menu.add(Menu.NONE, MENU_SEARCH_TORRENT, Menu.NONE, R.string.search)
			.setIcon(R.drawable.ic_menu_search)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		
		return true;
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if (progressDialog_ != null) {
			progressDialog_.dismiss();
			progressDialog_ = null;
		}
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		super.onMenuItemSelected(featureId, item);
		
		switch (item.getItemId()) {
			case MENU_SETTINGS:
				Intent intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				break;
			case MENU_SEARCH_TORRENT:
				onSearchRequested();
				break;
				
			case MENU_CLEAR_HISTORY:
				AlertDialog.Builder builder = new AlertDialog.Builder(SearchActivity.this);
				builder.setTitle(R.string.clear_history)
				.setMessage(R.string.clear_history_message)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						SearchRecentSuggestions suggestions = new SearchRecentSuggestions(SearchActivity.this, TorrentSuggestionProvider.AUTHORITY, TorrentSuggestionProvider.MODE);
						suggestions.clearHistory();
						
						dialog.dismiss();
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.create().show();
				
				break;
				
			default:
		}
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home:
	            // app icon in action bar clicked; go home
	            Intent intent = new Intent(this, DrTorrentActivity.class);
	            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            startActivity(intent);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, DrTorrentActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
		super.onBackPressed();
	}
	
	private void downloadTorrent(String url) {
		/*Uri uri = Uri.parse(url);
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		startActivity(intent);*/
		
		Uri uri = Uri.parse(url);
		Intent intent = new Intent(this, DrTorrentActivity.class);
		intent.setData(uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
	}
	
	public class SearchTask extends AsyncTask<String, Integer, ArrayList<SearchResultListItem>> {

		private boolean shouldInstall_ = false;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			setTitle(query_);
			lvResults_.setVisibility(ListView.GONE);
			tvMessage_.setVisibility(TextView.GONE);
			progress_.setVisibility(ProgressBar.VISIBLE);
		}
		
		@Override
		protected ArrayList<SearchResultListItem> doInBackground(String... params) {
			ArrayList<SearchResultListItem> results = null;
			if (params.length > 1) {
				String query = params[0];
				String siteCode = params[1];
				
				Cursor result = null;
				try {
					Uri uri = Uri.parse("content://org.transdroid.search.torrentsearchprovider/search/" + query);
					ContentProviderClient client = getContentResolver().acquireContentProviderClient(uri);
					if (client != null) {
						result = client.query(uri, null, "SITE = ?", new String[] { siteCode }, "BySeeders");
					} else {
						shouldInstall_ = true;
						return null;
					}
				} catch (Exception e) {
					shouldInstall_ = true;
					return null;
				}
				
				results = new ArrayList<SearchResultListItem>();
				if (result != null) {
					if (result.moveToFirst()) {
						do {
							// { "_ID", "NAME", "TORRENTURL", "DETAILSURL", "SIZE", "ADDED", "SEEDERS", "LEECHERS" };
							results.add(new SearchResultListItem(result.getString(1), result.getString(2), result.getString(3),
																 result.getString(4), result.getString(5), result.getString(6),
																 result.getString(7)));
						} while (result.moveToNext());
					}
					result.close();
					result = null;
				}
			}
			return results;
		}
		
		@Override
		protected void onPostExecute(ArrayList<SearchResultListItem> results) {
			progress_.setVisibility(ProgressBar.GONE);
			
			if (results == null && shouldInstall_) {
				new ApkInstallerTask().execute("http://transdroid-search.googlecode.com/files/transdroid-search-1.10.apk");
			} else {
				if (results != null && !results.isEmpty()) {
					ArrayAdapter<SearchResultListItem> adapter = new SearchResultListAdapter<SearchResultListItem>(SearchActivity.this, results);
					lvResults_.setAdapter(adapter);
					
					lvResults_.setVisibility(ListView.VISIBLE);
				} else {
					tvMessage_.setVisibility(TextView.VISIBLE);
				}
			}
			
			super.onPostExecute(results);
		}
		
	}
	
	/** Task that downloads and installs the apk with the given url. */
	public class ApkInstallerTask extends AsyncTask<String, Integer, Boolean> {
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (progressDialog_ == null) {
				progressDialog_ = new ProgressDialog(SearchActivity.this);
			}
			progressDialog_.setMessage("Downloading the APK file...");
			progressDialog_.show();
		}
		
		@Override
		protected Boolean doInBackground(String... params) {
			if (params.length > 0) {
				final String url = params[0];
				final String filename = url.substring(url.lastIndexOf("/"));
				
				HttpConnection conn = new HttpConnection(url);
				
				byte[] content = conn.execute();
				if (content == null) {
					return false;
				}
				
				final String path = Environment.getExternalStorageDirectory().getPath().concat(filename);
				FileOutputStream fos = null;
				try {
					fos = new FileOutputStream(path);
					fos.write(content);
					fos.flush();
				} catch (Exception e) {
					return false;
				} finally {
					try {
						fos.close();
					} catch (Exception e) {
					}
				}
				
				Intent intent = new Intent(Intent.ACTION_VIEW);
			    intent.setDataAndType(Uri.fromFile(new File(path)), "application/vnd.android.package-archive");
			    startActivity(intent);
				
				return true;
			}
			return false;
		}
		
		 @Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (progressDialog_ != null) {
				progressDialog_.dismiss();
				progressDialog_ = null;
			}
		}
	}
}
