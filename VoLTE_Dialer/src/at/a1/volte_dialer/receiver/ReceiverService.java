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

package at.a1.volte_dialer.receiver;

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
import android.view.KeyEvent;
import at.a1.volte_dialer.Globals;
import at.a1.volte_dialer.callmonitor.CallMonitorService;

/**
 * This service is used to handle incoming calls when
 * the app is acting as a receiver.
 * This service binds to the CallMonitorService to get notifications
 * when a call is received from the specified number.
 * 
 * A right match is done with the number passed. If the length is zero, then all calls
 * are auto-answered.
 * 
 * @author Juan Noguera
 *
 */
public class ReceiverService extends Service {
	
	private final static String TAG = "ReceiverService";
	
	// Messages to this service from the calling activity
	static final public int MSG_NEW_SUFFIX 			= 1;
	static final public int MSG_CLIENT_ADDHANDLER 	= 2;
	

	// Messages from this service to the calling activity
	static final public int MSG_RS_NEWCALLATTEMPT = 50;
	
	// Extras defined for this service
	final static public String EXTRA_SUFFIX = "suffix";
	
	private String suffix;
	
	final Messenger 	mCmsClient;		// provided by this service to CallMonitorService
	private Messenger 	mCmsServer;		// provided by CallMonitorService to this service
	final Messenger 	mRsServer;		// provided by this service to the calling activity
	private Messenger 	mRsClient;		// provided by the calling activity to this service
	
	
	/**
     * Handler of incoming messages from CallMonitorService
     */
    @SuppressLint("HandlerLeak")
	class CmsHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	final String METHOD = "::CmsHandler::handleMessage()  ";
            switch (msg.what) {
                case CallMonitorService.MSG_SERVER_INCOMING_CALL:
                	Log.i(TAG + METHOD, "MSG_SERVER_INCOMING_CALL received from CallMonitorService.");
                	Bundle srv_data = msg.getData();
                	String msisdn = srv_data.getString(CallMonitorService.EXTRA_MTC_MSISDN);
                	Log.i(TAG + METHOD, "MSISDN: " + msisdn);
                	if(msisdn != null) {
                		if(msisdn.endsWith(suffix)) {
                			sendMsg(mRsClient, MSG_RS_NEWCALLATTEMPT, null);
                			answerCall();
                		}
                	}
                	else {
                		Log.i(TAG + METHOD, "ERROR null MISDN received.");
                	}
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
                case MSG_NEW_SUFFIX:
                	Log.i(TAG + METHOD, "MSG_NEW_PREFIX received from activity.");
                	Bundle bsuffix = msg.getData();
                	String newsuffix = bsuffix.getString(EXTRA_SUFFIX);
                	if(newsuffix != null) {
                		suffix = newsuffix;
                	}
                	else {
                		Log.i(TAG + METHOD, "ERROR null suffix received.");
                	}
                    break;
                case MSG_CLIENT_ADDHANDLER:
                	Log.i(TAG + METHOD, "MSG_CLIENT_ADDHANDLER received from activity.");
                	mRsClient = msg.replyTo;
                	break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    
	
	public ReceiverService() {
		suffix		= "";
		mCmsServer	= null;
		mCmsClient 	= new Messenger(new CmsHandler());
		mRsClient	= null;
		mRsServer 	= new Messenger(new IncomingHandler());
	}
	
	
	private ServiceConnection mConnection = new ServiceConnection() {
		
        public void onServiceConnected(ComponentName className, IBinder service) {
        	final String METHOD = "::ServiceConnection::onServiceConnected()  ";
        	mCmsServer = new Messenger(service);
            sendMsg(mCmsServer, CallMonitorService.MSG_CLIENT_ADDHANDLER, mCmsClient);
            Log.i(TAG + METHOD, "Bound to CallMonitorService");
        }

        public void onServiceDisconnected(ComponentName className) {
        	final String METHOD = "::ServiceConnection::onServiceDisconnected()  ";
        	mCmsServer = null;
            Log.d(TAG + METHOD, "Unbound to CallMonitorService");
        }
    };
	
	
/*	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final String METHOD = "::onStartCommand()  ";
		
		if(intent.hasExtra(EXTRA_SUFFIX)) {
			suffix = intent.getStringExtra(EXTRA_SUFFIX);
		}
		
		// start the PhoneStateService
		Intent monintent = new Intent(this, CallMonitorService.class);
		monintent.putExtra(CallMonitorService.EXTRA_OPMODE, CallMonitorService.OPMODE_MT);
		bindService(monintent, mConnection, Context.BIND_AUTO_CREATE);
		Log.d(TAG + METHOD, "Binding to CallMonitorService");
		int res = super.onStartCommand(intent, flags, startId);
		return res;
	}  */
	
	
	@Override
	public void onDestroy() {
		final String METHOD = "::onDestroy()   ";
		super.onDestroy();
		// Disconnect any ongoing call
		if(Globals.is_mtc_ongoing == true) {
			Globals.hangupCall();	// No need to check whether it is in system space.
			Globals.is_mtc_ongoing = false;
		}
		if(mCmsServer != null) {
            unbindService(mConnection);
            Intent monintent = new Intent(this, CallMonitorService.class);
            stopService(monintent);
            mCmsServer = null;
            Log.i(TAG + METHOD, "Unbound to CallMonitorService");
        }
		Log.i(TAG + METHOD, "service stopped");
	}

	
	@Override
    public IBinder onBind(Intent intent) {
		final String METHOD = "::onBind()  ";
		
		if(intent.hasExtra(EXTRA_SUFFIX)) {
			suffix = intent.getStringExtra(EXTRA_SUFFIX);
		}
		
		// start the CallMonitorService
		Intent monintent = new Intent(this, CallMonitorService.class);
		monintent.putExtra(CallMonitorService.EXTRA_OPMODE, CallMonitorService.OPMODE_MT);
		bindService(monintent, mConnection, Context.BIND_AUTO_CREATE);
		Log.d(TAG + METHOD, "Binding to CallMonitorService");
		
		return mRsServer.getBinder();
	}
	
	
	/**
	 * Sends a message to the CallMonitorService.
	 * Includes a reply-to Messenger that the CallMonitorService shall use
	 * to communicate with this service.
	 * 
	 * @param what
	 * @param messenger
	 */
	public void sendMsg(Messenger toMsgr, int what, Messenger rplyToMsgr) {
		final String METHOD = "::sendMsg()  ";
		
		if(toMsgr != null) {
			Log.i(TAG + METHOD, "Sending message to client. What = " + Integer.toString(what));
			Message msg = Message.obtain(null, what, 0, 0);
			if(rplyToMsgr != null) {
				msg.replyTo = rplyToMsgr;
			}
			try {
				toMsgr.send(msg);
				Log.i(TAG + METHOD, "Message sent to client.");
			} catch (RemoteException e) {
				Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
			}
		}
		else {
			Log.d(TAG + METHOD, "ERROR. Null value for toMsgr.");
		}
	}
	
    
	public void answerCall() {
    	final String METHOD = ":answerCall()  ";
    	try {
    		Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
    		i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP,
    	            KeyEvent.KEYCODE_HEADSETHOOK));
    		sendOrderedBroadcast(i, null);
    	} catch(Exception e) {
    		Log.d(TAG + METHOD, "Exception: " + e);
    	}
    }
}
