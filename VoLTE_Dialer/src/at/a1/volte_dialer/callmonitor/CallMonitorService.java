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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;


/**
 * This service is used to monitor outgoing calls.
 * Tw
 * 
 * @author Juan Noguera
 *
 */
public class CallMonitorService extends Service {
	private static final String TAG = "CallMonitorService";
	
	final static public String EXTRA_OPMODE 		= "op_mode";
	final static public String EXTRA_SYSTEMSPACE 	= "system_space";	// is the service running in system process?
	
	// Operation modes
	final static public int CMS_MOC	= 0;		// Monitor outgoing calls
	final static public int CMS_MTC	= 1;		// Monitor incoming calls
	
	private int 	opmode;
	private boolean is_system;
	private CallMonitorReceiver mCallMonitorReceiver;
	private OutgoingCallReceiver mOutgoingCallReceiver;

	public CallMonitorService() {
		opmode = CMS_MOC;		// monitor outgoing calls
		is_system = false;
		mCallMonitorReceiver = null;
		mOutgoingCallReceiver = null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final String METHOD = "::onStartCommand()  ";
		
		Bundle extras = intent.getExtras();
		if(extras != null) {
			opmode 		= (extras.containsKey(EXTRA_OPMODE)) ? 
							extras.getInt(EXTRA_OPMODE) : 
							CMS_MOC;
			is_system	= (extras.containsKey(EXTRA_SYSTEMSPACE)) ? 
							extras.getBoolean(EXTRA_SYSTEMSPACE) : 
							false;
		}
		
		mCallMonitorReceiver = new CallMonitorReceiver(this);
		TelephonyManager telMng = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		int mflags = (is_system && opmode == CMS_MOC) ? 0 : PhoneStateListener.LISTEN_CALL_STATE;
		if(opmode == CMS_MOC) {
			mflags = mflags | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS | PhoneStateListener.LISTEN_SERVICE_STATE;
			mOutgoingCallReceiver = new OutgoingCallReceiver(this);
		    telMng.listen(mCallMonitorReceiver, flags);
		    IntentFilter mocFilter = new IntentFilter(Intent.ACTION_NEW_OUTGOING_CALL);
		    registerReceiver(mOutgoingCallReceiver, mocFilter);
		}
	    
		int res = super.onStartCommand(intent, flags, startId);
		return res;
	}
		
	@Override
	public void onDestroy() {
		final String METHOD = "::onDestroy()  ";
		TelephonyManager telMng = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		telMng.listen(mCallMonitorReceiver, PhoneStateListener.LISTEN_NONE);
		if(opmode == CMS_MOC) {
		    unregisterReceiver(mOutgoingCallReceiver);
		}
		Log.d(TAG + METHOD, " Call info receivers stopped.");
		super.onDestroy();
	}

	@Override
    public IBinder onBind(Intent intent) {
		// binding not needed
		return null;
	}
	
	// Methods for CallMonitorReceiver and OutgoingCallReceiver to report events
	// This methods 
	
	/**
	 * Used by CallMonitorReceiver to report a change in call state.
	 * The new call state is communicated to the binded apps.
	 * 
	 * @param callstate		TelephonyManager states
	 * @param inmsisdn		incoming number (for MT calls)
	 */
	public void callStateChangedNotif(int callstate, String inmsisdn) {
		
	}
	
	/**
	 * Notifies the number for an outgoing call.
	 * 
	 * @param msisdn
	 */
	public void moCallNotif(String msisdn) {
		
	}
}
