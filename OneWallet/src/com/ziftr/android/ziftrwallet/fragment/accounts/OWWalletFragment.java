package com.ziftr.android.ziftrwallet.fragment.accounts;

import java.util.Collection;

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
import android.widget.ListView;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.crypto.OWTransaction;
import com.ziftr.android.ziftrwallet.network.OWDataSyncHelper;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

/**
 * This is the abstract superclass for all of the individual Wallet type
 * Fragments. It provides some generic methods that will be useful
 * to all Fragments of the OneWallet. 
 * 
 * TODO in the evenListener for the search bar, make a call to notifyDatasetChanged
 * so that all the transactions that don't match are filtered out. 
 */
public class OWWalletFragment extends OWWalletUserFragment implements TextWatcher {

	/** The root view for this application. */
	private View rootView;

	private ListView txListView;

	private OWWalletTransactionListAdapter txAdapter;

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
	public void onResume() {
		super.onResume();
		// Changes action bar and registers this as the listener for search button clicks
		this.getOWMainActivity().changeActionBar("ACCOUNT", true, true, this, this.txAdapter);
	}

	@Override
	public void onPause() {
		this.getOWMainActivity().unregisterSearchBarTextWatcher(this);
		super.onPause();
	}

	@Override
	public void onStop() {
		//detoggle search bar if user navigates away
		View searchBar = this.getOWMainActivity().findViewById(R.id.searchBar);
		searchBar.setVisibility(View.GONE);

		super.onStop();
	}

	/**
	 * When the view is created, we must initialize the header, the list
	 * view with all of the transactions in it, and the buttons at the
	 * bottom of the screen. 
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {

		if (getWalletManager() == null || getWalletManager().getWallet(getSelectedCoin()) == null) {
			throw new IllegalArgumentException(
					"Shouldn't happen, this is just a safety check because we "
							+ "need a wallet to send coins.");
		}

		rootView = inflater.inflate(R.layout.accounts_wallet, container, false);

		this.showWalletHeader();

		this.initializeTxListView();

		this.initializeButtons();

		return this.rootView;
	}


	private void initializeTxListView() {
		// TODO make this non-UI blocking
		this.txAdapter = new OWWalletTransactionListAdapter(this.getActivity());

		Collection<OWTransaction> pendingTxs = 
				this.getWalletManager().getPendingTransactions(getSelectedCoin());

		if (pendingTxs.size() > 0) {
			// Add the Pending Divider
			this.txAdapter.getFullList().add(
					new OWTransaction(
							null, null, "PENDING", -1, null, 
							OWWalletTransactionListAdapter.Type.PENDING_DIVIDER,
							R.layout.accounts_wallet_tx_list_divider));
			// Add all the pending transactions
			for (OWTransaction tx : pendingTxs) {
				this.txAdapter.getFullList().add(tx);
			}
		}

		// Add the History Divider
		this.txAdapter.getFullList().add(new OWTransaction(
				null, null, "HISTORY", -1, null, 
				OWWalletTransactionListAdapter.Type.HISTORY_DIVIDER,
				R.layout.accounts_wallet_tx_list_divider));
		// Add all the pending transactions
		for (OWTransaction tx : this.getWalletManager().getConfirmedTransactions(getSelectedCoin())) {
			this.txAdapter.getFullList().add(tx);
		}

		this.txAdapter.refreshWorkingList(true);
		this.txAdapter.notifyDataSetChanged();

		this.txListView = (ListView) this.rootView.findViewById(R.id.txListView);
		this.txListView.setAdapter(this.txAdapter);

		// Opens transactions details fragment by calling openTxnDetails in MainActivity, passing
		// the txnItem when user clicks a txn list item.
		this.txListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (!txAdapter.getItem(position).isDivider()) {
					OWTransaction txItem = (OWTransaction) 
							txListView.getItemAtPosition(position);
					getOWMainActivity().openTxnDetails(txItem);
				}
			}
		});

	}

	private void initializeButtons() {
		Button receiveButton = (Button) 
				this.rootView.findViewById(R.id.leftButton);
		receiveButton.setText("RECEIVE");
		receiveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getOWMainActivity().openReceiveCoinsView();
			}
		});

		Button sendButton = (Button) 
				this.rootView.findViewById(R.id.rightButton);
		sendButton.setText("SEND");
		sendButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getOWMainActivity().openSendCoinsView();
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
		//nothing
	}

	@Override
	public void afterTextChanged(Editable s) {
		// TODO Auto-generated method stub
		this.txAdapter.getFilter().filter(s);
	}

	
	
	@Override
	public void refreshData() {
		ZiftrUtils.runOnNewThread(new Runnable() {
			
			@Override
			public void run() {
				OWDataSyncHelper.updateTransactionHistory(getOWMainActivity().getSelectedCoin());
			}
		});
	}
}
