package hu.bute.daai.amorg.drtorrent;

import hu.bute.daai.amorg.drtorrent.adapter.FileListAdapter;
import hu.bute.daai.amorg.drtorrent.adapter.PeerListAdapter;
import hu.bute.daai.amorg.drtorrent.adapter.TrackerListAdapter;
import hu.bute.daai.amorg.drtorrent.adapter.item.FileListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.PeerListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.TorrentListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.TrackerListItem;
import hu.bute.daai.amorg.drtorrent.torrentengine.Bitfield;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.viewpagerindicator.TitleProvider;

public class TorrentDetailsPagerAdapter extends PagerAdapter implements TitleProvider {
	private TorrentListItem torrent_ = null;
	private ArrayList<PeerListItem> peers_;
	private ArrayList<FileListItem> files_;
	private ArrayList<TrackerListItem> trackers_;
	private PeerListAdapter<PeerListItem> peersAdapter_;
	private FileListAdapter<FileListItem> filesAdapter_;
	private TrackerListAdapter<TrackerListItem> trackersAdapter_;
	private ArrayList<PeerListItem> itemList_ = null;
	private ArrayList<FileListItem> fileItemList_ = null;
	private ArrayList<TrackerListItem> trackerItemList_ = null;
	
	private TextView tvName_ = null;
	private TextView tvStatus_ = null;
	private TextView tvPercent_ = null;
	private TextView tvSize_ = null;
	private ProgressBar progress_ = null;
	private TextView tvDownSpeed_ = null;
	private TextView tvUpSpeed_ = null;
	private TextView tvDownloaded_ = null;
	private TextView tvUploaded_ = null;
	private TextView tvPeers_ = null;
	private TextView tvElapsedTime_ = null;
	private TextView tvRemainingTime_ = null;
	private ListView lvPeers_ = null;
	private ListView lvFiles_ = null;
	private ListView lvTrackers_ = null;
	private Bitfield bitfield_ = null;
	private Bitfield downloadingBitfield_ = null;
	
	View infoView_ = null;
	View peersView_ = null;
	View filesView_ = null;
	View trackersView_ = null;
	PiecesView piecesView_ = null;

	private String[] titles_;
	private final Context context_;

	public TorrentDetailsPagerAdapter(Context context) {
		this.context_ = context;
		
		titles_ = new String[] {
			context_.getString(R.string.tab_info),
			context_.getString(R.string.tab_peers), 
			context_.getString(R.string.tab_pieces),
			context_.getString(R.string.tab_files),
			context_.getString(R.string.tab_trackers)
		};

		peers_ = new ArrayList<PeerListItem>();
		files_ = new ArrayList<FileListItem>();
		trackers_ = new ArrayList<TrackerListItem>();
		peersAdapter_ = new PeerListAdapter<PeerListItem>((Activity) context_, peers_);
		filesAdapter_ = new FileListAdapter<FileListItem>((Activity) context_, files_);
		trackersAdapter_ = new TrackerListAdapter<TrackerListItem>((Activity) context_, trackers_);
		
		piecesView_ = new PiecesView(context_);
	}

	public String getTitle(int position) {
		return titles_[position];
	}

	@Override
	public int getCount() {
		return titles_.length;
	}

	@Override
	public Object instantiateItem(View pager, int position) {
		LayoutInflater inflater = (LayoutInflater) this.context_.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View v = null;
		switch (position) {
			case 0:
				if (infoView_ == null) {
					infoView_ = inflater.inflate(R.layout.torrent_info, null);
				
					tvName_ = (TextView) infoView_.findViewById(R.id.torrent_details_tvName);
	    			tvStatus_ = (TextView) infoView_.findViewById(R.id.torrent_details_tvStatus);
	    			tvPercent_ = (TextView) infoView_.findViewById(R.id.torrent_details_tvPercent);
	    			tvSize_ = (TextView) infoView_.findViewById(R.id.torrent_details_tvSize);
	    			progress_ = (ProgressBar) infoView_.findViewById(R.id.torrent_details_progress);
	    			tvDownSpeed_ = (TextView) infoView_.findViewById(R.id.torrent_details_tvDownloadSpeed);
	    			tvUpSpeed_ = (TextView) infoView_.findViewById(R.id.torrent_details_tvUploadSpeed);
	    			tvDownloaded_ = (TextView) infoView_.findViewById(R.id.torrent_details_tvDownloaded);
	    			tvUploaded_ = (TextView) infoView_.findViewById(R.id.torrent_details_tvUploaded);
	    			tvElapsedTime_ = (TextView) infoView_.findViewById(R.id.torrent_details_tvElapsedTime);
	    			tvRemainingTime_ = (TextView) infoView_.findViewById(R.id.torrent_details_tvRemainingTime);
	    			tvPeers_ = (TextView) infoView_.findViewById(R.id.torrent_details_tvPeers);
	    			//lvPeers_ = null;
				}
				
				v = infoView_;
	    		if (torrent_ != null) refreshTorrentItem(torrent_, false);
				break;
			case 1:
				if (peersView_ == null) {
					peersView_ = inflater.inflate(R.layout.list, null);
					
					/*tvName_ = null;
	    			tvStatus_ = null;
	    			tvPercent_ = null;
	    			progress_ = null;
	    			tvDownSpeed_ = null;
	    			tvUpSpeed_ = null;
	    			tvDownloaded_ = null;
	    			tvUploaded_ = null;
	    			tvElapsedTime_ = null;
	    			tvRemainingTime_ = null;
	    			tvPeers_ = null;*/
					lvPeers_ = (ListView) peersView_.findViewById(R.id.list_listview);
					lvPeers_.setAdapter(peersAdapter_);
				}
				
				v = peersView_;
				if (itemList_ != null) refreshPeerList(itemList_);
    			
				break;
			case 2:
				v = piecesView_;
				break;
			case 3:
				if (filesView_ == null) {
					filesView_ = inflater.inflate(R.layout.list, null);
					
					lvFiles_ = (ListView) filesView_.findViewById(R.id.list_listview);
					lvFiles_.setAdapter(filesAdapter_);
					lvFiles_.setOnItemClickListener(new OnItemClickListener() {
						@Override
						public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
							FileListItem item = filesAdapter_.getItem(position);
							File file = new File(item.getPath());
							
			                String extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
			                String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
			             
			                Intent intent = new Intent(Intent.ACTION_VIEW);
			                intent.setDataAndType(Uri.fromFile(file), type);
							context_.startActivity(intent);
						}
					});
				}
				
				v = filesView_;
				if (fileItemList_ != null) refreshFileList(fileItemList_);
				
				break;
			case 4:
				if (trackersView_ == null) {
					trackersView_ = inflater.inflate(R.layout.list, null);
					
					lvTrackers_ = (ListView) trackersView_.findViewById(R.id.list_listview);
					lvTrackers_.setAdapter(trackersAdapter_);
				}
				
				v = trackersView_;
				if (trackerItemList_ != null) refreshTrackerList(trackerItemList_);
				
				break;
			default:
		}

