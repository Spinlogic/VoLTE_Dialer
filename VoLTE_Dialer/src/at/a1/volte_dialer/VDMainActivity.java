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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import at.a1.volte_dialer.callmonitor.CallMonitorService;
import at.a1.volte_dialer.dialer.DialerService;
import at.a1.volte_dialer.receiver.ReceiverService;

public class VDMainActivity extends Activity {
	private final String TAG = "VDMainActivity";
	
	private Thread 			count_thread;
	private Messenger		mReceiverService	= null;		// to send messages to ReceiverService
	private Messenger		mDialerService 		= null;		// to send messages to DialerService
	final private Messenger	mDsClient 			= new Messenger(new DsMsgHandler());	// to receive messages from DialerService
	final private Messenger	mRsClient 			= new Messenger(new RsMsgHandler());	// to receive messages from ReceiverService
	
	
	
	/**
     * Handler of incoming messages from DialerService
     */
    @SuppressLint("HandlerLeak")
	class DsMsgHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	final String METHOD = "::DsMsgHandler::handleMessage()  ";
            switch (msg.what) {
                case DialerService.MSG_DS_NEWCALLATTEMPT:
                	Log.i(TAG + METHOD, "MSG_DS_NEWCALLATTEMPT received from DialerService.");
                	Globals.icallnumber++;
                	refreshCallNumber(null);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
	
	
	/**
     * Handler of incoming messages from ReceiverService
     */
    @SuppressLint("HandlerLeak")
	class RsMsgHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	final String METHOD = "::RsMsgHandler::handleMessage()  ";
            switch (msg.what) {
                case ReceiverService.MSG_RS_NEWCALLATTEMPT:
                	Log.i(TAG + METHOD, "MSG_RS_NEWCALLATTEMPT received from ReceiverService.");
                	Globals.icallnumber++;
                	refreshCallNumber(null);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    
    
	private ServiceConnection mDsConnection = new ServiceConnection() {
			
	        public void onServiceConnected(ComponentName className, IBinder service) {
	        	final String METHOD = "::mDsConnection::onServiceConnected()  ";
	        	mDialerService = new Messenger(service);
	        	sendMsg(mDialerService, DialerService.MSG_CLIENT_ADDHANDLER, mDsClient);
	        	Log.i(TAG + METHOD, "Bound to RemoteService");
	        }
	
	        public void onServiceDisconnected(ComponentName className) {
	            // This is called when the connection with the service has been
	            // unexpectedly disconnected -- that is, its process crashed.
	        	mDialerService = null;
	        	String msg = getString(R.string.str_dsstopped);
	        	refreshCallNumber(msg);
	        	// Set button properly
	        	Globals.is_vd_running = false;
				Globals.icallnumber	= 0;
				stopNextCallTimer();
	    		String btn_text = getString(R.string.btn_start);
	    		Button button = (Button) findViewById(R.id.startstop_button);
	    	    button.setText(btn_text);
	        }
	    };
    
    
	private ServiceConnection mRsConnection = new ServiceConnection() {
		
        public void onServiceConnected(ComponentName className, IBinder service) {
        	final String METHOD = "::mRsConnection::onServiceConnected()  ";
        	mReceiverService = new Messenger(service);
        	sendMsg(mReceiverService, ReceiverService.MSG_CLIENT_ADDHANDLER, mRsClient);
        	Log.i(TAG + METHOD, "Bound to RemoteService");
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
        	mReceiverService = null;
        	String msg = getString(R.string.str_rsstopped);
        	refreshCallNumber(msg);
        	// Set button properly
        	Globals.is_vd_running = false;
			Globals.icallnumber	= 0;
			stopNextCallTimer();
    		String btn_text = getString(R.string.btn_start);
    		Button button = (Button) findViewById(R.id.startstop_button);
    	    button.setText(btn_text);
        }
    };

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_volte_dialer_main);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		if((Globals.opmode == Globals.OPMODE_MT) && 
					Globals.msisdn != null 		 && 
					!Globals.msisdn.isEmpty()) {
			bindReceiverService();
		}
		Globals.mainactivity = this;
		Globals.is_running_as_system = Globals.isAppRunningAsSystem();
	}
	
