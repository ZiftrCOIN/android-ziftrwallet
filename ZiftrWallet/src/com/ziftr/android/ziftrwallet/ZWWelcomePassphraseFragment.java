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

import com.ziftr.android.ziftrwallet.fragment.ZWRequestCodes;
import com.ziftr.android.ziftrwallet.fragment.ZWTags;
import com.ziftr.android.ziftrwallet.sqlite.ZWReceivingAddressesTable.reencryptionStatus;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWWelcomePassphraseFragment extends Fragment implements OnClickListener {

	private View rootView;
	private EditText passphraseEditText;
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

		passphraseEditText = (EditText) rootView.findViewById(R.id.new_password).findViewWithTag(ZWTags.ZW_EDIT_TEXT);
		passphraseEditText.setId(R.id.zw_welcome_passphrase_1);
		passphraseEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

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
		} else if (v == setPassphraseButton) {
			final String passphrase = passphraseEditText.getText().toString();
			String confirmPassphrase = confirmPassphraseEditText.getText().toString();

			if (passphrase.equals(confirmPassphrase)) {
				if (!passphrase.isEmpty()) {
					
					ZiftrUtils.runOnNewThread(new Runnable() {
						@Override
						public void run() {
							final reencryptionStatus status = ZWWalletManager.getInstance().changeEncryptionOfReceivingAddresses(null, passphrase);
							//tried to set passphrase on encrypted database private keys
							if (status == reencryptionStatus.encrypted || status == reencryptionStatus.error){
								welcomeActivity.runOnUiThread(new Runnable(){
									@Override
									public void run() {
										if (status == reencryptionStatus.encrypted){
											welcomeActivity.alertPassphraseDialog(ZWRequestCodes.PASSPHRASE_FOR_DECRYPTING, new Bundle(), 
													"pass_for_old_encrypted_keys", "Your keys have already been encrypted with a passphrase, please enter your old passphrase:");
										} else {
											welcomeActivity.alert("We've encountered a fatal error in your database, please contact customer service!",  
													"inconsistent_encrypted_database");
										}
									}
									
								});

							} else {
								//set passphrase
								String saltedHash = ZiftrUtils.saltedHashString(passphrase);
								ZWPreferencesUtils.setStoredPassphraseHash(saltedHash);
							}
						}
					});
					this.startNextScreen();
				} else {
					welcomeActivity.alert("Your passphrase may not be empty. This is not secure!", 
							"passphrase_is_empty_string");
				}
			} else {
				welcomeActivity.alert("Your passphrases do not match!", 
						"passphrases_dont_match");
			}
		}
	}
	
	private void startNextScreen() {
		ZWWelcomeActivity welcomeActivity = (ZWWelcomeActivity) this.getActivity();
		if (ZWPreferencesUtils.userHasSetName() || ZWPreferencesUtils.getDisabledName()) {
			welcomeActivity.startZWMainActivity();
		} else {
			welcomeActivity.openNameFragment(true);
		}
	}

}