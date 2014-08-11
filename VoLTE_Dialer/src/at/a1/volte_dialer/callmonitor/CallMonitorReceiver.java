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

package at.a1.volte_dialer.callmonitor;

import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import at.a1.volte_dialer.Globals;

public class CallMonitorReceiver extends PhoneStateListener {
	private static final String TAG = "CallMonitorReceiver";
	
	public static int 	signalstrength;
	CallMonitorService	mCms;
	
	public CallMonitorReceiver(CallMonitorService cms) {
		signalstrength	= 99;	// = Unknown. Values in 3GPP TS27.007
		mCms 			= cms;
	}
	
	@Override
    public void onServiceStateChanged(ServiceState serviceState) {
		super.onServiceStateChanged(serviceState);
		Globals.iservicestate = serviceState.getState();
	}
	
	@Override
	public void onCallStateChanged(int state, String incomingNumber) {
		mCms.callStateChangedNotif(state, incomingNumber);
	}
	
	@Override
    public void onSignalStrengthsChanged(SignalStrength strength) {
		super.onSignalStrengthsChanged(strength);
		if(strength.isGsm()) {
			signalstrength = strength.getGsmSignalStrength();
		}
	}
}
