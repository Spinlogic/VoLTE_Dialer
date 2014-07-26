package at.a1.volte_dialer;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;

@SuppressLint("NewApi")
public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
	private static final String TAG = "SettingsFragment";
	
	private EditTextPreference MsisdnETPref		= null;
	private ListPreference WaitTimeListPref 	= null;
	private ListPreference CallDurationListPref = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
	    addPreferencesFromResource(R.xml.voltedialerprefs);
	    
	    MsisdnETPref = (EditTextPreference) findPreference(VD_Settings.PREF_MSIDN);
	    MsisdnETPref.setSummary(Globals.msisdn);
	    
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
	    	if(Globals.timebetweencalls == Integer.parseInt(calldurationvalues[i].toString())) {
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
        	Globals.msisdn = sharedPreferences.getString(key, "");
        	MsisdnETPref.setSummary(MsisdnETPref.getText());
        } else if(key.equals(VD_Settings.PREF_CALL_DURATION)) {
        	Globals.callduration = Integer.parseInt(sharedPreferences.getString(key, "5"));
        	CallDurationListPref.setSummary(CallDurationListPref.getEntry());
        } else if(key.equals(VD_Settings.PREF_WAIT_TIME)) {
        	Globals.timebetweencalls = Integer.parseInt(sharedPreferences.getString(key, "5"));
        	WaitTimeListPref.setSummary(WaitTimeListPref.getEntry());
        }
    }
	
}