		if (v != null) {
			((ViewPager) pager).addView(v, 0);
		}

		return v;
	}
	
	public void refreshTorrentItem(TorrentListItem item, boolean isRemoved) {
		torrent_ = item;
		if (tvPeers_ != null) {
			tvName_.setText(item.getName());
			tvStatus_.setText(context_.getString(item.getStatus()));
			tvPercent_.setText(item.getPercent() + " %");
			tvSize_.setText(item.getSize());
			progress_.setProgress(item.getPercent());
			tvDownSpeed_.setText(item.getDownloadSpeed());
			tvUpSpeed_.setText(item.getUploadSpeed());
			tvDownloaded_.setText(item.getDownloaded());
			tvUploaded_.setText(item.getUploaded());
			tvElapsedTime_.setText(item.getElapsedTime());
			tvRemainingTime_.setText(item.getRemainingTime());
			tvPeers_.setText(item.getPeers());
		}
	}
	
	public void refreshPeerList(ArrayList<PeerListItem> itemList) {
		boolean foundOld = false;
		for (int i = 0; i < peersAdapter_.getCount() && i >= 0; i++) {
			foundOld = false;
			for (int j = 0; j < itemList.size(); j++) {
				if (peersAdapter_.getItem(i).equals(itemList.get(j))) {
					foundOld = true;
					peersAdapter_.getItem(i).set(itemList.get(j));
					itemList.remove(j);
				}
			}
			if (!foundOld) {
				peersAdapter_.remove(peersAdapter_.getItem(i));
				i--;
			}
		}

		if (itemList == null) return;
		for (int i = 0; i < itemList.size(); i++) {
			peersAdapter_.add(itemList.get(i));
		}

		if (lvPeers_ != null) lvPeers_.invalidateViews();
	}
	
	public void refreshFileList(ArrayList<FileListItem> fileItemList) {
		boolean foundOld = false;
		for (int i = 0; i < filesAdapter_.getCount() && i >= 0; i++) {
			foundOld = false;
			for (int j = 0; j < fileItemList.size(); j++) {
				if (filesAdapter_.getItem(i).equals(fileItemList.get(j))) {
					foundOld = true;
					filesAdapter_.getItem(i).set(fileItemList.get(j));
					fileItemList.remove(j);
				}
			}
			if (!foundOld) {
				filesAdapter_.remove(filesAdapter_.getItem(i));
				i--;
			}
		}

		if (fileItemList == null) return;
		for (int i = 0; i < fileItemList.size(); i++) {
			filesAdapter_.add(fileItemList.get(i));
		}

		if (lvFiles_ != null) lvFiles_.invalidateViews();
	}
	
	public void refreshTrackerList(ArrayList<TrackerListItem> trackerItemList) {
		boolean foundOld = false;
		for (int i = 0; i < trackersAdapter_.getCount() && i >= 0; i++) {
			foundOld = false;
			for (int j = 0; j < trackerItemList.size(); j++) {
				if (trackersAdapter_.getItem(i).equals(trackerItemList.get(j))) {
					foundOld = true;
					trackersAdapter_.getItem(i).set(trackerItemList.get(j));
					trackerItemList.remove(j);
				}
			}
			if (!foundOld) {
				trackersAdapter_.remove(trackersAdapter_.getItem(i));
				i--;
			}
		}

		if (trackerItemList == null) return;
		for (int i = 0; i < trackerItemList.size(); i++) {
			trackersAdapter_.add(trackerItemList.get(i));
		}

		if (lvTrackers_ != null) lvTrackers_.invalidateViews();
	}
	
	public void refreshBitfield(Bitfield bitfield, Bitfield downloadingBitfield) {
		bitfield_ = bitfield;
		downloadingBitfield_ = downloadingBitfield;
		if (piecesView_ !=  null) piecesView_.updateBitfield(bitfield_, downloadingBitfield_);
	}
	
	@Override
	public void destroyItem(View pager, int position, Object view) {
		((ViewPager) pager).removeView((View) view);
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view.equals(object);
	}

	@Override
	public void finishUpdate(View view) {
	}

	@Override
	public void restoreState(Parcelable p, ClassLoader c) {
	}

	@Override
	public Parcelable saveState() {
		return null;
	}

	@Override
	public void startUpdate(View view) {
	}
}