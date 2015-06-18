/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.spongycastle.crypto.digests.RIPEMD160Digest;
import org.spongycastle.crypto.digests.SHA512Digest;
import org.spongycastle.crypto.macs.HMac;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.math.ec.ECPoint;

import android.text.TextUtils;

import com.ziftr.android.ziftrwallet.ZWPreferences;
import com.ziftr.android.ziftrwallet.crypto.ZWCurveParameters;
import com.ziftr.android.ziftrwallet.crypto.ZWExtendedPrivateKey;
import com.ziftr.android.ziftrwallet.crypto.ZWExtendedPublicKey;
import com.ziftr.android.ziftrwallet.crypto.ZWHdChildNumber;
import com.ziftr.android.ziftrwallet.crypto.ZWHdWalletException;
import com.ziftr.android.ziftrwallet.crypto.ZWKeyCrypter;

public class CryptoUtils {

	public static final String HD_PATH_DELIMITER = "/";

	public static final Long HARDENED_START = (long) 0x7fffffff;
	
	private static final MessageDigest digest;
	static {
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			// Can't happen, we should know about it if it does happen.
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the Sha256 hash of the specified byte array.
	 * The returned array will have 32 elements.
	 * 
	 * @param arr - The array of information to hash.
	 * @return - the Sha256 hash of the specified byte array.
	 */
	public static byte[] Sha256Hash(byte[] arr) {
		synchronized (digest) {
			return digest.digest(arr);
		}
	}

	public static byte[] Hmac(byte[] key, byte[] input) {
		SHA512Digest digest = new SHA512Digest();
		HMac hMac = new HMac(digest);
		hMac.init(new KeyParameter(key));

		hMac.reset();
		hMac.update(input, 0, input.length);
		byte[] out = new byte[64];
		hMac.doFinal(out, 0);

		return out;
	}

	public static byte[] left(byte[] a) {
		return Arrays.copyOfRange(a, 0, 256);
	}

	public static byte[] right(byte[] a) {
		return Arrays.copyOfRange(a, 256, 512);
	}

	/**
	 * Calculates RIPEMD160(SHA256(input)). This is used in Address calculations.
	 * TODO change name order, as it is confusing
	 */
	public static byte[] sha256hash160(byte[] input) {
		try {
			byte[] sha256 = MessageDigest.getInstance("SHA-256").digest(input);
			RIPEMD160Digest digest = new RIPEMD160Digest();
			digest.update(sha256, 0, sha256.length);
			byte[] out = new byte[20];
			digest.doFinal(out, 0);
			return out;
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);  // Cannot happen.
		}
	}


	public static byte[] saltedHash(String password) {
		return saltedHash(ZWPreferences.getSalt(), password);
	}

	public static byte[] saltedHash(String salt, String password) {
		if (password == null) {
			return null;
		}

		byte[] newPasswordBytes = password.getBytes();
		byte[] concat = new byte[32 + password.getBytes().length];
		System.arraycopy(CryptoUtils.Sha256Hash(salt.getBytes()), 0, concat, 0, 32);
		System.arraycopy(newPasswordBytes, 0, concat, 32, newPasswordBytes.length);
		return CryptoUtils.Sha256Hash(concat);
	}

	public static String saltedHashString(String newPassword) {
		if(newPassword == null || newPassword.isEmpty()) {
			return null;
		}
		return ZiftrUtils.bytesToHexString(saltedHash(newPassword));
	}

	/**
	 * See Utils#doubleDigest(byte[], int, int).
	 */
	public static byte[] doubleDigest(byte[] input) {
		return doubleDigest(input, 0, input.length);
	}

	/**
	 * Calculates the SHA-256 hash of the given byte range, and then hashes the resulting hash again. This is
	 * standard procedure in Bitcoin. The resulting hash is in big endian form.
	 */
	public static byte[] doubleDigest(byte[] input, int offset, int length) {
		synchronized (digest) {
			digest.reset();
			digest.update(input, offset, length);
			byte[] first = digest.digest();
			return digest.digest(first);
		}
	}

