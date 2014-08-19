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

package at.a1.volte_dialer.callmonitor;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import at.a1.volte_dialer.VD_Settings;


/**
 * This service is used to monitor outgoing calls.
 * The client may include the following extras:
 * 
 * EXTRA_CLIENTMSGR			a binder to send messages back to the client 
 * 
 * @author Juan Noguera
 *
 */
public class CallMonitorService extends Service implements CallMonitorInterface {
	private static final String TAG = "CallMonitorService";
	private final static Logger LOGGER = Logger.getLogger(CallMonitorService.class.getName());
	
	// Extras for the intent create by the client
	final static public String EXTRA_OPMODE 		= "opmode";		
	
	// Messages from clients to this service
	public static final int MSG_CLIENT_ENDCALL 		= 1;	// Disconnect triggered by the client
	public static final int MSG_CLIENT_ADDHANDLER 	= 2;	// Add the messenger to send messages to client
	
	// Messages from this service to clients
	public static final int MSG_SERVER_STATE_INSERVICE	= 0;	// The service state is STATE_IN_SERVICE
	public static final int MSG_SERVER_STATE_OUTSERVICE	= 1;	// The service state is STATE_EMERGENCY_ONLY, STATE_OUT_OF_SERVICE or STATE_POWER_OFF
	public static final int MSG_SERVER_INCOMING_CALL	= 2;
	public static final int MSG_SERVER_OUTCALL_DIALING	= 3;
	public static final int MSG_SERVER_CALL_ACTIVE		= 4;
	public static final int MSG_SERVER_OUTCALL_END		= 5;
	public static final int MSG_SERVER_SYSTEMPROCESS	= 6;	// sent to client if this service is running in the system process
	
	// Call States
	final static public int CALLSTATE_IDLE			= 1000;
	final static public int CALLSTATE_OFFHOOK		= 1001;
	final static public int CALLSTATE_DIALING		= 1002;
	final static public int CALLSTATE_INCOMING		= 1003;
	final static public int CALLSTATE_ALERTING		= 1004;
	final static public int CALLSTATE_RINGING		= 1005;
	final static public int CALLSTATE_ACTIVE		= 1006;
	final static public int CALLSTATE_DISCONNECTING	= 1007;
	final static public int CALLSTATE_DISCONNECTED	= 1008;
	final static public int CALLSTATE_HOLDING		= 1009;
	final static public int CALLSTATE_WAITING		= 1010;
	
	
	// Operation modes
	public static final int OPMODE_BG = 100;		// Background
	public static final int OPMODE_MT = 101;		// Receiver
	public static final int OPMODE_MO = 102;		// Sender
	
	// Extras for MSG_SERVER_INCOMING_CALL sent to client
	final static public String EXTRA_MTC_MSISDN = "msisdn";
	
	private boolean 					is_system;
	private boolean						is_client_diconnect;
	private boolean						is_oncall;	// call being dialed, active or ringing (incoming)
	private int							opmode;
	private int							signalstrength;
	private CallMonitorReceiver 		mCallMonitorReceiver;
	private OutgoingCallReceiver 		mOutgoingCallReceiver;
	private CallDescription				mCallDescription;
	private PreciseCallStateReceiver	mPcsr;
	
	private Messenger mClient;		// provided by client to service (at most one client can be bound)
	final Messenger mService;		// provided by service to client
	
