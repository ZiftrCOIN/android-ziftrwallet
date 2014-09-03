package com.ziftr.android.onewallet.fragment.accounts;


import java.math.BigInteger;

import com.ziftr.android.onewallet.crypto.OWAddress;
import com.ziftr.android.onewallet.crypto.OWSha256Hash;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWFiat;

/**
 * This class is just a data holder for the {@link OWWalletTransactionListAdapter}.
 * 
 * TODO do we need to add any of the functionality from Bitcoinj's TransactionOutput or TransactionInput?
 */
public class OWWalletTransaction {

	// Database stuff
	
	/** 
	 * The id of this transaction in the database.
	 * This will be put in on created. 
	 */
	private long _id = -1;

	/** 
	 * The identifier of this transaciton in the network. 
	 * This should be given after creation, in an update.
	 */
	private OWSha256Hash sha256Hash;

	/** 
	 * The amount that the transaction caused the balance of this wallet to change by. 
	 * If this is a received transaction, then the fee was not paid by 
	 * us, and so does not affect the wallet balance.
	 * This should be be put in when created.
	 */
	private BigInteger txAmount;

	/** 
	 * The fee paid to the miners for this tx. May or may not have been paid by user.
	 * If this is a sending transaction, user paid. If it is a reciving transaction, 
	 * then the sender paid. 
	 * This should be be put in when created.
	 */
	private BigInteger txFee;

	/** 
	 * The title of the transaction. This is given in the send/recieve
	 * section when the user makes/receivers a transaction.
	 * Default create has no note, calls other create with empty string.
	 */
	private String txNote;

	/** 
	 * The time this transaction was broadcasted to the network.
	 * Doesn't need to be included in create method, can be just calculated. 
	 */
	private long txTime;
	
	/** 
	 * The last known number of confirmations for this tx. May be out of date.
	 * Default create has no num confs, calls other create with -1 confs. 
	 * 0 confs means has been seen by netowrk. -1 confs means is local only.  
	 */
	private int numConfirmations;
	
	/** 
	 * The one address of all the output addresses that is most representative 
	 * of this transaction. For sending txs, this will just be the address we 
	 * sent coins to. For receiving txs, this will likely be the first in the list
	 * of outputs that is an address that belongs to us.
	 * Should always be included in all creates.
	 */
	private OWAddress displayAddress;
	
	// Viewing stuff

	/** The currency's title. e.g. "Bitcoin". */
	private OWCoin.Type coinId;

	/** They type of fiat. e.g. Fiat.Type.USD. */
	private OWFiat.Type fiatType;

	public enum Type {
		Transaction,
		Divider
	}

	/** A boolean describing whether or not this transaction is pending. */
	private OWWalletTransaction.Type txType;

	/** The resources id to draw the view for this list item. */
	private int resId;

	public OWWalletTransaction(OWCoin.Type coinId, 
			OWFiat.Type fiatType, 
			String txNote, 
			long txTime, 
			BigInteger txAmount, 
			OWWalletTransaction.Type txType,
			int resId) {
		this.setCoinId(coinId);
		this.setFiatType(fiatType);
		this.setTxNote(txNote);
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
	 * @return the txNote
	 */
	public String getTxNote() {
		return txNote;
	}

	/**
	 * @param txNote the txNote to set
	 */
	public void setTxNote(String txNote) {
		this.txNote = txNote;
	}

	/**
	 * @return the _id
	 */
	public long get_id() {
		return _id;
	}

	/**
	 * @param _id the _id to set
	 */
	public void set_id(long _id) {
		this._id = _id;
	}

	/**
	 * @return the sha256Hash
	 */
	public OWSha256Hash getSha256Hash() {
		return sha256Hash;
	}

	/**
	 * @param sha256Hash the sha256Hash to set
	 */
	public void setSha256Hash(OWSha256Hash sha256Hash) {
		this.sha256Hash = sha256Hash;
	}

	/**
	 * @return the txFee
	 */
	public BigInteger getTxFee() {
		return txFee;
	}

	/**
	 * @param txFee the txFee to set
	 */
	public void setTxFee(BigInteger txFee) {
		this.txFee = txFee;
	}

	/**
	 * @return the txTime
	 */
	public long getTxTime() {
		return txTime;
	}

	/**
	 * @param txTime the txTime to set
	 */
	public void setTxTime(long txTime) {
		this.txTime = txTime;
	}
	
	/**
	 * @return the txAmount
	 */
	public BigInteger getTxAmount() {
		return txAmount;
	}

	/**
	 * @param txAmount the txAmount to set
	 */
	public void setTxAmount(BigInteger txAmount) {
		this.txAmount = txAmount;
	}

	/**
	 * @return the txType
	 */
	public OWWalletTransaction.Type getTxType() {
		return txType;
	}

	/**
	 * @param txType the txType to set
	 */
	public void setTxType(OWWalletTransaction.Type txType) {
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
	 * @return True if this transaction is seen by the network.
	 */
	public Boolean isBroadcasted() {
		return this.numConfirmations > -1;
	}

	/**
	 * @return True if this transaction type is pending
	 */
	public Boolean isPending() {
		// TODO should this be -1 or 0?
		return -1 <= this.numConfirmations && 
				this.numConfirmations < this.coinId.getNumRecommendedConfirmations();
	}
	
	/**
	 * @return the numConfirmations
	 */
	public int getNumConfirmations() {
		return numConfirmations;
	}

	/**
	 * @param numConfirmations the numConfirmations to set
	 */
	public void setNumConfirmations(int numConfirmations) {
		this.numConfirmations = numConfirmations;
	}

	/**
	 * @return the displayAddress
	 */
	public OWAddress getDisplayAddress() {
		return displayAddress;
	}

	/**
	 * @param displayAddress the displayAddress to set
	 */
	public void setDisplayAddress(OWAddress displayAddress) {
		this.displayAddress = displayAddress;
	}

}