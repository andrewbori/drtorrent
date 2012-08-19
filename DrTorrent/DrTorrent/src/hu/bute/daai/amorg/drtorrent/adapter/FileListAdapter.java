package hu.bute.daai.amorg.drtorrent.adapter;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.adapter.item.FileListItem;

import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class FileListAdapter<T> extends ArrayAdapter<T> {
	private Activity context_;
	private ArrayList<T> items_;
	
	private final int rowResource = R.layout.list_item_file_torrent;
	private final int tvNameResource = R.id.list_item_file_torrent_tvFilePath;
	private final int tvPriorityResource = R.id.list_item_file_torrent_tvPriority;
	private final int tvSizeResource = R.id.list_item_file_torrent_tvFileSize;

	public FileListAdapter(Activity context, ArrayList<T> items) {
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

		TextView tvName = (TextView) reusableView.findViewById(tvNameResource);
		TextView tvPriority = (TextView) reusableView.findViewById(tvPriorityResource);
		TextView tvSize = (TextView) reusableView.findViewById(tvSizeResource);

		FileListItem item = (FileListItem) items_.get(position);

		tvName.setText(item.getName());
		tvPriority.setText(item.getPriorityString(context_));
		tvSize.setText(item.getSize());

		return reusableView;
	}
}
