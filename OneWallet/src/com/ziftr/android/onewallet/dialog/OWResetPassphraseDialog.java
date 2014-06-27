package com.ziftr.android.onewallet.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.dialog.handlers.OWResetPassphraseDialogHandler;
import com.ziftr.android.onewallet.util.ZLog;

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
	
	/** The view for this dialog. */
	private View dialogView;

	/**
	 * Whenever this is fragment is attached to an activity 
	 * we must make sure that it is able to handle accepting 
	 * and cancelling from passphrase dialogs.
	 * 
	 * This method throws an exception if the newly attached activity
	 * is not an instance of OWResetPassphraseDialogHandler. 
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (this.getTargetFragment() != null) {
			if (!(this.getTargetFragment() instanceof OWResetPassphraseDialogHandler)) {
				throw new ClassCastException(this.getTargetFragment().toString() + 
						" must implement " + 
						OWResetPassphraseDialogHandler.class.toString() + 
						" to be able to use this kind of dialog.");
			}
		} else if (!(activity instanceof OWResetPassphraseDialogHandler)) {
			throw new ClassCastException(activity.toString() + " must implement " + 
					OWResetPassphraseDialogHandler.class.toString() + 
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
		AlertDialog.Builder builder = this.createBuilder(savedInstanceState);

		this.dialogView = this.getActivity().getLayoutInflater().inflate(
				R.layout.reset_passphrase_dialog, null);
		builder.setView(this.dialogView);
		
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
	 * Handle clicks on this dialog. Just calls the handler for this
	 * reset.
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		OWResetPassphraseDialogHandler handler = this.getTargetFragment() == null ? 
				((OWResetPassphraseDialogHandler) this.getActivity()) : 
					((OWResetPassphraseDialogHandler) this.getTargetFragment());

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
	}

	/**
	 * When we save the instance, in addition to doing everything that
	 * all dialogs must do, we also have to store the current entered 
	 * text in the 
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Save all of the important strings in the dialog
		outState.putString(OLD_PASSPHRASE_KEY, 
				this.getStringFromEditText(R.id.textbox_old_passphrase));
		outState.putString(NEW_PASSPHRASE_KEY, 
				this.getStringFromEditText(R.id.textbox_new_passphrase));
		outState.putString(CONFIRM_PASSPHRASE_KEY, 
				this.getStringFromEditText(R.id.textbox_confirm_passphrase));
		
		// Prevent memory leak?
		this.dialogView = null;

		super.onSaveInstanceState(outState);
	}
	
	/**
	 * Given a resource id, this method finds the correpsonding
	 * view and then gets the text from from that view, returning 
	 * the value in bytes.
	 * Will throw a null pointer if no such view with id.
	 * Will thow a class cast if view is not an EditText.
	 * 
	 * @param id - The id of the view to find.
	 * @return as above
	 */
	private byte[] getBytesFromEditText(int id) {
		return this.getStringFromEditText(id).getBytes();
	}
	
	/**
	 * Given a resource id, this method finds the correpsonding
	 * view and then gets the text from from that view, returning 
	 * the value in a string.
	 * Will throw a null pointer if no such view with id.
	 * Will thow a class cast if view is not an EditText.
	 * 
	 * @param id - The id of the view to find.
	 * @return as above
	 */
	private String getStringFromEditText(int id) {
		EditText textBox = (EditText) this.dialogView.findViewById(id);
		return textBox.getText().toString();
	}
	
	/**
	 * Sets a specified EditText to have the string setVal in it's 
	 * writable field. 
	 * Will throw a null pointer if no such view with id.
	 * Will thow a class cast if view is not an EditText.
	 * 
	 * @param id - The id of the EditText to set the value in.
	 * @param setVal - The value to set it to.
	 * @return as above.
	 */
	private void setStringInEditText(int id, String setVal) {
		EditText textBox = (EditText) this.dialogView.findViewById(id);
		textBox.setText(setVal);
	}

}