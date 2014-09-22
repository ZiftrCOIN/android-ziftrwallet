package com.ziftr.android.ziftrwallet.util;

import java.math.BigDecimal;

public class OWFiat implements OWCurrency {

	public static final OWFiat USD = new OWFiat("US Dollars","$", 2);
	public static final OWFiat EUR = new OWFiat("Euros", String.valueOf((char) 0x80), 2);

	public static final OWFiat[] TYPES = new OWFiat[] {USD, EUR};

	public static final OWFiat[] values() {
		return TYPES;
	}
	
	public static OWFiat valueOf(String fiatStr) {
		for (OWFiat fiat : OWFiat.values()) {
			if (fiat.toString().equals(fiatStr)) {
				return fiat;
			}
		}
		return null;
	}

	/** This is the symbol for the currency. "$" for USD, etc. */
	private String symbol;

	/** Name of currency US Dollars for USD*/
	private String name;

	/** For most currencies this is 2, as non-whole amounts are displayed as 0.XX */
	private int numberOfDigitsOfPrecision;

	private OWFiat(String name, String symbol, int numberOfDigitsOfPrecision) {
		this.symbol = symbol;
		this.name = name;
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

}