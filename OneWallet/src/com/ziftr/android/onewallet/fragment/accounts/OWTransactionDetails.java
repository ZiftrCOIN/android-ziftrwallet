package com.ziftr.android.onewallet.fragment.accounts;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ziftr.android.onewallet.R;

public class OWTransactionDetails extends Fragment{
	
	private View rootView;

	public static OWTransactionDetails newInstance(Bundle data){
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
	 * @param args
	 */
	public void init_fields(Bundle args){
		
		TextView amountsent = (TextView) rootView.findViewById(R.id.amount_sent);
		amountsent.setText(getArguments().getString("amountsent"));
		TextView pending = (TextView) rootView.findViewById(R.id.pending);
		if (getArguments().getBoolean("pending")){
			pending.setVisibility(rootView.VISIBLE);
			pending.setTextColor(getResources().getColor(R.color.Crimson));
			amountsent.setTextColor(getResources().getColor(R.color.Crimson));
		} else {
			pending.setVisibility(rootView.INVISIBLE);
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
