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
	
	// ---- Constants ----
	public static final String FN_VDDIR 	= "volte_dialer";		// This is the directory in the external 
																	//	storage where reports are stored
	public static final String FN_VDLOG 	= "vdlog.txt";			// Log file
	public static final String DEF_MSISDN 	= "";					// Default test number where to call to
	// ---- End constants ----
	
	
	// ---- Variables ----
    public static boolean	is_vd_running;		// is the dialer running?
	public static String 	msisdn;				// TelNum to call to
	public static int		callduration;		// in ms
	public static int		timebetweencalls;	// in ms
	public static int		iservicestate;		// ServiceState
	// ---- End variables ----
    
	
	// ---- Methods ----
	
	/**
	 * Uses reflection to hangup an active call 
	 */
	public static void hangupCall(){
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
			Log.d("HWFunctionsContainer.hangupCall", "Exception: " + e.getMessage());
	    }
	}
	
	
    public static void answerCall(Context context) {
    	try {
    		Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
    		i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP,
    	            KeyEvent.KEYCODE_HEADSETHOOK));
    		context.sendOrderedBroadcast(i, null);
    	} catch(Exception e) {
    		Log.d("HWFunctionsContainer.answerCall", "Exception: " + e);
    	}
    }
    
}
