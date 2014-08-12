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
import android.telephony.TelephonyManager;
import android.util.Log;
import at.a1.volte_dialer.Globals;

/**
 * This class implements a handler for the PhoneStateService
 * Two receiver classes are used. One [PhoneStateReceiver] to listen 
 * for standard call, signal and service states, and the other
 * [PreciseCallStateReceiver] to listen for precise states. This second
 * one works only on rooted devices.
 * 
 * @author Juan Noguera
 *
 */
public class PhoneStateHandler {
	private static final String TAG = "PhoneStateHandler";
	
	private PhoneStateReceiver mPhoneStateReceiver;
	private PreciseCallStateReceiver mPreciseCallStateReceiver;
	
	public PhoneStateHandler(Context context) {
		mPhoneStateReceiver 		= new PhoneStateReceiver(context);
		mPreciseCallStateReceiver 	= (Globals.is_running_as_system) ? 
									  new PreciseCallStateReceiver(context) : 
									  null;
	}
	
	public void start(Context context) {
		final String METHOD = "::start()  ";
		Log.d(TAG + METHOD, " Starting Phone state receivers.");
	    // Start listening for changes in service and call states
		TelephonyManager telMng = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		int flags = (Globals.is_running_as_system && (Globals.opmode != Globals.OPMODE_MT)) ? 0 : PhoneStateListener.LISTEN_CALL_STATE;
		if(Globals.opmode != Globals.OPMODE_MT) {
			flags = flags | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS | PhoneStateListener.LISTEN_SERVICE_STATE;
		}
	    telMng.listen(mPhoneStateReceiver, flags);
	    if(Globals.is_running_as_system) {	// this receiver is only needed for MO calls
	    	mPreciseCallStateReceiver.listen();
	    }
	    Log.d(TAG + METHOD, " Phone state receivers started.");
	}
	
	public void stop(Context context) {
		final String METHOD = "::stop()  ";
		if(Globals.is_running_as_system) {
			mPreciseCallStateReceiver.stop();
		}
		TelephonyManager telMng = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		telMng.listen(mPhoneStateReceiver, PhoneStateListener.LISTEN_NONE);
		Log.d(TAG + METHOD, " Phone state receivers stopped.");
	}
	
}
