package hu.bute.daai.amorg.drtorrent.activity;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.adapter.item.FileListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.PeerListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.TorrentListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.TrackerListItem;
import hu.bute.daai.amorg.drtorrent.service.TorrentService;
import hu.bute.daai.amorg.drtorrent.torrentengine.Bitfield;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;

public abstract class TorrentHostActivity extends SherlockActivity {
	public static final String KEY_TORRENT_ID = "id";
	
	protected static final int MENU_ADD_TORRENT     = 101;
	protected static final int MENU_START_TORRENT   = 102;
	protected static final int MENU_STOP_TORRENT    = 103;
	protected static final int MENU_DELETE_TORRENT  = 104;
	protected static final int MENU_ADD_TRACKER     = 105;
	protected static final int MENU_ADD_PEER 	    = 106;
	protected static final int MENU_ADD_MAGNET_LINK = 107;
	protected static final int MENU_SEARCH_TORRENT  = 110;
	protected static final int MENU_SEARCH_TORRENT2 = 111;
	protected static final int MENU_SETTINGS        = 112;
	protected static final int MENU_ABOUT 		    = 113;
	protected static final int MENU_FEEDBACK	    = 114;
	protected static final int MENU_RATE_APP		= 115;
	protected static final int MENU_SHUT_DOWN       = 116;
	
	protected final static String SHUT_DOWN = "shut_down";
	protected boolean isShuttingDown_ = false; 
	
	protected static final int RESULT_FILEBROWSER_ACTIVITY = 201;
	protected static final int RESULT_TORRENT_SETTINGS     = 202;
	
	protected Activity context_ = this;
	protected int torrentId_ = -1;
	
	protected Messenger serviceMessenger_ = null;
	protected final Messenger clientMessenger_ = new Messenger(new IncomingMessageHandler(this));
	private boolean isBound_ = false;
	
	protected ProgressDialog progressDialog_ = null;

	protected boolean isOpening_ = false;
	protected Uri fileToOpen_ = null;
	
