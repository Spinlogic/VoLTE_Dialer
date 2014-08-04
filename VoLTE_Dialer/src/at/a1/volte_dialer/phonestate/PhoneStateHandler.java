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

import java.lang.reflect.Method;

import android.content.Context;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import at.a1.volte_dialer.Globals;

/**
 * This class implements a handler for the PhoneStateService
 * 
 * @author Juan Noguera
 *
 */
public class PhoneStateHandler {
	private static final String TAG = "PhoneStateHandler";
	
	private PhoneStateReceiver stateListener;
	
	public PhoneStateHandler(Context context) {
		stateListener 	= new PhoneStateReceiver(context);
	}
	
	public void start(Context context) {
	    // Start listening for changes in service and call states
		TelephonyManager telMng = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		int flags = PhoneStateListener.LISTEN_CALL_STATE;
		if(!Globals.is_receiver) {
			flags = flags | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS | PhoneStateListener.LISTEN_SERVICE_STATE;
		}
	    telMng.listen(stateListener, flags);
	}
	
	public void stop(Context context) {
		TelephonyManager telMng = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		telMng.listen(stateListener, PhoneStateListener.LISTEN_NONE);
	}
	
/*	private void registerForDetailedCallEvents(Context context) {
		Class<?> mPhoneFactory = Class.forName("com.android.internal.telephony.PhoneFactory");
		Method mMakeDefaultPhone = mPhoneFactory.getMethod("makeDefaultPhone", new Class[] {Context.class});
		mMakeDefaultPhone.invoke(null, context);

		Method mGetDefaultPhone = mPhoneFactory.getMethod("getDefaultPhone", null);
		Object mPhone = mGetDefaultPhone.invoke(null);

		Method mRegisterForStateChange = mPhone.getClass().getMethod("registerForPreciseCallStateChanged",
		new Class[]{Handler.class, Integer.TYPE, Object.class});            

		mRegisterForStateChange.invoke(mPhone, mHandler, CALL_STATE_CHANGED, null);
	} */
}
