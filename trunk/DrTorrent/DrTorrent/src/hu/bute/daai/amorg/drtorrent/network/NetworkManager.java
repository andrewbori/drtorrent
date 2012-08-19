package hu.bute.daai.amorg.drtorrent.network;

import hu.bute.daai.amorg.drtorrent.Preferences;
import hu.bute.daai.amorg.drtorrent.torrentengine.TorrentManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import android.util.Log;

public class NetworkManager {
	private static final String LOG_TAG = "NetworkManager";
	
	private ServerSocket serverSocket_ = null;
	private TorrentManager torrentManager_ = null;
	private boolean isListening_ = false;
	
	public NetworkManager(TorrentManager torrentManager) {
		torrentManager_ = torrentManager;
	}
	
	private class AcceptConnections extends Thread {
		@Override
		public void run() {
			try {
				serverSocket_ = new ServerSocket(Preferences.getPort());

				while (isListening_)  {
					Log.v(LOG_TAG, "Listening....");
					Socket socket = serverSocket_.accept();
					Log.v(LOG_TAG, "Incoming connection: " + socket.getInetAddress() + ":" + socket.getPort());
					torrentManager_.addIncomingConnection(socket);
				}
			} catch (IOException e) {
				Log.v(LOG_TAG, "Incoming connection error.");
			}
		}
	}
	
	public void startListening() {
		isListening_ = true;
		(new AcceptConnections()).start();
	}
	
	public void stopListening() {
		isListening_ = false;
		try {
			serverSocket_.close();
		} catch (IOException e) {}
	}
}
