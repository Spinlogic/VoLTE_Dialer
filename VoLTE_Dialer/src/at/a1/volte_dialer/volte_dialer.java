package at.a1.volte_dialer;

import at.a1.volte_dialer.VD_Logger;
import android.app.Application;
import android.content.Context;

public class volte_dialer extends Application {

	public volte_dialer() {
		super();
	}
	
	
	@Override
	public void onCreate() {
        super.onCreate();
        
        Globals.is_vd_running = false;
        
        // Init the logger
        VD_Logger.initializeValues();
        
        //	init globals        
        Globals.msisdn = VD_Settings.getStringPref(
        				this,
        				VD_Settings.PREF_MSIDN,
        				"066466618");
        Globals.callduration = VD_Settings.getIntPref(
        				this, 
        				VD_Settings.PREF_CALL_DURATION,
        				5);
        Globals.timebetweencalls = VD_Settings.getIntPref(
        				this, 
        				VD_Settings.PREF_WAIT_TIME,
        				5);
    }
	
}
