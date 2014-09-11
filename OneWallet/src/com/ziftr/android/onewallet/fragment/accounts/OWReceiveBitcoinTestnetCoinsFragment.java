package com.ziftr.android.onewallet.fragment.accounts;

import com.ziftr.android.onewallet.util.OWCoin;


public class OWReceiveBitcoinTestnetCoinsFragment extends OWReceiveCoinsFragment {

	@Override
	public OWCoin getCoinId() {
		return OWCoin.BTC_TEST;
	}

}