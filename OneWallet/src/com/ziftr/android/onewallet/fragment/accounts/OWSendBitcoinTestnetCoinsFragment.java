package com.ziftr.android.onewallet.fragment.accounts;

import com.ziftr.android.onewallet.util.OWCoin;


public class OWSendBitcoinTestnetCoinsFragment extends OWSendCoinsFragment {

	@Override
	public OWCoin.Type getCoinId() {
		return OWCoin.Type.BTC_TEST;
	}

	@Override
	public boolean addressIsValid(String address) {
		return address != null && 27 <= address.length() && address.length() <= 34; 
	}

}