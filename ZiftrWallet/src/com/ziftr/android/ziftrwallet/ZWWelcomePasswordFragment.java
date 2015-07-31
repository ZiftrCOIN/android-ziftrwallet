/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */
package com.ziftr.android.ziftrwallet;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.ziftr.android.ziftrwallet.dialog.ZiftrDialogManager;
import com.ziftr.android.ziftrwallet.dialog.ZiftrSimpleDialogFragment;
import com.ziftr.android.ziftrwallet.dialog.ZiftrTextDialogFragment;
import com.ziftr.android.ziftrwallet.sqlite.ZWReceivingAddressesTable.EncryptionStatus;
import com.ziftr.android.ziftrwallet.sqlite.ZWWalletManager;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWWelcomePasswordFragment extends Fragment implements OnClickListener {

	private View rootView;
	private EditText passwordEditText;
	private EditText confirmPasswordEditText;

	private Button setPasswordButton;
	private Button skipPasswordButton;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		this.rootView = inflater.inflate(R.layout.welcome_passphrase, container, false);

		this.initializeViewFields();

		return this.rootView;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}

	/**
	 * Finds all the view fields and sets the listeners.  
	 */
	private void initializeViewFields() {
		setPasswordButton = (Button) rootView.findViewById(R.id.acceptTerms);
		skipPasswordButton = (Button) rootView.findViewById(R.id.skip_password);

		passwordEditText = (EditText) rootView.findViewById(R.id.new_password).findViewById(R.id.customEditText);
		passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

		confirmPasswordEditText = 
				(EditText) rootView.findViewById(R.id.new_confirm_password).findViewById(R.id.customEditText);

		confirmPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		confirmPasswordEditText.clearFocus();

		setPasswordButton.setOnClickListener(this);
		skipPasswordButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {

		final ZWWelcomeActivity welcomeActivity = (ZWWelcomeActivity) this.getActivity();
		// Need an activity to do pretty much everything below
		if (welcomeActivity == null) {
			return;
		}

		if (v == skipPasswordButton) {
			this.startNextScreen();
		}
		else if (v == setPasswordButton) {
			final String password = passwordEditText.getText().toString();
			String confirmPassword = confirmPasswordEditText.getText().toString();

			if (password.equals(confirmPassword)) {

				ZiftrUtils.runOnNewThread(new Runnable() {
					@Override
					public void run() {
						final EncryptionStatus status = ZWWalletManager.getInstance().changeEncryptionOfReceivingAddresses(null, password);
						//tried to set password on encrypted database private keys
						if (status == EncryptionStatus.ALREADY_ENCRYPTED || status == EncryptionStatus.ERROR){
							welcomeActivity.runOnUiThread(new Runnable(){
								@Override
								public void run() {
									if (status == EncryptionStatus.ALREADY_ENCRYPTED){
										ZiftrTextDialogFragment oldPasswordDialog = new ZiftrTextDialogFragment();
										oldPasswordDialog.setupDialog(R.string.zw_dialog_old_encryption);
										oldPasswordDialog.addEmptyTextbox(true);

										oldPasswordDialog.show(getFragmentManager(), ZWActivityDialogHandler.DIALOG_OLD_PASSWORD_TAG);	
									} 
									else {
										ZiftrDialogManager.showSimpleAlert(getFragmentManager(), R.string.zw_dialog_old_encryption_fatal);
									}
								}

							});

						} 
						else {
							//set password
							if (ZWPreferences.setStoredPassword(password) <= 0) {
								//if we failed setting password, something could be wrong with db
								ZiftrSimpleDialogFragment dbErrorFragment = new ZiftrTextDialogFragment();
								dbErrorFragment.setupDialog(R.string.zw_app_name, 
										R.string.zw_dialog_database_error, 
										R.string.zw_dialog_restart, 
										R.string.zw_dialog_cancel);

								dbErrorFragment.show(getFragmentManager(), ZWActivityDialogHandler.DIALOG_DATABASE_ERROR_TAG);
							}
						}
					}
				});

				this.startNextScreen();

			} 
			else {
				ZiftrDialogManager.showSimpleAlert(getFragmentManager(), R.string.zw_dialog_new_password_match);
			}
		}
	}

	private void startNextScreen() {
		ZWWelcomeActivity welcomeActivity = (ZWWelcomeActivity) this.getActivity();
		welcomeActivity.showNameFragment(false);
	}

}