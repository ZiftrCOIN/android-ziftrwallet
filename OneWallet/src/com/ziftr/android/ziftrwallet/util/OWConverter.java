package com.ziftr.android.ziftrwallet.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;

public class OWConverter {
	/**
	 * A Map of currencies related to USD.
	 * Formatted as how much USD that 1.0 of the currency is, eg 1.0 bitcoins
	 * might be worth 375.56 USD.
	 */
	private static final Map<OWCurrency, BigDecimal> convertMap;
	static {
		// TODO need to get these values from some sort of an API
		Map<OWCurrency, BigDecimal> cMap = new HashMap<OWCurrency, BigDecimal>();
		cMap.put(OWFiat.USD, new BigDecimal("1.00", MathContext.DECIMAL64));
		cMap.put(OWFiat.EUR, new BigDecimal("1.25", MathContext.DECIMAL64));
		cMap.put(OWFiat.GBP, new BigDecimal("1.59", MathContext.DECIMAL64));

		
		cMap.put(OWCoin.BTC, new BigDecimal("622.06", MathContext.DECIMAL64));
		cMap.put(OWCoin.LTC, new BigDecimal("3.65", MathContext.DECIMAL64));
		cMap.put(OWCoin.PPC, new BigDecimal("1.38", MathContext.DECIMAL64));
		cMap.put(OWCoin.DOGE, new BigDecimal("0.000227", MathContext.DECIMAL64));

		cMap.put(OWCoin.BTC_TEST, new BigDecimal("0", MathContext.DECIMAL64));
		cMap.put(OWCoin.LTC_TEST, new BigDecimal("3.65", MathContext.DECIMAL64));
		cMap.put(OWCoin.PPC_TEST, new BigDecimal("0", MathContext.DECIMAL64));
		cMap.put(OWCoin.DOGE_TEST, new BigDecimal("0", MathContext.DECIMAL64));
		convertMap = cMap;
	}

	
	public static BigDecimal convert(BigDecimal amount, OWCurrency convertFrom, OWCurrency convertTo) {
		try {
			BigDecimal convertFromUsdMultiplier = convertMap.get(convertFrom);
			BigDecimal convertToUsdMultiplier = convertMap.get(convertTo);
			BigDecimal conversionRate = convertFromUsdMultiplier.divide(convertToUsdMultiplier, MathContext.DECIMAL64);
			
			return amount.multiply(conversionRate, MathContext.DECIMAL64);
			
		} catch (ArithmeticException ae) {
			//certain test coins can have a value of zero could could cause this
			return BigDecimal.ZERO;
		}
	} 
	

	public static BigInteger convert(BigInteger amount, OWCurrency convertFrom, OWCurrency convertTo) {
		
		BigDecimal convertFromDecimal = convertFrom.getAmount(amount);
		BigDecimal convertToDecimal = convert(convertFromDecimal, convertFrom, convertTo);
		
		return convertTo.getAtomicUnits(convertToDecimal);
	}
	
	public static void updateConvertRate(OWCurrency curr, String usdEquiv){
		if (!usdEquiv.isEmpty()){
			convertMap.put(curr, new BigDecimal(usdEquiv, MathContext.DECIMAL64));
		} else {
			ZLog.log("updateConvertRate failed for " + curr.toString() + " usdEquiv returned was empty");
		}
	}

}
