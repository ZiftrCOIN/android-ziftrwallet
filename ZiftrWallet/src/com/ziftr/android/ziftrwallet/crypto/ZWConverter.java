package com.ziftr.android.ziftrwallet.crypto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;

import com.ziftr.android.ziftrwallet.util.ZLog;

public class ZWConverter {
	/**
	 * A Map of currencies related to USD.
	 * Formatted as how much USD that 1.0 of the currency is, eg 1.0 bitcoins
	 * might be worth 375.56 USD.
	 */
	private static final Map<ZWCurrency, BigDecimal> convertMap;
	static {
		// TODO need to get these values from some sort of an API
		Map<ZWCurrency, BigDecimal> cMap = new HashMap<ZWCurrency, BigDecimal>();
		cMap.put(ZWFiat.USD, new BigDecimal("1.00", MathContext.DECIMAL64));
		cMap.put(ZWFiat.EUR, new BigDecimal("1.25", MathContext.DECIMAL64));
		cMap.put(ZWFiat.GBP, new BigDecimal("1.59", MathContext.DECIMAL64));

		
		cMap.put(ZWCoin.BTC, new BigDecimal("622.06", MathContext.DECIMAL64));
		cMap.put(ZWCoin.LTC, new BigDecimal("3.65", MathContext.DECIMAL64));
		cMap.put(ZWCoin.PPC, new BigDecimal("1.38", MathContext.DECIMAL64));
		cMap.put(ZWCoin.DOGE, new BigDecimal("0.000227", MathContext.DECIMAL64));

		cMap.put(ZWCoin.BTC_TEST, new BigDecimal("0", MathContext.DECIMAL64));
		cMap.put(ZWCoin.LTC_TEST, new BigDecimal("3.65", MathContext.DECIMAL64));
		cMap.put(ZWCoin.PPC_TEST, new BigDecimal("0", MathContext.DECIMAL64));
		cMap.put(ZWCoin.DOGE_TEST, new BigDecimal("0", MathContext.DECIMAL64));
		convertMap = cMap;
	}

	
	public static BigDecimal convert(BigDecimal amount, ZWCurrency convertFrom, ZWCurrency convertTo) {
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
	

	public static BigInteger convert(BigInteger amount, ZWCurrency convertFrom, ZWCurrency convertTo) {
		
		BigDecimal convertFromDecimal = convertFrom.getAmount(amount);
		BigDecimal convertToDecimal = convert(convertFromDecimal, convertFrom, convertTo);
		
		return convertTo.getAtomicUnits(convertToDecimal);
	}
	
	
	public static void updateConvertRate(ZWCurrency curr, String usdEquiv){
		//don't crash the app if the market value server is down or there is some other network issue
		if(usdEquiv != null && usdEquiv.length() > 0) {
			convertMap.put(curr, new BigDecimal(usdEquiv, MathContext.DECIMAL64));
		} else {
			ZLog.log("updateConvertRate failed for " + curr.toString() + " usdEquiv returned was empty");
		}
	}

}
