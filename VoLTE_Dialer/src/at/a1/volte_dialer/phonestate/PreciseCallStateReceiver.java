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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import at.a1.volte_dialer.CallDescription;
import at.a1.volte_dialer.Globals;
import at.a1.volte_dialer.dialer.DialerHandler;


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
	
	private static final int EVENT_PRECISE_CALL_STATE_CHANGED = 101;
	private static final int EVENT_IMS_SRVCC_HANDOVER = 140;
	
	private PreciseCallEventsHandler mHandler;
	private static Object mPhone;		// Default phone instance retrieved from PhoneFactory
	
	private static Context context;
	
	public PreciseCallStateReceiver(Context c) {
		context = c;
		mHandler = new PreciseCallEventsHandler(this);
	}
	
	public void listen() {
		final String METHOD = "::listen()   ";
		try {
			mPhone = getPhoneInstance();
			registerForDetailedCallEvents();
			registerForSrvccEvent();
		} catch (ClassNotFoundException e) {
			Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
		} catch (NoSuchMethodException e) {
			Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
		} catch (IllegalAccessException e) {
			Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
		} catch (IllegalArgumentException e) {
			Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
		} catch (InvocationTargetException e) {
			Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
		}
	}
	
	public void stop() {
		unregisterForDetailedCallEvents();
		unregisterForSrvccEvent();
		mPhone = null;
		context = null;
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
        	Log.i(TAG + METHOD, "  Message: " + Integer.toString(msg.what));
            switch (msg.what) {
                case EVENT_PRECISE_CALL_STATE_CHANGED:
                	processPrecisseCallEvent();
                    break;
                case EVENT_IMS_SRVCC_HANDOVER:
                	mPreciseCallStateReceiver.unregisterForDetailedCallEvents();
                	mPreciseCallStateReceiver.unregisterForSrvccEvent();
                	mPhone = null;	// current mPhone is not valid anymore
					try {
						// Get a new phone and register for 
						mPhone = mPreciseCallStateReceiver.getPhoneInstance();
						mPreciseCallStateReceiver.registerForDetailedCallEvents();
						mPreciseCallStateReceiver.registerForSrvccEvent();
					} catch (ClassNotFoundException e) {
						Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
					} catch (NoSuchMethodException e) {
						Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
					} catch (IllegalAccessException e) {
						Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
					} catch (IllegalArgumentException e) {
						Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
					} catch (InvocationTargetException e) {
						Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
					}
                	break;
                default:
                    super.handleMessage(msg);
            }
        }
        
        
        private void processPrecisseCallEvent() {
        	final String METHOD = "::PreciseCallEventsHandler::processPrecisseCallEvent()  ";
        	try {
				Class<?>  cPhone = Class.forName("com.android.internal.telephony.Phone");
				Method mGetForegroundCall = cPhone.getMethod("getForegroundCall", (Class[]) null);
				Object oCall = mGetForegroundCall.invoke(PreciseCallStateReceiver.mPhone);
				Log.i(TAG + METHOD, "DEBUG mCall object obtained");
				Class<?>  cCall = Class.forName("com.android.internal.telephony.Call");
				Method mGetState = cCall.getMethod("getState", (Class[]) null);
				Object oCallstate = mGetState.invoke(oCall);
				Log.i(TAG + METHOD, "DEBUG call state obtained");
				
				// We need to retrieve enum Call.State
				Class<?> cCallState = oCallstate.getClass();
				Object eCallStates[] = cCallState.getEnumConstants();
				
				if(eCallStates != null) {
					if(oCallstate == eCallStates[0]) { // IDLE
						if(DialerHandler.isCallOngoing()) {
							// The call has been disconnected by the network.
							// Stop pending alarms to terminate the call from UE side.
							DialerHandler.stop(context);
							DialerHandler.endCall(CallDescription.CALL_DISCONNECTED_BY_NW);
							if(Globals.is_vd_running) {
								DialerHandler.setAlarm(context, Globals.timebetweencalls);	//	Set timer for next call
								if(Globals.mainactivity != null) {
									Globals.mainactivity.startNextCallTimer();
								}
							}
						}
					} else if(oCallstate == eCallStates[1]) { // ACTIVE
						DialerHandler.stop(context);	// Clear call setup timer
						DialerHandler.setActiveTime();
						DialerHandler.setAlarm(context, Globals.callduration);
					} else if (oCallstate == eCallStates[4]) { // ALERTING
						DialerHandler.setAlertingTime();
					} else if (oCallstate == eCallStates[8]) { // DISCONNECTING
						String disconnectioncause = getDisconnectCause(cCall, oCall);
						DialerHandler.setDisconnectionCause(disconnectioncause);
					} else if (oCallstate == eCallStates[7]) { // DISCONNECTED
						if(!DialerHandler.isDisconnectionCauseKnown()) {
							String disconnectioncause = getDisconnectCause(cCall, oCall);
							DialerHandler.setDisconnectionCause(disconnectioncause);
						}
					}
				}
			} catch (ClassNotFoundException e) {
				Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
			} catch (NoSuchMethodException e) {
				Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
			} catch (IllegalAccessException e) {
				Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
			} catch (IllegalArgumentException e) {
				Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
			} catch (InvocationTargetException e) {
				Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
			} catch (Exception e) { // Any other exception. For debugging purposes
				Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
			}
    	}
        
        
        private String getDisconnectCause(Class cCall, Object oCall) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        	final String METHOD = "::getDisconnectCause()  ";
        	
        	String disconnectioncause = "UNKNOWN";
        	Method mGetLastestConnection = cCall.getDeclaredMethod("getLatestConnection", (Class[]) null);
			Object oConnection = mGetLastestConnection.invoke(oCall);
			Class<?> cConnection = oConnection.getClass();
			Method mGetDisconnectCause = cConnection.getDeclaredMethod("getDisconnectCause", (Class[]) null);
			Object cause = mGetDisconnectCause.invoke(oConnection);
			// We need to retrieve enum Connection.DisconnectCause
			Class<?> cDisconnectCause = cause.getClass();
			Object causes[] = cDisconnectCause.getEnumConstants();
			if(causes != null) {
				disconnectioncause = causes.toString();
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
			}
			Log.i(TAG + METHOD, "Disconnect cause = " + disconnectioncause);
			return disconnectioncause;
        }
    } // class PreciseCallEventsHandler
	
	
	/*
	 * Hangs up the foreground call.
	 * This is needed to hang calls up when this app is running as system process.
	 * Otherwise Globals.hangupCall() shall be used instead.
	 * 
	 */
	static public void hangupCall() {
		final String METHOD = "::hangupCall()   ";
		if(mPhone != null) {
			try {
				Log.i(TAG + METHOD, "Hanging call up.");
				Class<?>  cPhone = Class.forName("com.android.internal.telephony.Phone");
				Method mGetForegroundCall = cPhone.getMethod("getForegroundCall", (Class[]) null);
				Object mCall = mGetForegroundCall.invoke(PreciseCallStateReceiver.mPhone);
				Log.i(TAG + METHOD, "DEBUG mCall object obtained");
				Class<?>  cCall = Class.forName("com.android.internal.telephony.Call");
				Method mGetState = cCall.getMethod("hangup", (Class[]) null);
				mGetState.invoke(mCall);
				Log.i(TAG + METHOD, "call hanged");
			} catch (ClassNotFoundException e) {
				Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
			} catch (NoSuchMethodException e) {
				Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
			} catch (IllegalAccessException e) {
				Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
			} catch (IllegalArgumentException e) {
				Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
			} catch (InvocationTargetException e) {
				Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
			} catch (Exception e) { // Any other exception. For debugging purposes
				Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
			}
		}
		else {
			Log.d(TAG + METHOD, "ERROR: Trying to hang call up on null mPhone.");
		}
	}
    
	
	private void registerForDetailedCallEvents() {
		final String METHOD = "::registerForDetailedCallEvents()   ";
		try {
			Method mRegisterForStateChange = mPhone.getClass().getMethod("registerForPreciseCallStateChanged",
														new Class[]{Handler.class, Integer.TYPE, Object.class});            
			mRegisterForStateChange.invoke(mPhone, mHandler, EVENT_PRECISE_CALL_STATE_CHANGED, null);
			Log.i(TAG + METHOD, "DEBUG registered to receive precise");
		} catch (NoSuchMethodException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
	    } catch (InvocationTargetException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
	    } catch (IllegalAccessException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
	    } catch (SecurityException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
	    } catch (IllegalArgumentException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
		} catch (Exception e) { // Any other exception. For debugging purposes
			Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
		}
	}
	
	
	private void unregisterForDetailedCallEvents() {
		final String METHOD = "::unregisterForDetailedCallEvents()   ";
		try {
			Method mUnregisterForStateChange = 
				mPhone.getClass().getMethod("unregisterForPreciseCallStateChanged", new Class[]{Handler.class});            
			mUnregisterForStateChange.invoke(mPhone, mHandler);
			Log.i(TAG + METHOD, "DEBUG unregistered for precised call state changes");
		} catch (NoSuchMethodException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
	    } catch (InvocationTargetException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
	    } catch (IllegalAccessException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
	    } catch (SecurityException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
	    } catch (IllegalArgumentException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
		} catch (Exception e) { // Any other exception. For debugging purposes
			Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
		}
	}
	
	
	private void registerForSrvccEvent() {
		final String METHOD = "::registerForSrvccEvent()   ";
		try {
			Method mRegisterForSrvcc = mPhone.getClass().getMethod("registerForSrvccHandOver",
														new Class[]{Handler.class, Integer.TYPE, Object.class});            
			mRegisterForSrvcc.invoke(mPhone, mHandler, EVENT_IMS_SRVCC_HANDOVER, null);
			Log.i(TAG + METHOD, "DEBUG registered to receive precise");
		} catch (NoSuchMethodException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
	    } catch (InvocationTargetException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
	    } catch (IllegalAccessException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
	    } catch (SecurityException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
	    } catch (IllegalArgumentException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
		} catch (Exception e) { // Any other exception. For debugging purposes
			Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
		}
	}
	
	
	private void unregisterForSrvccEvent() {
		final String METHOD = "::unregisterForSrvccEvent()   ";
		
		try {
			Method mUnregisterForSrvcc = mPhone.getClass().getMethod("unregisterForSrvccHandOver",
														new Class[]{Handler.class, Integer.TYPE, Object.class});            
			mUnregisterForSrvcc.invoke(mPhone, mHandler);
			Log.i(TAG + METHOD, "DEBUG registered to receive precise");
		} catch (NoSuchMethodException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
	    } catch (InvocationTargetException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
	    } catch (IllegalAccessException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
	    } catch (SecurityException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
	    } catch (IllegalArgumentException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
		} catch (Exception e) { // Any other exception. For debugging purposes
			Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
		}
	}
	
	
	/**
	 * 
	 * This method is used to retrieve the current instance of Phone 
	 * used by the system.
	 * 
	 * Some changes, like SRVCC, change the instance.
	 * 
	 */
	private Object getPhoneInstance() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		final String METHOD = "::getPhoneInstance()   ";
		Class<?> mPhoneFactory = Class.forName("com.android.internal.telephony.PhoneFactory");
		Method mMakeDefaultPhone = mPhoneFactory.getMethod("makeDefaultPhone", new Class[] {Context.class});
		mMakeDefaultPhone.invoke(null, context);			
		Log.i(TAG + METHOD, "DEBUG makeDefaultPhone() completed");
		Method mGetDefaultPhone = mPhoneFactory.getMethod("getDefaultPhone", (Class[]) null);
		Object oPhone = mGetDefaultPhone.invoke(null);
		Log.i(TAG + METHOD, "DEBUG Got default phone");
		return oPhone;
	}
	
}
