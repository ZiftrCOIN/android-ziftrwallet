package com.ziftr.android.ziftrwallet;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Window;

import com.ziftr.android.ziftrwallet.dialog.ZWDialogFragment;
import com.ziftr.android.ziftrwallet.dialog.ZWSimpleAlertDialog;
import com.ziftr.android.ziftrwallet.dialog.handlers.ZWNeutralDialogHandler;
import com.ziftr.android.ziftrwallet.fragment.ZWRequestCodes;

public class ZWWelcomeActivity extends FragmentActivity implements ZWNeutralDialogHandler {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// To make activity fullscreen
		// Have to do this before setContentView so that we don't get a AndroidRuntimeException
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.welcome);
		
		if (savedInstanceState == null) {
			if (!ZWPreferencesUtils.userHasPassphrase() && !ZWPreferencesUtils.getPassphraseWarningDisabled()) {
				this.openPassphraseFragment();
			} else if (!ZWPreferencesUtils.userHasSetName() && !ZWPreferencesUtils.getDisabledName()) {
				this.openNameFragment(false);
			} else {
				throw new RuntimeException(
						"Shouldn't be in the welcome screen if user already has passphrase and set name!");
			}
		}
	}

	/**
	 * 
	 */
	public void openPassphraseFragment() {
		this.openFragment(new ZWWelcomePassphraseFragment(), false, false);
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
		tx.replace(R.id.welcomeScreenFragmentContainer, fragToOpen, "welcome_passphrase_fragment");
		tx.commit();
	}

	@Override
	public void handleNeutral(int requestCode) {
		// Nothing to do
	}

	@Override
	public void handleNegative(int requestCode) {
		// Nothing to do
	}
	
	
	/**
	 * @param a
	 */
	protected void alert(String message, String tag) {
		ZWSimpleAlertDialog alertDialog = new ZWSimpleAlertDialog();
		Bundle b = new Bundle();
		b.putInt(ZWDialogFragment.REQUEST_CODE_KEY, ZWRequestCodes.ALERT_USER_DIALOG);

		// Set negative text to null to not have negative button
		alertDialog.setupDialog("ziftrWALLET", message, null, "OK", null);
		alertDialog.show(this.getSupportFragmentManager(), tag);
	}
	
	protected void startZWMainActivity() {
		Intent main = new Intent(this, ZWMainFragmentActivity.class);
		main.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(main);
		this.finish();
	}
	
}
