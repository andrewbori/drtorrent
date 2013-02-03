package hu.bute.daai.amorg.drtorrent.fragment;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.adapter.TorrentPagerAdapter;
import hu.bute.daai.amorg.drtorrent.adapter.item.FileListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.PeerListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.TorrentListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.TrackerListItem;
import hu.bute.daai.amorg.drtorrent.torrentengine.Bitfield;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class TorrentDetailsFragment extends Fragment {

	private TorrentPagerAdapter pagerAdapter_;
	
	boolean isStopped_ = false;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.torrent_details, container, false);
		init(layout);
		
		return layout;
	}
	
	private void init(LinearLayout layout) {
		pagerAdapter_ = new TorrentPagerAdapter(getActivity());
        ViewPager viewPager = (ViewPager) layout.findViewById(R.id.viewPager);
        PagerTabStrip pagerTabStrip = (PagerTabStrip) layout.findViewById(R.id.pagerTabStrip);
        pagerTabStrip.setTabIndicatorColor(0xFF6899FF);
        viewPager.setAdapter(pagerAdapter_);
	}
	
	public void reset() {
		if (pagerAdapter_ != null) {
			pagerAdapter_.reset();
		}
	}
	
	public void refreshTorrentItem(TorrentListItem item, boolean isRemoved) {
		if (item == null) {
			return;
		}
		
		if (isStopped_) {
			if (item.getStatus() != R.string.status_stopped && item.getStatus() != R.string.status_finished) {
				isStopped_ = false;
			}
		} else {
			if (item.getStatus() == R.string.status_stopped || item.getStatus() == R.string.status_finished) {
				isStopped_ = true;
			}
		}

		if (pagerAdapter_ != null) {
			pagerAdapter_.refreshTorrentItem(item, isRemoved);
		}
	}
	
	public void refreshPeerList(ArrayList<PeerListItem> itemList) {
		if (pagerAdapter_ != null) {
			pagerAdapter_.refreshPeerList(itemList);
		}
	}
	
	public void refreshFileList(ArrayList<FileListItem> itemList) {
		if (pagerAdapter_ != null) {
			pagerAdapter_.refreshFileList(itemList);
		}
	}
	
	public void refreshTrackerList(ArrayList<TrackerListItem> itemList) {
		if (pagerAdapter_ != null) {
			pagerAdapter_.refreshTrackerList(itemList);
		}
	}
	
	public void refreshBitfield(Bitfield bitfield, Bitfield downloadingBitfield) {
		if (pagerAdapter_ != null) {
			pagerAdapter_.refreshBitfield(bitfield, downloadingBitfield);
		}
	}
}
