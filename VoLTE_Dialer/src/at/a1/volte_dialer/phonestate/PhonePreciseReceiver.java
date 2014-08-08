package at.a1.volte_dialer.phonestate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class PhonePreciseReceiver extends Thread {
	private static final String TAG = "PhoneStateHandler";
	
	private static final int EVENT_PRECISE_CALL_STATE_CHANGED = 101;
	
	private PreciseCallEventsHandler mHandler;
	private Context context;
	
	public PhonePreciseReceiver(Context c) {
		 mHandler = null;
		 context = c;
	}
	
	public void run() {
		Looper.prepare();
        mHandler = new PreciseCallEventsHandler();
        registerForDetailedCallEvents2(context);
        Looper.loop();
    }
	
	public void stopLooper() {
		Looper.myLooper().quit();
	}
	
    /**
     * Handler of incoming messages from clients.
     */
    static class PreciseCallEventsHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	final String METHOD = "PreciseCallEventsHandler::handleMessage() DEBUG   ";
        	Log.d(TAG + METHOD, "  Message: " + Integer.toString(msg.what));
            switch (msg.what) {
                case EVENT_PRECISE_CALL_STATE_CHANGED:
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    
    
	public void registerForDetailedCallEvents2(Context context) {
		final String METHOD = "registerForDetailedCallEvents";
		try {
			Class<?> mPhoneFactory = Class.forName("com.android.internal.telephony.PhoneFactory");
//			Method mMakeDefaultPhone = mPhoneFactory.getMethod("makeDefaultPhone", new Class[] {Context.class});
//			mMakeDefaultPhone.invoke(null, context);			

			Method mGetDefaultPhone = mPhoneFactory.getMethod("getDefaultPhone", (Class[]) null);
			Object mPhone = mGetDefaultPhone.invoke(null);
			Method mRegisterForStateChange = mPhone.getClass().getMethod("registerForPreciseCallStateChanged",
														new Class[]{Handler.class, Integer.TYPE, Object.class});            
			mHandler = new PreciseCallEventsHandler();
			mRegisterForStateChange.invoke(mPhone, mHandler, EVENT_PRECISE_CALL_STATE_CHANGED, null);
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
		catch (Exception e) {
	        Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
		}
	}

}
