package com.ziftr.android.onewallet.fragment.accounts;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.fragment.OWFragment;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWConverter;
import com.ziftr.android.onewallet.util.OWFiat;
import com.ziftr.android.onewallet.util.OWUtils;

public class OWTransactionDetailsFragment extends OWFragment {

	private View rootView;
	
	private OWWalletTransaction txItem;

	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {

		this.rootView = inflater.inflate(
				R.layout.accounts_transaction_details, container, false);

		this.init_fields(savedInstanceState);
		return this.rootView;
	}
	
	@Override
	public void onResume(){
		super.onResume();
		this.getOWMainActivity().changeActionBar("TRANSACTION", false, false);
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
		
		TextView pending = (TextView) rootView.findViewById(R.id.pending);

		if (txItem.isPending()) {
			pending.setVisibility(View.VISIBLE);
			pending.setTextColor(getResources().getColor(R.color.Crimson));
			amount.setTextColor(getResources().getColor(R.color.Crimson));
		} else {
			pending.setVisibility(View.INVISIBLE);
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

		//TODO add confirmation fee field for OWWalletTransactionListItem
		//TextView fee = (TextView) rootView.findViewById(R.id.confirmation_fee_amount);

	}

	/**
	 * @return the txItem
	 */
	public OWWalletTransaction getTxItem() {
		return txItem;
	}

	/**
	 * @param txItem the txItem to set
	 */
	public void setTxItem(OWWalletTransaction txItem) {
		this.txItem = txItem;
	}

}
