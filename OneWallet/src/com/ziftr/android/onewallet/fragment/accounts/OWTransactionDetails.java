package com.ziftr.android.onewallet.fragment.accounts;

import java.math.BigDecimal;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ziftr.android.onewallet.R;

public class OWTransactionDetails extends Fragment{

	private View rootView;

	public static OWTransactionDetails newInstance(Bundle data) {
		OWTransactionDetails frag = new OWTransactionDetails();
		frag.setArguments(data);
		return frag;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {

		this.rootView = inflater.inflate(
				R.layout.accounts_transaction_details, container, false);

		this.init_fields(savedInstanceState);
		return this.rootView;
	}
	/**
	 * Get the arguments passed from the activity and set the text fields on the view
	 * 
	 * TODO the string constants should be public static final fields in this class. 
	 * 
	 * @param args
	 */
	public void init_fields(Bundle args) {

		TextView amount = (TextView) rootView.findViewById(R.id.amount);
		BigDecimal amountvalue = new BigDecimal(getArguments().getString("amount"));

		TextView amount_label = (TextView) rootView.findViewById(R.id.amount_label);
		TextView time_label = (TextView) rootView.findViewById(R.id.date_label);
		if ( amountvalue.compareTo(BigDecimal.ZERO) > 0) {
			amount_label.setText("Amount Received");
			time_label.setText("Received");
		} else {
			amount_label.setText("Amount Sent");
			time_label.setText("Sent");
		}
		amount.setText(amountvalue.toString());
		TextView pending = (TextView) rootView.findViewById(R.id.pending);

		if (getArguments().getBoolean("pending")) {
			pending.setVisibility(View.VISIBLE);
			pending.setTextColor(getResources().getColor(R.color.Crimson));
			amount.setTextColor(getResources().getColor(R.color.Crimson));
		} else {
			pending.setVisibility(View.INVISIBLE);
		}

		TextView currency = (TextView) rootView.findViewById(R.id.currency_value);
		currency.setText(getArguments().getString("currencyval"));

		TextView currencyType = (TextView) rootView.findViewById(R.id.currency_type);
		currencyType.setText(getArguments().getString("currencytype"));

		TextView time = (TextView) rootView.findViewById(R.id.date);
		time.setText(getArguments().getString("date"));

		//TODO add confirmation fee field for OWWalletTransactionListItem
		//TextView fee = (TextView) rootView.findViewById(R.id.confirmation_fee_amount);

	}



}
