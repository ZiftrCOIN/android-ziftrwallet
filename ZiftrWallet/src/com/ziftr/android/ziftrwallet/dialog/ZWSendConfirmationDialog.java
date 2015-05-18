package com.ziftr.android.ziftrwallet.dialog;

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
	protected View buildRootView() {
		View customDialogView = getActivity().getLayoutInflater().inflate(R.layout.send_confirmation, null);
		return customDialogView;
	}


	@Override
	protected View initRootView(View rootView) {
		
		TextView titleTextView = (TextView) rootView.findViewById(R.id.dialog_title);
		titleTextView.setText(R.string.zw_dialog_send_confirmation_title);

		View sendButton = rootView.findViewById(R.id.right_dialog_button);
		if(sendButton != null) {
			sendButton.setOnClickListener(this);
		}
		
		View cancelButton = rootView.findViewById(R.id.left_dialog_button);
		if(cancelButton != null) {
			cancelButton.setOnClickListener(this);
		}
		
		
		TextView totalText = (TextView) rootView.findViewById(R.id.sendConfirmationTotal);
		String confirmTotal = totalString + " " + coinName;
		totalText.setText(confirmTotal);
		
		TextView feeText = (TextView) rootView.findViewById(R.id.sendConfirmationFee);
		String feeBase = getActivity().getString(R.string.zw_dialog_send_confirmation_fee);
		String fee = String.format(feeBase, feeString);
		feeText.setText(fee);
		
		TextView addressText = (TextView) rootView.findViewById(R.id.sendConfirmationAddress);
		addressText.setText(addressString);
		
		return rootView;
	}
	
	
	public void setupConfirmationDetails(String coinName, String totalString, String feeString, String addressString) {
		this.coinName = coinName;
		this.totalString = totalString;
		this.feeString = feeString;
		this.addressString = addressString;
	}
	
	

}
