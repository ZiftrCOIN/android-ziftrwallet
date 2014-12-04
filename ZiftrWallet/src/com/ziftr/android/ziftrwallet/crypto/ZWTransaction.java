package com.ziftr.android.ziftrwallet.crypto;


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;

import com.ziftr.android.ziftrwallet.fragment.ZWSearchableListItem;
import com.ziftr.android.ziftrwallet.fragment.ZWWalletTransactionListAdapter;

/**
 * This class is just a data holder for the {@link ZWWalletTransactionListAdapter}.
 * 
 * TODO do we need to add any of the functionality from Bitcoinj's TransactionOutput or TransactionInput?
 * 
 * TODO delete the id field, just use hash
 */
public class ZWTransaction implements ZWSearchableListItem {

	// Database stuff

	
	/** 
	 * The identifier of this transaciton in the network. 
	 * This should be given after creation, in an update.
	 */
	private String sha256Hash;

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
	private long numConfirmations;
	
	/** 
	 * The list of addresses that were used in this transaction. 
	 * Should always be included in all creates.
	 */
	private List<String> displayAddresses;
	
	// Viewing stuff

	/** Which coin this transaction is for */
	private ZWCoin coin;

	// TODO figure out what really needs to be in this constructor.
	/**
	public ZWTransaction(ZWCoin coin, String txNote, long txTime, BigInteger txAmount) {
		this.coin = coin;
		this.txNote = txNote;
		this.txTime = txTime;
		this.txAmount = txAmount;
	}
	***/
	
	public ZWTransaction(ZWCoin coin, String sha256Hash) {
		this.coin = coin;
		this.sha256Hash = sha256Hash; 
		
	}

	/**
	 * @return the coinId
	 */
	public ZWCoin getCoin() {
		return coin;
	}

	/**
	 * @param coinId the coinId to set
	 */
	public void setCoin(ZWCoin coin) {
		this.coin = coin;
	}


	/**
	 * @return the txNote
	 */
	public String getNote() {
		if(txNote == null) {
			return "";
		}
		return txNote;
	}

	/**
	 * @param txNote the txNote to set
	 */
	public void setNote(String txNote) {
		this.txNote = txNote;
	}


	/**
	 * @return the sha256Hash
	 */
	public String getSha256Hash() {
		return sha256Hash;
	}

	/**
	 * @param sha256Hash the sha256Hash to set
	 */
	public void setSha256Hash(String sha256Hash) {
		this.sha256Hash = sha256Hash;
	}

	/**
	 * @return the txFee
	 */
	public BigInteger getFee() {
		return txFee;
	}

	/**
	 * @param txFee the txFee to set
	 */
	public void setFee(BigInteger txFee) {
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
	public BigInteger getAmount() {
		return this.txAmount;
	}

	/**
	 * @param txAmount the txAmount to set
	 */
	public void setAmount(BigInteger txAmount) {
		this.txAmount = txAmount;
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
		if(this.numConfirmations < this.coin.getNumRecommendedConfirmations()) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * @return the numConfirmations
	 */
	public long getConfirmationCount() {
		return numConfirmations;
	}

	/**
	 * @param numConfirmations the numConfirmations to set
	 */
	public void setConfirmationCount(long numConfirmations) {
		this.numConfirmations = numConfirmations;
	}

	/**
	 * @return the displayAddress
	 */
	public List<String> getDisplayAddresses() {
		if(displayAddresses == null) {
			return new ArrayList<String>();
		}
		return displayAddresses;
	}

	/**
	 * @param displayAddresses the displayAddress to set
	 */
	public void setDisplayAddresses(List<String> displayAddresses) {
		this.displayAddresses = displayAddresses;
	}
	
	public void addDisplayAddress(String address) {
		if (this.displayAddresses == null) {
			this.displayAddresses = new ArrayList<String>();
		}
		this.displayAddresses.add(address);
	}
	
	public String getAddressAsCommaListString() {
		if (this.displayAddresses == null || this.displayAddresses.size() == 0) {
			return "";
		} else {
			StringBuilder sb = new StringBuilder();
			//sb.append(","); why did we want a comma in the front? it gets interpreted as an empty address later
			for (String address : this.getDisplayAddresses()) {
				sb.append(address).append(",");
			}
			return sb.toString();
		}
	}

	
	@SuppressLint("DefaultLocale")
	@Override
	public boolean matches(CharSequence constraint, ZWSearchableListItem nextItem) {
		// TODO This isn't quite right because the next item in this list doesn't necessarily 
		// also meet the search criteria...
		
		/**
		ZWTransaction owNextTransaction = (ZWTransaction) nextItem;
		if (owNextTransaction != null && owNextTransaction.isDivider()) {
			// The pending bar shouldn't show if it the only bar
			return false;
		} 
		**/
		//else if (this.isDivider()) {
		//	return true;
		//} 
		//else {
			return this.getNote().toLowerCase(Locale.ENGLISH).contains(constraint.toString().toLowerCase());
		//}
	}
	
}