package com.ziftr.android.ziftrwallet.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.ziftr.android.ziftrwallet.R;

public class ZWSendConfirmationDialog extends ZiftrSimpleDialogFragment {
	
	
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
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putString("feeString", feeString);
		outState.putString("totalString", totalString);
		outState.putString("coinName", coinName);
		outState.putString("addressString", addressString);
	}


	@Override
	protected void restoreInstanceState(Bundle savedInstanceState) {
		super.restoreInstanceState(savedInstanceState);
		
		if(savedInstanceState != null) {
			feeString = savedInstanceState.getString("feeString");
			totalString = savedInstanceState.getString("totalString");
			coinName = savedInstanceState.getString("coinName");
			addressString = savedInstanceState.getString("addressString");
		}
	}


	@Override
	protected Dialog buildCustomDialog(View customDialogView) {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
		
		builder.setView(customDialogView);
		
		TextView titleTextView = (TextView) customDialogView.findViewById(R.id.dialog_title);
		titleTextView.setText(R.string.zw_dialog_send_confirmation_title);

		View sendButton = customDialogView.findViewById(R.id.right_dialog_button);
		if(sendButton != null) {
			sendButton.setOnClickListener(this);
		}
		
		View cancelButton = customDialogView.findViewById(R.id.left_dialog_button);
		if(cancelButton != null) {
			cancelButton.setOnClickListener(this);
		}
		
		
		TextView totalText = (TextView) customDialogView.findViewById(R.id.sendConfirmationTotal);
		String confirmTotal = totalString + " " + coinName;
		totalText.setText(confirmTotal);
		
		TextView feeText = (TextView) customDialogView.findViewById(R.id.sendConfirmationFee);
		String feeBase = getActivity().getString(R.string.zw_dialog_send_confirmation_fee);
		String fee = String.format(feeBase, feeString);
		feeText.setText(fee);
		
		TextView addressText = (TextView) customDialogView.findViewById(R.id.sendConfirmationAddress);
		addressText.setText(addressString);
		
		
		return builder.create();
	}
	
	
	public void setupConfirmationDetails(String coinName, String totalString, String feeString, String addressString) {
		this.coinName = coinName;
		this.totalString = totalString;
		this.feeString = feeString;
		this.addressString = addressString;
	}
	
	

}
