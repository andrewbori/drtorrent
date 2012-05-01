package hu.bute.daai.amorg.drtorrent;

import hu.bute.daai.amorg.drtorrent.torrentengine.Bitfield;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.viewpagerindicator.TitleProvider;

public class TorrentDetailsPagerAdapter extends PagerAdapter implements TitleProvider {
	private TorrentListItem torrent_ = null;
	private ArrayList<PeerListItem> peers_;
	private PeerListAdapter<PeerListItem> peersAdapter_;
	private ArrayList<PeerListItem> itemList_ = null;
	
	private TextView tvName_ = null;
	private TextView tvStatus_ = null;
	private TextView tvPercent_ = null;
	private TextView tvSize_ = null;
	private ProgressBar progress_ = null;
	private TextView tvDownSpeed_ = null;
	private TextView tvUpSpeed_ = null;
	private TextView tvDownloaded_ = null;
	private TextView tvPeers_ = null;
	private TextView tvElapsedTime_ = null;
	private TextView tvRemainingTime_ = null;
	private ListView lvPeers_ = null;
	private Bitfield bitfield_ = null;
	
	View infoView_ = null;
	View peersView_ = null;
	PiecesView piecesView_ = null;

	private static String[] titles_ = new String[] { "Info", "Peers", "Pieces" };
	private final Context context_;

	public TorrentDetailsPagerAdapter(Context context) {
		this.context_ = context;

		peers_ = new ArrayList<PeerListItem>();
		peersAdapter_ = new PeerListAdapter<PeerListItem>((Activity) context_, peers_);
		
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
					peersView_ = inflater.inflate(R.layout.torrent_peers, null);
					
					/*tvName_ = null;
	    			tvStatus_ = null;
	    			tvPercent_ = null;
	    			progress_ = null;
	    			tvDownSpeed_ = null;
	    			tvUpSpeed_ = null;
	    			tvDownloaded_ = null;
	    			tvElapsedTime_ = null;
	    			tvRemainingTime_ = null;
	    			tvPeers_ = null;*/
					lvPeers_ = (ListView) peersView_.findViewById(R.id.torrent_details_lvPeer);
					lvPeers_.setAdapter(peersAdapter_);
				}
				
				v = peersView_;
				if (itemList_ != null) refreshPeerList(itemList_);
    			
				break;
			case 2:
				v = piecesView_;
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
	
	public void refreshBitfield(Bitfield bitfield) {
		bitfield_ = bitfield;
		if (piecesView_ !=  null) piecesView_.updateBitfield(bitfield_);
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