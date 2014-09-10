package com.ziftr.android.onewallet.fragment.accounts;

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

import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWConverter;
import com.ziftr.android.onewallet.util.OWFiat;
import com.ziftr.android.onewallet.util.OWUtils;

/**
 * An adapter class for transactions.
 */
public class OWWalletTransactionListAdapter extends OWSearchableListAdapter<OWWalletTransaction> {
	
	public enum Type {
		TRANSACTION,
		PENDING_DIVIDER,
		HISTORY_DIVIDER
	}

	public OWWalletTransactionListAdapter(Context ctx, List<OWWalletTransaction> txList) {
		super(ctx, txList);
	}
	
	public OWWalletTransactionListAdapter(Context context) {
		// Had to add this constructor to get rid of warnings, for some reason.
	    super(context, new ArrayList<OWWalletTransaction>());
	}
	
	/**
	 * Either transactionType or dividerType. Only footerType if it is 
	 * the last element in the list. 
	 */
	@Override
	public int getItemViewType(int position) {
		return getItemViewType(getItem(position));
	}
	
	private int getItemViewType(OWWalletTransaction item) {
		return item.getTxViewType().ordinal();
	}

	/**
	 * We have two types, dividerType and transactionType. 
	 */
	@Override
	public int getViewTypeCount() {
		return OWWalletTransactionListAdapter.Type.values().length;
	}

	/**
	 * Given the position of the item in the list, we get the list item
	 * and recreate the view from it. Note that this method recycles 
	 * the convertView when we have enough list elements. 
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// The convertView is an oldView that android is recycling.

		OWWalletTransaction txListItem = getItem(position);
		if (convertView == null) {
			// If it doesn't have an old view then we make a new one 
			convertView = this.getInflater().inflate(txListItem.getResId(), null);
		}

		if (getItemViewType(position) == Type.TRANSACTION.ordinal()) {

			String fiatSymbol = txListItem.getFiatType().getSymbol();

			// Whether or not we just created one, we reset all the resources
			// to match the currencyListItem.
			TextView txTitleTextView = (TextView) convertView.findViewById(R.id.txTitle);
			txTitleTextView.setText(txListItem.getTxNote());

			TextView txTimeTextView = (TextView) convertView.findViewById(R.id.txTime);
			// TODO we will continue using the seconds value for now. Is this right, though?
			Date date = new Date(txListItem.getTxTime() * 1000);
			txTimeTextView.setText(OWUtils.formatterNoTimeZone.format(date));

			TextView txAmount = (TextView) convertView.findViewById(R.id.txAmount);
			BigDecimal amt = OWCoin.formatCoinAmount(
					txListItem.getCoinId(), txListItem.getTxAmount());
			txAmount.setText(amt.toPlainString());

			TextView txAmountFiatEquiv = (TextView) 
					convertView.findViewById(R.id.txAmountFiatEquiv);
			BigInteger fiatAmt = OWConverter.convert(txListItem.getTxAmount(), 
					txListItem.getCoinId(), txListItem.getFiatType());
			BigDecimal formattedfiatAmt = OWFiat.formatFiatAmount(
					txListItem.getFiatType(), OWUtils.bigIntToBigDec(txListItem.getCoinId(), fiatAmt));
			txAmountFiatEquiv.setText(fiatSymbol + formattedfiatAmt.toPlainString());

			ImageView txIOIcon = (ImageView) convertView.findViewById(R.id.txIOIcon);
			Drawable image = this.getContext().getResources().getDrawable(
					getImgResIdForItem(txListItem));
			txIOIcon.setImageDrawable(image);

			return convertView;

		} else {
			// Just have to make sure the text is set for the appropriate divider
			TextView walletTxListTextView = (TextView) 
					convertView.findViewById(R.id.walletTxListDividerTextView);
			walletTxListTextView.setText(txListItem.getTxNote());
			return convertView;
		}

	}

	/**
	 * @param txListItem
	 * @return
	 */
	private int getImgResIdForItem(OWWalletTransaction txListItem) {
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
	
}