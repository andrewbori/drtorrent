package hu.bute.daai.amorg.drtorrent.service;

import hu.bute.daai.amorg.drtorrent.Preferences;
import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.activity.DrTorrentActivity;
import hu.bute.daai.amorg.drtorrent.adapter.item.FileListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.PeerListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.TorrentListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.TrackerListItem;
import hu.bute.daai.amorg.drtorrent.torrentengine.File;
import hu.bute.daai.amorg.drtorrent.torrentengine.Peer;
import hu.bute.daai.amorg.drtorrent.torrentengine.Torrent;
import hu.bute.daai.amorg.drtorrent.torrentengine.TorrentManager;
import hu.bute.daai.amorg.drtorrent.torrentengine.Tracker;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
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

	public final static int MSG_OPEN_TORRENT        	 = 101;
	public final static int MSG_START_TORRENT       	 = 102;
	public final static int MSG_STOP_TORRENT        	 = 103;
	public final static int MSG_CLOSE_TORRENT       	 = 104;
	public final static int MSG_SUBSCRIBE_CLIENT    	 = 201;
	public final static int MSG_UNSUBSCRIBE_CLIENT  	 = 202;
	public final static int MSG_SEND_TORRENT_ITEM   	 = 301;
	public final static int MSG_SEND_TORRENT_LIST   	 = 302;
	public final static int MSG_TORRENT_CHANGED     	 = 303;
	public final static int MSG_SHOW_TOAST         	 	 = 304;
	public final static int MSG_SHOW_DIALOG         	 = 305;
	public final static int MSG_SHOW_PROGRESS       	 = 306;
	public final static int MSG_HIDE_PROGRESS       	 = 307;
	public final static int MSG_SEND_PEER_ITEM   		 = 308;
	public final static int MSG_SEND_PEER_LIST   		 = 309;
	public final static int MSG_SEND_BITFIELD			 = 310;
	public final static int MSG_SEND_TRACKER_LIST		 = 311;
	public final static int MSG_SEND_FILE_LIST   		 = 312;
	public final static int MSG_SEND_FILE_LIST_TO_SELECT = 313;
	public final static int MSG_SEND_FILE_LIST_SELECTED  = 314;
	public final static int MSG_SEND_TORRENT_SETTINGS	 = 315;
	public final static int MSG_SHUT_DOWN				 = 321;
	
	public final static String MSG_KEY_FILEPATH         	= "filepath";
	public final static String MSG_KEY_TORRENT_INFOHASH	    = "infohash";
	public final static String MSG_KEY_TORRENT_OLD_INFOHASH = "oldinfohash";
	public final static String MSG_KEY_TORRENT_ITEM     	= "torrentitem";
	public final static String MSG_KEY_TORRENT_LIST     	= "torrentlist";
	public final static String MSG_KEY_IS_REMOVED           = "isremoved";
	public final static String MSG_KEY_PEER_ITEM     		= "peeritem";
	public final static String MSG_KEY_PEER_LIST     		= "peerlist";
	public final static String MSG_KEY_BITFIELD				= "bitfield";
	public final static String MSG_KEY_DOWNLOADING_BITFIELD = "downloadingbitfield";
	public final static String MSG_KEY_FILE_LIST     		= "filelist";
	public final static String MSG_KEY_TRACKER_LIST     	= "trackerlist";
	public final static String MSG_KEY_MESSAGE 				= "message";
	public final static String MSG_KEY_IS_DISCONNECTED		= "isdisconnected";
	
	private Context context_;
	
	private final Messenger serviceMessenger_ = new Messenger(new IncomingMessageHandler());

	private TorrentManager torrentManager_;

	private Messenger clientAll_;
	private Messenger clientSingle_;
	private String clientSingleInfoHash_;

	private Vector<TorrentListItem> torrents_; // only for sending message

	@Override
	public void onCreate() {
		super.onCreate();

		context_ = getApplicationContext();
		hasNetworkConnection();
	
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                    	Log.v(LOG_TAG, inetAddress.getHostName() + ": " + inetAddress.getHostAddress());
                    }
                }
            }
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		showNotification();
		
		Preferences.set(context_);
		
		torrentManager_ = new TorrentManager(this);
		schedulerHandler.postDelayed(schedulerRunnable, 1000);
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
		hideNotification();
		
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
					Uri torrentUri = bundleMsg.getParcelable(MSG_KEY_FILEPATH);
					openTorrent(torrentUri);
					break;

				case MSG_SEND_TORRENT_SETTINGS:
					TorrentListItem torrent = (TorrentListItem) bundleMsg.getSerializable(TorrentService.MSG_KEY_TORRENT_ITEM);
					@SuppressWarnings("unchecked")
					ArrayList<FileListItem> fileList = ((ArrayList<FileListItem>) bundleMsg.getSerializable(TorrentService.MSG_KEY_FILE_LIST));
					(new SetTorrentThread(torrent, fileList)).start();
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
					sendPeerList(msg.replyTo, infoHash);
					break;
					
				case MSG_SHUT_DOWN:
					shutDown();
					break;
					
				default:
					super.handleMessage(msg);
			}
		}
	}

	/** Subscribes client to the given torrent. */
	private void subscribeClient(Messenger messenger, String infoHash) {
		if (infoHash != null) {
			//Log.v(LOG_TAG, clients.size() + " Client subscried to the following torrent: " + infoHash);
			Log.v(LOG_TAG, " Client subscried to the following torrent: " + infoHash);
			clientAll_ = null;
			clientSingleInfoHash_ = infoHash;
			clientSingle_ = messenger;
			sendTorrentItem(messenger, infoHash);
			sendPeerList(messenger, infoHash);
			sendTrackerList(messenger, infoHash);
			sendFileList(messenger, infoHash);
			updateBitfield(torrentManager_.getTorrent(infoHash));
		} else {
			//Log.v(LOG_TAG, clientsAll_.size() + " Client subscribed to the Torrent Service.");
			Log.v(LOG_TAG, " Client subscribed to the Torrent Service.");
			clientSingle_ = null;
			clientSingleInfoHash_ = "";
			clientAll_ = messenger;
			sendTorrentList(messenger);
		}
	}
	
	/** Unsubscribes client from the given torrent. */
	private void unSubscribeClient(Messenger messenger, String infoHash) {
		if (infoHash != null) {
			Log.v(LOG_TAG, "Client unsubscribed from the following torrent: " + infoHash);
			clientSingle_ = null;
			clientSingleInfoHash_ = "";
			
		} else {
			clientAll_ = null;
			Log.v(LOG_TAG, "Client unsubscribed from the Torrent Service.");
		}
	}
	
	static private int NOTIFICATION_TORRENT_SERVICE = 1;
	/** Show Notification. */
	private void showNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		int icon = R.drawable.icon_notification;
		String text = "DrTorrent";
		long when = System.currentTimeMillis();
		
		Notification notification = new Notification(icon, text, when);
		
		Context context = getApplicationContext();
		Intent intent = new Intent(context, DrTorrentActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
		
		notification.setLatestEventInfo(context, text, "DrTorrent is running...", pendingIntent);
		notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
		notificationManager.notify(NOTIFICATION_TORRENT_SERVICE, notification);
	}
	
	/** Hides the service notification. */
	private void hideNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(NOTIFICATION_TORRENT_SERVICE);
	}
	
	/** Shuts down the service. */
	private void shutDown() {
		torrentManager_.shutDown();
		stopSelf();
	}

	/** Opens a torrent file with its given file path. */
	private void openTorrent(Uri torrentUri) {
		(new OpenTorrentThread(torrentUri)).start();
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
	
	public void showTorrentSettings(Torrent torrent) {
		if (clientAll_ != null) {
			Message msg = Message.obtain();
			Bundle bundle = new Bundle();
			bundle.putSerializable(MSG_KEY_TORRENT_ITEM, new TorrentListItem(torrent));
			
			Vector<File> files = torrent.getFiles();
			ArrayList<FileListItem> fileListItems = new ArrayList<FileListItem>();
			for (int i = 0; i < files.size(); i++) {
				fileListItems.add(new FileListItem(files.elementAt(i)));
			}
			bundle.putSerializable(MSG_KEY_FILE_LIST, fileListItems);
			
			msg.what = MSG_SEND_TORRENT_SETTINGS;
			msg.setData(bundle);

			sendMessage(msg, null);
		}
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
				break;
			}
		}

		// If the torrent item could not be found it has to be created. 
		if (!found) {
			item = new TorrentListItem(torrent);
			torrents_.add(item);
		}
		
		if (clientAll_ == null && (!clientSingleInfoHash_.equals(infoHash) || clientSingle_ == null)) return;
		
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putSerializable(MSG_KEY_TORRENT_ITEM, item);
		msg.what = MSG_SEND_TORRENT_ITEM;
		msg.setData(bundle);

		sendMessage(msg, infoHash);
	}
	
	/** Torrent deleted */
	public void removeTorrentItem(Torrent torrent) {
		String infoHash = torrent.getInfoHash();	

		// Searching the torrent item
		TorrentListItem item = null;
		for (int i = 0; i < torrents_.size(); i++) {
			item = torrents_.elementAt(i);
			if (item.getInfoHash().equals(infoHash)) {
				torrents_.remove(i);
				break;
			}
		}
		
		if (clientAll_ == null && (!clientSingleInfoHash_.equals(infoHash) || clientSingle_ == null)) return;
		
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putSerializable(MSG_KEY_TORRENT_ITEM, item);
		bundle.putBoolean(MSG_KEY_IS_REMOVED, true);
		msg.what = MSG_SEND_TORRENT_ITEM;
		msg.setData(bundle);
		sendMessage(msg, null);
	}
	
	/** Peer changed. */
	public void updatePeerItem(Torrent torrent, Peer peer, boolean isDisconnected) {
		if (!clientSingleInfoHash_.equals(torrent.getInfoHash()) || clientSingle_ == null) return;
		
		PeerListItem peerListItem = new PeerListItem(peer);
		
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putSerializable(MSG_KEY_PEER_ITEM, peerListItem);
		bundle.putBoolean(MSG_KEY_IS_DISCONNECTED, isDisconnected);
		msg.what = MSG_SEND_PEER_ITEM;
		msg.setData(bundle);

		sendMessage(msg, torrent.getInfoHash());
	}
	
	/** Peer list changed. */
	public void updatePeerList(Torrent torrent) {
		if (!clientSingleInfoHash_.equals(torrent.getInfoHash()) || clientSingle_ == null) return;
		
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		
		Vector<Peer> peers = torrent.getConnectedPeers();
		ArrayList<PeerListItem> peerListItems = new ArrayList<PeerListItem>();
		for (int i = 0; i < peers.size(); i++) {
			peerListItems.add(new PeerListItem(peers.elementAt(i)));
		}
		bundle.putSerializable(MSG_KEY_PEER_LIST, peerListItems);
		
		msg.what = MSG_SEND_PEER_LIST;
		msg.setData(bundle);

		sendMessage(msg, torrent.getInfoHash());
	}
	
	/** Bitfield changed. */
	public void updateBitfield(Torrent torrent) {
		if (!clientSingleInfoHash_.equals(torrent.getInfoHash()) || clientSingle_ == null) return;
		
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		
		bundle.putSerializable(MSG_KEY_BITFIELD, torrent.getBitfield());
		bundle.putSerializable(MSG_KEY_DOWNLOADING_BITFIELD, torrent.getDownloadingBitfield());
		
		msg.what = MSG_SEND_BITFIELD;
		msg.setData(bundle);

		sendMessage(msg, torrent.getInfoHash());
	}
	
	/** Shows a toast with a message. */
	public void showToast(String s) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putString(MSG_KEY_MESSAGE, s);
		msg.what = MSG_SHOW_TOAST;
		msg.setData(bundle);
		
		sendMessage(msg, null);
	}
	
	/** Shows a dialog with a message. */
	public void showDialog(String s) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putString(MSG_KEY_MESSAGE, s);
		msg.what = MSG_SHOW_DIALOG;
		msg.setData(bundle);
		
		sendMessage(msg, null);
	}
	
	/** Shows a progress dialog with a message. */
	public void showProgress(String s) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putString(MSG_KEY_MESSAGE, s);
		msg.what = MSG_SHOW_PROGRESS;
		msg.setData(bundle);
		
		sendMessage(msg, null);
	}
	
	/** Hides the progress dialog. */
	public void hideProgress() {
		Message msg = Message.obtain();
		msg.what = MSG_HIDE_PROGRESS;
		
		sendMessage(msg, null);
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
	
	/** Sends an optional torrent's peer list to a client. */
	private void sendPeerList(Messenger messenger, String infoHash) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		
		Torrent torrent = torrentManager_.getTorrent(infoHash);
		Vector<Peer> peers = torrent.getConnectedPeers();
		ArrayList<PeerListItem> peerListItems = new ArrayList<PeerListItem>();
		for (int i = 0; i < peers.size(); i++) {
			peerListItems.add(new PeerListItem(peers.elementAt(i)));
		}
		bundle.putSerializable(MSG_KEY_PEER_LIST, peerListItems);
		
		msg.what = MSG_SEND_PEER_LIST;
		msg.setData(bundle);

		try {
			messenger.send(msg);
		} catch (RemoteException e) {
			Log.v(LOG_TAG, LOG_ERROR_SENDING);
		}
	}
	
	/** Sends an optional torrent's file list to a client. */
	private void sendFileList(Messenger messenger, String infoHash) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		
		Torrent torrent = torrentManager_.getTorrent(infoHash);
		Vector<File> files = torrent.getFiles();
		ArrayList<FileListItem> fileListItems = new ArrayList<FileListItem>();
		for (int i = 0; i < files.size(); i++) {
			fileListItems.add(new FileListItem(files.elementAt(i)));
		}
		bundle.putSerializable(MSG_KEY_FILE_LIST, fileListItems);
		
		msg.what = MSG_SEND_FILE_LIST;
		msg.setData(bundle);

		try {
			messenger.send(msg);
		} catch (RemoteException e) {
			Log.v(LOG_TAG, LOG_ERROR_SENDING);
		}
	}
	
	/** Sends an optional torrent's tracker list to a client. */
	private void sendTrackerList(Messenger messenger, String infoHash) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		
		Torrent torrent = torrentManager_.getTorrent(infoHash);
		Vector<Tracker> trackers = torrent.getTrackers();
		ArrayList<TrackerListItem> trackerListItems = new ArrayList<TrackerListItem>();
		for (int i = 0; i < trackers.size(); i++) {
			trackerListItems.add(new TrackerListItem(trackers.elementAt(i)));
		}
		bundle.putSerializable(MSG_KEY_TRACKER_LIST, trackerListItems);
		
		msg.what = MSG_SEND_TRACKER_LIST;
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
	
	// Sends a message to the subscribed client
	private void sendMessage(Message msg, String infoHash) {
		Messenger messenger = null;
		if (clientAll_ != null) {
			messenger = clientAll_;
		}
		if (clientSingle_ != null && clientSingleInfoHash_.equals(infoHash)) {
			messenger = clientSingle_;
		}
		if (messenger != null) {
			try {
				messenger.send(msg);
			} catch (RemoteException e) {
				Log.v(LOG_TAG, LOG_ERROR_SENDING);
			}
		}
	}
	
	/** Thread class for open and read a torrent file */
	private class OpenTorrentThread extends Thread {

		private Uri torrentUri_;
		
		public OpenTorrentThread(Uri torrentUri) {
			torrentUri_ = torrentUri;
		}
		
		@Override
		public void run() {
			if (torrentUri_ == null) return;
			
			torrentManager_.openTorrent(torrentUri_);
		}
	}
	
	/** Thread class for set the settings of the torrent (download path, files to download etc.). */
	private class SetTorrentThread extends Thread {

		private TorrentListItem item_;
		private ArrayList<FileListItem> fileList_;
		
		public SetTorrentThread(TorrentListItem item, ArrayList<FileListItem> fileList) {
			item_ = item;
			fileList_ = fileList;
		}
		
		@Override
		public void run() {
			torrentManager_.setTorrentSettings(item_, fileList_);
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
			torrentManager_.startTorrent(infoHash_);
		}
	}
	
	
	private Handler schedulerHandler = new Handler();
	private Runnable schedulerRunnable = new Runnable() {
		
		@Override
		public void run() {
			torrentManager_.update();
			schedulerHandler.postDelayed(schedulerRunnable, 1000);
		}
	};
	
	public boolean hasNetworkConnection() {
		boolean haveConnectedWifi = false;
		boolean haveConnectedMobile = false;

		ConnectivityManager cm = (ConnectivityManager) context_.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] netInfo = cm.getAllNetworkInfo();
		for (NetworkInfo ni : netInfo) {
			if (ni.getTypeName().equalsIgnoreCase("WIFI")) {
				if (ni.isConnected()) {
					haveConnectedWifi = true;
					Log.v("WIFI CONNECTION ", "AVAILABLE");
				} else {
					Log.v("WIFI CONNECTION ", "NOT AVAILABLE");
				}
			}
			if (ni.getTypeName().equalsIgnoreCase("MOBILE")) {
				if (ni.isConnected()) {
					haveConnectedMobile = true;
					Log.v("MOBILE INTERNET CONNECTION ", "AVAILABLE");
				} else {
					Log.v("MOBILE INTERNET CONNECTION ", "NOT AVAILABLE");
				}
			}
		}
		return haveConnectedWifi || haveConnectedMobile;
	}

	
}
