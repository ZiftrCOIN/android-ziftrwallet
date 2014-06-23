package com.ziftr.android.onewallet;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.params.TestNet3Params;

public class OWBitcoinWalletFragment extends OWWalletFragment {

	@Override
	public String getCoinPrefix() {
		return "BTC";
	}

	@Override
	public NetworkParameters getCoinNetworkParameters() {
		// TODO for now return the test network, obviously this 
		// needs to be changed to use the real network at some point
		return TestNet3Params.get();
	}


}
