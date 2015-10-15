/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */
package com.ziftr.android.ziftrwallet.crypto;

import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.exceptions.ZWDataEncryptionException;

public abstract class ZWReceivingAddress extends ZWAddress {

	
	/** Receiving address can be sent to, and spent from. Spending from uses this private key. */
	//private Object priv;
	
	protected ZWPrivateData privateKeyData;
	
	/**
	 * Creation time of the key in seconds since the epoch, or zero if the key was deserialized from a 
	 * version that did not have this field.
	 */
	private long creationTimeSeconds;

	/** whether the address has been spent from */
	private boolean spentFrom = false;

	/** 
	 * whether the address is hidden to the user
	 * This has to be kept as a separate field to be backwards compatible
	 * with keys not derived from HD wallets. 
	 */
	private boolean hidden = false;
	
	private ZWPublicKey publicKey;

	
	/**
	 * 
	 * @param coin
	 * @param publicKey
	 * @param privateKeyData
	 * @param change
	 * @param hdAccount
	 * @param hdIndex
	 * @throws ZWAddressFormatException
	 */
	protected ZWReceivingAddress(ZWCoin coin, ZWPublicKey publicKey, boolean change) throws ZWAddressFormatException {
		this.publicKey = publicKey;
		this.hidden = change; //most other wallets call this change, so we're starting to move over to that (change addresses are hidden from the user)
		this.initialize(coin, coin.getPubKeyHashPrefix(), publicKey.getPubKeyHash());
	}
	
	

	/**
	 * Returns the creation time of this key or zero if the key was deserialized from a version 
	 * that did not store that data.
	 */
	public long getCreationTimeSeconds() {
		return creationTimeSeconds;
	}

	/**
	 * Sets the creation time of this key. Zero is a convention to mean "unavailable". This 
	 * method can be useful when you have a raw key you are importing from somewhere else.
	 */
	public void setCreationTimeSeconds(long newCreationTimeSeconds) {
		if (newCreationTimeSeconds < 0)
			throw new IllegalArgumentException("Cannot set creation time to negative value: " + newCreationTimeSeconds);
		creationTimeSeconds = newCreationTimeSeconds;
	}

	public boolean isSpentFrom(){
		return this.spentFrom;
	}

	public void setSpentFrom(boolean spentFrom){
		this.spentFrom = spentFrom;
	}

	public boolean isHidden() {
		
		/***
		ZWHdPath path = null;
		if (this.keyType == KeyType.EXTENDED_UNENCRYPTED) {
			path = this.getExtendedPriv().getPath();
		} else if (this.keyType == KeyType.DERIVABLE_PATH) {
			path = this.getPath();
		}
		
		if (path != null) {
			Integer childToDetermineHidden = path.getBip44Change();
			if (childToDetermineHidden == ZWReceivingAddressesTable.HIDDEN_FROM_USER) {
				this.hidden = true;
			} else if (childToDetermineHidden == ZWReceivingAddressesTable.VISIBLE_TO_USER) {
				this.hidden = false;
			}
		}
		***/
		
		return this.hidden;
	}

	

	@Override
	public boolean isOwnedAddress() {
		return true;
	}
	
	
	public ZWPrivateData getPrivateKeyData() {
		return this.privateKeyData;
	}
	
	/**
	 * @return the key
	 */
	public ZWPublicKey getPublicKey() {
		return publicKey;
	}

	public abstract ZWPrivateKey getPrivateKey(String password) throws ZWDataEncryptionException;
	
}
