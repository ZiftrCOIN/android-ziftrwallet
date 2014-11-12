package com.ziftr.android.ziftrwallet.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class OWFiat implements OWCurrency {

	public static final OWFiat USD = new OWFiat("US Dollars","$", "USD", 2);
	public static final OWFiat EUR = new OWFiat("Euros", "\u20ac", "EUR", 2);
	public static final OWFiat GBP = new OWFiat("Pound", "\u00a3", "GBP", 2);


	public static final OWFiat[] TYPES = new OWFiat[] {USD, EUR, GBP};

	public static final OWFiat[] values() {
		return TYPES;
	}

	public static OWFiat valueOf(String fiatStr) {
		for (OWFiat fiat : OWFiat.values()) {
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
	private String code;

	/** For most currencies this is 2, as non-whole amounts are displayed as 0.XX */
	private int numberOfDigitsOfPrecision;

	private OWFiat(String name, String symbol, String code, int numberOfDigitsOfPrecision) {
		this.symbol = symbol;
		this.name = name;
		this.code = code;
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

	public static List<OWFiat> getFiatTypesList(){
		List<OWFiat> fiats = new ArrayList<OWFiat>();
		for (OWFiat type: OWFiat.TYPES){
			fiats.add(type);
		}
		return fiats;

	}

	public String getCode(){
		return this.code;
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




