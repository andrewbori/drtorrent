package hu.bute.daai.amorg.drtorrent.network;

import hu.bute.daai.amorg.drtorrent.Preferences;
import hu.bute.daai.amorg.drtorrent.service.NetworkStateListener;
import hu.bute.daai.amorg.drtorrent.torrentengine.TorrentManager;

import java.net.ServerSocket;
import java.net.Socket;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/** CLass that handles incoming connections. */
public class NetworkManager implements NetworkStateListener {
	private static final String LOG_TAG = "NetworkManager";
	
	private TorrentManager torrentManager_ = null;
	private AcceptConnections acceptConnectionsThread_ = null;
	private static boolean isListening_ = false;
	private static int port_;
	
	private boolean hasConnection_ 		  = false;
	private boolean isWifiConnected_ 	  = false;
	private boolean isEthernetConnected_  = false;
	private boolean isMobileNetConnected_ = false;
	private boolean isWiMaxConnected_ 	  = false;
	
	public void setTorrentManager(TorrentManager torrentManager) {
		this.torrentManager_ = torrentManager;
		port_ = Preferences.getPort();
	}
	
	/** Thread for handling incoming connections. */
	private class AcceptConnections extends Thread {
		private ServerSocket serverSocket_ = null;
		private boolean enabled_ = false;
		
		@Override
		public void run() {
			try {
				port_ = Preferences.getPort();
				serverSocket_ = new ServerSocket(port_);

				Log.v(LOG_TAG, "Start listening on port " + port_);
				
				while (enabled_)  {
					Socket socket = serverSocket_.accept();
					Log.v(LOG_TAG, "Incoming connection: " + socket.getInetAddress() + ":" + socket.getPort());
					if (torrentManager_ != null) {
						torrentManager_.addIncomingConnection(socket);
					}
				}
			} catch (Exception e) {
			} finally {
				Log.v(LOG_TAG, "Stop listening on port " + port_);
				try {
					if (enabled_) {
						if (serverSocket_ != null) {
							serverSocket_.close();
						}
						serverSocket_ = null;
					}
				} catch (Exception e) {
				}
			}
		}
		
		/** Enables the incoming connection accepting thread. */
		public void enable() {
			enabled_ = true;
		}
		
		/** Disables the incoming connection accepting thread. */
		public void disable() {
			enabled_ = false;
			
			try {
				if (serverSocket_ != null) {
					serverSocket_.close();
				}
				serverSocket_ = null;
			} catch (Exception e) {
			}
		}
	}
	
	/** Starts listening for incoming connections. */
	public void startListening() {
		if (isListening_) stopListening();
		if (Preferences.isIncomingConnectionsEnabled()) {
			isListening_ = true;
			acceptConnectionsThread_ = new AcceptConnections();
			acceptConnectionsThread_.enable();
			acceptConnectionsThread_.start();
		}
	}
	
	/** Stops listening for incoming connections. */
	public void stopListening() {
		isListening_ = false;
		if (acceptConnectionsThread_ != null) {
			acceptConnectionsThread_.disable();
			acceptConnectionsThread_ = null;
		}
	}
	
	/** Called when the connection settings are changed. */
	public void connectionSettingsChanged() {
		boolean hasNewConnection = hasConnection();
		
		if (hasConnection_ != hasNewConnection) {
			hasConnection_ = hasNewConnection;
			
			if (hasConnection_) {
				startListening();
				torrentManager_.enable();
			} else {
				stopListening();
				torrentManager_.disable();
			}
		} else {
			if (hasConnection_) {
				// Incoming connections enabled/disabled or P2P Port changed.
				if (!isListening_) {
					if (Preferences.isIncomingConnectionsEnabled()) {
						startListening();
					}
				} else {
					if (!Preferences.isIncomingConnectionsEnabled()) {
						stopListening();
					} else {
						if (port_ != Preferences.getPort()) {
							startListening();
						}
					}
				}
			}
		}
	}
	
	/** Called when the network state changes. */
	@Override
	public void onNetworkStateChanged(boolean noConnectivity, NetworkInfo networkInfo) {
		if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI){
			isWifiConnected_ = !noConnectivity;
			Log.v(LOG_TAG, "Has connection? WIFI CONNECTION: " + !noConnectivity);
		} else if (networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET){
			isEthernetConnected_ = !noConnectivity;
			Log.v(LOG_TAG, "Has connection? ETHERNET CONNECTION: " + !noConnectivity);
		} else if (networkInfo.getType() == ConnectivityManager.TYPE_WIMAX){
			isWiMaxConnected_ = !noConnectivity;
			Log.v(LOG_TAG, "Has connection? WIMAX CONNECTION: " + !noConnectivity);
		} else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
			isMobileNetConnected_ = !noConnectivity;
			Log.v(LOG_TAG, "Has connection? MOBILE INTERNET CONNECTION: " + !noConnectivity);
		} else Log.v(LOG_TAG, "Has connection? OTHER CONNECTION: " + !noConnectivity);
		
		connectionSettingsChanged();
	}
	
	/** Returns whether has connection or not. */
	public boolean hasConnection() {
		return (isWifiConnected_ || isEthernetConnected_ || ((isWiMaxConnected_ || isMobileNetConnected_) && !Preferences.isWiFiOnly()));
	}
}
