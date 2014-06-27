package com.ziftr.android.onewallet.dialog;

import android.app.Activity;
import android.content.DialogInterface;

import com.ziftr.android.onewallet.dialog.handlers.OWNeutralDialogHandler;
import com.ziftr.android.onewallet.util.ZLog;

/**
 * A persistent (doesn't go away when the view is destroyed) dialog
 * for displaying a simple message to the user. 
 */
public class OWSimpleAlertDialog extends OWDialogFragment {

	/**
	 * Whenever this is fragment is attached to an activity 
	 * we must make sure that it is able to handle confirmations.
	 * 
	 * This method throws an exception if the newly attached activity
	 * is not an instance of OWOkayDialogHandler. 
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (this.getTargetFragment() != null) {
			if (!(this.getTargetFragment() instanceof OWNeutralDialogHandler)) {
				throw new ClassCastException(this.getTargetFragment().toString() + 
						" must implement " + OWNeutralDialogHandler.class.toString() + 
						" to be able to use this kind of dialog.");
			}
		} else if (!(activity instanceof OWNeutralDialogHandler)) {
			throw new ClassCastException(activity.toString() + " must implement " + 
					OWNeutralDialogHandler.class.toString() + 
					" to be able to use this kind of dialog.");
		}
	}

	/**
	 * Whenever the dialog is clicked we get the activity
	 * and have it handle the confirmation of the dialog.
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		// Get the correct handler, depends on whether this dialog
		// was created from a fragment or from an activity

		OWNeutralDialogHandler handler = this.getTargetFragment() == null ? 
				((OWNeutralDialogHandler) this.getActivity()) : 
					((OWNeutralDialogHandler) this.getTargetFragment());

		if (which == DialogInterface.BUTTON_NEUTRAL) {
			handler.handleNeutral(this.getTargetRequestCode());
		} else if (which == DialogInterface.BUTTON_NEGATIVE) {
			handler.handleNegative(this.getTargetRequestCode());
		} else if (which == DialogInterface.BUTTON_POSITIVE) {
			ZLog.log("These dialogs are not supposed to have positive buttons.");
		}
	}

}