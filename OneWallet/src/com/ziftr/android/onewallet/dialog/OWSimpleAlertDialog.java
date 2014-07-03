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
	 * we must make sure that it is able to handle accepting 
	 * and cancelling from simple alert dialogs.
	 * 
	 * This method throws an exception if neither the newly attached activity
	 * nor the target fragment are instances of {@link OWNeutralDialogHandler}.
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.validateHandler(activity, OWNeutralDialogHandler.class);
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