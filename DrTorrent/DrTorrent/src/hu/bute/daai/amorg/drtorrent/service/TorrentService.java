package hu.bute.daai.amorg.drtorrent.service;

import hu.bute.daai.amorg.drtorrent.Preferences;
import hu.bute.daai.amorg.drtorrent.Quantity;
import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.activity.DrTorrentActivity;
import hu.bute.daai.amorg.drtorrent.adapter.item.FileListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.PeerListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.TorrentListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.TrackerListItem;
import hu.bute.daai.amorg.drtorrent.network.NetworkManager;
import hu.bute.daai.amorg.drtorrent.torrentengine.File;
import hu.bute.daai.amorg.drtorrent.torrentengine.Peer;
import hu.bute.daai.amorg.drtorrent.torrentengine.Torrent;
import hu.bute.daai.amorg.drtorrent.torrentengine.TorrentManager;
import hu.bute.daai.amorg.drtorrent.torrentengine.Tracker;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Vector;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/** Torrent service. */
public class TorrentService extends Service {
	private final static String LOG_TAG           = "TorrentService";
	private final static String LOG_ERROR_SENDING = "Error during sending message.";
	private final static String STATE_FILE		  = "state.json";
	
	public final static String APP_VERSION_NAME = "1.2"; // TODO: Refresh
	
	public final static int MSG_OPEN_TORRENT        	 = 101;
	public final static int MSG_START_TORRENT       	 = 102;
	public final static int MSG_STOP_TORRENT        	 = 103;
	public final static int MSG_CLOSE_TORRENT       	 = 104;
	public final static int MSG_ADD_TRACKER 			 = 105;
	public final static int MSG_REMOVE_TACKER 			 = 106;
	public final static int MSG_ADD_PEER 				 = 107;
	public final static int MSG_REMOVE_PEER 			 = 108;
	public final static int MSG_SUBSCRIBE_CLIENT    	 = 201;
	public final static int MSG_UNSUBSCRIBE_CLIENT  	 = 202;
	public final static int MSG_UPDATE_TORRENT			 = 203;
	public final static int MSG_CHANGE_FILE_PRIORITY	 = 204;
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
	public final static int MSG_TORRENT_ALREADY_OPENED   = 316;
	public final static int MSG_SHUT_DOWN				 = 321;
	public static final int MSG_CONN_SETTINGS_CHANGED    = 400;
	
	public final static int UPDATE_INFO		    =  1;	// 0b00000001
	public final static int UPDATE_FILE_LIST    =  2;	// 0b00000010
	public final static int UPDATE_BITFIELD     =  4;	// 0b00000100
	public final static int UPDATE_PEER_LIST    =  8;	// 0b00001000
	public final static int UPDATE_TRACKER_LIST = 16;	// 0b00010000
	public final static int UPDATE_SOMETHING 	= 31;	// 0b00011111
	
	public final static String MSG_KEY_FILEPATH         	= "a";
	public final static String MSG_KEY_TORRENT_INFOHASH		= "b";
	public final static String MSG_KEY_TORRENT_ID		    = "c";
	public final static String MSG_KEY_TORRENT_UPDATE 		= "d";
	public final static String MSG_KEY_TORRENT_ITEM     	= "e";
	public final static String MSG_KEY_TORRENT_LIST     	= "f";
	public final static String MSG_KEY_IS_REMOVED           = "g";
	public final static String MSG_KEY_PEER_ITEM     		= "h";
	public final static String MSG_KEY_PEER_LIST     		= "i";
	public final static String MSG_KEY_BITFIELD				= "j";
	public final static String MSG_KEY_DOWNLOADING_BITFIELD = "k";
	public final static String MSG_KEY_FILE_ITEM			= "l";
	public final static String MSG_KEY_FILE_LIST     		= "m";
	public final static String MSG_KEY_TRACKER_LIST     	= "n";
	public final static String MSG_KEY_MESSAGE 				= "o";
	public final static String MSG_KEY_IS_DISCONNECTED		= "p";
	public final static String MSG_KEY_TRACKER_URL			= "q";
	public final static String MSG_KEY_TRACKER_ID			= "r";
	public final static String MSG_KEY_PEER_IP				= "s";
	public final static String MSG_KEY_PEER_PORT			= "t";
	public final static String MSG_KEY_IS_SETTINGS			= "u";
	
