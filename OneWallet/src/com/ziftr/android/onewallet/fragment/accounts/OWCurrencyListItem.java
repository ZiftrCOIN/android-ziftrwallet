package com.ziftr.android.onewallet.fragment.accounts;


import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWFiat;
import com.ziftr.android.onewallet.util.OWFiat.Type;

public class OWCurrencyListItem {

	/** The currency's title. e.g. "Bitcoin". */
	private OWCoin.Type coinId;

	/** They type of fiat. e.g. Fiat.Type.USD. */
	private Type fiatType;

	/** The price of one unit, encoded as a string in the fiatType units. e.g. 620.18. */
	private String unitFiatMarketValue;

	/** The total amount of currency contained in the wallet (in coins, not fiatType). */
	private String walletTotal;

	/** The equivalent of walletTotal in the fiatType currency units. */
	private String walletTotalFiatEquiv;

	/** The resources id of the coin logo. e.g. drawable/icon_bitcoin_logo. */
	private int coinLogoId;

	/**
	 * Constructs a currency list itme from the given values. 
	 * 
	 * @param currencyTitle
	 * @param fiatType
	 * @param unitFiatMarketValue
	 * @param walletTotal
	 * @param walletTotalFiatEquiv
	 */
	public OWCurrencyListItem(int coinLogoId, OWCoin.Type coinId, 
			OWFiat.Type fiatType, String unitFiatMarketValue, 
			String walletTotal, String walletTotalFiatEquiv) {
		this.setCoinLogoId(coinLogoId);
		this.setCoinId(coinId);
		this.setFiatType(fiatType);
		this.setUnitFiatMarketValue(unitFiatMarketValue);
		this.setWalletTotal(walletTotal);
		this.setWalletTotalFiatEquiv(walletTotalFiatEquiv);
	}


	/**
	 * @return the coinLogoId
	 */
	public int getCoinLogoId() {
		return coinLogoId;
	}


	/**
	 * @param coinLogoId the coinLogoId to set
	 */
	public void setCoinLogoId(int coinLogoId) {
		this.coinLogoId = coinLogoId;
	}


	/**
	 * @return the coinId
	 */
	public OWCoin.Type getCoinId() {
		return coinId;
	}


	/**
	 * @param currencyId the coinId to set
	 */
	public void setCoinId(OWCoin.Type coinId) {
		this.coinId = coinId;
	}


	/**
	 * @return the fiatType
	 */
	public Type getFiatType() {
		return fiatType;
	}


	/**
	 * @param fiatType the fiatType to set
	 */
	public void setFiatType(OWFiat.Type fiatType) {
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

}