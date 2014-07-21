package com.ziftr.android.onewallet.util;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;
import com.ziftr.android.onewallet.R;

public class OWCoin {
	public enum Type {
		BTC("0.0001", "BTC", "Bitcoin", "BITCOIN", 
				8, R.drawable.logo_bitcoin, MainNetParams.get()),
		LTC("0.0010", "LTC", "Litecoin", "LITECOIN", 
				8, R.drawable.logo_litecoin, null),
		PPC("0.0100", "PPC", "Peercoin", "PEERCOIN", 
				8, R.drawable.logo_peercoin, null),
		DOGE("1.0000", "DOGE", "Dogecoin", "DOGECOIN", 
				8, R.drawable.logo_dogecoin, null),
				
		BTC_TEST("0.0000", "BTC_TEST", "Bitcoin Testnet", "BITCOIN", 
				8, R.drawable.logo_bitcoin, TestNet3Params.get()),
		LTC_TEST("0.0000", "LTC_TEST", "Litecoin Testnet", "LITECOIN", 
				8, R.drawable.logo_litecoin, null),
		PPC_TEST("0.0000", "PPC_TEST", "Peercoin Testnet", "PEERCOIN", 
				8, R.drawable.logo_peercoin, null),
		DOGE_TEST("0.0000", "DOGE_TEST", "Dogecoin Testnet", "DOGECOIN", 
				8, R.drawable.logo_dogecoin, null),
		;

		private String defaultFeePerKb;

		private String shortTitle;
		
		private String longTitle;
		
		private String capsTitle;
		
		private int numberOfDigitsOfPrecision;
		
		private int logoResId;
		
		private NetworkParameters networkParameters;

		private Type(String defaultFeePerKb, String shortTitle, 
				String longTitle, String capsTitle,
				int numberOfDigitsOfPrecision, int logoResId,
				NetworkParameters networkParameters) {
			this.defaultFeePerKb = defaultFeePerKb;
			this.shortTitle = shortTitle;
			this.longTitle = longTitle;
			this.capsTitle = capsTitle;
			this.numberOfDigitsOfPrecision = numberOfDigitsOfPrecision;
			this.logoResId = logoResId;
			this.networkParameters = networkParameters;
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
		
		/**
		 * @return the shortTitle
		 */
		public String getShortTitle() {
			return shortTitle;
		}

		/**
		 * @return the longTitle
		 */
		public String getLongTitle() {
			return longTitle;
		}

		/**
		 * @return the capsTitle
		 */
		public String getCapsTitle() {
			return capsTitle;
		}
		
		/**
		 * Gives the title of the coin. e.g. "Bitcoin" for OWCoin.Type.BTC, etc. 
		 * 
		 * @return as above
		 */
		
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
		
		/**
		 * @return the logoResId
		 */
		public int getLogoResId() {
			return logoResId;
		}
		

		/**
		 * @return the networkParameters
		 */
		public NetworkParameters getNetworkParameters() {
			return networkParameters;
		}
		
	};

}