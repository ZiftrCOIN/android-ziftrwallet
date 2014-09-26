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

		this.setContentView(R.layout.welcome);

		// To make activity fullscreen
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

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

		// TODO this should be in XML
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
		} else if (v == setPassphraseButton) {
			String passphrase = passphraseEditText.getText().toString();
			String confirmPassphrase = confirmPassphraseEditText.getText().toString();

			if (passphrase.equals(confirmPassphrase)) {
				Intent main = new Intent(OWWelcomeActivity.this, OWMainFragmentActivity.class);
				main.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				main.putExtra("SET_PASSPHRASE", passphrase);
				startActivity(main);
			} else {
				OWSimpleAlertDialog alertDialog = new OWSimpleAlertDialog();

				alertDialog.setTargetFragment(null, OWRequestCodes.ALERT_USER_DIALOG);

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
