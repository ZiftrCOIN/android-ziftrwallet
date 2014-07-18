package com.ziftr.android.onewallet.util;

public class OWCoin {
	public enum Type {
		BTC("0.0001", "Bitcoin", 8),
		PPC("0.01", "Peercoin", 8),
		DOGE("1.0", "Dogecoin", 8),
		LTC("0.001", "Litecoin", 8),
		
		BTC_TEST("0.000", "BTC Testnet", 8),
		PPC_TEST("0.0", "PPC Test", 8),
		DOGE_TEST("0.0", "DOGE Testnet", 8),
		LTC_TEST("0.000", "LTC Testnet", 8);

		private String defaultFeePerKb;

		private String title;
		
		private int numberOfDigitsOfPrecision;

		private Type(String defaultFeePerKb, String title, 
				int numberOfDigitsOfPrecision) {
			this.defaultFeePerKb = defaultFeePerKb;
			this.title = title;
			this.numberOfDigitsOfPrecision = numberOfDigitsOfPrecision;
		}

		/**
		 * As the name describes, this gets the default fee per kb for the
		 * coin. It might be useful to use BigDecimal() or BigInteger for the parsing
		 * as they are better to store monetary values and they will do the parsing
		 * of this string right in the constructor. 
		 * 
		 * @return as above
		 */
		public String getDefaultFeePerKb() {
			return this.defaultFeePerKb;
		}
		
		// TODO add getLongTitle, getMediumTitle, getShortTitle methods

		/**
		 * Gives the title of the coin. e.g. "Bitcoin" for OWCoin.Type.BTC, etc. 
		 * 
		 * @return as above
		 */
		public String getTitle() {
			return this.title;
		}
		
		/**
		 * This is an okay place to get this information for now, but will likely 
		 * want to get this info somewhere else eventually. This gets the number of 
		 * decimal places of precision, just in case coins have different precision
		 * amounts. For exmaple, for Bitcoin this returns 8 because 1/10^8 is the 
		 * smallest unit of bitcoin.
		 * 
		 * @return as above
		 */
		public int getNumberOfDigitsOfPrecision() {
			return this.numberOfDigitsOfPrecision;
		}
		
	};


}