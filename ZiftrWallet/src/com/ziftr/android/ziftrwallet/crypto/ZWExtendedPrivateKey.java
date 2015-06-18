package com.ziftr.android.ziftrwallet.crypto;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.spongycastle.math.ec.ECPoint;

import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.util.Base58;
import com.ziftr.android.ziftrwallet.util.CryptoUtils;

public class ZWExtendedPrivateKey extends ZWPrivateKey {

	private static byte[] MASTER_HMAC_KEY = "Bitcoin seed".getBytes();

	public static final byte[] HD_VERSION_MAIN_PRIV = new byte[] {(byte)0x04, (byte)0x88, (byte)0xAD, (byte)0xE4};
	public static final byte[] HD_VERSION_TEST_PRIV = new byte[] {(byte)0x04, (byte)0x35, (byte)0x83, (byte)0x94};

	ZWHdData data;

	/** 
	 * Constructs a master private key from the seed.
	 */
	public ZWExtendedPrivateKey(byte[] seed) {
		if (seed == null || seed.length != 256) {
			throw new ZWHdWalletException("ZiftrWallet uses a seed of length 256!");
		}
		byte[] result = CryptoUtils.Hmac(MASTER_HMAC_KEY, seed);
		this.priv = new BigInteger(1, CryptoUtils.left(result));
		this.data = new ZWHdData(CryptoUtils.right(result));
	}

	/**
	 * Deserialize from an extended private key.
	 */
	public ZWExtendedPrivateKey(String xprv) throws ZWAddressFormatException {
		byte[] decoded = Base58.decodeChecked(xprv);
		byte[] version = Arrays.copyOfRange(decoded, 0, 4);
		CryptoUtils.checkPrivateVersionBytes(version);
		this.data = new ZWHdData(xprv);
	}

	/**
	 * Make an extended key from the private key and the extension data. 
	 */
	public ZWExtendedPrivateKey(BigInteger priv, ZWHdData data) {
		super(priv);
		this.data = data;
	}

	public ZWExtendedPrivateKey deriveChild(ZWHdChildNumber index) throws ZWHdWalletException {
		this.checkUnencrypted();

		byte[] hmacData = new byte[37];
		if (index.isHardened()) {
			byte[] privBytes = this.getPrivKeyBytes();
			if (privBytes == null || privBytes.length != 32)
				throw new ZWHdWalletException("Could not load private key bytes for key derivation");

			hmacData[0] = 0;
			System.arraycopy(privBytes, 0, hmacData, 1, 32);
		} else {
			this.ensureHasPublicKey();
			if (!this.pub.isPubKeyCanonical())
				throw new ZWHdWalletException("Non-canonical public key!");
			if (!this.pub.isCompressed())
				throw new ZWHdWalletException("HD Wallets do not support uncompressed keys!");

			byte[] pkBytes = this.pub.getPubKeyBytes();
			System.arraycopy(pkBytes, 0, hmacData, 0, 33);
		}

		System.arraycopy(index.serialize(), 0, hmacData, 33, 4);
		byte[] result = CryptoUtils.Hmac(this.data.chainCode, hmacData);

		// This addition modulo the curve order is what makes it possible to generate child public keys without
		// knowing the private key (for non-hardened children). Essentially
		//     priv_child = (priv_parent + one_way(pub_parent)) mod CURVE_ORDER
		// This means the points generated will be the same (adding here means ECC addition):
		//     priv_child * G = priv_parent * G + one_way(pub_parent) * G
		// which is the same as
		//     pub_child = pub_parent + one_way(pub_parent) * G
		// TODO In case parse256(I_left) ³ n or ki = 0, the resulting key is invalid, proceed with the next value for i
		BigInteger left = new BigInteger(1, CryptoUtils.left(result));
		CryptoUtils.checkLessThanCurveOrder(left);
		BigInteger newPriv = left.add(this.priv).mod(ZWCurveParameters.CURVE_ORDER);
		CryptoUtils.checkNonZero(newPriv);

		byte[] childChainCode = CryptoUtils.right(result);
		this.ensureHasPublicKey();
		ZWHdFingerPrint parentFingerPrint = new ZWHdFingerPrint(this.pub.getPubKeyHash());
		ZWHdData childData = new ZWHdData((byte)(this.data.depth + 1), parentFingerPrint, index, childChainCode);

		return new ZWExtendedPrivateKey(newPriv, childData);
	}

	public ZWExtendedPrivateKey deriveChild(String path) throws ZWHdWalletException {
		if (path == null || path.isEmpty())
			throw new ZWHdWalletException("Cannot derive null or empty path");
		if (!CryptoUtils.isPrivPath(path))
			throw new ZWHdWalletException("Must be deriving a private key with this method.");

		ZWExtendedPrivateKey p = this;
		List<ZWHdChildNumber> pathInts = CryptoUtils.parsePath(path);
		for (ZWHdChildNumber ci : pathInts) {
			p = p.deriveChild(ci);
		}

		return p;
	}

	public String xprv(boolean testnet) {
		return Base58.encode(this.serialize(testnet));
	}

	public byte[] serialize(boolean testnet) {
		ByteBuffer b = ByteBuffer.allocate(78);
		b.put(testnet ? HD_VERSION_TEST_PRIV : HD_VERSION_MAIN_PRIV);
		b.put(this.data.serialize());
		b.put((byte)0);
		b.put(this.getPrivKeyBytes());
		CryptoUtils.checkHd(b.remaining() == 0);
		return b.array();
	}

	/**
	 * CPU costly operation!
	 */
	@Override
	public ZWPublicKey calculatePublicKey(boolean compressed) {
		if (!compressed) {
			throw new UnsupportedOperationException("HD wallets only support compressd public keys.");
		}
		this.checkUnencrypted();
		ECPoint point = ZWCurveParameters.CURVE.getG().multiply(this.priv);
		return new ZWExtendedPublicKey(point.getEncoded(compressed), this.data);
	}

	private void checkUnencrypted() {
		if (this.isEncrypted()) {
			throw new ZWHdWalletException("Cannot derive children private keys from an encrypted parent!");
		}
	}

}
