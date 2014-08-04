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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import at.a1.volte_dialer.Globals;

public class DialerReceiver extends BroadcastReceiver  {
	public static final String TAG = "DialerReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {
    	final String METHOD = ":onReceive()  ";
    	
    	if(DialerHandler.isCallOngoing()) {
    		Log.d(TAG + METHOD, "Terminate call.");
    		DialerHandler.endCall(CallDescription.CALL_DISCONNECTED_BY_UE);
    		DialerHandler.setAlarm(context, Globals.timebetweencalls);
    		Globals.mainactivity.startNextCallTimer();
    	}
    	else {
    		Log.d(TAG + METHOD, "Trigger new call.");
    		DialerHandler.dialCall(context, Globals.msisdn);
    		Globals.mainactivity.stopNextCallTimer();
    	}
    }
	
}
