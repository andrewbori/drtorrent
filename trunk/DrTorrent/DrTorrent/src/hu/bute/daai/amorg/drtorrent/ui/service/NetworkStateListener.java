package hu.bute.daai.amorg.drtorrent.ui.service;

import android.net.NetworkInfo;

public interface NetworkStateListener {
	public void onNetworkStateChanged(boolean noConnectivity, NetworkInfo networkInfo);
}
