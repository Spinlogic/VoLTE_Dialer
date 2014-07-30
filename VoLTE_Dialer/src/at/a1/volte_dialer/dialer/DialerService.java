/**
 *  Dialer for testing VoLTE network side KPIs.
 *  
 *   Copyright (C) 2014  Spinlogic
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as 
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package at.a1.volte_dialer.dialer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import at.a1.volte_dialer.Globals;
import at.a1.volte_dialer.phonestate.PhoneStateService;

/**
 * This service is used to start and stop the dialer.
 * 
 * @author Juan Noguera
 *
 */
public class DialerService extends Service {
	
	private final String TAG = "DialerService";

	public static DialerHandler hdialer;
	
	public DialerService() {
		
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final String METHOD = "::onStartCommand()  ";
		
		// start the PhoneStateService
		Intent psintent = new Intent(this, PhoneStateService.class);
		startService(psintent);
		// start dialing loop
		hdialer = new DialerHandler();
		hdialer.start(this);
		Log.d(TAG + METHOD, "service started");
		int res = super.onStartCommand(intent, flags, startId);
		return res;
	}
		
	@Override
	public void onDestroy() {
		final String METHOD = "::onDestroy()  ";
		final Context context = getApplicationContext();
		super.onDestroy();
		if(DialerHandler.isCallOngoing()) {
			Globals.hangupCall(); // Disconnect any call that is still ongoing
		}
		// Give some time to log the last call. In case there was one ongoing
		Handler h = new Handler();
		h.postDelayed(new Runnable() {
			public void run() {
				// stop the PhoneStateService
				Intent psintent = new Intent(context, PhoneStateService.class);
				stopService(psintent);
				// stop dialing loop
				hdialer.stop(context);
				Log.d(TAG + METHOD, "service destroyed");
			}
		}, 1000);	// Give a couple of seconds for PhoneStateHandler to 
					// find ServiceState
	}

	@Override
    public IBinder onBind(Intent intent) {
		// binding not needed
		return null;
	}
}
