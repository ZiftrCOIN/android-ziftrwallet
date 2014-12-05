package com.ziftr.android.ziftrwallet.crypto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import com.ziftr.android.ziftrwallet.ZWWalletManager;

public class ZWConverter {

	public static BigDecimal convert(BigDecimal amount, ZWCurrency convertFrom, ZWCurrency convertTo) {
		try {
			BigDecimal rate = new BigDecimal (ZWWalletManager.getInstance().getExchangeValue(convertFrom, convertTo), MathContext.DECIMAL64);
			return amount.multiply(rate, MathContext.DECIMAL64);
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
	
}
