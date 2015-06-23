package com.ziftr.android.ziftrwallet.crypto;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.spongycastle.math.ec.ECPoint;

import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.util.Base58;
import com.ziftr.android.ziftrwallet.util.CryptoUtils;

/**
 * Paths look like this:
 * 
 *   M/44'/0'/0'/1/23
 *   or
 *   m/44'/0'/0'/1/23
 * 
 * where 
 *   m denotes the master private key (actualy just the 'this' key for derivation relative to this key)
 *   m denotes the child private key (actualy just the 'this' key for derivation relative to this key)
 *   ' dentoes a hardened child key
 *   
 * Reference: 
 *   https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki
 *   https://github.com/bitcoin/bips/blob/master/bip-0044.mediawiki
 */
public class ZWExtendedPublicKey extends ZWPublicKey {

	public static final byte[] HD_VERSION_MAIN_PUB  = new byte[] {(byte)0x04, (byte)0x88, (byte)0xB2, (byte)0x1E};
	public static final byte[] HD_VERSION_TEST_PUB  = new byte[] {(byte)0x04, (byte)0x35, (byte)0x87, (byte)0xCF};

	ZWHdData data;

	/**
	 * Deserialize from an extended public key.
	 */
	public ZWExtendedPublicKey(String xpub) throws ZWAddressFormatException {
		byte[] decoded = Base58.decodeChecked(xpub);
		byte[] version = Arrays.copyOfRange(decoded, 0, 4);
		CryptoUtils.checkPublicVersionBytes(version);
		this.data = new ZWHdData(xpub);
		this.pub = Arrays.copyOfRange(decoded, 45, 78);
	}
	
	/**
	 * Make an extended key from the public key and the extension data. 
	 */
	public ZWExtendedPublicKey(byte[] encodedPubKey, ZWHdData data) {
		super(encodedPubKey);
		this.data = data;
	}

	public ZWExtendedPublicKey deriveChild(ZWHdChildNumber index) throws ZWHdWalletException {
		byte[] hmacData = new byte[37];
		if (index.isHardened()) {
			throw new ZWHdWalletException("Public keys cannot derive hardened child keys!");
		} else {
			if (!this.isPubKeyCanonical()) 
				throw new ZWHdWalletException("Non-canonical public key!");
			if (!this.isCompressed())
				throw new ZWHdWalletException("HD Wallets do not support uncompressed keys!");
			byte[] pkBytes = this.getPubKeyBytes();
			System.arraycopy(pkBytes, 0, hmacData, 0, 33);
		}

		System.arraycopy(index.serialize(), 0, hmacData, 33, 4);
		byte[] result = CryptoUtils.Hmac(this.data.chainCode, hmacData);

		BigInteger left = new BigInteger(1, CryptoUtils.left(result));
		CryptoUtils.checkLessThanCurveOrder(left);
		ECPoint leftMultiple = ZWCurveParameters.CURVE.getG().multiply(left);
		ECPoint newPub = leftMultiple.add(this.getPoint());
		CryptoUtils.checkNotPointAtInfinity(newPub);

		byte[] childChainCode = CryptoUtils.right(result);
		ZWHdFingerPrint parentFingerPrint = new ZWHdFingerPrint(this.getPubKeyHash());
		ZWHdData childData = new ZWHdData((byte)(this.data.depth + 1), parentFingerPrint, index, childChainCode);

		return new ZWExtendedPublicKey(newPub.getEncoded(true), childData);
	}

	public ZWExtendedPublicKey deriveChild(String path) throws ZWHdWalletException {
		if (path == null || path.isEmpty())
			throw new ZWHdWalletException("Cannot derive null or empty path");
		if (!CryptoUtils.isPubPath(path))
			throw new ZWHdWalletException("Must be deriving a public key with this method.");

		ZWExtendedPublicKey p = this;
		List<ZWHdChildNumber> pathInts = CryptoUtils.parsePath(path);
		for (ZWHdChildNumber ci : pathInts) {
			p = p.deriveChild(ci);
		}

		return p;
	}

	public String xpub(boolean testnet) {
		return Base58.encodeChecked(this.serialize(testnet));
	}

	public byte[] serialize(boolean testnet) {
		if (!this.isCompressed())
			throw new UnsupportedOperationException("HD wallets only support compressd public keys.");
		ByteBuffer b = ByteBuffer.allocate(78);
		b.put(testnet ? HD_VERSION_TEST_PUB : HD_VERSION_MAIN_PUB);
		b.put(this.data.serialize());
		b.put(this.getPubKeyBytes());
		CryptoUtils.checkHd(b.remaining() == 0);
		return b.array();
	}

}
