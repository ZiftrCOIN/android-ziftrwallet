/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWPreferences;
import com.ziftr.android.ziftrwallet.crypto.ZWFiat;

public class ZWSetFiatFragment extends ZWFragment implements OnItemClickListener{
	
	public static final String FRAGMENT_TAG = "set_fiat";
	
	private ZWFiatListAdapter fiatAdapter;

	private View rootView;
	private ListView listView;
	
	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {

		this.rootView = inflater.inflate(R.layout.settings_set_fiat_currency, container, false);
		
		this.fiatAdapter = new ZWFiatListAdapter(this.getActivity(), ZWFiat.getFiatTypesList());

		this.listView = (ListView) this.rootView.findViewById(R.id.fiatListView);
		listView.setFooterDividersEnabled(false);
		listView.setAdapter(this.fiatAdapter);
		listView.setOnItemClickListener(this);

		return this.rootView;
	}


	@Override
	public void onResume() {
		super.onResume();
		this.getZWMainActivity().changeActionBar(R.string.zw_actionbar_settings, false, true, false);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		ZWFiat fiatType = (ZWFiat) parent.getItemAtPosition(position);
		ZWPreferences.setFiatCurrency(fiatType.getName());
		getZWMainActivity().onBackPressed();
	}

}
