package com.ziftr.android.onewallet.util;

import java.math.BigDecimal;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;
import com.ziftr.android.onewallet.R;

/**
 * This class holds the Type enum. Each member of the enum can be thought of as an identifier for 
 * that cryptocurrency type. Abstract classes can define an abstract getCoinId() method and 
 * then subclasses can be made that implement this method, which is very useful for making classes 
 * applicable to many types of cryptocurrencies. 
 * 
 * TODO somehow incorporate a list of address prefixes for each coin type. 
 * e.g. bitcoin address start with a 1, bitcoin private keys start with 
 * 
 * Four leading prefixes are needed, currently, for each coin type:
 *     1. pubKeyHashPrefix
 *     2. scriptHashPrefix
 *     3. privKeyPrefix
 */
public class OWCoin {

	/** When using bundles, this can be used to store a specific coin type. */
	public static final String TYPE_KEY = "OWCOIN_TYPE_KEY";

	public enum Type implements OWCurrency {
		BTC("0.0001", "BTC", "Bitcoin", "BITCOIN", 8, R.drawable.logo_bitcoin, MainNetParams.get(), false,
				(byte) 0, (byte) 5, (byte) 128),
		LTC("0.0010", "LTC", "Litecoin", "LITECOIN", 8, R.drawable.logo_litecoin, null, false,
				(byte) 0, (byte) 0, (byte) 0),
		PPC("0.0100", "PPC", "Peercoin", "PEERCOIN", 8, R.drawable.logo_peercoin, null, false,
				(byte) 0, (byte) 0, (byte) 0),
		DOGE("1.0000", "DOGE", "Dogecoin", "DOGECOIN", 8, R.drawable.logo_dogecoin, null, false,
				(byte) 0, (byte) 0, (byte) 0),

		BTC_TEST("0.0000", "BTC_TEST", "Bitcoin Testnet", "BITCOIN", 8, R.drawable.logo_bitcoin, TestNet3Params.get(), true,
				(byte) 0, (byte) 5, (byte) 128),
//				(byte) 111, (byte) 196, (byte) 239, (byte) 239),
		LTC_TEST("0.0000", "LTC_TEST", "Litecoin Testnet", "LITECOIN", 8, R.drawable.logo_litecoin, null, true,
				(byte) 0, (byte) 0, (byte) 0),
		PPC_TEST("0.0000", "PPC_TEST", "Peercoin Testnet", "PEERCOIN", 8, R.drawable.logo_peercoin, null, true,
				(byte) 0, (byte) 0, (byte) 0),
		DOGE_TEST("0.0000", "DOGE_TEST", "Dogecoin Testnet", "DOGECOIN", 8, R.drawable.logo_dogecoin, null, true,
				(byte) 0, (byte) 0, (byte) 0)
		;

		private String defaultFeePerKb;
		private String shortTitle;
		private String longTitle;
		private String capsTitle;
		private int numberOfDigitsOfPrecision;
		private int logoResId;
		private NetworkParameters networkParameters;
		private boolean isTestNet;
		private byte pubKeyHashPrefix;
		private byte scriptHashPrefix;
		private byte privKeyPrefix;

		private Type(String defaultFeePerKb, String shortTitle, String longTitle, String capsTitle,
				int numberOfDigitsOfPrecision, int logoResId, NetworkParameters networkParameters, 
				boolean isTestNet, byte pubKeyHashPrefix, byte scriptHashPrefix, 
				byte privKeyPrefix) {
			this.defaultFeePerKb = defaultFeePerKb;
			this.shortTitle = shortTitle;
			this.longTitle = longTitle;
			this.capsTitle = capsTitle;
			this.numberOfDigitsOfPrecision = numberOfDigitsOfPrecision;
			this.logoResId = logoResId;
			this.networkParameters = networkParameters;
			this.isTestNet = isTestNet;
			this.pubKeyHashPrefix = pubKeyHashPrefix;
			this.scriptHashPrefix = scriptHashPrefix;
			this.privKeyPrefix = privKeyPrefix;
		}

		/**
		 * @return whether or not this coin is a testnet coin
		 */
		public boolean isTestNet() {
			return isTestNet;
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
		@Override
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
		
		/**
		 * @return the pubKeyHashPrefix
		 */
		public byte getPubKeyHashPrefix() {
			return pubKeyHashPrefix;
		}

		/**
		 * @return the scriptHashPrefix
		 */
		public byte getScriptHashPrefix() {
			return scriptHashPrefix;
		}

		/**
		 * @return the uncompressedPrivKeyPrefix
		 */
		public byte getPrivKeyPrefix() {
			return privKeyPrefix;
		}


	};

	/**
	 * A convenience method to format a BigDecimal. In Standard use,
	 * one can just use standard BigDecimal methods and use this
	 * right before formatting for displaying a coin value.
	 * 
	 * @param toFormat - The BigDecimal to format.
	 * @return as above
	 */
	public static BigDecimal formatCoinAmount(OWCoin.Type coinType, BigDecimal toFormat) {
		return OWUtils.formatToNDecimalPlaces(
				coinType.getNumberOfDigitsOfPrecision(), toFormat);
	}

}