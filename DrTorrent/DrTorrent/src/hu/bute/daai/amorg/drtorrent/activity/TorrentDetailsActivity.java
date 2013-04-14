package hu.bute.daai.amorg.drtorrent.activity;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.adapter.TorrentPagerAdapter;
import hu.bute.daai.amorg.drtorrent.adapter.item.FileListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.PeerListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.TorrentListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.TrackerListItem;
import hu.bute.daai.amorg.drtorrent.service.TorrentClient;
import hu.bute.daai.amorg.drtorrent.service.TorrentService;
import hu.bute.daai.amorg.drtorrent.torrentengine.Bitfield;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class TorrentDetailsActivity extends TorrentHostActivity {
	
	private TorrentPagerAdapter pagerAdapter_ = null;
	
	boolean isStopped_ = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// If we are in two-pane layout mode, this activity is no longer necessary
        if (getResources().getBoolean(R.bool.has_two_panes)) {
        	Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(MainActivity.KEY_TORRENT_ID, torrentId_);
            startActivity(intent);
            
            isShuttingDown_ = true;
            return;
        }
		
		setContentView(R.layout.torrent_details);
		getSupportActionBar().setDisplayShowTitleEnabled(false);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		pagerAdapter_ = new TorrentPagerAdapter(this);
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);
        PagerTabStrip pagerTabStrip = (PagerTabStrip) findViewById(R.id.pagerTabStrip);
        pagerTabStrip.setTabIndicatorColor(0xFF6899FF);
        viewPager.setAdapter(pagerAdapter_);
        
        clientType_ = TorrentClient.CLIENT_TYPE_SINGLE;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		createMenu(menu);
		
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		createMenu(menu);

		return true;
	}
	
	protected void createMenu(Menu menu) {
		if (!isStopped_) {
			menu.add(Menu.NONE, MENU_STOP_TORRENT, 0, R.string.stop)
				//.setIcon(R.drawable.ic_pause)
				.setIcon(R.drawable.ic_menu_pause)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		} else {
			menu.add(Menu.NONE, MENU_START_TORRENT, 0, R.string.start)
				//.setIcon(R.drawable.ic_play)
				.setIcon(R.drawable.ic_menu_play)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		}
		menu.add(Menu.NONE, MENU_DELETE_TORRENT, 1, R.string.remove)
			//.setIcon(R.drawable.ic_delete)
			.setIcon(R.drawable.ic_menu_delete)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		menu.add(Menu.NONE, MENU_ADD_PEER, 2, R.string.menu_add_peer)
			//.setIcon(R.drawable.ic_add)
			//.setIcon(R.drawable.ic_menu_add)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		
		menu.add(Menu.NONE, MENU_ADD_TRACKER, 3, R.string.menu_add_tracker)
			//.setIcon(R.drawable.ic_add)
			//.setIcon(R.drawable.ic_menu_add)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		
		menu.add(Menu.NONE, MENU_SHARE_TORRENT, 4, R.string.share_torrent)
			//.setIcon(R.drawable.ic_add)
			//.setIcon(R.drawable.ic_menu_add)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		
		menu.add(Menu.NONE, MENU_SHARE_MAGNET, 5, R.string.share_magnet)
			//.setIcon(R.drawable.ic_add)
			//.setIcon(R.drawable.ic_menu_add)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home:
	            // app icon in action bar clicked; go home
	            Intent intent = new Intent(this, MainActivity.class);
	            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            intent.putExtra(MainActivity.KEY_TORRENT_ID, torrentId_);
	            startActivity(intent);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		super.onMenuItemSelected(featureId, item);
		
		final Message msg = Message.obtain();
		final Bundle bundle = new Bundle();
		bundle.putInt(TorrentService.MSG_KEY_TORRENT_ID, torrentId_);
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
				LayoutInflater inflater = TorrentDetailsActivity.this.getLayoutInflater();
				ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.dialog_checkbox, null);
				((TextView) layout.findViewById(R.id.dialog_checkbox_message)).setText(R.string.remove_dialog_title);
				final CheckBox cbDeleteFiles = (CheckBox) layout.findViewById(R.id.dialog_checkbox_checkbox);
				cbDeleteFiles.setText(R.string.delete_downloaded_files);
				
				AlertDialog.Builder builder = new AlertDialog.Builder(context_);
				builder.setTitle(getString(R.string.remove)).
				setView(layout).
				setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						bundle.putBoolean(TorrentService.MSG_KEY_DELETE_FILES, cbDeleteFiles.isChecked());
						msg.what = TorrentService.MSG_CLOSE_TORRENT;
						try {
							serviceMessenger_.send(msg);
						} catch (RemoteException e) {}
						Intent intent = new Intent(context_, MainActivity.class);
			            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			            startActivity(intent);
					}
				}).
				setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				dialog_ = builder.create();
				dialog_.show();
            
				break;
				
			case MENU_ADD_PEER:
				addPeer(torrentId_);
				break;
				
			case MENU_ADD_TRACKER:
				addTracker(torrentId_);
				break;
				
			case MENU_SHARE_TORRENT:
				sendShareMessage(torrentId_, true);
				break;
				
			case MENU_SHARE_MAGNET:
				sendShareMessage(torrentId_, false);
				break;
				
			default:
		}
		return true;
	}
	
	@Override
	protected void refreshTorrentItem(TorrentListItem item, boolean isRemoved) {
		if (item == null) {
			return;
		}
		
		if (isStopped_) {
			if (item.getStatus() != R.string.status_stopped && item.getStatus() != R.string.status_finished) {
				isStopped_ = false;
				invalidateOptionsMenu();
			}
		} else {
			if (item.getStatus() == R.string.status_stopped || item.getStatus() == R.string.status_finished) {
				isStopped_ = true;
				invalidateOptionsMenu();
			}
		}

		if (pagerAdapter_ != null) {
			pagerAdapter_.refreshTorrentItem(item, isRemoved);
		}
	}
	
	@Override
	protected void refreshPeerList(ArrayList<PeerListItem> itemList) {
		if (pagerAdapter_ != null) {
			pagerAdapter_.refreshPeerList(itemList);
		}
	}
	
	@Override
	protected void refreshFileList(ArrayList<FileListItem> itemList) {
		if (pagerAdapter_ != null) {
			pagerAdapter_.refreshFileList(itemList);
		}
	}
	
	@Override
	protected void refreshTrackerList(ArrayList<TrackerListItem> itemList) {
		if (pagerAdapter_ != null) {
			pagerAdapter_.refreshTrackerList(itemList);
		}
	}
	
	@Override
	protected void refreshBitfield(Bitfield bitfield, Bitfield downloadingBitfield) {
		if (pagerAdapter_ != null) {
			pagerAdapter_.refreshBitfield(bitfield, downloadingBitfield);
		}
	}
}
