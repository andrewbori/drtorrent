package hu.bute.daai.amorg.drtorrent.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkStateReceiver extends BroadcastReceiver {

	private NetworkStateListener listener_;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
        //String reason = intent.getStringExtra(ConnectivityManager.EXTRA_REASON);
        //boolean isFailover = intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);
        NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
        //NetworkInfo otherNetworkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);
                    
        if (listener_ != null) listener_.onNetworkStateChanged(noConnectivity, networkInfo);
	}
	
	public void setOnNetworkStateListener(NetworkStateListener listener) {
		listener_ = listener;
	}

}
