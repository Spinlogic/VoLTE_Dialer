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
package at.a1.volte_dialer.dialer;

import net.spinlogic.logger.Logger;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import at.a1.volte_dialer.callmonitor.CallMonitorService;

/**
 * This service is used to trigger outgoing calls when
 * the app is acting as a sender.
 * This service binds to the CallMonitorService to get notifications
 * about outgoing calls.
 * 
 * The main activity binds to this service to communicate changes 
 * in dialer configuration, whether the MSISDN, the duration of calls or
 * the time between calls.
 * 
 * @author Juan Noguera
 *
 */
public class DialerService extends Service implements DsHandlerInterface {
	
	private final String TAG = "DialerService";
	
	static final public String INTENT_ACTION_ALARM = "at.a1.volte_dialer.alarm";
	
	// Messages to this service from the calling activity
	static final public int MSG_NEW_CONFIG 			= 1;
	static final public int MSG_CLIENT_ADDHANDLER 	= 2;
	
	// Messages to the activity from this server
	static final public int MSG_DS_NEWCALLATTEMPT 	= 100;	// used to increase call counter
	static final public int MSG_DS_CALLENDED 		= 101;	// call has been released
	
	// Fields in the binder that the activity passes to this service
	final static public String EXTRA_MSISDN 	= "msisdn";		// dial string
	final static public String EXTRA_DURATION	= "duration";	// in seconds
	final static public String EXTRA_WAITTIME	= "waittime";	// in seconds (time between calls)
	final static public String EXTRA_TMAXSETUP	= "maxcallsetuptime";	// in seconds (maximum call setup time)
	final static public String EXTRA_TAVGSETUP	= "avgcallsetuptime";	// in seconds (avarage call setup time)
	
	// default values
	final static private int DEF_TMAXSETUP	= 30;	// disconnect if the call is not connected within this time
	final static private int DEF_TAVGSETUP	= 10;	// add this value to the call duration if no access to precise call events
	
	// local Call States
	final static private int STATE_IDLE		= 1000;
	final static private int STATE_DIALING	= 1001;
	final static private int STATE_ACTIVE	= 1002;

	final Messenger 	mCmsClient;		// provided by this service to CallMonitorService
	private Messenger 	mCmsServer;		// provided by CallMonitorService to this service
	final Messenger 	mDsServer;		// provided by this service to the calling activity
	private Messenger 	mDsClient;		// provided by the calling activity to this service
		
	private boolean is_inservice;
	private boolean is_system;			// is CallMonitor running as system process
	private boolean is_dismissed;		// is this service being destroyed?
	
	private int				callstate;
	private String 			msisdn;
	private int 			duration;		
	private int 			waittime;
	private int 			maxsetup;
	private int 			avgsetup;
	
