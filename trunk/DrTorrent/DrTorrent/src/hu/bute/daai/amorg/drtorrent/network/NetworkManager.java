package hu.bute.daai.amorg.drtorrent.network;

import hu.bute.daai.amorg.drtorrent.core.torrent.TorrentManager;
import hu.bute.daai.amorg.drtorrent.ui.service.NetworkStateListener;
import hu.bute.daai.amorg.drtorrent.util.Log;
import hu.bute.daai.amorg.drtorrent.util.Preferences;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

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
			Thread portMappingThread = null;
			try {
				port_ = Preferences.getPort();
				
				if (Preferences.isIncomingConnectionsEnabled() && Preferences.isUpnpEnabled()) {
					portMappingThread = new AddPortMappingThread();
					portMappingThread.start();
				}
				
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
				
				try {
					if (portMappingThread != null && portMappingThread.isAlive()) {
						portMappingThread.stop();
					}
				} catch (Exception e) {
				}
			}
			
			deletePortMapping();
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
	
	private class AddPortMappingThread extends Thread {
		@Override
		public void run() {
			addPortMapping();
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
	
	/** Adds port mapping. */
	public boolean addPortMapping() {
		try {
			if (System.getProperty("org.xml.sax.driver") == null) {
				System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");
			}
			GatewayDiscover gatewayDiscover = new GatewayDiscover();
			gatewayDiscover.discover();
			GatewayDevice gatewayDevice = gatewayDiscover.getValidGateway();
			
			if (null != gatewayDevice) {
			    Log.v(LOG_TAG, "Gateway device found. " + gatewayDevice.getModelName() + " " + gatewayDevice.getModelDescription());
			} else {
			    Log.v(LOG_TAG, "No valid gateway device found.");
			    return false;
			}
			
			InetAddress localAddress = gatewayDevice.getLocalAddress();
			Log.v(LOG_TAG, "Local address: " + localAddress);
			String externalAddress = gatewayDevice.getExternalIPAddress();
			Log.v(LOG_TAG, "External address: " + externalAddress);
			
			PortMappingEntry portMapping = new PortMappingEntry();
			boolean success = false;
			if (!gatewayDevice.getSpecificPortMappingEntry(port_, "TCP", portMapping)) {
				success = gatewayDevice.addPortMapping(port_, port_, localAddress.getHostAddress(), "TCP", "DrTorrent");
				Log.v(LOG_TAG, "Port " + port_ + " mapped: " + success);
			} else {
				Log.v(LOG_TAG, "Port " + port_ + " already mapped by " + portMapping.getInternalClient());
				if (portMapping.getInternalClient().equals(localAddress.getHostAddress())) {
					success = true;
				}
			}
			return success;
		} catch (Exception e) {
			Log.v(LOG_TAG, "Exception: " + e.toString());
		}
		return false;
	}
	
	/** Removes port mapping. */
	public void deletePortMapping() {
		try {
			if (System.getProperty("org.xml.sax.driver") == null) {
				System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");
			}
			GatewayDiscover discover = new GatewayDiscover();
			discover.discover();
			GatewayDevice device = discover.getValidGateway();
			
			Log.v(LOG_TAG, "Removing port mapping: " + port_);
			
			device.deletePortMapping(port_, "TCP");
		} catch (Exception e) {
		}
	}
	
	/** Called when the connection settings are changed. */
	public void connectionSettingsChanged(boolean isChangeSpecified, boolean isPortChanged, boolean isIncomingChanged, boolean isWifiOnlyChanged, boolean isUpnpChanged) {
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
				// Incoming connections enabled/disabled or P2P Port changed or UPnP changed.
				if (!isListening_) {
					if (Preferences.isIncomingConnectionsEnabled()) {
						startListening();
					}
				} else {
					if (!Preferences.isIncomingConnectionsEnabled()) {
						stopListening();
					} else {
						if (isChangeSpecified && isUpnpChanged && !Preferences.isUpnpEnabled()) {
							(new Thread() {
								public void run() {
									deletePortMapping();
								};
							}).start();
						}
						
						if (port_ != Preferences.getPort()) {
							startListening();
						} else {
							if (isChangeSpecified && isUpnpChanged && Preferences.isUpnpEnabled()) {
								(new Thread() {
									public void run() {
										addPortMapping();
									};
								}).start();
							}
						}
					}
				}
			} else {
				stopListening();
				torrentManager_.disable();
			}
		}
	}
	
	/** Called when the network state changes. */
	@Override
	public void onNetworkStateChanged(boolean noConnectivity, NetworkInfo networkInfo) {
		if (networkInfo != null) {
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
		}
		
		connectionSettingsChanged(false, false, false, false, false);
	}
	
	/** Returns whether has connection or not. */
	public boolean hasConnection() {
		return (isWifiConnected_ || isEthernetConnected_ || ((isWiMaxConnected_ || isMobileNetConnected_) && !Preferences.isWiFiOnly()));
	}
}
