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

package at.a1.volte_dialer.phonestate;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


/**
 * This service is used to listen for changes in service and call states of the phone
 * It is used to:
 * 		1. make sure that calls are not triggered if the ServiceState is not 
 * 		   STATE_IN_SERVICE.
 * 		2. Monitor call state for the calls triggered by the application.
 * 
 * This service runs while the Dialer service is running.
 * 
 * @author Juan Noguera
 *
 */
public class PhoneStateService extends Service {
	
	private final String TAG = "PhoneStateService";

	private PhoneStateHandler phoneStateHandler;
	
	public PhoneStateService() {
		
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final String METHOD = "::onStartCommand()  ";
		phoneStateHandler = new PhoneStateHandler(this);
		int res = super.onStartCommand(intent, flags, startId);
		phoneStateHandler.start(this);
		Log.d(TAG + METHOD, "service started");
		return res;
	}
		
	@Override
	public void onDestroy() {
		final String METHOD = "::onDestroy()  ";
		super.onDestroy();
		phoneStateHandler.stop(this);
		Log.d(TAG + METHOD, "service destroyed");
	}

	@Override
    public IBinder onBind(Intent intent) {
		// binding not needed
		return null;
	}
	
}
