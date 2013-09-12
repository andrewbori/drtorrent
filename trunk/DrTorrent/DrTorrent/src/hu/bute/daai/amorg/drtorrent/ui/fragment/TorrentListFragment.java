package hu.bute.daai.amorg.drtorrent.ui.fragment;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.ui.adapter.TorrentListAdapter;
import hu.bute.daai.amorg.drtorrent.ui.adapter.item.TorrentListItem;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

public class TorrentListFragment extends Fragment {

	private TorrentItemInteractionListener interactionCallback_ = null;
	
	private ListView lvTorrent_ = null;
	private int selectedPosition_ = -1;
	private int torrentId_ = -1;
	private ArrayAdapter<TorrentListItem> adapter_ = null;
	private ArrayList<TorrentListItem> list_ = null;
	
	public interface TorrentItemInteractionListener {
		public void onTorrentItemSelected(int torrentId);
		public void onTorrentItemLongClicked(TorrentListItem item);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		try {
            interactionCallback_ = (TorrentItemInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnTorrentItemSelectedListener");
        }
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.list, container, false);
		init(layout);
		
		return layout;
	}
	
	private void init(LinearLayout layout) {
		lvTorrent_ = (ListView) layout.findViewById(R.id.list_listview);
		lvTorrent_.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		if (list_ != null) {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				int rowResource = R.layout.list_item_torrent;
				if (!activity.getResources().getBoolean(R.bool.has_two_panes) &&
					 activity.getResources().getBoolean(R.bool.horizontal_style)) {
					rowResource = R.layout.list_item_torrent_land;
				}
				adapter_ = new TorrentListAdapter<TorrentListItem>(activity, rowResource, list_);
				lvTorrent_.setAdapter(adapter_);
				if (selectedPosition_ != -1) {
					lvTorrent_.setItemChecked(selectedPosition_, true);
					lvTorrent_.invalidateViews();
				}
			}
		}
		
		lvTorrent_.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				selectedPosition_ = position;

				final TorrentListItem item = adapter_.getItem(position);
				torrentId_ = item.getId();
				
				interactionCallback_.onTorrentItemSelected(torrentId_);
			}
		});
		
		lvTorrent_.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				selectedPosition_ = position;
				
				final TorrentListItem item = adapter_.getItem(position);
				torrentId_ = item.getId();
				
				lvTorrent_.setItemChecked(position, true);
				lvTorrent_.invalidateViews();
				
				interactionCallback_.onTorrentItemLongClicked(item);
				
				return true;
			}
		});
	}
	
	/** Sets the selected item. */
	public void setSelectedItem(int torrentId) {
		torrentId_ = torrentId;
		if (list_ != null) {
			for (int i = 0; i < list_.size(); i++) {
				if (list_.get(i).getId() == torrentId) {
					selectedPosition_ = i;
					if (lvTorrent_ != null) {
						lvTorrent_.setItemChecked(i, true);
						//lvTorrent_.invalidateViews();
					}
					break;
				}
			}
		}
	}
	
	/** Sets the items of the list. */
	public void setTorrentList(ArrayList<TorrentListItem> items) {
		list_ = items;
		
		if (lvTorrent_ != null) {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				int rowResource = R.layout.list_item_torrent;
				if (!activity.getResources().getBoolean(R.bool.has_two_panes) &&
					 activity.getResources().getBoolean(R.bool.horizontal_style)) {
					rowResource = R.layout.list_item_torrent_land;
				}
				adapter_ = new TorrentListAdapter<TorrentListItem>(activity, rowResource, list_);
				lvTorrent_.setAdapter(adapter_);
			}
		}
	}
	
	/** Called when the elements of the list are changed. */
	public void onTorrentListChanged() {
		if (lvTorrent_ != null) {
			lvTorrent_.invalidateViews();
			if (torrentId_ != -1) {
				try {
					int position = lvTorrent_.getCheckedItemPosition();
					if (position == ListView.INVALID_POSITION) {
						setSelectedItem(torrentId_);
					} else if(list_.get(position).getId() != torrentId_) {
						setSelectedItem(torrentId_);
					}
				} catch (Exception e) {}
			}
		}
	}
}
