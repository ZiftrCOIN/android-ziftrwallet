package com.ziftr.android.ziftrwallet.util;

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

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.google.common.base.Charsets;
import com.google.common.primitives.UnsignedLongs;
import com.ziftr.android.ziftrwallet.OWPreferencesUtils;
import com.ziftr.android.ziftrwallet.crypto.OWVarInt;

public class ZiftrUtils {

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
	public static String bytesToHexString(byte[] data) {
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
	public static byte[] hexStringToBytes(String hexStr) {
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
	}
	
	/**
	 * Calculates RIPEMD160(SHA256(input)). This is used in Address calculations.
	 * TODO change name order, as it is confusing
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
	 * @param newPassphrase
	 * @return
	 */
	public static byte[] saltedHash(Context c, String newPassphrase) {
		return saltedHash(OWPreferencesUtils.getSalt(c), newPassphrase);
	}
	
	public static byte[] saltedHash(String salt, String newPassphrase) {
		byte[] newPassphraseBytes = newPassphrase.getBytes();
		byte[] concat = new byte[32 + newPassphrase.getBytes().length];
		System.arraycopy(ZiftrUtils.Sha256Hash(salt.getBytes()), 0, concat, 0, 32);
		System.arraycopy(newPassphraseBytes, 0, concat, 32, newPassphraseBytes.length);
		return ZiftrUtils.Sha256Hash(concat);
	}
	
	/**
	 * @param newPassphrase
	 * @return
	 */
	public static String saltedHashString(Context c, String newPassphrase) {
		return ZiftrUtils.bytesToHexString(saltedHash(c, newPassphrase));
	}

	/**
	 * See Utils#doubleDigest(byte[], int, int).
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
	 * 
	 * TODO this is only for bitcoin right now, need to make it general
	 */
	public static byte[] formatMessageForSigning(OWCoin coinId, String message) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] magicBytes = coinId.getSigningMessageMagic().getBytes(Charsets.UTF_8);
			bos.write(magicBytes.length);
			bos.write(magicBytes);
			byte[] messageBytes = message.getBytes(Charsets.UTF_8);
			OWVarInt size = new OWVarInt(messageBytes.length);
			bos.write(size.encode());
			bos.write(messageBytes);
			return bos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);  // Cannot happen.
		}
	}
	
	/**
     * Work around lack of unsigned types in Java.
     */
    public static boolean isLessThanUnsigned(long n1, long n2) {
        return UnsignedLongs.compare(n1, n2) < 0;
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
	 * @param type - OWCoin.BTC for bitcoin, etc.
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
	 * @param type - OWCoin.BTC for bitcoin, etc.
	 * @param amount - The BigDecimal to convert.
	 * @return The big decimal converted to a big integer
	 */
	public static BigInteger bigDecToBigInt(OWCurrency type, BigDecimal amount) {
		return amount.multiply(new BigDecimal(BigInteger.TEN, 
				type.getNumberOfDigitsOfPrecision())).toBigInteger();
	}
	
	/**
	 * Decoded bytes sometimes have format (when for uncompressed private keys):
	 *   version byte + data
	 * Other times they have the format (when for compressed private keys):
	 *   version byte + data + 0x01
	 * 
	 * When getting back the the private key bytes all we want is the middle data section.
	 * Use this method to get just that middle data section. 
	 * 
	 * @param wifPrivBytes
	 * @return
	 */
	public static byte[] stripVersionAndChecksum(byte[] wifPrivBytes) {
		byte[] privBytes = new byte[32];
		System.arraycopy(wifPrivBytes, 1, privBytes, 0, 32);
		return privBytes;
	}
	
	public static long readUint32(byte[] bytes, int offset) {
        return ((bytes[offset++] & 0xFFL) << 0) |
                ((bytes[offset++] & 0xFFL) << 8) |
                ((bytes[offset++] & 0xFFL) << 16) |
                ((bytes[offset] & 0xFFL) << 24);
    }
    
    public static long readInt64(byte[] bytes, int offset) {
        return ((bytes[offset++] & 0xFFL) << 0) |
               ((bytes[offset++] & 0xFFL) << 8) |
               ((bytes[offset++] & 0xFFL) << 16) |
               ((bytes[offset++] & 0xFFL) << 24) |
               ((bytes[offset++] & 0xFFL) << 32) |
               ((bytes[offset++] & 0xFFL) << 40) |
               ((bytes[offset++] & 0xFFL) << 48) |
               ((bytes[offset] & 0xFFL) << 56);
    }

    public static long readUint32BE(byte[] bytes, int offset) {
        return ((bytes[offset + 0] & 0xFFL) << 24) |
                ((bytes[offset + 1] & 0xFFL) << 16) |
                ((bytes[offset + 2] & 0xFFL) << 8) |
                ((bytes[offset + 3] & 0xFFL) << 0);
    }

    public static int readUint16BE(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xff) << 8) | bytes[offset + 1] & 0xff;
    }
    
    public static void uint32ToByteArrayBE(long val, byte[] out, int offset) {
        out[offset + 0] = (byte) (0xFF & (val >> 24));
        out[offset + 1] = (byte) (0xFF & (val >> 16));
        out[offset + 2] = (byte) (0xFF & (val >> 8));
        out[offset + 3] = (byte) (0xFF & (val >> 0));
    }

    public static void uint32ToByteArrayLE(long val, byte[] out, int offset) {
        out[offset + 0] = (byte) (0xFF & (val >> 0));
        out[offset + 1] = (byte) (0xFF & (val >> 8));
        out[offset + 2] = (byte) (0xFF & (val >> 16));
        out[offset + 3] = (byte) (0xFF & (val >> 24));
    }

    public static void uint64ToByteArrayLE(long val, byte[] out, int offset) {
        out[offset + 0] = (byte) (0xFF & (val >> 0));
        out[offset + 1] = (byte) (0xFF & (val >> 8));
        out[offset + 2] = (byte) (0xFF & (val >> 16));
        out[offset + 3] = (byte) (0xFF & (val >> 24));
        out[offset + 4] = (byte) (0xFF & (val >> 32));
        out[offset + 5] = (byte) (0xFF & (val >> 40));
        out[offset + 6] = (byte) (0xFF & (val >> 48));
        out[offset + 7] = (byte) (0xFF & (val >> 56));
    }

}
