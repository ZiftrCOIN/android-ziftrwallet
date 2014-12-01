package com.ziftr.android.ziftrwallet.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.dialog.handlers.ZWResetPassphraseDialogHandler;

/**
 * Dialogs where the app requests to get the passphrase
 * from the user.
 */
public class ZWResetPassphraseDialog extends ZWDialogFragment {

	/** The key to save the text in the previous passphrase box. */
	private static final String OLD_PASSPHRASE_KEY = "OLD_PASSPHRASE_KEY";
	/** The key to save the text in the new passphrase box. */
	private static final String NEW_PASSPHRASE_KEY = "NEW_PASSPHRASE_KEY";
	/** The key to save the text in the confirm passphrase box. */
	private static final String CONFIRM_PASSPHRASE_KEY = "CONFIRM_PASSPHRASE_KEY";
	
	/**
	 * Whenever this is fragment is attached to an activity 
	 * we must make sure that it is able to handle accepting 
	 * and cancelling from reset passphrase dialogs.
	 * 
	 * This method throws an exception if neither the newly attached activity
	 * nor the target fragment are instances of {@link ZWResetPassphraseDialogHandler}.
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.validateHandler(ZWResetPassphraseDialogHandler.class);
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
				R.layout.dialog_reset_passphrase, null));
		
		this.initDialogFields();

		builder.setView(this.getDialogView());
		
		Button cancel = (Button) this.getDialogView().findViewById(R.id.left_dialog_button);
		cancel.setOnClickListener(this);
		
		Button cont = (Button) this.getDialogView().findViewById(R.id.right_dialog_button);
		cont.setOnClickListener(this);
		
		// Do this after the view has been inflated so views with ids exist
		if (savedInstanceState != null) {
			if (savedInstanceState.getString(OLD_PASSPHRASE_KEY) != null) {
				this.setStringInTextView(R.id.textbox_old_passphrase, 
						savedInstanceState.getString(OLD_PASSPHRASE_KEY));
			}
			if (savedInstanceState.getString(NEW_PASSPHRASE_KEY) != null) {
				this.setStringInTextView(R.id.textbox_new_passphrase, 
						savedInstanceState.getString(NEW_PASSPHRASE_KEY));
			}
			if (savedInstanceState.getString(CONFIRM_PASSPHRASE_KEY) != null) {
				this.setStringInTextView(R.id.textbox_confirm_passphrase, 
						savedInstanceState.getString(CONFIRM_PASSPHRASE_KEY));
			}
		}

		return builder.create();
	}
	
	/**
	 * Handle clicks on this dialog.
	 */
	@Override
	public void onClick(View view) {
		ZWResetPassphraseDialogHandler handler = 
				(ZWResetPassphraseDialogHandler) this.getHandler();
		
		switch(view.getId()) {
		case R.id.left_dialog_button:
			//CANCEL
			handler.handleNegative(this.getTargetRequestCode());
			this.dismiss();
			break;
		case R.id.right_dialog_button:
			//CONTINUE
			handler.handleResetPassphrasePositive(
					this.getTargetRequestCode(), 
					this.getStringFromTextView(R.id.textbox_old_passphrase), 
					this.getStringFromTextView(R.id.textbox_new_passphrase), 
					this.getStringFromTextView(R.id.textbox_confirm_passphrase));
			this.dismiss();
			break;
		}
	}

	/**
	 * When we save the instance, in addition to doing everything that
	 * all dialogs must do, we also have to store the current entered 
	 * text in the three text boxes.
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		// Save all of the important strings in the dialog
		outState.putString(OLD_PASSPHRASE_KEY, 
				this.getStringFromTextView(R.id.textbox_old_passphrase));
		outState.putString(NEW_PASSPHRASE_KEY, 
				this.getStringFromTextView(R.id.textbox_new_passphrase));
		outState.putString(CONFIRM_PASSPHRASE_KEY, 
				this.getStringFromTextView(R.id.textbox_confirm_passphrase));
	}
	
	@Override
	protected Object getHandler() {
		return this.getActivity();
	}
	
}