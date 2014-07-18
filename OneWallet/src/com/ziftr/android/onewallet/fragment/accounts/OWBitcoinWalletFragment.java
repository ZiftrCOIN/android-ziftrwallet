package com.ziftr.android.onewallet.fragment.accounts;

import com.ziftr.android.onewallet.util.OWCoin;

public class OWBitcoinWalletFragment extends OWWalletFragment {

	/**
	 * Get the market exchange prefix for the 
	 * actual sub-classing coin fragment type.
	 * 
	 * @return OWCoin.Type.BTC for Bitcoin
	 */
	@Override
	public OWCoin.Type getCoinId() {
		return OWCoin.Type.BTC;
	}

}
