package hu.bute.daai.amorg.drtorrent.adapter;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.adapter.item.TorrentListItem;

import java.text.DecimalFormat;
import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/** Adapter of the torrent list. */
public class TorrentListAdapter<T> extends ArrayAdapter<T> {
	private final int rowResource = R.layout.list_item_torrent;
	private final int tvNameResource = R.id.list_item_torrent_tvName;
	private final int tvStatusResource = R.id.list_item_torrent_tvStatus;
	private final int tvPercentResource = R.id.list_item_torrent_tvPercent;
	private final int progressResource = R.id.list_item_torrent_progress;
	
	private final int layoutDownloadRecource = R.id.list_item_torrent_layoutDownload;
	private final int layoutRemainingRecource  = R.id.list_item_torrent_layoutRemaining;
	private final int tvRemainingTimeResource = R.id.list_item_torrent_tvRemainingTime;
	private final int tvSizeResource = R.id.list_item_torrent_tvSize;
	private final int tvDownloadedResource = R.id.list_item_torrent_tvDownloaded;
	private final int layoutDownloadSpeedResource  = R.id.list_item_torrent_layoutDownloadSpeed;
	private final int tvDownloadSpeedResource = R.id.list_item_torrent_tvDownloadSpeed;
	
	private final int layoutUploadRecource = R.id.list_item_torrent_layoutUpload;
	private final int tvUploadedResource = R.id.list_item_torrent_tvUploaded;
	private final int tvUploadSpeedResource = R.id.list_item_torrent_tvUploadSpeed;

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

		TorrentListItem item = (TorrentListItem) torrents_.get(position);
		int status = item.getStatus();
		
		TextView tvName = (TextView) reusableView.findViewById(tvNameResource);
		TextView tvStatus = (TextView) reusableView.findViewById(tvStatusResource);
		TextView tvPercent = (TextView) reusableView.findViewById(tvPercentResource);
		ProgressBar progress = (ProgressBar) reusableView.findViewById(progressResource);
		LinearLayout layoutDownload = (LinearLayout) reusableView.findViewById(layoutDownloadRecource);
		LinearLayout layoutUpload = (LinearLayout) reusableView.findViewById(layoutUploadRecource);
		
		tvName.setText(item.getName());
		tvStatus.setText(context_.getString(status));
		
		DecimalFormat dec = new DecimalFormat("###.#");
		tvPercent.setText(dec.format(item.getPercent()).concat(" %"));
		
		if (status == R.string.status_seeding || status == R.string.status_finished || status == R.string.status_metadata) {
			tvPercent.setVisibility(TextView.GONE);
		} else {
			tvPercent.setVisibility(TextView.VISIBLE);
		}
		
		if (status != R.string.status_seeding) {
			LinearLayout layoutRemaining = (LinearLayout) reusableView.findViewById(layoutRemainingRecource);
			LinearLayout layoutDownloadSpeed = (LinearLayout) reusableView.findViewById(layoutDownloadSpeedResource);
			TextView tvRemainingTime = (TextView) reusableView.findViewById(tvRemainingTimeResource);
			TextView tvSize = (TextView) reusableView.findViewById(tvSizeResource);
			TextView tvDownloaded = (TextView) reusableView.findViewById(tvDownloadedResource);
			TextView tvDownSpeed = (TextView) reusableView.findViewById(tvDownloadSpeedResource);
			
			tvRemainingTime.setText(item.getRemainingTime(context_));
			tvSize.setText(item.getSize(context_));
			tvDownloaded.setText(item.getReadySize(context_));
			tvDownSpeed.setText(item.getDownloadSpeed(context_));
			
			layoutDownload.setVisibility(LinearLayout.VISIBLE);
			layoutUpload.setVisibility(LinearLayout.GONE);
			if (status != R.string.status_stopped && status != R.string.status_finished && status != R.string.status_metadata) {
				layoutRemaining.setVisibility(LinearLayout.VISIBLE);
				layoutDownloadSpeed.setVisibility(LinearLayout.VISIBLE);
			}
			else {
				layoutRemaining.setVisibility(LinearLayout.GONE);
				layoutDownloadSpeed.setVisibility(LinearLayout.GONE);
			}
		} else {	
			TextView tvUploaded = (TextView) reusableView.findViewById(tvUploadedResource);
			TextView tvUpSpeed = (TextView) reusableView.findViewById(tvUploadSpeedResource);

			tvUploaded.setText(item.getUploaded(context_));
			tvUpSpeed.setText(item.getUploadSpeed(context_));
			
			layoutDownload.setVisibility(LinearLayout.GONE);
			layoutUpload.setVisibility(LinearLayout.VISIBLE);
		}
		
		/*Rect bounds = progress.getProgressDrawable().getBounds();
		
		if (status == R.string.status_stopped) {
			progress.setProgressDrawable(context_.getResources().getDrawable(R.drawable.progress_blue_light));
		} else if (status == R.string.status_finished) {
			progress.setProgressDrawable(context_.getResources().getDrawable(R.drawable.progress_green_light));
		} else if (status == R.string.status_seeding) {
			progress.setProgressDrawable(context_.getResources().getDrawable(R.drawable.progress_green));
		} else {
			progress.setProgressDrawable(context_.getResources().getDrawable(R.drawable.progress_blue));
		}
		
		progress.getProgressDrawable().setBounds(bounds);*/
		
		progress.setProgress((int) item.getPercent());
		
		//progress.invalidate();
		
		return reusableView;
	}
}
