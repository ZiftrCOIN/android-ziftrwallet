/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.fragment;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWPreferences;
import com.ziftr.android.ziftrwallet.crypto.ZWConverter;
import com.ziftr.android.ziftrwallet.crypto.ZWFiat;
import com.ziftr.android.ziftrwallet.crypto.ZWTransaction;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

/**
 * An adapter class for transactions.
 */
public class ZWWalletTransactionListAdapter extends ZWSearchableListAdapter<ZWSearchableListItem> {
	
	//different types for the views in the list
	public static final int TYPE_TRANSACTION = 0;
	public static final int TYPE_DIVIDER = 1;
	
	private static final int TYPE_MAX_TYPES = 2;

	public ZWWalletTransactionListAdapter(Context ctx, List<ZWSearchableListItem> txList) {
		super(ctx, 0, txList);
	}
	
	public ZWWalletTransactionListAdapter(Context context) {
		// Had to add this constructor to get rid of warnings, for some reason.
	    this(context, new ArrayList<ZWSearchableListItem>());
	}
	
	/**
	 * Either transactionType or dividerType. Only footerType if it is 
	 * the last element in the list. 
	 */
	@Override
	public int getItemViewType(int position) {
	
		if(this.getItem(position) instanceof ZWTransaction) {
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
		
		//ZWTransaction txListItem = getItem(position);
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
			
			ZWTransaction txListItem = (ZWTransaction) getItem(position);
			
			// Whether or not we just created one, we reset all the resources
			// to match the currencyListItem.
			TextView txTitleTextView = (TextView) convertView.findViewById(R.id.txTitle);
			txTitleTextView.setText(txListItem.getNote());

			TextView txTimeTextView = (TextView) convertView.findViewById(R.id.txTime);
			Date date = new Date(txListItem.getTxTime());
			txTimeTextView.setText(ZiftrUtils.formatterNoTimeZone.format(date));

			TextView txAmount = (TextView) convertView.findViewById(R.id.txAmount);
			String amt = txListItem.getCoin().getFormattedAmount(txListItem.getAmount());
			txAmount.setText(amt);

			ZWFiat fiat = ZWPreferences.getFiatCurrency();
			
			TextView txAmountFiatEquiv = (TextView) convertView.findViewById(R.id.txAmountFiatEquiv);
			
			BigInteger fiatAmt = ZWConverter.convert(txListItem.getAmount(), txListItem.getCoin(), fiat);
			
			String fiatAmountString = fiat.getFormattedAmount(fiatAmt, true);
			txAmountFiatEquiv.setText(ZiftrUtils.getCurrencyDisplayString(fiatAmountString));

			ImageView txIOIcon = (ImageView) convertView.findViewById(R.id.txIOIcon);
			txIOIcon.setImageResource(getImgResIdForItem(txListItem));
			
			if (txListItem.isPending()){
				txTitleTextView.setTextColor(this.getContext().getResources().getColor(R.color.Crimson));
				txTimeTextView.setTextColor(this.getContext().getResources().getColor(R.color.Crimson));
				txAmount.setTextColor(this.getContext().getResources().getColor(R.color.Crimson));
				txAmountFiatEquiv.setTextColor(this.getContext().getResources().getColor(R.color.Crimson));
			} else if (txListItem.getAmount().compareTo(BigInteger.ZERO) >= 0){
				txTitleTextView.setTextColor(this.getContext().getResources().getColor(R.color.Black));
				txTimeTextView.setTextColor(this.getContext().getResources().getColor(R.color.Black));
				txAmount.setTextColor(this.getContext().getResources().getColor(R.color.Green));
				txAmountFiatEquiv.setTextColor(this.getContext().getResources().getColor(R.color.Green));
			} else {
				txTitleTextView.setTextColor(this.getContext().getResources().getColor(R.color.Black));
				txTimeTextView.setTextColor(this.getContext().getResources().getColor(R.color.Black));
				txAmount.setTextColor(this.getContext().getResources().getColor(R.color.Black));
				txAmountFiatEquiv.setTextColor(this.getContext().getResources().getColor(R.color.Black));

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
	private int getImgResIdForItem(ZWTransaction txListItem) {
		int imgResId;
		if (txListItem.getAmount().compareTo(BigInteger.ZERO) >= 0) {
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