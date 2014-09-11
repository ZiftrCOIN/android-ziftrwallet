package com.ziftr.android.onewallet.fragment.accounts;

import com.ziftr.android.onewallet.util.OWCoin;

/** 
 * Each one of these views has a an image with a text view under it,
 * along with a '+' icon in the top right.
 * The text view should contain the title of the coin.
 * 
 * This doesn't contain a lot of functionality right now, but could
 * be helpful if we need to ad more to each one of these later. 
 */
public class OWNewCurrencyListItem {
	
	/** The name of the coin to put under the logo. */
	private OWCoin coinType;
	
	/**
	 * Make a new {@link OWNewCurrencyListItem} and set the appropriate
	 * fields. 
	 * 
	 * @param coinLogoResId
	 * @param coinType
	 */
	public OWNewCurrencyListItem(OWCoin coinType) {
		this.setCoinType(coinType);
	}

	/**
	 * @return the coinType
	 */
	public OWCoin getCoinId() {
		return coinType;
	}

	/**
	 * @param coinType the coinType to set
	 */
	public void setCoinType(OWCoin coinType) {
		this.coinType = coinType;
	}

}