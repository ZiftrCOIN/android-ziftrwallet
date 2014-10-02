package com.ziftr.android.ziftrwallet;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Window;
import android.view.WindowManager;

import com.ziftr.android.ziftrwallet.dialog.OWDialogFragment;
import com.ziftr.android.ziftrwallet.dialog.OWSimpleAlertDialog;
import com.ziftr.android.ziftrwallet.dialog.handlers.OWNeutralDialogHandler;
import com.ziftr.android.ziftrwallet.util.OWPreferencesUtils;
import com.ziftr.android.ziftrwallet.util.OWRequestCodes;

public class OWWelcomeActivity extends FragmentActivity implements OWNeutralDialogHandler {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// To make activity fullscreen
		// Have to do this before setContentView so that we don't get a AndroidRuntimeException
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		this.setContentView(R.layout.welcome);
		
		if (savedInstanceState == null) {
			if (!OWPreferencesUtils.userHasPassphrase(this)) {
				this.openPassphraseFragment();
			} else if (!OWPreferencesUtils.userHasSetName(this)) {
				this.openNameFragment(null);
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
		this.openFragment(new OWWelcomePassphraseFragment(), false);
	}
	
	/**
	 * 
	 */
	public void openNameFragment(Bundle resultsOfPassphraseFragment) {
		Fragment welcomeFrag = new OWWelcomeNameFragment();
		if (resultsOfPassphraseFragment != null) {
			welcomeFrag.setArguments(resultsOfPassphraseFragment);
		}
		this.openFragment(welcomeFrag, true);
	}
	
	/**
	 * 
	 */
	private void openFragment(Fragment fragToOpen, boolean addTransition) {
		FragmentTransaction tx = this.getSupportFragmentManager().beginTransaction();
		if (addTransition) {
			tx.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left, R.anim.slide_in_right, R.anim.slide_out_right);
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
		OWSimpleAlertDialog alertDialog = new OWSimpleAlertDialog();
		Bundle b = new Bundle();
		b.putInt(OWDialogFragment.REQUEST_CODE_KEY, OWRequestCodes.ALERT_USER_DIALOG);

		// Set negative text to null to not have negative button
		alertDialog.setupDialog("ziftrWALLET", message, null, "OK", null);
		alertDialog.show(this.getSupportFragmentManager(), tag);
	}
	
	protected void startOWMainActivity(Bundle extras) {
		Intent main = new Intent(this, OWMainFragmentActivity.class);
		if (extras != null) {
			main.putExtras(extras);
		}
		main.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(main);
		this.finish();
	}
	
}
