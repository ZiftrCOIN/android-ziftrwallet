package com.ziftr.android.ziftrwallet.crypto;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.spongycastle.math.ec.ECPoint;

import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.util.ZWCryptoUtils;
import com.ziftr.android.ziftrwallet.util.ZiftrBase58;

public class ZWExtendedPrivateKey extends ZWPrivateKey {

	private static byte[] MASTER_HMAC_KEY = "Bitcoin seed".getBytes();

	public static final byte[] HD_VERSION_MAIN_PRIV = new byte[] {(byte)0x04, (byte)0x88, (byte)0xAD, (byte)0xE4};
	public static final byte[] HD_VERSION_TEST_PRIV = new byte[] {(byte)0x04, (byte)0x35, (byte)0x83, (byte)0x94};

	protected ZWHdPath path;
	protected ZWHdData data;

	/** 
	 * Constructs a master private key from the seed.
	 */
	public ZWExtendedPrivateKey(byte[] seed) {
		super(false);
		if (seed == null) {
			throw new ZWHdWalletException("Null seed is not valid. ");
		}
		byte[] result = ZWCryptoUtils.Hmac(MASTER_HMAC_KEY, seed);
		this.priv = new BigInteger(1, ZWCryptoUtils.left(result));
		this.path = new ZWHdPath("m");
		this.data = new ZWHdData(ZWCryptoUtils.right(result));
	}
	
	public ZWExtendedPrivateKey(String path, String xprv) throws ZWAddressFormatException {
		this(new ZWHdPath(path), xprv);
	}

	/**
	 * Deserialize from an extended private key.
	 */
	protected ZWExtendedPrivateKey(ZWHdPath path, String xprv) throws ZWAddressFormatException {
		super(false);
		byte[] decoded = ZiftrBase58.decodeChecked(xprv);
		byte[] version = Arrays.copyOfRange(decoded, 0, 4);
		ZWCryptoUtils.checkPrivateVersionBytes(version);
		this.data = new ZWHdData(xprv);
		if (path == null) {
			throw new ZWHdWalletException("Keys should be combined with their relative path.");
		}
		this.path = path;
		this.priv = new BigInteger(1, Arrays.copyOfRange(decoded, 46, 78));
	}
	
	public ZWExtendedPrivateKey(String path, BigInteger priv, ZWHdData data) {
		this(new ZWHdPath(path), priv, data);
	}

	/**
	 * Make an extended key from the private key and the extension data. 
	 */
	protected ZWExtendedPrivateKey(ZWHdPath path, BigInteger priv, ZWHdData data) {
		super(priv);
		if (path == null) {
			throw new ZWHdWalletException("Keys should be combined with their relative path.");
		}
		this.path = path;
		this.data = data;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////

	public ZWExtendedPrivateKey deriveChild(ZWHdChildNumber index) throws ZWHdWalletException {
		byte[] hmacData = new byte[37];
		if (index.isHardened()) {
			byte[] privBytes = this.getPrivKeyBytes();
			if (privBytes == null || privBytes.length != 32)
				throw new ZWHdWalletException("Could not load private key bytes for key derivation");

			hmacData[0] = 0;
			System.arraycopy(privBytes, 0, hmacData, 1, 32);
		} else {
			if (!this.getPub().isPubKeyCanonical())
				throw new ZWHdWalletException("Non-canonical public key!");
			if (!this.getPub().isCompressed())
				throw new ZWHdWalletException("HD Wallets do not support uncompressed keys!");

			byte[] pkBytes = this.getPub().getPubKeyBytes();
			System.arraycopy(pkBytes, 0, hmacData, 0, 33);
		}

		System.arraycopy(index.serialize(), 0, hmacData, 33, 4);
		byte[] result = ZWCryptoUtils.Hmac(this.data.chainCode, hmacData);

		// This addition modulo the curve order is what makes it possible to generate child public keys without
		// knowing the private key (for non-hardened children). Essentially
		//     priv_child = (priv_parent + one_way(pub_parent)) mod CURVE_ORDER
		// This means the points generated will be the same (adding here means ECC addition):
		//     priv_child * G = priv_parent * G + one_way(pub_parent) * G
		// which is the same as
		//     pub_child = pub_parent + one_way(pub_parent) * G
		// TODO In case parse256(I_left) � n or ki = 0, the resulting key is invalid, proceed with the next value
		BigInteger left = new BigInteger(1, ZWCryptoUtils.left(result));
		ZWCryptoUtils.checkLessThanCurveOrder(left);
		BigInteger newPriv = left.add(this.priv).mod(ZWCurveParameters.CURVE_ORDER);
		ZWCryptoUtils.checkNonZero(newPriv);

		byte[] childChainCode = ZWCryptoUtils.right(result);
		ZWHdFingerPrint parentFingerPrint = new ZWHdFingerPrint(this.getPub().getPubKeyHash());
		ZWHdData childData = new ZWHdData((byte)(this.data.depth + 1), parentFingerPrint, index, childChainCode);

		return new ZWExtendedPrivateKey(this.path.slash(index), newPriv, childData);
	}
	
	public ZWExtendedPrivateKey deriveChild(String path) throws ZWHdWalletException {
		return this.deriveChild(new ZWHdPath(path));
	}

	public ZWExtendedPrivateKey deriveChild(ZWHdPath path) throws ZWHdWalletException {
		if (path == null)
			throw new ZWHdWalletException("Cannot derive null path");
		if (!path.derivedFromPrivateKey())
			throw new ZWHdWalletException("Cannot derive this path, it is relative to a public key");
		
		ZWExtendedPrivateKey p = this;
		for (ZWHdChildNumber ci : path.toPrivatePath().getRelativeToPrv()) {
			p = p.deriveChild(ci);
		}
		
		return p;
	}
	
	
	
	public String xprv(boolean mainnet) {
		return ZiftrBase58.encodeChecked(this.serialize(mainnet));
	}

	
	/**
	 * Gets the serialized version of this extended private key.
	 * Note: returned bytes do NOT include checksum (for converting to base58)
	 * @param mainnet
	 * @return
	 */
	public byte[] serialize(boolean mainnet) {
		ByteBuffer b = ByteBuffer.allocate(78);
		b.put(mainnet ? HD_VERSION_MAIN_PRIV : HD_VERSION_TEST_PRIV); //version bytes
		b.put(this.data.serialize()); //data contains (in order), depth, fingerprint, child number, chain code
		b.put((byte)0);
		b.put(this.getPrivKeyBytes());
		ZWCryptoUtils.checkHd(b.remaining() == 0); 
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
		ECPoint point = ZWCurveParameters.CURVE.getG().multiply(this.priv);
		return new ZWExtendedPublicKey(this.path.toPublicPath(true), point.getEncoded(compressed), this.data);
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
