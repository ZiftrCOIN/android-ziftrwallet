package com.ziftr.android.ziftrwallet.util;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * We just need a common type to be able to store values of currencies.
 */
public interface OWCurrency {
	
	public int getNumberOfDigitsOfPrecision();
	
	
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


