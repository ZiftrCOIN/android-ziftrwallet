/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */
package com.ziftr.android.ziftrwallet.crypto;

import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;

public class ZWReceivingAddress extends ZWSendingAddress {

	/** Receiving address can be sent to, and spent from. Spending from uses this private key. */
	private ZWPrivateKey priv;

	/**
	 * Creation time of the key in seconds since the epoch, or zero if the key was deserialized from a 
	 * version that did not have this field.
	 */
	private long creationTimeSeconds;

	/** whether the address has been spent from */
	private boolean spentFrom = false;

	/** whether the address is hidden to the user */
	private boolean hidden = false;

	public ZWReceivingAddress(ZWCoin coinId) throws ZWAddressFormatException {
		this(coinId, new ZWPrivateKey());
	}

	/**
	 * @throws ZWAddressFormatException
	 */
	public ZWReceivingAddress(ZWCoin coinId, ZWPrivateKey priv) throws ZWAddressFormatException {
		if (priv == null) {
			throw new ZWAddressFormatException("Receiving addresses must have a private key");
		}
		this.priv = priv;
		this.pub = this.priv.getPub();
		this.initialize(coinId, coinId.getPubKeyHashPrefix(), this.priv.getPub().getPubKeyHash());
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
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public boolean isPersonalAddress() {
		return true;
	}
	
	public void decrypt(ZWKeyCrypter crypter) {
		priv.setKeyCrypter(crypter);
		priv = priv.decrypt();
	}

	/**
	 * @return the priv
	 */
	public ZWPrivateKey getPriv() {
		return priv;
	}

}
