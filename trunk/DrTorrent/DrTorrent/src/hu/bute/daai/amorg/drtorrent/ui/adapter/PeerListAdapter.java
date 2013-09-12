package hu.bute.daai.amorg.drtorrent.ui.adapter;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.core.peer.PeerConnection;
import hu.bute.daai.amorg.drtorrent.ui.adapter.item.PeerListItem;

import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Color;
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
	private final int tvDownloadStatusRecource = R.id.list_item_peer_tvDownloadStatus;
	private final int tvUploadStatusRecource = R.id.list_item_peer_tvUploadStatus;
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
		TextView tvDownloadStatus = (TextView) reusableView.findViewById(tvDownloadStatusRecource);
		TextView tvUploadStatus = (TextView) reusableView.findViewById(tvUploadStatusRecource);
		TextView tvPercent = (TextView) reusableView.findViewById(tvPercentRecource);

		PeerListItem item = (PeerListItem) items_.get(position);

		int colorId = Color.LTGRAY;
		int downColorId = Color.GRAY;
		int upColorId = Color.GRAY;
		switch (item.getState()) {
			case PeerConnection.STATE_PW_CONNECTED:
				if (item.isChoked()) {
					colorId = Color.GRAY;
				} else {
					colorId = Color.LTGRAY;
					downColorId = Color.LTGRAY;
				}
				if (!item.isPeerChoked()) {
					upColorId = Color.LTGRAY;
				}
				break;
				
			case PeerConnection.STATE_PW_HANDSHAKING:
				colorId = Color.CYAN;
				break;

			case PeerConnection.STATE_TCP_CONNECTING:
			case PeerConnection.STATE_TCP_CONNECTED:
				colorId = Color.BLUE;
				break;
				
			default:
				colorId = Color.RED;
				break;
		}
		
		tvAddress.setText(item.getAddress());
		//tvRequests.setText(item.getRequests());
		tvDownloadStatus.setText(item.getDownloadStatus(context_));
		tvUploadStatus.setText(item.getUploadStatus(context_));
		tvPercent.setText(item.getPercent());
		
		tvAddress.setTextColor(colorId);
		tvDownloadStatus.setTextColor(downColorId);
		tvUploadStatus.setTextColor(upColorId);

		return reusableView;
	}
}
