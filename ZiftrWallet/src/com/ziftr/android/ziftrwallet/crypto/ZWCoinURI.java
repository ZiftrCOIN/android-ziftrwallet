/*
 * Copyright 2012, 2014 the original author or authors.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ziftr.android.ziftrwallet.crypto;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import android.net.Uri;

import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.util.ZLog;

/**
 * <p>Provides a standard implementation of a Bitcoin URI with support for the following:</p>
 *
 * <ul>
 * <li>URLEncoded URIs (as passed in by IE on the command line)</li>
 * <li>BIP21 names (including the "req-" prefix handling requirements)</li>
 * </ul>
 *
 * <h2>Accepted formats</h2>
 *
 * <p>The following input forms are accepted:</p>
 *
 * <ul>
 * <li>{@code bitcoin:<address>}</li>
 * <li>{@code bitcoin:<address>?<name1>=<value1>&<name2>=<value2>} with multiple
 * additional name/value pairs</li>
 * </ul>
 *
 * <p>The name/value pairs are processed as follows.</p>
 * <ol>
 * <li>URL encoding is stripped and treated as UTF-8</li>
 * <li>names prefixed with {@code req-} are treated as required and if unknown or conflicting cause a parse exception</li>
 * <li>Unknown names not prefixed with {@code req-} are added to a Map, accessible by parameter name</li>
 * <li>Known names not prefixed with {@code req-} are processed unless they are malformed</li>
 * </ol>
 *
 * <p>The following names are known and have the following formats:</p>
 * <ul>
 * <li>{@code amount} decimal value to 8 dp (e.g. 0.12345678) <b>Note that the
 * exponent notation is not supported any more</b></li>
 * <li>{@code label} any URL encoded alphanumeric</li>
 * <li>{@code message} any URL encoded alphanumeric</li>
 * </ul>
 * 
 * @author Andreas Schildbach (initial code)
 * @author Jim Burton (enhancements for MultiBit)
 * @author Gary Rowe (BIP21 support)
 * @see <a href="https://github.com/bitcoin/bips/blob/master/bip-0021.mediawiki">BIP 0021</a>
 * 
 * https://en.bitcoin.it/wiki/Talk:BIP_0021
 */
public class ZWCoinURI {

	private String scheme;

	// Not worth turning into an enum
	public static final String FIELD_AMOUNT = "amount";
	public static final String FIELD_LABEL = "label";
	public static final String FIELD_MESSAGE = "message";
	public static final String FIELD_ADDRESS = "address";
	public static final String[] RECOGNIZED_QUERY_FIELDS = new String[] {FIELD_AMOUNT, FIELD_LABEL, FIELD_MESSAGE};

	// TODO see if we can figure out the network stuff for the payment request URL
	// For now, URI parsing should be fine even if this isn't recognized
	// public static final String FIELD_PAYMENT_REQUEST_URL = "r";

	private static final String REQUIRED_INDICATOR = "req-";

	private static final String ENCODED_SPACE_CHARACTER = "%20";
	private static final String AMPERSAND_SEPARATOR = "&";
	private static final String QUESTION_MARK_SEPARATOR = "?";

	/**
	 * Contains all the parameters in the order in which they were processed
	 */
	private final Map<String, String> parameterMap = new LinkedHashMap<String, String>();

