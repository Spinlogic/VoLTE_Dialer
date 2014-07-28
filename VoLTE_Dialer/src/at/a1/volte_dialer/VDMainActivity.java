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

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.os.Build;
import at.a1.volte_dialer.dialer.DialerService;

public class VDMainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_volte_dialer_main);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}
	
	@Override
	public void onResume() {
	    super.onResume();
	    String btn_text = ""; 
	    if(Globals.is_vd_running) {
	    	btn_text = getResources().getText(R.string.btn_stop).toString();
	    }
	    else {
	    	btn_text = getResources().getText(R.string.btn_start).toString();
	    }
	    Button button = (Button) findViewById(R.id.startstop_button);
	    button.setText(btn_text);
	    if(Globals.msisdn == null || Globals.msisdn.length() < 3) {
	    	button.setEnabled(false);
	    }
	    else {
	    	button.setEnabled(true);
	    }
	}
	
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.volte_dialer_main, menu);
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

}
