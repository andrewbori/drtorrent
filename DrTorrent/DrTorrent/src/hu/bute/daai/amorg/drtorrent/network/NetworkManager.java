package hu.bute.daai.amorg.drtorrent.network;

import hu.bute.daai.amorg.drtorrent.Preferences;
import hu.bute.daai.amorg.drtorrent.torrentengine.TorrentManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import android.util.Log;

/** CLass that handles incoming connections. */
public class NetworkManager {
	private static final String LOG_TAG = "NetworkManager";
	
	private ServerSocket serverSocket_ = null;
	private TorrentManager torrentManager_ = null;
	private AcceptConnections acceptConnectionsThread_ = null;
	private static boolean isListening_ = false;
	
	/** Constructor with the Torrent Manager. */
	public NetworkManager(TorrentManager torrentManager) {
		torrentManager_ = torrentManager;
	}
	
	/** Thread for handling incoming connections. */
	private class AcceptConnections extends Thread {
		private boolean enabled_ = false;
		
		@Override
		public void run() {
			try {
				serverSocket_ = new ServerSocket(Preferences.getPort());

				while (enabled_)  {
					Log.v(LOG_TAG, "Listening....");
					Socket socket = serverSocket_.accept();
					Log.v(LOG_TAG, "Incoming connection: " + socket.getInetAddress() + ":" + socket.getPort());
					torrentManager_.addIncomingConnection(socket);
				}
			} catch (IOException e) {
				Log.v(LOG_TAG, "Incoming connection error.");
			}
		}
		
		public void enable() { enabled_ = true; }
		public void disable() { enabled_ = false; }
	}
	
	/** Starts listening for incoming connections. */
	public void startListening() {
		if (isListening_) stopListening();
		
		isListening_ = true;
		acceptConnectionsThread_ = new AcceptConnections();
		acceptConnectionsThread_.enable();
		acceptConnectionsThread_.start();
	}
	
	/** Stops listening for incoming connections. */
	public void stopListening() {
		isListening_ = false;
		if (acceptConnectionsThread_ != null) acceptConnectionsThread_.disable();
		try {
			serverSocket_.close();
		} catch (Exception e) {}
	}
	
	/** Returns whether the device is connected to a network or not. */
	public static boolean hasNetorkConnection() {
		return isListening_;
	}
}
