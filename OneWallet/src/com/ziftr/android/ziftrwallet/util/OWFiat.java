package com.ziftr.android.ziftrwallet.util;

import java.math.BigDecimal;
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

	/**
	 * A convenience method to format a BigDecimal. In Standard use,
	 * one can just use standard BigDecimal methods and use this
	 * right before formatting for displaying a fiat value. 
	 * 
	 * @param toFormat - The BigDecimal to format.
	 * @return as above
	 */
	public static String formatFiatAmount(OWFiat fiat, BigDecimal toFormat, boolean includeSymbols) {

		String formattedString = "";

		if(includeSymbols) {
			formattedString += fiat.getSymbol();
		}

		formattedString += ZiftrUtils.formatToNDecimalPlaces(fiat.getNumberOfDigitsOfPrecision(), toFormat).toPlainString();

		return formattedString;
	}
	
	public String getCode(){
		return this.code;
	}
	
}