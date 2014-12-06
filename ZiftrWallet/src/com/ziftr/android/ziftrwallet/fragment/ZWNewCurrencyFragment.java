package com.ziftr.android.ziftrwallet.fragment;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;

public class ZWNewCurrencyFragment extends ZWFragment implements OnItemClickListener{

	private ZWNewCurrencyListAdapter currencyAdapter;

	/** The root view for this application. */
	private View rootView;

	/** We only want to show the coins that the user doesn't already have a wallet for. List of types from args */
	private List<ZWCoin> coinsToShow;

	private ListView listView;

	/** List of items currently used by adapter to display currencies */
	private List<ZWNewCurrencyListItem> currencyList = new ArrayList<ZWNewCurrencyListItem>();

	@Override
	public void onResume() {
		super.onResume();
		this.getZWMainActivity().changeActionBar("CURRENCY", true, true, false);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {

		this.rootView = inflater.inflate(R.layout.currency_get_new_list, container, false);

		initializeFromBundle(this.getArguments());

		generateCurrencyListFrom(this.coinsToShow, savedInstanceState);

		initializeCurrencyList();
		return this.rootView;
	}

	public void initializeCurrencyList() {
		this.currencyAdapter = new ZWNewCurrencyListAdapter(this.getActivity(), 
				R.layout.accounts_new_currency_list_item,this.currencyList);
		this.listView = (ListView) this.rootView.findViewById(R.id.currencyListView);
		listView.setFooterDividersEnabled(false);
		listView.setAdapter(this.currencyAdapter);
		listView.setOnItemClickListener(this);
	}

	public List<ZWNewCurrencyListItem> generateCurrencyListFrom(List<ZWCoin> list, Bundle savedInstanceState) {
		this.currencyList.clear();
		for (ZWCoin type : list) {
			ZWNewCurrencyListItem newCurrency = new ZWNewCurrencyListItem(type);
			this.currencyList.add(newCurrency);
		}
		return this.currencyList;
	}

	/**
	 * The bundle should have booleans put into it, one for each {@link ZWCoin}.
	 * The boolean should describe whether or not to include the currency
	 * in the view. Used only when initially navigating to currency screen from accounts
	 */
	@Override
	public void setArguments(Bundle args) {
		this.initializeFromBundle(args);
		if (this.getArguments() == null){
			super.setArguments(args);
		}
	}

	/**
	 * We initialize the list of coins to show in the dialog and then 
	 * for each ZWCoin.___.toString() that has a boolean value
	 * of true in the bundle we add it to the list.
	 *  
	 * @param args - The bundle with the booleans put into it. The keys are 
	 * the toString()s of the different ZWCoin possible values.
	 */
	private void initializeFromBundle(Bundle args) {
		this.coinsToShow = new ArrayList<ZWCoin>();
		if (args != null) {
			for (ZWCoin type : ZWCoin.values()) {
				if (args.getBoolean(type.getShortTitle())) {
					this.coinsToShow.add(type);
				}
			}
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		ZWNewCurrencyListItem item = (ZWNewCurrencyListItem)listView.getItemAtPosition(position);
		getZWMainActivity().addNewCurrency(item);
	}

}
