package com.ziftr.android.ziftrwallet.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.spongycastle.asn1.sec.SECNamedCurves;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.crypto.params.ECDomainParameters;

import android.annotation.SuppressLint;

import com.ziftr.android.ziftrwallet.util.ZiftrUtils;
@SuppressLint("TrulyRandom")

public class ZWCurveParameters {

	/** The parameters of the secp256k1 curve that Bitcoin uses. */
	public static final ECDomainParameters CURVE;

	/**
	 * Equal to CURVE.getN().shiftRight(1), used for canonicalising the S value of a signature. If you aren't
	 * sure what this is about, you can ignore it.
	 */
	public static final BigInteger HALF_CURVE_ORDER;
	
	public static final BigInteger CURVE_ORDER;

	private static final SecureRandom secureRandom;

	static {
		// All clients must agree on the curve to use by agreement. Bitcoin uses secp256k1.
		X9ECParameters params = SECNamedCurves.getByName("secp256k1");
		CURVE = new ECDomainParameters(params.getCurve(), params.getG(), params.getN(), params.getH());
		CURVE_ORDER = params.getN();
		HALF_CURVE_ORDER = CURVE_ORDER.shiftRight(1);

		// Have added a fix for this in com.ziftr.android.onewallet.PRNGFixes.java
		// Applied the fix in com.ziftr.android.onewallet.ZWApplication.java
		secureRandom = ZiftrUtils.createTrulySecureRandom();
	}
	
	public static SecureRandom getSecureRandom() {
		return secureRandom;
	}

}
