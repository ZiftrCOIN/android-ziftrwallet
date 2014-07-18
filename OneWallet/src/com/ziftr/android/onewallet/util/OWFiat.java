package com.ziftr.android.onewallet.util;

public class OWFiat {
	public enum Type {
		USD("$"),
		EUR(String.valueOf((char) 0x80));
		
		/** This is the symbol for the currency. "$" for USD, etc. */
		private String symbol;
		
		private Type(String symbol) {
			this.symbol = symbol;
		}
		
		/**
		 * @return the symbol
		 */
		public String getSymbol() {
			return symbol;
		}

		/**
		 * @param symbol the symbol to set
		 */
		public void setSymbol(String symbol) {
			this.symbol = symbol;
		}
	}
}