package hu.bute.daai.amorg.drtorrent.ui.service;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.core.File;
import hu.bute.daai.amorg.drtorrent.core.peer.PeerInfo;
import hu.bute.daai.amorg.drtorrent.core.torrent.TorrentInfo;
import hu.bute.daai.amorg.drtorrent.core.torrent.TorrentManager;
import hu.bute.daai.amorg.drtorrent.core.torrent.TorrentManagerObserver;
import hu.bute.daai.amorg.drtorrent.core.tracker.TrackerInfo;
import hu.bute.daai.amorg.drtorrent.file.FileManager;
import hu.bute.daai.amorg.drtorrent.network.NetworkManager;
import hu.bute.daai.amorg.drtorrent.ui.activity.MainActivity;
import hu.bute.daai.amorg.drtorrent.ui.activity.TorrentCreatorActivity;
import hu.bute.daai.amorg.drtorrent.ui.adapter.item.FileListItem;
import hu.bute.daai.amorg.drtorrent.ui.adapter.item.PeerListItem;
import hu.bute.daai.amorg.drtorrent.ui.adapter.item.TorrentListItem;
import hu.bute.daai.amorg.drtorrent.ui.adapter.item.TrackerListItem;
import hu.bute.daai.amorg.drtorrent.util.Log;
import hu.bute.daai.amorg.drtorrent.util.Preferences;
import hu.bute.daai.amorg.drtorrent.util.Quantity;
import hu.bute.daai.amorg.drtorrent.util.analytics.Analytics;
import hu.bute.daai.amorg.drtorrent.util.bencode.Bencoded;
import hu.bute.daai.amorg.drtorrent.util.bencode.BencodedDictionary;
import hu.bute.daai.amorg.drtorrent.util.bencode.BencodedList;
import hu.bute.daai.amorg.drtorrent.util.bencode.BencodedString;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
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

/** Torrent service. */
public class TorrentService extends Service implements TorrentManagerObserver {
	private final static String LOG_TAG           = "TorrentService";
	private final static String LOG_ERROR_SENDING = "Error during sending message.";
	private final static String STATE_FILE		  = "state.json";
	
	public final static int MSG_OPEN_TORRENT        	 = 101;
	public final static int MSG_START_TORRENT       	 = 102;
	public final static int MSG_STOP_TORRENT        	 = 103;
	public final static int MSG_CLOSE_TORRENT       	 = 104;
	public final static int MSG_ADD_TRACKER 			 = 105;
	public final static int MSG_UPDATE_TACKER 			 = 106;
	public final static int MSG_REMOVE_TACKER 			 = 107;
	public final static int MSG_ADD_PEER 				 = 108;
	public final static int MSG_REMOVE_PEER 			 = 109;
	public final static int MSG_CREATE_TORRENT			 = 111;
	public final static int MSG_SUBSCRIBE_CLIENT    	 = 201;
	public final static int MSG_UNSUBSCRIBE_CLIENT  	 = 202;
	public final static int MSG_UPDATE_TORRENT			 = 203;
	public final static int MSG_CHANGE_TORRENT			 = 204;
	public final static int MSG_CHANGE_FILE_PRIORITY	 = 205;
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
	public final static int MSG_SEND_MAGNET_LINK		 = 317;
	public final static int MSG_SEND_TORRENT_FILE		 = 318;
	public final static int MSG_SHUT_DOWN				 = 321;
	public static final int MSG_CONN_SETTINGS_CHANGED    = 400;
	
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
	public final static String MSG_KEY_CLIENT_TYPE			= "u";
	public final static String MSG_KEY_CLIENT_ID			= "v";
	public final static String MSG_KEY_DELETE_FILES			= "w";
	public final static String MSG_KEY_NAME					= "x";
	public final static String MSG_KEY_MAGNET_LINK			= "y";
	public final static String MSG_KEY_PORT_CHANGED			= "n1";
	public final static String MSG_KEY_INCOMING_CHANGED		= "n2";
	public final static String MSG_KEY_WIFIONLY_CHANGED		= "n3";
	public final static String MSG_KEY_UPNP_CHANGED			= "n4";
	
