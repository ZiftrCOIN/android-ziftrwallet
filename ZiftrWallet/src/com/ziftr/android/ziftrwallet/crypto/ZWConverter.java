package com.ziftr.android.ziftrwallet.crypto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import com.ziftr.android.ziftrwallet.ZWWalletManager;
import com.ziftr.android.ziftrwallet.sqlite.ZWWalletTransactionTable;

public class ZWConverter {

	public static BigDecimal convert(BigDecimal amount, ZWCoin convertFrom, ZWFiat convertTo) {
		try {
			BigDecimal rate = new BigDecimal (ZWWalletManager.getInstance().getExchangeValue(convertFrom, convertTo), MathContext.DECIMAL64);
			return amount.multiply(rate, MathContext.DECIMAL64);
		} catch (ArithmeticException ae) {
			//certain test coins can have a value of zero could could cause this
			return BigDecimal.ZERO;
		}
	}
	
	public static BigDecimal convert(BigDecimal amount, ZWFiat convertFrom, ZWCoin convertTo) {
		try {
			BigDecimal rate = new BigDecimal (ZWWalletManager.getInstance().getExchangeValue(convertFrom, convertTo), MathContext.DECIMAL64);
			return amount.divide(rate, MathContext.DECIMAL64);
		} catch (ArithmeticException ae) {
			//certain test coins can have a value of zero could could cause this
			return BigDecimal.ZERO;
		}
	}
	

	public static BigInteger convert(BigInteger amount, ZWCoin convertFrom, ZWFiat convertTo) {
		
		BigDecimal convertFromDecimal = convertFrom.getAmount(amount);
		BigDecimal convertToDecimal = convert(convertFromDecimal, convertFrom, convertTo);
		
		return convertTo.getAtomicUnits(convertToDecimal);
	}
	
	public static BigInteger convert(BigInteger amount, ZWFiat convertFrom, ZWCoin convertTo) {
		
		BigDecimal convertFromDecimal = convertFrom.getAmount(amount);
		BigDecimal convertToDecimal = convert(convertFromDecimal, convertFrom, convertTo);

		return convertTo.getAtomicUnits(convertToDecimal);
	}
	
}
