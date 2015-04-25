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

import com.ziftr.android.ziftrwallet.dialog.ZiftrDialogFragment;
import com.ziftr.android.ziftrwallet.dialog.ZiftrDialogManager;
import com.ziftr.android.ziftrwallet.dialog.ZiftrTextDialogFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWTags;
import com.ziftr.android.ziftrwallet.sqlite.ZWReceivingAddressesTable.reencryptionStatus;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWWelcomePassphraseFragment extends Fragment implements OnClickListener {

	private View rootView;
	private EditText passwordEditText;
	private EditText confirmPassphraseEditText;

	private Button setPassphraseButton;
	private Button skipPassphraseButton;


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
		setPassphraseButton = (Button) rootView.findViewById(R.id.set_password);
		skipPassphraseButton = (Button) rootView.findViewById(R.id.skip_password);

		passwordEditText = (EditText) rootView.findViewById(R.id.new_password).findViewWithTag(ZWTags.ZW_EDIT_TEXT);
		passwordEditText.setId(R.id.zw_welcome_passphrase_1);
		passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

		confirmPassphraseEditText = (EditText) rootView.findViewById(R.id.new_confirm_password).findViewWithTag(ZWTags.ZW_EDIT_TEXT);
		confirmPassphraseEditText.setId(R.id.zw_welcome_passphrase_2);
		confirmPassphraseEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		confirmPassphraseEditText.clearFocus();

		setPassphraseButton.setOnClickListener(this);
		skipPassphraseButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {

		final ZWWelcomeActivity welcomeActivity = (ZWWelcomeActivity) this.getActivity();
		// Need an activity to do pretty much everything below
		if (welcomeActivity == null) {
			return;
		}

		if (v == skipPassphraseButton) {
			this.startNextScreen();
		}
		else if (v == setPassphraseButton) {
			final String password = passwordEditText.getText().toString();
			String confirmPassphrase = confirmPassphraseEditText.getText().toString();

			if (password.equals(confirmPassphrase)) {

				ZiftrUtils.runOnNewThread(new Runnable() {
					@Override
					public void run() {
						final reencryptionStatus status = ZWWalletManager.getInstance().changeEncryptionOfReceivingAddresses(null, password);
						//tried to set passphrase on encrypted database private keys
						if (status == reencryptionStatus.encrypted || status == reencryptionStatus.error){
							welcomeActivity.runOnUiThread(new Runnable(){
								@Override
								public void run() {
									if (status == reencryptionStatus.encrypted){
										ZiftrTextDialogFragment oldPasswordDialog = new ZiftrTextDialogFragment();
										oldPasswordDialog.setupDialog(R.string.zw_dialog_old_encryption);
										oldPasswordDialog.setupTextboxes();
										
										oldPasswordDialog.show(getFragmentManager(), ZWActivityDialogHandler.DIALOG_OLD_PASSWORD_TAG);	
									} 
									else {
										ZiftrDialogManager.showSimpleAlert(getFragmentManager(), R.string.zw_dialog_old_encryption_fatal);
									}
								}
								
							});

						} 
						else {
							//set passphrase
							String saltedHash = ZiftrUtils.saltedHashString(password);
							
							
							if (ZWPreferences.setStoredPasswordHash(saltedHash) == -1){
								//if we failed setting passphrase, something could be wrong with db
								ZiftrDialogFragment dbErrorFragment = new ZiftrTextDialogFragment();
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
		if (ZWPreferences.userHasSetName() || ZWPreferences.getDisabledName()) {
			welcomeActivity.startZWMainActivity();
		} else {
			welcomeActivity.openNameFragment(true);
		}
	}

}