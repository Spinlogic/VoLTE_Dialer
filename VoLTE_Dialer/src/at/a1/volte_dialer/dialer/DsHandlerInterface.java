package at.a1.volte_dialer.dialer;

import android.content.Context;


/**
 * 
 * Implemented by DialerService to provide a set of methods for the
 * DialerReceiver to use.
 * 
 * @author Juan Noguera
 *
 */
public interface DsHandlerInterface {
	public boolean	dsIf_isCallOngoing();
	public void 	dsIf_endCall();
	public void 	dsIf_startNextCallTimer();
	public void 	dsIf_dialCall();
}
