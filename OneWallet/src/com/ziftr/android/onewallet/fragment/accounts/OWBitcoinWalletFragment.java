package com.ziftr.android.onewallet.fragment.accounts;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.params.MainNetParams;
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

	@Override
	public NetworkParameters getCoinNetworkParameters() {
		// TODO for now return the test network, obviously this 
		// needs to be changed to use the real network at some point
		return MainNetParams.get();
	}

}
