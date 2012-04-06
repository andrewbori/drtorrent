package hu.bute.daai.amorg.drtorrent.activity;

import hu.bute.daai.amorg.drtorrent.PeerListItem;
import hu.bute.daai.amorg.drtorrent.TorrentListItem;
import hu.bute.daai.amorg.drtorrent.service.TorrentService;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.Toast;

public abstract class TorrentHostActivity extends Activity {
	public static final String KEY_INFO_HASH = "infoHash";
	
	protected static final int MENU_ADD_TORRENT = 101;
	protected static final int MENU_STOP_TORRENT = 102;
	protected static final int RESULT_FILEBROWSER_ACTIVITY = 201;
	
	protected Activity activity_ = this;
	protected String infoHash_ = null;
	
	protected Messenger serviceMessenger_ = null;
	protected final Messenger clientMessenger_ = new Messenger(new IncomingMessageHandler());
	private boolean isBound_ = false;
	
	protected AlertDialog dialog_;
	protected ProgressDialog progressDialog_;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			infoHash_ = extras.getString(KEY_INFO_HASH);
		}
		
		Intent i = new Intent(activity_, TorrentService.class);
		startService(i);

		AlertDialog.Builder builder = new AlertDialog.Builder(activity_);
		builder.setCancelable(false)
		       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dialog.cancel();
		           }
		       });
		dialog_ = builder.create();

		progressDialog_ = new ProgressDialog(activity_);
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
			if (infoHash_ != null) {
				Bundle bundle = new Bundle();
				bundle.putString(TorrentService.MSG_KEY_TORRENT_INFOHASH, infoHash_);
				msg.setData(bundle);
			}
			msg.replyTo = clientMessenger_;
			try {
				serviceMessenger_.send(msg);
			} catch (RemoteException e) {}
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
				if (infoHash_ != null) {
					Bundle bundle = new Bundle();
					bundle.putString(TorrentService.MSG_KEY_TORRENT_INFOHASH, infoHash_);
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
					Toast.makeText(activity_, message, Toast.LENGTH_SHORT).show();
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
					TorrentListItem torrent = (TorrentListItem) bundle.getSerializable(TorrentService.MSG_KEY_TORRENT_ITEM);
					refreshTorrentItem(torrent);
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
					
				default:
					super.handleMessage(msg);
			}
		}
	}
	
	protected void refreshTorrentItem(TorrentListItem item) {}
	
	protected void refreshTorrentList(ArrayList<TorrentListItem> list) {}

	protected void refreshPeerItem(PeerListItem item, boolean isDisconnected) {}
	
	protected void refreshPeerList(ArrayList<PeerListItem> list) {}
}
