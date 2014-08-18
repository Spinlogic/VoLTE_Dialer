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
import net.spinlogic.logger.Logger;
import android.os.Binder;
import android.os.IBinder;

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
	public static final String DEF_MSISDN 	= "066466618";			// Default test number where to call to
	public static final int RIGHT_MATCH		= 6;					// MSISDN right matching for this number of digits 
	public static final String DEF_EMAIL 	= "juan.noguera@a1telekom.at";	
	public static final int average_call_setup_time = 10;			// Average call setup time to test number in seconds
	public static final int max_call_setup_time = 60;				// Maximum call setup time. Only used when running is system space.
	
	// Operation modes
	public static final int OPMODE_BG = 100;		// Background
	public static final int OPMODE_MT = 101;		// Receiver
	public static final int OPMODE_MO = 102;		// Sender
	// ---- End constants ----
	
	
	// ---- Variables ----
	public static int		opmode;					// Mode of operation
	public static boolean	is_mtc_ongoing;			// indicates if there is a MT call ongoing
    public static boolean	is_vd_running;			// is the dialer running?
	public static String 	msisdn;					// TelNum to call to
	public static int		callduration;			// seconds
	public static int		timebetweencalls;		// seconds
	public static int		iservicestate;			// ServiceState
	public static int		icallnumber;			// used to display the call number that is being executed 
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
    
}
