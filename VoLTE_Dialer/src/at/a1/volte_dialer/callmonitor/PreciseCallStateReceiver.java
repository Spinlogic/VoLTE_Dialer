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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.spinlogic.logger.SP_Logger;
import android.content.Context;
import android.os.Handler;
import android.os.Message;


/**
 * This class can be used only on rooted devices. The app must be running in system space.
 * This class is used to:
 * 	- get detail call states, such as actual call connected.
 *  - get the disconnect cause
 *  - get SRVCC events
 * 
 * @author Juan Noguera
 *
 */
public class PreciseCallStateReceiver {
	private static final String TAG = "PreciseCallStateReceiver";
	private final static Logger LOGGER = Logger.getLogger(SP_Logger.LOGGER_NAME);
	
	private static final int EVENT_PRECISE_CALL_STATE_CHANGED 	= 101;
	private static final int EVENT_NEW_RINGING_CONNECTION 		= 102;
	private static final int EVENT_IMS_SRVCC_HANDOVER 			= 140;
	
	private PreciseCallEventsHandler mHandler;
//	private static Object mPhone;		// Default phone instance retrieved from PhoneFactory
//	private static Object mFgCall;		// Foreground call singleton
	
	private static Context context;
	private static CallMonitorInterface mCmIf;
	private int op_mode;
	private static String currentFgCallState;	// current call state for the foreground call
	private static String currentRgCallState;	// Current call state for the ringing call
	private static String currentBgCallState;	// Current call state for the background call
	
	public PreciseCallStateReceiver(Context c, CallMonitorInterface cmif, int opmode) {
		context 		= c;
		mCmIf			= cmif;
		op_mode			= opmode;
		currentFgCallState = "IDLE";
		currentRgCallState = "IDLE";
		currentBgCallState = "IDLE";
		mHandler		= new PreciseCallEventsHandler(this);
		LOGGER.setLevel(Level.INFO);
	}
	
	public void listen() {
		final String METHOD = "::listen()   ";
		Object mPhone 	= getPhoneInstance();
//		mFgCall	= getForegroundCallSingleton(mPhone);
		registerForDetailedCallEvents(mPhone);
		registerForSrvccEvent(mPhone);
		if(op_mode != CallMonitorService.OPMODE_MO) {
			registerForIncomingCallRinging(mPhone);
		}
	}
	
	public void stop() {
		Object mPhone;
		mPhone = getPhoneInstance();
		unregisterForDetailedCallEvents(mPhone);
		unregisterForSrvccEvent(mPhone);
		if(op_mode != CallMonitorService.OPMODE_MO) {
			unregisterForIncomingCallRinging(mPhone);
		}
		mPhone = null;
		context = null;
		mCmIf	= null;
	}
	
	
    /**
     * Handler of incoming messages from clients.
     */
	private static class PreciseCallEventsHandler extends Handler {
		
		PreciseCallStateReceiver mPreciseCallStateReceiver;
		
		public PreciseCallEventsHandler(PreciseCallStateReceiver preciseCallStateReceiver) {
			mPreciseCallStateReceiver = preciseCallStateReceiver;
		}
	   
        @Override
        public void handleMessage(Message msg) {
        	final String METHOD = "::PreciseCallEventsHandler::handleMessage()  ";
//        	Logger.Log(TAG + METHOD, "  Message: " + Integer.toString(msg.what));
        	LOGGER.info(TAG + METHOD + "  Message: " + Integer.toString(msg.what));
            switch (msg.what) {
                case EVENT_PRECISE_CALL_STATE_CHANGED:
                	processPrecisseCallEvent(msg.obj);
                    break;
                case EVENT_NEW_RINGING_CONNECTION:
					String msisdn = getNumberForIncomingCall(msg.obj);
					mCmIf.csmif_CallState(CallMonitorService.CALLSTATE_RINGING, msisdn);
					currentRgCallState = "INCOMING";
                    break;
                case EVENT_IMS_SRVCC_HANDOVER:
                	mCmIf.csmif_SrvccEvent();
                	Object mPhone = getPhoneFromAsyncResult(msg.obj);
                	mPreciseCallStateReceiver.unregisterForDetailedCallEvents(mPhone);
                	mPreciseCallStateReceiver.unregisterForSrvccEvent(mPhone);
					mPreciseCallStateReceiver.registerForDetailedCallEvents(mPhone);
//					mPreciseCallStateReceiver.registerForSrvccEvent(mPhone);	// Currently, SRVCC can only happen once
                	break;
                default:
                    super.handleMessage(msg);
            }
        }
        
   
        
