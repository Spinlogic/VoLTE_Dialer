package at.a1.volte_dialer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootUpReceiver extends BroadcastReceiver{
	@Override
	public void onReceive(Context context, Intent intent) {
		if(Globals.is_bgmode) {	// Activate logging service
			Globals.is_running_as_system = Globals.isAppRunningAsSystem();
		}
	}

}
