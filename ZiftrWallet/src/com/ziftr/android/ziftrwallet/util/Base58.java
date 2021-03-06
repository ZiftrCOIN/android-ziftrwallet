/**
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */


package com.ziftr.android.ziftrwallet.util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Arrays;

import com.ziftr.android.ziftrwallet.crypto.ZWECKey;
import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;

/**
 * <p>Base58 is a way to encode Bitcoin addresses as numbers and letters. Note that this is not the same base58 as used by
 * Flickr, which you may see reference to around the internet.</p>
 *
 * <p>You may instead wish to work with VersionedChecksummedBytes, which adds support for testing the prefix
 * and suffix bytes commonly found in addresses.</p>
 *
 * <p>Satoshi says: why base-58 instead of standard base-64 encoding?<p>
 *
 * <ul>
 * <li>Don't want 0OIl characters that look the same in some fonts and
 *     could be used to create visually identical looking account numbers.</li>
 * <li>A string with non-alphanumeric characters is not as easily accepted as an account number.</li>
 * <li>E-mail usually won't line-break if there's no punctuation to break at.</li>
 * <li>Doubleclicking selects the whole number as one word if it's all alphanumeric.</li>
 * </ul>
 */
public class Base58 {
    public static final char[] ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();

    private static final int[] INDEXES = new int[128];
    static {
        for (int i = 0; i < INDEXES.length; i++) {
            INDEXES[i] = -1;
        }
        for (int i = 0; i < ALPHABET.length; i++) {
            INDEXES[ALPHABET[i]] = i;
        }
    }
    /**
     * Encodes the given bytes in base58. Checksum is appended.
     */
    public static String encode(byte version, byte[] data) {
        // A stringified buffer is:
        //   1 byte version + data bytes + 4 bytes check code (a truncated hash)
        byte[] addressBytes = new byte[1 + data.length + 4];
        addressBytes[0] = version;
        System.arraycopy(data, 0, addressBytes, 1, data.length);
        byte[] check = ZiftrUtils.doubleDigest(addressBytes, 0, data.length + 1);
        System.arraycopy(check, 0, addressBytes, data.length + 1, 4);
        return encode(addressBytes);
    }

