package com.ziftr.android.onewallet.fragment.accounts;


import java.math.BigDecimal;

import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWFiat;

/**
 * This class is just a data holder for the {@link OWWalletTransactionListAdapter}.
 */
public class OWWalletTransactionListItem {
	
	protected enum Type {
		PendingTransaction,
		ConfirmedTransaction,
		PendingDivider,
		HistoryDivider
	}

	/** The currency's title. e.g. "Bitcoin". */
	private OWCoin.Type coinId;

	/** They type of fiat. e.g. Fiat.Type.USD. */
	private OWFiat.Type fiatType;

	/** 
	 * The title of the transaction. This is given in the send/recieve
	 * section when the user makes/receivers a transaction.
	 */
	private String txTitle;

	/** The time that the transaction took place. */
	private String txTime;

	/** The amount that the transaction causes the wallet balance to change by. */
	private BigDecimal txAmount;

	/** A boolean describing whether or not this transaction is pending. */
	private OWWalletTransactionListItem.Type txType;

	/** The resources id to draw the view for this list item. */
	private int resId;

	public OWWalletTransactionListItem(OWCoin.Type coinId, 
			OWFiat.Type fiatType, 
			String txTitle, 
			String txTime, 
			BigDecimal txAmount, 
			OWWalletTransactionListItem.Type txType,
			int resId) {
		this.setCoinId(coinId);
		this.setFiatType(fiatType);
		this.setTxTitle(txTitle);
		this.setTxTime(txTime);
		this.setTxAmount(txAmount);
		this.setTxType(txType);
		this.setResId(resId);
	}

	/**
	 * @return the coinId
	 */
	public OWCoin.Type getCoinId() {
		return coinId;
	}

	/**
	 * @param coinId the coinId to set
	 */
	public void setCoinId(OWCoin.Type coinId) {
		this.coinId = coinId;
	}

	/**
	 * @return the fiatType
	 */
	public OWFiat.Type getFiatType() {
		return fiatType;
	}

	/**
	 * @param fiatType the fiatType to set
	 */
	public void setFiatType(OWFiat.Type fiatType) {
		this.fiatType = fiatType;
	}

	/**
	 * @return the txTitle
	 */
	public String getTxTitle() {
		return txTitle;
	}

	/**
	 * @param txTitle the txTitle to set
	 */
	public void setTxTitle(String txTitle) {
		this.txTitle = txTitle;
	}

	/**
	 * @return the txTime
	 */
	public String getTxTime() {
		return txTime;
	}

	/**
	 * @param txTime the txTime to set
	 */
	public void setTxTime(String txTime) {
		this.txTime = txTime;
	}

	/**
	 * @return the txAmount
	 */
	public BigDecimal getTxAmount() {
		return txAmount;
	}

	/**
	 * @param txAmount the txAmount to set
	 */
	public void setTxAmount(BigDecimal txAmount) {
		this.txAmount = txAmount;
	}

	/**
	 * @return the txType
	 */
	public OWWalletTransactionListItem.Type getTxType() {
		return txType;
	}

	/**
	 * @param txType the txType to set
	 */
	public void setTxType(OWWalletTransactionListItem.Type txType) {
		this.txType = txType;
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
	/**
	 * 
	 * @return True if this transaction type is pending
	 */
	public Boolean isPending(){
		return this.txType.name() == "PendingTransaction";
	}

}