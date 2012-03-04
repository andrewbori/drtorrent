package hu.bute.daai.amorg.drtorrent.service;

import hu.bute.daai.amorg.drtorrent.TorrentListItem;
import hu.bute.daai.amorg.drtorrent.torrentengine.Torrent;
import hu.bute.daai.amorg.drtorrent.torrentengine.TorrentManager;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

/** Torrent service. */
public class TorrentService extends Service {
	private final static String LOG_TAG           = "TorrentService";
	private final static String LOG_ERROR_SENDING = "Error during sending message.";

	public final static int MSG_OPEN_TORRENT        = 1;
	public final static int MSG_START_TORRENT       = 2;
	public final static int MSG_STOP_TORRENT        = 3;
	public final static int MSG_CLOSE_TORRENT       = 4;
	public final static int MSG_SUBSCRIBE_CLIENT    = 5;
	public final static int MSG_UNSUBSCRIBE_CLIENT  = 6;
	public final static int MSG_SEND_TORRENT_ITEM   = 7;
	public final static int MSG_SEND_TORRENT_LIST   = 8;
	public final static int MSG_TORRENT_CHANGED     = 9;
	public final static int MSG_SHOW_TOAST          = 10;
	public final static int MSG_SHOW_DIALOG         = 11;
	public final static int MSG_SHOW_PROGRESS       = 12;
	public final static int MSG_HIDE_PROGRESS       = 13;
	
	public final static String MSG_KEY_FILEPATH         	= "filepath";
	public final static String MSG_KEY_TORRENT_INFOHASH	    = "infohash";
	public final static String MSG_KEY_TORRENT_OLD_INFOHASH = "oldinfohash";
	public final static String MSG_KEY_TORRENT_ITEM     	= "torrentitem";
	public final static String MSG_KEY_TORRENT_LIST     	= "torrentlist";
	public final static String MSG_KEY_MESSAGE 				= "message";
	
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
			String infoHash = null;

