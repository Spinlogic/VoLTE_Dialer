/**
 *  Part of the dialer for testing VoLTE network side KPIs.
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

package at.a1.volte_dialer;

import net.spinlogic.logger.Logger;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import at.a1.volte_dialer.callmonitor.CallMonitorService;

public class BootUpReceiver extends BroadcastReceiver{
	private static final String TAG = "BootUpReceiver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		final String METHOD = "::onReceive()  ";
		
		Logger.Log(TAG + METHOD, "Checking if service should run in background.");
		if(Globals.opmode == Globals.OPMODE_BG) {	// Activate logging service
			Logger.Log(TAG + METHOD, "Launching CallMonitorService on Boot.");
			Intent cms = new Intent(context, CallMonitorService.class);
			cms.putExtra(CallMonitorService.EXTRA_OPMODE, CallMonitorService.OPMODE_BG);
    		context.startService(cms);
		}
	}

}