	/**
	 * Constructs a new object by trying to parse the input as a valid Coin URI.
	 * URI is formed as: coin:<address>?<query parameters>
	 *
	 * @param params The network parameters that determine which network the URI is from, or null if you don't have
	 *               any expectation about what network the URI is for and wish to check yourself.
	 * @param input The raw URI data to be parsed (see class comments for accepted formats)
	 */
	public ZWCoinURI(ZWCoin coin, String input) throws ZWCoinURIParseException, ZWAddressFormatException {
		try {
			this.scheme = coin.getScheme();
		} catch(NullPointerException npe) {
			throw new ZWCoinURIParseException("The coin id may not be null. ");
		}
		if (input == null || input.isEmpty()) {
			throw new ZWCoinURIParseException("Cannot have null or empty input. ");
		}

		// Have to do this because of a bug in the android Uri parsing library.
		// The bug is that if you don't have the "//" after the ":" then it won't be able
		// to read any query parameters (throws UnsupportedOperationExceptions).
		// Kind of a hack, but it's more error proof than parsing the URI ourselves
		if (input.startsWith(this.scheme) && !input.startsWith(this.scheme + "://")) {
			StringBuilder sb = new StringBuilder(input);
			sb.insert(input.indexOf(":")+1, "//");
			input = sb.toString();
		}

		if (!input.startsWith(this.scheme + "://")) {
			throw new ZWCoinURIParseException("Error parsing uri. Expected scheme: " + this.scheme + ". " + 
					"Scheme received: " + input);
		}

		Uri uri;
		try {
			uri = Uri.parse(input);
		} catch(NullPointerException npe) {
			// Cannot happen
			throw new ZWCoinURIParseException("Error parsing input, input was null. ");
		}

		if (uri.getAuthority() != null && !uri.getAuthority().isEmpty()) {
			if (coin.addressIsValid(uri.getAuthority())) {
				this.parameterMap.put(FIELD_ADDRESS, uri.getAuthority());
			} else {
				throw new ZWAddressFormatException("The address does not validate. ");
			}
		}

		List<String> recognizedQueryParams = Arrays.asList(RECOGNIZED_QUERY_FIELDS);
		for (String key : uri.getQueryParameterNames()) {
			String parameterMapKey = key;
			if (parameterMapKey.startsWith(REQUIRED_INDICATOR)) {
				parameterMapKey = parameterMapKey.substring(REQUIRED_INDICATOR.length());
				if (!recognizedQueryParams.contains(parameterMapKey)) {
					throw new ZWCoinURIParseException("There is a required field that is not recognized. ");
				}
			}

			if (parameterMap.containsKey(parameterMapKey)) {
				throw new ZWCoinURIParseException(String.format("'%s' is duplicated, URI is invalid", key));
			} else {
				parameterMap.put(parameterMapKey, uri.getQueryParameter(key));
			}
		}

	}
	
	/**
	 * The address from the URI, if one was present. It's possible to have Bitcoin URI's with no address if a
	 * r= payment protocol parameter is specified, though this form is not recommended as older wallets can't understand
	 * it.
	 */
	public String getAddress() {
		return getParameterByName(FIELD_ADDRESS, null);
	}

	/**
	 * @return The amount name encoded using a pure integer value based at
	 *         10,000,000 units is 1 BTC. May be null if no amount is specified
	 */
	public String getAmount() {
		return getParameterByName(FIELD_AMOUNT, null);
	}

	/**
	 * @return The label from the URI.
	 */
	public String getLabel() {
		return getParameterByName(FIELD_LABEL, null);
	}

	/**
	 * @return The message from the URI.
	 */
	public String getMessage() {
		return getParameterByName(FIELD_MESSAGE, null);
	}

	@Nullable
	public String getAddress(String defaultValue) {
		return getParameterByName(FIELD_ADDRESS, defaultValue);
	}

	public String getAmount(String defaultValue) {
		return getParameterByName(FIELD_AMOUNT, defaultValue);
	}

	public String getLabel(String defaultValue) {
		return getParameterByName(FIELD_LABEL, defaultValue);
	}

	public String getMessage(String defaultValue) {
		return getParameterByName(FIELD_MESSAGE, defaultValue);
	}

	//	/**
	//	 * @return The URL where a payment request (as specified in BIP 70) may
	//	 *         be fetched.
	//	 */
	//	public String getPaymentRequestUrl() {
	//		return (String) parameterMap.get(FIELD_PAYMENT_REQUEST_URL);
	//	}

	/**
	 * @param name The name of the parameter
	 * @return The parameter value, or null if not present
	 */
	public String getParameterByName(String name, String defaultValue) {
		String valInMap = parameterMap.get(name);
		return valInMap == null ? defaultValue : valInMap;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(scheme).append(" URI[");
		boolean first = true;
		for (Map.Entry<String, String> entry : parameterMap.entrySet()) {
			if (first) {
				first = false;
			} else {
				builder.append(",");
			}
			builder.append("'").append(entry.getKey()).append("'=").append("'").append(entry.getValue().toString()).append("'");
		}
		builder.append("]");
		return builder.toString();
	}