	@Override
	protected void onDestroy() {
/* Services are stopped when this activity is destroyed.
 * It is assumed that the user wants to use the phone for other purposes.	*/
		if(mReceiverService != null) {
				unbindReceiverService();
		}
		else {
			if(Globals.is_vd_running) {
				unbindDialerService();
			}
		}
		Globals.is_mtc_ongoing = false;
		Globals.mainactivity = null;
		super.onDestroy();
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
	    if(Globals.opmode == Globals.OPMODE_BG) {
	    	button.setEnabled(true);
	    	btn_text = getString(R.string.btn_bg);
	    	txt_role += getString(R.string.str_role_bg);
	    } else if(Globals.opmode == Globals.OPMODE_MT) {
	    	btn_text = getString(R.string.btn_disabled);
	    	button.setEnabled(false);
    		txt_role += getString(R.string.str_role_mt);
	    } else {
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
	    String txt_callnum = (Globals.opmode == Globals.OPMODE_BG) ? 
	    		getString(R.string.str_send_bg) :
	    		getString(R.string.str_callnum) + "  " + Integer.toString(Globals.icallnumber);
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
		if(Globals.opmode == Globals.OPMODE_BG) {
			sendToBg();
		} 
		else if(Globals.is_vd_running) {
			Globals.icallnumber	= 0;
			stopNextCallTimer();
			unbindDialerService();
    		btn_text = getResources().getText(R.string.btn_start).toString();
		}
		else {
			bindDialerService();
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
    
    /**
     * Updates the call number being display.
     * If a string is provided, the this string is displayed instead.
     * 
     * @param msg	message to display. Set to null if the number of calls shall be displayed.
     */
    public void refreshCallNumber(final String msg) {
    	if(Globals.mainactivity != null) {
    		Globals.mainactivity.runOnUiThread(new Runnable(){
				public void run(){
					TextView tv_callnum = (TextView) findViewById(R.id.callnumber_tv);
					String txt_callnum = (msg != null) ? msg : getString(R.string.str_callnum) + "  " + Integer.toString(Globals.icallnumber);
					tv_callnum.setText(txt_callnum);
				}
			});
    	}
    }
    
    public void startNextCallTimer() {
    	final String METHOD = ".updateNextCallTimer()";
    	if(Globals.mainactivity != null) {
    		if(count_thread != null) {
    			count_thread = null;	// MUST be stopped
    		}
	    	count_thread = new Thread() {
	    		int countdown = Globals.timebetweencalls;
	    		@Override
	    		public void run() {
	                try {
	                    while(!isInterrupted() && countdown >= 0) {
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
    
    
    public void bindReceiverService() {
    	Intent intent = new Intent(this, ReceiverService.class);
		String suffix = (Globals.msisdn.length() < Globals.RIGHT_MATCH) ? 
						Globals.msisdn : 
						Globals.msisdn.substring(Globals.msisdn.length() - Globals.RIGHT_MATCH);
		intent.putExtra(ReceiverService.EXTRA_SUFFIX, suffix);
		bindService(intent, mRsConnection, Context.BIND_AUTO_CREATE);
    }
    
    
    public void unbindReceiverService() {
    	unbindService(mRsConnection);
    	mReceiverService = null;
    }
    
    
    public void bindDialerService() {
    	if(!Globals.msisdn.isEmpty()) {
    		Globals.is_vd_running = true;
	    	Intent intent = new Intent(this, DialerService.class);
			intent.putExtra(DialerService.EXTRA_MSISDN, Globals.msisdn);
			intent.putExtra(DialerService.EXTRA_DURATION, Globals.callduration);
			intent.putExtra(DialerService.EXTRA_WAITTIME, Globals.timebetweencalls);
			intent.putExtra(DialerService.EXTRA_TMAXSETUP, Globals.max_call_setup_time);
			intent.putExtra(DialerService.EXTRA_TAVGSETUP, Globals.average_call_setup_time);
			startService(intent);
			bindService(intent, mDsConnection, Context.BIND_AUTO_CREATE);
    	}
    }
    
    
    public void unbindDialerService() {
    	Globals.is_vd_running = false;
    	unbindService(mDsConnection);
    	mDialerService = null;
    }
    
    public void newMsisdn() {
    	final String METHOD = "::newMsisdn()   ";
    	if(mReceiverService != null) {
    		String suffix = (Globals.msisdn.length() < Globals.RIGHT_MATCH) ? 
					Globals.msisdn : 
					Globals.msisdn.substring(Globals.msisdn.length() - Globals.RIGHT_MATCH);
    		Bundle b = new Bundle();
    		b.putString(ReceiverService.EXTRA_SUFFIX, suffix);
    		Log.i(TAG + METHOD, "Sending new suffix.");
			Message msg = Message.obtain(null, ReceiverService.MSG_NEW_SUFFIX, 0, 0);
			msg.setData(b);
			try {
				mReceiverService.send(msg);
				Log.i(TAG + METHOD, "Message sent to client.");
			} catch (RemoteException e) {
				Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
			}
    	}
    	else {
    		// Dialer service case
    	}
    }
    
	/**
	 * Sends a message to the RemoteService or the DialingService.
	 * Includes a reply-to Messenger that the RemoteService / DialingService shall use
	 * to communicate with this activity.
	 * 
	 * @param what
	 * @param messenger
	 */
	public void sendMsg(Messenger toMsgr, int what, Messenger plyToMsgr) {
		final String METHOD = "::sendMsg()  ";
		
		Log.i(TAG + METHOD, "Sending message to client. What = " + Integer.toString(what));
		Message msg = Message.obtain(null, what, 0, 0);
		if(plyToMsgr != null) {
			msg.replyTo = plyToMsgr;
		}
		try {
			toMsgr.send(msg);
			Log.i(TAG + METHOD, "Message sent to client.");
		} catch (RemoteException e) {
			Log.d(TAG + METHOD, e.getClass().getName() + e.toString());
		}
	}
    
    private void sendToBg() {
    	Intent i = new Intent();
    	i.setAction(Intent.ACTION_MAIN);
    	i.addCategory(Intent.CATEGORY_HOME);
    	this.startActivity(i);
    }

}
