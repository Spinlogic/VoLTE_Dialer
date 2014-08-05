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


package at.a1.volte_dialer;

import java.io.File;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;

/**
 * Defines global constants, variables and static methods. 
 * 
 * @author Juan Noguera
 *
 */
public class Globals {
	final static String TAG = "Globals";
	
	// ---- Constants ----
	public static final String FN_VDDIR 	= "volte_dialer";		// This is the directory in the external 
																	//	storage where reports are stored
	public static final String FN_VDLOG 	= "vdlog.txt";			// Log file
	public static final String DEF_MSISDN 	= "";					// Default test number where to call to
	public static final int average_call_setup_time = 10;			// Average call setup time to test number in seconds
	// ---- End constants ----
	
	
	// ---- Variables ----
	public static boolean	is_receiver;		// is the app configured for MO or MT. Receiver = MT.
	public static boolean	is_receiver_running;// indicates if the receiver service is running
	public static boolean	is_mtc_ongoing;		// indicates if there is a MT call ongoing
    public static boolean	is_vd_running;		// is the dialer running?
	public static String 	msisdn;				// TelNum to call to
	public static int		callduration;		// seconds
	public static int		timebetweencalls;	// seconds
	public static int		iservicestate;		// ServiceState
	public static int		icallnumber;		// used to display the call number that is being executed 
												// since the start of this dialer session
	
	public static VDMainActivity mainactivity;	// handle to the main activity
	// ---- End variables ----
    
	
	// ---- Methods ----

    public static boolean isEmailAddress(CharSequence addr) {
    	return android.util.Patterns.EMAIL_ADDRESS.matcher(addr).matches();
    }
    
    public static boolean isUrl(CharSequence addr) {
    	return android.util.Patterns.WEB_URL.matcher(addr).matches();
    }
    
    public static boolean fileExist(String filepath) {
    	File path = new File(filepath);
    	return path.exists();
    }
    
	/**
	 * Uses reflection to hangup an active call 
	 */
	public static void hangupCall(){
		final String METHOD = ":hangupCall()  ";
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

	    } catch (Exception e) {
			Log.d(TAG + METHOD, "Exception: " + e.getMessage());
	    }
	}
	
    public static void answerCall(Context context) {
    	final String METHOD = ":answerCall()  ";
    	try {
    		Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
    		i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP,
    	            KeyEvent.KEYCODE_HEADSETHOOK));
    		context.sendOrderedBroadcast(i, null);
    	} catch(Exception e) {
    		Log.d(TAG + METHOD, "Exception: " + e);
    	}
    }
    
}
