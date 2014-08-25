package com.ziftr.android.onewallet.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.dialog.handlers.OWValidatePassphraseDialogHandler;

/**
 * Dialogs where the app requests to get the passphrase
 * from the user.
 */
public class OWValidatePassphraseDialog extends OWDialogFragment {

	/** The key to save the text in the box. */
	private static final String CURRENT_ENTERED_TEXT_KEY = "entered_text";

	/**
	 * Whenever this is fragment is attached to an activity 
	 * we must make sure that it is able to handle accepting 
	 * and cancelling from passphrase dialogs.
	 * 
	 * This method throws an exception if neither the newly attached activity nor 
	 * the target fragment are instances of {@link OWValidatePassphraseDialogHandler}.
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.validateHandler(OWValidatePassphraseDialogHandler.class);
	}

	/**
	 * Creates and returns the dialog to show the user.
	 * Sets all the basic text fields that all dialogs have 
	 * to be what this diaglog was set up with. 
	 */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = this.createBuilder(savedInstanceState);

		this.setDialogView(this.getActivity().getLayoutInflater().inflate(
				R.layout.dialog_new_passphrase, null));
		this.initDialogFields();

		builder.setView(this.getDialogView());
		
		Button cancel = (Button) this.getDialogView().findViewById(R.id.left_dialog_button);
		cancel.setOnClickListener(this);
		
		Button next = (Button) this.getDialogView().findViewById(R.id.right_dialog_button);
		next.setOnClickListener(this);


		if (savedInstanceState != null) {
			if (savedInstanceState.getString(CURRENT_ENTERED_TEXT_KEY) != null) {
				this.setStringInEditText(R.id.textbox_passphrase, 
						savedInstanceState.getString(CURRENT_ENTERED_TEXT_KEY));
			}
		}
		return builder.create();
	}

	public void onClick(View view){
		OWValidatePassphraseDialogHandler handler = 
				(OWValidatePassphraseDialogHandler) this.getActivity();
		
		switch(view.getId()) {
		case R.id.left_dialog_button:
			//CANCEL
			handler.handleNegative(this.getTargetRequestCode());
			this.dismiss();
			break;
		case R.id.right_dialog_button:
			//CONTINUE
			handler.handlePassphrasePositive(this.getTargetRequestCode(), 
					this.getBytesFromEditText(R.id.textbox_passphrase), 
					this.getArguments());
			this.dismiss();
			break;
		}
	}

	/**
	 * When we save the instance, in addition to doing everything that
	 * all dialogs must do, we also have to store the current entered 
	 * text in the 
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		// Save all of the important strings in the dialog
		outState.putString(CURRENT_ENTERED_TEXT_KEY, 
				this.getStringFromEditText(R.id.textbox_passphrase));
	}

	@Override
	protected Object getHandler() {
		return this.getActivity();
	}

}