package com.ziftr.android.onewallet.fragment.accounts;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.params.TestNet3Params;
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

	@Override
	public NetworkParameters getCoinNetworkParameters() {
		// TODO for now return the test network, obviously this 
		// needs to be changed to use the real network at some point
		return TestNet3Params.get();
	}

}
