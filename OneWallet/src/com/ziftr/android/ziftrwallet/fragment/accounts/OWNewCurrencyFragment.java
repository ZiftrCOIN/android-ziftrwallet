package com.ziftr.android.ziftrwallet.fragment.accounts;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.fragment.OWFragment;
import com.ziftr.android.ziftrwallet.util.OWCoin;

public class OWNewCurrencyFragment extends OWFragment implements OnClickListener{

	private OWNewCurrencyListAdapter currencyAdapter;

	/** The root view for this application. */
	private View rootView;

	/** We only want to show the coins that the user doesn't already have a wallet for. List of types from args */
	private List<OWCoin> coinsToShow;

	private ImageView addAllCurrencies;

	private ListView listView;

	private static final String SAVED_CURRENCIES = "SAVED_CURRENCIES";

	/** List of items currently used by adapter to display currencies */
	private List<OWNewCurrencyListItem> currencyList = new ArrayList<OWNewCurrencyListItem>();

	@Override
	public void onResume() {
		super.onResume();
		this.getOWMainActivity().changeActionBar("CURRENCY", false, true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {

		this.rootView = inflater.inflate(R.layout.currency_get_new_list, container, false);

		this.addAllCurrencies = (ImageView)rootView.findViewById(R.id.addAllCheckedCurrencies);
		addAllCurrencies.setOnClickListener(this);
		initializeFromBundle(this.getArguments());
		generateCurrencyListFrom(this.coinsToShow, savedInstanceState);

		initializeCurrencyList();
		return this.rootView;
	}

	@Override
	public void onSaveInstanceState(Bundle outState){
		ArrayList<String> checked = new ArrayList<String>();
		for (OWNewCurrencyListItem item : this.currencyList){
			if (item.isChecked()){
				checked.add(item.getCoinId().toString());
			}
		}
		outState.putStringArrayList(SAVED_CURRENCIES, checked);
		super.onSaveInstanceState(outState);
	}

	public void initializeCurrencyList() {
		this.currencyAdapter = new OWNewCurrencyListAdapter(this.getActivity(), R.layout.accounts_new_currency_list_item,
				this.currencyList);
		this.listView = (ListView) this.rootView.findViewById(R.id.currencyListView);
		listView.setFooterDividersEnabled(false);
		listView.setAdapter(this.currencyAdapter);
	}

	public List<OWNewCurrencyListItem> generateCurrencyListFrom(List<OWCoin> list, Bundle savedInstanceState) {
		this.currencyList.clear();
		ArrayList<String> checked = new ArrayList<String>();
		if (savedInstanceState != null && savedInstanceState.containsKey(SAVED_CURRENCIES)){
			checked = savedInstanceState.getStringArrayList(SAVED_CURRENCIES);
		}
		for (OWCoin type : list) {
			OWNewCurrencyListItem newCurrency = new OWNewCurrencyListItem(type);
			if (checked.contains(type.toString())){
				newCurrency.setIsChecked(true);
			}
			this.currencyList.add(newCurrency);
		}
		return this.currencyList;
	}

	/**
	 * The bundle should have booleans put into it, one for each {@link OWCoin}.
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
	 * for each OWCoin.___.toString() that has a boolean value
	 * of true in the bundle we add it to the list.
	 *  
	 * @param args - The bundle with the booleans put into it. The keys are 
	 * the toString()s of the different OWCoin possible values.
	 */
	private void initializeFromBundle(Bundle args) {
		this.coinsToShow = new ArrayList<OWCoin>();
		if (args != null) {
			for (OWCoin type : OWCoin.values()) {
				if (args.getBoolean(type.toString())) {
					this.coinsToShow.add(type);
				}
			}
		}
	}

	@Override
	public void onClick(View v) {
		if (v == this.addAllCurrencies){
			ArrayList<String> toAdd = new ArrayList<String>();
			for (OWNewCurrencyListItem item : this.currencyList){
				if (item.isChecked()){
					toAdd.add(item.getCoinId().toString());
				}
			}
			Bundle b = new Bundle();
			b.putStringArrayList(OWCoin.TYPE_KEY, toAdd);

			getOWMainActivity().addNewCurrency(b);
		}
	}

}
