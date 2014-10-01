package com.ziftr.android.ziftrwallet;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;

import com.google.bitcoin.utils.BriefLogFormatter;
import com.ziftr.android.ziftrwallet.util.OWPreferencesUtils;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

/**
 * This activity is responsible for loading the splash screen and then,
 * one second later, starting the app. It also can load different screens 
 * based on whether or not we are compiling for a tablet.  
 */
public class OWSplashScreenActivity extends FragmentActivity {

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

		// Temporary Bitcoinj logging
		BriefLogFormatter.initVerbose();

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
	 * we switch over to the {@link OWMainFragmentActivity} activity.
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
		if (!OWPreferencesUtils.userHasPassphrase(this) && !OWPreferencesUtils.getPassphraseDisabled(this)) {
			Intent noFragmentIntent = new Intent(OWSplashScreenActivity.this, OWWelcomeActivity.class);
			noFragmentIntent.setFlags(noFragmentIntent.getFlags());
			startActivity(noFragmentIntent);
		} else {
			Intent noFragmentIntent = new Intent(OWSplashScreenActivity.this, OWMainFragmentActivity.class);
			noFragmentIntent.setFlags(noFragmentIntent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
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