    /** 
     * Encodes the given bytes in base58. Checksum is not appended. 
     */
    public static String encode(byte[] input) {
        if (input.length == 0) {
            return "";
        }       
        input = copyOfRange(input, 0, input.length);
        // Count leading zeroes.
        int zeroCount = 0;
        while (zeroCount < input.length && input[zeroCount] == 0) {
            ++zeroCount;
        }
        // The actual encoding.
        byte[] temp = new byte[input.length * 2];
        int j = temp.length;

        int startAt = zeroCount;
        while (startAt < input.length) {
            byte mod = divmod58(input, startAt);
            if (input[startAt] == 0) {
                ++startAt;
            }
            temp[--j] = (byte) ALPHABET[mod];
        }

        // Strip extra '1' if there are some after decoding.
        while (j < temp.length && temp[j] == ALPHABET[0]) {
            ++j;
        }
        // Add as many leading '1' as there were leading zeros.
        while (--zeroCount >= 0) {
            temp[--j] = (byte) ALPHABET[0];
        }

        byte[] output = copyOfRange(temp, j, temp.length);
        try {
            return new String(output, "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    public static byte[] decode(String input) throws ZWAddressFormatException {
        if (input.length() == 0) {
            return new byte[0];
        }
        byte[] input58 = new byte[input.length()];
        // Transform the String to a base58 byte sequence
        for (int i = 0; i < input.length(); ++i) {
            char c = input.charAt(i);

            int digit58 = -1;
            if (c >= 0 && c < 128) {
                digit58 = INDEXES[c];
            }
            if (digit58 < 0) {
                throw new ZWAddressFormatException("Illegal character " + c + " at " + i);
            }

            input58[i] = (byte) digit58;
        }
        // Count leading zeroes
        int zeroCount = 0;
        while (zeroCount < input58.length && input58[zeroCount] == 0) {
            ++zeroCount;
        }
        // The encoding
        byte[] temp = new byte[input.length()];
        int j = temp.length;

        int startAt = zeroCount;
        while (startAt < input58.length) {
            byte mod = divmod256(input58, startAt);
            if (input58[startAt] == 0) {
                ++startAt;
            }

            temp[--j] = mod;
        }
        // Do no add extra leading zeroes, move j to first non null byte.
        while (j < temp.length && temp[j] == 0) {
            ++j;
        }
        return copyOfRange(temp, j - zeroCount, temp.length);
    }
    
    public static BigInteger decodeToBigInteger(String input) throws ZWAddressFormatException {
        return new BigInteger(1, decode(input));
    }

    /**
     * Uses the checksum in the last 4 bytes of the decoded data to verify the rest are correct. The checksum is
     * removed from the returned data.
     *
     * @throws ZWAddressFormatException if the input is not base 58 or the checksum does not validate.
     */
    public static byte[] decodeChecked(String input) throws ZWAddressFormatException {
        byte tmp [] = decode(input);
        if (tmp.length < 4)
            throw new ZWAddressFormatException("Input too short");
        byte[] bytes = copyOfRange(tmp, 0, tmp.length - 4);
        byte[] checksum = copyOfRange(tmp, tmp.length - 4, tmp.length);
        
        tmp = ZiftrUtils.doubleDigest(bytes);
        byte[] hash = copyOfRange(tmp, 0, 4);
        if (!Arrays.equals(checksum, hash)) 
            throw new ZWAddressFormatException("Checksum does not validate");
        
        return bytes;
    }
    
    //
    // number -> number / 58, returns number % 58
    //
    private static byte divmod58(byte[] number, int startAt) {
        int remainder = 0;
        for (int i = startAt; i < number.length; i++) {
            int digit256 = (int) number[i] & 0xFF;
            int temp = remainder * 256 + digit256;

            number[i] = (byte) (temp / 58);

            remainder = temp % 58;
        }

        return (byte) remainder;
    }

    //
    // number -> number / 256, returns number % 256
    //
    private static byte divmod256(byte[] number58, int startAt) {
        int remainder = 0;
        for (int i = startAt; i < number58.length; i++) {
            int digit58 = (int) number58[i] & 0xFF;
            int temp = remainder * 58 + digit58;
            number58[i] = (byte) (temp / 256);

            remainder = temp % 256;
        }

        return (byte) remainder;
    }

    private static byte[] copyOfRange(byte[] source, int from, int to) {
        byte[] range = new byte[to - from];
        System.arraycopy(source, from, range, 0, range.length);

        return range;
    }
    
    public static void main(String[] args) {
    	try {
    		String hexTxData = "0100000001eccf7e3034189b851985d871f91384b8ee357cd47c3024736e5676eb2debb3f2010000001976a914010966776006953d5567439e5e39f86a0d273bee88acffffffff01605af405000000001976a914097072524438d003d23a2f23edb65aae1bb3e46988ac0000000001000000";
    		byte[] txData = ZiftrUtils.hexStringToBytes(hexTxData);
    		System.out.println("double hash: " + ZiftrUtils.bytesToHexString(ZiftrUtils.Sha256Hash(ZiftrUtils.Sha256Hash(txData))));
    		
    		
    		System.out.println("\n\n\n");
    		
    		
    		BigInteger bi = new BigInteger(1, ZiftrUtils.hexStringToBytes("9fbdeae6e1d6e3d2e9c9a49fc32d87d439dff8be9b677c19fa1617e5180bc1b3"));
    		System.out.println(bi.toString());
    		System.out.println(ZiftrUtils.bytesToHexString(bi.toByteArray()));
    		System.out.println(ZiftrUtils.bytesToHexString(ZiftrUtils.bigIntegerToBytes(bi, 32)));
    		ZWECKey key = new ZWECKey(bi);
    		String encoded = encode((byte) 128, key.getPrivKeyBytesForAddressEncoding());
    		System.out.println("priv: " + ZiftrUtils.bytesToHexString(key.getPrivKeyBytes()));
			System.out.println("encoded: " + encoded);
			byte[] x = ZiftrUtils.stripVersionAndChecksum(decodeChecked(encoded), 32);
			System.out.println("decoded: " + (x.length) + "  " + ZiftrUtils.bytesToHexString(x));
		} catch (ZWAddressFormatException e) {
			e.printStackTrace();
		}
    }
}