	private Context context_; 
	
	private final Messenger serviceMessenger_ = new Messenger(new IncomingMessageHandler(this));

	private TorrentManager torrentManager_;
	private NetworkManager networkManager_;
	private NetworkStateReceiver networkStateReceiver_;
	private PowerConnectionStateReceiver batteryStateReceiver_;
	
	private TorrentClient client_ = null;

	private Vector<TorrentListItem> torrents_; // only for sending message

	@Override
	public void onCreate() {
		super.onCreate();

		context_ = getApplicationContext();
		Preferences.set(context_);
		Analytics.init(context_);
		showNotification(null);
		
		Preferences.setLatestVersion(Preferences.APP_VERSION_CODE);
		
		torrents_ = new Vector<TorrentListItem>();
		networkManager_ = new NetworkManager();
		torrentManager_ = new TorrentManager(this, networkManager_);
		schedulerHandler.postDelayed(schedulerRunnable, 1000);
		networkStateReceiver_ = new NetworkStateReceiver();
		networkStateReceiver_.setOnNetworkStateListener(networkManager_);
		registerReceiver(networkStateReceiver_, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		
		batteryStateReceiver_ = new PowerConnectionStateReceiver();
		batteryStateReceiver_.setOnPowerConnectionStateListener(networkManager_);
		registerReceiver(batteryStateReceiver_, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		registerReceiver(batteryStateReceiver_, new IntentFilter(Intent.ACTION_POWER_CONNECTED));
		registerReceiver(batteryStateReceiver_, new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
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
		Analytics.shutDown();
		unregisterReceiver(networkStateReceiver_);
		unregisterReceiver(batteryStateReceiver_);
		hideNotification();
		
		super.onDestroy();
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
		int clientType = TorrentClient.CLIENT_TYPE_ALL;
		int torrentId = -1;
		int clientId = 0;

		switch (msg.what) {

			case MSG_SUBSCRIBE_CLIENT:
				clientId = bundleMsg.getInt(MSG_KEY_CLIENT_ID, 0);
				clientType = bundleMsg.getInt(MSG_KEY_CLIENT_TYPE, TorrentClient.CLIENT_TYPE_ALL);
				torrentId = bundleMsg.getInt(MSG_KEY_TORRENT_ID, -1);
				subscribeClient(clientId, msg.replyTo, clientType, torrentId);
				break;

			case MSG_UNSUBSCRIBE_CLIENT:
				clientId = bundleMsg.getInt(MSG_KEY_CLIENT_ID, 0);
				unSubscribeClient(clientId, msg.replyTo);
				break;
				
			case MSG_CHANGE_TORRENT:
				clientId = bundleMsg.getInt(MSG_KEY_CLIENT_ID, 0);
				torrentId = bundleMsg.getInt(MSG_KEY_TORRENT_ID, -1);
				changeTorrent(clientId, msg.replyTo, torrentId);
				break;
				
			case MSG_UPDATE_TORRENT:
				clientId = bundleMsg.getInt(MSG_KEY_CLIENT_ID, 0);
				int updateField = bundleMsg.getInt(MSG_KEY_TORRENT_UPDATE, 0);
				changeUpdateField(clientId, msg.replyTo, updateField);
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
				
			case MSG_CREATE_TORRENT:
				String filePath = bundleMsg.getString(TorrentCreatorActivity.RESULT_KEY_FILEPATH);
				String trackers = bundleMsg.getString(TorrentCreatorActivity.RESULT_KEY_TRACKERS);
				boolean isPrivate = bundleMsg.getBoolean(TorrentCreatorActivity.RESULT_KEY_IS_PRIVATE);
				String filePathSaveAs = bundleMsg.getString(TorrentCreatorActivity.RESULT_KEY_FILEPATH_SAVE_AS);
				boolean shouldStart = bundleMsg.getBoolean(TorrentCreatorActivity.RESULT_KEY_SHOULD_START);
				(new CreateTorrentThread(filePath, trackers, isPrivate, filePathSaveAs, shouldStart)).start();
				break;
				
			case MSG_SEND_MAGNET_LINK:
				torrentId = bundleMsg.getInt(MSG_KEY_TORRENT_ID);
				TorrentInfo t = torrentManager_.getTorrent(torrentId);
				if (t != null) {
					Message rMsg = Message.obtain();
					Bundle bundle = new Bundle();
					bundle.putString(MSG_KEY_NAME, t.getName());
					bundle.putString(MSG_KEY_MAGNET_LINK, t.getMagnetLink());
					
					rMsg.setData(bundle);
					rMsg.what = MSG_SEND_MAGNET_LINK;
					
					try {
						msg.replyTo.send(rMsg);
					} catch (Exception e) {
					}
				}
				
				break;
				
			case MSG_SEND_TORRENT_FILE:
				torrentId = bundleMsg.getInt(MSG_KEY_TORRENT_ID);
				final TorrentInfo fTorrent = torrentManager_.getTorrent(torrentId);
				final Messenger replyTo = msg.replyTo;
				new Thread() {
					public void run() {
						try {
							
							if (fTorrent != null) {
								BencodedDictionary dict = new BencodedDictionary();
								ArrayList<TrackerInfo> trackers = fTorrent.getTrackers();
								
								if (trackers.size() > 0) {
									dict.addEntry("announce", trackers.get(0).getUrl());
									
									BencodedList bTrackerList = new BencodedList();
									for (int i = 0; i < trackers.size(); i++) {
										BencodedList bTracker = new BencodedList();
										bTracker.append(new BencodedString(trackers.get(i).getUrl()));
										bTrackerList.append(bTracker);
									}
									dict.addEntry("announce-list", bTrackerList);
								}
								
								byte[] torrentContent = torrentManager_.readTorrentContent(fTorrent.getInfoHashString());
								Bencoded bencoded = Bencoded.parse(torrentContent);
								torrentContent = null;
								if (bencoded != null && bencoded.type() == Bencoded.BENCODED_DICTIONARY) {
									bencoded = ((BencodedDictionary) bencoded).entryValue("info");
									if (bencoded != null && bencoded.type() == Bencoded.BENCODED_DICTIONARY) {
										dict.addEntry("info", bencoded);
										bencoded = null;
								
										String filePath = Preferences.getExternalCacheDir() + "/share/" + fTorrent.getInfoHashString() + ".torrent";
										FileManager.write(filePath, 0, dict.Bencode());
										dict = null;
										
										
										Message rMsg = Message.obtain();
										Bundle bundle = new Bundle();
										bundle.putString(MSG_KEY_NAME, fTorrent.getName());
										bundle.putString(MSG_KEY_FILEPATH, filePath);
										
										rMsg.setData(bundle);
										rMsg.what = MSG_SEND_TORRENT_FILE;
										
										try {
											replyTo.send(rMsg);
										} catch (Exception e) {
										}
									}
								}
								
							}
						} catch (Exception e) {
							
						}
					}
				}.start();
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
				boolean deleteFiles = bundleMsg.getBoolean(MSG_KEY_DELETE_FILES, false);
				torrentManager_.closeTorrent(torrentId, deleteFiles);
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
				
			case MSG_UPDATE_TACKER:
				torrentId = bundleMsg.getInt(MSG_KEY_TORRENT_ID);
				int trackerId = bundleMsg.getInt(MSG_KEY_TRACKER_ID);
				torrentManager_.updateTracker(torrentId, trackerId);
				break;
				
			case MSG_REMOVE_TACKER:
				torrentId = bundleMsg.getInt(MSG_KEY_TORRENT_ID);
				trackerId = bundleMsg.getInt(MSG_KEY_TRACKER_ID);
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
				boolean isPortChanged = bundleMsg.getBoolean(TorrentService.MSG_KEY_PORT_CHANGED);
				boolean isIncomingChanged = bundleMsg.getBoolean(TorrentService.MSG_KEY_INCOMING_CHANGED);
				boolean isWifiOnlyChanged = bundleMsg.getBoolean(TorrentService.MSG_KEY_WIFIONLY_CHANGED);
				boolean isUpnpChanged = bundleMsg.getBoolean(TorrentService.MSG_KEY_UPNP_CHANGED);
				networkManager_.connectionSettingsChanged(true, isPortChanged, isIncomingChanged, isWifiOnlyChanged, isUpnpChanged);
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
	private void subscribeClient(int clientId, Messenger messenger, int clientType, int torrentId) {
		Log.v(LOG_TAG, "Client subscribed to the Torrent Service.");
		
		synchronized (this) {
			client_ = new TorrentClient(clientId, messenger, clientType, torrentId);
		}
		
		if (client_.isTypeAll()) {
			sendTorrentList(messenger);
		}
		
		if (torrentId != -1) {
			if (client_.isTypeSingle(torrentId)) {
				sendTorrentItem(messenger, torrentId);
				sendPeerList(messenger, torrentId);
				sendTrackerList(messenger, torrentId);
				sendFileList(messenger, torrentId);
				updateBitfield(torrentManager_.getTorrent(torrentId));
			} else {
				TorrentInfo torrent = torrentManager_.getOpeningTorrent(torrentId);
				if (torrent != null && torrent.isValid()) {
					updateMetadata(torrent);
				}
			}
		}
	}
	
	/** Unsubscribes client from the given torrent. */
	private void unSubscribeClient(int clientId, Messenger messenger) {
		Log.v(LOG_TAG, "Client unsubscribed from the Torrent Service.");
		
		synchronized (this) {
			if (client_ != null && client_.hasId(clientId)) {
				client_ = null;
			}
		}
	}
	
	/** Changes the subscription (the id of the torrent it subscribed to). */
	private void changeTorrent(int clientId, Messenger messenger, int torrentId) {
		Log.v(LOG_TAG, "Change torrent: " + torrentId);
		if (client_ != null && client_.hasId(clientId)) {
			Log.v(LOG_TAG, "Change torrent1: " + torrentId);
			client_.setTorrentId(torrentId);
			
			if (torrentId != -1) {
				Log.v(LOG_TAG, "Change torrent2: " + torrentId);
				sendTorrentItem(messenger, torrentId);
				sendPeerList(messenger, torrentId);
				sendTrackerList(messenger, torrentId);
				sendFileList(messenger, torrentId);
				updateBitfield(torrentManager_.getTorrent(torrentId));
			}
		}
	}
	
	private void changeUpdateField(int clientId, Messenger messenger, int updateField) {
		if (client_ != null && client_.hasId(clientId)) {
			client_.setUpdateField(updateField);
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
	
	@Override
	public void showNotification(final int messageId) {
		switch (messageId) {
			case MESSAGE_INACTIVE:
				showNotification(context_.getString(R.string.inactive));
				break;
	
			default:
				break;
		}
	}
	
	final static private int NOTIFICATION_TORRENT_SERVICE = 1;
	/** Show Notification. */
	private void showNotification(String message) {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		String text = "DrTorrent";
		if (message != null) {
			text = text + " (" + message + ")"; 
		}
		
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context_)
			.setContentTitle(text)
		    .setContentText(getString(R.string.drtorrent_is_running))
		    .setSmallIcon(R.drawable.ic_notification);

		Context context = getApplicationContext();
		Intent intent = new Intent(context, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
		
		builder.setContentIntent(pendingIntent);
		Notification notification = builder.build();
		notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT | Notification.FLAG_ONLY_ALERT_ONCE;
		
		notificationManager.notify(NOTIFICATION_TORRENT_SERVICE, notification);
	}
	
	static private int notifiacionId = 10; 
	@Override
	public void showCompletedNotification(String torrentName) {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(context_)
			.setContentTitle(torrentName)
		    .setContentText(getString(R.string.download_complete))
		    .setSmallIcon(R.drawable.ic_notification);

		Context context = getApplicationContext();
		Intent intent = new Intent(context, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
		
		builder.setContentIntent(pendingIntent);
		Notification notification = builder.build();
		notification.ledARGB = 0xff00ff00;
		notification.ledOnMS = 300;
		notification.ledOffMS = 1000;
		notification.flags |= Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS;
		
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
	
	@Override
	public void saveState(String state)  {
		try {
			FileOutputStream fos = openFileOutput(STATE_FILE, Context.MODE_PRIVATE);
			fos.write(state.getBytes());
			fos.close();
		} catch (Exception e) {}
	}
	
	@Override
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
	
	@Override
	public void saveTorrentContent(String infoHash, byte[] content) {
		try {
			FileOutputStream fos = openFileOutput(infoHash + ".torrent", Context.MODE_PRIVATE);
			fos.write(content);
			fos.close();
		} catch (Exception e) {}
	}
	
	@Override
	public void removeTorrentContent(String infoHash) {
		try {
			context_.deleteFile(infoHash + ".torrent");
		} catch (Exception e) {}
	}
	
	@Override
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

	@Override
	public void showTorrentSettings(TorrentInfo torrent) {
		if (client_ != null) {
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
	
	@Override
	public void updateTorrentItem(TorrentInfo torrent) {
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
		
		if (client_ != null && (client_.isTypeAll() || client_.isTypeSingle(torrentId))) {
			Message msg = Message.obtain();
			Bundle bundle = new Bundle();
			bundle.putSerializable(MSG_KEY_TORRENT_ITEM, item);
			msg.what = MSG_SEND_TORRENT_ITEM;
			msg.setData(bundle);
	
			sendMessage(msg, torrentId);
		}
	}
	
	@Override
	public void removeTorrentItem(TorrentInfo torrent) {
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
		
		if (client_ != null && (client_.isTypeAll() || client_.isTypeSingle(torrentId))) {
			Message msg = Message.obtain();
			Bundle bundle = new Bundle();
			bundle.putSerializable(MSG_KEY_TORRENT_ITEM, item);
			bundle.putBoolean(MSG_KEY_IS_REMOVED, true);
			msg.what = MSG_SEND_TORRENT_ITEM;
			msg.setData(bundle);
			sendMessage(msg, -1);
		}
	}
	
	@Override
	public void updateFileList(TorrentInfo torrent) {
		if (torrent != null && client_ != null && client_.isTypeSingle(torrent.getId())) {
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
	}
	
	@Override
	public void updatePeerList(TorrentInfo torrent) {
		if (torrent != null && client_ != null && client_.isTypeSingle(torrent.getId())) {
			Message msg = Message.obtain();
			Bundle bundle = new Bundle();
			
			final PeerInfo[] peers = torrent.getConnectedPeers();
			final ArrayList<PeerListItem> peerListItems = new ArrayList<PeerListItem>();
			for (int i = 0; i < peers.length; i++) {
				try {
					peerListItems.add(new PeerListItem(peers[i]));
				} catch (Exception e) {
					break;
				}
			}
			bundle.putSerializable(MSG_KEY_PEER_LIST, peerListItems);
			
			msg.what = MSG_SEND_PEER_LIST;
			msg.setData(bundle);
	
			sendMessage(msg, torrent.getId());
		}
	}
	
	@Override
	public void updateTrackerList(TorrentInfo torrent) {
		if (torrent != null && client_ != null && client_.isTypeSingle(torrent.getId())) {
			Message msg = Message.obtain();
			Bundle bundle = new Bundle();
			
			final ArrayList<TrackerInfo> trackers = torrent.getTrackers();
			final ArrayList<TrackerListItem> trackerListItems = new ArrayList<TrackerListItem>();
			for (int i = 0; i < trackers.size(); i++) {
				trackerListItems.add(new TrackerListItem(trackers.get(i)));
			}
			bundle.putSerializable(MSG_KEY_TRACKER_LIST, trackerListItems);
			
			msg.what = MSG_SEND_TRACKER_LIST;
			msg.setData(bundle);
			
			sendMessage(msg, torrent.getId());
		}
	}
	
	@Override
	public void updateBitfield(TorrentInfo torrent) {
		if (torrent != null && client_ != null && client_.isTypeSingle(torrent.getId())) {
			Bundle bundle = new Bundle();
			bundle.putSerializable(MSG_KEY_BITFIELD, torrent.getBitfield());
			bundle.putSerializable(MSG_KEY_DOWNLOADING_BITFIELD, torrent.getDownloadingBitfield());
			
			Message msg = Message.obtain();
			msg.what = MSG_SEND_BITFIELD;
			msg.setData(bundle);
	
			
			Log.v(LOG_TAG, "Send bitfield");
			sendMessage(msg, torrent.getId());
		}
	}
	
	/*/** Shows a toast with a message. */
	/*public void showToast(String s) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putString(MSG_KEY_MESSAGE, s);
		msg.what = MSG_SHOW_TOAST;
		msg.setData(bundle);
		
		sendMessage(msg, -1);
	}*/
	
	@Override
	public void showDialog(final int messageId) {
		int resId = -1;
		switch (messageId) {
			case MESSAGE_NOT_A_TORRENT_FILE:
				resId = R.string.not_a_torrent_file;
				break;
							
			case MESSAGE_WRONG_FILE:
				resId = R.string.status_wrong_file;
				break;
							
			case MESSAGE_WRONG_MAGNET_LINK:
				resId = R.string.wrong_magnet_link;
				break;
				
			case MESSAGE_NOT_ENOUGH_FREE_SPACE:
				resId = R.string.there_is_not_enough_free_space_on_the_sd_card;
				break;
				
			case MESSAGE_THE_TORRENT_IS_ALREADY_OPENED:
				resId = R.string.the_torrent_is_already_opened;
				break;
				
			case MESSAGE_ERROR:
				resId = R.string.error;
				break;
	
			default:
				break;
		}
		if (resId > -1) {
			showDialog(getString(resId));
		}
	}
	
	/** Shows a dialog with a message. */
	protected void showDialog(final String s) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putString(MSG_KEY_MESSAGE, s);
		msg.what = MSG_SHOW_DIALOG;
		msg.setData(bundle);
		
		sendMessage(msg, -1);
	}
	
	@Override
	public void showAlreadyOpenedDialog(String infoHash, ArrayList<String> trackerUrls) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putString(MSG_KEY_TORRENT_INFOHASH, infoHash);
		bundle.putStringArrayList(MSG_KEY_TRACKER_LIST, trackerUrls);
		msg.what = MSG_TORRENT_ALREADY_OPENED;
		msg.setData(bundle);
		
		sendMessage(msg, -1);
	}
	
	@Override
	public void showProgress(final int messageId) {
		int resId = -1;
		switch (messageId) {
			case MESSAGE_READING_THE_TORRENT_FILE:
				resId = R.string.reading_the_torrent_file;
				break;
	
			case MESSAGE_DOWNLOADING_THE_TORRENT:
				resId = R.string.downloading_the_torrent;
				
			default:
				break;
		}
		if (resId > -1) {
			showProgress(getString(resId));
		}
	}
	
	/** Shows a progress dialog with a message. */
	private void showProgress(final String s) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putString(MSG_KEY_MESSAGE, s);
		msg.what = MSG_SHOW_PROGRESS;
		msg.setData(bundle);
		
		sendMessage(msg, -1);
	}
	
	@Override
	public void hideProgress() {
		Message msg = Message.obtain();
		msg.what = MSG_HIDE_PROGRESS;
		
		sendMessage(msg, -1);
	}
	
	@Override
	public void updateMetadata(TorrentInfo torrent) {
		if (client_ != null && torrent != null && client_.isTypeSettings(torrent.getId())) {	
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
	
	@Override
	public boolean shouldUpdate(int updateField) {
		try {
			return (client_ != null && client_.shouldUpdate(updateField));
		} catch (Exception e) {
			return false;
		}
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
		
		TorrentInfo torrent = torrentManager_.getTorrent(torrentId);
		if (torrent == null) return;
		final PeerInfo[] peers = torrent.getConnectedPeers();
		final ArrayList<PeerListItem> peerListItems = new ArrayList<PeerListItem>();
		for (int i = 0; i < peers.length; i++) {
			peerListItems.add(new PeerListItem(peers[i]));
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
		
		TorrentInfo torrent = torrentManager_.getTorrent(torrentId);
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
		
		TorrentInfo torrent = torrentManager_.getTorrent(torrentId);
		if (torrent == null) return;
		final ArrayList<TrackerInfo> trackers = torrent.getTrackers();
		final ArrayList<TrackerListItem> trackerListItems = new ArrayList<TrackerListItem>();
		for (int i = 0; i < trackers.size(); i++) {
			trackerListItems.add(new TrackerListItem(trackers.get(i)));
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
		synchronized (torrents_) {
			Collections.sort(torrents_);
		}
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
		if (client_ != null) {
			if (client_.isTypeAll() || client_.isTypeSingle(torrentId)) {
				try {
					Messenger messenger = client_.getMessenger();
					messenger.send(msg);
				} catch (RemoteException e) {
					Log.v(LOG_TAG, LOG_ERROR_SENDING);
				}
			}
		}
	}
	
	/** Thread for creating a torrent. */
	private class CreateTorrentThread extends Thread {

		String filePath_ = null;
		String trackers_ = null;
		boolean isPrivate_ = false;
		String filePathSaveAs_ = null;
		boolean shouldStart_ = false;

		public CreateTorrentThread(String filePath, String trackers, boolean isPrivate, String filePathSaveAs, boolean shouldStart) {
			this.filePath_ = filePath;
			this.trackers_ = trackers;
			this.isPrivate_ = isPrivate;
			this.filePathSaveAs_ = filePathSaveAs;
			this.shouldStart_ = shouldStart;
		}

		@Override
		public void run() {
			if (filePath_ != null) {
				torrentManager_.createTorrent(filePath_, trackers_, isPrivate_, filePathSaveAs_, shouldStart_);
			}
		}
	}
	
	/** Thread for opening and reading a torrent file. */
	private class OpenTorrentThread extends Thread {

		private Uri torrentUri_ = null;
		
		public OpenTorrentThread(Uri torrentUri) {
			torrentUri_ = torrentUri;
		}
		
		@Override
		public void run() {
			if (torrentUri_ != null) {
				torrentManager_.openTorrent(torrentUri_);
			}
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
			ArrayList<Integer> filePriorities = new ArrayList<Integer>();
			if (fileList_ != null && !fileList_.isEmpty()) {
				for (FileListItem file : fileList_) {
					filePriorities.add(file.getPriority());
				}
			}
			torrentManager_.setTorrentSettings(item_.getId(), item_.getDownloadFolder(), isRemoved_, filePriorities);
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
			torrentManager_.changeFilePriority(torrentId_, item_.getIndex(), item_.getPriority());
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
