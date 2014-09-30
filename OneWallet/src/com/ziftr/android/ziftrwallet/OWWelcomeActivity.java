package com.ziftr.android.ziftrwallet;

import java.util.Arrays;

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
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class OWWelcomeActivity extends FragmentActivity implements OWNeutralDialogHandler{
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//make activity fullscreen
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		this.setContentView(R.layout.welcome);
		Button setPassword = (Button) this.findViewById(R.id.set_password);
		Button skipPassword = (Button) this.findViewById(R.id.skip_password);
		final EditText passphrase = (EditText) this.findViewById(R.id.new_password).findViewById(R.id.ow_editText);
		passphrase.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		final EditText confirmPassphrase = (EditText) this.findViewById(R.id.new_confirm_password).findViewById(R.id.ow_editText);
		confirmPassphrase.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		passphrase.clearFocus();
		confirmPassphrase.clearFocus();
		setPassword.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				byte[] inputHash = ZiftrUtils.Sha256Hash(passphrase.getText().toString().getBytes());
				byte[] confirmedHash = ZiftrUtils.Sha256Hash(confirmPassphrase.getText().toString().getBytes());

				if (Arrays.equals(inputHash, confirmedHash)){
					Intent main = new Intent(OWWelcomeActivity.this, OWMainFragmentActivity.class);
					main.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					main.putExtra("SET_PASSPHRASE", inputHash);
					startActivity(main);
					OWWelcomeActivity.this.finish();
				}else{
					OWSimpleAlertDialog alertDialog = new OWSimpleAlertDialog();

					alertDialog.setTargetFragment(null, OWRequestCodes.ALERT_USER_DIALOG);

					// Set negative text to null to not have negative button
					alertDialog.setupDialog("ziftrWALLET", "Your passphrases do not match!", null, "OK", null);
					alertDialog.show(getSupportFragmentManager(), "passphrases_dont_match");

				}
			}
		});

		skipPassword.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent main = new Intent(OWWelcomeActivity.this, 
						OWMainFragmentActivity.class);
				main.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(main);
				OWWelcomeActivity.this.finish();
			}
		});

	}

	@Override
	public void handleNeutral(int requestCode) {
		// TODO Auto-generated method stub
		//nothing to do
		
	}

	@Override
	public void handleNegative(int requestCode) {
		// TODO Auto-generated method stub
		//nothing to do
	}
}
