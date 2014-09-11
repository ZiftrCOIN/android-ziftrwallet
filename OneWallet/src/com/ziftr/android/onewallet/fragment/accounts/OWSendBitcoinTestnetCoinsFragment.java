package com.ziftr.android.onewallet.fragment.accounts;

import com.ziftr.android.onewallet.util.OWCoin;


public class OWSendBitcoinTestnetCoinsFragment extends OWSendCoinsFragment {

	@Override
	public OWCoin getCoinId() {
		return OWCoin.BTC_TEST;
	}

	@Override
	public boolean addressIsValid(String address) {
		// This is just a quick check for now, probably won't even 
		// want this method here. Maybe go into OWCoin enums? 
		return address != null && 27 <= address.length() && address.length() <= 34; 
	}

}