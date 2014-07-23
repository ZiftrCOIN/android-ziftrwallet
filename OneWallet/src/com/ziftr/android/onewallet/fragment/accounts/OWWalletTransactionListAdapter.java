package com.ziftr.android.onewallet.fragment.accounts;

import java.math.BigDecimal;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWConverter;
import com.ziftr.android.onewallet.util.OWFiat;

/**
 * 
 */
public class OWWalletTransactionListAdapter extends ArrayAdapter<OWWalletTransactionListItem> {
	private LayoutInflater inflater;
	private Context context;
	private int historyIndex;
	
	// TODO add a boolean returner that determines whether or not to add items
	// to the list. This can be used in searching

	/** Standard entries for transactions. */
	public static final int transactionType = 0;
	/** The divider type. */
	public static final int dividerType = 1;
	
	public OWWalletTransactionListAdapter(Context ctx, 
			List<OWWalletTransactionListItem> dataSet) {
		super(ctx, 0, dataSet);
		this.inflater = LayoutInflater.from(ctx);
		this.context = ctx;
		this.updateHistoryIndex();
	}

	/**
	 * Either transactionType or dividerType. Only footerType if it is 
	 * the last element in the list. 
	 */
	@Override
	public int getItemViewType(int position) {
		this.updateHistoryIndex();
		if (position == 0 || position == this.historyIndex) {
			return dividerType;
		} else {
			return transactionType;
		}
	}

	/**
	 * We have two types, dividerType and transactionType. 
	 */
	@Override
	public int getViewTypeCount() {
		// 2 because we have 2 wallet types, dividerType and transactionType
		return 2;
	}

	/**
	 * Given the position of the item in the list, we get the list item
	 * and recreate the view from it. Note that this method recycles 
	 * the convertView when we have enough list elements. 
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// The convertView is an oldView that android is recycling.

		OWWalletTransactionListItem txListItem = getItem(position);
		if (convertView == null) {
			// If it doesn't have an old view then we make a new one 
			convertView = this.inflater.inflate(txListItem.getResId(), null);
		}

		if (getItemViewType(position) == transactionType) {
			
			String fiatSymbol = txListItem.getFiatType().getSymbol();

			// Whether or not we just created one, we reset all the resources
			// to match the currencyListItem.
			TextView txTitleTextView = (TextView) convertView.findViewById(R.id.txTitle);
			txTitleTextView.setText(txListItem.getTxTitle());
			
			TextView txTimeTextView = (TextView) convertView.findViewById(R.id.txTime);
			txTimeTextView.setText(txListItem.getTxTime());
			
			TextView txAmount = (TextView) convertView.findViewById(R.id.txAmount);
			BigDecimal amt = OWCoin.formatCoinAmount(
					txListItem.getCoinId(), txListItem.getTxAmount());
			txAmount.setText(amt.toPlainString());

			TextView txAmountFiatEquiv = (TextView) 
					convertView.findViewById(R.id.txAmountFiatEquiv);
			BigDecimal fiatAmt = OWConverter.convert(txListItem.getTxAmount(), 
					txListItem.getCoinId(), txListItem.getFiatType());
			BigDecimal formattedfiatAmt = OWFiat.formatFiatAmount(
					txListItem.getFiatType(), fiatAmt);
			txAmountFiatEquiv.setText(fiatSymbol + formattedfiatAmt.toPlainString());
			
			ImageView txIOIcon = (ImageView) convertView.findViewById(R.id.txIOIcon);
			Drawable image = context.getResources().getDrawable(
					getImgResIdForItem(txListItem));
			txIOIcon.setImageDrawable(image);
			
			return convertView;

		} else {
			// Just have to make sure the text is set for the appropriate divider
			TextView walletTxListTextView = (TextView) 
					convertView.findViewById(R.id.walletTxListDividerTextView);
			walletTxListTextView.setText(txListItem.getTxTitle());
			return convertView;
		}

	}

	/**
	 * 
	 */
	@Override
	public void notifyDataSetChanged() {
		this.updateHistoryIndex();
		super.notifyDataSetChanged();
	}

	/**
	 * @param txListItem
	 * @return
	 */
	private int getImgResIdForItem(OWWalletTransactionListItem txListItem) {
		int imgResId;
		if (txListItem.getTxAmount().compareTo(BigDecimal.ZERO) >= 0) {
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

	private void updateHistoryIndex() {
		int newIndex = 0;
		do {
			newIndex++;
			// Safe because we are assuming that the data set will always end
			// with a transaction that is not pending. Even if the data set
			// only contains two items (the two dividers), the second will not 
			// be pending.
		} while (getItem(newIndex).isPending());
		this.historyIndex = newIndex;
	}
	
}