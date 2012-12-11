package hu.bute.daai.amorg.drtorrent.adapter;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.adapter.item.PeerListItem;

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
	//private final int tvRequestsResource = R.id.list_item_peer_tvRequests;
	private final int tvDownloadSpeedRecource = R.id.list_item_peer_tvDownloadSpeed;
	private final int tvDownloadedRecource = R.id.list_item_peer_tvDownloaded;
	private final int tvUploadSpeedRecource = R.id.list_item_peer_tvUploadSpeed;
	private final int tvUploadedRecource = R.id.list_item_peer_tvUploaded;
	private final int tvPercentRecource = R.id.list_item_peer_tvPercent;

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
		//TextView tvRequests = (TextView) reusableView.findViewById(tvRequestsResource);
		TextView tvDownloadSpeed = (TextView) reusableView.findViewById(tvDownloadSpeedRecource);
		TextView tvDownloaded = (TextView) reusableView.findViewById(tvDownloadedRecource);
		TextView tvUploadSpeed = (TextView) reusableView.findViewById(tvUploadSpeedRecource);
		TextView tvUploaded = (TextView) reusableView.findViewById(tvUploadedRecource);
		TextView tvPercent = (TextView) reusableView.findViewById(tvPercentRecource);

		PeerListItem item = (PeerListItem) items_.get(position);

		tvAddress.setText(item.getAddress());
		//tvRequests.setText(item.getRequests());
		tvDownloadSpeed.setText(item.getDownloadSpeed());
		tvDownloaded.setText(item.getDownloaded());
		tvUploadSpeed.setText(item.getUploadSpeed());
		tvUploaded.setText(item.getUploaded());
		tvPercent.setText(item.getPercent());

		return reusableView;
	}
}