	/**
     * Handler of incoming messages from CallMonitorService
     */
    @SuppressLint("HandlerLeak")
	class CmsHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	final String METHOD = "::CmsHandler::handleMessage()  ";
            switch (msg.what) {
                case CallMonitorService.MSG_SERVER_OUTCALL_DIALING:
                	Logger.Log(TAG + METHOD, "MSG_SERVER_OUTCALL_DIALING received from CallMonitorService.");
                	callstate = (is_system) ? STATE_DIALING : STATE_ACTIVE;
                	sendMsg(mDsClient, MSG_DS_NEWCALLATTEMPT, null);
                    break;
                case CallMonitorService.MSG_SERVER_CALL_ACTIVE:	// only received if is_system
                	Logger.Log(TAG + METHOD, "MSG_SERVER_OUTCALL_ACTIVE received from CallMonitorService.");
                		// stop the next call timer
                		stopAlarms();
                		// start the call timer
                		setAlarm((long) duration);
                	break;
                case CallMonitorService.MSG_SERVER_OUTCALL_END:
                	Logger.Log(TAG + METHOD, "MSG_SERVER_OUTCALL_END received from CallMonitorService.");
                	callstate = STATE_IDLE;
                	if(!is_dismissed) {
                		sendMsg(mDsClient, MSG_DS_CALLENDED, null);
                		setAlarm((long) waittime);	// set the timer for the next call
                	}
                    break;
                case CallMonitorService.MSG_SERVER_STATE_INSERVICE:
                	Logger.Log(TAG + METHOD, "MSG_SERVER_STATE_INSERVICE received from CallMonitorService.");
                	if(!is_inservice) {
                		// This is the first message that DialerService gets from CallMonitorService.
                		startDialingLoop();
                	}
                	is_inservice = true;
                    break;
                case CallMonitorService.MSG_SERVER_STATE_OUTSERVICE:
                	Logger.Log(TAG + METHOD, "MSG_SERVER_STATE_OUTSERVICE received from CallMonitorService.");
                	if(is_inservice) {
                		stopDialingLoop();
                	}
                	is_inservice = false;
                    break;
                case CallMonitorService.MSG_SERVER_SYSTEMPROCESS:
                	Logger.Log(TAG + METHOD, "MSG_SERVER_SYSTEMPROCESS received from CallMonitorService.");
                	is_system = true;
                	break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    
    
	/**
     * Handler of incoming messages from client activities.
     */
    @SuppressLint("HandlerLeak")
	class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	final String METHOD = "::IncomingHandler::handleMessage()  ";
            switch (msg.what) {
                case MSG_NEW_CONFIG:
                	Logger.Log(TAG + METHOD, "MSG_NEW_PREFIX received from activity.");
                	Bundle bundle = msg.getData();
                	String newmsisdn = bundle.getString(EXTRA_MSISDN);
                	if(newmsisdn != null) {
                		msisdn = newmsisdn;
                	}
                	Integer newduration = bundle.getInt(EXTRA_DURATION);
                	if(newduration != null) {
                		duration = newduration;
                	}
                	Integer newwt = bundle.getInt(EXTRA_WAITTIME);
                	if(newwt != null) {
                		waittime = newwt;
                	}
                    break;
                case MSG_CLIENT_ADDHANDLER:
                	Logger.Log(TAG + METHOD, "MSG_CLIENT_ADDHANDLER received from activity.");
                	mDsClient = msg.replyTo;
                	break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    
	private ServiceConnection mConnection = new ServiceConnection() {
		
        public void onServiceConnected(ComponentName className, IBinder service) {
        	final String METHOD = "::ServiceConnection::onServiceConnected()  ";
        	mCmsServer = new Messenger(service);
            sendMsg(mCmsServer, CallMonitorService.MSG_CLIENT_ADDHANDLER, mCmsClient);
            Logger.Log(TAG + METHOD, "Bound to CallMonitorService");
        }

        public void onServiceDisconnected(ComponentName className) {
        	final String METHOD = "::ServiceConnection::onServiceDisconnected()  ";
        	mCmsServer = null;
            Logger.Log(TAG + METHOD, "Unbound to CallMonitorService");
        }
    };
	
	public DialerService() {
		callstate		= STATE_IDLE;
		is_system		= false;
		is_inservice 	= false;
		is_dismissed	= false;
		msisdn			= "";
		duration		= 20;		
		waittime		= 20;
		maxsetup		= DEF_TMAXSETUP;
		avgsetup		= DEF_TAVGSETUP;
		
		mCmsServer		= null;
		mCmsClient 		= new Messenger(new CmsHandler());
		mDsClient		= null;
		mDsServer 		= new Messenger(new IncomingHandler());
	}

		
	@Override
	public void onDestroy() {
		final String METHOD = "::onDestroy()  ";
		is_dismissed = true;
		final Context context = this;
		if(callstate != STATE_IDLE) {
			sendMsg(mCmsServer, CallMonitorService.MSG_CLIENT_ENDCALL, null);
		}
		// Give some time to log the last call. In case there was one ongoing
		stopAlarms();
		DialerReceiver.dsIf = null;
		if(mCmsServer != null) {
            unbindService(mConnection);
            Intent monintent = new Intent(context, CallMonitorService.class);
            stopService(monintent);
            mCmsServer = null;
            Logger.Log(TAG + METHOD, "Unbound to CallMonitorService");
        }
		Logger.Log(TAG + METHOD, "service destroyed");
/*		h.postDelayed(new Runnable() {
			public void run() {
				// stop dialing loop (MSG_SERVER_OUTCALL_END hopefully processed)
				stopAlarms();
				DialerReceiver.dsIf = null;
				if(mCmsServer != null) {
//		            unbindService(mConnection);
		            Intent monintent = new Intent(context, CallMonitorService.class);
		            stopService(monintent);
		            mCmsServer = null;
		            Logger.Log(TAG + METHOD, "Unbound to CallMonitorService");
		        }
				Logger.Log(TAG + METHOD, "service destroyed");
			}
		}, 1000); */	
		super.onDestroy();
	}

	
	@Override
    public IBinder onBind(Intent intent) {
		final String METHOD = "::onBind()  ";
		
		if(!intent.hasExtra(EXTRA_MSISDN)	|| 
		   !intent.hasExtra(EXTRA_DURATION)	|| 
		   !intent.hasExtra(EXTRA_WAITTIME)) {
			return null;	// The three parameters must be provided to bind
		}
    	msisdn		= intent.getStringExtra(EXTRA_MSISDN);
    	duration	= intent.getIntExtra(EXTRA_DURATION, 20);
    	waittime	= intent.getIntExtra(EXTRA_WAITTIME, 20);
    	maxsetup	= intent.getIntExtra(EXTRA_TMAXSETUP, DEF_TMAXSETUP);
		avgsetup	= intent.getIntExtra(EXTRA_TAVGSETUP, DEF_TAVGSETUP);
    	
		// start the CallMonitorService
		Intent monintent = new Intent(this, CallMonitorService.class);
		monintent.putExtra(CallMonitorService.EXTRA_OPMODE, CallMonitorService.OPMODE_MO);
		bindService(monintent, mConnection, Context.BIND_AUTO_CREATE);
		Logger.Log(TAG + METHOD, "Binding to CallMonitorService");
		DialerReceiver.dsIf = this;
		return mDsServer.getBinder();
	}
	
	
	private void startDialingLoop() {
		 // Start dialing loop
		dialCall(msisdn);
	}
	
	private void stopDialingLoop() {
		 // Start dialing loop
		stopAlarms();
	}
	
	
	/**
	 * Sends a message to the client via the Messenger object provided 
	 * by the client, if any.
	 * @param what
	 */
	public void sendMsg(Messenger toMsgr, int what, Messenger rplyToMsgr) {
		final String METHOD = "::sendMsg()  ";
		
		Logger.Log(TAG + METHOD, "Sending message to client. What = " + Integer.toString(what));
		Message msg = Message.obtain(null, what, 0, 0);
		if(rplyToMsgr != null) {
			msg.replyTo = rplyToMsgr;
		}
		try {
			toMsgr.send(msg);
			Logger.Log(TAG + METHOD, "Message sent to client.");
		} catch (RemoteException e) {
			Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
		}
	}
	
	
	// INTERFACE DsHandlerInterface 
	
	public boolean dsIf_isCallOngoing() {
		return (callstate != STATE_IDLE) ? true : false;
	}
	
	public void dsIf_endCall() {
		sendMsg(mCmsServer, CallMonitorService.MSG_CLIENT_ENDCALL, null);
	}
	
	/**
	 * sets an Alarm to go off in waittime seconds from current time.
	 * 
	 * @param waittime		in seconds
	 */
	@SuppressLint("NewApi")
	public void setAlarm(long waittime) {
		final String METHOD = "::setAlarm()  ";
		Intent alarmIntent = new Intent(this, DialerReceiver.class);
//		alarmIntent.setAction(INTENT_ACTION_ALARM);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentapiVersion >= android.os.Build.VERSION_CODES.KITKAT){
			alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (waittime * 1000), pendingIntent);
		} else{
			alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (waittime * 1000), pendingIntent);
		}
		Logger.Log(TAG + METHOD, "ALARM will go off in " + Long.toString(waittime * 1000));
	}
	
	
	public void stopAlarms() {
		final String METHOD = "::stopAlarms()  ";
		Intent alarmIntent = new Intent(this, DialerReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(pendingIntent);
		Logger.Log(TAG + METHOD, "All timers have been stopped.");
	}
	
	public void dsIf_dialCall() {
		dialCall(msisdn);
	}
	
	
	public void dialCall(final String telnum) {
		final String METHOD = "::dialCall()  ";
		
		Handler h = new Handler();
		h.postDelayed(new Runnable() {
			public void run() {
				Intent intent = new Intent(Intent.ACTION_CALL);
				intent.setData(Uri.parse("tel:" + telnum));
				intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
				Logger.Log(TAG + METHOD, "Calling " + telnum);
				
				// Activate an alarm to end the call
				if(is_system) {
					setAlarm((long) maxsetup);
				}
				else {
					setAlarm((long) (avgsetup + duration));
				}
				callstate = STATE_DIALING;
			}
		}, 500);	// Works better if the call is trigger after some delay
	
	}	
	
}
