package com.ziftr.android.ziftrwallet.crypto;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.spongycastle.math.ec.ECPoint;

import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.util.ZiftrBase58;
import com.ziftr.android.ziftrwallet.util.ZWCryptoUtils;

/**
 * Public key paths look like this:
 * 
 *   M/1/23
 *   or
 *   [m/44'/0'/0']/1/23
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

	protected ZWHdPath path;
	protected ZWHdData data;

	/**
	 * Deserialize from an extended public key.
	 */
	public ZWExtendedPublicKey(String path, String xpub) throws ZWAddressFormatException {
		this(new ZWHdPath(path), xpub);
	}
	
	protected ZWExtendedPublicKey(ZWHdPath path, String xpub) throws ZWAddressFormatException {
		if (path == null) {
			throw new ZWHdWalletException("Keys should be combined with their relative path.");
		}
		this.path = path;
		byte[] decoded = ZiftrBase58.decodeChecked(xpub);
		byte[] version = Arrays.copyOfRange(decoded, 0, 4);
		ZWCryptoUtils.checkPublicVersionBytes(version);
		this.data = new ZWHdData(xpub);
		this.pub = Arrays.copyOfRange(decoded, 45, 78);
	}

	/**
	 * Make an extended key from the public key and the extension data. 
	 */
	public ZWExtendedPublicKey(String path, byte[] encodedPubKey, ZWHdData data) {
		this(new ZWHdPath(path), encodedPubKey, data);
	}
	
	protected ZWExtendedPublicKey(ZWHdPath path, byte[] encodedPubKey, ZWHdData data) {
		super(encodedPubKey);
		if (path == null) {
			throw new ZWHdWalletException("Keys should be combined with their relative path.");
		}
		this.path = path;
		this.data = data;
		
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////

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
		byte[] result = ZWCryptoUtils.Hmac(this.data.chainCode, hmacData);

		BigInteger left = new BigInteger(1, ZWCryptoUtils.left(result));
		ZWCryptoUtils.checkLessThanCurveOrder(left);
		ECPoint leftMultiple = ZWCurveParameters.CURVE.getG().multiply(left);
		ECPoint newPub = leftMultiple.add(this.getPoint());
		ZWCryptoUtils.checkNotPointAtInfinity(newPub);

		byte[] childChainCode = ZWCryptoUtils.right(result);
		ZWHdFingerPrint parentFingerPrint = new ZWHdFingerPrint(this.getPubKeyHash());
		ZWHdData childData = new ZWHdData((byte)(this.data.depth + 1), parentFingerPrint, index, childChainCode);

		return new ZWExtendedPublicKey(this.path.slash(index), newPub.getEncoded(true), childData);
	}

	public ZWExtendedPublicKey deriveChild(String path) throws ZWHdWalletException {
		return this.deriveChild(new ZWHdPath(path));
	}
	
	protected ZWExtendedPublicKey deriveChild(ZWHdPath path) throws ZWHdWalletException {
		if (path == null)
			throw new ZWHdWalletException("Cannot derive null path");
		if (!path.resolvesToPublicKey())
			throw new ZWHdWalletException("Must be deriving a public key with this method.");
		if (path.derivedFromPrivateKey())
			throw new ZWHdWalletException("Cannot derive this path, it relies on having the private key");

		ZWExtendedPublicKey p = this;
		for (ZWHdChildNumber ci : path.getRelativeToPub()) {
			p = p.deriveChild(ci);
		}

		return p;
	}

	public String xpub(boolean mainnet) {
		return ZiftrBase58.encodeChecked(this.serialize(mainnet));
	}

	public byte[] serialize(boolean mainnet) {
		if (!this.isCompressed())
			throw new UnsupportedOperationException("HD wallets only support compressd public keys.");
		ByteBuffer b = ByteBuffer.allocate(78);
		b.put(mainnet ? HD_VERSION_MAIN_PUB: HD_VERSION_TEST_PUB);
		b.put(this.data.serialize());
		b.put(this.getPubKeyBytes());
		ZWCryptoUtils.checkHd(b.remaining() == 0);
		return b.array();
	}
	
	/**
	 * @return the path
	 */
	public ZWHdPath getPath() {
		return path;
	}

	/**
	 * @return the data
	 */
	public ZWHdData getData() {
		return data;
	}
	
}
