package com.ziftr.android.ziftrwallet.crypto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWFiat implements ZWCurrency {

	public static final ZWFiat USD = new ZWFiat("US Dollars","$", "USD", 2);
	public static final ZWFiat EUR = new ZWFiat("Euros", "\u20ac", "EUR", 2);
	public static final ZWFiat GBP = new ZWFiat("Pound", "\u00a3", "GBP", 2);


	public static final ZWFiat[] TYPES = new ZWFiat[] {USD, EUR, GBP};

	public static final ZWFiat[] values() {
		return TYPES;
	}

	public static ZWFiat valueOf(String fiatStr) {
		for (ZWFiat fiat : ZWFiat.values()) {
			if (fiat.getName().equals(fiatStr)) {
				return fiat;
			}
		}
		return null;
	}

	/** This is the symbol for the currency. "$" for USD, etc. */
	private String symbol;
	private boolean symbolBeforeNumber = true;
	
	/** Name of currency US Dollars for USD*/
	private String name;
	
	/** short name of currency USD for dollars*/
	private String shortTitle;

	/** For most currencies this is 2, as non-whole amounts are displayed as 0.XX */
	private int numberOfDigitsOfPrecision;

	private ZWFiat(String name, String symbol, String shortTitle, int numberOfDigitsOfPrecision) {
		this.symbol = symbol;
		this.name = name;
		this.shortTitle = shortTitle;
		this.numberOfDigitsOfPrecision = numberOfDigitsOfPrecision;
	}

	/**
	 * @return the symbol
	 */
	public String getSymbol() {
		return symbol;
	}

	/**
	 * @return name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @return the numberOfDigitsOfPrecision
	 */
	@Override
	public int getNumberOfDigitsOfPrecision() {
		return numberOfDigitsOfPrecision;
	}

	public static List<ZWFiat> getFiatTypesList(){
		List<ZWFiat> fiats = new ArrayList<ZWFiat>();
		for (ZWFiat type: ZWFiat.TYPES){
			fiats.add(type);
		}
		return fiats;

	}

	public String getShortTitle(){
		return this.shortTitle;
	}
	

	@Override
	public BigDecimal getAmount(BigInteger atomicUnits) {
		return new BigDecimal(atomicUnits, this.getNumberOfDigitsOfPrecision());
	}

	
	@Override
	public BigInteger getAtomicUnits(BigDecimal amount) {
		int precision = -1*this.getNumberOfDigitsOfPrecision();
		
		//note, this constructor is weird to me, but RTFM, it's for making small numbers and makes the scale negative
		BigDecimal multiplier = new BigDecimal(BigInteger.ONE, precision); 
		return amount.multiply(multiplier).toBigInteger();
	}

	

	public String getFormattedAmount(BigDecimal amount, boolean addSymbol) {

		String formattedString = "";

		if(addSymbol && symbolBeforeNumber) {
			formattedString += this.getSymbol();
		}

		BigDecimal formatted = ZiftrUtils.formatToNDecimalPlaces(getNumberOfDigitsOfPrecision(), amount);
		
		formattedString += formatted.toPlainString();

		if(addSymbol && !symbolBeforeNumber) {
			formattedString += this.getSymbol();
		}
		
		return formattedString;
	}
	
	
	@Override
	public String getFormattedAmount(BigDecimal amount) {
		return this.getFormattedAmount(amount, false);
	}

	
	public String getFormattedAmount(BigInteger atmoicUnits, boolean addSymbol) {
		BigDecimal toFormatAsDecimal = this.getAmount(atmoicUnits);
		BigDecimal dollars = ZiftrUtils.formatToNDecimalPlaces(this.getNumberOfDigitsOfPrecision(), toFormatAsDecimal);
		
		return this.getFormattedAmount(dollars, addSymbol);
	}
	
	@Override
	public String getFormattedAmount(BigInteger atmoicUnits) {
		return this.getFormattedAmount(atmoicUnits, false);
	}

}




