package at.a1.volte_dialer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import at.a1.volte_dialer.callmonitor.CallMonitorService;
import at.a1.volte_dialer.dialer.DialerService;

public class BootUpReceiver extends BroadcastReceiver{
	private static final String TAG = "CallMonitorService";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		final String METHOD = "::onStartCommand()  ";
		
		if(Globals.opmode == Globals.OPMODE_BG) {	// Activate logging service
			Log.i(TAG + METHOD, "Launching CallMonitorService.");
			Intent cms = new Intent(context, CallMonitorService.class);
    		context.startService(cms);
		}
	}

}
