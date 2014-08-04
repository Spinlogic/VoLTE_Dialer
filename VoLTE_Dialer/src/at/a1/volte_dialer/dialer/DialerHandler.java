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

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.telephony.ServiceState;
import android.util.Log;
import at.a1.volte_dialer.Globals;

/**
 * This class is used to set the 
 * 
 * @author Juan Noguera
 *
 */

public class DialerHandler {

	private static final String TAG = "DialerHandler";
	private static CallDescription calldescription;
	
	
	// PUBLIC METHODS
	
	public void start(final Context context) {
		Handler h = new Handler();
		h.postDelayed(new Runnable() {
			public void run() {
			dialCall(context, Globals.msisdn);
			}
		}, 2000);	// Give a couple of seconds for PhoneStateHandler to 
					// find ServiceState
	}
	
	public static void stop(final Context context) {
		Intent alarmIntent = new Intent(context, DialerReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(pendingIntent);
	}
	
	/**
	 * Terminates an ongoing call
	 */
	public static void endCall(int side) {
		// The call may have been terminated by the PhoneStateReceiver
		calldescription.endCall(side);
		if(side == CallDescription.CALL_DISCONNECTED_BY_UE) {
			Globals.hangupCall();
		}
		calldescription.writeCallInfoToLog();
		calldescription = null;	// let GC clean up the object
	}
	
	public static boolean isCallOngoing() {
		return (calldescription != null) ? true : false;
	}
	
	
	public static void dialCall(final Context c, final String msisdn) {
		final String METHOD = "::dialCall()  ";
		
		if(Globals.iservicestate == ServiceState.STATE_IN_SERVICE) {
			Handler h = new Handler();
			h.postDelayed(new Runnable() {
				public void run() {
					Intent intent = new Intent(Intent.ACTION_CALL);
					intent.setData(Uri.parse("tel:" + msisdn));
					intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
					c.startActivity(intent);
					Globals.icallnumber++;	// increment call counter
					Log.d(TAG + METHOD, "Making call to number: " + msisdn);
					DialerHandler.calldescription = new CallDescription(c);
					
					// Activate an alarm to end the call
					setAlarm(c, Globals.average_call_setup_time + Globals.callduration);	// TODO: the alarm should be set when the call is established
				}
			}, 500);	// Works better if the call is trigger after some delay
		}
		else {
			// wait 10 seconds and try again
			calldescription = null;
			setAlarm(c, 10);
		}
	}
	
	/**
	 * sets an Alarm to go off in waittime seconds from current time.
	 * 
	 * @param c
	 * @param waittime		in seconds
	 */
	@SuppressLint("NewApi")
	public static void setAlarm(Context c, long waittime) {
		Intent alarmIntent = new Intent(c, DialerReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(c, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarmManager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentapiVersion >= android.os.Build.VERSION_CODES.KITKAT){
			alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (waittime * 1000), pendingIntent);
		} else{
			alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (waittime * 1000), pendingIntent);
		}
	}
	
	public static void setCallState(final int newstate) {
		calldescription.setState(newstate);
	}
	
	public static int getCallState() {
		return calldescription.getState();
	}
	
}
