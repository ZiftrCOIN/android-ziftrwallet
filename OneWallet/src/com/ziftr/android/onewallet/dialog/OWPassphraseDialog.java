package com.ziftr.android.onewallet.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.EditText;

import com.ziftr.android.onewallet.dialog.handlers.OWPassphraseDialogHandler;
import com.ziftr.android.onewallet.util.ZLog;
import com.ziftr.android.onewallet.R;

/**
 * Dialogs where the app requests to get the passphrase
 * from the user.
 */
public class OWPassphraseDialog extends OWDialogFragment {

	private EditText passphraseTextBox;

	/**
	 * Whenever this is fragment is attached to an activity 
	 * we must make sure that it is able to handle accepting 
	 * and cancelling from passphrase dialogs.
	 * 
	 * This method throws an exception if the newly attached activity
	 * is not an instance of OWPassphraseDialogHandler. 
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (this.getTargetFragment() != null) {
			if (!(this.getTargetFragment() instanceof OWPassphraseDialogHandler)) {
				throw new ClassCastException(this.getTargetFragment().toString() + 
						" must implement " + OWPassphraseDialogHandler.class.toString() + 
						" to be able to use this kind of dialog.");
			}
		} else if (!(activity instanceof OWPassphraseDialogHandler)) {
			throw new ClassCastException(activity.toString() + " must implement " + 
					OWPassphraseDialogHandler.class.toString() + 
					" to be able to use this kind of dialog.");
		}
	}

	/**
	 * Creates and returns the dialog to show the user.
	 * Sets all the basic text fields that all dialogs have 
	 * to be what this diaglog was set up with. 
	 */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = createBuilder(savedInstanceState);
		
		this.passphraseTextBox = new EditText(this.getActivity());
		passphraseTextBox.setHint(R.string.passphrase_hint);
		builder.setView(this.passphraseTextBox);

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
			handler.handlePassphraseEnter(this.getTargetRequestCode(),
					this.passphraseTextBox.getText().toString().getBytes());
		} else if (which == DialogInterface.BUTTON_NEGATIVE) {
			handler.handleNegative(this.getTargetRequestCode());
		} else {
			ZLog.log("These dialogs shouldn't have neutral buttons.");
		}
	}

}