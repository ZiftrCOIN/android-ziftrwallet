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
			if (!ZWPreferences.userHasPassword() && !ZWPreferences.getPasswordWarningDisabled()) {
				this.openPasswordFragment();
			} else if (!ZWPreferences.userHasSetName() && !ZWPreferences.getDisabledName()) {
				this.openNameFragment(false);
			} else {
				ZLog.log(
						"Shouldn't be in the welcome screen if user already has password and set name!");
			}
		}
	}
	
	/**
	 * 
	 */
	public void openPasswordFragment() {
		this.openFragment(new ZWWelcomePasswordFragment(), false, false);
	}
	
	/**
	 * 
	 */
	public void openNameFragment(boolean addToBackStack) {
		Fragment welcomeFrag = new ZWWelcomeNameFragment();
		this.openFragment(welcomeFrag, true, addToBackStack);
	}
	
	/**
	 * 
	 */
	private void openFragment(Fragment fragToOpen, boolean addTransition, boolean addToBackStack) {
		FragmentTransaction tx = this.getSupportFragmentManager().beginTransaction();
		if (addTransition) {
			tx.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left, R.anim.slide_in_right, R.anim.slide_out_right);
		}
		if  (addToBackStack){
			tx.addToBackStack("");
		}
		tx.replace(R.id.welcomeScreenFragmentContainer, fragToOpen, "welcome_password_fragment");
		tx.commit();
	}
	
	protected void startZWMainActivity() {
		Intent main = new Intent(this, ZWMainFragmentActivity.class);
		main.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(main);
		this.finish();
	}
	
}
