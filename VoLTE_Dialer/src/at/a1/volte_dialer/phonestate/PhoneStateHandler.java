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

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

/**
 * This class implements a handler for the PhoneStateService
 * 
 * @author Juan Noguera
 *
 */
public class PhoneStateHandler {
	private static final String TAG = "PhoneStateHandler";
	
	private Context context;
	private PhoneStateReceiver stateListener;
	private TelephonyManager telMng;
	
	public PhoneStateHandler(Context c) {
		context 		= c;
		stateListener 	= new PhoneStateReceiver(c);
	}
	
	public void start() {
	    // Start listening for changes in service and call states
	    telMng = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
	    telMng.listen(stateListener, PhoneStateListener.LISTEN_CALL_STATE |
	    			  PhoneStateListener.LISTEN_SERVICE_STATE);
	}
	
	public void stop() {
		telMng.listen(stateListener, PhoneStateListener.LISTEN_NONE);
	}
}
