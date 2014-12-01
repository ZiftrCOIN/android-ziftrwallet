package com.ziftr.android.ziftrwallet.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.dialog.handlers.ZWEditAddressLabelDialogHandler;
import com.ziftr.android.ziftrwallet.dialog.handlers.ZWValidatePassphraseDialogHandler;
import com.ziftr.android.ziftrwallet.util.ZLog;

/**
 * Dialogs where the app requests to get the passphrase
 * from the user.
 */
public class ZWEditAddressLabelDialog extends ZWDialogFragment {
	/** The key to save the text in the box. */
	public static final String CURRENT_ENTERED_TEXT_KEY = "entered_text";
	
	/** The key to save the address.  */
	public static final String ADDRESS_KEY = "address_key";
	
	public static final String IS_RECEIVING_NOT_SENDING_KEY = "receiving_not_sending";
	
	/**
	 * Whenever this is fragment is attached to an activity 
	 * we must make sure that it is able to handle accepting 
	 * and cancelling.
	 * 
	 * This method throws an exception if neither the newly attached activity nor 
	 * the target fragment are instances of {@link ZWValidatePassphraseDialogHandler}.
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.validateHandler(ZWEditAddressLabelDialogHandler.class);
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
				R.layout.dialog_single_edit_text, null));
		this.initDialogFields();

		builder.setView(this.getDialogView());
		
		Button cancel = (Button) this.getDialogView().findViewById(R.id.left_dialog_button);
		cancel.setOnClickListener(this);
		
		Button next = (Button) this.getDialogView().findViewById(R.id.right_dialog_button);
		next.setOnClickListener(this);
		
		this.setStringInTextView(R.id.singleEditText, this.getArguments().getString(CURRENT_ENTERED_TEXT_KEY));
		
		if (savedInstanceState != null) {
			if (savedInstanceState.getString(CURRENT_ENTERED_TEXT_KEY) != null) {
				this.setStringInTextView(R.id.singleEditText, 
						savedInstanceState.getString(CURRENT_ENTERED_TEXT_KEY));
			}
		}

		return builder.create();
	}

	public void onClick(View view) {
		ZWEditAddressLabelDialogHandler handler = (ZWEditAddressLabelDialogHandler) this.getHandler();
		
		switch(view.getId()) {
		case R.id.left_dialog_button:
			//CANCEL
			handler.handleNegative(this.getRequestCode());
			this.dismiss();
			break;
		case R.id.right_dialog_button:
			//CONTINUE
			String address = this.getArguments().getString(ADDRESS_KEY);
			String label = this.getStringFromTextView(R.id.singleEditText);
			handler.handleAddressLabelChange(this.getRequestCode(), address, label, getIsReceiving());
			this.dismiss();
			break;
		}
	}
	
	public boolean getIsReceiving() {
		Bundle args = this.getArguments();
		if (args != null) {
			return args.getBoolean(IS_RECEIVING_NOT_SENDING_KEY);
		}
		ZLog.log("No boolean set, it should have been set. ");
		return false;
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
				this.getStringFromTextView(R.id.singleEditText));
	}

	@Override
	protected Object getHandler() {
		return this.getActivity();
	}
	
	@Override
	public void dismiss() {
		this.closeKeyboard(R.id.singleEditText);
		super.dismiss();
	}
	
}