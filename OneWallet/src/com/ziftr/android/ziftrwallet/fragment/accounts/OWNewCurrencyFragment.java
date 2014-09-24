package com.ziftr.android.ziftrwallet.fragment.accounts;

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
import com.ziftr.android.ziftrwallet.fragment.OWFragment;
import com.ziftr.android.ziftrwallet.util.OWCoin;
import com.ziftr.android.ziftrwallet.util.OWRequestCodes;
import com.ziftr.android.ziftrwallet.util.OWTags;

public class OWNewCurrencyFragment extends OWFragment {

	private OWNewCurrencyListAdapter currencyAdapter;
	
	/** The root view for this application. */
	private View rootView;
	
	/** We only want to show the coins that the user doesn't already have a wallet for. List of types from args */
	private List<OWCoin> coinsToShow;
	
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

		rootView = inflater.inflate(R.layout.currency_get_new_list, container, false);
		initializeFromBundle(this.getArguments());
		initializeCurrencyList();
		return this.rootView;
	}

	public void initializeCurrencyList() {
		generateCurrencyListFrom(this.coinsToShow);
		this.currencyAdapter = new OWNewCurrencyListAdapter(this.getActivity(), R.layout.accounts_new_currency_list_item,
				this.currencyList);
		
		ListView listView = (ListView) this.rootView.findViewById(R.id.currencyListView);
		listView.addFooterView(this.getActivity().getLayoutInflater().inflate(R.layout.dropshadowlayout, null), null, false);
		listView.setFooterDividersEnabled(false);
		listView.setAdapter(this.currencyAdapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, 
					int position, long id) {
				OWNewCurrencyListItem newItem = (OWNewCurrencyListItem) 
						parent.getItemAtPosition(position);
				
				Bundle b = new Bundle();
				b.putString(OWCoin.TYPE_KEY, newItem.getCoinId().toString());
				if (getOWMainActivity().userHasPassphrase()){
					getOWMainActivity().showGetPassphraseDialog(OWRequestCodes.VALIDATE_PASSPHRASE_DIALOG_NEW_CURRENCY, b, 
						OWTags.VALIDATE_PASS_NEW_COIN);
				} else {
					getOWMainActivity().addNewCurrency(b);
				}
			}
		});
	}
	
	public List<OWNewCurrencyListItem> generateCurrencyListFrom(List<OWCoin> list) {
		this.currencyList.clear();
		for (OWCoin type : list) {
			this.currencyList.add(new OWNewCurrencyListItem(type));
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
	
}
