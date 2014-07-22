package com.ziftr.android.onewallet.fragment.accounts;

import com.ziftr.android.onewallet.util.OWCoin;


public class OWSendBitcoinsFragment extends OWSendCoinsFragment {

	@Override
	public OWCoin.Type getCoinId() {
		return OWCoin.Type.BTC;
	}

	@Override
	public boolean addressIsValid(String address) {
		return address != null && 27 <= address.length() && address.length() <= 34; 
	}

}