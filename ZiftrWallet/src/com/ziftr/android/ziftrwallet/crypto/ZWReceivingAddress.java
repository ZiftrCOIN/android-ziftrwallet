/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */
package com.ziftr.android.ziftrwallet.crypto;

import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.exceptions.ZWDataEncryptionException;
import com.ziftr.android.ziftrwallet.sqlite.ZWReceivingAddressesTable;

public class ZWReceivingAddress extends ZWAddress {

	public enum KeyType {
		/** priv is a {@link ZWPrivateKey} */
		STANDARD_UNENCRYPTED,
		
		/** priv is a {@link ZWExtendedPrivateKey} */
		EXTENDED_UNENCRYPTED,
		
		/** priv is a {@link ZWHdPath}. */
		DERIVABLE_PATH,
		
		/** priv is a {@link ZWEncryptedData} */
		ENCRYPTED
	};
	
	private KeyType keyType;
	
	/** Receiving address can be sent to, and spent from. Spending from uses this private key. */
	private Object priv;
	
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

	public ZWReceivingAddress(ZWCoin coinId) throws ZWAddressFormatException {
		this(coinId, new ZWPrivateKey(), false);
	}

	/**
	 * @throws ZWAddressFormatException
	 */
	public ZWReceivingAddress(ZWCoin coinId, ZWPrivateKey priv, boolean hidden) throws ZWAddressFormatException {
		if (priv == null) {
			throw new ZWAddressFormatException("Cannot use this constructor without a addresses must have a private key");
		}
		this.priv = priv;
		this.pub = priv.getPub();
		this.hidden = hidden;
		this.initialize(coinId, coinId.getPubKeyHashPrefix(), this.pub.getPubKeyHash());
		keyType = KeyType.STANDARD_UNENCRYPTED;
	}
	
	public ZWReceivingAddress(ZWCoin coinId, ZWExtendedPrivateKey priv) throws ZWAddressFormatException {
		// hidden=false doesn't do anything here, the values is ignored in isHidden for this KeyType
		this(coinId, (ZWPrivateKey)priv, false);
		keyType = KeyType.EXTENDED_UNENCRYPTED;
	}
	
	public ZWReceivingAddress(ZWCoin coinId, ZWPublicKey pub, ZWHdPath path) throws ZWAddressFormatException {
		this.priv = path;
		this.pub = pub;
		// Do not need to set hidden, it is derived from the path stored in this.priv
		this.initialize(coinId, coinId.getPubKeyHashPrefix(), this.pub.getPubKeyHash());
		keyType = KeyType.DERIVABLE_PATH;
	}
	
	public ZWReceivingAddress(ZWCoin coinId, ZWPublicKey pub, ZWPrivateData data, boolean hidden) throws ZWAddressFormatException {
		if (data == null) {
			throw new ZWAddressFormatException("Cannot use this constructor without any encrpted data");
		}
		this.priv = data;
		this.pub = pub;
		this.hidden = hidden;
		this.initialize(coinId, coinId.getPubKeyHashPrefix(), pub.getPubKeyHash());
		keyType = KeyType.ENCRYPTED;
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
		return hidden;
	}

	public void setHidden(boolean hidden) {
		switch (this.keyType) {
		case ENCRYPTED:
		case STANDARD_UNENCRYPTED:
			this.hidden = hidden;
			break;
		case DERIVABLE_PATH:
		case EXTENDED_UNENCRYPTED:
			throw new ZWHdWalletException("Cannot change whether a HD derived key is hidden.");
		}
	}

	@Override
	public boolean isOwnedAddress() {
		return true;
	}
	
	/**
	 * @return the keyType
	 */
	public KeyType getKeyType() {
		return keyType;
	}
	
	public ZWPrivateData getEncryptedKey() {
		return (ZWPrivateData) this.priv;
	}
	
	public ZWExtendedPrivateKey getExtendedPriv() {
		return (ZWExtendedPrivateKey) this.priv;
	}
	
	public ZWHdPath getPath() {
		return (ZWHdPath) this.priv;
	}
	
	public ZWPrivateKey getStandardKey() {
		return (ZWPrivateKey) this.priv;
	}
	
	public void decrypt(String password) throws ZWDataEncryptionException {
		switch (this.keyType) {
		case STANDARD_UNENCRYPTED:
			if (password != null && password.length() > 0) {
				throw new ZWKeyCrypterException("Already decrypted.");
			}
			break;
		case DERIVABLE_PATH:
		case EXTENDED_UNENCRYPTED:
			throw new ZWHdWalletException("Should not encrypt extended keys directly, only encrypt the seed.");
		case ENCRYPTED:
			this.priv = this.getEncryptedKey().decrypt(password).getDataString();
			this.keyType = KeyType.STANDARD_UNENCRYPTED;
			break;
		}
	}
	
	public void deriveFromStoredPath(ZWExtendedPrivateKey masterPrivKey) {
		switch (this.keyType) {
		case DERIVABLE_PATH:
			ZWExtendedPrivateKey xprvkey = (ZWExtendedPrivateKey) masterPrivKey.deriveChild(this.getPath().toPrivatePath());
			this.priv = xprvkey;
			break;
		case STANDARD_UNENCRYPTED:
		case EXTENDED_UNENCRYPTED:
		case ENCRYPTED:
			throw new ZWHdWalletException("Should only call this method on addresses that store the derivable path");
		}
		
		this.keyType = KeyType.EXTENDED_UNENCRYPTED;
	}
	
}
