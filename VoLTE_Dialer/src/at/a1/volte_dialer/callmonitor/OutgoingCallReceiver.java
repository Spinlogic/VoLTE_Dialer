package at.a1.volte_dialer.callmonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OutgoingCallReceiver extends BroadcastReceiver {
	
	private CallMonitorService mCms;
	
	public OutgoingCallReceiver(CallMonitorService cms) {
		mCms = cms;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		mCms.moCallNotif(intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER));
	}

}