	private Context context_;
	
	private static int latestVersion_ = 0;  
	
	private final Messenger serviceMessenger_ = new Messenger(new IncomingMessageHandler(this));

	private TorrentManager torrentManager_;
	private NetworkManager networkManager_;
	private NetworkStateReceiver networkStateReceiver_;

	private Messenger clientAll_;
	private Messenger clientSingle_;
	private boolean isSettingsClient_ = false;
	private int clientSingleTorrentId_ = -1;
	private int clientSingleUpdateField_ = 0;

	private Vector<TorrentListItem> torrents_; // only for sending message

	@Override
	public void onCreate() {
		super.onCreate();

		context_ = getApplicationContext();
		Preferences.set(context_);
		showNotification(null);
		
		latestVersion_ = getVersionCode();
		
		torrents_ = new Vector<TorrentListItem>();
		networkManager_ = new NetworkManager();
		torrentManager_ = new TorrentManager(this, networkManager_);
		schedulerHandler.postDelayed(schedulerRunnable, 1000);
		networkStateReceiver_ = new NetworkStateReceiver();
		networkStateReceiver_.setOnNetworkStateListener(networkManager_);
		registerReceiver(networkStateReceiver_, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return serviceMessenger_.getBinder();
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(networkStateReceiver_);
		hideNotification();
		
		super.onDestroy();
	}
	
	/** Returns the version code of the application. (for example: 3) */
	public int getVersionCode() {
		/*try {
			PackageManager manager = getPackageManager();
			PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
			return info.versionCode;
		} catch (Exception e) {
		}
		return 0;*/
		return 3; // TODO: Refresh!!
	}

	/** Handler of incoming Messages from clients. */
	static class IncomingMessageHandler extends Handler {
		private final WeakReference<TorrentService> service_; 

		IncomingMessageHandler(TorrentService service) {
			service_ = new WeakReference<TorrentService>(service);
	    }
		
		@Override
		public void handleMessage(Message msg) {
			TorrentService service = service_.get();
			if (service != null) {
				service.handleMessage(msg);
			}
		}
	}
	
	/** Handling the incoming message. */
	public void handleMessage(Message msg) {
		Bundle bundleMsg = msg.getData();
		int torrentId = -1;

		switch (msg.what) {

			case MSG_SUBSCRIBE_CLIENT:
				torrentId = bundleMsg.getInt(MSG_KEY_TORRENT_ID, -1);
				isSettingsClient_ = bundleMsg.getBoolean(MSG_KEY_IS_SETTINGS, false);
				subscribeClient(msg.replyTo, torrentId);
				break;

			case MSG_UNSUBSCRIBE_CLIENT:
				torrentId = bundleMsg.getInt(MSG_KEY_TORRENT_ID, -1);
				unSubscribeClient(msg.replyTo, torrentId);
				break;
				
			case MSG_UPDATE_TORRENT:
				clientSingleUpdateField_ = bundleMsg.getInt(MSG_KEY_TORRENT_UPDATE, 0);
				break;
				
			case MSG_CHANGE_FILE_PRIORITY:
				torrentId = bundleMsg.getInt(MSG_KEY_TORRENT_ID);
				final FileListItem item = (FileListItem) bundleMsg.getSerializable(TorrentService.MSG_KEY_FILE_ITEM);
				(new ChangeFilePriorityThread(torrentId, item)).start();
				break;

			case MSG_OPEN_TORRENT:
				Uri torrentUri = bundleMsg.getParcelable(MSG_KEY_FILEPATH);
				(new OpenTorrentThread(torrentUri)).start();
				break;

			case MSG_SEND_TORRENT_SETTINGS:
				TorrentListItem torrent = (TorrentListItem) bundleMsg.getSerializable(TorrentService.MSG_KEY_TORRENT_ITEM);
				@SuppressWarnings("unchecked")
				final ArrayList<FileListItem> fileList = ((ArrayList<FileListItem>) bundleMsg.getSerializable(TorrentService.MSG_KEY_FILE_LIST));
				final boolean isRemoved = bundleMsg.getBoolean(TorrentService.MSG_KEY_IS_REMOVED, true);
				(new SetTorrentThread(torrent, isRemoved, fileList)).start();
				break;
				
			case MSG_START_TORRENT:
				torrentId = bundleMsg.getInt(MSG_KEY_TORRENT_ID);
				(new StartTorrentThread(torrentId)).start();
				break;

			case MSG_STOP_TORRENT:
				torrentId = bundleMsg.getInt(MSG_KEY_TORRENT_ID);
				torrentManager_.stopTorrent(torrentId);
				break;

			case MSG_CLOSE_TORRENT:
				torrentId = bundleMsg.getInt(MSG_KEY_TORRENT_ID);
				torrentManager_.closeTorrent(torrentId);
				break;
			
			case MSG_ADD_PEER:
				torrentId = bundleMsg.getInt(MSG_KEY_TORRENT_ID);
				String ip = bundleMsg.getString(MSG_KEY_PEER_IP);
				int port = bundleMsg.getInt(MSG_KEY_PEER_PORT);
				torrentManager_.addPeer(torrentId, ip, port);
				break;
				
			case MSG_ADD_TRACKER:
				torrentId = bundleMsg.getInt(MSG_KEY_TORRENT_ID);
				String trackerUrl = bundleMsg.getString(MSG_KEY_TRACKER_URL);
				torrentManager_.addTracker(torrentId, trackerUrl);
				break;
				
			case MSG_REMOVE_TACKER:
				torrentId = bundleMsg.getInt(MSG_KEY_TORRENT_ID);
				int trackerId = bundleMsg.getInt(MSG_KEY_TRACKER_ID);
				torrentManager_.removeTracker(torrentId, trackerId);
				break;
				
			case MSG_SEND_TORRENT_LIST:
				sendTorrentList(msg.replyTo);
				break;
				
			case MSG_SEND_TORRENT_ITEM:
				torrentId = bundleMsg.getInt(MSG_KEY_TORRENT_ID);
				sendTorrentItem(msg.replyTo, torrentId);
				break;
				
			case MSG_CONN_SETTINGS_CHANGED:
				networkManager_.connectionSettingsChanged();
				break;
				
			case MSG_TORRENT_ALREADY_OPENED:
				final String infoHash = bundleMsg.getString(TorrentService.MSG_KEY_TORRENT_INFOHASH);
				final ArrayList<String> trackerUrls = bundleMsg.getStringArrayList(TorrentService.MSG_KEY_TRACKER_LIST);
				torrentManager_.addTrackers(infoHash, trackerUrls);
				break;	
				
			case MSG_SHUT_DOWN:
				shutDown();
				break;
				
			default:
		}
	}
	

	/** Subscribes client to the given torrent. */
	private void subscribeClient(Messenger messenger, int torrentId) {
		if (torrentId != -1) {
			Log.v(LOG_TAG, "Client subscried to the following torrent: " + torrentId);
			clientAll_ = null;
			clientSingleTorrentId_ = torrentId;
			clientSingle_ = messenger;
			if (!isSettingsClient_) {
				sendTorrentItem(messenger, torrentId);
				sendPeerList(messenger, torrentId);
				sendTrackerList(messenger, torrentId);
				sendFileList(messenger, torrentId);
				updateBitfield(torrentManager_.getTorrent(torrentId));
			} else {
				Torrent torrent = torrentManager_.getOpeningTorrent(torrentId);
				if (torrent != null && torrent.isValid()) {
					updateMetadata(torrent);
				}
			}
		} else {
			Log.v(LOG_TAG, "Client subscribed to the Torrent Service.");
			clientSingle_ = null;
			clientSingleTorrentId_ = -1;
			isSettingsClient_ = false;
			clientAll_ = messenger;
			sendTorrentList(messenger);
		}
	}
	
	/** Unsubscribes client from the given torrent. */
	private void unSubscribeClient(Messenger messenger, int torrentId) {
		if (torrentId != -1) {
			Log.v(LOG_TAG, "Client unsubscribed from the following torrent: " + torrentId);
			clientSingle_ = null;
			clientSingleTorrentId_ = -1;
			clientSingleUpdateField_ = 0;
			
		} else {
			clientAll_ = null;
			Log.v(LOG_TAG, "Client unsubscribed from the Torrent Service.");
		}
	}
	
	/** Show Notification. */
	public void showNotification(long downloadSpeed, long uploadSpeed) {
		Quantity downSpeed = new Quantity(downloadSpeed, Quantity.SPEED);
		Quantity upSpeed = new Quantity(uploadSpeed, Quantity.SPEED);
		String message = context_.getString(R.string.arrow_down) + downSpeed.toString(context_) + " " +
						 context_.getString(R.string.arrow_up) + upSpeed.toString(context_);
		showNotification(message);
	}
	
	/** Show Notification. */
	public void showNotification(int resId) {
		showNotification(context_.getString(resId));
	}
	
	final static private int NOTIFICATION_TORRENT_SERVICE = 1;
	/** Show Notification. */
	private void showNotification(String message) {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		int icon = R.drawable.ic_notification;
		String text = "DrTorrent";
		if (message != null) {
			text = text + " (" + message + ")"; 
		}
		
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context_)
			.setContentTitle(text)
		    .setContentText(getString(R.string.drtorrent_is_running))
		    .setSmallIcon(icon);

		Context context = getApplicationContext();
		Intent intent = new Intent(context, DrTorrentActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
		
		builder.setContentIntent(pendingIntent);
		Notification notification = builder.getNotification();
		notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT | Notification.FLAG_ONLY_ALERT_ONCE;
		
		notificationManager.notify(NOTIFICATION_TORRENT_SERVICE, notification);
	}
	
	static private int notifiacionId = 10; 
	/** Shows a new notificaion for download completed. */
	public void showCompletedNotification(String torrentName) {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		int icon = R.drawable.ic_notification;
		String text = torrentName;
		long when = System.currentTimeMillis();
		
		Notification notification = new Notification(icon, text, when);
		
		Context context = getApplicationContext();
		Intent intent = new Intent(context, DrTorrentActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
		
		notification.ledARGB = 0xff00ff00;
		notification.ledOnMS = 300;
		notification.ledOffMS = 1000;
		notification.flags |= Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS;
		notification.setLatestEventInfo(context, text, getString(R.string.download_complete), pendingIntent);
		notificationManager.notify(notifiacionId++, notification);
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
	
	/** Saves the state of the manager. */
	public void saveState(String state)  {
		try {
			FileOutputStream fos = openFileOutput(STATE_FILE, Context.MODE_PRIVATE);
			fos.write(state.getBytes());
			fos.close();
		} catch (Exception e) {}
	}
	
	/** Loads the saved state of the manager. */
	public String loadState() {
		try {
			FileInputStream fis = openFileInput(STATE_FILE);
			StringBuffer buffer = new StringBuffer();
			int ch;
			while ((ch = fis.read()) != -1) {
				buffer.append((char)ch);
			}
			return buffer.toString();
		} catch (Exception e) {
			Log.v(LOG_TAG, e.getMessage());
			return null;
		}
	}
	
	/** Saves the content of a torrent file. */
	public void saveTorrentContent(String infoHash, byte[] content) {
		try {
			FileOutputStream fos = openFileOutput(infoHash + ".torrent", Context.MODE_PRIVATE);
			fos.write(content);
			fos.close();
		} catch (Exception e) {}
	}
	
	/** Removes the torrent file. */
	public void removeTorrentContent(String infoHash) {
		try {
			context_.deleteFile(infoHash + ".torrent");
		} catch (Exception e) {}
	}
	
	/** Loads the content of a torrent file. */
	public byte[] loadTorrentContent(String infoHash) {
		FileInputStream fis = null;
		ByteArrayOutputStream baos = null;
		try {
			fis = openFileInput(infoHash + ".torrent");
			baos = new ByteArrayOutputStream();
			int ch;
			while ((ch = fis.read()) != -1) {
				baos.write(ch);
			}
			return baos.toByteArray();
		} catch (Exception e) {
			Log.v(LOG_TAG, "Error while loading saved torrent: " + infoHash);
			return null;
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (Exception e) {}
			}
			if (baos != null) {
				try {
					baos.close();
				} catch (Exception e) {}
			}
		}
	}

	/** Shows the torrent's settings dialog. */
	public void showTorrentSettings(Torrent torrent) {
		if (clientAll_ != null) {
			Message msg = Message.obtain();
			Bundle bundle = new Bundle();
			bundle.putSerializable(MSG_KEY_TORRENT_ITEM, new TorrentListItem(torrent));
			
			final Vector<File> files = torrent.getFiles();
			final ArrayList<FileListItem> fileListItems = new ArrayList<FileListItem>();
			for (int i = 0; i < files.size(); i++) {
				fileListItems.add(new FileListItem(files.elementAt(i)));
			}
			bundle.putSerializable(MSG_KEY_FILE_LIST, fileListItems);
			
			msg.what = MSG_SEND_TORRENT_SETTINGS;
			msg.setData(bundle);

			sendMessage(msg, -1);
		}
	}
	
	/** Torrent changed. */
	public void updateTorrentItem(Torrent torrent) {
		if (torrent == null) return;
		int torrentId = torrent.getId();
		
		// Searching the torrent item
		TorrentListItem item = null;
		boolean found = false;
		TorrentListItem tempTorrent;
		for (int i = 0; i < torrents_.size(); i++) {
			tempTorrent = torrents_.elementAt(i);
			if (tempTorrent.getId() == torrentId) {
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
		
		if (clientAll_ == null && (clientSingleTorrentId_ != torrentId || clientSingle_ == null)) return;
		
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putSerializable(MSG_KEY_TORRENT_ITEM, item);
		msg.what = MSG_SEND_TORRENT_ITEM;
		msg.setData(bundle);

		sendMessage(msg, torrentId);
	}
	
	/** Torrent deleted. */
	public void removeTorrentItem(Torrent torrent) {
		if (torrent == null) return;
		int torrentId = torrent.getId();	

		// Searching the torrent item
		TorrentListItem item = null;
		for (int i = 0; i < torrents_.size(); i++) {
			item = torrents_.elementAt(i);
			if (item.getId() == torrentId) {
				torrents_.remove(i);
				break;
			}
		}
		
		if (clientAll_ == null && (clientSingleTorrentId_ != torrentId || clientSingle_ == null)) return;
		
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putSerializable(MSG_KEY_TORRENT_ITEM, item);
		bundle.putBoolean(MSG_KEY_IS_REMOVED, true);
		msg.what = MSG_SEND_TORRENT_ITEM;
		msg.setData(bundle);
		sendMessage(msg, -1);
	}
	
	/** FileList changed. */
	public void updateFileList(Torrent torrent) {
		if (torrent == null) return;
		if (clientSingleTorrentId_ != torrent.getId() || clientSingle_ == null) return;
		
		final Vector<File> files = torrent.getFiles();
		final ArrayList<FileListItem> fileListItems = new ArrayList<FileListItem>();
		for (int i = 0; i < files.size(); i++) {
			final File file = files.elementAt(i);
			if (file.isChanged()) {
				fileListItems.add(new FileListItem(file));
			}
		}
		if (fileListItems.isEmpty()) return;
		
		Bundle bundle = new Bundle();
		bundle.putSerializable(MSG_KEY_FILE_LIST, fileListItems);
		
		Message msg = Message.obtain();
		msg.what = MSG_SEND_FILE_LIST;
		msg.setData(bundle);

		sendMessage(msg, torrent.getId());
	}
	
	/** Peer list changed. */
	public void updatePeerList(Torrent torrent) {
		if (torrent == null) return;
		if (clientSingleTorrentId_ != torrent.getId() || clientSingle_ == null) return;
		
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		
		final Vector<Peer> peers = torrent.getConnectedPeers();
		final ArrayList<PeerListItem> peerListItems = new ArrayList<PeerListItem>();
		for (int i = 0; i < peers.size(); i++) {
			try {
				peerListItems.add(new PeerListItem(peers.elementAt(i)));
			} catch (Exception e) {
				break;
			}
		}
		bundle.putSerializable(MSG_KEY_PEER_LIST, peerListItems);
		
		msg.what = MSG_SEND_PEER_LIST;
		msg.setData(bundle);

		sendMessage(msg, torrent.getId());
	}
	
	/** Peer list changed. */
	public void updateTrackerList(Torrent torrent) {
		if (torrent == null) return;
		if (clientSingleTorrentId_ != torrent.getId() || clientSingle_ == null) return;
		
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		
		final Vector<Tracker> trackers = torrent.getTrackers();
		final ArrayList<TrackerListItem> trackerListItems = new ArrayList<TrackerListItem>();
		for (int i = 0; i < trackers.size(); i++) {
			trackerListItems.add(new TrackerListItem(trackers.elementAt(i)));
		}
		bundle.putSerializable(MSG_KEY_TRACKER_LIST, trackerListItems);
		
		msg.what = MSG_SEND_TRACKER_LIST;
		msg.setData(bundle);

		sendMessage(msg, torrent.getId());
	}
	
	/** Bitfield changed. */
	public void updateBitfield(Torrent torrent) {
		if (torrent == null) return;
		if (clientSingleTorrentId_ != torrent.getId() || clientSingle_ == null) return;
		
		
		Bundle bundle = new Bundle();
		bundle.putSerializable(MSG_KEY_BITFIELD, torrent.getBitfield());
		bundle.putSerializable(MSG_KEY_DOWNLOADING_BITFIELD, torrent.getDownloadingBitfield());
		
		Message msg = Message.obtain();
		msg.what = MSG_SEND_BITFIELD;
		msg.setData(bundle);

		sendMessage(msg, torrent.getId());
	}
	
	/** Shows a toast with a message. */
	public void showToast(String s) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putString(MSG_KEY_MESSAGE, s);
		msg.what = MSG_SHOW_TOAST;
		msg.setData(bundle);
		
		sendMessage(msg, -1);
	}
	
	/** Shows a dialog with a message. */
	public void showDialog(String s) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putString(MSG_KEY_MESSAGE, s);
		msg.what = MSG_SHOW_DIALOG;
		msg.setData(bundle);
		
		sendMessage(msg, -1);
	}
	
	/** Shows the already opened dialog. */
	public void showAlreadyOpenedDialog(String infoHash, ArrayList<String> trackerUrls) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putString(MSG_KEY_TORRENT_INFOHASH, infoHash);
		bundle.putStringArrayList(MSG_KEY_TRACKER_LIST, trackerUrls);
		msg.what = MSG_TORRENT_ALREADY_OPENED;
		msg.setData(bundle);
		
		sendMessage(msg, -1);
	}
	
	/** Shows a progress dialog with a message. */
	public void showProgress(String s) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putString(MSG_KEY_MESSAGE, s);
		msg.what = MSG_SHOW_PROGRESS;
		msg.setData(bundle);
		
		sendMessage(msg, -1);
	}
	
