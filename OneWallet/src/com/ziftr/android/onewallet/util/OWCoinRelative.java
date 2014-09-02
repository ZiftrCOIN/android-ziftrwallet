package com.ziftr.android.onewallet.util;

public interface OWCoinRelative {
	
	/**
	 * Get the OWCoin.Type for the actual implementer.
	 * 
	 * @return OWCoin.Type.BTC for Bitcoin, OWCoin.Type.LTC for Litecoin, etc.
	 */
	public OWCoin.Type getCoinId();
	
}