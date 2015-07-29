/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.fragment;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWPreferences;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWConverter;
import com.ziftr.android.ziftrwallet.crypto.ZWFiat;
import com.ziftr.android.ziftrwallet.dialog.ZiftrSimpleDialogFragment;
import com.ziftr.android.ziftrwallet.network.ZWDataSyncHelper;
import com.ziftr.android.ziftrwallet.network.ZiftrNetworkManager;
import com.ziftr.android.ziftrwallet.sqlite.ZWWalletManager;

/**
 * The ZWMainActivity starts this fragment. This fragment is 
 * associated with list view of user wallets and a bar at the bottom of the list
 * view which opens a dialog to add rows to the list view.  
 */
public class ZWAccountsFragment extends ZWFragment implements OnItemClickListener, OnItemLongClickListener {

	/** The view container for this fragment. */
	private View rootView;

	/** The context for this fragment. */
	private FragmentActivity mContext;

	/** The list view which shows a link to open each of the wallets/currency types. */
	private ListView currencyListView;

	/** The wallet manager. This fragment works with */
	private ZWWalletManager walletManager;

	private List<ZWCoin> activatedCoins;
	private ArrayAdapter<ZWCoin> walletAdapter;

	private View footer;
	
	private TextView totalBalance;
	private ZWFiat fiatType;
	
	
	/**
	 * a small listener class for handling when the user ok's removing a coin type
	 *
	 */
	private static class DialogListener implements DialogInterface.OnClickListener {
		
		private ZWCoin coin;
		
		public DialogListener(ZWCoin coin) {
			this.coin = coin;
		}
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			if(which == DialogInterface.BUTTON_POSITIVE) {
				ZWWalletManager.getInstance().deactivateCoin(coin);
				
				//in this case data was updated by the UI, not network, but this delivers the same message to the UI
				ZiftrNetworkManager.dataUpdated(); 
			}
			
		}
	};
	
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public void onSaveInstanceState(Bundle outState) { 
		super.onSaveInstanceState(outState);
	}


	@Override
	public void onResume() {
		super.onResume();
		this.getZWMainActivity().changeActionBar("ziftrWALLET", true, false, true);
		this.refreshData(true);
	}

	/**
	 * To create the view for this fragment, we start with the 
	 * basic fragment_home view which just has a few buttons that
	 * open up different coin-type wallets.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		this.rootView = inflater.inflate(R.layout.section_accounts_layout, container, false);

		this.walletManager = this.getZWMainActivity().getWalletManager();

		//dropshadow under listview
		this.footer = this.getActivity().getLayoutInflater().inflate(R.layout.dropshadowlayout, null);

		this.totalBalance = (TextView) this.rootView.findViewById(R.id.user_total_balance);
		
		// Initialize the list of user wallets that they can open
		this.initializeCurrencyListView();
		
		//show Add Currency Message if user has no wallets
		this.toggleAddCurrencyMessage();
		
		this.fiatType = ZWPreferences.getFiatCurrency();

		this.calculateTotal();
		// Return the view which was inflated
		return rootView;
	}

	/**
	 * Whenever this is fragment is attached to an activity 
	 * we save a reference to the activity.
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.mContext = (FragmentActivity) activity;
	}


	/**
	 * Sets up the data set, the adapter, and the onclick for 
	 * the list view of currencies that the user currently has a 
	 * wallet for.
	 */
	private void initializeCurrencyListView() {
		// Get the values from the manager and initialize the list view from them
		
		this.activatedCoins = this.walletManager.getActivatedCoins();
		
		this.currencyListView = (ListView) 
		this.rootView.findViewById(R.id.listOfUserWallets);

		// The dropshadow at the bottom
		if (this.activatedCoins.size() > 0){
			this.currencyListView.addFooterView(this.footer, null ,false);
		}
		this.currencyListView.setFooterDividersEnabled(false);

		this.walletAdapter = new ZWCurrencyListAdapter(this.mContext, this.activatedCoins);
		this.currencyListView.setAdapter(this.walletAdapter);

		this.currencyListView.setOnItemClickListener(this);
		
		//Long click to deactivate wallet
		this.currencyListView.setOnItemLongClickListener(this);
	}
	
	
	public void toggleAddCurrencyMessage(){
		if (this.activatedCoins.size() <=0){
			this.rootView.findViewById(R.id.add_currency_message).setVisibility(View.VISIBLE);
			this.rootView.findViewById(R.id.total_label).setVisibility(View.GONE);
			this.totalBalance.setVisibility(View.GONE);
		} else {
			this.rootView.findViewById(R.id.add_currency_message).setVisibility(View.GONE);
			this.rootView.findViewById(R.id.total_label).setVisibility(View.VISIBLE);
			this.totalBalance.setVisibility(View.VISIBLE);
		}

	}
	
	private void calculateTotal(){
		BigDecimal total = BigDecimal.ZERO;
		ZWFiat fiat = ZWPreferences.getFiatCurrency();
		
		for(ZWCoin coin : this.activatedCoins) {
			BigInteger balanceAtomic = walletManager.getWalletBalance(coin);
			BigDecimal balance = coin.getAmount(balanceAtomic);
			total = total.add(ZWConverter.convert(balance, coin, fiat));
		}

		this.totalBalance.setText(this.fiatType.getSymbol() + total.setScale(fiat.getScale(), BigDecimal.ROUND_HALF_UP).toPlainString());
	}
	
	
	//this updates transaction history for all activated wallets
	@Override
	public void refreshData(final boolean autorefresh) {	
		ZWDataSyncHelper.updateTransactionHistories(activatedCoins, autorefresh);
	}
	

	@Override
	public void onDataUpdated() {
		super.onDataUpdated();
		
		//we just completely replace the old data with new data, so just replace the adapter instead of updating it
		this.activatedCoins = this.walletManager.getActivatedCoins();
		this.walletAdapter = new ZWCurrencyListAdapter(this.mContext, this.activatedCoins);
		//this.walletAdapter.notifyDataSetChanged();
		
		this.currencyListView.setAdapter(this.walletAdapter);
		
		this.toggleAddCurrencyMessage();
	}
	

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		
		ZWCoin coin = (ZWCoin) parent.getItemAtPosition(position);
		
		ZiftrSimpleDialogFragment dialog = new ZiftrSimpleDialogFragment();
		dialog.setupDialog(0, R.string.zw_dialog_deactivate_message, R.string.zw_dialog_deactivate, R.string.zw_dialog_cancel);

		dialog.setOnClickListener(new ZWAccountsFragment.DialogListener(coin));
		dialog.show(getFragmentManager(), "deactivate_coin");
		
		// remove the dropshadow at the bottom if no more coins left
		if (this.activatedCoins.size() > 0){
			this.currencyListView.removeFooterView(this.footer);
		}
		return true;
	}

	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ZWCoin coin = (ZWCoin) parent.getItemAtPosition(position);
		getZWMainActivity().openWalletView(coin);		
	}

	
}

