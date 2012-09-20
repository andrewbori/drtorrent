package hu.bute.daai.amorg.drtorrent;

import hu.bute.daai.amorg.drtorrent.activity.TorrentDetailsActivity;
import hu.bute.daai.amorg.drtorrent.adapter.FileListAdapter;
import hu.bute.daai.amorg.drtorrent.adapter.PeerListAdapter;
import hu.bute.daai.amorg.drtorrent.adapter.TrackerListAdapter;
import hu.bute.daai.amorg.drtorrent.adapter.item.FileListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.PeerListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.TorrentListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.TrackerListItem;
import hu.bute.daai.amorg.drtorrent.torrentengine.Bitfield;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.viewpagerindicator.TitleProvider;

public class TorrentDetailsPagerAdapter extends PagerAdapter implements TitleProvider {
	private TorrentListItem torrent_ = null;
	
	private int updateField_ = 0;
	
	private PeerListAdapter<PeerListItem> peersAdapter_;
	private FileListAdapter<FileListItem> filesAdapter_;
	private TrackerListAdapter<TrackerListItem> trackersAdapter_;
	
	private ListView lvPeers_ = null;
	private ListView lvFiles_ = null;
	private ListView lvTrackers_ = null;
	
	private TextView tvName_ = null;
	private TextView tvStatus_ = null;
	private TextView tvPercent_ = null;
	private TextView tvSize_ = null;
	private TextView tvReady_ = null;
	private ProgressBar progress_ = null;
	private TextView tvDownSpeed_ = null;
	private TextView tvUpSpeed_ = null;
	private TextView tvDownloaded_ = null;
	private TextView tvUploaded_ = null;
	private TextView tvPeers_ = null;
	private TextView tvElapsedTime_ = null;
	private TextView tvRemainingTime_ = null;
	
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
			context_.getString(R.string.tab_files),
			context_.getString(R.string.tab_pieces),
			context_.getString(R.string.tab_peers),
			context_.getString(R.string.tab_trackers)
		};
		
		peersAdapter_ = new PeerListAdapter<PeerListItem>((Activity) context_, new ArrayList<PeerListItem>());
		filesAdapter_ = new FileListAdapter<FileListItem>((Activity) context_, new ArrayList<FileListItem>());
		trackersAdapter_ = new TrackerListAdapter<TrackerListItem>((Activity) context_, new ArrayList<TrackerListItem>());
		
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
	    			progress_ = (ProgressBar) infoView_.findViewById(R.id.torrent_details_progress);
	    			tvSize_ = (TextView) infoView_.findViewById(R.id.torrent_details_tvSize);
	    			tvReady_ = (TextView) infoView_.findViewById(R.id.torrent_details_tvReady);
	    			tvDownloaded_ = (TextView) infoView_.findViewById(R.id.torrent_details_tvDownloaded);
	    			tvDownSpeed_ = (TextView) infoView_.findViewById(R.id.torrent_details_tvDownloadSpeed);
	    			tvUploaded_ = (TextView) infoView_.findViewById(R.id.torrent_details_tvUploaded);
	    			tvUpSpeed_ = (TextView) infoView_.findViewById(R.id.torrent_details_tvUploadSpeed);
	    			tvElapsedTime_ = (TextView) infoView_.findViewById(R.id.torrent_details_tvElapsedTime);
	    			tvRemainingTime_ = (TextView) infoView_.findViewById(R.id.torrent_details_tvRemainingTime);
	    			tvPeers_ = (TextView) infoView_.findViewById(R.id.torrent_details_tvPeers);
				}
				
				v = infoView_;
	    		if (torrent_ != null) refreshTorrentItem(torrent_, false);
				break;
			case 1:
				if (filesView_ == null) {
					filesView_ = inflater.inflate(R.layout.list, null);
					
					lvFiles_ = (ListView) filesView_.findViewById(R.id.list_listview);
					lvFiles_.setAdapter(filesAdapter_);
					lvFiles_.setOnItemClickListener(new OnItemClickListener() {
						@Override
						public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
							FileListItem item = filesAdapter_.getItem(position);
							String filePath = item.getPath();
							File file = new File(filePath);
							
			                String extension = filePath.substring(filePath.lastIndexOf(".") + 1); // MimeTypeMap.getFileExtensionFromUrl(filePath);
			                String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
			             
			                Intent intent = new Intent(Intent.ACTION_VIEW);
			                intent.setDataAndType(Uri.fromFile(file), type);
							context_.startActivity(intent);
						}
					});
					lvFiles_.setOnItemLongClickListener(new OnItemLongClickListener() {
						@Override
						public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
							final FileListItem file = filesAdapter_.getItem(position);
							
							final CharSequence[] items = {
								context_.getString(R.string.priority_skip),
								context_.getString(R.string.priority_low),
								context_.getString(R.string.priority_normal),
								context_.getString(R.string.priority_high)
							};

							AlertDialog.Builder builder = new AlertDialog.Builder(context_);
							builder.setTitle(file.getName());
							builder.setSingleChoiceItems(items, file.getPriority(), new DialogInterface.OnClickListener() {
							    public void onClick(DialogInterface dialog, int priority) {
							    	file.setPriority(priority);
							        lvFiles_.invalidateViews();
							        dialog.cancel();
							        
							        FileListItem item = new FileListItem(file);
							        item.setPriority(priority);
							        ((TorrentDetailsActivity) context_).changeFilePriority(item);
							    }
							});
							final AlertDialog dialog = builder.create();
							dialog.show();
							
							return true;
						}
					});
				}
				
				v = filesView_;
				// if (files_ != null) refreshFileList(files_);
    			
				break;
			case 2:
				v = piecesView_;
				break;
			case 3:
				if (peersView_ == null) {
					peersView_ = inflater.inflate(R.layout.list, null);

					lvPeers_ = (ListView) peersView_.findViewById(R.id.list_listview);
					lvPeers_.setAdapter(peersAdapter_);
				}
				
				v = peersView_;
				// if (peers_ != null) refreshPeerList(peers_);
				
				break;
			case 4:
				if (trackersView_ == null) {
					trackersView_ = inflater.inflate(R.layout.list, null);
					
					lvTrackers_ = (ListView) trackersView_.findViewById(R.id.list_listview);
					lvTrackers_.setAdapter(trackersAdapter_);
				}
				
				v = trackersView_;
				// if (trackers_ != null) refreshTrackerList(trackers_);
				
				break;
			default:
		}

		if (v != null) {
			((ViewPager) pager).addView(v, 0);
		}
		
		updateTorrent(position);
		
		return v;
	}
	
	public void refreshTorrentItem(TorrentListItem item, boolean isRemoved) {
		torrent_ = item;
		if (tvPeers_ != null) {
			tvName_.setText(item.getName());
			tvStatus_.setText(context_.getString(item.getStatus()));
			
			DecimalFormat dec = new DecimalFormat("###.#");
			tvPercent_.setText(dec.format(item.getPercent()).concat(" %"));
			progress_.setProgress((int) item.getPercent());
			
			tvSize_.setText(item.getSize());
			tvReady_.setText(item.getReady());
			tvDownloaded_.setText(item.getDownloaded());
			tvDownSpeed_.setText(item.getDownloadSpeed());
			tvUploaded_.setText(item.getUploaded());
			tvUpSpeed_.setText(item.getUploadSpeed());
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
		for (int i = 0; i < filesAdapter_.getCount() && i >= 0; i++) {
			for (int j = 0; j < fileItemList.size(); j++) {
				if (filesAdapter_.getItem(i).equals(fileItemList.get(j))) {
					filesAdapter_.getItem(i).set(fileItemList.get(j));
					fileItemList.remove(j);
				}
			}
		}

		if (fileItemList == null) return;
		for (int i = 0; i < fileItemList.size(); i++) {
			filesAdapter_.add(fileItemList.get(i));
		}

		if (lvFiles_ != null) lvFiles_.invalidateViews();
	}
	
	public void refreshTrackerList(ArrayList<TrackerListItem> trackerItemList) {
		for (int i = 0; i < trackersAdapter_.getCount() && i >= 0; i++) {
			for (int j = 0; j < trackerItemList.size(); j++) {
				if (trackersAdapter_.getItem(i).equals(trackerItemList.get(j))) {
					trackersAdapter_.getItem(i).set(trackerItemList.get(j));
					trackerItemList.remove(j);
				}
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
	
	public void updateTorrent(int position) {
		switch (position) {
			case 0: updateField_ ^=  1; break;
			case 1: updateField_ ^=  2; break;
			case 2: updateField_ ^=  4; break;
			case 3: updateField_ ^=  8; break;
			case 4: updateField_ ^= 16; break;
			default: break;
		}
		((TorrentDetailsActivity) context_).updateTorrent(updateField_);
	}
	
	@Override
	public void destroyItem(View pager, int position, Object view) {
		((ViewPager) pager).removeView((View) view);
		updateTorrent(position);
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