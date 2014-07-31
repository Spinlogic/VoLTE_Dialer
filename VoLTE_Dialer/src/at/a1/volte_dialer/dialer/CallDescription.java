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

package at.a1.volte_dialer.dialer;

import java.util.List;

import android.content.Context;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import at.a1.volte_dialer.Globals;
import at.a1.volte_dialer.VD_Logger;
import at.a1.volte_dialer.phonestate.PhoneStateReceiver;

public class CallDescription {
	
	private final String TAG = "CallDescription";
	
	public static final String ACCESS_UNKNOWN	= "Unknown";
	public static final String ACCESS_LTE		= "LTE";
	public static final String ACCESS_WCDMA		= "WCDMA";
	public static final String ACCESS_GSM		= "GSM";
	public static final String ACCESS_CDMA		= "CDMA";
	
	// Call disconnection options
	public static final int CALL_DISCONNECTED_BY_UE = 0;
	public static final int CALL_DISCONNECTED_BY_NW = 1;
	
	private Context context;
	private long	starttime;
	private long	endtime;
	private int     disconnectionside;  // 0 -> UE, 1 -> NW
	private int		state;				// call state in TelephonyManager
	private String	startcellinfo;
	private String	endcellinfo;
	private int		startsignalstrength;
	private int		endsignalstrength;

	/**
	 * Constructor.
	 * 
	 * @param scid	Cell Id when call is started
	 */
	public CallDescription(Context c) {
		context 			= c;
		starttime			= System.currentTimeMillis();
		endtime				= 0;
		disconnectionside	= 0;
		state 				= TelephonyManager.CALL_STATE_IDLE;
		startcellinfo		= getCurrentCellId();
		endcellinfo 		= "";
		startsignalstrength	= PhoneStateReceiver.signalstrength;
		endsignalstrength	= 99;	// unknown
	}
	
	/**
	 * Records data when the call is disconnected.
	 * 
	 * @param ds	0 -> call terminated by UE
	 * 				1 -> call terminated by NW
	 */
	public void endCall(int ds) {
		state 				= TelephonyManager.CALL_STATE_IDLE;
		disconnectionside	= ds;
		endtime 			= System.currentTimeMillis();
		endcellinfo 		= getCurrentCellId();
		endsignalstrength	= PhoneStateReceiver.signalstrength;
	}
	
	/**
	 * Writes a log entry for the call in the log file.
	 */
	public void writeCallInfoToLog() {
		String logline = Long.toString((endtime - starttime) / 1000) + "," + 
						 Integer.toString(disconnectionside) + "," + 
						 startcellinfo + "," + Integer.toString(startsignalstrength) + 
						 "," + endcellinfo + "," + Integer.toString(endsignalstrength);
		VD_Logger.appendLog(logline);
	}
	
	public void setState(int newstate) {
		state = newstate;
	}
	
	public int getState() {
		return state;
	}
	
	public int getDisconnectionSide(){
		return disconnectionside;
	}
	
	// PRIVATE METHODS
	
