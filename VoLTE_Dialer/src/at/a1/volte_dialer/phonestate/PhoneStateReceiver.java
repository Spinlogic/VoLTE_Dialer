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

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import at.a1.volte_dialer.Globals;
import at.a1.volte_dialer.dialer.DialerHandler;

/**
 * This class is used to listen for changes in phone state.
 * 
 * @author Juan Noguera
 *
 */
public class PhoneStateReceiver extends PhoneStateListener {
	private static final String TAG = "SosPhoneStateListener";
	
	private Context	context;	
	
	public PhoneStateReceiver(Context c) {
		context = c;
	}
	
	@Override
    public void onServiceStateChanged(ServiceState serviceState) {
		super.onServiceStateChanged(serviceState);
		Globals.iservicestate = serviceState.getState();
	}
	
	@Override
	public void onCallStateChanged(int state, String incomingNumber) {
		if(incomingNumber == null) {	// MO call
			switch(state) {
				case TelephonyManager.CALL_STATE_IDLE:
					if(DialerHandler.isCallOngoing()) {
						// The call has been disconnected by the network.
						// Stop pending alarms to terminate the call from UE side.
						DialerHandler.stop(context);
						DialerHandler.clearCall();
						DialerHandler.endCall();
					}
					break;
				case TelephonyManager.CALL_STATE_RINGING:
					// TODO: only for MT calls
					DialerHandler.setCallState(TelephonyManager.CALL_STATE_RINGING); // DEBUG
					break;
				case TelephonyManager.CALL_STATE_OFFHOOK:
					DialerHandler.setCallState(TelephonyManager.CALL_STATE_OFFHOOK); // DEBUG
					// TODO: check when this state is actually triggered
					// if triggered when the call is active, then set the callduration alarm here
					// if when the call is dialing, then wait for four or five seconds to make sure that
					// there is enough time to connect the call to an autoanswer system.
					break;
			}
			
		}
		else {	// MT call
			// TODO: ffs.
		}
	}
	
}
