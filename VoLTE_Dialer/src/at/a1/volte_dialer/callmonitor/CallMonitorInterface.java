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
}
