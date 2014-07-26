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

}
