package com.ziftr.android.ziftrwallet;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Window;

import com.ziftr.android.ziftrwallet.dialog.ZWDialogFragment;
import com.ziftr.android.ziftrwallet.dialog.ZWSimpleAlertDialog;
import com.ziftr.android.ziftrwallet.dialog.ZWValidatePassphraseDialog;
import com.ziftr.android.ziftrwallet.dialog.handlers.ZWNeutralDialogHandler;
import com.ziftr.android.ziftrwallet.dialog.handlers.ZWValidatePassphraseDialogHandler;
import com.ziftr.android.ziftrwallet.fragment.ZWRequestCodes;
import com.ziftr.android.ziftrwallet.util.ZLog;

public class ZWWelcomeActivity extends FragmentActivity implements ZWNeutralDialogHandler, ZWValidatePassphraseDialogHandler {

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
				ZLog.log(
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
		if (requestCode == ZWRequestCodes.UPSERT_DB_ERROR){
			//terminate app if error setting password
			System.exit(0);
		}
	}

	@Override
	public void handleNegative(int requestCode) {
		// Nothing to do
	}
	
	protected void alert(String message, String tag, int requestCode) {
		ZWSimpleAlertDialog alertDialog = new ZWSimpleAlertDialog();
		Bundle b = new Bundle();
		b.putInt(ZWDialogFragment.REQUEST_CODE_KEY, requestCode);
		alertDialog.setArguments(b);

		// Set negative text to null to not have negative button
		alertDialog.setupDialog("ziftrWALLET", message, null, "OK", null);
		alertDialog.show(this.getSupportFragmentManager(), tag);
	}
	
	protected void alert(String message, String tag) {
		this.alert(message, tag, ZWRequestCodes.ALERT_USER_DIALOG);
	}
	
	protected void alertPassphraseDialog(int requestcode, Bundle args, String tag, String message){
		ZWValidatePassphraseDialog passphraseDialog = new ZWValidatePassphraseDialog();

		args.putInt(ZWDialogFragment.REQUEST_CODE_KEY, requestcode);
		passphraseDialog.setArguments(args);

		passphraseDialog.setupDialog("ziftrWALLET", message, "Continue", null, "Cancel");
		passphraseDialog.show(this.getSupportFragmentManager(), tag);
	}
	
	protected void startZWMainActivity() {
		Intent main = new Intent(this, ZWMainFragmentActivity.class);
		main.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(main);
		this.finish();
	}

	@Override
	public void handlePassphrasePositive(int requestCode, String passphrase) {
		switch(requestCode) {
			case ZWRequestCodes.PASSPHRASE_FOR_DECRYPTING:

				if (ZWWalletManager.getInstance().attemptDecrypt(passphrase)){
					//the passphrase worked, decrypt all keys now
					ZWWalletManager.getInstance().changeEncryptionOfReceivingAddresses(passphrase, null);
					alert("Your passphrase was correct! Your new passphrase was not set! Please settings to re-encrypt your data", "correct_passphrase");
				} else {
					alert("Your passphrase was incorrect. Warning: your database is still encrypted with the old passphrase.", "incorrect_passphrase");
				}
			break;
		}
	}
	
}
