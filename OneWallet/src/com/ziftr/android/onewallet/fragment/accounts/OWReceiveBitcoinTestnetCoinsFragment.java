package com.ziftr.android.onewallet.fragment.accounts;

import com.ziftr.android.onewallet.util.OWCoin;


public class OWReceiveBitcoinTestnetCoinsFragment extends OWReceiveCoinsFragment {

	@Override
	public OWCoin.Type getCoinId() {
		return OWCoin.Type.BTC_TEST;
	}

}