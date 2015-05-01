/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */
package com.ziftr.android.ziftrwallet;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;

import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

/**
 * This activity is responsible for loading the splash screen and then,
 * one second later, starting the app. It also can load different screens 
 * based on whether or not we are compiling for a tablet.  
 */
public class ZWSplashScreenActivity extends FragmentActivity {

	private boolean started = false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// First just set view to splash screen
		this.setContentView(R.layout.splash_layout);

		// Here we just load the splash screen and then start the app one
		// second later.
		if (savedInstanceState !=null && savedInstanceState.containsKey("started")) {
			this.started = savedInstanceState.getBoolean("started");
		}

		if (!this.started) {
			this.started = true;
			loadAppOneSecondLater();
		}

	}

	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("started", this.started);
		super.onSaveInstanceState(outState);
	}

	/**
	 * The splash screen stays open for one second and then
	 * we switch over to the {@link ZWMainFragmentActivity} activity.
	 */
	private void loadAppOneSecondLater() {
		ZiftrUtils.runOnNewThread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					ZLog.log("Cut out early....\nCut out early....\nCut out early....");
					e.printStackTrace();
				}

				buildAndInitUi();
				finish();
			}
		});
	}

	@Override
	public void onBackPressed() {
		// To block back button press during loading screen, we override this
		// and do not make any call to super.onBackPressed();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// We want the only thing on the screen to be the splash screen,
		// so we disable the options menu.
		return false;
	}

	public void buildAndInitUi() {

		//TODO Currently we only have one version of the UI that should work 
		// on all devices. This selection code will let us have multiple versions 
		// of the UI with the same code base if we want. All we will have to do
		// is change the value of isTablet in flags.xml and recompile for tablet.

		boolean isTablet = getResources().getBoolean(R.bool.isTablet);

		if (isTablet) {
			loadTabletUi();
		} else {
			loadFragmentBasedUi();
		}
	}


	private void loadNoFragmentUi() {
		
		boolean existingPassword = ZWPreferences.userHasPassword();
		boolean passwordWarningDisabled = ZWPreferences.getPasswordWarningDisabled();
		boolean existingUserName = ZWPreferences.userHasSetName();
		boolean userNameDisabled = ZWPreferences.getDisabledName();
		
		boolean passwordSetup = existingPassword || passwordWarningDisabled;
		boolean userNameSetup = existingUserName || userNameDisabled;
		
		if(passwordSetup && userNameSetup) {
			//if the user has both their password and username setup (or properly disabled) go directly to wallet
			Intent noFragmentIntent = new Intent(ZWSplashScreenActivity.this, ZWMainFragmentActivity.class);
			noFragmentIntent.setFlags(noFragmentIntent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(noFragmentIntent);
		}
		else {
			//otherwise, bug them about username/password
			Intent noFragmentIntent = new Intent(ZWSplashScreenActivity.this, ZWWelcomeActivity.class);
			startActivity(noFragmentIntent);
		}

	}

	private void loadFragmentBasedUi() {
		//TODO Load different UI for devices that can use fragments and action bars
		loadNoFragmentUi();
	}

	private void loadTabletUi() {
		//TODO Implement different UI for tablets
		loadNoFragmentUi();
	}

}