	public static byte[] singleDigest(byte[] input, int offset, int length) {
		synchronized (digest) {
			digest.reset();
			digest.update(input, offset, length);
			return digest.digest();
		}
	}

	public static boolean isPrivPath(String s) {
		return s != null && s.startsWith("m" + HD_PATH_DELIMITER);
	}

	public static boolean isPubPath(String s) {
		return s != null && s.startsWith("M" + HD_PATH_DELIMITER);
	}

	public static void checkLessThanCurveOrder(BigInteger val) {
		if (val.compareTo(ZWCurveParameters.CURVE_ORDER) >= 0) {
			throw new ZWHdWalletException("Miraculous coincience, very high random number, this should never happen! " + 
					ZiftrUtils.bigIntegerToString(val, 32));
		}
	}

	public static void checkNonZero(BigInteger val) {
		if (val.compareTo(BigInteger.ZERO) == 0) {
			throw new ZWHdWalletException("Miraculous coincience, private keys cancel each other mod n, this should never happen!");
		}
	}
	
	public static void checkNotPointAtInfinity(ECPoint point) {
		if (point.isInfinity()) {
			throw new ZWHdWalletException("Miraculous coincience, ECPoints added to zero, this should never happen!");
		}
	}
	
	public static void checkPrivateVersionBytes(byte[] version) {
		if (!Arrays.equals(version, ZWExtendedPrivateKey.HD_VERSION_MAIN_PRIV) && !Arrays.equals(version, ZWExtendedPrivateKey.HD_VERSION_TEST_PRIV)) {
			throw new ZWHdWalletException("Version bytes do not match for a private key!");
		}
	}
	
	public static void checkPublicVersionBytes(byte[] version) {
		if (!Arrays.equals(version, ZWExtendedPublicKey.HD_VERSION_MAIN_PUB) && !Arrays.equals(version, ZWExtendedPublicKey.HD_VERSION_TEST_PUB)) {
			throw new ZWHdWalletException("Version bytes do not match for a public key!");
		}
	}
	
	public static void checkHd(boolean b) {
		if (!b) {
			throw new ZWHdWalletException("");
		}
	}

	public static String walletPath(List<ZWHdChildNumber> path, boolean publicKeyPath) {
		List<String> list = new ArrayList<String>();
		for (ZWHdChildNumber i : path) {
			list.add(i.toString());
		}
		String s = (publicKeyPath ? "M" : "m") + HD_PATH_DELIMITER;
		return s + TextUtils.join(HD_PATH_DELIMITER, list);
	}

	public static List<ZWHdChildNumber> parsePath(String path) throws ZWHdWalletException {
		if (!isPrivPath(path) && !isPubPath(path)) {
			throw new ZWHdWalletException("The path does not specify if it is deriving a "
					+ "private/public key by starting with M" + HD_PATH_DELIMITER + " or m" + HD_PATH_DELIMITER);
		}

		if (isPrivPath(path)) {
			path = path.replaceFirst("m" + HD_PATH_DELIMITER, "");
		} else {
			path = path.replaceFirst("M" + HD_PATH_DELIMITER, "");
		}

		List<ZWHdChildNumber> list = new ArrayList<ZWHdChildNumber>();
		String[] elements = path.split(HD_PATH_DELIMITER);
		for (String s : elements) {
			list.add(new ZWHdChildNumber(s));
		}

		return list;
	}

	/**
	 * Get the fingerprint from the hash160 of the public key
	 */
	public static byte[] getFingerPrint(byte[] h) {
		return new byte[] { h[0], h[1], h[2], h[3] };
	}

	public static boolean isEncryptedIdentifier(char id) {
		return id == ZWKeyCrypter.PBE_AES_ENCRYPTION;
	}
	
}