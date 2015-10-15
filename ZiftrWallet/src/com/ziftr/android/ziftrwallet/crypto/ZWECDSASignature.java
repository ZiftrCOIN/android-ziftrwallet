/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.DERSequenceGenerator;
import org.spongycastle.asn1.DLSequence;

import com.ziftr.android.ziftrwallet.util.ZLog;


/**
 * Groups the two components that make up a signature, and provides a way to encode to DER form, which is
 * how ECDSA signatures are represented when embedded in other data structures in the Bitcoin protocol. The raw
 * components can be useful for doing further EC maths on them.
 */
public class ZWECDSASignature {
	/** The two components of the signature. */
	public BigInteger r, s;

	/**
	 * Constructs a signature with the given components. Does NOT automatically canonicalise the signature.
	 */
	public ZWECDSASignature(BigInteger r, BigInteger s) {
		this.r = r;
		this.s = s;
	}

	/**
	 * Will automatically adjust the S component to be less than or equal to half the curve order, if necessary.
	 * This is required because for every signature (r,s) the signature (r, -s (mod N)) is a valid signature of
	 * the same message. However, we dislike the ability to modify the bits of a Bitcoin transaction after it's
	 * been signed, as that violates various assumed invariants. Thus in future only one of those forms will be
	 * considered legal and the other will be banned.
	 */
	public void ensureCanonical() {
		if (s.compareTo(ZWCurveParameters.HALF_CURVE_ORDER) > 0) {
			// The order of the curve is the number of valid points that exist on that curve. If S is in the upper
			// half of the number of valid points, then bring it back to the lower half. Otherwise, imagine that
			//    N = 10
			//    s = 8, so (-8 % 10 == 2) thus both (r, 8) and (r, 2) are valid solutions.
			//    10 - 8 == 2, giving us always the latter solution, which is canonical.
			s = ZWCurveParameters.CURVE.getN().subtract(s);
		}
	}

	/**
	 * DER is an international standard for serializing data structures which is widely used in cryptography.
	 * It's somewhat like protocol buffers but less convenient. This method returns a standard DER encoding
	 * of the signature, as recognized by OpenSSL and other libraries.
	 */
	public byte[] encodeToDER() {
		try {
			return derByteStream().toByteArray();
		} catch (IOException e) {
			ZLog.log("Exception encoding to DER " + e);  // Cannot happen.
			return null;
		}
	}

	public static ZWECDSASignature decodeFromDER(byte[] bytes) {
		try {
			ASN1InputStream decoder = new ASN1InputStream(bytes);
			DLSequence seq = (DLSequence) decoder.readObject();
			ASN1Integer r, s;
			try {
				r = (ASN1Integer) seq.getObjectAt(0);
				s = (ASN1Integer) seq.getObjectAt(1);
				// OpenSSL deviates from the DER spec by interpreting these values as unsigned, though they should not be
				// Thus, we always use the positive versions. See: http://r6.ca/blog/20111119T211504Z.html
				return new ZWECDSASignature(r.getPositiveValue(), s.getPositiveValue());
			} catch (ClassCastException e) {
				ZLog.log("decoding DER exception " + e);
			} finally {
				decoder.close();
			}
		} catch (IOException e) {
			ZLog.log("decoding DER IO exception " + e);
		}
		return null;
	}

	protected ByteArrayOutputStream derByteStream() throws IOException {
		// Usually 70-72 bytes.
		ByteArrayOutputStream bos = new ByteArrayOutputStream(72);
		DERSequenceGenerator seq = new DERSequenceGenerator(bos);
		seq.addObject(new ASN1Integer(r));
		seq.addObject(new ASN1Integer(s));
		seq.close();
		return bos;
	}
}