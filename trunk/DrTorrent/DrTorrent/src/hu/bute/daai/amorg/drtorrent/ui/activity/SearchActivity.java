package hu.bute.daai.amorg.drtorrent.ui.activity;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.ui.adapter.SearchResultListAdapter;
import hu.bute.daai.amorg.drtorrent.ui.adapter.item.SearchResultListItem;
import hu.bute.daai.amorg.drtorrent.ui.provider.TorrentSuggestionProvider;
import hu.bute.daai.amorg.drtorrent.util.Preferences;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class SearchActivity extends SherlockActivity {
	
	private final static int MENU_SETTINGS		  = 1;
	private final static int MENU_SEARCH_TORRENT  = 2;
	private final static int MENU_SEARCH_TORRENT2 = 3;
	private final static int MENU_CLEAR_HISTORY   = 4;
	private final static int MENU_SHOW_RESULTS	  = 5;
	private final static int MENU_REFRESH		  = 6;
	
	private SearchView searchView_ = null;
	private SearchTask searchTask_ = null;
	
	private String query_ = null;
	private ListView lvResults_ = null;
	private ArrayList<SearchResultListItem> items_ = null;
	private ProgressBar progress_ = null;
	private TextView tvMessage_ = null;
	private ProgressDialog progressDialog_ = null;
	
	private AlertDialog dialog_ = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.Theme_Sherlock_ForceOverflow);
		super.onCreate(savedInstanceState);
		
		init();
		
		handleIntent(getIntent());
	}
	
	private void init() {
		setContentView(R.layout.search);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		//setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
		
		int visibility;
		visibility = (lvResults_ != null) ? lvResults_.getVisibility() : ListView.VISIBLE;
		lvResults_ = (ListView) findViewById(R.id.search_lvResults);
		lvResults_.setVisibility(visibility);
		
		visibility = (tvMessage_ != null) ? tvMessage_.getVisibility() : TextView.GONE;
		tvMessage_ = (TextView) findViewById(R.id.search_tvMessage); 
		tvMessage_.setVisibility(visibility);
		
		visibility = (progress_ != null) ? progress_.getVisibility() : ProgressBar.GONE;
		progress_ = (ProgressBar) findViewById(R.id.search_progress);
		progress_.setVisibility(visibility);
		
		lvResults_.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				
				final SearchResultListItem item = (SearchResultListItem) lvResults_.getItemAtPosition(position);
				
				final CharSequence[] items = { getString(R.string.details), getString(R.string.download) };
				
				AlertDialog.Builder builder = new AlertDialog.Builder(SearchActivity.this);
				builder.setTitle(item.getName()).
				setItems(items, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
							case 0:
								try {
									Uri uri = Uri.parse(item.getDetailUrl());
									Intent intent = new Intent(Intent.ACTION_VIEW, uri);
									intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
									startActivity(intent);
								} catch (Exception ex) { }
								break;
								
							case 1:
								downloadTorrent(item.getTorrentUrl());
								break;
								
							default:
								break;
						}
					}
				});
				dialog_ = builder.create();
				dialog_.show();
				
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
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		handleIntent(intent);
	}
	
	@Override
	public boolean onSearchRequested() {
		startSearch(query_, false, null, false);
	    return true;
	}
	
	/** Handles the incoming intent. */
	private void handleIntent(Intent intent) {
		if (intent != null && Intent.ACTION_SEARCH.equals(intent.getAction())) {
	    	query_ = intent.getStringExtra(SearchManager.QUERY);
	    	
	    	SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, TorrentSuggestionProvider.AUTHORITY, TorrentSuggestionProvider.MODE);
	    	suggestions.saveRecentQuery(query_, null);
	    	
	    	startSearchTask();
	    }
	}
	
	private void startSearchTask() {
		if (searchTask_ != null && searchTask_.getStatus() == AsyncTask.Status.RUNNING) {
    		searchTask_.cancel(true);	
    	}
    	searchTask_ = new SearchTask();
    	searchTask_.execute(query_, Preferences.getSearchEngine(this));
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		init();
		
		if (items_ != null) {
			lvResults_.setAdapter(new SearchResultListAdapter<SearchResultListItem>(this, items_));
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		try {
			 // Get the SearchView and set the searchable configuration
			searchView_ = new SearchView(this);
			SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
	    	searchView_.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		    searchView_.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
		    searchView_.setQueryRefinementEnabled(true);
		     
		    searchView_.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
				@Override
				public boolean onSuggestionSelect(int position) {
					return false;
				}
				
				@Override
				public boolean onSuggestionClick(int position) {
					if (searchView_ != null) {
						Cursor cursor = null;
						try {
				    		cursor = searchView_.getSuggestionsAdapter().getCursor();
				    		if (cursor.moveToPosition(position)) {
					    		query_ = cursor.getString(2);
					    		searchView_.setQuery(query_, true);
					    		return true;
				    		}
						} catch (Exception e) {
						} finally {
							if (cursor != null) {
								cursor.close();
							}
						}
				    }
					return false;
				}
			});
		    
		    menu.add(Menu.NONE, MENU_SEARCH_TORRENT2, Menu.NONE, R.string.search)
				.setIcon(R.drawable.ic_menu_search)
				.setActionView(searchView_)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		    
		    if (query_ != null) {
		    	searchView_.setQuery(query_, false);
		    }
		    
		} catch (NoClassDefFoundError e) {
			menu.add(Menu.NONE, MENU_SEARCH_TORRENT, Menu.NONE, R.string.search)
				.setIcon(R.drawable.ic_menu_search)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	    }
	
		menu.add(Menu.NONE, MENU_REFRESH, Menu.NONE, R.string.refresh)
			.setIcon(R.drawable.ic_menu_refresh)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
		menu.add(Menu.NONE, MENU_SHOW_RESULTS, Menu.NONE, R.string.show_results_in_browser)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		
		menu.add(Menu.NONE, MENU_CLEAR_HISTORY, Menu.NONE, R.string.clear_history)
			.setIcon(R.drawable.ic_menu_delete)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		
		menu.add(Menu.NONE, MENU_SETTINGS, Menu.NONE, R.string.search_engine)
			.setIcon(R.drawable.ic_menu_settings)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		
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
				// Strings to Show In Dialog with Radio Buttons
				final String[] items = getResources().getStringArray(R.array.search_engine_names);
				final String[] values = getResources().getStringArray(R.array.search_engine_values);
				final String selected = Preferences.getSearchEngine(this);
				
				int selectedIndex = -1; 
				for (int i = 0; i < values.length; i++ ) {
					if (values[i].equals(selected)) {
						selectedIndex = i;
						break;
					}
				}
				
                // Creating and Building the Dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(SearchActivity.this);
                builder.setTitle(R.string.search_engine);
                builder.setSingleChoiceItems(items, selectedIndex, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int item) {
	                	if (item < values.length) {
	                		String selectedValue = values[item];
	                		Preferences.setSearchEngine(selectedValue);
	                	}
	                	
	                    dialog_.dismiss();
	                }});
                dialog_ = builder.create();
				dialog_.show();
				break;
				
			case MENU_SEARCH_TORRENT:
				onSearchRequested();
				break;
				
			case MENU_CLEAR_HISTORY:
				builder = new AlertDialog.Builder(SearchActivity.this);
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
				});
				dialog_ = builder.create();
				dialog_.show();
				
				break;
				
			case MENU_SHOW_RESULTS:
				if (query_ != null) {
					try {
						String url = String.format(Preferences.getSearchEngineUrl(this), query_);
						Uri uri = Uri.parse(url);
						Intent intent = new Intent(Intent.ACTION_VIEW, uri);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);
					} catch (Exception e) {
					}
				}
				break;
				
			case MENU_REFRESH:
				startSearchTask();
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
	            Intent intent = new Intent(this, MainActivity.class);
	            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            startActivity(intent);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
		super.onBackPressed();
	}
	
	private void downloadTorrent(String url) {
		Uri uri = Uri.parse(url);
		Intent intent = new Intent(this, MainActivity.class);
		intent.setData(uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
	}
	
	public class SearchTask extends AsyncTask<String, Integer, ArrayList<SearchResultListItem>> {
		
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
					Uri uri = Uri.parse("content://org.transdroid.search.TorrentSearchProvider/search/" + query);
					ContentProviderClient client = getContentResolver().acquireContentProviderClient(uri);
					if (client != null) {
						result = client.query(uri, null, "SITE = ?", new String[] { siteCode }, "BySeeders");
					} else {
						return null;
					}
				} catch (Exception e) {
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
			items_ = results;
			
			if (results != null && !results.isEmpty()) {
				ArrayAdapter<SearchResultListItem> adapter = new SearchResultListAdapter<SearchResultListItem>(SearchActivity.this, results);
				lvResults_.setAdapter(adapter);
				
				lvResults_.setVisibility(ListView.VISIBLE);
			} else {
				tvMessage_.setVisibility(TextView.VISIBLE);
			}
			
			super.onPostExecute(results);
		}
	}
	
	@Override
	protected void onDestroy() {
		if (searchTask_ != null && searchTask_.getStatus() == AsyncTask.Status.RUNNING) {
    		searchTask_.cancel(true);
    		searchTask_ = null;
    	}
		if (dialog_ != null) {
			dialog_.dismiss();
			dialog_ = null;
		}
		super.onDestroy();
	}
}
