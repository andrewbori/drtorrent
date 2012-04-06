package hu.bute.daai.amorg.drtorrent;

import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class PeerListAdapter<T> extends ArrayAdapter<T> {
	private Activity context_;
	private ArrayList<T> items_;
	
	private final int rowResource = R.layout.list_item_peer;
	private final int tvAddressResource = R.id.list_item_peer_tvAddress;
	private final int tvRequestsResource = R.id.list_item_peer_tvRequests;

	public PeerListAdapter(Activity context, ArrayList<T> items) {
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
		TextView tvRequests = (TextView) reusableView.findViewById(tvRequestsResource);

		PeerListItem item = (PeerListItem) items_.get(position);

		tvAddress.setText(item.getAddress());
		tvRequests.setText(item.getRequests());

		return reusableView;
	}
}
