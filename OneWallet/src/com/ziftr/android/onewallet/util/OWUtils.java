package com.ziftr.android.onewallet.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.spongycastle.crypto.digests.RIPEMD160Digest;

import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.VarInt;
import com.google.common.base.Charsets;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class OWUtils {

	private static final ExecutorService threadService = Executors.newCachedThreadPool();

	/** formatting for date written like "2013-08-26T13:59:30-04:00" */
	public static final DateFormat formatter = 
			new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
	/** formatting for date written like "Tue, 19 Nov 2013 10:28:31 -0500" */
	public static final DateFormat formatterExpanded = 
			new SimpleDateFormat("EE, dd MMM yyyy HH:mm:ss Z", Locale.US);
	/** formatting for date written like "2013-12-11 10:59:48" */
	public static final DateFormat formatterNoTimeZone = 
			new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()); 

	/** The string that prefixes all text messages signed using Bitcoin keys. */
	public static final String BITCOIN_SIGNED_MESSAGE_HEADER = "Bitcoin Signed Message:\n";
	public static final byte[] BITCOIN_SIGNED_MESSAGE_HEADER_BYTES = BITCOIN_SIGNED_MESSAGE_HEADER.getBytes(Charsets.UTF_8);

	private static final MessageDigest digest;
	static {
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			// Can't happen, we should know about it if it does happen.
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts a date string in a format such as "2013-08-26T13:59:30-04:00" 
	 * into milliseconds.
	 * 
	 * @param dateString string representing the date
	 * @return time in milliseconds, or -1 if the string cannot be parsed to a date
	 */
	public static long getTimeMillis(String dateString) {

		try {
			Date date = formatter.parse(dateString);
			return date.getTime();
		} catch (ParseException e) {
			ZLog.log("Exception trying to parse date: ", dateString, " - ", e);
		}

		return -1;
	}

	/**
	 * Converts a date string in a format such as "Tue, 19 Nov 2013 10:28:31 -0500" 
	 * into milliseconds.
	 * 
	 * @param dateString string representing the date
	 * @return time in milliseconds, or -1 if the string cannot be parsed to a date
	 */
	public static long getTimeMillisExpanded(String expandedDateString) {

		try {
			Date date = formatterExpanded.parse(expandedDateString);
			return date.getTime();
		} catch (ParseException e) {
			ZLog.log("Exception trying to parse date: ", expandedDateString, " - ", e);
		}

		return -1;
	}

	/**
	 * Converts a date string in a format such as "2013-12-11 10:59:48" into 
	 * milliseconds. Assumes users current time zone.
	 * 
	 * @param noTimeZoneDateString string representing the date
	 * @return time in milliseconds, or -1 if string cannot be parsed to a date
	 */
	public static long getTimeMillisNoTimeZone(String noTimeZoneDateString) {
		try {
			Date date = formatterNoTimeZone.parse(noTimeZoneDateString);
			return date.getTime();
		} catch(ParseException e) {
			ZLog.log("Exception trying to parse date: ", noTimeZoneDateString, " - ", e);
		}

		return -1;
	}


	/** 
	 * Converts the specified number of dp units to the system specific
	 * number of pixels.
	 * 
	 * @param dp - The number of dp to conver.
	 * @return The result of the conversion.
	 */
	public static int convertDpToPixels(int dp) {
		return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
	}

	/**
	 * takes a timestamp in milliseconds and changes it to a string format the 
	 * server expects, such as "2013-08-26T13:59:30-04:00".
	 * 
	 * @param timestamp time in ms
	 * @return formatted string representing the date and time of the timestamp
	 */
	public static String formatTimestamp(long timestamp) {
		Date date = new Date(timestamp);
		return formatter.format(date);
	}

	/**
	 * Pipes the data in an input stream to an output stream.
	 * Both streams will be wrapped in buffered streams before piping.
	 * The input stream will be closed when finished.
	 * The output stream will be flushed and closed when finished.
	 * Note: this call is done on the calling thread and will block if
	 * the streams are file or network streams
	 * @param input an {@link InputStream} containing the data to be read
	 * @param output an {@link OutputStream} to write the data to
	 */
	public static void pipeStreams(InputStream input, OutputStream output) {

		BufferedInputStream inStream = new BufferedInputStream(input);
		BufferedOutputStream outStream = new BufferedOutputStream(output);

		try {
			byte[] buffer = new byte[4096];
			int read = inStream.read(buffer);
			while(read >= 0) {

				outStream.write(buffer, 0, read);
				read = inStream.read(buffer);
			}
			outStream.flush();

		} catch (IOException e) {
			ZLog.log("Exception piping streams: ", e);
		} finally {
			try {
				inStream.close();
			} catch (IOException e) {
				ZLog.log(e);
			} try {
				outStream.close();
			} catch (IOException e) {
				ZLog.log(e);
			}
		}

	}

	/**
	 * Takes an input stream, reads it into a string, then closes the stream
	 * @param stream An {@link InputStream} to be read and closed.
	 * @return a String of whatever was contained in the input stream, returns a 
	 * blank strink if there was an error
	 */
	public static String streamToString(InputStream stream) {

		StringBuilder builder = new StringBuilder("");

		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		String line = null;
		try {
			while((line = reader.readLine()) != null) {
				builder.append(line);
			}
		} catch (IOException e) {
			ZLog.log("Exception reading stream to string: \n", e);
		} finally {
			try {
				reader.close();
			} catch(Exception e) {}
		}

		return builder.toString();
	}

	/**
	 * A simple method meant to run an anonymous inner class {@link Runnable} 
	 * as soon as it's called.
	 * 
	 * @param runnable the runnable to be run
	 */
	public static void runOnNewThread(Runnable runnable) {
		threadService.execute(runnable);
	}

	/** 
	 * Closes the keyboard for the current activity.
	 * 
	 * @param activity - The current activity.
	 */
	public static void closeKeyboard(Activity activity) {
		InputMethodManager inputMan = (InputMethodManager)
				activity.getSystemService(Context.INPUT_METHOD_SERVICE);
		View currentFocus = activity.getCurrentFocus();
		inputMan.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
	}

	/**
	 * Returns the Sha256 hash of the specified byte array.
	 * The returned array will have 32 elements.
	 * 
	 * @param arr - The array of information to hash.
	 * @return - the Sha256 hash of the specified byte array.
	 */
	public static byte[] Sha256Hash(byte[] arr) {
		synchronized (digest) {
			return digest.digest(arr);
		}
	}

	/**
	 * Converts an array of bytes to string data.
	 * For example:
	 * [0xf0, 0x34, 0x5a] => "f0345a"
	 * 
	 * @param data - The byte array to convert.
	 * @return The result of the conversion.
	 */
	public static String binaryToHexString(byte[] data) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < data.length; i++) {
			sb.append(String.format("%02x", data[i]));
		}
		return sb.toString();
	}

	/**
	 * Converts a hex string to an array of bytes.
	 * For example:
	 * "f0345a" => [0xf0, 0x34, 0x5a] 
	 * 
	 * If string is not a properly formatted hex string then
	 * this method will throw a number format exception. 
	 * 
	 * @param hexStr - The string to convert.
	 * @return The result of the conversion.
	 */
	public static byte[] hexStringToBinary(String hexStr) {
		// TODO is this the same: Hex.decode(hexStr); ???
		byte bArray[] = new byte[hexStr.length()/2];  
		for (int i=0; i<(hexStr.length()/2); i++) {
			// [x,y) 
			byte firstNibble  = Byte.parseByte(
					hexStr.substring(2*i,2*i+1),16); 
			byte secondNibble = Byte.parseByte(
					hexStr.substring(2*i+1,2*i+2),16);
			// bit-operations only with numbers, not bytes.
			int finalByte = (secondNibble) | (firstNibble << 4 ); 
			bArray[i] = (byte) finalByte;
		}
		return bArray;
	}

	/**
	 * Given a double, this method gives the number of 'satoshis' in the double.
	 * For example, 1.5001 BTC (where "BTC" is the coinType) will return 150010000. 
	 * 
	 * @param coinType - The type of coin, "BTC" for bitcoin, etc. 
	 * @param d - the amount to convert.
	 */
	public static int doubleToAtomicUnits(String coinType, double d) {
		if ("BTC".equals(coinType)) {
			d *= Math.pow(10, 8);
			return (int) d;
		}
		return 0;
	}

	/**
	 * Given a double as a string, this method gives the number of 'satoshis' 
	 * in the double.
	 * For example, "1.5001" BTC (where "BTC" is the coinType) will return 150010000. 
	 * 
	 * @param coinType - The type of coin, "BTC" for bitcoin, etc. 
	 * @param d - the amount to convert.
	 */
	public static int stringToAtomicUnits(String coinType, String d) {
		double val = 0.0;
		if ("BTC".equals(coinType)) {
			try {
				val = Double.parseDouble(d);
			} catch(NumberFormatException nfe) {
				return 0;
			}
		}
		return doubleToAtomicUnits(coinType, val);
	}

	/**
	 * Based on the coin type, this method converts back the number of atomic
	 * units to a double.
	 * 
	 * @param coinType - "BTC" for bitcoin, etc.
	 * @param numAtomicUnits - The number of atomic units to convert. 
	 * @return The integer converted back to a double.
	 */
	public static double atomicUnitsToDouble(String coinType, int numAtomicUnits) {
		if ("BTC".equals(coinType)) {
			double d = (double) numAtomicUnits;
			d /= Math.pow(10, 8);
			return d;
		}
		return 0;
	}

	/**
	 * Based on the coin type, this method converts back the number of atomic
	 * units to a double and gives back a string containing that double.
	 * 
	 * @param coinType - "BTC" for bitcoin, etc.
	 * @param numAtomicUnits - The number of atomic units to convert. 
	 * @return The integer converted back to a double, represented as a string.
	 */
	public static String atomicUnitsToString(String coinType, int numAtomicUnits) {
		BigDecimal basic = 
				(new BigDecimal(atomicUnitsToDouble(coinType, numAtomicUnits)));
		return trimZeroes(formatTo8DecimalPlaces(basic));
	}

	/**
	 * This makes it so that the BigDecimal returned has a string value with
	 * exactly 8 decimal places. If it is desirable to have the zeroes trimmed,
	 * call trimZeroes on the result.
	 * 
	 * @param toFormat - The big decimal to format
	 * @return a new big decimal formatted correctly as above.
	 */
	public static BigDecimal formatTo8DecimalPlaces(BigDecimal toFormat) {
		return formatToNDecimalPlaces(8, toFormat);
	}

	/**
	 * This makes it so that the BigDecimal returned has a string value with
	 * exactly numDecimalPlaces decimal places. If it is desirable to have 
	 * the zeroes trimmed, call trimZeroes on the result.
	 * 
	 * @param toFormat - The big decimal to format
	 * @return a new big decimal formatted correctly as above.
	 */
	public static BigDecimal formatToNDecimalPlaces(int numDecimalPlaces, 
			BigDecimal toFormat) {
		return toFormat.setScale(numDecimalPlaces, RoundingMode.HALF_UP);
	}

	/**
	 * Trims the zeroes 
	 * @param decToFormat
	 * @return
	 */
	public static String trimZeroes(BigDecimal decToFormat) {
		return String.format("%s", decToFormat.doubleValue());
		//		// Check that not null
		//		if (decToFormat == null) {
		//			return null;
		//		}
		//		
		//		// Trim white space
		//		decToFormat = decToFormat.trim();
		//		
		//		// Get rid of leading zeroes
		//		while (decToFormat.startsWith("0")) {
		//			decToFormat = decToFormat.substring(1);
		//		}
		//		
		//		// Get rid of trailing zeroes
		//		while (decToFormat.endsWith("0")) {
		//			decToFormat = decToFormat.substring(0, decToFormat.length()-1);
		//		}
		//		
		//		// Return the result
		//		return decToFormat;
	}

	/**
	 * Returns the given value in nanocoins as a 0.12 type string. More digits after the decimal place will be used
	 * if necessary, but two will always be present.
	 */
	public static String bitcoinValueToFriendlyString(BigInteger value) {
		// TODO: This API is crap. This method should go away when we encapsulate money values.
		boolean negative = value.compareTo(BigInteger.ZERO) < 0;
		if (negative)
			value = value.negate();
		BigDecimal bd = new BigDecimal(value, 8);
		String formatted = bd.toPlainString();   // Don't use scientific notation.
		int decimalPoint = formatted.indexOf(".");
		// Drop unnecessary zeros from the end.
		int toDelete = 0;
		for (int i = formatted.length() - 1; i > decimalPoint + 2; i--) {
			if (formatted.charAt(i) == '0')
				toDelete++;
			else
				break;
		}
		return (negative ? "-" : "") + formatted.substring(0, formatted.length() - toDelete);
	}

	/**
	 * Calculates RIPEMD160(SHA256(input)). This is used in Address calculations.
	 */
	public static byte[] sha256hash160(byte[] input) {
		try {
			byte[] sha256 = MessageDigest.getInstance("SHA-256").digest(input);
			RIPEMD160Digest digest = new RIPEMD160Digest();
			digest.update(sha256, 0, sha256.length);
			byte[] out = new byte[20];
			digest.doFinal(out, 0);
			return out;
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);  // Cannot happen.
		}
	}

	/**
	 * See {@link Utils#doubleDigest(byte[], int, int)}.
	 */
	public static byte[] doubleDigest(byte[] input) {
		return doubleDigest(input, 0, input.length);
	}

	/**
	 * Calculates the SHA-256 hash of the given byte range, and then hashes the resulting hash again. This is
	 * standard procedure in Bitcoin. The resulting hash is in big endian form.
	 */
	public static byte[] doubleDigest(byte[] input, int offset, int length) {
		synchronized (digest) {
			digest.reset();
			digest.update(input, offset, length);
			byte[] first = digest.digest();
			return digest.digest(first);
		}
	}

	public static byte[] singleDigest(byte[] input, int offset, int length) {
		synchronized (digest) {
			digest.reset();
			digest.update(input, offset, length);
			return digest.digest();
		}
	}

	/**
	 * <p>Given a textual message, returns a byte buffer formatted as follows:</p>
	 *
	 * <tt><p>[24] "Bitcoin Signed Message:\n" [message.length as a varint] message</p></tt>
	 */
	public static byte[] formatMessageForSigning(String message) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			bos.write(BITCOIN_SIGNED_MESSAGE_HEADER_BYTES.length);
			bos.write(BITCOIN_SIGNED_MESSAGE_HEADER_BYTES);
			byte[] messageBytes = message.getBytes(Charsets.UTF_8);
			VarInt size = new VarInt(messageBytes.length);
			bos.write(size.encode());
			bos.write(messageBytes);
			return bos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);  // Cannot happen.
		}
	}

	/**
	 * The regular {@link java.math.BigInteger#toByteArray()} method isn't quite what we often need: it appends a
	 * leading zero to indicate that the number is positive and may need padding.
	 *
	 * @param b the integer to format into a byte array
	 * @param numBytes the desired size of the resulting byte array
	 * @return numBytes byte long array.
	 */
	public static byte[] bigIntegerToBytes(BigInteger b, int numBytes) {
		if (b == null) {
			return null;
		}
		byte[] bytes = new byte[numBytes];
		byte[] biBytes = b.toByteArray();
		int start = (biBytes.length == numBytes + 1) ? 1 : 0;
		int length = Math.min(biBytes.length, numBytes);
		System.arraycopy(biBytes, start, bytes, numBytes - length, length);
		return bytes;        
	}

	/**
	 * Based on the type, this method converts the number of atomic
	 * units to a BigDecimal. 
	 * 
	 * @param type - OWCoin.Type.BTC for bitcoin, etc.
	 * @param numAtomicUnits - The number of atomic units to convert. 
	 * @return The big integer converted to a big decimal
	 */
	public static BigDecimal bigIntToBigDec(OWCurrency type, BigInteger numAtomicUnits) {
		return new BigDecimal(numAtomicUnits, type.getNumberOfDigitsOfPrecision());
	}

	/**
	 * Based on the coin type, this method converts the number BigDecimal units
	 * to a BigInteger (i.e. to the number of atomic units).
	 * 
	 * @param type - OWCoin.Type.BTC for bitcoin, etc.
	 * @param amount - The BigDecimal to convert.
	 * @return The big decimal converted to a big integer
	 */
	public static BigInteger bigDecToBigInt(OWCurrency type, BigDecimal amount) {
		return amount.multiply(new BigDecimal(BigDecimal.TEN.toBigInteger(), 
				type.getNumberOfDigitsOfPrecision())).toBigInteger();
	}

}
