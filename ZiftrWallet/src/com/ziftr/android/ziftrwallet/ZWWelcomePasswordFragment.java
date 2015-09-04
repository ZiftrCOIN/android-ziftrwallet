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

						ZWActivityDialogHandler.changePassword(welcomeActivity, null, password);
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