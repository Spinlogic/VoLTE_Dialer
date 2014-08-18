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

package at.a1.volte_dialer.callmonitor;


/**
 * Defines a set of methods that the CallMonitorService implements for the
 * Receivers to use.
 * 
 * @author Juan Noguera
 *
 */
public interface CallMonitorInterface {

	// the receiver communicates a change in the UE service state
	public void csmif_ServiceState(final int what);
	
	// the receiver communicates a change in call state
	// "extra" contains an msisdn (in call setup) or a 
	// disconnection cause (in call release).
	public void csmif_CallState(final int state, final String extra);
	
	// the receiver communicates that SRVCC has occurred
	public void csmif_SrvccEvent();
	
	public void csmif_SignalStrength(int strength);
	
	public boolean csmif_isCallOngoing();
}
