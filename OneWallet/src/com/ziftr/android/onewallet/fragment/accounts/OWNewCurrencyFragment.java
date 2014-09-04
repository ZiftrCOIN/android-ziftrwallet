package com.ziftr.android.onewallet.fragment.accounts;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.ziftr.android.onewallet.OWWalletManager;
import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.fragment.OWFragment;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.ZLog;

public class OWNewCurrencyFragment extends OWFragment {

	private OWNewCurrencyListAdapter currencyAdapter;
	
	/** The root view for this application. */
	private View rootView;
	
	/** We only want to show the coins that the user doesn't already have a wallet for. List of types from args */
	private List<OWCoin.Type> coinsToShow;
	
	/** List of items currently used by adapter to display currencies */
	private List<OWNewCurrencyListItem> currencyList = new ArrayList<OWNewCurrencyListItem>();
	
	private OWWalletManager walletManager;

	@Override
	public void onResume() {
		super.onResume();
		this.getOWMainActivity().changeActionBar("CURRENCY", true, true);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState){
		for (OWCoin.Type type : this.coinsToShow) {
			outState.putBoolean(type.toString(), true);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {

		this.walletManager = this.getOWMainActivity().getWalletManager();

		//rootView = inflater.inflate(R.layout.currency_get_new_list, container, false);
		if (savedInstanceState != null){
			initializeFromBundle(savedInstanceState);
		} else {
			initializeFromBundle(this.getArguments());
		}
		initializeCurrencyList();
		return this.rootView;
	}

	public void initializeCurrencyList(){
		setCurrencyList(this.coinsToShow);
		this.currencyAdapter = new OWNewCurrencyListAdapter(this.getActivity(), R.layout._dual_icon_coin_view, 
				this.currencyList);
		
		ListView listView = (ListView) this.rootView.findViewById(R.id.currencyListView);
		listView.setAdapter(this.currencyAdapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, 
					int position, long id) {
				OWNewCurrencyListItem newItem = (OWNewCurrencyListItem) 
						parent.getItemAtPosition(position);
				clickAddNewCurrency(newItem.getCoinId());
				//TODO remove addedCurrency from list
			}
		});
	}
	
	public List<OWNewCurrencyListItem> setCurrencyList(List<OWCoin.Type> list){
		this.currencyList.clear();
		for (OWCoin.Type type : list) {
			this.currencyList.add(new OWNewCurrencyListItem(type));
		}
		return this.currencyList;
	}
	
	/**
	 * The bundle should have booleans put into it, one for each {@link OWCoin.Type}.
	 * The boolean should describe whether or not to include the currency
	 * in the view. Used only when initially navigating to currency screen from accounts
	 */
	@Override
	public void setArguments(Bundle args) {
		this.initializeFromBundle(args);
		super.setArguments(args);
	}
	
	/**
	 * We initialize the list of coins to show in the dialog and then 
	 * for each OWCoin.Type.___.toString() that has a boolean value
	 * of true in the bundle we add it to the list.
	 *  
	 * @param args - The bundle with the booleans put into it. The keys are 
	 * the toString()s of the different OWCoin.Type possible values.
	 */
	private void initializeFromBundle(Bundle args) {
		this.coinsToShow = new ArrayList<OWCoin.Type>();
		if (args != null) {
			for (OWCoin.Type type : OWCoin.Type.values()) {
				if (args.getBoolean(type.toString())) {
					this.coinsToShow.add(type);
				}
			}
		}
	}
	
	public void clickAddNewCurrency(OWCoin.Type newItem) {
		ZLog.log("handle new currency in accounts fragment");
		// TODO make sure that this view only has wallets
		// in it which the user do
		for (OWCoin.Type type : this.walletManager.getAllUsersWalletTypes()) {
			if (type == newItem) {
				// Already in list, shouldn't ever get here though because
				// we only show currencies in the dialog which we don't have
				return;
			}
		}

		// TODO add this for all coin types eventually 
		// walletManager.setupWallet(newItem.getCoinId());
		// Right now we are just doing it for the testnet
		if (newItem == OWCoin.Type.BTC_TEST) {
			// We can assume the wallet hasn't been set up yet
			// or we wouldn't have gotten here 

			// TODO see java docs for this method. Is it okay to 
			// have this called here? 
			if (walletManager.setUpWallet(newItem)) {
				ZLog.log("added new currency " + newItem.toString());
				this.coinsToShow.remove(newItem);
				setCurrencyList(this.coinsToShow);
				this.currencyAdapter.notifyDataSetChanged();
			}
		}
		
	}

	
}
