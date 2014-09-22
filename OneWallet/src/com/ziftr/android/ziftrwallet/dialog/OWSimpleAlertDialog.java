package com.ziftr.android.ziftrwallet.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.dialog.handlers.OWNeutralDialogHandler;

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
		this.validateHandler(OWNeutralDialogHandler.class);
	}
	
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = this.createBuilder(savedInstanceState);
		
		this.setDialogView(this.getActivity().getLayoutInflater().inflate(R.layout.dialog_default, null));
		this.initDialogFields();
		builder.setView(this.getDialogView());
		Button okbutton = (Button) this.getDialogView().findViewById(R.id.left_dialog_button);
		okbutton.setOnClickListener(this);
		
		return builder.create();
	}
	
	public void onClick(View view) {
		switch(view.getId()) {
			case R.id.left_dialog_button:
				//CANCEL
				this.dismiss();
				break;
		}
	}
	
	@Override
	protected Object getHandler() {
		return this.getActivity();
	}
}