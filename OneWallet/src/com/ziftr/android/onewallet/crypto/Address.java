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

import javax.annotation.Nullable;

import com.ziftr.android.onewallet.util.Base58;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWCoin.Type;
import com.ziftr.android.onewallet.util.OWCoinRelative;
import com.ziftr.android.onewallet.util.OWUtils;

/**
 * <p>An address looks like 1MsScoe2fTJoq4ZPdQgqyhgWeoNamYPevy and is derived from an 
 * elliptic curve public key plus a coinId. </p>
 *
 * <p>A standard address is built by taking the RIPE-MD160 hash of the public key bytes, 
 * with a version prefix and a checksum suffix, then encoding it textually as base58. 
 * The version prefix is used to both denote the network for which the address is 
 * valid (see {@link OWCoin.Type}), and also to indicate how the bytes inside the address
 * should be interpreted. Whilst almost all addresses today are hashes of public keys, 
 * another (currently not fully supported type) can contain a hash of a script instead.</p>
 */
public class Address implements OWCoinRelative {
	/**
	 * An address is a RIPEMD160 hash of a public key, therefore is always 160 bits or 20 bytes.
	 */
	public static final int LENGTH = 20;

	private OWCoin.Type coinId;

	private byte versionByte;

	private byte[] hash160;

	/**
	 * Construct an address from coinId, the address version, and the 
	 * hash160 form. <p>
	 * @throws AddressFormatException 
	 */
	public Address(OWCoin.Type coinId, byte versionByte, byte[] hash160) throws AddressFormatException {
		if (coinId == null) {

		}

		if (hash160.length != 20) {
			throw new AddressFormatException("Addresses are 160-bit hashes, so you must provide 20 bytes");
		}

		this.coinId = coinId;
		this.versionByte = versionByte;
		this.hash160 = hash160;
	}

	/** 
	 * Returns an Address that represents the default P2PubKeyHash address. 
	 * @throws AddressFormatException 
	 */
	public Address(OWCoin.Type coinId, byte[] hash160) throws AddressFormatException {
		this(coinId, coinId.getPubKeyHashPrefix(), hash160);
	}

	//	/** Returns an Address that represents the script hash extracted from the given scriptPubKey */
	//	public static Address fromP2SHScript(NetworkParameters params, Script scriptPubKey) {
	//		checkArgument(scriptPubKey.isPayToScriptHash(), "Not a P2SH script");
	//		return fromP2SHHash(params, scriptPubKey.getPubKeyHash());
	//	}

	/**
	 * Construct an address from parameters and the standard "human readable" form. Example:<p>
	 *
	 * <pre>new Address(NetworkParameters.prodNet(), "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL");</pre><p>
	 *
	 * @param params The expected NetworkParameters or null if you don't want validation.
	 * @param address The textual form of the address, such as "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL"
	 * @throws AddressFormatException if the given address doesn't parse or the checksum is invalid
	 * @throws WrongNetworkException if the given address is valid but for a different chain (eg testnet vs prodnet)
	 */
	public Address(OWCoin.Type coinId, String address) throws AddressFormatException {
		// Checksum is validated in the decoding
		byte[] allData = Base58.decodeChecked(address);
		byte[] hash160 = OWUtils.stripVersionAndChecksum(allData);
		if (coinId != null) {
			if (!this.isAcceptableVersion(coinId, allData[0])) {
				throw new WrongNetworkException(allData[0], coinId.getAcceptableAddressCodes());
			}
		} else {
			// We need to infer what the coinType is 
			for (OWCoin.Type type : OWCoin.Type.values()) {
				if (this.isAcceptableVersion(type, allData[0])) {
					coinId = type;
					break;
				}
			}
		}

		this.coinId = coinId; 
		this.versionByte = allData[0];
		this.hash160 = hash160;
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
		return coinId != null && this.versionByte == coinId.getScriptHashPrefix();
	}

	/**
	 * Given an address, examines the version byte and attempts to find a matching NetworkParameters. If you aren't sure
	 * which network the address is intended for (eg, it was provided by a user), you can use this to decide if it is
	 * compatible with the current wallet.
	 * @return a NetworkParameters or null if the string wasn't of a known version.
	 */
	@Nullable
	public static OWCoin.Type getCoinTypeFromAddress(String address) throws AddressFormatException {
		try {
			return new Address(null, address).getCoinId();
		} catch (WrongNetworkException e) {
			throw new RuntimeException(e);  // Cannot happen.
		}
	}

	private boolean isAcceptableVersion(OWCoin.Type coinId, byte b) {
		return coinId.getPubKeyHashPrefix() == b || coinId.getScriptHashPrefix() == b;
	}

	@Override
	public Type getCoinId() {
		return this.getCoinId();
	}

}
