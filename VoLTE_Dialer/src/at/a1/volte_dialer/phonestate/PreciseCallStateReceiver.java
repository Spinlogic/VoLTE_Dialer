package at.a1.volte_dialer.phonestate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import at.a1.volte_dialer.dialer.DialerHandler;

public class PreciseCallStateReceiver {
	private static final String TAG = "PreciseCallStateReceiver";
	
	private static final int EVENT_PRECISE_CALL_STATE_CHANGED = 101;
	
	private PhoneStateReceiver mPhoneStateReceiver;
	private PreciseCallEventsHandler mHandler;
	private Object mPhone;		// Default phone instance retrieved from PhoneFactory
	
	Context context;
	
	public PreciseCallStateReceiver(Context c) {
		context = c;
		mHandler = new PreciseCallEventsHandler();
	}
	
	public void listen() {
		registerForDetailedCallEvents(context);
	}
	
	public void stop() {
		unregisterForDetailedCallEvents();
	}
	
	
    /**
     * Handler of incoming messages from clients.
     */
	private static class PreciseCallEventsHandler extends Handler {
	   
	   public enum State {
		   IDLE, ACTIVE, HOLDING, DIALING, ALERTING, INCOMING, WAITING, DISCONNECTED, DISCONNECTING
	   }
	   
        @Override
        public void handleMessage(Message msg) {
        	final String METHOD = "::PreciseCallEventsHandler::handleMessage()  ";
        	Log.d(TAG + METHOD, "  Message: " + Integer.toString(msg.what));
            switch (msg.what) {
                case EVENT_PRECISE_CALL_STATE_CHANGED:
                	processPrecisseCallEvent(msg.obj);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
        
        
        private void processPrecisseCallEvent(Object mGsmPhone) {
        	final String METHOD = "::PreciseCallEventsHandler::processPrecisseCallEvent()  ";
        	try {
				Class<?>  cGsmPhone = Class.forName("com.android.internal.telephony.gsm.GSMPhone");
				Method mGetForegroundCall = cGsmPhone.getMethod("getForegroundCall", (Class[]) null);
				Object mGsmCall = mGetForegroundCall.invoke(mGsmPhone);
				Log.d(TAG + METHOD, "DEBUG mGsmCall object obtained");
				Class<?>  cGsmCall = Class.forName("com.android.internal.telephony.gsm.GsmCall");
				Method mGetState = cGsmCall.getMethod("getState", (Class[]) null);
				Object mCallstate = mGetState.invoke(mGsmCall);
				Log.d(TAG + METHOD, "DEBUG call state obtained");
				switch((State) mCallstate) {
					case ALERTING:
						DialerHandler.setAlertingTime();
						break;
					case ACTIVE:
						DialerHandler.setActiveTime();
						break;
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
			}
    	}
    }
    
    
	public void registerForDetailedCallEvents(Context context) {
		final String METHOD = "registerForDetailedCallEvents";
		try {
			Class<?> mPhoneFactory = Class.forName("com.android.internal.telephony.PhoneFactory");
			Method mMakeDefaultPhone = mPhoneFactory.getMethod("makeDefaultPhone", new Class[] {Context.class});
			mMakeDefaultPhone.invoke(null, context);			
			Log.d(TAG + METHOD, "DEBUG makeDefaultPhone() completed");
			Method mGetDefaultPhone = mPhoneFactory.getMethod("getDefaultPhone", (Class[]) null);
			mPhone = mGetDefaultPhone.invoke(null);
			Log.d(TAG + METHOD, "DEBUG Got default phone");
			Method mRegisterForStateChange = mPhone.getClass().getMethod("registerForPreciseCallStateChanged",
														new Class[]{Handler.class, Integer.TYPE, Object.class});            
			mRegisterForStateChange.invoke(mPhone, mHandler, EVENT_PRECISE_CALL_STATE_CHANGED, null);
			Log.d(TAG + METHOD, "DEBUG registered to receive precise");
		} 
		catch (ClassNotFoundException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
	    }
	    catch (NoSuchMethodException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
	    }
	    catch (InvocationTargetException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
	    }
	    catch (IllegalAccessException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
	    } 
		catch (SecurityException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
	    } 
		catch (IllegalArgumentException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
		}
		finally {
	        Log.d(TAG + METHOD, "Unexpected exception");
	        mPhone = null;		// release Phone instance
		}
	}
	
	private void unregisterForDetailedCallEvents() {
		final String METHOD = "unregisterForDetailedCallEvents";
		try {
			Method mUnregisterForStateChange = 
				mPhone.getClass().getMethod("unregisterForPreciseCallStateChanged", new Class[]{Handler.class});            
			mUnregisterForStateChange.invoke(mPhone, mHandler);
			Log.d(TAG + METHOD, "DEBUG unregistered for precised call state changes");
		}
	    catch (NoSuchMethodException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
	    }
	    catch (InvocationTargetException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
	    }
	    catch (IllegalAccessException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
	    } 
		catch (SecurityException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
	    }
		catch (IllegalArgumentException e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
		}
		finally {
			Log.d(TAG + METHOD, "Unexpected exception");
			mPhone = null;		// release Phone instance
		}
	
	}
	
}
