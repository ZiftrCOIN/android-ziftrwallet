package com.ziftr.android.ziftrwallet.fragment;

import com.ziftr.android.ziftrwallet.crypto.ZWCoin;

/** 
 * Each one of these views has a an image with a text view under it,
 * along with a '+' icon in the top right.
 * The text view should contain the title of the coin.
 * 
 * This doesn't contain a lot of functionality right now, but could
 * be helpful if we need to ad more to each one of these later. 
 */
public class ZWNewCurrencyListItem {
	
	/** The name of the coin to put under the logo. */
	private ZWCoin coinType;
	
	/**
	 * Make a new {@link ZWNewCurrencyListItem} and set the appropriate
	 * fields. 
	 * 
	 * @param coinLogoResId
	 * @param coinType
	 */
	public ZWNewCurrencyListItem(ZWCoin coinType) {
		this.setCoinType(coinType);
	}

	/**
	 * @return the coinType
	 */
	public ZWCoin getCoinId() {
		return coinType;
	}

	/**
	 * @param coinType the coinType to set
	 */
	public void setCoinType(ZWCoin coinType) {
		this.coinType = coinType;
	}
	
}