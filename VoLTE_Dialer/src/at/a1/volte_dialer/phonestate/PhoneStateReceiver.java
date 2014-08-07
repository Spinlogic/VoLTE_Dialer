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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import at.a1.volte_dialer.Globals;
import at.a1.volte_dialer.VDMainActivity;
import at.a1.volte_dialer.dialer.CallDescription;
import at.a1.volte_dialer.dialer.DialerHandler;

/**
 * This class is used to listen for changes in phone state.
 * 
 * @author Juan Noguera
 *
 */
public class PhoneStateReceiver extends PhoneStateListener {
	private static final String TAG = "PhoneStateReceiver";
	
	private Context	context;
	
	public static int 	signalstrength;
	
	public PhoneStateReceiver(Context c) {
		context			= c;
		signalstrength	= 99;	// = Unknown. Values in 3GPP TS27.007
	}
	
	@Override
    public void onServiceStateChanged(ServiceState serviceState) {
		super.onServiceStateChanged(serviceState);
		Globals.iservicestate = serviceState.getState();
	}
	
	@Override
	public void onCallStateChanged(int state, String incomingNumber) {
		switch(state) {
			case TelephonyManager.CALL_STATE_IDLE:
				if(!Globals.is_receiver && DialerHandler.isCallOngoing()) {
					// The call has been disconnected by the network.
					// Stop pending alarms to terminate the call from UE side.
					DialerHandler.stop(context);
					DialerHandler.endCall(CallDescription.CALL_DISCONNECTED_BY_NW);
					DialerHandler.setAlarm(context, Globals.timebetweencalls);	//	Set timer for next call
					if(Globals.mainactivity != null) {
						Globals.mainactivity.startNextCallTimer();
					}
				}
				Globals.is_mtc_ongoing = false;
				break;
			case TelephonyManager.CALL_STATE_RINGING:
				if(Globals.is_receiver) {
					boolean autoanswer = false;
					if(incomingNumber != null && !incomingNumber.isEmpty()) {
						// right match of the last n - 2 digits
						String numbertomatch = incomingNumber.substring(2);
						if(Globals.msisdn.contains(numbertomatch)) {
							Globals.answerCall(context);
							Globals.is_mtc_ongoing = true;
							Globals.icallnumber++;
							if(Globals.mainactivity != null) {
								Globals.mainactivity.refreshCallNumber();
							}
						}
					}
				}
				else {
					DialerHandler.setCallState(TelephonyManager.CALL_STATE_RINGING); // DEBUG
				}
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK:
				if(!Globals.is_receiver) {
					DialerHandler.setCallState(TelephonyManager.CALL_STATE_OFFHOOK); // DEBUG
				}
				// This state is triggered when the line is seized. The call is being dialed.
				// There is no call state that indicates that the call is connected.
				break;
		}
	}
	
	@Override
    public void onSignalStrengthsChanged(SignalStrength strength) {
		super.onSignalStrengthsChanged(strength);
		if(strength.isGsm()) {
			signalstrength = strength.getGsmSignalStrength();
		}
	}
	
}
