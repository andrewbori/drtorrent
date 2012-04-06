package hu.bute.daai.amorg.drtorrent.activity;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.TorrentListAdapter;
import hu.bute.daai.amorg.drtorrent.TorrentListItem;
import hu.bute.daai.amorg.drtorrent.service.TorrentService;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class DrTorrentActivity extends TorrentHostActivity {

	private ListView lvTorrent_;
	private ArrayList<TorrentListItem> torrents_;
	private ArrayAdapter<TorrentListItem> adapter_;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		torrents_ = new ArrayList<TorrentListItem>();
		lvTorrent_ = (ListView) findViewById(R.id.main_lvTorrent);
		adapter_ = new TorrentListAdapter<TorrentListItem>(this, torrents_);
		lvTorrent_.setAdapter(adapter_);
		
		lvTorrent_.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				TorrentListItem item = adapter_.getItem(position);
				String infoHash = item.getInfoHash();
				
				Intent intent = new Intent(activity_, TorrentDetailsActivity.class);
				intent.putExtra(KEY_INFO_HASH, infoHash);
				startActivity(intent);
				/*
				Message msg = Message.obtain();
				Bundle bundle = new Bundle();
				bundle.putString(TorrentService.MSG_KEY_TORRENT_INFOHASH, infoHash);
				msg.setData(bundle);
				msg.what = TorrentService.MSG_STOP_TORRENT;
				try {
					serviceMessenger_.send(msg);
				} catch (RemoteException e) {}
				*/
			}
		});
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
				} catch (Exception e) {}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(Menu.NONE, MENU_ADD_TORRENT, Menu.NONE, R.string.menu_addtorrent);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		super.onMenuItemSelected(featureId, item);
		switch (item.getItemId()) {
			case MENU_ADD_TORRENT:
				Intent i = new Intent(this, FileBrowserActivity.class);
				startActivityForResult(i, RESULT_FILEBROWSER_ACTIVITY);
				break;
		}
		return true;
	}

	@Override
	protected void refreshTorrentItem(TorrentListItem item) {
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
	}

	@Override
	protected void refreshTorrentList(ArrayList<TorrentListItem> itemList) {
		boolean foundOld = false;
		for (int i = 0; i < adapter_.getCount(); i++) {
			foundOld = false;
			for (int j = 0; j < itemList.size(); j++) {
				if (adapter_.getItem(i).compareTo(itemList.get(j)) == 0) {
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