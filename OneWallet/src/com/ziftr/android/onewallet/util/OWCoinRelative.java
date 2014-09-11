package com.ziftr.android.onewallet.util;

public interface OWCoinRelative {
	
	/**
	 * Get the OWCoin for the actual implementer.
	 * 
	 * @return OWCoin.BTC for Bitcoin, OWCoin.LTC for Litecoin, etc.
	 */
	public OWCoin getCoinId();
	
}