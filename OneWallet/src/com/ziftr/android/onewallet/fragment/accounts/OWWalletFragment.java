package com.ziftr.android.onewallet.fragment.accounts;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.bitcoin.core.AbstractWalletEventListener;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet;
import com.ziftr.android.onewallet.OWMainFragmentActivity;
import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWCoin.Type;
import com.ziftr.android.onewallet.util.OWConverter;
import com.ziftr.android.onewallet.util.OWFiat;
import com.ziftr.android.onewallet.util.OWUtils;
import com.ziftr.android.onewallet.util.ZLog;

/**
 * This is the abstract superclass for all of the individual Wallet type
 * Fragments. It provides some generic methods that will be useful
 * to all Fragments of the OneWallet. 
 * 
 * TODO in the evenListener for the search bar, make a call to notifyDatasetChanged
 * so that all the transactions that don't match are filtered out. 
 */
public abstract class OWWalletFragment extends OWWalletUserFragment implements TextWatcher {

	/** The root view for this application. */
	private View rootView;

	private ListView txListView;

	private List<OWWalletTransactionListItem> txList;

	/**
	 * When this fragment is created, this method is called
	 * unless it is overridden. 
	 * 
	 * @param savedInstanceState - The last state of this fragment.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onResume(){
		super.onResume();
		this.getOWMainActivity().changeActionBar("ACCOUNT", true, true, true);

	}
	

	/**
	 * When the view is created, we must initialize the header, the list
	 * view with all of the transactions in it, and the buttons at the
	 * bottom of the screen. 
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {

		if (getWallet() == null) {
			throw new IllegalArgumentException(
					"Shouldn't happen, this is just a safety check because we "
							+ "need a wallet to send coins.");
		}

		rootView = inflater.inflate(R.layout.accounts_wallet, container, false);

		this.initializeWalletHeaderView();

		this.initializeTxListView();

		this.initializeButtons();

		return this.rootView;
	}

	private void initializeWalletHeaderView() {
		View headerView = this.rootView.findViewById(R.id.walletHeader);

		ImageView coinLogo = (ImageView) (headerView.findViewById(R.id.leftIcon));
		Drawable coinImage = this.getActivity().getResources().getDrawable(
				getCoinId().getLogoResId());
		coinLogo.setImageDrawable(coinImage);

		TextView coinTitle = (TextView) headerView.findViewById(R.id.topLeftTextView);
		coinTitle.setText(getCoinId().getLongTitle());

		TextView coinUnitPriceInFiatTextView = (TextView) 
				headerView.findViewById(R.id.bottomLeftTextView);
		BigDecimal unitPriceInFiat = OWConverter.convert(
				BigDecimal.ONE, getCoinId(), OWFiat.Type.USD);
		coinUnitPriceInFiatTextView.setText(OWFiat.Type.USD.getSymbol() + 
				OWFiat.formatFiatAmount(OWFiat.Type.USD, 
						unitPriceInFiat).toPlainString());

		TextView walletBalanceTextView = (TextView) 
				headerView.findViewById(R.id.topRightTextView);
		BigDecimal walletBallance = OWUtils.bigIntToBigDec(getCoinId(), 
				getWallet().getBalance(Wallet.BalanceType.ESTIMATED));
		walletBalanceTextView.setText(
				OWCoin.formatCoinAmount(getCoinId(), walletBallance).toPlainString());

		TextView walletBalanceFiatEquivTextView = (TextView) 
				headerView.findViewById(R.id.bottomRightTextView);
		BigDecimal walletBalanceFiatEquiv = OWConverter.convert(
				walletBallance, getCoinId(), OWFiat.Type.USD);
		walletBalanceFiatEquivTextView.setText(OWFiat.Type.USD.getSymbol() + 
				walletBalanceFiatEquiv.toPlainString());

		ImageView marketIcon = (ImageView) (headerView.findViewById(R.id.rightIcon));
		Drawable marketImage = this.getActivity().getResources().getDrawable(
				R.drawable.stats_enabled);
		marketIcon.setImageDrawable(marketImage);
	}

	private void initializeTxListView() {
		this.txList = new ArrayList<OWWalletTransactionListItem>();
		Collection<Transaction> pendingTxs = this.getWallet().getPendingTransactions();
		Set<String> pendingTxHashes = null;
		if (pendingTxs.size() > 0) {
			// Add the Pending Divider
			this.txList.add(new OWWalletTransactionListItem(
					null, null, "PENDING", null, null, 
					OWWalletTransactionListItem.Type.PendingDivider, 
					R.layout.accounts_wallet_tx_list_divider));
			pendingTxHashes = new HashSet<String>();
			// Add all the pending transactions
			for (Transaction tx : pendingTxs) {
				this.txList.add(new OWWalletTransactionListItem(
						getCoinId(), 
						OWFiat.Type.USD, 
						tx.getHashAsString().substring(0, 6), 
						OWUtils.formatterNoTimeZone.format(tx.getUpdateTime()),
						OWUtils.bigIntToBigDec(getCoinId(), tx.getValue(getWallet())), 
						OWWalletTransactionListItem.Type.PendingTransaction, 
						R.layout.accounts_wallet_tx_list_item));
				pendingTxHashes.add(tx.getHashAsString());
			}
		}

		// Add the History Divider
		this.txList.add(new OWWalletTransactionListItem(
				null, null, "HISTORY", null, null, 
				OWWalletTransactionListItem.Type.HistoryDivider, 
				R.layout.accounts_wallet_tx_list_divider));
		// Add all the pending transactions
		for (Transaction tx : this.getWallet().getTransactionsByTime()) {
			if (pendingTxHashes == null || (pendingTxHashes != null && 
					!pendingTxHashes.contains(tx.getHashAsString()))) {
				this.txList.add(new OWWalletTransactionListItem(
						getCoinId(), 
						OWFiat.Type.USD, 
						tx.getHashAsString().substring(0, 6), 
						OWUtils.formatterNoTimeZone.format(tx.getUpdateTime()),
						OWUtils.bigIntToBigDec(getCoinId(), tx.getValue(getWallet())), 
						OWWalletTransactionListItem.Type.ConfirmedTransaction, 
						R.layout.accounts_wallet_tx_list_item));
			}
		}

		this.txListView = (ListView)
				this.rootView.findViewById(R.id.txListView);
		this.txListView.setAdapter(new OWWalletTransactionListAdapter(
				this.getActivity(), this.txList));

		/**
		 *  Opens transactions details fragment by calling openTxnDetails in MainActivity, passing
		 *  the txnItem when user clicks a txn list item.
		 */
		this.txListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				OWWalletTransactionListItem txItem = (OWWalletTransactionListItem) 
						txListView.getItemAtPosition(position);
				OWMainFragmentActivity mActivity = 
						(OWMainFragmentActivity) getActivity();

				mActivity.openTxnDetails(txItem);
			}
		});

		this.getWallet().addEventListener(new AbstractWalletEventListener() {
			@Override
			public synchronized void onCoinsReceived(
					Wallet w, 
					Transaction tx, 
					BigInteger prevBalance, 
					final BigInteger newBalance) {
				// TODO need to update the data in the list of transactions
				ZLog.log("coins received...d..d");
			}

			// TODO override more methods here to do things like changing 
			// transactions from pending to finalized, etc.
		});
	}

	private void initializeButtons() {
		Button receiveButton = (Button) 
				this.rootView.findViewById(R.id.leftButton);
		receiveButton.setText("RECEIVE");
		receiveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				OWMainFragmentActivity mActivity = 
						(OWMainFragmentActivity) getActivity();
				mActivity.openReceiveCoinsView(getCoinId());
			}
		});

		Button sendButton = (Button) 
				this.rootView.findViewById(R.id.rightButton);
		sendButton.setText("SEND");
		sendButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				OWMainFragmentActivity mActivity = 
						(OWMainFragmentActivity) getActivity();
				mActivity.openSendCoinsView(getCoinId());
			}
		});

	}


	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterTextChanged(Editable s) {
		// TODO Auto-generated method stub
		
	}

}
