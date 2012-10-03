package hu.bute.daai.amorg.drtorrent.activity;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.adapter.item.FileListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.PeerListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.TorrentListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.TrackerListItem;
import hu.bute.daai.amorg.drtorrent.service.TorrentService;
import hu.bute.daai.amorg.drtorrent.torrentengine.Bitfield;

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
	
	protected static final int MENU_ADD_TORRENT    = 101;
	protected static final int MENU_START_TORRENT  = 102;
	protected static final int MENU_STOP_TORRENT   = 103;
	protected static final int MENU_DELETE_TORRENT = 104;
	protected static final int MENU_ADD_TRACKER    = 105;
	protected static final int MENU_ADD_PEER 	   = 106;
	protected static final int MENU_SETTINGS       = 111;
	protected static final int MENU_SHUT_DOWN      = 112;
	
	protected static final int RESULT_FILEBROWSER_ACTIVITY = 201;
	protected static final int RESULT_TORRENT_SETTINGS     = 202;
	
	protected Activity context_ = this;
	protected int torrentId_ = -1;
	
	protected Messenger serviceMessenger_ = null;
	protected final Messenger clientMessenger_ = new Messenger(new IncomingMessageHandler());
	private boolean isBound_ = false;
	
	protected AlertDialog dialog_;
	protected ProgressDialog progressDialog_;

	protected Uri fileToOpen_ = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.Theme_Sherlock);
		super.onCreate(savedInstanceState);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			torrentId_ = extras.getInt(KEY_TORRENT_ID, -1);
		}
		
		Intent i = new Intent(context_, TorrentService.class);
		startService(i);

		AlertDialog.Builder builder = new AlertDialog.Builder(context_);
		builder.setCancelable(false)
		       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dialog.cancel();
		           }
		       });
		dialog_ = builder.create();

		progressDialog_ = new ProgressDialog(context_);
	}

	@Override
	protected void onStart() {
		super.onStart();
		doBindService();
	}

	@Override
	protected void onStop() {
		super.onStop();
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
	protected class IncomingMessageHandler extends Handler {

		@Override
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
					dialog_.setMessage(message);
					dialog_.show();
					break;
				
				case TorrentService.MSG_SHOW_PROGRESS:
					message = bundle.getString(TorrentService.MSG_KEY_MESSAGE);
					progressDialog_.setMessage(message);
					progressDialog_.show();
					break;
					
				case TorrentService.MSG_HIDE_PROGRESS:
					progressDialog_.hide();
					break;
					
				case TorrentService.MSG_SEND_TORRENT_ITEM:
					TorrentListItem item = (TorrentListItem) bundle.getSerializable(TorrentService.MSG_KEY_TORRENT_ITEM);
					boolean isRemoved = bundle.getBoolean(TorrentService.MSG_KEY_IS_REMOVED, false); 
					refreshTorrentItem(item, isRemoved);
					break;

				case TorrentService.MSG_SEND_TORRENT_LIST:
					@SuppressWarnings("unchecked")
					ArrayList<TorrentListItem> torrents = ((ArrayList<TorrentListItem>) bundle.getSerializable(TorrentService.MSG_KEY_TORRENT_LIST));
					refreshTorrentList(torrents);
					break;
					
				case TorrentService.MSG_SEND_PEER_ITEM:
					PeerListItem peer = (PeerListItem) bundle.getSerializable(TorrentService.MSG_KEY_PEER_ITEM);
					boolean isDisconnected = bundle.getBoolean(TorrentService.MSG_KEY_IS_DISCONNECTED);
					refreshPeerItem(peer, isDisconnected);
					break;
					
				case TorrentService.MSG_SEND_PEER_LIST:
					@SuppressWarnings("unchecked")
					ArrayList<PeerListItem> peers = ((ArrayList<PeerListItem>) bundle.getSerializable(TorrentService.MSG_KEY_PEER_LIST));
					refreshPeerList(peers);
					break;
					
				case TorrentService.MSG_SEND_FILE_LIST:
					@SuppressWarnings("unchecked")
					ArrayList<FileListItem> files = ((ArrayList<FileListItem>) bundle.getSerializable(TorrentService.MSG_KEY_FILE_LIST));
					refreshFileList(files);
					break;
					
				case TorrentService.MSG_SEND_TORRENT_SETTINGS:
					TorrentListItem torrent = (TorrentListItem) bundle.getSerializable(TorrentService.MSG_KEY_TORRENT_ITEM);
					@SuppressWarnings("unchecked")
					ArrayList<FileListItem> fileList = ((ArrayList<FileListItem>) bundle.getSerializable(TorrentService.MSG_KEY_FILE_LIST));
					showTorrentSettings(torrent, fileList);
					break;
					
				case TorrentService.MSG_SEND_TRACKER_LIST:
					@SuppressWarnings("unchecked")
					ArrayList<TrackerListItem> trackers = ((ArrayList<TrackerListItem>) bundle.getSerializable(TorrentService.MSG_KEY_TRACKER_LIST));
					refreshTrackerList(trackers);
					break;
					
				case TorrentService.MSG_SEND_BITFIELD:
					Bitfield bitfield = (Bitfield) bundle.getSerializable(TorrentService.MSG_KEY_BITFIELD);
					Bitfield downloadingBitfield = (Bitfield) bundle.getSerializable(TorrentService.MSG_KEY_DOWNLOADING_BITFIELD);
					refreshBitfield(bitfield, downloadingBitfield);
					
				default:
					super.handleMessage(msg);
			}
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
}
