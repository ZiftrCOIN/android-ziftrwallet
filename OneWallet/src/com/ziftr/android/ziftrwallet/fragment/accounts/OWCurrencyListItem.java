package com.ziftr.android.ziftrwallet.fragment.accounts;


import com.ziftr.android.ziftrwallet.util.OWCoin;
import com.ziftr.android.ziftrwallet.util.OWFiat;

public class OWCurrencyListItem {

	/** The currency's title. e.g. "Bitcoin". */
	private OWCoin coinId;

	/** They type of fiat. e.g. Fiat.Type.USD. */
	private OWFiat fiatType;

	/** The price of one unit, encoded as a string in the fiatType units. e.g. 620.18. */
	private String unitFiatMarketValue;

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
	public OWCurrencyListItem(OWCoin coinId, OWFiat fiatType, 
			String unitFiatMarketValue, String walletTotal, 
			String walletTotalFiatEquiv, int resId) {
		this.setCoinId(coinId);
		this.setFiatType(fiatType);
		this.setUnitFiatMarketValue(unitFiatMarketValue);
		this.setWalletTotal(walletTotal);
		this.setWalletTotalFiatEquiv(walletTotalFiatEquiv);
		this.setResId(resId);
	}


	/**
	 * @return the coinId
	 */
	public OWCoin getCoinId() {
		return coinId;
	}


	/**
	 * @param currencyId the coinId to set
	 */
	public void setCoinId(OWCoin coinId) {
		this.coinId = coinId;
	}


	/**
	 * @return the fiatType
	 */
	public OWFiat getFiatType() {
		return fiatType;
	}


	/**
	 * @param fiatType the fiatType to set
	 */
	public void setFiatType(OWFiat fiatType) {
		this.fiatType = fiatType;
	}


	/**
	 * @return the unitFiatMarketValue
	 */
	public String getUnitFiatMarketValue() {
		return unitFiatMarketValue;
	}


	/**
	 * @param unitFiatMarketValue the unitFiatMarketValue to set
	 */
	public void setUnitFiatMarketValue(String unitFiatMarketValue) {
		this.unitFiatMarketValue = unitFiatMarketValue;
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