	/**
     * Handler of incoming messages from clients.
     */
    @SuppressLint("HandlerLeak")
	class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	final String METHOD = "::IncomingHandler::handleMessage()  ";
            switch (msg.what) {
                case MSG_CLIENT_ENDCALL:
                	is_client_diconnect = true;
                	hangupCall();
//                	Logger.Log(TAG + METHOD, "MSG_CLIENT_ENDCALL received from client.");
                	LOGGER.info(TAG + METHOD + "MSG_CLIENT_ENDCALL received from client.");
                    break;
                case MSG_CLIENT_ADDHANDLER:
//                	Logger.Log(TAG + METHOD, "MSG_CLIENT_ADDHANDLER received from client.");
                	LOGGER.info(TAG + METHOD + "MSG_CLIENT_ADDHANDLER received from client.");
                	mClient = msg.replyTo;
                	if(is_system) {
                		sendMsg(MSG_SERVER_SYSTEMPROCESS, null);
                	}
                	activateReceivers();	// when bounding, we activate receivers here
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

	public CallMonitorService() {
		is_system				= false;
		is_client_diconnect		= false;
		is_oncall				= false;
		mCallMonitorReceiver	= null;
		mOutgoingCallReceiver	= null;
		mCallDescription		= null;
		mClient					= null;
		opmode					= OPMODE_BG;
		signalstrength			= 99;	// = Unknown. Values in 3GPP TS27.007 
		mService 				= new Messenger(new IncomingHandler());
		mPcsr 					= null;
		CallLogger.initializeValues();		// init logging
	}

	/**
	 * This is only called when the service is created to operate
	 * in background mode. For sender and receiver modes, onBind is
	 * called instead. 
	 * 
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final String METHOD = "::onStartCommand()  ";
		
		LOGGER.setLevel(Level.INFO);
//		Logger.Log(TAG + METHOD, "Starting service.");
		LOGGER.info(TAG + METHOD + "Starting service.");
		Bundle extras = intent.getExtras();
		if(extras != null) {
//			if(extras.containsKey(EXTRA_CLIENTMSGR)) {
//				mClient = new Messenger(extras.getBinder(EXTRA_CLIENTMSGR));
//			}
			if(extras.containsKey(EXTRA_OPMODE)) {
				opmode = extras.getInt(EXTRA_OPMODE);
			}
		}
		is_system	= isRunningAsSystem();
		activateReceivers();
	    
//		Logger.Log(TAG + METHOD, "Service started.");
		LOGGER.info(TAG + METHOD + "Service started.");
		int res = super.onStartCommand(intent, flags, startId);
		return res;
	}
		
	@Override
	public void onDestroy() {
		final String METHOD = "::onDestroy()  ";
		// Disconnect the ongoing call if the service
		// is not running in background
		if(is_oncall && opmode != OPMODE_BG) {
			hangupCall();
		}
		deactivateReceivers();
		mClient = null;
//		Logger.Log(TAG + METHOD, " Call info receivers stopped.");
		LOGGER.info(TAG + METHOD + " Call info receivers stopped.");
//		Logger.Log(TAG + METHOD, " Service destroyed.");
		LOGGER.info(TAG + METHOD + " Service destroyed.");
		super.onDestroy();
	}

	@Override
    public IBinder onBind(Intent intent) {
		final String METHOD = "::onBind()  ";
		
		LOGGER.setLevel(Level.INFO);
		// Only one client can be bound to this service at any given time
		if(mClient != null) {
			return null;
		}
		
		Bundle extras = intent.getExtras();
		if(extras != null) {
			if(extras.containsKey(EXTRA_OPMODE)) {
				opmode = extras.getInt(EXTRA_OPMODE);
			}
		}
		is_system	= isRunningAsSystem();
//		Logger.Log(TAG + METHOD, " binding to client.");
		LOGGER.info(TAG + METHOD + " binding to client.");
		return mService.getBinder();
	}
	
	// Methods for CallMonitorReceiver and OutgoingCallReceiver to report events
	
	/**
	 * Records the start of a call.
	 * If the call is incoming (MT), then it is communicated to the 
	 * bound client.
	 * 
	 * @param direction		direction of the call
	 * @param mt_msisdn		incoming number (for MT calls)
	 */
	public void startCall(String direction, String mt_msisdn) {
		final String METHOD = "::startCall()  ";
		is_oncall = true;
//		Logger.Log(TAG + METHOD, " Starting call. Direction = " + direction);
		LOGGER.info(TAG + METHOD + " Starting call. Direction = " + direction);
		if(mCallDescription == null) {
			mCallDescription = new CallDescription(this, direction, signalstrength);
			if(mt_msisdn != null && !mt_msisdn.isEmpty()) {
				int plength = mt_msisdn.length();
				plength = (plength > 8) ? 8 : plength;	// prefix is, at most, 8 char long	
				mCallDescription.setPrefix(mt_msisdn.substring(0, plength));
			}
		}
		// Notify incoming call if 
		if(opmode == OPMODE_MT) {
			if(mt_msisdn != null && !mt_msisdn.isEmpty()) {
				Bundle bundle = new Bundle();
				bundle.putString(EXTRA_MTC_MSISDN, mt_msisdn);
				sendMsg(MSG_SERVER_INCOMING_CALL, bundle);
			}
		}
	}
	
	/**
	 * Records the end of a call
	 * The call info is written to the call log.
	 * 
	 * If the disconnection is triggered by the client hanging up, then the 
	 * client must communicate this to this service before actually hanging up. 
	 */
	public void endCall() {
		final String METHOD = "::endCall()  ";
//		Logger.Log(TAG + METHOD, " Ending call.");
		LOGGER.info(TAG + METHOD + " Ending call.");
		String side = (opmode == OPMODE_BG) ? 
						CallDescription.CALL_DISCONNECTED_BY_UNK : 
						CallDescription.CALL_DISCONNECTED_BY_NW;
		if(is_client_diconnect) {	// will not be set in background mode
			side = CallDescription.CALL_DISCONNECTED_BY_UE;
			is_client_diconnect = false;
		}
		if(mCallDescription != null) {
			mCallDescription.endCall(side, signalstrength);
			mCallDescription.writeCallInfoToLog();
			mCallDescription = null;	// no needed any more
		}
		if(opmode == OPMODE_MO && is_oncall) {
			sendMsg(MSG_SERVER_OUTCALL_END, null);
		}
		is_oncall = false;
//		Logger.Log(TAG + METHOD, " Call terminated.");
		LOGGER.info(TAG + METHOD + " Call terminated.");
	}
	
	/**
	 * Notifies the number for an outgoing call.
	 * 
	 * @param msisdn
	 */
	public void moCallNotif(String bpty) {
		final String METHOD = "::moCallNotif()  ";
//		Logger.Log(TAG + METHOD, " MO calls to " + bpty);
		LOGGER.info(TAG + METHOD + " MO calls to " + bpty);
		is_oncall = true;
		boolean ismocallongoing = true; // in case the receiver sends multiple CALLSTATE_DIALING
		if(mCallDescription == null) {
			ismocallongoing = false;
			mCallDescription = new CallDescription(this, CallDescription.MO_CALL, signalstrength);
		} 
		if(!ismocallongoing) {
			if(bpty != null && !bpty.isEmpty()) {
				int plength = bpty.length();
				plength = (plength > 8) ? 8 : plength;	// prefix is, at most, 6 char long	
				mCallDescription.setPrefix(bpty.substring(0, plength));
			}
//			mCallDescription.setDirection(CallDescription.MO_CALL);
			if(opmode == OPMODE_MO) {
				sendMsg(MSG_SERVER_OUTCALL_DIALING, null);
			}
		}
	}
	
	/**
	 * Sends a message to the client via the Messenger object provided 
	 * by the client, if any.
	 * @param what
	 */
	public void sendMsg(int what, Bundle bundle) {
		final String METHOD = "::sendMsg()  ";
		
		if(mClient != null) {
//			Logger.Log(TAG + METHOD, "Sending message to client. What = " + Integer.toString(what));
			LOGGER.info(TAG + METHOD + "Sending message to client. What = " + Integer.toString(what));
			Message msg = Message.obtain(null, what, 0, 0);
			if(bundle != null) {
				msg.setData(bundle);
			}
			try {
				mClient.send(msg);
//				Logger.Log(TAG + METHOD, "Message sent to client.");
				LOGGER.info(TAG + METHOD + "Message sent to client.");
			} catch (RemoteException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			}
		}
	}
	
	
	private void activateReceivers() {
		final String METHOD = "::activateReceivers()  ";
		
//		Logger.Log(TAG + METHOD, "Activating receivers");
		LOGGER.info(TAG + METHOD + "Activating receivers");
		mCallMonitorReceiver = new CallMonitorReceiver(this);
		TelephonyManager telMng = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		
		int mflags = PhoneStateListener.LISTEN_SIGNAL_STRENGTHS;
		if(opmode != OPMODE_MT) {
			mflags |= PhoneStateListener.LISTEN_SERVICE_STATE;
		}
		if(!is_system) {
			mflags |= PhoneStateListener.LISTEN_CALL_STATE;
		}
		else {
			mPcsr = new PreciseCallStateReceiver(this, this, opmode);
			mPcsr.listen();
//			Logger.Log(TAG + METHOD, "PreciseCallStateReceiver activated");
			LOGGER.info(TAG + METHOD + "PreciseCallStateReceiver activated");
		}
		telMng.listen(mCallMonitorReceiver, mflags);
//		Logger.Log(TAG + METHOD, "CallMonitorReceiver activated");
		LOGGER.info(TAG + METHOD + "CallMonitorReceiver activated");
		
		if(opmode != OPMODE_MT && !is_system) {	// not needed in receiver mode or if running as system
			mOutgoingCallReceiver = new OutgoingCallReceiver(this);
			// To get number for outgoing calls
			IntentFilter mocFilter = new IntentFilter(Intent.ACTION_NEW_OUTGOING_CALL);
			registerReceiver(mOutgoingCallReceiver, mocFilter);
//			Logger.Log(TAG + METHOD, "OutgoingCallReceiver activated");
			LOGGER.info(TAG + METHOD + "OutgoingCallReceiver activated");
		}
	}
	
	
	private void deactivateReceivers() {
		final String METHOD = "::deactivateReceivers()  ";
		
//		Logger.Log(TAG + METHOD, "Deactivating receivers");
		LOGGER.info(TAG + METHOD + "Deactivating receivers");
		TelephonyManager telMng = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		telMng.listen(mCallMonitorReceiver, PhoneStateListener.LISTEN_NONE);
//		Logger.Log(TAG + METHOD, "mCallMonitorReceiver deactivated");
		LOGGER.info(TAG + METHOD + "mCallMonitorReceiver deactivated");
		if(mOutgoingCallReceiver != null) {
			unregisterReceiver(mOutgoingCallReceiver);
			mOutgoingCallReceiver = null;
//			Logger.Log(TAG + METHOD, "OutgoingCallReceiver deactivated");
			LOGGER.info(TAG + METHOD + "OutgoingCallReceiver deactivated");
		}
		if(mPcsr != null) {
			mPcsr.stop();
			mPcsr = null;
//			Logger.Log(TAG + METHOD, "PreciseCallStateReceiver deactivated");
			LOGGER.info(TAG + METHOD + "PreciseCallStateReceiver deactivated");
		}
	}
	
	
	/**
     * Determines whether the app is running in system or user space
     * 
     * @return	true	App is running in system space
     * 			false	App is running in user space
     */
    private boolean isRunningAsSystem() {
    	int uid_radio = android.os.Process.getUidForName("radio");
    	int uid_system = android.os.Process.getUidForName("system");
    	int uid_root = android.os.Process.getUidForName("root");
    	int myuid = android.os.Process.myUid();
    	return (myuid == uid_radio || myuid == uid_system || myuid == uid_root) ? true : false;
    }
    
    
    private void hangupCall() {
    	if(is_system) {
    		hangupCallHard();
    	}
    	else {
    		hangupCallSoft();
    	}
    }
    
    
	/**
	 * Uses reflection to hang an active call up
	 */
	private void hangupCallSoft(){
		final String METHOD = ":hangupCallSoft()  ";
		try {
	        //String serviceManagerName = "android.os.IServiceManager";
	        String serviceManagerName = "android.os.ServiceManager";
	        String serviceManagerNativeName = "android.os.ServiceManagerNative";
	        String telephonyName = "com.android.internal.telephony.ITelephony";

	        Class telephonyClass;
	        Class telephonyStubClass;
	        Class serviceManagerClass;
	        Class serviceManagerNativeClass;
	        Class serviceManagerNativeStubClass;

	        //	Method telephonyCall;
	        Method telephonyEndCall;
	        //	Method telephonyAnswerCall;
	        Method getDefault;

	        // Method getService;
	        Object telephonyObject;
	        Object serviceManagerObject;

	        telephonyClass = Class.forName(telephonyName);
	        telephonyStubClass = telephonyClass.getClasses()[0];
	        serviceManagerClass = Class.forName(serviceManagerName);
	        serviceManagerNativeClass = Class.forName(serviceManagerNativeName);

	        Method getService = // getDefaults[29];
	                serviceManagerClass.getMethod("getService", String.class);

	        Method tempInterfaceMethod = serviceManagerNativeClass.getMethod(
	                					"asInterface", IBinder.class);

	        Binder tmpBinder = new Binder();
	        tmpBinder.attachInterface(null, "fake");

	        serviceManagerObject = tempInterfaceMethod.invoke(null, tmpBinder);
	        IBinder retbinder = (IBinder) getService.invoke(serviceManagerObject, "phone");
	        Method serviceMethod = telephonyStubClass.getMethod("asInterface", IBinder.class);

	        telephonyObject = serviceMethod.invoke(null, retbinder);
	        //telephonyCall = telephonyClass.getMethod("call", String.class);
	        telephonyEndCall = telephonyClass.getMethod("endCall");
	        //telephonyAnswerCall = telephonyClass.getMethod("answerRingingCall");

	        telephonyEndCall.invoke(telephonyObject);
//	        Logger.Log(TAG + METHOD, "Call disconnected.");
	        LOGGER.info(TAG + METHOD + "Call disconnected.");

	    } catch (Exception e) {
//			Logger.Log(TAG + METHOD, "Exception: " + e.getMessage());
			LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
	    }
	}
	
	
	/**
	 * Hangs the call up using the mPhone instance retrieved for 
	 * precise call state monitoring.
	 */
	private void hangupCallHard() {
		final String METHOD = ":hangupCallHard()  ";
		mPcsr.hangupCall();
//		Logger.Log(TAG + METHOD, "Call disconnected.");
		LOGGER.info(TAG + METHOD + "Call disconnected.");
	}
	
	
	// INTERFACE CallMonitorInterface
	
	public void csmif_ServiceState(final int what) {
		final String METHOD = ":csmif_ServiceState()  ";
//		Logger.Log(TAG + METHOD, "Service state = " + Integer.toString(what));
		LOGGER.info(TAG + METHOD + "Service state = " + Integer.toString(what));
		sendMsg(what, null);
	}
	
	
	public void csmif_CallState(final int state, final String extra) {
		final String METHOD = ":csmif_CallState()  ";
		
		switch(state) {
			case CALLSTATE_IDLE:
//				Logger.Log(TAG + METHOD, "Call state = IDLE");
				LOGGER.info(TAG + METHOD + "Call state = IDLE");
				if(extra != null && !extra.isEmpty() && mCallDescription != null) {
					mCallDescription.setDisconnectionCause(extra);
				}
				endCall();
				break;
			case CALLSTATE_OFFHOOK:
//				Logger.Log(TAG + METHOD, "Call state = OFFHOOK");
				LOGGER.info(TAG + METHOD + "Call state = OFFHOOK");
				startCall(CallDescription.MO_CALL, extra);
				break;
			case CALLSTATE_DIALING:
//				Logger.Log(TAG + METHOD, "Call state = DIALING");
				LOGGER.info(TAG + METHOD + "Call state = DIALING");
				moCallNotif(extra);
				break;
			case CALLSTATE_INCOMING:
//				Logger.Log(TAG + METHOD, "Call state = INCOMING");
				LOGGER.info(TAG + METHOD + "Call state = INCOMING");
				startCall(CallDescription.MT_CALL, extra);
				break;
			case CALLSTATE_ALERTING:
//				Logger.Log(TAG + METHOD, "Call state = ALERTING");
				LOGGER.info(TAG + METHOD + "Call state = ALERTING");
				if(mCallDescription != null) {
					mCallDescription.setAlertingTime();
				}
				break;
			case CALLSTATE_RINGING:
//				Logger.Log(TAG + METHOD, "Call state = RINGING");
				LOGGER.info(TAG + METHOD + "Call state = RINGING");
				startCall(CallDescription.MT_CALL, extra);
				break;
			case CALLSTATE_ACTIVE:
//				Logger.Log(TAG + METHOD, "Call state = ACTIVE");
				LOGGER.info(TAG + METHOD + "Call state = ACTIVE");
				if(mCallDescription != null) {
					mCallDescription.setActiveTime();
				}
				sendMsg(MSG_SERVER_CALL_ACTIVE, null);
				break;
			case CALLSTATE_DISCONNECTING:
//				Logger.Log(TAG + METHOD, "Call state = DISCONNECTING");
				LOGGER.info(TAG + METHOD + "Call state = DISCONNECTING");
				// extra may contain a disconnection cause
				if(extra != null && !extra.isEmpty() && mCallDescription != null) {
					mCallDescription.setDisconnectionCause(extra);
				}
				break;
			case CALLSTATE_DISCONNECTED:
//				Logger.Log(TAG + METHOD, "Call state = DISCONNECTED");
				LOGGER.info(TAG + METHOD + "Call state = DISCONNECTED");
				// extra may contain a disconnection cause
				if(extra != null && !extra.isEmpty() && mCallDescription != null) {
					mCallDescription.setDisconnectionCause(extra);
				}
				break;
			case CALLSTATE_HOLDING:
//				Logger.Log(TAG + METHOD, "Call state = HOLDING");
				LOGGER.info(TAG + METHOD + "Call state = HOLDING");
				break;
			case CALLSTATE_WAITING:
//				Logger.Log(TAG + METHOD, "Call state = WAITING");
				LOGGER.info(TAG + METHOD + "Call state = WAITING");
				break;
		}
	}
	
	
	public void csmif_SrvccEvent() {
		mCallDescription.setSrvccTime();
	}
	
	
	public void csmif_SignalStrength(int strength) {
		signalstrength = strength;
	}
	
	
	public boolean csmif_isCallOngoing() {
		return is_oncall;
	}
	
}
