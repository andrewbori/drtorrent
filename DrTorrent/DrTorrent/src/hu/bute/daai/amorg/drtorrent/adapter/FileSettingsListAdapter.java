package hu.bute.daai.amorg.drtorrent.adapter;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.adapter.item.FileListItem;
import hu.bute.daai.amorg.drtorrent.torrentengine.File;

import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class FileSettingsListAdapter<T> extends ArrayAdapter<T> {
	private Activity context_;
	private ArrayList<T> items_;
	
	private final int rowResource = R.layout.list_item_file_select;
	private final int cbFileResource = R.id.list_item_file_select_cbFile;
	private final int tvSizeResource = R.id.list_item_file_select_tvFileSize;

	public FileSettingsListAdapter(Activity context, ArrayList<T> items) {
		super(context, R.layout.list_item_torrent, items);

		this.context_ = context;
		this.items_ = items;
	}

	@Override
	public View getView(int position, View reusableView, ViewGroup parent) {
		final int pos = position;
		
		if (reusableView == null) {
			LayoutInflater inflater = context_.getLayoutInflater();
			reusableView = inflater.inflate(this.rowResource, null);
		}

		CheckBox cbFile = (CheckBox) reusableView.findViewById(cbFileResource);
		TextView tvSize = (TextView) reusableView.findViewById(tvSizeResource);

		FileListItem item = (FileListItem) items_.get(position);

		cbFile.setText(item.getName());
		if (item.getPriority() != File.PRIORITY_SKIP) cbFile.setChecked(true);
		else cbFile.setChecked(false);
		
		cbFile.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (((CheckBox) v).isChecked()) ((FileListItem) items_.get(pos)).setPriority(File.PRIORITY_NORMAL);
				else ((FileListItem) items_.get(pos)).setPriority(File.PRIORITY_SKIP);
			}
		});
		
		tvSize.setText(item.getSize());

		return reusableView;
	}
}