			switch (msg.what) {

				case MSG_SUBSCRIBE_CLIENT:
					infoHash = bundleMsg.getString(MSG_KEY_TORRENT_INFOHASH);
					subscribeClient(msg.replyTo, infoHash);
					break;

				case MSG_UNSUBSCRIBE_CLIENT:
					infoHash = bundleMsg.getString(MSG_KEY_TORRENT_INFOHASH);
					unSubscribeClient(msg.replyTo, infoHash);
					break;

				case MSG_OPEN_TORRENT:
					String filepath = bundleMsg.getString(MSG_KEY_FILEPATH);
					openTorrent(filepath);
					break;

				case MSG_START_TORRENT:
					infoHash = bundleMsg.getString(MSG_KEY_TORRENT_INFOHASH);
					startTorrent(infoHash);
					break;

				case MSG_STOP_TORRENT:
					infoHash = bundleMsg.getString(MSG_KEY_TORRENT_INFOHASH);
					stopTorrent(infoHash);
					break;

				case MSG_CLOSE_TORRENT:
					infoHash = bundleMsg.getString(MSG_KEY_TORRENT_INFOHASH);
					closeTorrent(infoHash);
					break;

				case MSG_SEND_TORRENT_LIST:
					sendTorrentList(msg.replyTo);
					break;

				case MSG_SEND_TORRENT_ITEM:
					infoHash = bundleMsg.getString(MSG_KEY_TORRENT_INFOHASH);
					sendTorrentItem(msg.replyTo, infoHash);
					break;
					
				default:
					super.handleMessage(msg);
			}
		}
	}

	/** Subscribes client to the given torrent. */
	private void subscribeClient(Messenger messenger, String infoHash) {
		if (infoHash != null) {
			Vector<Messenger> clients = clientsSingle_.get(infoHash);
			clients.add(messenger);
			Log.v(LOG_TAG, "Client subscried to the following torrent: " + infoHash);
			sendTorrentItem(messenger, infoHash);
		} else {
			clientsAll_.add(messenger);
			Log.v(LOG_TAG, "Client subscribed to the Torrent Service.");
			sendTorrentList(messenger);
		}
	}
	
	/** Unsubscribes client from the given torrent. */
	private void unSubscribeClient(Messenger messenger, String infoHash) {
		if (infoHash != null) {
			Vector<Messenger> clients = clientsSingle_.get(infoHash);
			clients.remove(messenger);
			Log.v(LOG_TAG, "Client unsubscribed from the following torrent: " + infoHash);
		} else {
			clientsAll_.remove(messenger);
			Log.v(LOG_TAG, "Client unsubscribed from the Torrent Service.");
		}
	}

	/** Opens a torrent file with its given file path. */
	private void openTorrent(String filePath) {
		(new OpenTorrentThread(filePath)).start();
	}

	/** Starts a torrent. */
	private void startTorrent(String infoHash) {
		(new StartTorrentThread(infoHash)).start();
	}
	
	/** Stops a torrent. */
	private void stopTorrent(String infoHash) {
		torrentManager_.stopTorrent(infoHash);
	}
	
	/** Closes a torrent. */
	private void closeTorrent(String infoHash) {
		torrentManager_.closeTorrent(infoHash);
	}
	
	/** Torrent changed */
	public void updateTorrentItem(Torrent torrent) {
		String infoHash = torrent.getInfoHash();	

		// Searching the torrent item
		TorrentListItem item = null;
		boolean found = false;
		TorrentListItem tempTorrent;
		for (int i = 0; i < torrents_.size(); i++) {
			tempTorrent = torrents_.elementAt(i);
			if (tempTorrent.getInfoHash().equals(infoHash)) {
				item = torrents_.elementAt(i);
				item.set(torrent);
				i = torrents_.size();
				found = true;
			}
		}

		// If the torrent item could not be found it has to be created. 
		if (!found) {
			item = new TorrentListItem(torrent);
			torrents_.add(item);
			
			Vector<Messenger> messengers = new Vector<Messenger>();
			clientsSingle_.put(torrent.getInfoHash(), messengers);
		}
		
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putSerializable(MSG_KEY_TORRENT_ITEM, item);
		msg.what = MSG_SEND_TORRENT_ITEM;
		msg.setData(bundle);

		sendMessageToAll(msg);
		sendMessageToSingle(msg, infoHash);
	}
	
	/** Shows a toast with a message. */
	public void showToast(String s) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putString(MSG_KEY_MESSAGE, s);
		msg.what = MSG_SHOW_TOAST;
		msg.setData(bundle);
		
		sendMessageToAll(msg);
	}
	
	/** Shows a dialog with a message. */
	public void showDialog(String s) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putString(MSG_KEY_MESSAGE, s);
		msg.what = MSG_SHOW_DIALOG;
		msg.setData(bundle);
		
		sendMessageToAll(msg);
	}
	
	/** Shows a progress dialog with a message. */
	public void showProgress(String s) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putString(MSG_KEY_MESSAGE, s);
		msg.what = MSG_SHOW_PROGRESS;
		msg.setData(bundle);
		
		sendMessageToAll(msg);
	}
	
	/** Hides the progress dialog. */
	public void hideProgress() {
		Message msg = Message.obtain();
		msg.what = MSG_HIDE_PROGRESS;
		
		sendMessageToAll(msg);
	}

	/** Sends an optional torrent to a client. */
	private void sendTorrentItem(Messenger messenger, String infoHash) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		
		TorrentListItem item = null;
		for (int i = 0; i < torrents_.size(); i++) {
			if (torrents_.elementAt(i).getInfoHash().equals(infoHash)) {
				item = new TorrentListItem(torrents_.elementAt(i));
				i = torrents_.size();
			}
		}
		bundle.putSerializable(MSG_KEY_TORRENT_ITEM, item);
		msg.what = MSG_SEND_TORRENT_ITEM;
		msg.setData(bundle);

		try {
			messenger.send(msg);
		} catch (RemoteException e) {
			Log.v(LOG_TAG, LOG_ERROR_SENDING);
		}
	}
	
	/** Sends the actual torrent list to a client. */
	private void sendTorrentList(Messenger messenger) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putSerializable(MSG_KEY_TORRENT_LIST, new ArrayList<TorrentListItem>(torrents_));
		msg.what = MSG_SEND_TORRENT_LIST;
		msg.setData(bundle);

		try {
			messenger.send(msg);
		} catch (RemoteException e) {
			Log.v(LOG_TAG, LOG_ERROR_SENDING);
		}
	}
	
	/** Sending a message to clients subscribed to all torrents. */
	private void sendMessageToAll(Message msg) {
		for (int i=0; i < clientsAll_.size(); i++) {
			try {
				clientsAll_.get(i).send(msg);
			} catch (RemoteException e) {
				Log.v(LOG_TAG, LOG_ERROR_SENDING);
			}
		}
	}
	
	/** Sending a message clients subscribed to a torrent. */
	private void sendMessageToSingle(Message msg, String infoHash) {
		Vector<Messenger> clients = clientsSingle_.get(infoHash);
		for (int i = 0; i < clients.size(); i++) {
			try {
				clients.get(i).send(msg);
			} catch (RemoteException e) {
				Log.v(LOG_TAG, LOG_ERROR_SENDING);
			}
		}
	}
	
	/** Thread class for open and read a torrent file */
	private class OpenTorrentThread extends Thread {

		private String filePath_;
		
		public OpenTorrentThread(String filePath) {
			filePath_ = filePath;
		}
		
		@Override
		public void run() {
			if (filePath_ == null || filePath_ == "") return;
			
			torrentManager_.openTorrent(filePath_);
		}
	}
	
	/** Thread class for open and read a torrent file */
	private class StartTorrentThread extends Thread {

		private String infoHash_;
		
		public StartTorrentThread(String infoHash) {
			infoHash_ = infoHash;
		}
		
		@Override
		public void run() {
			startTorrent(infoHash_);
		}
	}
}
