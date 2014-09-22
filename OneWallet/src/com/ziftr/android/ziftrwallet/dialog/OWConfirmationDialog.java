package com.ziftr.android.ziftrwallet.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.dialog.handlers.OWConfirmationDialogHandler;
import com.ziftr.android.ziftrwallet.dialog.handlers.OWValidatePassphraseDialogHandler;

/**
 * Dialogs where the app requests to get the passphrase
 * from the user.
 */
public class OWConfirmationDialog extends OWDialogFragment {
	/**
	 * Whenever this is fragment is attached to an activity 
	 * we must make sure that it is able to handle accepting 
	 * and cancelling.
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
				R.layout.dialog_confirmation, null));
		
		this.initDialogFields();

		builder.setView(this.getDialogView());
		
		Button cancel = (Button) this.getDialogView().findViewById(R.id.left_dialog_button);
		cancel.setOnClickListener(this);
		
		Button next = (Button) this.getDialogView().findViewById(R.id.right_dialog_button);
		next.setOnClickListener(this);

		return builder.create();
	}

	public void onClick(View view) {
		OWConfirmationDialogHandler handler = 
				(OWConfirmationDialogHandler) this.getHandler();
		
		switch(view.getId()) {
		case R.id.left_dialog_button:
			//CANCEL
			handler.handleNegative(this.getRequestCode());
			this.dismiss();
			break;
		case R.id.right_dialog_button:
			//CONTINUE
			handler.handleConfirmationPositive(this.getRequestCode(), this.getArguments());
			this.dismiss();
			break;
		}
	}

	@Override
	protected Object getHandler() {
		return this.getActivity();
	}
	
}