package hu.bute.daai.amorg.drtorrent.service;

import hu.bute.daai.amorg.drtorrent.TorrentListItem;
import hu.bute.daai.amorg.drtorrent.activity.DrTorrentActivity;
import hu.bute.daai.amorg.drtorrent.torrentengine.Torrent;
import hu.bute.daai.amorg.drtorrent.torrentengine.TorrentManager;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

/** Torrent service. */
public class TorrentService extends Service {
	private final static String LOG_TAG = "TorrentService";

	public final static int MSG_TORRENT_OPEN       = 1;
	public final static int MSG_TORRENT_START      = 2;
	public final static int MSG_TORRENT_STOP       = 3;
	public final static int MSG_TORRENT_CLOSE      = 4;
	public final static int MSG_SUBSCRIBE_CLIENT   = 5;
	public final static int MSG_UNSUBSCRIBE_CLIENT = 6;
	public final static int MSG_GET_TORRENT_ITEM   = 7;
	public final static int MSG_GET_TORRENT_LIST   = 8;
	
	public final static String MSG_KEY_FILEPATH         = "filepath";
	public final static String MSG_KEY_TORRENT_INFOHASH = "torrentinfohash";
	public final static String MSG_KEY_TORRENT_ITEM     = "torrentitem";
	public final static String MSG_KEY_TORRENT_LIST     = "torrentlist";

	private final Messenger serviceMessenger_ = new Messenger(new IncomingMessageHandler());

	private TorrentManager torrentManager_;

	private Vector<Messenger> clientsAll_;
	private Hashtable<String, Vector<Messenger>> clientsSingle_;

	private Vector<TorrentListItem> torrents_; // only for sending message

	@Override
	public void onCreate() {
		super.onCreate();

		torrentManager_ = new TorrentManager(this);
		clientsAll_ = new Vector<Messenger>();
		clientsSingle_ = new Hashtable<String, Vector<Messenger>>();
		torrents_ = new Vector<TorrentListItem>();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return serviceMessenger_.getBinder();
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	/** Handler of incoming Messages from clients. */
	class IncomingMessageHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Bundle bundleMsg = msg.getData();
			String torrentInfoHash = null;

			switch (msg.what) {

				case MSG_SUBSCRIBE_CLIENT:
					torrentInfoHash = bundleMsg.getString(MSG_KEY_TORRENT_INFOHASH);
					subscribeClient(msg.replyTo, torrentInfoHash);
					break;

				case MSG_UNSUBSCRIBE_CLIENT:
					torrentInfoHash = bundleMsg.getString(MSG_KEY_TORRENT_INFOHASH);
					unSubscribeClient(msg.replyTo, torrentInfoHash);
					break;

				case MSG_TORRENT_OPEN:
					String filepath = bundleMsg.getString(MSG_KEY_FILEPATH);
					openTorrent(filepath);
					break;

				case MSG_TORRENT_START:
					torrentInfoHash = bundleMsg.getString(MSG_KEY_TORRENT_INFOHASH);
					startTorrent(torrentInfoHash);
					break;

				case MSG_TORRENT_STOP:
					torrentInfoHash = bundleMsg.getString(MSG_KEY_TORRENT_INFOHASH);
					stopTorrent(torrentInfoHash);
					break;

				case MSG_TORRENT_CLOSE:
					torrentInfoHash = bundleMsg.getString(MSG_KEY_TORRENT_INFOHASH);
					closeTorrent(torrentInfoHash);
					break;

				case MSG_GET_TORRENT_LIST:
					sendTorrentList(msg.replyTo);
					break;

				case MSG_GET_TORRENT_ITEM:
					sendTorrentList(msg.replyTo);
					break;
					
				default:
					super.handleMessage(msg);
			}
		}
	}

	/** Subscribes client to the given torrent. */
	private void subscribeClient(Messenger messenger, String torrentInfoHash) {
		if (torrentInfoHash != null) {
			Vector<Messenger> clients = clientsSingle_.get(torrentInfoHash);
			clients.add(messenger);
			Log.v(LOG_TAG, "Client subscried to the following torrent: " + torrentInfoHash);
			sendTorrentItem(messenger, torrentInfoHash);
		} else {
			clientsAll_.add(messenger);
			Log.v(LOG_TAG, "Client subscribed to the Torrent Service.");
			sendTorrentList(messenger);
		}
	}
	
	/** Unsubscribes client from the given torrent. */
	private void unSubscribeClient(Messenger messenger, String torrentInfoHash) {
		if (torrentInfoHash != null) {
			Vector<Messenger> clients = clientsSingle_.get(torrentInfoHash);
			clients.remove(messenger);
			Log.v(LOG_TAG, "Client unsubscribed from the following torrent: " + torrentInfoHash);
		} else {
			clientsAll_.remove(messenger);
			Log.v(LOG_TAG, "Client unsubscribed from the Torrent Service.");
		}
	}

	/** Opens a torrent file with its given file path. */
	private void openTorrent(String filepath) {
		(new OpenTorrentAsyncTask()).execute(filepath);
	}

	/** Starts a torrent. */
	private void startTorrent(String torrentInfoHash) {
		// TODO: implement method
	}
	
	/** Stops a torrent. */
	private void stopTorrent(String torrentInfoHash) {
		// TODO: implement method
	}
	
	/** Closes a torrent. */
	private void closeTorrent(String torrentInfoHash) {
		// TODO: implement method
	}

	/** Sends an optional torrent to the subscribed clients. */
	private void sendTorrentItem(Messenger messenger, String torrentInfoHash) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		
		TorrentListItem item = null;
		for (int i = 0; i < torrents_.size(); i++) {
			if (torrents_.elementAt(i).getInfoHash().equals(torrentInfoHash)) {
				item = new TorrentListItem(torrents_.elementAt(i));
				i = torrents_.size();
			}
		}
		bundle.putSerializable(MSG_KEY_TORRENT_ITEM, item);
		msg.what = MSG_GET_TORRENT_ITEM;
		msg.setData(bundle);

		try {
			messenger.send(msg);
		} catch (RemoteException e) {
			Log.v(LOG_TAG, "Error during sending message.");
		}
	}
	
	/** Sends the actual loaded torrent list to the subscribed clients. */
	private void sendTorrentList(Messenger messenger) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putSerializable(MSG_KEY_TORRENT_LIST, new ArrayList<TorrentListItem>(torrents_));
		msg.what = MSG_GET_TORRENT_LIST;
		msg.setData(bundle);

		try {
			messenger.send(msg);
		} catch (RemoteException e) {
			Log.v(LOG_TAG, "Error during sending message.");
		}
	}

	/** AsyncTask class for open and read a torrent file */
	private class OpenTorrentAsyncTask extends AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(String... filePath) {
			if (filePath[0] == null || filePath[0] == "") return null;
			Torrent newTorrent = torrentManager_.openTorrent(filePath[0]);

			if (newTorrent == null)
				return null;

			TorrentListItem item = new TorrentListItem(newTorrent);
			torrents_.add(item);

			Bundle bundle = new Bundle();
			bundle.putSerializable(MSG_KEY_TORRENT_ITEM, item);
			Message msg = Message.obtain();
			msg.what = DrTorrentActivity.MSG_ADD_TORRENT;
			msg.setData(bundle);

			for (Messenger client : clientsAll_) {
				try {
					if (client != null)
						client.send(msg);
				} catch (Exception e) {
					Log.v(LOG_TAG, "Error during sending message.");
				}
			}
			
			return newTorrent.getInfoHash();
		}
		
		@Override
		protected void onPostExecute(String result) {
			startTorrent(result);
		}
	}

}
