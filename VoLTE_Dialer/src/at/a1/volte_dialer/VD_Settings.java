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

import net.spinlogic.logger.Logger;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


public class VD_Settings {
	
	public static final String PREF_MSIDN			= "pref_key_mt_msisdn";
	public static final String PREF_RECEIVER		= "pref_key_receiver";
	public static final String PREF_CALL_DURATION	= "pref_key_call_duration";
	public static final String PREF_WAIT_TIME		= "pref_key_time_between_calls";
	public static final String PREF_SENDLOGSURL		= "pref_key_sendlogsurl";
	public static final String PREF_DELETELOG		= "pref_key_deletelogfile";
	public static final String PREF_BGMODE			= "pref_key_bgmode";

	public static String getStringPref(Context c, String prefname, String defvalue) {
		String result = defvalue;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		try {
			result = prefs.getString(prefname, defvalue);
		} catch(ClassCastException e) {
			Logger.Log("VD_Settings::getStringPref    ClassCastException: ", e.getMessage());
		}
		return result;
	}
	
	public static boolean setStringPref(Context c, String prefname, String prefvalue) {		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(prefname, prefvalue);
		return editor.commit();
	}
	
	public static boolean getBoolPref(Context c, String prefname, boolean defvalue) {
		boolean result = defvalue;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		try {
			result = prefs.getBoolean(prefname, defvalue);
		} catch(ClassCastException e) {
			Logger.Log("VD_Settings::getStringPref    ClassCastException: ", e.getMessage());
		}
		return result;
	}
	
	public static boolean setBoolPref(Context c, String prefname, boolean prefvalue) {		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(prefname, prefvalue);
		return editor.commit();
	}
	
	public static long getLongPref(Context c, String prefname, long defvalue) {
		long result = defvalue;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		try {
			result = prefs.getLong(prefname, defvalue);
		} catch(ClassCastException e) {
			Logger.Log("VD_Settings::getLongPref    ClassCastException: ", e.getMessage());
		}
		return result;
	}
	
	public static boolean setLongPref(Context c, String prefname, long prefvalue) {		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putLong(prefname, prefvalue);
		return editor.commit();
	}
	
	public static int getIntPref(Context c, String prefname, int defvalue) {
		int result = defvalue;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		try {
			result = prefs.getInt(prefname, defvalue);
		} catch(ClassCastException e) {
			Logger.Log("VD_Settings::getIntPref    ClassCastException: ", e.getMessage());
		}
		return result;
	}
	
	public static boolean setIntPref(Context c, String prefname, int prefvalue) {		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(prefname, prefvalue);
		return editor.commit();
	}
	
}
