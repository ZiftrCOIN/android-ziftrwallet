/** Copyright 2011 Google Inc.
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
/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */
package com.ziftr.android.ziftrwallet.crypto;

import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.util.ZiftrBase58;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

/**
 * <p>An address looks like 1MsScoe2fTJoq4ZPdQgqyhgWeoNamYPevy and is derived from an 
 * elliptic curve public key plus a coinId. </p>
 *
 * <p>A standard address is built by taking RIPEMD160(SHA256( public key bytes )), 
 * with a version prefix and a checksum suffix, then encoding it textually as base58. 
 * The version prefix is used to both denote the network for which the address is 
 * valid (see {@link ZWCoin}), and also to indicate how the bytes inside the address
 * should be interpreted. Whilst almost all addresses today are hashes of public keys, 
 * another (currently not fully supported type) can contain a hash of a script instead.</p>
 */
public class ZWSendingAddress extends ZWAddress {


	/**
	 * <p>Decodes the given address and determines the coin type and version byte.</p>
	 * 
	 * @param address
	 * @throws ZWAddressFormatException
	 */
	public ZWSendingAddress(String address) throws ZWAddressFormatException {
		this(null, address);
	}

	/**
	 * <p>Construct an address from parameters and the standard "human readable" form.</p>
	 *  
	 * @param coinId
	 * @param address
	 * @throws ZWAddressFormatException
	 */
	public ZWSendingAddress(ZWCoin coin, String address) throws ZWAddressFormatException {
		// Checksum is validated in the decoding
		byte[] allData = ZiftrBase58.decodeChecked(address);
		byte[] hash160 = ZiftrUtils.stripVersionAndChecksum(allData, 20);

		this.initialize(coin, allData[0], hash160);
	}

	/**
	 * <p>Construct an address from coinId, the address version, and the 
	 * hash160 form.</p>
	 * 
	 * @param coinId
	 * @param versionByte
	 * @param hash160
	 * @throws ZWAddressFormatException
	 */
	public ZWSendingAddress(ZWCoin coin, byte versionByte, byte[] hash160) throws ZWAddressFormatException {
		this.initialize(coin, versionByte, hash160);
	}

	/** 
	 * <p>Returns an Address that represents the default P2PubKeyHash address.</p> 
	 * 
	 * @throws ZWAddressFormatException 
	 */
	public ZWSendingAddress(ZWCoin coinId, byte[] hash160) throws ZWAddressFormatException {
		this(coinId, coinId.getPubKeyHashPrefix(), hash160);
	}

	@Override
	public boolean isOwnedAddress() {
		
		//since this address is used for sending coins to, the user/app does not own it
		return false;
	}


}
