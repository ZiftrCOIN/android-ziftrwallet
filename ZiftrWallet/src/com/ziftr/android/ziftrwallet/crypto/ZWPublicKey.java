/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */


package com.ziftr.android.ziftrwallet.crypto;

import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Arrays;

import javax.annotation.Nullable;

import org.spongycastle.asn1.x9.X9IntegerConverter;
import org.spongycastle.crypto.params.ECPublicKeyParameters;
import org.spongycastle.crypto.signers.ECDSASigner;
import org.spongycastle.math.ec.ECAlgorithms;
import org.spongycastle.math.ec.ECCurve;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.util.encoders.Base64;

import com.google.common.base.Preconditions;
import com.ziftr.android.ziftrwallet.util.ZWCryptoUtils;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;


// TODO: This class is quite a mess by now. Once users are migrated away from Java serialization for the wallets,
// refactor this to have better internal layout and a more consistent API.

/**
 * <p>Represents an elliptic curve public point, usable for digital signatures but not encryption.
 * You can check signatures but not create them.</p>
 *
 * <p>Also provides access to *coin-Qt compatible text message signing, as accessible via the UI or JSON-RPC.
 * This is slightly different to signing raw bytes - if you want to sign your own data and it won't be exposed as
 * text to people, you don't want to use this. If in doubt, ask on the bitcoin-dev mailing list.</p>
 *
 * <p>The ECDSA algorithm supports <i>key recovery</i> in which a signature plus a couple of discriminator bits can
 * be reversed to find the public key used to calculate it. This can be convenient when you have a message and a
 * signature and want to find out who signed it, rather than requiring the user to provide the expected identity.</p>
 */
public class ZWPublicKey {

	/**
	 * The public key for this ECKey.
	 */
	protected byte[] pub;

	// Transient because it's calculated on demand.
	transient private byte[] pubKeyHash;
	
	protected ZWPublicKey() { 
		// This is convenient for subclass constructors, which must make sure the public
		// key bytes are calculated.  
	}
	
	public ZWPublicKey(byte[] encodedPubKey) {
		if (encodedPubKey != null && encodedPubKey.length == 33 || encodedPubKey.length == 65) {
			pub = Arrays.copyOf(encodedPubKey, encodedPubKey.length);
		} else {
			throw new ZWInvalidPublicKeyException("Invalid bytes.");
		}
	}

	/**
	 * Returns whether this key is using the compressed form or not. Compressed pubkeys are only 33 bytes, not 64.
	 */
	public boolean isCompressed() {
		return pub.length == 33;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ZWPublicKey otherKey = (ZWPublicKey) o;
		return Arrays.equals(pub, otherKey.pub);
	}

	@Override
	public int hashCode() {
		// Public keys are random already so we can just use a part of them as the hashcode. Read from the middle to
		// avoid picking up the type code (compressed vs uncompressed).
		return (pub[4] & 0xFF) | ((pub[5] & 0xFF) << 8) | ((pub[6] & 0xFF) << 16) | ((pub[7] & 0xFF) << 24);
	}

	/** Gets the hash160 form of the public key (as seen in addresses). */
	public byte[] getPubKeyHash() {
		if (pubKeyHash == null)
			pubKeyHash = ZWCryptoUtils.sha256hash160(this.pub);
		return pubKeyHash;
	}

	/**
	 * Gets the raw public key value. This appears in transaction scriptSigs. Note that this is <b>not</b> the same
	 * as the pubKeyHash/address.
	 */
	public byte[] getPubKeyBytes() {
		return pub;
	}
	
