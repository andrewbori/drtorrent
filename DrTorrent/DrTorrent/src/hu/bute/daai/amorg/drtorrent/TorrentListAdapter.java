package hu.bute.daai.amorg.drtorrent;

import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

/** Adapter of the torrent list. */
public class TorrentListAdapter<T> extends ArrayAdapter<T> {
	private final int rowResource = R.layout.list_item_torrent;
	private final int ivStatusResource = R.id.list_item_torrent_ivStatus;
	private final int tvNameResource = R.id.list_item_torrent_tvName;
	private final int tvStatusResource = R.id.list_item_torrent_tvStatus;
	private final int tvPercentResource = R.id.list_item_torrent_tvPercent;
	private final int progressResource = R.id.list_item_torrent_progress;
	private final int tvDownloadSpeedResource = R.id.list_item_torrent_tvDownloadSpeed;
	private final int tvUploadSpeedResource = R.id.list_item_torrent_tvUploadSpeed;
	private final int tvDownloadedResource = R.id.list_item_torrent_tvDownloaded;
	private final int tvUploadedResource = R.id.list_item_torrent_tvUploaded;

	private Activity context_;
	private ArrayList<T> torrents_;

	public TorrentListAdapter(Activity context, ArrayList<T> torrents) {
		super(context, R.layout.list_item_torrent, torrents);

		this.context_ = context;
		this.torrents_ = torrents;
	}

	@Override
	public View getView(int position, View reusableView, ViewGroup parent) {
		if (reusableView == null) {
			LayoutInflater inflater = context_.getLayoutInflater();
			reusableView = inflater.inflate(this.rowResource, null);
		}

		ImageView ivStatus = (ImageView) reusableView.findViewById(ivStatusResource);
		TextView tvName = (TextView) reusableView.findViewById(tvNameResource);
		TextView tvStatus = (TextView) reusableView.findViewById(tvStatusResource);
		TextView tvPercent = (TextView) reusableView.findViewById(tvPercentResource);
		ProgressBar progress = (ProgressBar) reusableView.findViewById(progressResource);
		TextView tvDownSpeed = (TextView) reusableView.findViewById(tvDownloadSpeedResource);
		TextView tvUpSpeed = (TextView) reusableView.findViewById(tvUploadSpeedResource);
		TextView tvDownloaded = (TextView) reusableView.findViewById(tvDownloadedResource);
		TextView tvUploaded = (TextView) reusableView.findViewById(tvUploadedResource);

		TorrentListItem item = (TorrentListItem) torrents_.get(position);
		int status = item.getStatus();

		tvName.setText(item.getName());
		tvStatus.setText(context_.getString(status));
		tvPercent.setText(item.getPercent() + " %");
		progress.setProgress(item.getPercent());
		tvDownSpeed.setText(item.getDownloadSpeed());
		tvUpSpeed.setText(item.getUploadSpeed());
		tvDownloaded.setText(item.getDownloaded());
		tvUploaded.setText(item.getUploaded());

		if (status == R.string.downloading) {
			ivStatus.setImageResource(R.drawable.icon_download);
		} else if (status == R.string.sharing) {
			ivStatus.setImageResource(R.drawable.icon_upload);
		} else {
			ivStatus.setImageResource(R.drawable.icon_pause);
		}

		return reusableView;
	}
}
