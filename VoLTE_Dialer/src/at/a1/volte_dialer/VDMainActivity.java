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

import java.io.File;
import java.util.List;
import java.util.Stack;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import at.a1.volte_dialer.dialer.DialerService;
import at.a1.volte_dialer.receiver.ReceiverService;

public class VDMainActivity extends Activity {
	private final String TAG = "VDMainActivity";
	
	private Thread count_thread;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_volte_dialer_main);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		if(Globals.is_receiver) {
			Intent intent = new Intent(this, ReceiverService.class);
			startService(intent);
		}
		
		Globals.mainactivity = this;
		Globals.is_running_as_system = Globals.isAppRunningAsSystem();
	}
	
	@Override
	protected void onDestroy() {
/* Services are stopped when this activity is destroyed.
 * It is assumed that the user wants to use the phone for other purposes.	*/
	if(Globals.is_receiver) {
			Intent intent = new Intent(this, ReceiverService.class);
			stopService(intent);
	}
	else {
		if(Globals.is_vd_running) {
			Intent intent = new Intent(this, DialerService.class);
			stopService(intent);
		}
	}
	Globals.is_mtc_ongoing = false;
	Globals.is_vd_running = false;
	Globals.is_receiver_running = false;
	Globals.mainactivity = null;
	super.onDestroy();
	}
	
	@Override
	public void onStart() {
		super.onStart();
	}
	
	@Override
	public void onResume() {
	    super.onResume();
	    String btn_text = "";
	    // GUI elements for which to set text
	    Button button 		= (Button) findViewById(R.id.startstop_button);
	    TextView tv_role 	= (TextView) findViewById(R.id.role_tv);
	    TextView tv_callnum = (TextView) findViewById(R.id.callnumber_tv);
	    String txt_role = getString(R.string.str_role) + "  ";
	    if(Globals.is_receiver) {
	    	btn_text = getString(R.string.btn_disabled);
	    	button.setEnabled(false);
	    	txt_role += getString(R.string.str_role_mt);
	    }
	    else {
	    	button.setEnabled(true);
		    if(Globals.is_vd_running) {
		    	btn_text = getString(R.string.btn_stop);
		    }
		    else {
		    	btn_text = getString(R.string.btn_start);
		    }
		    txt_role += getString(R.string.str_role_mo);
		    if(Globals.msisdn == null || Globals.msisdn.length() < 3) {
		    	button.setEnabled(false);
		    }
		    else {
		    	button.setEnabled(true);
		    }
	    }
	    button.setText(btn_text);
	    tv_role.setText(txt_role);
	    String txt_callnum = getResources().getText(R.string.str_callnum).toString() + "  " + Integer.toString(Globals.icallnumber);
	    tv_callnum.setText(txt_callnum);
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.volte_dialer_main, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		String addr = VD_Settings.getStringPref(this, VD_Settings.PREF_SENDLOGSURL, Globals.DEF_EMAIL);
		String logpath = Environment.getExternalStorageDirectory() + 
						 File.separator + Globals.FN_VDDIR + 
						 File.separator + Globals.FN_VDLOG;
	    if(addr.isEmpty() || !Globals.fileExist(logpath)) {
	        menu.getItem(0).setEnabled(false);
	    }
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		Intent myIntent;
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			myIntent = new Intent(this, SettingsActivity.class);
			startActivity(myIntent);
		} else if (id == R.id.action_sendlog) {
			// check if email or url
			String addr = VD_Settings.getStringPref(this, VD_Settings.PREF_SENDLOGSURL, "");
			if(!addr.isEmpty()) {
				String logpath = Environment.getExternalStorageDirectory() + 
								 File.separator + Globals.FN_VDDIR + 
								 File.separator + Globals.FN_VDLOG;
				if(Globals.isEmailAddress(addr)) {
					sendEmail(addr, logpath);
				}
				else { // is a URL. Can´t be anything else
					
				}
				boolean dellog = VD_Settings.getBoolPref(this, VD_Settings.PREF_DELETELOG, true);
				if(dellog) {
//					VD_Logger.deleteLog();
				}
			}
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(
					R.layout.fragment_volte_dialer_main, container, false);
			return rootView;
		}
		
	}
	
	/**
	 * Processes a clink on the start / stop button
	 * 
	 * @param view
	 */
	public void processStarStopBtnClick(View view) {
		String btn_text = "";
		if(Globals.is_vd_running) {
			Globals.is_vd_running = false;
			//	The stop process is as follows:
			//	1. DialService is stopped.
			//	2a. DialService hangs up the current call, if any.
			//	2b. DialService stops PhoneStateService.
			//	2c. DialService stops DialHandler.
			//	3. DialHandler stop() removes the alarm to trigger new call.
			//	4. PhoneStateService stops PhoneStateHandler.
			//	5a. PhoneStateHandler stops PhoneStateReceiver.
			//	5b. If the app is running in system space, then PhoneStateHandler
			//      stops PreciseCallStateReceiver.
			Globals.icallnumber	= 0;
			stopNextCallTimer();
			Intent intent = new Intent(this, DialerService.class);
    		stopService(intent);
    		btn_text = getResources().getText(R.string.btn_start).toString();
		}
		else {
			Intent intent = new Intent(this, DialerService.class);
    		startService(intent);
    		Globals.is_vd_running = true;
    		btn_text = getResources().getText(R.string.btn_stop).toString();
		}
		Button button = (Button) findViewById(R.id.startstop_button);
	    button.setText(btn_text);
	}
	
    /**
     * Open the email editor to send an email to address.
     * If filepath exists, then add it as attachment.
     * 
     * @param address
     * @param filepath
     */
    private void sendEmail(String address, String filepath) {    	
    	File path = new File(Environment.getExternalStorageDirectory() + 
    						File.separator + Globals.FN_VDDIR + 
    						File.separator + Globals.FN_VDLOG);	
    	boolean addattachment = path.exists();
    	final Intent emailIntent = new Intent(Intent.ACTION_SEND);
    	emailIntent.setType("message/rfc822");
    	emailIntent.putExtra(Intent.EXTRA_SUBJECT, "VolTE Dialer log" );
    	emailIntent.putExtra(Intent.EXTRA_TEXT, "");
    	emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {address});