        /**
         * Monitors precise call states.
         * This is complicate because it receives call state events for all the call
         * singletons (Foreground, Background and Ringing).
         * In theory, the AsyncResult object inside Message.obj contains a Connection
         * object that can be use to check which call singleton this event refers to.
         * In reality, AsyncResult.result contains an object gsmPhone.
         * 
         * We need to monitor changes in state for the ringing call and the foreground
         * call singletons.
         * 
         */
        private void processPrecisseCallEvent(Object asyncresult) {
        	final String METHOD = "::PreciseCallEventsHandler::processPrecisseCallEvent()  ";
        	try {
        		String[] newcallstates = getNewCallStates(asyncresult);
        		String new_rgcall_state = newcallstates[0];
        		String new_fgcall_state = newcallstates[1];
        		String new_bgcall_state = newcallstates[2];
        		
//        		String new_fgcall_state = getCallState(mFgCall);
//        		String new_rgcall_state = getCallState(mRgCall);
//        		String new_bgcall_state = getCallState(mBgCall);
        		
//        		Logger.Log(TAG + METHOD, "\n\tCurrent FG = " + currentFgCallState + 
//        							"    RG = " + currentRgCallState +
//        							"    BG = " + currentBgCallState +
//        							"\n\tNew FG = " + new_fgcall_state +
//        							"    RG = " + new_rgcall_state + 
//        							"    BG = " + new_bgcall_state);
        		LOGGER.info(TAG + METHOD + "\n\tCurrent FG = " + currentFgCallState + 
									"    RG = " + currentRgCallState +
									"    BG = " + currentBgCallState +
									"\n\tNew FG = " + new_fgcall_state +
									"    RG = " + new_rgcall_state + 
									"    BG = " + new_bgcall_state);
        		
        		if(new_bgcall_state != currentBgCallState) {	
        			// TODO: report status for background calls
        			currentBgCallState = new_bgcall_state;
        		}
        		// change of state in foreground call is the first to be
        		// evaluated.
        		if(new_fgcall_state != currentFgCallState) { 
					currentFgCallState = new_fgcall_state;
					
					// We need to retrieve enum Call.State
//					Class<?> cCallState = Class.forName("com.android.internal.telephony.Call$State");
//					Object eCallStates[] = cCallState.getEnumConstants();
					
					if(currentFgCallState == "IDLE") { // IDLE
						mCmIf.csmif_CallState(CallMonitorService.CALLSTATE_IDLE, null);
//						Logger.Log(TAG + METHOD, "Call State = IDLE");
						LOGGER.info(TAG + METHOD + "Call State = IDLE");
					} else if(currentFgCallState == "ACTIVE") { // ACTIVE
						mCmIf.csmif_CallState(CallMonitorService.CALLSTATE_ACTIVE, null);
//						Logger.Log(TAG + METHOD, "Call State = ACTIVE");
						LOGGER.info(TAG + METHOD + "Call State = ACTIVE");
					} else if (currentFgCallState == "HOLDING") { // HOLDING
						mCmIf.csmif_CallState(CallMonitorService.CALLSTATE_HOLDING, null);
//						Logger.Log(TAG + METHOD, "Call State = HOLDING");
						LOGGER.info(TAG + METHOD + "Call State = HOLDING");
					} else if (currentFgCallState == "DIALING") { // DIALING
						String msisdn = getNumberForOutgoingCall(asyncresult);
						mCmIf.csmif_CallState(CallMonitorService.CALLSTATE_DIALING, msisdn);
//						Logger.Log(TAG + METHOD, "Call State = DIALING");
						LOGGER.info(TAG + METHOD + "Call State = DIALING");
					} else if (currentFgCallState == "ALERTING") { // ALERTING
						mCmIf.csmif_CallState(CallMonitorService.CALLSTATE_ALERTING, null);
//						Logger.Log(TAG + METHOD, "Call State = ALERTING");
						LOGGER.info(TAG + METHOD + "Call State = ALERTING");
					} else if (currentFgCallState == "INCOMING") { // INCOMING
						// Never called by the Galaxy S5 (seems to affect only the ringing call)
	//					String msisdn = getNumber(cCall, oCall);
	//					mCmIf.csmif_CallState(CallMonitorService.CALLSTATE_INCOMING, msisdn);
//						Logger.Log(TAG + METHOD, "Call State = INCOMING");
						LOGGER.info(TAG + METHOD + "Call State = INCOMING");
					} else if (currentFgCallState == "WAITING") { // WAITING
						mCmIf.csmif_CallState(CallMonitorService.CALLSTATE_WAITING, null);
//						Logger.Log(TAG + METHOD, "Call State = WAITING");
						LOGGER.info(TAG + METHOD + "Call State = WAITING");
					} else if (currentFgCallState == "DISCONNECTED") { // DISCONNECTED
						String disconnectioncause = getDisconnectCause(asyncresult);
						mCmIf.csmif_CallState(CallMonitorService.CALLSTATE_DISCONNECTED, disconnectioncause);
//						Logger.Log(TAG + METHOD, "Call State = DISCONNECTED");
						LOGGER.info(TAG + METHOD + "Call State = DISCONNECTED");
					} else if (currentFgCallState == "DISCONNECTING") { // DISCONNECTING
						String disconnectioncause = getDisconnectCause(asyncresult);
						mCmIf.csmif_CallState(CallMonitorService.CALLSTATE_DISCONNECTING, disconnectioncause);
//						Logger.Log(TAG + METHOD, "Call State = DISCONNECTING");
						LOGGER.info(TAG + METHOD + "Call State = DISCONNECTING");
					}
        		} else if(new_rgcall_state != currentRgCallState) {
        			// Change of state of ringing call is evaluated next
        			if(new_rgcall_state == "DISCONNECTED" || 
             			   new_rgcall_state == "DISCONNECTING") {	// has been cancelled by NW
             				String disconnectioncause = getDisconnectCauseForIncomingCall(asyncresult);
 //            				Logger.Log(TAG + METHOD, "Disconnection cause for Ringing call = " + disconnectioncause);
             				LOGGER.info(TAG + METHOD + "Disconnection cause for Ringing call = " + disconnectioncause);
             				mCmIf.csmif_CallState(CallMonitorService.CALLSTATE_DISCONNECTED, disconnectioncause);
             			} else if(new_rgcall_state == "IDLE" && 
             					  (currentRgCallState == "DISCONNECTED" || 
             					  currentRgCallState == "DISCONNECTING")) {
             				mCmIf.csmif_CallState(CallMonitorService.CALLSTATE_IDLE, null);
             			} else if(new_rgcall_state == "IDLE" && currentRgCallState == "INCOMING") {
             				String disconnectioncause = getDisconnectCauseForIncomingCall(asyncresult);
             				mCmIf.csmif_CallState(CallMonitorService.CALLSTATE_IDLE, disconnectioncause);
             			}
             			currentRgCallState = new_rgcall_state;
             		} 
        	} catch (NoSuchMethodException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (IllegalAccessException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (IllegalArgumentException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (InvocationTargetException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (NoSuchFieldException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (ClassNotFoundException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (Exception e) { // Any other exception. For debugging purposes
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			}
    	}
   
        
        
        private Object getPhoneFromAsyncResult(Object asyncresult) {
        	final String METHOD = "::PreciseCallEventsHandler::getPhoneFromAsyncResult()  ";
        	Object oGsmPhone = null;
			try {
				Field fResult;
				Class<?> cAsyncResult = asyncresult.getClass();
				fResult = cAsyncResult.getDeclaredField("result");
				oGsmPhone = fResult.get(asyncresult);
			} catch (NoSuchFieldException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (IllegalAccessException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (IllegalArgumentException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			}
        	return oGsmPhone;
        }
        
        /**
         * Gets the state for the three call 
         * @param asyncresult
         * @return
         * @throws NoSuchFieldException
         * @throws IllegalAccessException
         * @throws IllegalArgumentException
         * @throws NoSuchMethodException
         * @throws InvocationTargetException
         * @throws ClassNotFoundException 
         */
        private String[] getNewCallStates(Object asyncresult) throws NoSuchFieldException, 
        														  	 IllegalAccessException, 
        														  	 IllegalArgumentException, 
        														  	 NoSuchMethodException, 
        														  	 InvocationTargetException, 
        														  	 ClassNotFoundException {
        	final String METHOD = "::PreciseCallEventsHandler::getNewCallState()  ";
        	
        	String result[] = {"", "", ""};
        	Class<?> cAsyncResult = asyncresult.getClass();
    	    Field fResult = cAsyncResult.getDeclaredField("result");
    	    Object oGsmPhone = fResult.get(asyncresult);
    	    Class<?> cGsmPhone = oGsmPhone.getClass();
    	    Method mGetRingingCall = cGsmPhone.getDeclaredMethod("getRingingCall", (Class[]) null);
    	    Method mGetForegroundCall = cGsmPhone.getDeclaredMethod("getForegroundCall", (Class[]) null);
    	    Method mGetBackgroundCall = cGsmPhone.getDeclaredMethod("getBackgroundCall", (Class[]) null);
    	    Object oRgGsmCall = mGetRingingCall.invoke(oGsmPhone);
    	    Object oFgGsmCall = mGetForegroundCall.invoke(oGsmPhone);
    	    Object oBgGsmCall = mGetBackgroundCall.invoke(oGsmPhone);
    	    Class<?> cGsmCall = Class.forName("com.android.internal.telephony.Call");
    	    Method mGetState = cGsmCall.getDeclaredMethod("getState", (Class[]) null);
    	    Object oRgState = mGetState.invoke(oRgGsmCall);
    	    Object oFgState = mGetState.invoke(oFgGsmCall);
    	    Object oBgState = mGetState.invoke(oBgGsmCall);
    	    if(oRgState != null) {
    	    	result[0] = oRgState.toString();
    	    }
    	    if(oFgState != null) {
    	    	result[1] = oFgState.toString();
    	    }
    	    if(oBgState != null) {
    	    	result[2] = oBgState.toString();
    	    }
    	    return result;
        }
        
        
 /*       private String getCallState(Object call) throws NoSuchMethodException, 
        												IllegalAccessException, 
        												IllegalArgumentException, 
        												InvocationTargetException {
        	final String METHOD = "::PreciseCallEventsHandler::getCallState()  ";
        	
        	Class<?>  cCall = call.getClass();
			Method mGetState = cCall.getMethod("getState", (Class[]) null);
			Object oCallstate = mGetState.invoke(mFgCall);
			Logger.Log(TAG + METHOD, "call state obtained");
			return oCallstate.toString();
        } */

        
        private String getNumberForIncomingCall(final Object asyncresult) {
        	final String METHOD = "::PreciseCallEventsHandler::getNumberForIncomingCall()  ";
        	String telnum = "";
        	
			try {
				Class<?> cAsyncResult = asyncresult.getClass();
	    	    Field fResult = cAsyncResult.getDeclaredField("result");
	    	    Object oConnection = fResult.get(asyncresult);
	    	    Class<?> cConnection = oConnection.getClass();
				Method mGetAddress = cConnection.getDeclaredMethod("getAddress", (Class[]) null);
				Object address = mGetAddress.invoke(oConnection);
				telnum = address.toString();
			} catch (NoSuchMethodException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (IllegalAccessException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (IllegalArgumentException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (InvocationTargetException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (Exception e) { // Any other exception. For debugging purposes
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			}
			return telnum;
        }
        
        
        /**
         * Gets the number for a MO or MT call.
         * 
         * @param cCall
         * @param oCall
         * @return
         * @throws NoSuchMethodException
         * @throws IllegalAccessException
         * @throws IllegalArgumentException
         * @throws InvocationTargetException
         * @throws ClassNotFoundException 
         */
        private String getNumberForOutgoingCall(final Object asyncresult) {
        	final String METHOD = "::getNumberForOutgoingCall()  ";
        	
        	Object address = null;
			try {
				Object phone = getPhoneFromAsyncResult(asyncresult);
	        	Class<?> cPhone;
				cPhone = Class.forName("com.android.internal.telephony.Phone");
				Method mGetForegroundCall = cPhone.getDeclaredMethod("getForegroundCall", (Class[]) null);
	        	Object oCall = mGetForegroundCall.invoke(phone);
	        	Class<?> cCall = Class.forName("com.android.internal.telephony.Call");
	        	Method mGetLastestConnection = cCall.getDeclaredMethod("getLatestConnection", (Class[]) null);
				Object oConnection = mGetLastestConnection.invoke(oCall);
				Class<?> cConnection = oConnection.getClass();
				Method mGetAddress = cConnection.getDeclaredMethod("getAddress", (Class[]) null);
				address = mGetAddress.invoke(oConnection);
			} catch (ClassNotFoundException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (NoSuchMethodException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (IllegalAccessException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (IllegalArgumentException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (InvocationTargetException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			}
			return (address.toString());
        }
        
        private String getDisconnectCauseForIncomingCall(Object asyncresult) {
        	final String METHOD = "::getDisconnectCauseForIncomingCall()  ";
        	String disconnectioncause = "UNKNOWN";
        	
			try {
				Class<?> cAsyncResult = asyncresult.getClass();
	    	    Field fResult = cAsyncResult.getDeclaredField("result");
	    	    Object oGsmPhone = fResult.get(asyncresult);
	    	    Class<?> cGsmPhone = oGsmPhone.getClass();
	    	    Method mGetRingingCall = cGsmPhone.getDeclaredMethod("getRingingCall", (Class[]) null);
	    	    Object oRgGsmCall = mGetRingingCall.invoke(oGsmPhone);
				Class<?>  cCall = Class.forName("com.android.internal.telephony.Call");
				Method mGetLastestConnection = cCall.getDeclaredMethod("getLatestConnection", (Class[]) null);
				Object oConnection = mGetLastestConnection.invoke(oRgGsmCall);
				Class<?> cConnection = oConnection.getClass();
				Method mGetDisconnectCause = cConnection.getDeclaredMethod("getDisconnectCause", (Class[]) null);
				Object cause = mGetDisconnectCause.invoke(oConnection);
				disconnectioncause = cause.toString();
			} catch (ClassNotFoundException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (NoSuchMethodException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (IllegalAccessException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (IllegalArgumentException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (InvocationTargetException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (Exception e) { // Any other exception. For debugging purposes
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			}
			return disconnectioncause;
        }
        
        private String getDisconnectCause(final Object asyncresult) {
        	final String METHOD = "::getDisconnectCause()  ";
        	
        	String disconnectioncause = "UNKNOWN";	
			try {
				Object phone = getPhoneFromAsyncResult(asyncresult);
	        	Class<?> cPhone = Class.forName("com.android.internal.telephony.Phone");
				Method mGetForegroundCall = cPhone.getDeclaredMethod("getForegroundCall", (Class[]) null);
	        	Object oCall = mGetForegroundCall.invoke(phone);
	        	Class<?> cCall = Class.forName("com.android.internal.telephony.Call");
	        	Method mGetLastestConnection = cCall.getDeclaredMethod("getLatestConnection", (Class[]) null);
				Object oConnection = mGetLastestConnection.invoke(oCall);
				Class<?> cConnection = oConnection.getClass();
				Method mGetDisconnectCause = cConnection.getDeclaredMethod("getDisconnectCause", (Class[]) null);
				Object cause = mGetDisconnectCause.invoke(oConnection);
				disconnectioncause = cause.toString();
				// We need to retrieve enum Connection.DisconnectCause
	/*			Class<?> cDisconnectCause = cause.getClass();
				Object causes[] = cDisconnectCause.getEnumConstants();
				if(causes != null) {
					if(cause == causes[0]) { // NOT_DISCONNECTED
						disconnectioncause = "NOT_DISCONNECTED";
					} else if(cause == causes[2]) { // NORMAL (remote hanged)
						disconnectioncause = "NORMAL";
					} else if(cause == causes[3]) { // LOCAL (local hanged)
						disconnectioncause = "LOCAL";
					} else if(cause == causes[4]) { // BUSY
						disconnectioncause = "BUSY";
					} else if(cause == causes[5]) { // CONGESTION
						disconnectioncause = "CONGESTION";
					} else if(cause == causes[7]) { // INVALID_NUMBER
						disconnectioncause = "INVALID_NUMBER";
					} else if(cause == causes[8]) { // NUMBER_UNREACHABLE
						disconnectioncause = "NUMBER_UNREACHABLE";
					} else if(cause == causes[9]) { // SERVER_UNREACHABLE
						disconnectioncause = "SERVER_UNREACHABLE";
					} else if(cause == causes[11]) { // OUT_OF_NETWORK
						disconnectioncause = "OUT_OF_NETWORK";
					} else if(cause == causes[12]) { // SERVER_ERROR
						disconnectioncause = "SERVER_ERROR";
					} else if(cause == causes[13]) { // TIMED_OUT
						disconnectioncause = "TIMED_OUT";
					} else if(cause == causes[14]) { // LOST_SIGNAL
						disconnectioncause = "LOST_SIGNAL";
					} else if(cause == causes[15]) { // LIMIT_EXCEEDED
						disconnectioncause = "LIMIT_EXCEEDED";
					} else if(cause == causes[17]) { // POWER_OFF
						disconnectioncause = "POWER_OFF";
					} else if(cause == causes[18]) { // OUT_OF_SERVICE
						disconnectioncause = "OUT_OF_SERVICE";
					} else if(cause == causes[19]) { // ICC_ERROR
						disconnectioncause = "ICC_ERROR";
					} else if(cause == causes[20]) { // CALL_BARRED
						disconnectioncause = "CALL_BARRED";
					} else if(cause == causes[22]) { // CS_RESTRICTED
						disconnectioncause = "CS_RESTRICTED";
					} else if(cause == causes[23]) { // CS_RESTRICTED_NORMAL
						disconnectioncause = "CS_RESTRICTED_NORMAL";
					} else if(cause == causes[24]) { // CS_RESTRICTED_EMERGENCY
						disconnectioncause = "CS_RESTRICTED_EMERGENCY";
					} else if(cause == causes[25]) { // UNOBTAINABLE_NUMBER
						disconnectioncause = "UNOBTAINABLE_NUMBER";
					} else if(cause == causes[36]) { // ERROR_UNSPECIFIED
						disconnectioncause = "ERROR_UNSPECIFIED";
					}
				} */
			} catch (ClassNotFoundException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (NoSuchMethodException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (IllegalAccessException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (IllegalArgumentException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (InvocationTargetException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			}
//			Logger.Log(TAG + METHOD, "Disconnect cause = " + disconnectioncause);
			LOGGER.info(TAG + METHOD + "Disconnect cause = " + disconnectioncause);
			return disconnectioncause;
        }
    } // class PreciseCallEventsHandler
	
	
	/*
	 * Hangs up the foreground call.
	 * This is needed to hang calls up when this app is running as system process.
	 * Otherwise Globals.hangupCall() shall be used instead.
	 * 
	 */
	public void hangupCall() {
		final String METHOD = "::hangupCall()   ";
		Object mPhone = getPhoneInstance();
		if(mPhone != null) {
			try {
//				Logger.Log(TAG + METHOD, "Hanging call up.");
				LOGGER.info(TAG + METHOD + "Hanging call up.");
				Class<?>  cPhone = Class.forName("com.android.internal.telephony.Phone");
				Method mGetForegroundCall = cPhone.getMethod("getForegroundCall", (Class[]) null);
				Object mCall = mGetForegroundCall.invoke(mPhone);
//				Logger.Log(TAG + METHOD, "mCall object obtained");
				LOGGER.info(TAG + METHOD + "mCall object obtained");
				Class<?>  cCall = Class.forName("com.android.internal.telephony.Call");
				Method mGetState = cCall.getMethod("hangup", (Class[]) null);
				mGetState.invoke(mCall);
//				Logger.Log(TAG + METHOD, "call hanged");
				LOGGER.info(TAG + METHOD + "call hanged");
			} catch (ClassNotFoundException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (NoSuchMethodException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (IllegalAccessException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (IllegalArgumentException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (InvocationTargetException e) {
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			} catch (Exception e) { // Any other exception. For debugging purposes
//				Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
				LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
			}
		}
		else {
//			Logger.Log(TAG + METHOD, "ERROR: Trying to hang call up on null mPhone.");
			LOGGER.info(TAG + METHOD + "ERROR: Trying to hang call up on null mPhone.");
		}
	}
    
	
	private void registerForDetailedCallEvents(Object mPhone) {
		final String METHOD = "::registerForDetailedCallEvents()   ";
		try {
			Method mRegisterForStateChange = mPhone.getClass().getMethod("registerForPreciseCallStateChanged",
														new Class[]{Handler.class, Integer.TYPE, Object.class});            
			mRegisterForStateChange.invoke(mPhone, mHandler, EVENT_PRECISE_CALL_STATE_CHANGED, null);
//			Logger.Log(TAG + METHOD, "registered to receive precise call events.");
			LOGGER.info(TAG + METHOD + "registered to receive precise call events.");
		} catch (NoSuchMethodException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
	    } catch (InvocationTargetException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
	    } catch (IllegalAccessException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
	    } catch (SecurityException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
	    } catch (IllegalArgumentException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
		} catch (Exception e) { // Any other exception. For debugging purposes
//			Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
			LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
		}
	}
	
	
	private void unregisterForDetailedCallEvents(Object mPhone) {
		final String METHOD = "::unregisterForDetailedCallEvents()   ";
		try {
			Method mUnregisterForStateChange = 
				mPhone.getClass().getMethod("unregisterForPreciseCallStateChanged", new Class[]{Handler.class});            
			mUnregisterForStateChange.invoke(mPhone, mHandler);
//			Logger.Log(TAG + METHOD, "unregistered for precise call events.");
			LOGGER.info(TAG + METHOD + "unregistered for precise call events.");
		} catch (NoSuchMethodException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
	    } catch (InvocationTargetException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
	    } catch (IllegalAccessException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
	    } catch (SecurityException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
	    } catch (IllegalArgumentException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
		} catch (Exception e) { // Any other exception. For debugging purposes
//			Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
			LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
		}
	}
	
	
	private void registerForIncomingCallRinging(Object mPhone) {
		final String METHOD = "::registerForIncomingCallRinging()   ";
		try {
			Method mRegisterForInCall = mPhone.getClass().getMethod("registerForNewRingingConnection",
														new Class[]{Handler.class, Integer.TYPE, Object.class});            
			mRegisterForInCall.invoke(mPhone, mHandler, EVENT_NEW_RINGING_CONNECTION, null);
//			Logger.Log(TAG + METHOD, "registered to receive incoming call ringing events.");
			LOGGER.info(TAG + METHOD + "registered to receive incoming call ringing events.");
		} catch (NoSuchMethodException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
	    } catch (InvocationTargetException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
	    } catch (IllegalAccessException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
	    } catch (SecurityException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
	    } catch (IllegalArgumentException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
		} catch (Exception e) { // Any other exception. For debugging purposes
//			Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
			LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
		}
	}
	
	
	private void unregisterForIncomingCallRinging(Object mPhone) {
		final String METHOD = "::unregisterForIncomingCallRinging()   ";
		
		try {
			Method mUnregisterForInCall = 
				mPhone.getClass().getMethod("unregisterForIncomingRing", new Class[]{Handler.class});            
			mUnregisterForInCall.invoke(mPhone, mHandler);
//			Logger.Log(TAG + METHOD, "unregistered to receive call ringing events.");
			LOGGER.info(TAG + METHOD + "unregistered to receive call ringing events.");
		} catch (NoSuchMethodException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
	    } catch (InvocationTargetException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
	    } catch (IllegalAccessException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
	    } catch (SecurityException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
	    } catch (IllegalArgumentException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
		} catch (Exception e) { // Any other exception. For debugging purposes
//			Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
			LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
		}
	}
	
	
	private void registerForSrvccEvent(Object mPhone) {
		final String METHOD = "::registerForSrvccEvent()   ";
		try {
			Method mRegisterForSrvcc = mPhone.getClass().getMethod("registerForSrvccHandOver",
														new Class[]{Handler.class, Integer.TYPE, Object.class});            
			mRegisterForSrvcc.invoke(mPhone, mHandler, EVENT_IMS_SRVCC_HANDOVER, null);
//			Logger.Log(TAG + METHOD, "registered to receive SRVCC events.");
			LOGGER.info(TAG + METHOD + "registered to receive SRVCC events.");
		} catch (NoSuchMethodException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
	    } catch (InvocationTargetException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
	    } catch (IllegalAccessException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
	    } catch (SecurityException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
	    } catch (IllegalArgumentException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
		} catch (Exception e) { // Any other exception. For debugging purposes
//			Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
			LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
		}
	}
	
	
	private void unregisterForSrvccEvent(Object mPhone) {
		final String METHOD = "::unregisterForSrvccEvent()   ";
		
		try {
			Method mUnregisterForSrvcc = mPhone.getClass().getMethod("unregisterForSrvccHandOver",
														new Class[]{Handler.class});            
			mUnregisterForSrvcc.invoke(mPhone, mHandler);
//			Logger.Log(TAG + METHOD, "unregistered to receive SRVCC events.");
			LOGGER.info(TAG + METHOD + "unregistered to receive SRVCC events.");
		} catch (NoSuchMethodException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
	    } catch (InvocationTargetException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
	    } catch (IllegalAccessException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
	    } catch (SecurityException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
	    } catch (IllegalArgumentException e) {
//	        Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
	        LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
		} catch (Exception e) { // Any other exception. For debugging purposes
//			Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
			LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
		}
	}
	
	
	/**
	 * This method is used to retrieve the current instance of Phone 
	 * used by the system.
	 * 
	 * @return
	 */
	private Object getPhoneInstance() {
		final String METHOD = "::getPhoneInstance()   ";
		Class<?> cPhoneFactory;
		Object oPhone = null;
		try {
			cPhoneFactory = Class.forName("com.android.internal.telephony.PhoneFactory");
			Method mMakeDefaultPhone = cPhoneFactory.getMethod("makeDefaultPhone", new Class[] {Context.class});
			mMakeDefaultPhone.invoke(null, context);			
//			Logger.Log(TAG + METHOD, "makeDefaultPhone() completed");
			LOGGER.info(TAG + METHOD + "makeDefaultPhone() completed");
			Method mGetDefaultPhone = cPhoneFactory.getMethod("getDefaultPhone", (Class[]) null);
			oPhone = mGetDefaultPhone.invoke(null);
//			Logger.Log(TAG + METHOD, "Got default phone");
			LOGGER.info(TAG + METHOD + "Got default phone");
		} catch (ClassNotFoundException e) {
//			Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
			LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
		} catch (NoSuchMethodException e) {
//			Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
			LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
		} catch (IllegalAccessException e) {
//			Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
			LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
		} catch (IllegalArgumentException e) {
//			Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
			LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
		} catch (InvocationTargetException e) {
//			Logger.Log(TAG + METHOD, e.getClass().getName() + e.toString());
			LOGGER.info(TAG + METHOD + e.getClass().getName() + e.toString());
		}
		return oPhone;
	}
	
	
	/**
	 * Gets the ringing call object, which represents an incoming connection (if present) 
	 * that is pending answer/accept. (This connection may be RINGING or WAITING, and 
	 * there may be only one.)
	 * The ringing call is a singleton object. It is constant for the life of this phone. 
	 * It is never null.
	 * The ringing call will only ever be in one of these states: IDLE, INCOMING, WAITING 
	 * or DISCONNECTED.
	 * 
	 * @param phone
	 * @return
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
/*	private Object getRingingCallSingleton(Object phone) throws NoSuchMethodException, 
 																IllegalAccessException, 
 																IllegalArgumentException, 
 																InvocationTargetException {
		final String METHOD = "::getRingingCallSingleton()   ";
		Class<?> cPhone = phone.getClass();
		Method mGetRingingCall = cPhone.getMethod("getRingingCall", (Class[]) null);
		Object oCall = mGetRingingCall.invoke(PreciseCallStateReceiver.mPhone);
		Logger.Log(TAG + METHOD, "mGetRingingCall() completed");
		return oCall;
	} */
	
	/**
	 * Gets the foreground call object, which represents all connections that are dialing 
	 * or active (all connections that have their audio path connected).
	 * The foreground call is a singleton object. It is constant for the life of this phone.
	 * It is never null.
	 * The foreground call will only ever be in one of these states: IDLE, ACTIVE, DIALING, 
	 * ALERTING, or DISCONNECTED.
	 * 
	 * @param phone
	 * @return
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
/*	private Object getForegroundCallSingleton(Object phone) throws NoSuchMethodException, 
 																   IllegalAccessException, 
 																   IllegalArgumentException, 
 																   InvocationTargetException {
		final String METHOD = "::getForegroundCallSingleton()   ";
		Class<?> cPhone = phone.getClass();
		Method mGetForegroundCall = cPhone.getMethod("getForegroundCall", (Class[]) null);
		Object oCall = mGetForegroundCall.invoke(PreciseCallStateReceiver.mPhone);
		Logger.Log(TAG + METHOD, "mGetRingingCall() completed");
		return oCall;
	} */
	
	
	/**
	 * Gets the background call object, which represents all connections that are holding 
	 * (all connections that have been accepted or connected, but do not have their audio 
	 * path connected).
	 * The background call is a singleton object. It is constant for the life of this phone 
	 * object . It is never null.
	 * The background call will only ever be in one of these states: IDLE, HOLDING or 
	 * DISCONNECTED.
	 *  
	 * @param phone
	 * @return
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
/*	private Object getBackgroundCallSingleton(Object phone) throws NoSuchMethodException, 
 																   IllegalAccessException, 
 																   IllegalArgumentException, 
 																   InvocationTargetException {
		final String METHOD = "::getBackgroundCallSingleton()   ";
		Class<?> cPhone = phone.getClass();
		Method mGetBackgroundCall = cPhone.getMethod("getBackgroundCall", (Class[]) null);
		Object oCall = mGetBackgroundCall.invoke(PreciseCallStateReceiver.mPhone);
		Logger.Log(TAG + METHOD, "mGetRingingCall() completed");
		return oCall;
	} */
	
}