	/**
	 * <p>Verifies the given ECDSA signature against the message bytes using the public key bytes.</p>
	 * 
	 * <p>When using native ECDSA verification, data must be 32 bytes, and no element may be
	 * larger than 520 bytes.</p>
	 *
	 * @param data      Hash of the data to verify.
	 * @param signature ASN.1 encoded signature.
	 * @param pub       The public key bytes to use.
	 */
	public static boolean verify(byte[] data, ZWECDSASignature signature, byte[] pub) {

		ECDSASigner signer = new ECDSASigner();
		ECPublicKeyParameters params = new ECPublicKeyParameters(ZWCurveParameters.CURVE.getCurve().decodePoint(pub), ZWCurveParameters.CURVE);
		signer.init(false, params);
		try {
			return signer.verifySignature(data, signature.r, signature.s);
		} catch (NullPointerException e) {
			// Bouncy Castle contains a bug that can cause NPEs given specially crafted signatures. Those signatures
			// are inherently invalid/attack sigs so we just fail them here rather than crash the thread.
			ZLog.log("Caught NPE inside bouncy castle");
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Verifies the given ASN.1 encoded ECDSA signature against a hash using the public key.
	 *
	 * @param data      Hash of the data to verify.
	 * @param signature ASN.1 encoded signature.
	 * @param pub       The public key bytes to use.
	 */
	public static boolean verify(byte[] data, byte[] signature, byte[] pub) {
		ZWECDSASignature decoded = ZWECDSASignature.decodeFromDER(signature);
		if (decoded != null){
			return verify(data, decoded, pub);
		} else {
			return false;
		}
	}

	/**
	 * Verifies the given ASN.1 encoded ECDSA signature against a hash using the public key.
	 *
	 * @param data      Hash of the data to verify.
	 * @param signature ASN.1 encoded signature.
	 */
	public boolean verify(byte[] data, byte[] signature) {
		return verify(data, signature, getPubKeyBytes());
	}

	/**
	 * Verifies the given R/S pair (signature) against a hash using the public key.
	 */
	public boolean verify(ZWSha256Hash sigHash, ZWECDSASignature signature) {
		return verify(sigHash.getBytes(), signature, getPubKeyBytes());
	}

	/**
	 * Returns true if this pubkey is canonical, i.e. the correct length taking into account compression.
	 */
	public boolean isPubKeyCanonical() {
		if (this.pub.length < 33)
			return false;
		if (this.pub[0] == 0x04) {
			// Uncompressed pubkey
			if (this.pub.length != 65)
				return false;
		} else if (this.pub[0] == 0x02 || this.pub[0] == 0x03) {
			// Compressed pubkey
			if (this.pub.length != 33)
				return false;
		} else {
			return false;
		}
		return true;
	}

	/**
	 * Given an arbitrary piece of text and a Bitcoin-format message signature encoded in base64, returns an ECKey
	 * containing the public key that was used to sign it. This can then be compared to the expected public key to
	 * determine if the signature was correct. These sorts of signatures are compatible with the Bitcoin-Qt/bitcoind
	 * format generated by signmessage/verifymessage RPCs and GUI menu options. They are intended for humans to verify
	 * their communications with each other, hence the base64 format and the fact that the input is text.
	 *
	 * @param message Some piece of human readable text.
	 * @param signatureBase64 The Bitcoin-format message signature in base64
	 * @throws SignatureException If the public key could not be recovered or if there was a signature format error.
	 */
	public static ZWPublicKey signedMessageToKey(byte[] messageBytes, String signatureBase64) throws SignatureException {
		byte[] signatureEncoded;
		try {
			signatureEncoded = Base64.decode(signatureBase64);
		} catch (RuntimeException e) {
			// This is what you get back from Bouncy Castle if base64 doesn't decode :(
			throw new SignatureException("Could not decode base64", e);
		}
		// Parse the signature bytes into r/s and the selector value.
		if (signatureEncoded.length < 65)
			throw new SignatureException("Signature truncated, expected 65 bytes and got " + signatureEncoded.length);
		int header = signatureEncoded[0] & 0xFF;
		// The header byte: 0x1B = first key with even y, 0x1C = first key with odd y,
		//                  0x1D = second key with even y, 0x1E = second key with odd y
		if (header < 27 || header > 34)
			throw new SignatureException("Header byte out of range: " + header);
		BigInteger r = new BigInteger(1, Arrays.copyOfRange(signatureEncoded, 1, 33));
		BigInteger s = new BigInteger(1, Arrays.copyOfRange(signatureEncoded, 33, 65));
		ZWECDSASignature sig = new ZWECDSASignature(r, s);
		// Note that the C++ code doesn't actually seem to specify any character encoding. Presumably it's whatever
		// JSON-SPIRIT hands back. Assume UTF-8 for now.
		ZWSha256Hash messageHash = ZWSha256Hash.createDouble(messageBytes);
		boolean compressed = false;
		if (header >= 31) {
			compressed = true;
			header -= 4;
		}
		int recId = header - 27;
		ZWPublicKey key = ZWPublicKey.recoverFromSignature(recId, sig, messageHash, compressed);
		if (key == null)
			throw new SignatureException("Could not recover public key from signature");
		return key;
	}

	/**
	 * Convenience wrapper around {@link ZWPublicKey#signedMessageToKey(String, String)}. If the key derived from the
	 * signature is not the same as this one, throws a SignatureException.
	 * 
	 * ZiftrUtils.formatMessageForSigning(coinId, message)
	 */
	public void verifyMessage(byte[] messageBytes, String signatureBase64) throws SignatureException {
		ZWPublicKey key = ZWPublicKey.signedMessageToKey(messageBytes, signatureBase64);
		if (!Arrays.equals(key.getPubKeyBytes(), pub))
			throw new SignatureException("Signature did not match for message");
	}

	/**
	 * <p>Given the components of a signature and a selector value, recover and return the public key
	 * that generated the signature according to the algorithm in SEC1v2 section 4.1.6.</p>
	 *
	 * <p>The recId is an index from 0 to 3 which indicates which of the 4 possible keys is the correct one. Because
	 * the key recovery operation yields multiple potential keys, the correct key must either be stored alongside the
	 * signature, or you must be willing to try each recId in turn until you find one that outputs the key you are
	 * expecting.</p>
	 *
	 * <p>If this method returns null it means recovery was not possible and recId should be iterated.</p>
	 *
	 * <p>Given the above two points, a correct usage of this method is inside a for loop from 0 to 3, and if the
	 * output is null OR a key that is not the one you expect, you try again with the next recId.</p>
	 *
	 * @param recId Which possible key to recover.
	 * @param sig the R and S components of the signature, wrapped.
	 * @param message Hash of the data that was signed.
	 * @param compressed Whether or not the original pubkey was compressed.
	 * @return An ECKey containing only the public part, or null if recovery wasn't possible.
	 */
	@Nullable
	public static ZWPublicKey recoverFromSignature(int recId, ZWECDSASignature sig, ZWSha256Hash message, boolean compressed) {
		Preconditions.checkArgument(recId >= 0, "recId must be positive");
		Preconditions.checkArgument(sig.r.compareTo(BigInteger.ZERO) >= 0, "r must be positive");
		Preconditions.checkArgument(sig.s.compareTo(BigInteger.ZERO) >= 0, "s must be positive");
		Preconditions.checkNotNull(message);
		//   1.0 For j from 0 to h   (h == recId here and the loop is outside this function)
		//   1.1 Let x = r + jn
		BigInteger n = ZWCurveParameters.CURVE.getN();  // Curve order.
		BigInteger i = BigInteger.valueOf((long) recId / 2);
		BigInteger x = sig.r.add(i.multiply(n));
		//   1.2. Convert the integer x to an octet string X of length mlen using the conversion routine
		//        specified in Section 2.3.7, where mlen = ⌈(log2 p)/8⌉ or mlen = ⌈m/8⌉.
		//   1.3. Convert the octet string (16 set binary digits)||X to an elliptic curve point R using the
		//        conversion routine specified in Section 2.3.4. If this conversion routine outputs “invalid”, then
		//        do another iteration of Step 1.
		//
		// More concisely, what these points mean is to use X as a compressed public key.
		ECCurve.Fp curve = (ECCurve.Fp) ZWCurveParameters.CURVE.getCurve();
		BigInteger prime = curve.getQ();  // Bouncy Castle is not consistent about the letter it uses for the prime.
		if (x.compareTo(prime) >= 0) {
			// Cannot have point co-ordinates larger than this as everything takes place modulo Q.
			return null;
		}
		// Compressed keys require you to know an extra bit of data about the y-coord as there are two possibilities.
		// So it's encoded in the recId.
		ECPoint R = decompressKey(x, (recId & 1) == 1);
		//   1.4. If nR != point at infinity, then do another iteration of Step 1 (callers responsibility).
		if (!R.multiply(n).isInfinity())
			return null;
		//   1.5. Compute e from M using Steps 2 and 3 of ECDSA signature verification.
		BigInteger e = message.toBigInteger();
		//   1.6. For k from 1 to 2 do the following.   (loop is outside this function via iterating recId)
		//   1.6.1. Compute a candidate public key as:
		//               Q = mi(r) * (sR - eG)
		//
		// Where mi(x) is the modular multiplicative inverse. We transform this into the following:
		//               Q = (mi(r) * s ** R) + (mi(r) * -e ** G)
		// Where -e is the modular additive inverse of e, that is z such that z + e = 0 (mod n). In the above equation
		// ** is point multiplication and + is point addition (the EC group operator).
		//
		// We can find the additive inverse by subtracting e from zero then taking the mod. For example the additive
		// inverse of 3 modulo 11 is 8 because 3 + 8 mod 11 = 0, and -3 mod 11 = 8.
		BigInteger eInv = BigInteger.ZERO.subtract(e).mod(n);
		BigInteger rInv = sig.r.modInverse(n);
		BigInteger srInv = rInv.multiply(sig.s).mod(n);
		BigInteger eInvrInv = rInv.multiply(eInv).mod(n);
		ECPoint.Fp q = (ECPoint.Fp) ECAlgorithms.sumOfTwoMultiplies(ZWCurveParameters.CURVE.getG(), eInvrInv, R, srInv);

		return new ZWPublicKey(q.getEncoded(compressed));
	}

	/** Decompress a compressed public key (x co-ord and low-bit of y-coord). */
	private static ECPoint decompressKey(BigInteger xBN, boolean yBit) {
		X9IntegerConverter x9 = new X9IntegerConverter();
		byte[] compEnc = x9.integerToBytes(xBN, 1 + x9.getByteLength(ZWCurveParameters.CURVE.getCurve()));
		compEnc[0] = (byte)(yBit ? 0x03 : 0x02);
		return ZWCurveParameters.CURVE.getCurve().decodePoint(compEnc);
	}
	
	public ECPoint getPoint() {
		byte[] x = new byte[32];
		System.arraycopy(this.pub, 1, x, 0, 32);
		boolean yBit = this.isCompressed() ? (this.pub[0] & 1) == 1 : (this.pub[64] & 1) == 1;  
		return decompressKey(new BigInteger(1, x), yBit);
	}
	
	public String getPubHex() {
		return ZiftrUtils.bytesToHexString(pub);
	}

	@Override
	public String toString() {
		return this.getPubHex();
	}

}