	protected AlertDialog dialog_ = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.Theme_Sherlock_ForceOverflow);
		super.onCreate(savedInstanceState);
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			torrentId_ = extras.getInt(KEY_TORRENT_ID, -1);
			
			isShuttingDown_ = extras.getBoolean(SHUT_DOWN, false);
			if (isShuttingDown_) {
				return;
			}
		}
		
		final Intent i = new Intent(context_, TorrentService.class);
		startService(i);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (isShuttingDown_) {
			finish();
			return;
		}
		doBindService();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (isShuttingDown_) {
			return;
		}
		doUnbindService();
	}
	
	/** Connection of the Torrent Service. */
	final protected ServiceConnection serviceConnection = new ServiceConnection() {
		
		public void onServiceConnected(ComponentName name, IBinder service) {
			serviceMessenger_ = new Messenger(service);
			isBound_ = true;

			Message msg = Message.obtain();
			msg.what = TorrentService.MSG_SUBSCRIBE_CLIENT;
			if (torrentId_ != -1) {
				Bundle bundle = new Bundle();
				bundle.putInt(TorrentService.MSG_KEY_TORRENT_ID, torrentId_);
				msg.setData(bundle);
			}
			msg.replyTo = clientMessenger_;
			try {
				serviceMessenger_.send(msg);
			} catch (RemoteException e) {}
			
			if (fileToOpen_ != null) {
				openTorrent(fileToOpen_);
				fileToOpen_ = null;
			}
		}

		public void onServiceDisconnected(ComponentName name) {
			serviceMessenger_ = null;
			isBound_ = false;
		}
	};

	/** Bind to the Torrent Service. */
	protected void doBindService() {
		bindService(new Intent(this, TorrentService.class), serviceConnection, Context.BIND_AUTO_CREATE);
	}

	/** Unbind from the Torrent Service. */
	protected void doUnbindService() {
		if (isBound_) {
			if (serviceMessenger_ != null) {
				Message msg = Message.obtain();
				msg.what = TorrentService.MSG_UNSUBSCRIBE_CLIENT;
				if (torrentId_ != -1) {
					Bundle bundle = new Bundle();
					bundle.putInt(TorrentService.MSG_KEY_TORRENT_ID, torrentId_);
					msg.setData(bundle);
				}
				msg.replyTo = clientMessenger_;
				try {
					serviceMessenger_.send(msg);
				} catch (RemoteException e) {}
			}

			unbindService(serviceConnection);
		}
	}

	/** Handler of incoming Messages. */
	private static class IncomingMessageHandler extends Handler {
		
		private final WeakReference<TorrentHostActivity> activity_; 

		IncomingMessageHandler(TorrentHostActivity activity) {
	    	activity_ = new WeakReference<TorrentHostActivity>(activity);
	    }

		@Override
		public void handleMessage(Message msg) {
			TorrentHostActivity activity = activity_.get();
			if (activity != null) {
				activity.handleMessage(msg);
			}
		}
	}	

	/** Handling the incoming message. */
	public void handleMessage(Message msg) {
		Bundle bundle = msg.getData();
		String message;
		
		switch (msg.what) {
			case TorrentService.MSG_SHOW_TOAST:
				message = bundle.getString(TorrentService.MSG_KEY_MESSAGE);
				Toast.makeText(context_, message, Toast.LENGTH_SHORT).show();
				break;
				
			case TorrentService.MSG_SHOW_DIALOG:
				message = bundle.getString(TorrentService.MSG_KEY_MESSAGE);
				
				AlertDialog.Builder builder = new AlertDialog.Builder(context_);
				builder.setCancelable(false)
				       .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				        	   dialog.dismiss();
				           }
				       }).
			    setMessage(message);
			    dialog_ = builder.create();
			    dialog_.show();
				break;
			
			case TorrentService.MSG_SHOW_PROGRESS:
				if (progressDialog_ != null) {
					progressDialog_.dismiss();
					progressDialog_ = null;
				}
				message = bundle.getString(TorrentService.MSG_KEY_MESSAGE);
				progressDialog_ = new ProgressDialog(context_);
				progressDialog_.setMessage(message);
				progressDialog_.show();
				break;
				
			case TorrentService.MSG_HIDE_PROGRESS:
				if (progressDialog_ != null) {
					progressDialog_.dismiss();
					progressDialog_ = null;
				}
				break;
				
			case TorrentService.MSG_SEND_TORRENT_ITEM:
				final TorrentListItem item = (TorrentListItem) bundle.getSerializable(TorrentService.MSG_KEY_TORRENT_ITEM);
				boolean isRemoved = bundle.getBoolean(TorrentService.MSG_KEY_IS_REMOVED, false); 
				refreshTorrentItem(item, isRemoved);
				break;

			case TorrentService.MSG_SEND_TORRENT_LIST:
				@SuppressWarnings("unchecked")
				final ArrayList<TorrentListItem> torrents = ((ArrayList<TorrentListItem>) bundle.getSerializable(TorrentService.MSG_KEY_TORRENT_LIST));
				refreshTorrentList(torrents);
				break;
				
			case TorrentService.MSG_SEND_PEER_ITEM:
				final PeerListItem peer = (PeerListItem) bundle.getSerializable(TorrentService.MSG_KEY_PEER_ITEM);
				boolean isDisconnected = bundle.getBoolean(TorrentService.MSG_KEY_IS_DISCONNECTED);
				refreshPeerItem(peer, isDisconnected);
				break;
				
			case TorrentService.MSG_SEND_PEER_LIST:
				@SuppressWarnings("unchecked")
				final ArrayList<PeerListItem> peers = ((ArrayList<PeerListItem>) bundle.getSerializable(TorrentService.MSG_KEY_PEER_LIST));
				refreshPeerList(peers);
				break;
				
			case TorrentService.MSG_SEND_FILE_LIST:
				@SuppressWarnings("unchecked")
				final ArrayList<FileListItem> files = ((ArrayList<FileListItem>) bundle.getSerializable(TorrentService.MSG_KEY_FILE_LIST));
				refreshFileList(files);
				break;
			
			case TorrentService.MSG_TORRENT_ALREADY_OPENED:
				String infoHash = bundle.getString(TorrentService.MSG_KEY_TORRENT_INFOHASH);
				final ArrayList<String> trackerUrls = bundle.getStringArrayList(TorrentService.MSG_KEY_TRACKER_LIST);
				showTorrentAlreadyOpened(infoHash, trackerUrls);
				break;	
				
			case TorrentService.MSG_SEND_TORRENT_SETTINGS:
				TorrentListItem torrent = (TorrentListItem) bundle.getSerializable(TorrentService.MSG_KEY_TORRENT_ITEM);
				@SuppressWarnings("unchecked")
				final ArrayList<FileListItem> fileList = ((ArrayList<FileListItem>) bundle.getSerializable(TorrentService.MSG_KEY_FILE_LIST));
				showTorrentSettings(torrent, fileList);
				break;
				
			case TorrentService.MSG_SEND_TRACKER_LIST:
				@SuppressWarnings("unchecked")
				final ArrayList<TrackerListItem> trackers = ((ArrayList<TrackerListItem>) bundle.getSerializable(TorrentService.MSG_KEY_TRACKER_LIST));
				refreshTrackerList(trackers);
				break;
				
			case TorrentService.MSG_SEND_BITFIELD:
				Bitfield bitfield = (Bitfield) bundle.getSerializable(TorrentService.MSG_KEY_BITFIELD);
				Bitfield downloadingBitfield = (Bitfield) bundle.getSerializable(TorrentService.MSG_KEY_DOWNLOADING_BITFIELD);
				refreshBitfield(bitfield, downloadingBitfield);
				
			default:
		}
	}
	
	protected void openTorrent(Uri filePath) {}
	
	protected void showTorrentSettings(TorrentListItem torrent, ArrayList<FileListItem> fileList) {}
	
	protected void refreshTorrentItem(TorrentListItem item, boolean isRemoved) {}

	protected void refreshTorrentList(ArrayList<TorrentListItem> list) {}

	protected void refreshPeerItem(PeerListItem item, boolean isDisconnected) {}
	
	protected void refreshPeerList(ArrayList<PeerListItem> list) {}
	
	protected void refreshBitfield(Bitfield bitfield, Bitfield downloadingBitfield) {}
	
	protected void refreshFileList(ArrayList<FileListItem> list) {}
	
	protected void refreshTrackerList(ArrayList<TrackerListItem> list) {}
	
	protected void showTorrentAlreadyOpened(final String infoHash, final ArrayList<String> trackerUrls) {		
		AlertDialog.Builder builder = new AlertDialog.Builder(context_);
		builder.setCancelable(false).
		setMessage(R.string.torrent_already_opened_add_trackers).
		setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
    	   @Override
    	   public void onClick(DialogInterface dialog, int id) {
    		    Message msg = Message.obtain();
    			
    		    Bundle bundle = new Bundle();
    			bundle.putString(TorrentService.MSG_KEY_TORRENT_INFOHASH, infoHash);
    			bundle.putStringArrayList(TorrentService.MSG_KEY_TRACKER_LIST, trackerUrls);
    			
    			msg.what = TorrentService.MSG_TORRENT_ALREADY_OPENED;
    			msg.setData(bundle);
	   			msg.replyTo = clientMessenger_;
	   			
	   			try {
	   				serviceMessenger_.send(msg);
	   			} catch (RemoteException e) {}
	   			
        	    dialog.dismiss();
           }
		}).
		setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		dialog_ = builder.create();
		dialog_.show();
	}
	
	@Override
	protected void onDestroy() {
		if (dialog_ != null) {
			dialog_.dismiss();
			dialog_ = null;
		}
		super.onDestroy();
	}
}
