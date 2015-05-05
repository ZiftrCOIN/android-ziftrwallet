/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */
package com.ziftr.android.ziftrwallet;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Window;

import com.ziftr.android.ziftrwallet.dialog.ZiftrDialogManager;
import com.ziftr.android.ziftrwallet.util.ZLog;

public class ZWWelcomeActivity extends FragmentActivity {

	private ZWActivityDialogHandler dialogHandler;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		dialogHandler = new ZWActivityDialogHandler(this);
		ZiftrDialogManager.registerHandler(dialogHandler);

		// To make activity fullscreen
		// Have to do this before setContentView so that we don't get a AndroidRuntimeException
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.welcome);
		
		if (savedInstanceState == null) {
			showTermsFragment();
		}
	}
	
	
	public void showTermsFragment() {
		if(ZWPreferences.userAgreedToTos()) {
			//move on to the password screen if we don't need the terms screen
			showPasswordFragment(true);
		}
		else {
			showFragment(new ZWWelcomeTermsFragment(), true);
		}
		
	}
	
	
	public void showPasswordFragment(boolean fromSplashScreen) {
		if (ZWPreferences.userHasPassword() || ZWPreferences.getPasswordWarningDisabled()) {
			//move on to the name screen if we don't need to show the password screen
			showNameFragment(fromSplashScreen);
		}
		else {
			this.showFragment(new ZWWelcomePasswordFragment(), fromSplashScreen);
		}
	}
	
	
	public void showNameFragment(boolean fromSplashScreen) {
		if (ZWPreferences.userHasSetName() || ZWPreferences.getDisabledName()) {
			//start real activity if we don't need to show the name screen
			startZWMainActivity();
		}
		else {
			this.showFragment(new ZWWelcomeNameFragment(), fromSplashScreen);
		}
			
		
	}
	
	
	private void showFragment(Fragment fragment, boolean fromSplashScreen) { 
		FragmentTransaction tx = this.getSupportFragmentManager().beginTransaction();
		if (!fromSplashScreen) {
			tx.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left, R.anim.slide_in_right, R.anim.slide_out_right);
			tx.addToBackStack("");
		}
		
		tx.replace(R.id.welcomeScreenFragmentContainer, fragment, "welcome_password_fragment");
		tx.commit();
	}
	
	protected void startZWMainActivity() {
		Intent main = new Intent(this, ZWMainFragmentActivity.class);
		main.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(main);
		this.finish();
	}

}
