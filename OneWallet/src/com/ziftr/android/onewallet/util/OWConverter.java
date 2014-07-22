package com.ziftr.android.onewallet.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class OWConverter {
	private static final Map<OWCurrency, BigDecimal> convertMap;
	static {
		// TODO need to get these values from some sort of an API
		Map<OWCurrency, BigDecimal> cMap = new HashMap<OWCurrency, BigDecimal>();
		cMap.put(OWFiat.Type.USD, new BigDecimal("1.00", MathContext.DECIMAL64));
		cMap.put(OWFiat.Type.EUR, new BigDecimal("0.74", MathContext.DECIMAL64));

		cMap.put(OWCoin.Type.BTC, new BigDecimal("622.06", MathContext.DECIMAL64));
		cMap.put(OWCoin.Type.LTC, new BigDecimal("8.60", MathContext.DECIMAL64));
		cMap.put(OWCoin.Type.PPC, new BigDecimal("1.38", MathContext.DECIMAL64));
		cMap.put(OWCoin.Type.DOGE, new BigDecimal("0.000227", MathContext.DECIMAL64));

		cMap.put(OWCoin.Type.BTC_TEST, new BigDecimal("0", MathContext.DECIMAL64));
		cMap.put(OWCoin.Type.LTC_TEST, new BigDecimal("0", MathContext.DECIMAL64));
		cMap.put(OWCoin.Type.PPC_TEST, new BigDecimal("0", MathContext.DECIMAL64));
		cMap.put(OWCoin.Type.DOGE_TEST, new BigDecimal("0", MathContext.DECIMAL64));
		convertMap = Collections.unmodifiableMap(cMap);
	}

	public static BigDecimal convert(BigDecimal amount, 
			OWCurrency convertFrom, OWCurrency convertTo) {
		try {
			return amount
					.multiply(convertMap.get(convertFrom), MathContext.DECIMAL64)
					.divide(convertMap.get(convertTo), MathContext.DECIMAL64);
		} catch (ArithmeticException ae) {
			return BigDecimal.ZERO;
		}

	} 

	public static BigInteger convert(BigInteger amount, 
			OWCurrency convertFrom, OWCurrency convertTo) {
		return OWUtils.bigDecToBigInt(convertTo, 
				convert(OWUtils.bigIntToBigDec(convertFrom, amount), 
						convertFrom, convertTo));
	}

}