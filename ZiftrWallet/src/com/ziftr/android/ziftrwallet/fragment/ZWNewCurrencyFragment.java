package com.ziftr.android.ziftrwallet.fragment;

import java.util.List;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWPreferencesUtils;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;

public class ZWNewCurrencyFragment extends ZWFragment implements OnItemClickListener{

	private ZWNewCurrencyListAdapter currencyAdapter;

	/** The root view for this application. */
	private View rootView;


	private ListView listView;

	@Override
	public void onResume() {
		super.onResume();
		this.getZWMainActivity().changeActionBar("CURRENCY", true, true, false);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		this.rootView = inflater.inflate(R.layout.currency_get_new_list, container, false);

		boolean includeTestnet = ZWPreferencesUtils.getDebugMode();
		List<ZWCoin> unactivatedCoins = this.getZWMainActivity().getWalletManager().getInactiveCoins(includeTestnet);
		
		if (unactivatedCoins.size() <= 0){
			this.rootView.findViewById(R.id.no_currency_message).setVisibility(View.VISIBLE);
		} else {
			this.rootView.findViewById(R.id.no_currency_message).setVisibility(View.GONE);
		}
		
		initializeCurrencyList(unactivatedCoins);
		return this.rootView;
	}

	public void initializeCurrencyList(List<ZWCoin> unactivatedCoins) {
		this.currencyAdapter = new ZWNewCurrencyListAdapter(this.getActivity(), unactivatedCoins);
		this.listView = (ListView) this.rootView.findViewById(R.id.currencyListView);
		listView.setFooterDividersEnabled(false);
		listView.setAdapter(this.currencyAdapter);
		listView.setOnItemClickListener(this);
	}


	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		ZWCoin coin = (ZWCoin) listView.getItemAtPosition(position);
		getZWMainActivity().addNewCurrency(coin);
	}

}
