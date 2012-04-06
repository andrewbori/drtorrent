package hu.bute.daai.amorg.drtorrent.activity;

import hu.bute.daai.amorg.drtorrent.PeerListAdapter;
import hu.bute.daai.amorg.drtorrent.PeerListItem;
import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.TorrentListItem;
import hu.bute.daai.amorg.drtorrent.service.TorrentService;

import java.util.ArrayList;

import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TextView;

public class TorrentDetailsActivity extends TorrentHostActivity {

	private TorrentListItem torrent_;
	private ArrayList<PeerListItem> peers_;
	private PeerListAdapter<PeerListItem> peersAdapter_;
	
	private TextView tvName_;
	private TextView tvStatus_;
	private TextView tvPercent_;
	private ProgressBar progress_;
	private TextView tvDownSpeed_;
	private TextView tvUpSpeed_;
	private TextView tvDownloaded_;
	private TextView tvPeers_;
	private ListView lvPeers_;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		setContentView(R.layout.torrent_details);
		
		TabHost tabs = (TabHost) findViewById(android.R.id.tabhost);
		tabs.setup();
		
		TabHost.TabSpec spec = tabs.newTabSpec("tab1");   
		spec.setContent(R.id.torrent_details_tab1);
	   	spec.setIndicator("Torrent");
  		tabs.addTab(spec);
		
  		spec = tabs.newTabSpec("tab2");   
  		spec.setContent(R.id.torrent_details_tab2);
	   	spec.setIndicator("Peers");
  		tabs.addTab(spec);
  		
		spec=tabs.newTabSpec("tab3");
		spec.setContent(R.id.torrent_details_tab3);
		spec.setIndicator("Trackers");
		tabs.addTab(spec);
		
		tabs.setCurrentTab(0);
		
		tvName_ = (TextView) findViewById(R.id.torrent_details_tvName);
		tvStatus_ = (TextView) findViewById(R.id.torrent_details_tvStatus);
		tvPercent_ = (TextView) findViewById(R.id.torrent_details_tvPercent);
		progress_ = (ProgressBar) findViewById(R.id.torrent_details_progress);
		tvDownSpeed_ = (TextView) findViewById(R.id.torrent_details_tvDownloadSpeed);
		tvUpSpeed_ = (TextView) findViewById(R.id.torrent_details_tvUploadSpeed);
		tvDownloaded_ = (TextView) findViewById(R.id.torrent_details_tvDownloaded);
		tvPeers_ = (TextView) findViewById(R.id.torrent_details_tvPeers);
		lvPeers_ = (ListView) findViewById(R.id.torrent_details_lvPeer);
		
		peers_ = new ArrayList<PeerListItem>();
		/*for (int i = 10; i < 30; i++) {
			PeerListItem item = new PeerListItem("192.168.0.0" + i, "0/0");
			peers_.add(item);
		}*/
		peersAdapter_ = new PeerListAdapter<PeerListItem>(activity_, peers_);
		lvPeers_.setAdapter(peersAdapter_);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(Menu.NONE, MENU_STOP_TORRENT, Menu.NONE, R.string.menu_stop);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		super.onMenuItemSelected(featureId, item);
		switch (item.getItemId()) {
			case MENU_STOP_TORRENT:
				Message msg = Message.obtain();
				Bundle bundle = new Bundle();
				bundle.putString(TorrentService.MSG_KEY_TORRENT_INFOHASH, infoHash_);
				msg.setData(bundle);
				msg.what = TorrentService.MSG_STOP_TORRENT;
				try {
					serviceMessenger_.send(msg);
				} catch (RemoteException e) {}
				break;
		}
		return true;
	}

	@Override
	protected void refreshTorrentItem(TorrentListItem item) {
		tvName_.setText(item.getName());
		tvStatus_.setText(activity_.getString(item.getStatus()));
		tvPercent_.setText(item.getPercent() + " %");
		progress_.setProgress(item.getPercent());
		tvDownSpeed_.setText(item.getDownloadSpeed());
		tvUpSpeed_.setText(item.getUploadSpeed());
		tvDownloaded_.setText(item.getDownloaded());
		tvPeers_.setText(item.getPeers());
	}
	
	@Override
	protected void refreshPeerItem(PeerListItem item, boolean isDisconnected) {
		if (isDisconnected) {
			for (int i = 0; i < peersAdapter_.getCount(); i++) {
				if (peersAdapter_.getItem(i).equals(item)) {
					peersAdapter_.remove(peersAdapter_.getItem(i));
					return;
				}
			}
		} else {
			boolean found = false;
			PeerListItem tempItem;
			for (int i = 0; i < peersAdapter_.getCount(); i++) {
				tempItem = peersAdapter_.getItem(i);
				if (tempItem.equals(item)) {
					found = true;
					peersAdapter_.getItem(i).set(item);
					break;
				}
			}
			if (!found) {
				peersAdapter_.add(item);
			}
		}

		lvPeers_.invalidateViews();
	}
	
	@Override
	protected void refreshPeerList(ArrayList<PeerListItem> itemList) {
		boolean foundOld = false;
		for (int i = 0; i < peersAdapter_.getCount() && i >= 0; i++) {
			foundOld = false;
			for (int j = 0; j < itemList.size(); j++) {
				if (peersAdapter_.getItem(i).equals(itemList.get(j))) {
					foundOld = true;
					peersAdapter_.getItem(i).set(itemList.get(j));
					itemList.remove(j);
				}
			}
			if (!foundOld) {
				peersAdapter_.remove(peersAdapter_.getItem(i));
				i--;
			}
		}

		if (itemList == null) return;
		for (int i = 0; i < itemList.size(); i++) {
			peersAdapter_.add(itemList.get(i));
		}

		lvPeers_.invalidateViews();
	}
	
}
