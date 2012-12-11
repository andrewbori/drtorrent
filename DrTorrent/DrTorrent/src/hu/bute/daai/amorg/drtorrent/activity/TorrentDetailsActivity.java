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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

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
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		} else {
			menu.add(Menu.NONE, MENU_START_TORRENT, 0, R.string.start)
				//.setIcon(R.drawable.ic_play)
				.setIcon(R.drawable.ic_menu_play)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		}
		menu.add(Menu.NONE, MENU_DELETE_TORRENT, 1, R.string.remove)
			//.setIcon(R.drawable.ic_delete)
			.setIcon(R.drawable.ic_menu_delete)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

		menu.add(Menu.NONE, MENU_ADD_PEER, 2, R.string.menu_add_peer)
			//.setIcon(R.drawable.ic_add)
			.setIcon(R.drawable.ic_menu_add)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		
		menu.add(Menu.NONE, MENU_ADD_TRACKER, 3, R.string.menu_add_tracker)
			//.setIcon(R.drawable.ic_add)
			.setIcon(R.drawable.ic_menu_add)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
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
				AlertDialog.Builder builder = new AlertDialog.Builder(context_);
				builder.setMessage(R.string.remove_dialog_title).
				setTitle(getString(R.string.remove)).
				setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						msg.what = TorrentService.MSG_CLOSE_TORRENT;
						try {
							serviceMessenger_.send(msg);
						} catch (RemoteException e) {}
						Intent intent = new Intent(context_, DrTorrentActivity.class);
			            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			            startActivity(intent);
					}
				}).
				setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				}).
				create().show();
            
				break;
				
			case MENU_ADD_PEER:
				builder = new AlertDialog.Builder(context_);
				builder.setTitle(R.string.menu_add_peer);
				
				LayoutInflater inflater = (LayoutInflater) context_.getSystemService(LAYOUT_INFLATER_SERVICE);
				View layout = inflater.inflate(R.layout.dialog_add_peer, (ViewGroup) findViewById(R.id.dialog_add_peer_root));
				builder.setView(layout);
				final EditText etAddress = (EditText) layout.findViewById(R.id.dialog_add_peer_ip);
				final EditText etPort = (EditText) layout.findViewById(R.id.dialog_add_peer_port);
				
				builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {	
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							String address = etAddress.getText().toString();
							int port = Integer.valueOf(etPort.getText().toString());
							if ((address != null && address.length() > 0) && (port > 0 && port <= 65535)) {
								msg.what = TorrentService.MSG_ADD_PEER;
								bundle.putString(TorrentService.MSG_KEY_PEER_IP, address);
								bundle.putInt(TorrentService.MSG_KEY_PEER_PORT, port);
								try {
									serviceMessenger_.send(msg);
								} catch (RemoteException e) {}
							}
						} catch (Exception e) {}
						dialog.cancel();
					}
				}).
				setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {	
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				}).
				create().show();
				break;
				
			case MENU_ADD_TRACKER:
				builder = new AlertDialog.Builder(context_);
				builder.setTitle(R.string.menu_add_tracker);
				
				final EditText etTrackerUrl = new EditText(context_);
				etTrackerUrl.setHint("udp://...");
				builder.setView(etTrackerUrl);
				
				builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {	
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String url = etTrackerUrl.getText().toString();
						if (url != null && url.length() > 0) {
							msg.what = TorrentService.MSG_ADD_TRACKER;
							bundle.putString(TorrentService.MSG_KEY_TRACKER_URL, url);
							try {
								serviceMessenger_.send(msg);
							} catch (RemoteException e) {}
						}
						dialog.cancel();
					}
				}).
				setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {	
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				}).
				create().show();
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
		b.putInt(TorrentService.MSG_KEY_TORRENT_ID, torrentId_);
		b.putSerializable(TorrentService.MSG_KEY_FILE_ITEM, item);
		msg.what = TorrentService.MSG_CHANGE_FILE_PRIORITY;
		msg.setData(b);
		try {
			serviceMessenger_.send(msg);
		} catch (Exception e) {}
	}
	
	/** Removes a tracker from the tracker list. */
	public void removeTracker(int trackerId) {
		Message msg = Message.obtain();
		Bundle b = new Bundle();
		b.putInt(TorrentService.MSG_KEY_TORRENT_ID, torrentId_);
		b.putInt(TorrentService.MSG_KEY_TRACKER_ID, trackerId);
		msg.what = TorrentService.MSG_REMOVE_TACKER;
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
