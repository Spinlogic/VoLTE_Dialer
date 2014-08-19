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
package at.a1.volte_dialer.dialer;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.spinlogic.logger.SP_Logger;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DialerReceiver extends BroadcastReceiver  {
	public static final String TAG = "DialerReceiver";
	private final static Logger LOGGER = Logger.getLogger(SP_Logger.LOGGER_NAME);
	
	public static DsHandlerInterface dsIf = null;
	
/*	public void setDs(DsHandlerInterface ds) {
		dsIf = ds;
	} */
	
	static public void initLogger() {
		LOGGER.setLevel(Level.INFO);
	}
	
    @Override
    public void onReceive(final Context context, Intent intent) {
    	final String METHOD = ":onReceive()  ";
    	
    	if(dsIf != null) {
	    	if(dsIf.dsIf_isCallOngoing()) {
//	    		Logger.Log(TAG + METHOD, "Terminate call.");
	    		LOGGER.info(TAG + METHOD + "Terminate call.");
	    		dsIf.dsIf_endCall();
	    	}
	    	else {
//	    		Logger.Log(TAG + METHOD, "Trigger new call.");
	    		LOGGER.info(TAG + METHOD + "Trigger new call.");
	    		dsIf.dsIf_dialCall();
	    	}
    	}
    }
	
}
