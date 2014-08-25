package com.ziftr.android.onewallet.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.dialog.handlers.OWResetPassphraseDialogHandler;

/**
 * Dialogs where the app requests to get the passphrase
 * from the user.
 */
public class OWResetPassphraseDialog extends OWDialogFragment {

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
	 * nor the target fragment are instances of {@link OWResetPassphraseDialogHandler}.
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.validateHandler(OWResetPassphraseDialogHandler.class);
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
		builder.setView(this.getDialogView());
		
		// Do this after the view has been inflated so views with ids exist
		if (savedInstanceState != null) {
			if (savedInstanceState.getString(OLD_PASSPHRASE_KEY) != null) {
				this.setStringInEditText(R.id.textbox_old_passphrase, 
						savedInstanceState.getString(OLD_PASSPHRASE_KEY));
			}
			if (savedInstanceState.getString(NEW_PASSPHRASE_KEY) != null) {
				this.setStringInEditText(R.id.textbox_new_passphrase, 
						savedInstanceState.getString(NEW_PASSPHRASE_KEY));
			}
			if (savedInstanceState.getString(CONFIRM_PASSPHRASE_KEY) != null) {
				this.setStringInEditText(R.id.textbox_confirm_passphrase, 
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
		// TODO
		// Isn't the below method still corect?
	}

	/**
	 * Handle clicks on this dialog. Just calls the handler for this
	 * reset.
	 *
	@Override
	public void onClick(DialogInterface dialog, int which) {
		OWResetPassphraseDialogHandler handler = 
				(OWResetPassphraseDialogHandler) this.getHandler();

		if (which == DialogInterface.BUTTON_POSITIVE) {
			// Have the handler handle the data
			handler.handleResetPassphrasePositive(
					this.getTargetRequestCode(), 
					this.getBytesFromEditText(R.id.textbox_old_passphrase), 
					this.getBytesFromEditText(R.id.textbox_new_passphrase), 
					this.getBytesFromEditText(R.id.textbox_confirm_passphrase));
		} else if (which == DialogInterface.BUTTON_NEGATIVE) {
			handler.handleNegative(this.getTargetRequestCode());
		} else {
			ZLog.log("These dialogs shouldn't have neutral buttons.");
		}
	}*/

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
				this.getStringFromEditText(R.id.textbox_old_passphrase));
		outState.putString(NEW_PASSPHRASE_KEY, 
				this.getStringFromEditText(R.id.textbox_new_passphrase));
		outState.putString(CONFIRM_PASSPHRASE_KEY, 
				this.getStringFromEditText(R.id.textbox_confirm_passphrase));
	}
	
	@Override
	protected Object getHandler() {
		return this.getActivity();
	}
	
}