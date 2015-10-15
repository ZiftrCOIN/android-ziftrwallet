/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.crypto;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import android.annotation.SuppressLint;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWApplication;
import com.ziftr.android.ziftrwallet.dialog.ZiftrDialogManager;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWPbeAesCrypterUtils {

	// 128 is in default android, to get 256 AES encryption you have to use the JCE 
	public static final int KEY_LENGTH = 128;
	public static final int SALT_LENGTH = KEY_LENGTH / 8;

	// 100 is pretty low but we want this to be fast on mobile phones
	public static final int PBE_ITERATION_COUNT = 100;

	public static final String HASH_ALGORITHM = "SHA-256";
	public static final String PBE_ALGORITHM = "PBKDF2WithHmacSHA1"; //"PBKDF2WithHmacSHA256";

	// TODO use this in the case that the phone doesn't come with 
	public static final String SECONDARY_PBE_ALGORITHM = "PBKDF2WithHmacSHA1";

	public static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
	public static final String SECRET_KEY_ALGORITHM = "AES";
	
	
	private ZWPbeAesCrypterUtils(SecretKey secretKey) {
		//kept prive so this is just a static utility method class
	}

	
	@SuppressLint("TrulyRandom")
	public static String encrypt(SecretKey secretKey, String data) throws ZWKeyCrypterException {
		try {
			Cipher encryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM);

			byte[] iv = generateIv(encryptionCipher.getBlockSize());
			String ivHex = ZiftrUtils.bytesToHexString(iv); 
			IvParameterSpec ivSpec = new IvParameterSpec(iv);

			encryptionCipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
			byte[] encryptedText = encryptionCipher.doFinal(data.getBytes("UTF-8"));
			String encryptedHex = ZiftrUtils.bytesToHexString(encryptedText);

			// Why are we appending these values?
			// AES requires a random initialization vector (IV). We save the IV
			// with the encrypted value so we can get it back later in decrypt()
			return ivHex + encryptedHex;
		} 
		catch (Exception e) {
			System.out.println("error message: " + e.getMessage());
			throw new ZWKeyCrypterException("Unable to encrypt", e);
		}
	}


	public static String decrypt(SecretKey secretKey, String encryptedData) throws ZWKeyCrypterException {
		try {
			
			Cipher decryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM);
			int ivLength = decryptionCipher.getBlockSize();
			String ivHex = encryptedData.substring(0, ivLength * 2);
			String encryptedHex = encryptedData.substring(ivLength * 2);

			IvParameterSpec ivspec = new IvParameterSpec(ZiftrUtils.hexStringToBytes(ivHex));
			decryptionCipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);
			byte[] decryptedText = decryptionCipher.doFinal(ZiftrUtils.hexStringToBytes(encryptedHex));
			String decrypted = new String(decryptedText, "UTF-8");
			return decrypted;
		} catch (Exception e) {
			throw new ZWKeyCrypterException("Unable to decrypt", e);
		}
	}


	public static byte[] decryptToBytes(SecretKey secretKey, String encryptedData) throws ZWKeyCrypterException {
		return ZiftrUtils.hexStringToBytes(decrypt(secretKey, encryptedData));
	}

	public static SecretKey generateSecretKey(String password, String salt) throws ZWKeyCrypterException {
		
		if(password == null || password.length() == 0) {
			return null;
		}
		if(salt == null || salt.length() == 0) {
			throw new ZWKeyCrypterException("Cannot create SecretKey without valid salt.");
		}
		
		try {
			PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), ZiftrUtils.hexStringToBytes(salt), PBE_ITERATION_COUNT, KEY_LENGTH);
			SecretKeyFactory factory = SecretKeyFactory.getInstance(PBE_ALGORITHM);
			SecretKey tmp = factory.generateSecret(pbeKeySpec);
			SecretKey secret = new SecretKeySpec(tmp.getEncoded(), SECRET_KEY_ALGORITHM);
			return secret;
		} 
		catch (Exception e) {
			throw new ZWKeyCrypterException("Unable to get secret key", e);
		}
	}

	public static String generateSalt() throws ZWKeyCrypterException {
		try {
			SecureRandom random = ZiftrUtils.createTrulySecureRandom();
			if(random == null) {
				String rngError = ZWApplication.getApplication().getString(R.string.zw_dialog_error_rng);
				ZiftrDialogManager.showSimpleAlert(rngError);
			}

			byte[] salt = new byte[SALT_LENGTH];
			random.nextBytes(salt);
			String saltHex = ZiftrUtils.bytesToHexString(salt); 
			try {
				ZLog.log("salt: " + salt);
			} catch (Exception e) {
				System.out.println("salt: " + saltHex);
			}
			return saltHex;
		} catch (Exception e) {
			throw new ZWKeyCrypterException("Unable to generate salt", e);
		}
	}

	public static byte[] generateIv(int ivLength) {
		SecureRandom random = ZiftrUtils.createTrulySecureRandom();

		if (random == null) {
			String rngError = ZWApplication.getApplication().getString(R.string.zw_dialog_error_rng);
			ZiftrDialogManager.showSimpleAlert(rngError);
		}

		byte[] iv = new byte[ivLength];
		random.nextBytes(iv);
		return iv;
	}

}




