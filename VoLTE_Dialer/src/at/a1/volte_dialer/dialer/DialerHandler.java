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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
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
	private Context context;
	private static CallDescription calldescription;
	
	
	public DialerHandler(Context c) {
		context = c;
	}
	
	// PUBLIC METHODS
	
	public void start() {
		dialCall(context, Globals.msisdn);
	}
	
	public void stop() {
		Intent alarmIntent = new Intent(context, DialerReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(pendingIntent);
		
	}
	
	/**
	 * Terminates an ongoing call
	 */
	public static void endCall() {
		if(calldescription != null) {
			Globals.hangupCall();
			calldescription.endCall();
			calldescription.writeCallInfoToLog();
			calldescription = null;	// let GC take the object
		}
	}
	
	public static boolean isCallOngoing() {
		return (calldescription != null) ? true : false;
	}
	
	public static void dialCall(final Context c, final String msisdn) {
		final String METHOD = "::dialCall()  ";
		
		// TODO: read ServiceState before triggering a call. Do not trigger calls if state is not STATE_IN_SERVICE
		Handler h = new Handler();
		h.postDelayed(new Runnable() {
			public void run() {
				Intent intent = new Intent(Intent.ACTION_CALL);
				intent.setData(Uri.parse("tel:" + msisdn));
				intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
				c.startActivity(intent);
				Log.d(TAG + METHOD, "Making call to number: " + msisdn);
				DialerHandler.calldescription = new CallDescription(c);
				
				// Activate an alarm to end the call
				setAlarm(c, Globals.callduration);
			}
		 }, 500);	// Works better if the call is trigger after some delay
	}
	
	/**
	 * sets an Alarm to go off in waittime seconds from current time.
	 * 
	 * @param c
	 * @param waittime		in seconds
	 */
	public static void setAlarm(Context c, long waittime) {
		Intent alarmIntent = new Intent(c, DialerReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(c, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarmManager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (waittime * 1000), pendingIntent);
	}
	
}
