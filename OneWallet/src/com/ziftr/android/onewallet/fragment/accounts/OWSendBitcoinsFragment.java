package com.ziftr.android.onewallet.fragment.accounts;

import com.ziftr.android.onewallet.util.OWCoin;


public class OWSendBitcoinsFragment extends OWSendCoinsFragment {

	@Override
	public OWCoin.Type getCoinId() {
		return OWCoin.Type.BTC;
	}

	@Override
	public boolean addressIsValid(String address) {
		// This is just a quick check for now, probably won't even 
		// want this method here. Maybe go into OWCoin.Type enums? 
		return address != null && 27 <= address.length() && address.length() <= 34; 
	}

}