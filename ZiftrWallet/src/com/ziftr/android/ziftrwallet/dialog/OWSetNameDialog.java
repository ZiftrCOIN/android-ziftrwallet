package com.ziftr.android.ziftrwallet.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.dialog.handlers.OWSetNameDialogHandler;
import com.ziftr.android.ziftrwallet.util.OWPreferencesUtils;

public class OWSetNameDialog extends OWDialogFragment{
	
	private static final String SET_NAME_KEY = "SET_NAME_KEY";
	
	private EditText newName;

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
		
		this.setStringInTextView(R.id.singleEditText, OWPreferencesUtils.getUserName());

		this.newName = (EditText) this.getDialogView().findViewById(R.id.singleEditText);
		if (savedInstanceState != null && savedInstanceState.containsKey(SET_NAME_KEY)) {
			this.setStringInTextView(R.id.singleEditText, 
					savedInstanceState.getString(SET_NAME_KEY));
		}

		return builder.create();
	}

	@Override
	public void onClick(View v) {
		OWSetNameDialogHandler handler = 
				(OWSetNameDialogHandler) this.getHandler();

		switch(v.getId()){
			case R.id.left_dialog_button:
				//CANCEL
				handler.handleNegative(this.getRequestCode());
				this.dismiss();
				break;
			case R.id.right_dialog_button:
				//CONTINUE
				handler.handleSetNamePositive(this.getRequestCode(), newName.getText().toString());
				this.dismiss();
				break;
		}
	}

	@Override
	protected Object getHandler() {
		return this.getActivity();
	}
	
}
