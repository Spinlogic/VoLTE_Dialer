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
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import at.a1.volte_dialer.Globals;
import at.a1.volte_dialer.callmonitor.CallMonitorService;
import at.a1.volte_dialer.phonestate.PhoneStateService;
import at.a1.volte_dialer.phonestate.PreciseCallStateReceiver;

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
public class DialerService extends Service {
	
	private final String TAG = "DialerService";
	
	// Messages to this service from the calling activity
	static final public int MSG_NEW_CONFIG = 1;
	
	// Messages to the activity from this server
	static final public int MSG_NEW_CALLATTEMPT = 100;	// used to increase call counter
	
	// Fields in the binder that the activity passes to this service
	final static public String EXTRA_MSISDN 	= "msisdn";		// dial string
	final static public String EXTRA_DURATION	= "duration";	// in seconds
	final static public String EXTRA_WAITTIME	= "waittime";	// in seconds (time between calls)

	private Messenger mCms;		// provided by CallMonitorService to this service
	final Messenger mClient;	// provided by this service to CallMonitorService
	final Messenger mServer;	// provided by this service to the calling activity
	
	public static DialerHandler hdialer;
	
	private boolean is_inservice;
	
	/**
     * Handler of incoming messages from CallMonitorService
     */
    @SuppressLint("HandlerLeak")
	class CmsHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	final String METHOD = "::CmsHandler::handleMessage()  ";
            switch (msg.what) {
                case CallMonitorService.MSG_SERVER_OUTCALL_START:
                	Log.i(TAG + METHOD, "MSG_SERVER_OUTCALL_START received from CallMonitorService.");
                	// TODO: start call duration timer
                    break;
                case CallMonitorService.MSG_SERVER_OUTCALL_END:
                	Log.i(TAG + METHOD, "MSG_SERVER_OUTCALL_END received from CallMonitorService.");
                	// TODO: start time between calls timer
                    break;
                case CallMonitorService.MSG_SERVER_STATE_INSERVICE:
                	Log.i(TAG + METHOD, "MSG_SERVER_STATE_INSERVICE received from CallMonitorService.");
                	if(!is_inservice) {
                		startDialingLoop();
                	}
                	is_inservice = true;
                    break;
                case CallMonitorService.MSG_SERVER_STATE_OUTSERVICE:
                	Log.i(TAG + METHOD, "MSG_SERVER_STATE_OUTSERVICE received from CallMonitorService.");
                	if(is_inservice) {
                		stopDialingLoop();
                	}
                	is_inservice = false;
                	// TODO: stop hdialer, if started
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
                	Log.i(TAG + METHOD, "MSG_NEW_PREFIX received from activity.");
                	Bundle bundle = msg.getData();
                	String newmsisdn = bundle.getString(EXTRA_MSISDN);
                	if(newmsisdn != null) {
                		hdialer.setMsisdn(newmsisdn);
                	}
                	Integer newduration = bundle.getInt(EXTRA_DURATION);
                	if(newduration != null) {
                		hdialer.setCallDuration(newduration);
                	}
                	Integer newwt = bundle.getInt(EXTRA_WAITTIME);
                	if(newwt != null) {
                		hdialer.setTimeBetweenCalls(newwt);
                	}
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    
	private ServiceConnection mConnection = new ServiceConnection() {
		
        public void onServiceConnected(ComponentName className, IBinder service) {
        	final String METHOD = "::ServiceConnection::onServiceConnected()  ";
        	mCms = new Messenger(service);
            sendMsg(CallMonitorService.MSG_CLIENT_ADDHANDLER, mClient);
            Log.i(TAG + METHOD, "Bound to CallMonitorService");
            if(is_inservice) {
            	startDialingLoop();
            }
            Log.i(TAG + METHOD, "dialing loop started");
        }

        public void onServiceDisconnected(ComponentName className) {
        	final String METHOD = "::ServiceConnection::onServiceDisconnected()  ";
        	mCms = null;
            Log.d(TAG + METHOD, "Unbound to CallMonitorService");
        }
    };
	
	public DialerService() {
		is_inservice 	= false;
		hdialer 		= new DialerHandler();
		mClient 		= new Messenger(new CmsHandler());
		mServer 		= new Messenger(new IncomingHandler());
	}
	
/*	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final String METHOD = "::onStartCommand()  ";
		
		// start the PhoneStateService
		Intent psintent = new Intent(this, PhoneStateService.class);
		startService(psintent);
		// start dialing loop
		hdialer = new DialerHandler();
		hdialer.start(this);
		Log.d(TAG + METHOD, "service started");
		int res = super.onStartCommand(intent, flags, startId);
		return res;
	}  */
		
	@Override
	public void onDestroy() {
		final String METHOD = "::onDestroy()  ";
		final Context context = getApplicationContext();
		if(DialerHandler.isCallOngoing()) {
			if(Globals.is_running_as_system) {
				PreciseCallStateReceiver.hangupCall();
			}
			else {
				Globals.hangupCall(); // Disconnect any call that is still ongoing
			}
		}
		// Give some time to log the last call. In case there was one ongoing
		Handler h = new Handler();
		h.postDelayed(new Runnable() {
			public void run() {
				// stop the PhoneStateService
				Intent psintent = new Intent(context, PhoneStateService.class);
				stopService(psintent);
				// stop dialing loop
				hdialer.stop(context);
				Log.d(TAG + METHOD, "service destroyed");
			}
		}, 1000);
		if(mCms != null) {
            unbindService(mConnection);
            mCms = null;
            Log.i(TAG + METHOD, "Unbound to CallMonitorService");
        }
		Log.i(TAG + METHOD, "service stopped");
		super.onDestroy();
	}

	
	@Override
    public IBinder onBind(Intent intent) {
		final String METHOD = "::onBind()  ";
		
		if(intent.hasExtra(EXTRA_MSISDN)	|| 
		   intent.hasExtra(EXTRA_DURATION)	|| 
		   intent.hasExtra(EXTRA_WAITTIME)) {
			return null;	// The three parameters must be provided to bind
		}
    	hdialer.setMsisdn(intent.getStringExtra(EXTRA_MSISDN));
    	hdialer.setCallDuration(intent.getIntExtra(EXTRA_DURATION, 20));
    	hdialer.setTimeBetweenCalls(intent.getIntExtra(EXTRA_WAITTIME, 20));
		
		// start the CallMonitorService
		Intent monintent = new Intent(this, CallMonitorService.class);
		monintent.putExtra(CallMonitorService.EXTRA_OPMODE, CallMonitorService.OPMODE_MO);
		bindService(monintent, mConnection, Context.BIND_AUTO_CREATE);
		Log.d(TAG + METHOD, "Binding to CallMonitorService");
				
		return mServer.getBinder();
	}
	
	
	private void startDialingLoop() {
		 // Start dialing loop
		hdialer.start(this);
	}
	
	private void stopDialingLoop() {
		 // Start dialing loop
		hdialer.stop(this);
	}
	
	
	/**
	 * Sends a message to the client via the Messenger object provided 
	 * by the client, if any.
	 * @param what
	 */
	public void sendMsg(int what, Messenger messenger) {
		final String METHOD = "::sendMsg()  ";
		
		Log.i(TAG + METHOD, "Sending message to client. What = " + Integer.toString(what));
		Message msg = Message.obtain(null, what, 0, 0);
		if(messenger != null) {
			msg.replyTo = messenger;
		}
		try {
			mCms.send(msg);
			Log.i(TAG + METHOD, "Message sent to client.");
		} catch (RemoteException e) {
			Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
		}
	}
}
