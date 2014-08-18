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

import net.spinlogic.logger.Logger;
import android.app.Application;
import android.telephony.ServiceState;

public class volte_dialer extends Application {
	
	public volte_dialer() {
		super();
	}
	
	
	@Override
	public void onCreate() {
        super.onCreate();
		// Initialize global variables 
        Globals.is_vd_running 			= false;
        Globals.msisdn 			= VD_Settings.getStringPref(
						        				this,
						        				VD_Settings.PREF_MSIDN,
						        				Globals.DEF_MSISDN);
        Globals.callduration	 = Integer.parseInt(VD_Settings.getStringPref(
						        				this, 
						        				VD_Settings.PREF_CALL_DURATION,
						        				"20"));
        Globals.timebetweencalls = Integer.parseInt(VD_Settings.getStringPref(
						        				this, 
						        				VD_Settings.PREF_WAIT_TIME,
						        				"20"));
        boolean is_bgmode 	 	 = VD_Settings.getBoolPref(this, 
												VD_Settings.PREF_BGMODE, 
												false);
        if(is_bgmode) {
        	Globals.opmode = Globals.OPMODE_BG;
        }
        else {
        	boolean is_receiver  = VD_Settings.getBoolPref(this, 
        										VD_Settings.PREF_RECEIVER, 
        										false);
        	Globals.opmode = (is_receiver) ? Globals.OPMODE_MT : Globals.OPMODE_MO;
        }
        Globals.iservicestate 	= ServiceState.STATE_OUT_OF_SERVICE;	// default initial service state
        Globals.icallnumber 	= 0;
        Globals.is_mtc_ongoing	= false;
        Logger.initializeValues();
    }
		
}