	/**
	 * Gets info about the cell service the UE.
	 * It does not work for UEs that are only registered for LTE.
	 * 
	 * @param context
	 * @return	String with the following information:
	 * 		3GPP (WCDMA or GSM) / 3GPP2 (CDMA)	+ delimiter + 
	 * 			
	 * 		For LTE:
	 * 			 	MCC + delimiter + MNC + delimiter + TAC + delimiter + CID + delimiter + PCI
	 * 		For WCDMA:
	 * 				MCC + delimiter + MNC + delimiter + LAC + delimiter + CID + delimiter + PSC
	 * 		For GSM:
	 * 				MCC + delimiter + MNC + delimiter + LAC + delimiter + CID	
	 * 		For CDMA:
	 * 				System Id + delimiter + Network Id + delimiter + Base Station Id
	 * 
	 * NOTE: 	An alternative to this method is to listen for PhoneStateListener:onCellInfoChange,
	 * 			but we do not really care about cells added and removed from the neighbouring cell
	 * 			list. We only care for the current cell.
	 * 				
	 */
	private String getCurrentCellId() {
		final String METHOD = " getCurrentCellId()  ";
		final String DELIMITER = "_";
		
		String returnvalue = "";
		
		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		List<CellInfo> cellInfoList = tm.getAllCellInfo();
		
		if(cellInfoList != null) {
			for(CellInfo cellInfo : cellInfoList) {	// Requires API >= 17
				if(cellInfo instanceof CellInfoLte) {
					CellIdentityLte ci = ((CellInfoLte) cellInfo).getCellIdentity();
			    	returnvalue = ACCESS_LTE + DELIMITER +
			    				  Integer.toString(ci.getMcc()) + DELIMITER +
			    				  Integer.toString(ci.getMnc()) + DELIMITER +
			    				  Integer.toString(ci.getTac()) + DELIMITER +
			    				  Integer.toString(ci.getCi()) + DELIMITER +
			    				  Integer.toString(ci.getPci());
				} else if(cellInfo instanceof CellInfoWcdma) {	// Requires API >= 18
					CellIdentityWcdma ci = ((CellInfoWcdma) cellInfo).getCellIdentity();
					returnvalue = ACCESS_WCDMA + DELIMITER +
		    				  Integer.toString(ci.getMcc()) + DELIMITER +
		    				  Integer.toString(ci.getMnc()) + DELIMITER +
		    				  Integer.toString(ci.getLac()) + DELIMITER +
		    				  Integer.toString(ci.getCid()) + DELIMITER +
		    				  Integer.toString(ci.getPsc());
				} else if(cellInfo instanceof CellInfoGsm) {	// Requires API >= 17
					CellIdentityGsm ci = ((CellInfoGsm) cellInfo).getCellIdentity();
					returnvalue = ACCESS_GSM + DELIMITER +
		    				  Integer.toString(ci.getMcc()) + DELIMITER +
		    				  Integer.toString(ci.getMnc()) + DELIMITER +
		    				  Integer.toString(ci.getLac()) + DELIMITER +
		    				  Integer.toString(ci.getCid());
				} else if(cellInfo instanceof CellInfoCdma) {	// Requires API >= 17
					CellIdentityCdma ci = ((CellInfoCdma) cellInfo).getCellIdentity();
					returnvalue = ACCESS_CDMA + DELIMITER +
		    				  Integer.toString(ci.getSystemId()) + DELIMITER +
		    				  Integer.toString(ci.getNetworkId()) + DELIMITER +
		    				  Integer.toString(ci.getBasestationId());
				}
				break;	// Assume that first cell in the list is the current one.
						// We do not care about neighbouring ones.
			}
		}
		else {
			// Most likely getAllCellInfo() has an empty implementation in this UE
			// We can still get some cell info for non-LTE cells
		    CellLocation cl = tm.getCellLocation();
		    GsmCellLocation gsmLoc;
	        CdmaCellLocation cdmaLoc;
	        try {
	            String networkOperator = tm.getNetworkOperator();
	            if(networkOperator != null) {
	            returnvalue = networkOperator.substring(0, 3) + DELIMITER +
	            			  networkOperator.substring(3) + DELIMITER;
	    	    }
	            gsmLoc = (GsmCellLocation) cl;
	            returnvalue +=  String.valueOf(gsmLoc.getLac()) + DELIMITER;
	            returnvalue +=  String.valueOf(gsmLoc.getCid());
	        } catch (ClassCastException e) {
	        	try {
		            cdmaLoc = (CdmaCellLocation) cl;
		            returnvalue = ACCESS_CDMA + DELIMITER + String.valueOf(cdmaLoc.getSystemId()) + DELIMITER;
		            returnvalue +=  String.valueOf(cdmaLoc.getNetworkId()) + DELIMITER;
		            returnvalue +=  String.valueOf(cdmaLoc.getBaseStationId());
	        	} catch(ClassCastException ex) {
	        		returnvalue +=  ACCESS_UNKNOWN;
	        		Log.d("GeoPosition::getCurrentCellId : Tipo de localizacion de celda desconocido. Exception: ", ex.getMessage());
	        	}
	        }
		}
        return returnvalue;
	}
}
