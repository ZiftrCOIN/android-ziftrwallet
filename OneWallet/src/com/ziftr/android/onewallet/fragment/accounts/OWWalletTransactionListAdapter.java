package com.ziftr.android.onewallet.fragment.accounts;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
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
public class OWWalletTransactionListAdapter extends OWSearchableListAdapter {
	
	private List<OWWalletTransaction> fullPendingTxList;
	
	private List<OWWalletTransaction> fullConfirmedTxList;

	// TODO add a boolean returner that determines whether or not to add items
	// to the list. This can be used in searching

	/** Standard entries for transactions. */
	public static final int transactionType = 0;
	
	/** The divider type. */
	public static final int dividerType = 1;

	private OWWalletTransactionListAdapter(Context ctx, List<OWWalletTransaction> txList) {
		super(ctx, txList);
	}
	
	public OWWalletTransactionListAdapter(Context context) {
		// Had to add this constructor to get rid of warnings, for some reason.
	    this(context, new ArrayList<OWWalletTransaction>(), new ArrayList<OWWalletTransaction>());
	}
	
	private OWWalletTransactionListAdapter(Context ctx, 
			List<OWWalletTransaction> fullPendingTxList, 
			List<OWWalletTransaction> fullConfirmedTxList) {
		this(ctx, combineLists(fullPendingTxList, fullConfirmedTxList));
		this.fullPendingTxList = fullPendingTxList;
		this.fullConfirmedTxList = fullConfirmedTxList;
	}
	
	private static List<OWWalletTransaction> combineLists(
			List<OWWalletTransaction> fullPendingTxList, 
			List<OWWalletTransaction> fullConfirmedTxList) {
		List<OWWalletTransaction> combined = new ArrayList<OWWalletTransaction>();
		combined.addAll(fullPendingTxList);
		combined.addAll(fullConfirmedTxList);
		return combined;
	}
	
	@Override
	public void refreshWorkingList() {
		this.refreshWorkingList(this.fullPendingTxList, this.fullConfirmedTxList);
	}
	
	private void refreshWorkingList(List<OWWalletTransaction> pendingTxList, 
			List<OWWalletTransaction> confirmedTxList) {
		this.getWorkingTxList().clear();
		this.getWorkingTxList().addAll(combineLists(pendingTxList, confirmedTxList));
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
		if (item.getTxType() == OWWalletTransaction.Type.Divider) {
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

		OWWalletTransaction txListItem = getItem(position);
		if (convertView == null) {
			// If it doesn't have an old view then we make a new one 
			convertView = this.getInflater().inflate(txListItem.getResId(), null);
		}

		if (getItemViewType(position) == transactionType) {

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

	@SuppressLint("DefaultLocale")
	@Override
	public Filter getFilter(){
		Filter filter = new Filter() {

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				// Seems a little hacky, but we don't use the filter results at all
				
				List<OWWalletTransaction> workingPendingTxList = 
						new ArrayList<OWWalletTransaction>();
				List<OWWalletTransaction> workingConfirmedTxList = 
						new ArrayList<OWWalletTransaction>();
				
				constraint = constraint.toString().toLowerCase();
				
				for (OWWalletTransaction item : fullPendingTxList) {
					if (getItemViewType(item) == dividerType ||
							item.getTxNote().toLowerCase().contains(constraint)) {
						workingPendingTxList.add(item);
					}
				}
				
				// If only the divider is in the list then we don't need the divider
				if (workingPendingTxList.size() == 1) {
					workingPendingTxList.clear();
				}
				
				for (OWWalletTransaction item : fullConfirmedTxList) {
					if (getItemViewType(item) == dividerType ||
							item.getTxNote().toLowerCase().contains(constraint)) {
						workingConfirmedTxList.add(item);
					}
				}
				
				refreshWorkingList(workingPendingTxList, workingConfirmedTxList);
				
				return null;
			}

			@Override
			protected void publishResults(CharSequence constraint,
					FilterResults results) {
				notifyDataSetChanged();
			}

		};
		return filter;
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
	
	public List<OWWalletTransaction> getFullPendingTxList() {
		return fullPendingTxList;
	}

	public List<OWWalletTransaction> getFullConfirmedTxList() {
		return fullConfirmedTxList;
	}

}