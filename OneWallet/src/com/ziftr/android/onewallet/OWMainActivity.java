package com.ziftr.android.onewallet;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

public class OWMainActivity extends ActionBarActivity {

	/**
	 * Loads up the views and starts the OWHomeFragment 
	 * if necessary. 
	 * 
	 * @param savedInstanceState - The stored bundle
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// If the app has just been launched (so the fragment
		// doesn't exist yet), we create the main fragment.
		if (savedInstanceState == null) {
			FragmentManager fragmentManager = getSupportFragmentManager();
			OWHomeFragment homeFragment = new OWHomeFragment();
			fragmentManager.beginTransaction().add(
					R.id.mainActivityContainer, homeFragment).commit();
		} 
	}

	/**
	 * Create the options menu. May want to get rid of this later.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/** 
	 * We handle action bar item clicks here. The action bar will
	 * automatically handle clicks on the Home/Up button, so long
	 * as you specify a parent activity in AndroidManifest.xml.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
