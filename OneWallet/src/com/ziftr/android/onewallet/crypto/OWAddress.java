/**
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ziftr.android.onewallet.crypto;

import java.util.Locale;

import javax.annotation.Nullable;

import android.annotation.SuppressLint;

import com.ziftr.android.onewallet.exceptions.OWAddressFormatException;
import com.ziftr.android.onewallet.exceptions.OWWrongNetworkException;
import com.ziftr.android.onewallet.fragment.accounts.OWSearchableListItem;
import com.ziftr.android.onewallet.util.Base58;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWUtils;
import com.ziftr.android.onewallet.util.ZLog;

/**
 * <p>An address looks like 1MsScoe2fTJoq4ZPdQgqyhgWeoNamYPevy and is derived from an 
 * elliptic curve public key plus a coinId. </p>
 *
 * <p>A standard address is built by taking RIPEMD160(SHA256( public key bytes )), 
 * with a version prefix and a checksum suffix, then encoding it textually as base58. 
 * The version prefix is used to both denote the network for which the address is 
 * valid (see {@link OWCoin}), and also to indicate how the bytes inside the address
 * should be interpreted. Whilst almost all addresses today are hashes of public keys, 
 * another (currently not fully supported type) can contain a hash of a script instead.</p>
 */
public class OWAddress implements OWSearchableListItem {

	///////////////////////////////////////
	//////////  Database Fields  //////////
	///////////////////////////////////////

	/**
	 * The id for storage in an sqlite table. 
	 */
	private long id = -1;

	/**
	 * The note that the user has applied to this given address. 
	 */
	private String note;

	/**
	 * The last known balance for this address. May not be up to date.
	 * 
	 * TODO do we really need this for all addresses or can this go in the ECKey class?
	 */
	private long lastKnownBalance;
	
	/**
	 * The most recent time that this address had a transaction made using it. May not be up to date.
	 */
	private long lastTimeModifiedSeconds;


	//////////////////////////////////////////////
	//////////  Address Content Fields  //////////
	//////////////////////////////////////////////

	/**
	 * An address is a RIPEMD160 hash of a public key, therefore is always 
	 * 160 bits, or 20 bytes.
	 */
	public static final int LENGTH = 20;

	/** The type of coin that this is an address for. */
	private OWCoin coinId;

	/** The version byte specifies what type of address it is (P2SH, P2PubKey, etc). */
	private byte versionByte;

	/** 
	 * Addresses are ripemd(sha256( pub_key )) encoded in base 58. This is the holder
	 * for the result of that computation. 
	 */
	private byte[] hash160;

	/** The ECKey for this address, if known. May be null. */
	private OWECKey key;
	
	/**
	 * <p>Uses a new ECKey to make an Address.</p> 
	 * 
	 * @param coinId
	 * @param key
	 * @throws OWAddressFormatException
	 */
	public OWAddress(OWCoin coinId) {
		try {
			this.key = new OWECKey();
			this.initialize(coinId, coinId.getPubKeyHashPrefix(), key.getPubKeyHash());
		} catch(OWAddressFormatException afe) {
			ZLog.log("Error making new address, this should not have happened.");
		}
	}

	/**
	 * <p>Uses an ECKey to make an Address. The ECKey need not necessarilty have access
	 * to a private key as this class can be used for both sending (non-owned) and 
	 * receiving (owned) addresses.</p> 
	 * 
	 * @param coinId
	 * @param key
	 * @throws OWAddressFormatException
	 */
	public OWAddress(OWCoin coinId, OWECKey key) throws OWAddressFormatException {
		this.initialize(coinId, coinId.getPubKeyHashPrefix(), key.getPubKeyHash());
		this.key = key;
	}

	/**
	 * <p>Decodes the given address and determines the coin type and version byte.</p>
	 * 
	 * @param address
	 * @throws OWAddressFormatException
	 */
	public OWAddress(String address) throws OWAddressFormatException {
		this(null, address);
	}

	/**
	 * <p>Construct an address from parameters and the standard "human readable" form.</p>
	 *  
	 * @param coinId
	 * @param address
	 * @throws OWAddressFormatException
	 */
	public OWAddress(OWCoin coinId, String address) throws OWAddressFormatException {
		// Checksum is validated in the decoding
		byte[] allData = Base58.decodeChecked(address);
		byte[] hash160 = OWUtils.stripVersionAndChecksum(allData);

		this.initialize(coinId, allData[0], hash160);
	}

	/**
	 * <p>Construct an address from coinId, the address version, and the 
	 * hash160 form.</p>
	 * 
	 * @param coinId
	 * @param versionByte
	 * @param hash160
	 * @throws OWAddressFormatException
	 */
	public OWAddress(OWCoin coinId, byte versionByte, byte[] hash160) throws OWAddressFormatException {
		this.initialize(coinId, versionByte, hash160);
	}