//    	emailIntent.setData(Uri.parse("mailto:" + address));
//    	emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	if(addattachment) {
    		emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(path));
//    		emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); 
    	}
    	try {
    		String caption = getString(R.string.str_emailclientchoice);
//    		startActivity(Intent.createChooser(emailIntent, caption));
    		startActivity(createEmailOnlyChooserIntent(emailIntent, caption));
    	} catch (android.content.ActivityNotFoundException ex) {
    		Toast.makeText(this, R.string.str_noemailclient, Toast.LENGTH_SHORT).show();
    	}
    }
    
    /**
     * This code is borrowed from
     * http://stackoverflow.com/questions/2197741/how-to-send-email-from-my-android-application
     * 
     * @param source
     * @param chooserTitle
     * @return
     */
    public Intent createEmailOnlyChooserIntent(Intent source, CharSequence chooserTitle) {
    	Stack<Intent> intents = new Stack<Intent>();
    	Intent i = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "info@domain.com", null));
    	List<ResolveInfo> activities = getPackageManager().queryIntentActivities(i, 0);

    	for(ResolveInfo ri : activities) {
    		Intent target = new Intent(source);
            target.setPackage(ri.activityInfo.packageName);
            intents.add(target);
    	}

    	if(!intents.isEmpty()) {
    		Intent chooserIntent = Intent.createChooser(intents.remove(0), chooserTitle);
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toArray(new Parcelable[intents.size()]));
            return chooserIntent;
        } 
    	else {
        	return Intent.createChooser(source, chooserTitle);
        }
    }
    
    public void refreshCallNumber() {
    	if(Globals.mainactivity != null) {
    		Globals.mainactivity.runOnUiThread(new Runnable(){
				public void run(){
					TextView tv_callnum = (TextView) findViewById(R.id.callnumber_tv);
					String txt_callnum = getResources().getText(R.string.str_callnum).toString() + "  " + Integer.toString(Globals.icallnumber);
					tv_callnum.setText(txt_callnum);
				}
			});
    	}
    }
    
    public void startNextCallTimer() {
    	final String METHOD = ".updateNextCallTimer()";
    	if(Globals.mainactivity != null) {
/*	    	final String caption = getString(R.string.str_timetonextcall);
	    	final String units = getString(R.string.str_seconds);
	    	final TextView tv_counter = (TextView) findViewById(R.id.counter_tv); */
    		if(count_thread != null) {
    			count_thread = null;	// MUST be stopped	
    		}
	    	count_thread = new Thread() {
	    		int countdown = Globals.timebetweencalls;
	    		@Override
	    		public void run() {
	                try {
	                    while (!isInterrupted() && countdown >= 0) {
	                        Thread.sleep(1000);
	                        runOnUiThread(new Runnable() {
	                            @Override
	                            public void run() {
	                            	countdown--;
	                            	String caption = getString(R.string.str_timetonextcall);
	                            	String units = getString(R.string.str_seconds);
	                            	TextView tv_counter = (TextView) findViewById(R.id.counter_tv);
	                            	String txtcount = caption + " " + Integer.toString(countdown) + " " + units;
	                            	tv_counter.setText(txtcount);
	                            }
	                        });
	                    }
	                } catch (InterruptedException e) {
	                	Log.e(TAG + METHOD, "InterruptedException catched");
	                }
	            }
	        };
	
	        count_thread.start();
    	}
    	else {
    		count_thread = null;
    	}
    }
    
    public void stopNextCallTimer() {
    	if(count_thread != null) {
	    	count_thread.interrupt();
	    	TextView tv_counter = (TextView) findViewById(R.id.counter_tv);
	    	tv_counter.setText(""); 	// empty the text
    	}
    }

}
