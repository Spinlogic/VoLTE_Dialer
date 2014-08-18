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

package at.a1.volte_dialer.callmonitor;

import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

public class CallMonitorReceiver extends PhoneStateListener {
	private static final String TAG = "CallMonitorReceiver";
	
	CallMonitorInterface	mCmIf;
	
	public CallMonitorReceiver(CallMonitorService cmif) {
		mCmIf 			= cmif;
	}
	
	@Override
    public void onServiceStateChanged(ServiceState serviceState) {
		super.onServiceStateChanged(serviceState);
		int state = serviceState.getState();
		int cmState = CallMonitorService.MSG_SERVER_STATE_OUTSERVICE;
		if(state == ServiceState.STATE_IN_SERVICE) {
			cmState = CallMonitorService.MSG_SERVER_STATE_INSERVICE;
		}
		mCmIf.csmif_ServiceState(cmState);
	}
	
	@Override
	public void onCallStateChanged(int state, String incomingNumber) {
		switch(state) {
			case TelephonyManager.CALL_STATE_IDLE:
				mCmIf.csmif_CallState(CallMonitorService.CALLSTATE_IDLE, null);
				break;
			case TelephonyManager.CALL_STATE_RINGING:
				mCmIf.csmif_CallState(CallMonitorService.CALLSTATE_RINGING, incomingNumber);
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK:
				mCmIf.csmif_CallState(CallMonitorService.CALLSTATE_OFFHOOK, incomingNumber);
				break;
		}
	}
	
	@Override
    public void onSignalStrengthsChanged(SignalStrength strength) {
		super.onSignalStrengthsChanged(strength);
		if(strength.isGsm()) {
			mCmIf.csmif_SignalStrength(strength.getGsmSignalStrength());
		}
	}
}
