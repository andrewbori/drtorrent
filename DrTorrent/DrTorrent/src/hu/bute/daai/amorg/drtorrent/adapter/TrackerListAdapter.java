package hu.bute.daai.amorg.drtorrent.adapter;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.adapter.item.TrackerListItem;

import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class TrackerListAdapter<T> extends ArrayAdapter<T> {
	private Activity context_;
	private ArrayList<T> items_;
	
	private final int rowResource = R.layout.list_item_tracker;
	private final int tvAddressResource = R.id.list_item_tracker_tvUrl;
	private final int tvStatusResource = R.id.list_item_tracker_tvStatus;
	private final int tvTimeResource = R.id.list_item_tracker_tvTime;
	private final int tvPeersResource = R.id.list_item_tracker_tvPeers;

	public TrackerListAdapter(Activity context, ArrayList<T> items) {
		super(context, R.layout.list_item_torrent, items);

		this.context_ = context;
		this.items_ = items;
	}

	@Override
	public View getView(int position, View reusableView, ViewGroup parent) {
		if (reusableView == null) {
			LayoutInflater inflater = context_.getLayoutInflater();
			reusableView = inflater.inflate(this.rowResource, null);
		}

		TextView tvAddress = (TextView) reusableView.findViewById(tvAddressResource);
		TextView tvStatus = (TextView) reusableView.findViewById(tvStatusResource);
		TextView tvTime = (TextView) reusableView.findViewById(tvTimeResource);
		TextView tvPeers = (TextView) reusableView.findViewById(tvPeersResource);

		TrackerListItem item = (TrackerListItem) items_.get(position);
		
		int colorId;
		switch (item.getStatus()) {
			case R.string.failed:
				colorId = Color.RED;
				break;
				
			case R.string.updating:
				colorId = Color.CYAN;
				break;
			
			case R.string.working:
				
			default:
				colorId = Color.LTGRAY;
		}
		
		tvAddress.setText(item.getAddress());
		tvStatus.setText(context_.getString(item.getStatus()));
		tvPeers.setText(item.getPeers());
		tvTime.setText(item.getTime(context_));
		
		tvAddress.setTextColor(colorId);

		return reusableView;
	}
}
