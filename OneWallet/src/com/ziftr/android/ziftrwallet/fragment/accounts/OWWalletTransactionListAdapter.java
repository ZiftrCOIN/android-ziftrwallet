package com.ziftr.android.ziftrwallet.fragment.accounts;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.crypto.OWTransaction;
import com.ziftr.android.ziftrwallet.util.OWCoin;
import com.ziftr.android.ziftrwallet.util.OWConverter;
import com.ziftr.android.ziftrwallet.util.OWFiat;
import com.ziftr.android.ziftrwallet.util.OWPreferencesUtils;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

/**
 * An adapter class for transactions.
 */
public class OWWalletTransactionListAdapter extends OWSearchableListAdapter<OWSearchableListItem> {
	
	//different types for the views in the list
	public static final int TYPE_TRANSACTION = 0;
	public static final int TYPE_DIVIDER = 1;
	
	private static final int TYPE_MAX_TYPES = 2;

	public OWWalletTransactionListAdapter(Context ctx, List<OWSearchableListItem> txList) {
		super(ctx, 0, txList);
	}
	
	public OWWalletTransactionListAdapter(Context context) {
		// Had to add this constructor to get rid of warnings, for some reason.
	    this(context, new ArrayList<OWSearchableListItem>());
	}
	
	/**
	 * Either transactionType or dividerType. Only footerType if it is 
	 * the last element in the list. 
	 */
	@Override
	public int getItemViewType(int position) {
	
		if(this.getItem(position) instanceof OWTransaction) {
			return TYPE_TRANSACTION;
		}
		else {
			return TYPE_DIVIDER;
		}
	}
	
	@Override
	public boolean isEnabled(int position){
		if (this.getItemViewType(position) == TYPE_TRANSACTION){
			return super.isEnabled(position);
		} else {
			return false;
		}
	}

	
	/**
	 * We have two types, dividerType and transactionType. 
	 */
	@Override
	public int getViewTypeCount() {
		return TYPE_MAX_TYPES;
	}

	/**
	 * Given the position of the item in the list, we get the list item
	 * and recreate the view from it. Note that this method recycles 
	 * the convertView when we have enough list elements. 
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		int viewType = getItemViewType(position);
		
		//OWTransaction txListItem = getItem(position);
		if (convertView == null) {
			// If it doesn't have an old view then we make a new one
			if(viewType == TYPE_TRANSACTION) {
				convertView = this.getInflater().inflate(R.layout.accounts_wallet_tx_list_item, null);
			}
			else {
				convertView = this.getInflater().inflate(R.layout.accounts_wallet_tx_list_divider, null);
			}
		}

		if (viewType == TYPE_TRANSACTION) {
			
			OWTransaction txListItem = (OWTransaction) getItem(position);
			
			// Whether or not we just created one, we reset all the resources
			// to match the currencyListItem.
			TextView txTitleTextView = (TextView) convertView.findViewById(R.id.txTitle);
			txTitleTextView.setText(txListItem.getTxNote());

			TextView txTimeTextView = (TextView) convertView.findViewById(R.id.txTime);
			// TODO we will continue using the seconds value for now. Is this right, though?
			Date date = new Date(txListItem.getTxTime() * 1000);
			txTimeTextView.setText(ZiftrUtils.formatterNoTimeZone.format(date));

			TextView txAmount = (TextView) convertView.findViewById(R.id.txAmount);
			BigDecimal amt = OWCoin.formatCoinAmount(
					txListItem.getCoinId(), txListItem.getTxAmount());
			txAmount.setText(amt.toPlainString());

			OWFiat fiat = OWPreferencesUtils.getFiatCurrency(this.getContext());
			
			TextView txAmountFiatEquiv = (TextView) 
					convertView.findViewById(R.id.txAmountFiatEquiv);
			BigInteger fiatAmt = OWConverter.convert(txListItem.getTxAmount(), 
					txListItem.getCoinId(), fiat);
			
			txAmountFiatEquiv.setText(OWFiat.formatFiatAmount(fiat, ZiftrUtils.bigIntToBigDec(txListItem.getCoinId(), fiatAmt), true));

			ImageView txIOIcon = (ImageView) convertView.findViewById(R.id.txIOIcon);
			Drawable image = this.getContext().getResources().getDrawable(
					getImgResIdForItem(txListItem));
			txIOIcon.setImageDrawable(image);
			if (txListItem.isPending()){
				txTitleTextView.setTextColor(this.getContext().getResources().getColor(R.color.Crimson));
				txTimeTextView.setTextColor(this.getContext().getResources().getColor(R.color.Crimson));
				txAmount.setTextColor(this.getContext().getResources().getColor(R.color.Crimson));
				txAmountFiatEquiv.setTextColor(this.getContext().getResources().getColor(R.color.Crimson));
			} else if (txListItem.getTxAmount().compareTo(BigInteger.ZERO) >= 0){
				txAmount.setTextColor(this.getContext().getResources().getColor(R.color.Green));
				txAmountFiatEquiv.setTextColor(this.getContext().getResources().getColor(R.color.Green));

			}

		} 
		else {
			// Just have to make sure the text is set for the appropriate divider
			TextView walletTxListTextView = (TextView) 
					convertView.findViewById(R.id.walletTxListDividerTextView);
			
			String dividerText = this.getItem(position).toString();
			walletTxListTextView.setText(dividerText);
		}
		
		return convertView;

	}

	/**
	 * @param txListItem
	 * @return
	 */
	private int getImgResIdForItem(OWTransaction txListItem) {
		int imgResId;
		if (txListItem.getTxAmount().compareTo(BigInteger.ZERO) >= 0) {
			// This means the tx is received (relative to user)
			if (txListItem.isPending()) {
				imgResId = R.drawable.received_pending_enabled;
			} else {
				imgResId = R.drawable.received_enabled;
			}
		} else {
			// This means the tx is sent (relative to user)
			if (txListItem.isPending()) {
				imgResId = R.drawable.sent_pending_enabled;
			} else {
				imgResId = R.drawable.sent_enabled;
			}
		}
		return imgResId;
	}


	@Override
	public void applySortToFullList() {
		// Nothing to do, no sorting done here
	}
	
}