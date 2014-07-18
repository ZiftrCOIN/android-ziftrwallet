package com.ziftr.android.onewallet.fragment.accounts;

import com.ziftr.android.onewallet.util.OWCoin;

public class OWBitcoinTestnetWalletFragment extends OWWalletFragment {

	/**
	 * Get the market exchange prefix for the 
	 * actual sub-classing coin fragment type.
	 * 
	 * @return "BTC" for Bitcoin
	 */
	@Override
	public OWCoin.Type getCoinId() {
		return OWCoin.Type.BTC_TEST;
	}

}
