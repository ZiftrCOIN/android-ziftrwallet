package com.ziftr.android.onewallet.fragment.accounts;

import com.ziftr.android.onewallet.util.OWCoin;

/** 
 * Each one of these views has a an image with a text view under it,
 * along with a '+' icon in the top right.
 * The image is the logo of the coin.
 * The text view should contain the title of the coin.
 */
public class OWNewCurrencyListItem {
	/** The resource id of the image to load. */
	private int coinLogoResId;
	
	/** The name of the coin to put under the logo. */
	private OWCoin.Type coinType;
	
	/**
	 * Make a new {@link OWNewCurrencyListItem} and set the appropriate
	 * fields. 
	 * 
	 * @param coinLogoResId
	 * @param coinType
	 */
	public OWNewCurrencyListItem(int coinLogoResId, OWCoin.Type coinType) {
		this.setCoinLogoResId(coinLogoResId);
		this.setCoinType(coinType);
	}

	/**
	 * @return the coinType
	 */
	public OWCoin.Type getCoinId() {
		return coinType;
	}

	/**
	 * @param coinType the coinType to set
	 */
	public void setCoinType(OWCoin.Type coinType) {
		this.coinType = coinType;
	}

	/**
	 * @return the coinLogoResId
	 */
	public int getCoinLogoResId() {
		return coinLogoResId;
	}

	/**
	 * @param coinLogoResId the coinLogoResId to set
	 */
	public void setCoinLogoResId(int coinLogoResId) {
		this.coinLogoResId = coinLogoResId;
	}

}