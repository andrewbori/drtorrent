package hu.bute.daai.amorg.drtorrent.activity;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.TorrentDetailsPagerAdapter;
import hu.bute.daai.amorg.drtorrent.adapter.item.FileListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.PeerListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.TorrentListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.TrackerListItem;
import hu.bute.daai.amorg.drtorrent.service.TorrentService;
import hu.bute.daai.amorg.drtorrent.torrentengine.Bitfield;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.viewpagerindicator.TitlePageIndicator;

public class TorrentDetailsActivity extends TorrentHostActivity {

	private TorrentDetailsPagerAdapter pagerAdapter_;
	private int updateField_ = 0;
	
	boolean isStopped_ = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		setContentView(R.layout.torrent_details);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		pagerAdapter_ = new TorrentDetailsPagerAdapter(this);
        ViewPager viewPager = (ViewPager) findViewById(R.id.torrent_details_viewpager);
		TitlePageIndicator indicator = (TitlePageIndicator) findViewById(R.id.torrent_details_indicator);
        viewPager.setAdapter(pagerAdapter_);
        indicator.setViewPager(viewPager);
	}
	
	
	@Override
	protected void onStart() {
		super.onStart();
		updateTorrent(updateField_);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!isStopped_) {
			menu.add(Menu.NONE, MENU_STOP_TORRENT, 0, R.string.menu_stop)
				.setIcon(R.drawable.ic_pause)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		} else {
			menu.add(Menu.NONE, MENU_START_TORRENT, 0, R.string.menu_start)
				.setIcon(R.drawable.ic_play)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		}
		menu.add(Menu.NONE, MENU_DELETE_TORRENT, 1, R.string.menu_delete)
			.setIcon(R.drawable.ic_start)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		if (!isStopped_) {
			menu.add(Menu.NONE, MENU_STOP_TORRENT, 0, R.string.menu_stop)
				.setIcon(R.drawable.ic_pause)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		} else {
			menu.add(Menu.NONE, MENU_START_TORRENT, 0, R.string.menu_start)
			.setIcon(R.drawable.ic_play)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		}
		menu.add(Menu.NONE, MENU_DELETE_TORRENT, 1, R.string.menu_delete)
			.setIcon(R.drawable.ic_delete)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

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
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		super.onMenuItemSelected(featureId, item);
		
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putString(TorrentService.MSG_KEY_TORRENT_INFOHASH, infoHash_);
		msg.setData(bundle);
		
		switch (item.getItemId()) {
			case MENU_START_TORRENT:
				msg.what = TorrentService.MSG_START_TORRENT;
				try {
					serviceMessenger_.send(msg);
				} catch (RemoteException e) {}
				break;
			
			case MENU_STOP_TORRENT:
				msg.what = TorrentService.MSG_STOP_TORRENT;
				try {
					serviceMessenger_.send(msg);
				} catch (RemoteException e) {}
				break;
				
			case MENU_DELETE_TORRENT:
				msg.what = TorrentService.MSG_CLOSE_TORRENT;
				try {
					serviceMessenger_.send(msg);
				} catch (RemoteException e) {}
				Intent intent = new Intent(this, DrTorrentActivity.class);
	            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            startActivity(intent);	            
				break;
		}
		return true;
	}

	/** Sends torrent file open request to the Torrent Service. */
	public void updateTorrent(int updateField) {
		updateField_ = updateField;
		Message msg = Message.obtain();
		Bundle b = new Bundle();
		b.putInt(TorrentService.MSG_KEY_TORRENT_UPDATE, updateField);
		msg.what = TorrentService.MSG_UPDATE_TORRENT;
		msg.setData(b);
		try {
			serviceMessenger_.send(msg);
		} catch (Exception e) {}
	}
	
	/** Changes the priority of a file. */
	public void changeFilePriority(FileListItem item) {
		Message msg = Message.obtain();
		Bundle b = new Bundle();
		b.putString(TorrentService.MSG_KEY_TORRENT_INFOHASH, infoHash_);
		b.putSerializable(TorrentService.MSG_KEY_FILE_ITEM, item);
		msg.what = TorrentService.MSG_CHANGE_FILE_PRIORITY;
		msg.setData(b);
		try {
			serviceMessenger_.send(msg);
		} catch (Exception e) {}
	}
	
	@Override
	protected void refreshTorrentItem(TorrentListItem item, boolean isRemoved) {
		if (isStopped_) {
			if (item.getStatus() != R.string.status_stopped) {
				isStopped_ = false;
				invalidateOptionsMenu();
			}
		} else {
			if (item.getStatus() == R.string.status_stopped) {
				isStopped_ = true;
				invalidateOptionsMenu();
			}
		}

		pagerAdapter_.refreshTorrentItem(item, isRemoved);
	}
	
	@Override
	protected void refreshPeerList(ArrayList<PeerListItem> itemList) {
		pagerAdapter_.refreshPeerList(itemList);
	}
	
	@Override
	protected void refreshFileList(ArrayList<FileListItem> itemList) {
		pagerAdapter_.refreshFileList(itemList);
	}
	
	@Override
	protected void refreshTrackerList(ArrayList<TrackerListItem> itemList) {
		pagerAdapter_.refreshTrackerList(itemList);
	}
	
	@Override
	protected void refreshBitfield(Bitfield bitfield, Bitfield downloadingBitfield) {
		pagerAdapter_.refreshBitfield(bitfield, downloadingBitfield);
	}
}
