package hu.bute.daai.amorg.drtorrent.ui.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.widget.Toast;

public class PowerConnectionStateReceiver extends BroadcastReceiver {
	
	private PowerConnectionStateListener listener_;
	
    @Override
    public void onReceive(Context context, Intent intent) { 
    	
    	boolean isPlugged = false;
    	String action = intent.getAction();
    	if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
    		isPlugged = true;
    		
    	} else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
    		isPlugged = false;
    		
    	}  else if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
    		int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
    		if (chargePlug == -1) {
    			return;
    		}
    		isPlugged = (chargePlug > 0);
    		
    	} else {
    		return;
    	}
        
    	if (listener_ != null) {
    		listener_.onPowerConnectionStateChanged(isPlugged);
    	}
    	
        //Toast.makeText(context, "Plugged: " + isPlugged, Toast.LENGTH_SHORT).show();
    }
    
    public void setOnPowerConnectionStateListener(PowerConnectionStateListener listener) {
		listener_ = listener;
	}
}