	/** 
	 * <p>Returns an Address that represents the default P2PubKeyHash address.</p> 
	 * 
	 * @throws OWAddressFormatException 
	 */
	public OWAddress(OWCoin coinId, byte[] hash160) throws OWAddressFormatException {
		this(coinId, coinId.getPubKeyHashPrefix(), hash160);
	}

	/**
	 * A private helper method for initialization that prevents code duplication. 
	 * 
	 * @param coinId
	 * @param versionByte
	 * @param hash160
	 */
	private void initialize(OWCoin coinId, byte versionByte, byte[] hash160) throws OWAddressFormatException {
		if (hash160.length != 20) {
			throw new OWAddressFormatException("Addresses are 160-bit hashes, so you must provide 20 bytes");
		}

		if (coinId != null) {
			if (!isAcceptableVersion(coinId, versionByte)) {
				throw new OWWrongNetworkException(versionByte, coinId.getAcceptableAddressCodes());
			}
		} else {
			// If null then we need to infer what the coinType is 
			coinId = getCoinTypeFromVersionByte(versionByte);
		}

		if (coinId == null) {
			throw new OWAddressFormatException("Version byte does not match any supported coin types");
		}

		this.coinId = coinId;
		this.versionByte = versionByte;
		this.hash160 = hash160;
		
		// Default, should be overridden with the setter method
		lastTimeModifiedSeconds = System.currentTimeMillis() / 1000;
	}

	public OWCoin getCoinId() {
		return this.coinId;
	}

	/**
	 * @return the versionByte
	 */
	public byte getVersionByte() {
		return versionByte;
	}

	/**
	 * @return the key
	 */
	public OWECKey getKey() {
		return key;
	}

	/** The (big endian) 20 byte hash that is the core of an address. */
	public byte[] getHash160() {
		return hash160;
	}

	/**
	 * Returns true if this address is a Pay-To-Script-Hash (P2SH) address.
	 * See also https://github.com/bitcoin/bips/blob/master/bip-0013.mediawiki: 
	 * Address Format for pay-to-script-hash.
	 */
	public boolean isP2SHAddress() {
		return coinId != null && this.versionByte == this.coinId.getScriptHashPrefix();
	}

	public boolean isPersonalAddress() {
		return key != null && key.getPrivKeyBytes() != null;
	}

	@Override
	public String toString() {
		return Base58.encode(this.versionByte, this.hash160);
	}

	///////////////////////////////////////////
	//////////  Getters and Setters  //////////
	///////////////////////////////////////////

	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * @return the note
	 */
	public String getNote() {
		return note;
	}

	/**
	 * @param note the note to set
	 */
	public void setNote(String note) {
		this.note = note;
	}

	/**
	 * @return the lastKnownBalance
	 */
	public long getLastKnownBalance() {
		return lastKnownBalance;
	}

	/**
	 * @param lastKnownBalance the lastKnownBalance to set
	 */
	public void setLastKnownBalance(long lastKnownBalance) {
		this.lastKnownBalance = lastKnownBalance;
	}
	
	/**
	 * @return the lastTimeModifiedSeconds
	 */
	public long getLastTimeModifiedSeconds() {
		return lastTimeModifiedSeconds;
	}

	/**
	 * @param lastTimeModifiedSeconds the lastTimeModifiedSeconds to set
	 */
	public void setLastTimeModifiedSeconds(long lastTimeModifiedSeconds) {
		this.lastTimeModifiedSeconds = lastTimeModifiedSeconds;
	}
	
	public static boolean isAcceptableVersion(OWCoin coinId, byte b) {
		return coinId.getPubKeyHashPrefix() == b || coinId.getScriptHashPrefix() == b;
	}

	/**
	 * Given an address, examines the version byte and attempts to find a matching NetworkParameters. If you aren't sure
	 * which network the address is intended for (eg, it was provided by a user), you can use this to decide if it is
	 * compatible with the current wallet.
	 * @return a NetworkParameters or null if the string wasn't of a known version.
	 */
	@Nullable
	public static OWCoin getCoinTypeFromAddress(String address) throws OWAddressFormatException {
		return getCoinTypeFromDecodedAddress(Base58.decodeChecked(address));
	}

	@Nullable
	private static OWCoin getCoinTypeFromDecodedAddress(byte[] allData) {
		return getCoinTypeFromVersionByte(allData[0]);
	}

	@Nullable
	private static OWCoin getCoinTypeFromVersionByte(byte version) {
		for (OWCoin type : OWCoin.values()) {
			if (isAcceptableVersion(type, version)) {
				return type;
			}
		}
		return null;
	}

	@SuppressLint("DefaultLocale")
	@Override
	public boolean matches(CharSequence constraint, OWSearchableListItem nextItem) {
		return this.toString().toLowerCase(Locale.ENGLISH).contains(constraint.toString().toLowerCase());
	}

}
