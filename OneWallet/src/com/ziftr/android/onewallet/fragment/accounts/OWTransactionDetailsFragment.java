package com.ziftr.android.onewallet.fragment.accounts;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.crypto.OWAddress;
import com.ziftr.android.onewallet.crypto.OWTransaction;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWConverter;
import com.ziftr.android.onewallet.util.OWFiat;
import com.ziftr.android.onewallet.util.OWUtils;
import com.ziftr.android.onewallet.util.ZLog;

public class OWTransactionDetailsFragment extends OWWalletUserFragment {

	private View rootView;
	
	private OWTransaction txItem;
	
	private boolean isEditing;

	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {
		
		//resize view when keyboard pops up instead of just default pan so user can see field more clearly
		getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		
		this.getOWMainActivity().hideWalletHeader();
		
		this.rootView = inflater.inflate(R.layout.accounts_transaction_details, container, false);
		if (savedInstanceState != null && savedInstanceState.containsKey("isEditing")){
			this.isEditing = savedInstanceState.getBoolean("isEditing");
		}
		ZLog.log("OncReateView");
		this.txItem = getArguments().getParcelable("txItem");
		this.initFields();
		return this.rootView;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState){
		outState.putBoolean("isEditing", this.isEditing);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		this.getOWMainActivity().changeActionBar("TRANSACTION", false, false);
	}

	/**
	 * Get the arguments passed from the activity and set the text fields on the view
	 * 
	 * TODO the string constants should be public static final fields in this class. 
	 *  
	 */
	@SuppressLint("NewApi")
	public void initFields() {
		TextView amount = (TextView) rootView.findViewById(R.id.amount);
		BigInteger baseAmount = this.txItem.getTxAmount();
		BigDecimal amountValue = OWUtils.bigIntToBigDec(txItem.getCoinId(), baseAmount); 
		amount.setText(OWCoin.formatCoinAmount(txItem.getCoinId(), amountValue).toPlainString());
		TextView amountLabel = (TextView) rootView.findViewById(R.id.amountLabel);
		TextView timeLabel = (TextView) rootView.findViewById(R.id.date_label);
		if (this.txItem.getTxAmount().compareTo(BigInteger.ZERO) < 0) {
			// This means the tx is sent (relative to user)
			amountLabel.setText("Amount Sent");
			timeLabel.setText("Sent");
		} else {
			// This means the tx is received (relative to user)
			amountLabel.setText("Amount Received");
			timeLabel.setText("Received");
		}
		if (txItem.isPending()) {
			amount.setTextColor(getResources().getColor(R.color.Crimson));
			amountLabel.append(" (pending)");
		}
		TextView currency = (TextView) rootView.findViewById(R.id.currencyValue);
		BigInteger fiatAmt = OWConverter.convert(txItem.getTxAmount(), 
				txItem.getCoinId(), txItem.getFiatType());
		BigDecimal formattedfiatAmt = OWFiat.formatFiatAmount(
				txItem.getFiatType(), OWUtils.bigIntToBigDec(txItem.getCoinId(), fiatAmt));
		currency.setText(formattedfiatAmt.toPlainString());

		TextView currencyType = (TextView) rootView.findViewById(R.id.currencyType);
		currencyType.setText(this.txItem.getFiatType().getName());

		TextView time = (TextView) rootView.findViewById(R.id.date);
		Date date = new Date(this.txItem.getTxTime() * 1000);
		time.setText(OWUtils.formatterNoTimeZone.format(date));
		
		TextView routing_address = (TextView) rootView.findViewById(R.id.routing_address);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < txItem.getDisplayAddresses().size(); i++) {
			OWAddress a = txItem.getDisplayAddresses().get(i);
			sb.append(a.toString());
			if (i != txItem.getDisplayAddresses().size()-1) {
				sb.append("\n");
			}
		}
		routing_address.setText(sb.toString());
		//TODO add confirmation fee field for OWWalletTransactionListItem
		//TextView fee = (TextView) rootView.findViewById(R.id.confirmation_fee_amount);
		
		TextView status = (TextView) rootView.findViewById(R.id.status);
		status.setText("Confirmed (" + txItem.getNumConfirmations() + " of " + txItem.getCoinId().getNumRecommendedConfirmations() + ")");
		
		final ImageView editLabel = (ImageView) rootView.findViewById(R.id.edit_txn_note);
		final EditText txnNote = (EditText) rootView.findViewById(R.id.txn_note);
		editLabel.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!isEditing){
					txnNote.setBackgroundResource(android.R.drawable.editbox_background_normal);
					txnNote.setFocusable(true);
					txnNote.setFocusableInTouchMode(true);
					txnNote.requestFocus();
					editLabel.setImageResource(R.drawable.close_enabled);
					isEditing = true;
				} else {
					txnNote.setFocusable(false);
					txnNote.setFocusableInTouchMode(false);
					txnNote.setBackgroundColor(getResources().getColor(android.R.color.transparent));
					txnNote.setPadding(0, 0, 0, 0);
					editLabel.setImageResource(R.drawable.edit_enabled);
					isEditing = false;
					txItem.setTxNote(txnNote.getText().toString());
					getWalletManager().updateTransactionNote(txItem);

				}
			}
		});

	}

}
