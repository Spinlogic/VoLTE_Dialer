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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.widget.Toast;
import at.a1.volte_dialer.dialer.DialerService;
import at.a1.volte_dialer.receiver.ReceiverService;

@SuppressLint("NewApi")
public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
	private static final String TAG = "SettingsFragment";
	
	private EditTextPreference MsisdnETPref		= null;
	private CheckBoxPreference ReceiverPref		= null;
	private EditTextPreference SendLogsETPref	= null;
	private ListPreference WaitTimeListPref 	= null;
	private ListPreference CallDurationListPref = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
	    addPreferencesFromResource(R.xml.voltedialerprefs);
	    
	    MsisdnETPref = (EditTextPreference) findPreference(VD_Settings.PREF_MSIDN);
	    MsisdnETPref.setSummary(Globals.msisdn);
	    
	    ReceiverPref = (CheckBoxPreference) findPreference(VD_Settings.PREF_RECEIVER);
	    String receiversum = (Globals.is_receiver) ? 
	    					getActivity().getString(R.string.pref_mt_role) : 
	    					getActivity().getString(R.string.pref_mo_role);
	    ReceiverPref.setSummary(receiversum);
	    
	    SendLogsETPref = (EditTextPreference) findPreference(VD_Settings.PREF_SENDLOGSURL);
	    String sendlogsum = VD_Settings.getStringPref(getActivity(), VD_Settings.PREF_SENDLOGSURL, "");
	    if(!sendlogsum.isEmpty()) {
	    	SendLogsETPref.setSummary(sendlogsum);
	    }
	    
	    WaitTimeListPref = (ListPreference) findPreference(VD_Settings.PREF_WAIT_TIME);
	    CharSequence[] waittimeentries = WaitTimeListPref.getEntries();
	    CharSequence[] waittimevalues = WaitTimeListPref.getEntryValues();
	    for(int i = 0; i < waittimevalues.length; i++) {
	    	if(Globals.timebetweencalls == Integer.parseInt(waittimevalues[i].toString())) {
	    		WaitTimeListPref.setSummary(waittimeentries[i]);
	    	}
	    }
	    
	    CallDurationListPref = (ListPreference) findPreference(VD_Settings.PREF_CALL_DURATION);
	    CharSequence[] calldurationentries = CallDurationListPref.getEntries();
	    CharSequence[] calldurationvalues = CallDurationListPref.getEntryValues();
	    for(int i = 0; i < calldurationvalues.length; i++) {
	    	if(Globals.callduration == Integer.parseInt(calldurationvalues[i].toString())) {
	    		CallDurationListPref.setSummary(calldurationentries[i]);
	    	}
	    }
	   
	}
	
	@Override
	public void onResume() {
	    super.onResume();
	    getPreferenceScreen().getSharedPreferences()
	            .registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onPause() {
	    super.onPause();
	    getPreferenceScreen().getSharedPreferences()
	            .unregisterOnSharedPreferenceChangeListener(this);
	}
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if(key.equals(VD_Settings.PREF_MSIDN)) {
        	Globals.msisdn = sharedPreferences.getString(key, Globals.DEF_MSISDN);
        	MsisdnETPref.setSummary(MsisdnETPref.getText());
        } else if(key.equals(VD_Settings.PREF_RECEIVER)) {
        	Globals.is_receiver = sharedPreferences.getBoolean(key, false);
        	String receiversum = (Globals.is_receiver) ? 
 					getActivity().getString(R.string.pref_mt_role) : 
 					getActivity().getString(R.string.pref_mo_role);
 			ReceiverPref.setSummary(receiversum);
 			if(Globals.is_receiver) {
 				if(Globals.is_vd_running) {
	 				// stop the service that is establishing calls
	 				Intent intent = new Intent(getActivity(), DialerService.class);
	 				getActivity().stopService(intent);
 				}
 				Intent intent = new Intent(getActivity(), ReceiverService.class);
 				getActivity().startService(intent);
 			}
 			else {
 				// stop receiver service
 				Intent intent = new Intent(getActivity(), ReceiverService.class);
 				getActivity().stopService(intent);
 			}
 			Globals.icallnumber	= 0;	// reset call counter
        } else if(key.equals(VD_Settings.PREF_CALL_DURATION)) {
        	Globals.callduration = Integer.parseInt(sharedPreferences.getString(key, "20"));
        	CallDurationListPref.setSummary(CallDurationListPref.getEntry());
        } else if(key.equals(VD_Settings.PREF_WAIT_TIME)) {
        	Globals.timebetweencalls = Integer.parseInt(sharedPreferences.getString(key, "20"));
        	WaitTimeListPref.setSummary(WaitTimeListPref.getEntry());
        } else if(key.equals(VD_Settings.PREF_SENDLOGSURL)) {
        	String sendlogsum = sharedPreferences.getString(key, Globals.DEF_EMAIL);
        	if(!sendlogsum.isEmpty()) {
        		if(isValidUrl(sendlogsum)) {
        			SendLogsETPref.setSummary(sendlogsum);
        		}
        		else {
        			Toast.makeText(getActivity(), R.string.pref_invalidurl, Toast.LENGTH_SHORT).show();
        			Editor editor = sharedPreferences.edit();
        			editor.putString(VD_Settings.PREF_SENDLOGSURL,	"");
        			editor.commit();
        		}
    	    }
        }
    }
	
	private boolean isValidUrl(CharSequence url) {
		boolean isvalid = false;
		
		isvalid = Globals.isEmailAddress(url);
		if(!isvalid) {
			isvalid = Globals.isUrl(url);
		}
		return isvalid;
	}
	
	
	
}