	public static String convertToCoinURI(ZWAddress address, BigInteger amount, String label, String message) {
		try {
		 return convertToCoinURI(address.getCoin(), address.getAddress(), amount, label, message);
		} catch (IllegalArgumentException e) {
			ZLog.log("error converting to coinuri" + e);
			return null;
		}
	}

	/**
	 * Simple Bitcoin URI builder using known good fields.
	 * 
	 * @param address The Bitcoin address
	 * @param amount The amount in nanocoins (decimal)
	 * @param label A label
	 * @param message A message
	 * @return A String containing the Bitcoin URI
	 */
	public static String convertToCoinURI(ZWCoin coinType, String address, BigInteger amount, String label,String message) throws IllegalArgumentException {
		if (amount != null && amount.compareTo(BigInteger.ZERO) < 0) {
			throw new IllegalArgumentException("Amount must be positive");
		}

		StringBuilder builder = new StringBuilder();
		builder.append(coinType.getScheme()).append(":").append(address == null ? "" : address);

		boolean questionMarkHasBeenOutput = false;

		if (amount != null) {
			builder.append(QUESTION_MARK_SEPARATOR).append(FIELD_AMOUNT).append("=");
			builder.append(coinType.getFormattedAmount(amount));
			questionMarkHasBeenOutput = true;
		}

		if (label != null && !label.isEmpty()) {
			if (questionMarkHasBeenOutput) {
				builder.append(AMPERSAND_SEPARATOR);
			} else {
				builder.append(QUESTION_MARK_SEPARATOR);                
				questionMarkHasBeenOutput = true;
			}
			builder.append(FIELD_LABEL).append("=").append(encodeURLString(label));
		}

		if (message != null && !message.isEmpty()) {
			if (questionMarkHasBeenOutput) {
				builder.append(AMPERSAND_SEPARATOR);
			} else {
				builder.append(QUESTION_MARK_SEPARATOR);
			}
			builder.append(FIELD_MESSAGE).append("=").append(encodeURLString(message));
		}

		return builder.toString();
	}

	/**
	 * Encode a string using URL encoding
	 * 
	 * @param stringToEncode The string to URL encode
	 */
	static String encodeURLString(String stringToEncode) {
		try {
			return java.net.URLEncoder.encode(stringToEncode, "UTF-8").replace("+", ENCODED_SPACE_CHARACTER);
		} catch (UnsupportedEncodingException e) {
			// should not happen - UTF-8 is a valid encoding
			ZLog.log("encodeURLstring error " + e);
			return null;
		}
	}

	//	String input = "bitcoin://mqtBMfnRTpvxAzdGMiowWhLXMGR9uevuKM?r=https%3A%2F%2Fbitcoincore.org%2F%7Egavin%2Ff.php%3Fh%3Dc611e23a6f08bc0a0ac1a7fc38f02015&amount=10&dummy=false";
	//	Uri uri = Uri.parse(input);
	//
	//	ZLog.log("scheme: \n" + uri.getScheme() + "\n");
	//	ZLog.log("SchemeSpecificPart: \n" + uri.getSchemeSpecificPart() + "\n");
	//	ZLog.log("authority: \n" + uri.getAuthority() + "\n");
	//	ZLog.log("UserInfo: \n" + uri.getUserInfo() + "\n");
	//	ZLog.log("fragment: \n" + uri.getFragment() + "\n");
	//	ZLog.log("host: \n" + uri.getHost() + "\n");
	//	ZLog.log("getLastPathSegment: \n" + uri.getLastPathSegment() + "\n");
	//	ZLog.log("path: \n" + uri.getPath() + "\n");
	//	ZLog.log("port: \n" + uri.getPort() + "\n");
	//	ZLog.log("query: \n" + uri.getQuery() + "\n");
	//
	//	ZLog.log("query key strings:");
	//	for (String s : uri.getQueryParameterNames()) {
	//		ZLog.log(s + ": " + uri.getQueryParameter(s));
	//	}

}









