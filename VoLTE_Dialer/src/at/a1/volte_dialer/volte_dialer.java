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

package at.a1.volte_dialer;

import at.a1.volte_dialer.VD_Logger;
import android.app.Application;
import android.content.Context;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;

public class volte_dialer extends Application {

	public volte_dialer() {
		super();
	}
	
	
	@Override
	public void onCreate() {
        super.onCreate();
        
        Globals.is_vd_running = false;
        
        // Init the logger
        VD_Logger.initializeValues();
        
        //	init globals        
        Globals.msisdn = VD_Settings.getStringPref(
        				this,
        				VD_Settings.PREF_MSIDN,
        				Globals.DEF_MSISDN);
        Globals.callduration = VD_Settings.getIntPref(
        				this, 
        				VD_Settings.PREF_CALL_DURATION,
        				5);
        Globals.timebetweencalls = VD_Settings.getIntPref(
        				this, 
        				VD_Settings.PREF_WAIT_TIME,
        				5);
        
        Globals.iservicestate 	= ServiceState.STATE_OUT_OF_SERVICE;	// default initial service state
    }
	
}
