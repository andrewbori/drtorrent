package hu.bute.daai.amorg.drtorrent.adapter;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.adapter.item.SearchResultListItem;

import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class SearchResultListAdapter<T> extends ArrayAdapter<T> {

	private Activity context_;
	private ArrayList<T> items_;
	
	private final int rowResource = R.layout.list_item_search;
	private final int tvNameResource = R.id.list_item_search_tvName;
	private final int tvSizeResource = R.id.list_item_search_tvSize;
	private final int tvAddedResource = R.id.list_item_search_tvAdded;
	private final int tvPeersResource = R.id.list_item_search_tvPeers;
	private final int ivMagnetResource = R.id.list_item_search_ivMagnet;
	
	public SearchResultListAdapter(Activity context, ArrayList<T> items) {
		super(context, R.layout.list_item_search, items);
		
		context_ = context;
		items_ = items;
	}
	
	@Override
	public View getView(int position, View reusableView, ViewGroup parent) {
		if (reusableView == null) {
			LayoutInflater inflater = context_.getLayoutInflater();
			reusableView = inflater.inflate(this.rowResource, null);
		}
		
		TextView tvName  = (TextView) reusableView.findViewById(tvNameResource);
		TextView tvSize  = (TextView) reusableView.findViewById(tvSizeResource);
		TextView tvAdded = (TextView) reusableView.findViewById(tvAddedResource);
		TextView tvPeers = (TextView) reusableView.findViewById(tvPeersResource);
		ImageView ivMagnet = (ImageView) reusableView.findViewById(ivMagnetResource);

		SearchResultListItem item = (SearchResultListItem) items_.get(position);
		
		tvName.setText(item.getName());
		tvSize.setText(item.getSize());
		tvAdded.setText(item.getAdded());
		tvPeers.setText(item.getPeers());
		if (item.getTorrentUrl().startsWith("magnet")) {
			ivMagnet.setVisibility(ImageView.VISIBLE);
		} else {
			ivMagnet.setVisibility(ImageView.GONE);
		}
		
		return reusableView;
	}

}
