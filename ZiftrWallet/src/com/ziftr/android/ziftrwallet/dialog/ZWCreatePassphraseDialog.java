package com.ziftr.android.ziftrwallet.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.dialog.handlers.ZWResetPassphraseDialogHandler;
import com.ziftr.android.ziftrwallet.dialog.handlers.ZWValidatePassphraseDialogHandler;

/**
 * Dialogs where the app requests to sets the passphrase
 */
public class ZWCreatePassphraseDialog extends ZWDialogFragment {
	
	/**
	 * Whenever this is fragment is attached to an activity 
	 * we must make sure that it is able to handle accepting 
	 * and cancelling from passphrase dialogs.
	 * 
	 * This method throws an exception if neither the newly attached activity nor 
	 * the target fragment are instances of {@link ZWValidatePassphraseDialogHandler}.
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.validateHandler(ZWValidatePassphraseDialogHandler.class);
	}

	/**
	 * Creates and returns the dialog to show the user.
	 * Sets all the basic text fields that all dialogs have 
	 * to be what this dialog was set up with. 
	 */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = this.createBuilder(savedInstanceState);

		this.setDialogView(this.getActivity().getLayoutInflater().inflate(
				R.layout.dialog_create_passphrase, null));
		this.initDialogFields();

		builder.setView(this.getDialogView());
		
		Button cancel = (Button) this.getDialogView().findViewById(R.id.left_dialog_button);
		cancel.setOnClickListener(this);
		
		Button next = (Button) this.getDialogView().findViewById(R.id.right_dialog_button);
		next.setOnClickListener(this);

		return builder.create();
	}

	public void onClick(View view) {
		ZWResetPassphraseDialogHandler handler = 
				(ZWResetPassphraseDialogHandler) this.getHandler();
		
		switch(view.getId()) {
		case R.id.left_dialog_button:
			//CANCEL
			handler.handleNegative(this.getRequestCode());
			this.dismiss();
			break;
		case R.id.right_dialog_button:
			//CONTINUE
			handler.handleResetPassphrasePositive(this.getRequestCode(), null,
					this.getStringFromTextView(R.id.new_passphrase_dialog),
					this.getStringFromTextView(R.id.new_passphrase_dialog_confirm));
			this.dismiss();
			break;
		}
	}

	@Override
	protected Object getHandler() {
		return this.getActivity();
	}
	
	@Override
	public void dismiss() {
		this.closeKeyboard(R.id.new_passphrase_dialog);
		super.dismiss();
	}

}