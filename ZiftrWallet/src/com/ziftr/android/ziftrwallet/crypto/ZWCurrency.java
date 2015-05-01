/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.crypto;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * We just need a common type to be able to store values of currencies.
 */
public interface ZWCurrency {
	
	
	
	/**
	 * returns the number of digits of precision (number of decimal places) for a particular currency,
	 * eg 100 pennies in USD, so precision = 2
	 * eg 10^8 satoshis in a bitcoin, so precision = 8
	 * @return digits of precision
	 */
	public int getScale();
	
	
	/**
	 * returns the {@link String} symbol for a particular currency,
	 * eg US Dollar = $,
	 * eg bitcoin = BTC
	 * @return
	 */
	public String getSymbol();
	
	/**
	 * returns a {@link String} for the short version of the name of a currency, 
	 * eg bitcoin = BTC, 
	 * eg US Dollar = USD
	 * @return
	 */
	public String getShortTitle();
	
	
	/**
	 * converts an amount of atomic units to full coin values
	 * eg satoshis to bitcoins
	 * @param atomicUnits an integer value for the amount of "pennies" to convert
	 * @return a decimal representing the value in coins
	 */
	public BigDecimal getAmount(BigInteger atomicUnits);
	
	
	/**
	 * Converts the decimal amount to atomic units for this coin
	 * eg bitcoins to satoshis
	 * @param amount a decimal value representing an amount of coins
	 * @return an integer representing the value in the smallest possible denomination
	 */
	public BigInteger getAtomicUnits(BigDecimal amount);
	
	
	/**
	 * gets the value of the amount as a string without trailing zeros
	 * @param amount
	 * @return
	 */
	public String getFormattedAmount(BigDecimal amount);

	
	/**
	 * gets the value in coins of the atomic units, without trailing zeros
	 * @param atmoicUnits 
	 * @return 
	 */
	public String getFormattedAmount(BigInteger atmoicUnits);
	
}


