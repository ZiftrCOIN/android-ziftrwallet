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

import com.ziftr.android.ziftrwallet.util.OWPreferencesUtils;
import com.ziftr.android.ziftrwallet.util.OWTags;

public class OWWelcomePassphraseFragment extends Fragment implements OnClickListener {

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

		passphraseEditText = (EditText) rootView.findViewById(R.id.new_password).findViewWithTag(OWTags.OW_EDIT_TEXT);
		passphraseEditText.setId(R.id.ow_welcome_passphrase_1);
		passphraseEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

		confirmPassphraseEditText = (EditText) rootView.findViewById(R.id.new_confirm_password).findViewWithTag(OWTags.OW_EDIT_TEXT);
		confirmPassphraseEditText.setId(R.id.ow_welcome_passphrase_2);
		confirmPassphraseEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		confirmPassphraseEditText.clearFocus();

		setPassphraseButton.setOnClickListener(this);
		skipPassphraseButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {

		OWWelcomeActivity welcomeActivity = (OWWelcomeActivity) this.getActivity();
		// Need an activity to do pretty much everything below
		if (welcomeActivity == null) {
			return;
		}

		if (v == skipPassphraseButton) {
			this.startNextScreen(null);
		} else if (v == setPassphraseButton) {
			String passphrase = passphraseEditText.getText().toString();
			String confirmPassphrase = confirmPassphraseEditText.getText().toString();

			if (passphrase.equals(confirmPassphrase)) {
				if (!passphrase.isEmpty()) {
					this.startNextScreen(passphrase);
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

	private void startNextScreen(String passphrase) {
		Bundle b = null;
		if (passphrase != null) {
			b = new Bundle();
			b.putString(OWPreferencesUtils.BUNDLE_PASSPHRASE_KEY, passphrase);
		}

		OWWelcomeActivity welcomeActivity = (OWWelcomeActivity) this.getActivity();
		if (OWPreferencesUtils.userHasSetName(welcomeActivity)) {
			welcomeActivity.startOWMainActivity(b);
		} else {
			welcomeActivity.openNameFragment(b);
		}
	}

}