	/** Hides the progress dialog. */
	public void hideProgress() {
		Message msg = Message.obtain();
		msg.what = MSG_HIDE_PROGRESS;
		
		sendMessage(msg, -1);
	}
	
	/** Updates the metadata of the torrent. (torrent name & file list) */
	public void updateMetadata(Torrent torrent) {
		if (clientSingle_ != null && torrent != null && clientSingleTorrentId_ == torrent.getId() && isSettingsClient_) {	
			TorrentListItem item = new TorrentListItem(torrent); 
			Message msg = Message.obtain();
			Bundle bundle = new Bundle();
			bundle.putSerializable(MSG_KEY_TORRENT_ITEM, item);
			msg.what = MSG_SEND_TORRENT_ITEM;
			msg.setData(bundle);

			sendMessage(msg, torrent.getId());
			
			
			final Vector<File> files = torrent.getFiles();
			final ArrayList<FileListItem> fileListItems = new ArrayList<FileListItem>();
			for (int i = 0; i < files.size(); i++) {
				final File file = files.elementAt(i);
				fileListItems.add(new FileListItem(file));
			}
			
			bundle = new Bundle();
			bundle.putSerializable(MSG_KEY_FILE_LIST, fileListItems);
			
			msg = Message.obtain();
			msg.what = MSG_SEND_FILE_LIST;
			msg.setData(bundle);

			sendMessage(msg, torrent.getId());
		}
	}
	
