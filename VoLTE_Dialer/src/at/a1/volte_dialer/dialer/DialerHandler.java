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
import at.a1.volte_dialer.CallDescription;
import at.a1.volte_dialer.Globals;
import at.a1.volte_dialer.phonestate.PreciseCallStateReceiver;

/**
 * This class is used to set the 
 * 
 * @author Juan Noguera
 *
 */

public class DialerHandler {

	private static final String TAG = "DialerHandler";
	private static CallDescription calldescription;
	
	// default values
	String msisdn = "";
	int duration = 20;		
	int waittime = 20;
	
	
	// PUBLIC METHODS
	
	public void setMsisdn(String telnum) {
		msisdn = telnum;
	}
	
	public void setCallDuration(int cd) {
		duration = cd;
	}
	
	public void setTimeBetweenCalls(int tbc) {
		waittime = tbc;
	}
	
	public void start(final Context context) {
		dialCall(context, msisdn);
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
			if(Globals.is_running_as_system) {	// Globals.hangupCall() does not work in this case
				PreciseCallStateReceiver.hangupCall();
				// Delay the logging for 1 second to give time to
				// PreciseCallStateReceiver to get the disconnect cause
				Handler h = new Handler();
				h.postDelayed(new Runnable() {
					public void run() {
						calldescription.writeCallInfoToLog();
						calldescription = null;
					}
				}, 1000);
			}
			else {	
				Globals.hangupCall();
				// in user space, we do no get disconnect cause
				calldescription.writeCallInfoToLog();
				calldescription = null;
			}
		}
		
	}
	
	public static boolean isCallOngoing() {
		return (calldescription != null) ? true : false;
	}
	
	
	public static void dialCall(final Context c, final String msisdn) {
		final String METHOD = "::dialCall()  ";
		
		Handler h = new Handler();
		h.postDelayed(new Runnable() {
			public void run() {
				DialerHandler.calldescription = new CallDescription(c);
				Intent intent = new Intent(Intent.ACTION_CALL);
				intent.setData(Uri.parse("tel:" + msisdn));
				intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
				c.startActivity(intent);
//				Globals.icallnumber++;	// increment call counter
				Log.d(TAG + METHOD, "Calling " + msisdn);
				
				// Activate an alarm to end the call
				if(Globals.is_running_as_system) {
					setAlarm(c, Globals.max_call_setup_time);
				}
				else {
					setAlarm(c, Globals.average_call_setup_time + Globals.callduration);	// TODO: the alarm should be set when the call is established
				}
			}
		}, 500);	// Works better if the call is trigger after some delay
	
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
	
	public static void setAlertingTime() {
		calldescription.setAlertingTime();
	}
	
	public static void setActiveTime() {
		calldescription.setActiveTime();
	}
	
	public static void setDisconnectionCause(String cause) {
		calldescription.setDisconnectionCause(cause);
	}
	
	public static boolean isDisconnectionCauseKnown() {
		return calldescription.isDisconnectionCauseKnown();
	}
}
