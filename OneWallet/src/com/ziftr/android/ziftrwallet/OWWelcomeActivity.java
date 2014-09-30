package com.ziftr.android.ziftrwallet;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.ziftr.android.ziftrwallet.dialog.OWDialogFragment;
import com.ziftr.android.ziftrwallet.dialog.OWSimpleAlertDialog;
import com.ziftr.android.ziftrwallet.dialog.handlers.OWNeutralDialogHandler;
import com.ziftr.android.ziftrwallet.util.OWRequestCodes;

public class OWWelcomeActivity extends FragmentActivity 
implements OnClickListener, OWNeutralDialogHandler {

	private EditText passphraseEditText;
	private EditText confirmPassphraseEditText;

	private Button setPassphraseButton;
	private Button skipPassphraseButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// To make activity fullscreen
		// Have to do this before setContentView so that we don't get a AndroidRuntimeException
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		this.setContentView(R.layout.welcome);

		this.initializeViewFields();
	}

	/**
	 * Finds all the view fields and sets the listeners.  
	 */
	private void initializeViewFields() {
		setPassphraseButton = (Button) this.findViewById(R.id.set_password);
		skipPassphraseButton = (Button) this.findViewById(R.id.skip_password);

		passphraseEditText = (EditText) this.findViewById(R.id.new_password).findViewById(R.id.ow_editText);
		passphraseEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		confirmPassphraseEditText = (EditText) this.findViewById(R.id.new_confirm_password).findViewById(R.id.ow_editText);

		confirmPassphraseEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		confirmPassphraseEditText.clearFocus();

		setPassphraseButton.setOnClickListener(this);
		skipPassphraseButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if (v == skipPassphraseButton) {
			Intent main = new Intent(OWWelcomeActivity.this, OWMainFragmentActivity.class);
			main.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(main);
			OWWelcomeActivity.this.finish();
		} else if (v == setPassphraseButton) {
			String passphrase = passphraseEditText.getText().toString();
			String confirmPassphrase = confirmPassphraseEditText.getText().toString();

			if (passphrase.equals(confirmPassphrase)) {
				if (passphrase.length() > 0){
					Intent main = new Intent(OWWelcomeActivity.this, OWMainFragmentActivity.class);
					main.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					main.putExtra(OWPreferencesUtils.BUNDLE_PASSPHRASE_KEY, passphrase);
					startActivity(main);
					OWWelcomeActivity.this.finish();
				} else {
					OWSimpleAlertDialog alertDialog = new OWSimpleAlertDialog();
					Bundle b = new Bundle();
					b.putInt(OWDialogFragment.REQUEST_CODE_KEY, OWRequestCodes.ALERT_USER_DIALOG);

					// Set negative text to null to not have negative button
					alertDialog.setupDialog("ziftrWALLET", "An empty passphrase is the same as no passphrase!", null, "OK", null);
					alertDialog.show(getSupportFragmentManager(), "passphrase_is_empty_string");

				}
			} else {
				OWSimpleAlertDialog alertDialog = new OWSimpleAlertDialog();
				Bundle b = new Bundle();
				b.putInt(OWDialogFragment.REQUEST_CODE_KEY, OWRequestCodes.ALERT_USER_DIALOG);

				// Set negative text to null to not have negative button
				alertDialog.setupDialog("ziftrWALLET", "Your passphrases do not match!", null, "OK", null);
				alertDialog.show(getSupportFragmentManager(), "passphrases_dont_match");
			}
		}
	}

	@Override
	public void handleNeutral(int requestCode) {
		// Nothing to do
	}

	@Override
	public void handleNegative(int requestCode) {
		// Nothing to do
	}

}