	/** Returns whether the given field should be updated or not. <br>
	 *  <br>
	 *  MSG_UPDATE_INFO		    =  1;	// 0b00000001 <br>
	 *  MSG_UPDATE_FILE_LIST    =  2;	// 0b00000010 <br>
	 *	MSG_UPDATE_BITFIELD     =  4;	// 0b00000100 <br>
	 *	MSG_UPDATE_PEER_LIST    =  8;	// 0b00001000 <br>
	 *	MSG_UPDATE_TRACKER_LIST = 16;	// 0b00010000 <br>
	 *	MSG_UPDATE_SOMETHING	= 31;	// 0b00011111 <br>
	 * 
	 */
	public boolean shouldUpdate(int updateField) {
		return (updateField == UPDATE_SOMETHING && clientAll_ != null) || ((clientSingleUpdateField_ & updateField) != 0);
	}

	/** Sends a torrent to the subscribed client. */
	private void sendTorrentItem(Messenger messenger, int torrentId) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		
		TorrentListItem item = null;
		for (int i = 0; i < torrents_.size(); i++) {
			if (torrents_.elementAt(i).getId() == torrentId) {
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
	
	/** Sends the peer list to the subscribed client. */
	private void sendPeerList(Messenger messenger, int torrentId) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		
		Torrent torrent = torrentManager_.getTorrent(torrentId);
		if (torrent == null) return;
		final Vector<Peer> peers = torrent.getConnectedPeers();
		final ArrayList<PeerListItem> peerListItems = new ArrayList<PeerListItem>();
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
	
	/** Sends the file list to the subscribed client. */
	private void sendFileList(Messenger messenger, int torrentId) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		
		Torrent torrent = torrentManager_.getTorrent(torrentId);
		if (torrent == null) return;
		final Vector<File> files = torrent.getFiles();
		final ArrayList<FileListItem> fileListItems = new ArrayList<FileListItem>();
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
	
	/** Sends the tracker list to the subscribed client. */
	private void sendTrackerList(Messenger messenger, int torrentId) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		
		Torrent torrent = torrentManager_.getTorrent(torrentId);
		if (torrent == null) return;
		final Vector<Tracker> trackers = torrent.getTrackers();
		final ArrayList<TrackerListItem> trackerListItems = new ArrayList<TrackerListItem>();
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
	
	/** Sends the actual torrent list to the subscribed client. */
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
	
	/** Sends a message to the subscribed client. */
	private void sendMessage(Message msg, int torrentId) {
		Messenger messenger = null;
		if (clientAll_ != null) {
			messenger = clientAll_;
		}
		if (clientSingle_ != null && clientSingleTorrentId_ == torrentId) {
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
	
	/** Thread for opening and reading a torrent file. */
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
	
	/** Thread for setting a torrent (download path, files to download etc.). */
	private class SetTorrentThread extends Thread {

		final private TorrentListItem item_;
		final private boolean isRemoved_;
		final private ArrayList<FileListItem> fileList_;
		
		public SetTorrentThread(final TorrentListItem item, final boolean isRemoved, final ArrayList<FileListItem> fileList) {
			item_ = item;
			isRemoved_ = isRemoved;
			fileList_ = fileList;
		}
		
		@Override
		public void run() {
			torrentManager_.setTorrentSettings(item_, isRemoved_, fileList_);
		}
	}
	
	/** Thread for starting a torrent. */
	private class StartTorrentThread extends Thread {

		final private int torrentId_;
		
		public StartTorrentThread(final int torrentId) {
			torrentId_ = torrentId;
		}
		
		@Override
		public void run() {
			torrentManager_.startTorrent(torrentId_);
		}
	}
	
	/** Thread for changing the priority of a file. */
	private class ChangeFilePriorityThread extends Thread {
		final private int torrentId_;
		final private FileListItem item_;
		
		public ChangeFilePriorityThread(final int torrentId, final FileListItem item) {
			this.torrentId_ = torrentId;
			this.item_ = item;
		}
		
		@Override
		public void run() {
			torrentManager_.changeFilePriority(torrentId_, item_);
		}
	}
	
	final private Handler schedulerHandler = new Handler();
	final private Runnable schedulerRunnable = new Runnable() {
		
		@Override
		public void run() {
			torrentManager_.update();
			schedulerHandler.postDelayed(schedulerRunnable, 1000);
		}
	};
}
