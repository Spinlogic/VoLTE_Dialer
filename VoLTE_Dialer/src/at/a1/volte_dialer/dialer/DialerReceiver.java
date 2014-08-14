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

public class DialerReceiver extends BroadcastReceiver  {
	public static final String TAG = "DialerReceiver";
	
	public static DsHandlerInterface dsIf = null;
	
/*	public void setDs(DsHandlerInterface ds) {
		dsIf = ds;
	} */
	
    @Override
    public void onReceive(final Context context, Intent intent) {
    	final String METHOD = ":onReceive()  ";
    	
    	if(dsIf != null) {
	    	if(dsIf.dsIf_isCallOngoing()) {
	    		Log.i(TAG + METHOD, "Terminate call.");
	    		dsIf.dsIf_endCall();
	    	}
	    	else {
	    		Log.i(TAG + METHOD, "Trigger new call.");
	    		dsIf.dsIf_dialCall();
	    	}
    	}
    }
	
}
