/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */


package com.ziftr.android.ziftrwallet.crypto;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.util.ZiftrBase58;
import com.ziftr.android.ziftrwallet.util.ZWCryptoUtils;

/** 
 * A class that holds the associated HD wallet tree information
 * for a particular ZWExtendedPublicKey or ZWExtendedPrivateKey.
 *  
 * A particular ZWExtendedPublicKey and ZWExtendedPrivateKey may hold references
 * to the same ZWHdData.
 */
public class ZWHdData {

	/**
	 * The depth in the HD key tree. 0 for master.
	 */
	public byte depth;

	/**
	 * The fingerprint of a key is the first 4 bytes of the HASH160
	 * hash of the serialized ECDSA public key. Do not Base58 encode the
	 * full result of the HASH160, as it may appear to be an address.
	 */
	public ZWHdFingerPrint parentFingerPrint;

	/** 
	 * The index in the tree.
	 */
	private ZWHdChildNumber index;

	/** 
	 * 32 bytes for the chain code.
	 */
	public final byte[] chainCode;

	/**
	 * Make a new Master extended key.
	 */
	public ZWHdData(byte[] chainCode) {
		this.depth = 0;
		this.parentFingerPrint = new ZWHdFingerPrint();
		this.index = new ZWHdChildNumber(0, false);
		this.chainCode = Arrays.copyOf(chainCode, chainCode.length); 
	}
	
	public ZWHdData(String xp) throws ZWAddressFormatException {
		byte[] decoded = ZiftrBase58.decodeChecked(xp);
		if (decoded[4] < 0) throw new ZWAddressFormatException("Depth may not be less than zero.");
		this.depth = decoded[4];
		this.parentFingerPrint = new ZWHdFingerPrint(Arrays.copyOfRange(decoded, 5, 9));
		this.index = new ZWHdChildNumber(Arrays.copyOfRange(decoded, 9, 13));
		this.chainCode = Arrays.copyOfRange(decoded, 13, 45);
	}

	/**
	 * A new child extended key.
	 */
	public ZWHdData(byte depth, ZWHdFingerPrint parentFingerPrint, ZWHdChildNumber index, byte[] chainCode) {
		this.depth = depth;
		this.parentFingerPrint = parentFingerPrint;
		this.index = index;
		this.chainCode = Arrays.copyOf(chainCode, chainCode.length); 
	}

	protected byte[] serialize() {
		ByteBuffer b = ByteBuffer.allocate(41);
		b.put(this.depth);
		b.put(this.parentFingerPrint.serialize());
		b.put(this.index.serialize());
		b.put(this.chainCode);
		ZWCryptoUtils.checkHd(b.remaining() == 0);
		return b.array();
	}
	
}

