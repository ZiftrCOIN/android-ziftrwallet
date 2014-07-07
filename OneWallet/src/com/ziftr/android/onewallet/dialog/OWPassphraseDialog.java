package com.ziftr.android.onewallet.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.dialog.handlers.OWPassphraseDialogHandler;
import com.ziftr.android.onewallet.util.ZLog;

/**
 * Dialogs where the app requests to get the passphrase
 * from the user.
 */
public class OWPassphraseDialog extends OWDialogFragment {
	
	/** The key to save the text in the box. */
	private static final String CURRENT_ENTERED_TEXT_KEY = "entered_text";
	
	/**
	 * Whenever this is fragment is attached to an activity 
	 * we must make sure that it is able to handle accepting 
	 * and cancelling from passphrase dialogs.
	 * 
	 * This method throws an exception if neither the newly attached activity
	 * nor the target fragment are instances of {@link OWPassphraseDialogHandler}.
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.validateHandler(activity, OWPassphraseDialogHandler.class);
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
		builder.setView(this.getDialogView());
		
		if (savedInstanceState != null) {
			if (savedInstanceState.getString(CURRENT_ENTERED_TEXT_KEY) != null) {
				this.setStringInEditText(R.id.textbox_set_new_passphrase, 
						savedInstanceState.getString(CURRENT_ENTERED_TEXT_KEY));
			}
		}
		
		return builder.create();
	}

	/**
	 * Handle clicks on this dialog. 
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		OWPassphraseDialogHandler handler = this.getTargetFragment() == null ? 
				((OWPassphraseDialogHandler) this.getActivity()) : 
					((OWPassphraseDialogHandler) this.getTargetFragment());
		if (which == DialogInterface.BUTTON_POSITIVE) {
			handler.handlePassphrasePositive(this.getTargetRequestCode(), 
					this.getBytesFromEditText(R.id.textbox_set_new_passphrase));
		} else if (which == DialogInterface.BUTTON_NEGATIVE) {
			handler.handleNegative(this.getTargetRequestCode());
		} else {
			ZLog.log("These dialogs shouldn't have neutral buttons.");
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
				this.getStringFromEditText(R.id.textbox_set_new_passphrase));
	}

}