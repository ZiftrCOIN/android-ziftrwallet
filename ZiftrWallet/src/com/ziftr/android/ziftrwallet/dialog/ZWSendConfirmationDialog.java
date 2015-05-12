package com.ziftr.android.ziftrwallet.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.ziftr.android.ziftrwallet.R;

public class ZWSendConfirmationDialog extends ZiftrDialogFragment {
	
	
	private String feeString;
	private String totalString;
	private String coinName;
	private String addressString;
	
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		restoreInstanceState(savedInstanceState);
		
		View customDialogView = getActivity().getLayoutInflater().inflate(R.layout.send_confirmation, null);
		
		Dialog customDialog = buildCustomDialog(customDialogView);
		customDialog.setCancelable(isCancelable);
		customDialog.setCanceledOnTouchOutside(isCancelable);
		
		return customDialog;
	}
	

	@Override
	protected Dialog buildCustomDialog(View customDialogView) {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
		
		builder.setView(customDialogView);
		
		TextView titleTextView = (TextView) customDialogView.findViewById(R.id.dialog_title);
		titleTextView.setText(R.string.zw_dialog_send_confirmation_title);

		View sendButton = customDialogView.findViewById(R.id.sendConfirmationSendButton);
		if(yes != null) {
			sendButton.setOnClickListener(this);
		}
		
		View cancelButton = customDialogView.findViewById(R.id.sendConfirmationCancelButton);
		if(no != null) {
			cancelButton.setOnClickListener(this);
		}
		
		
		TextView total = (TextView) customDialogView.findViewById(R.id.sendConfirmationTotal);
		String confirmTotal = totalString + " " + coinName;
		total.setText(confirmTotal);
		
		
		return builder.create();
	}
	
	
	
	
	

}
