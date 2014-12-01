package com.ziftr.android.ziftrwallet.fragment;


import com.ziftr.android.ziftrwallet.crypto.ZWCoin;

public class ZWCurrencyListItem {

	/** The currency's title. e.g. "Bitcoin". */
	private ZWCoin coinId;

	/** The total amount of currency contained in the wallet (in coins, not fiatType). */
	private String walletTotal;

	/** The equivalent of walletTotal in the fiatType currency units. */
	private String walletTotalFiatEquiv;
	
	/** The resources id to draw the view for this list item. */
	private int resId;

	/**
	 * Constructs a currency list itme from the given values. 
	 * 
	 * @param currencyTitle
	 * @param fiatType
	 * @param unitFiatMarketValue
	 * @param walletTotal
	 * @param walletTotalFiatEquiv
	 * @param resId
	 */
	public ZWCurrencyListItem(ZWCoin coinId, 
			String unitFiatMarketValue, String walletTotal, 
			String walletTotalFiatEquiv, int resId) {
		this.setCoinId(coinId);
		this.setWalletTotal(walletTotal);
		this.setWalletTotalFiatEquiv(walletTotalFiatEquiv);
		this.setResId(resId);
	}


	/**
	 * @return the coinId
	 */
	public ZWCoin getCoinId() {
		return coinId;
	}


	/**
	 * @param currencyId the coinId to set
	 */
	public void setCoinId(ZWCoin coinId) {
		this.coinId = coinId;
	}

	/**
	 * @return the walletTotal
	 */
	public String getWalletTotal() {
		return walletTotal;
	}


	/**
	 * @param walletTotal the walletTotal to set
	 */
	public void setWalletTotal(String walletTotal) {
		this.walletTotal = walletTotal;
	}


	/**
	 * @return the walletTotalFiatEquiv
	 */
	public String getWalletTotalFiatEquiv() {
		return walletTotalFiatEquiv;
	}


	/**
	 * @param walletTotalFiatEquiv the walletTotalFiatEquiv to set
	 */
	public void setWalletTotalFiatEquiv(String walletTotalFiatEquiv) {
		this.walletTotalFiatEquiv = walletTotalFiatEquiv;
	}
	
	/**
	 * @return the resId
	 */
	public int getResId() {
		return resId;
	}


	/**
	 * @param resId the resId to set
	 */
	public void setResId(int resId) {
		this.resId = resId;
	}

}