package com.ziftr.android.ziftrwallet.crypto;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import android.annotation.SuppressLint;

import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWPbeAesCrypter implements ZWKeyCrypter {

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

	private SecretKey secretKey;

	public ZWPbeAesCrypter(SecretKey secretKey) {
		this.setSecretKey(secretKey);
	}

	@SuppressLint("TrulyRandom")
	@Override
	public ZWEncryptedData encrypt(String clearText) throws ZWKeyCrypterException {
		try {
			Cipher encryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM);

			byte[] iv = generateIv(encryptionCipher.getBlockSize());
			String ivHex = ZiftrUtils.bytesToHexString(iv); 
			IvParameterSpec ivSpec = new IvParameterSpec(iv);

			encryptionCipher.init(Cipher.ENCRYPT_MODE, this.secretKey, ivSpec);
			byte[] encryptedText = encryptionCipher.doFinal(clearText.getBytes("UTF-8"));
			String encryptedHex = ZiftrUtils.bytesToHexString(encryptedText);

			// Why are we appending these values?
			// AES requires a random initialization vector (IV). We save the IV
			// with the encrypted value so we can get it back later in decrypt()
			return new ZWEncryptedData(ivHex + encryptedHex);
		} catch (Exception e) {
			System.out.println("error message: " + e.getMessage());
			throw new ZWKeyCrypterException("Unable to encrypt", e);
		}
	}


	@Override
	public String decrypt(ZWEncryptedData encryptedData) throws ZWKeyCrypterException {
		try {
			String encrypted = encryptedData.getEncryptedData();
			Cipher decryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM);
			int ivLength = decryptionCipher.getBlockSize();
			String ivHex = encrypted.substring(0, ivLength * 2);
			String encryptedHex = encrypted.substring(ivLength * 2);

			IvParameterSpec ivspec = new IvParameterSpec(ZiftrUtils.hexStringToBytes(ivHex));
			decryptionCipher.init(Cipher.DECRYPT_MODE, this.secretKey, ivspec);
			byte[] decryptedText = decryptionCipher.doFinal(ZiftrUtils.hexStringToBytes(encryptedHex));
			String decrypted = new String(decryptedText, "UTF-8");
			return decrypted;
		} catch (Exception e) {
			throw new ZWKeyCrypterException("Unable to decrypt", e);
		}
	}

	@Override
	public byte[] decryptToBytes(ZWEncryptedData encryptedData) throws ZWKeyCrypterException {
		return ZiftrUtils.hexStringToBytes(decrypt(encryptedData));
	}

	@Override
	public SecretKey getSecretKey() {
		return this.secretKey;
	}

	@Override
	public void setSecretKey(SecretKey secretKey) {
		this.secretKey = secretKey;
	}

	public static SecretKey generateSecretKey(String password, String salt) throws ZWKeyCrypterException {
		try {
			PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), ZiftrUtils.hexStringToBytes(salt), PBE_ITERATION_COUNT, KEY_LENGTH);
			SecretKeyFactory factory = SecretKeyFactory.getInstance(PBE_ALGORITHM);
			SecretKey tmp = factory.generateSecret(pbeKeySpec);
			SecretKey secret = new SecretKeySpec(tmp.getEncoded(), SECRET_KEY_ALGORITHM);
			return secret;
		} catch (Exception e) {
			throw new ZWKeyCrypterException("Unable to get secret key", e);
		}
	}

	@SuppressLint("TrulyRandom") // Have applied the fix
	public static String generateSalt() throws ZWKeyCrypterException {
		try {
			SecureRandom random = new SecureRandom();
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

	private static byte[] generateIv(int ivLength) {
		SecureRandom random = new SecureRandom();
		byte[] iv = new byte[ivLength];
		random.nextBytes(iv);
		return iv;
	}

	public static void main(String[] args) {
		try {
			String password = "passwwword";
			String salt = generateSalt();
			SecretKey secKey = generateSecretKey(password, salt);
			ZWKeyCrypter c = new ZWPbeAesCrypter(secKey);

			String unencrypted = "E9873D79C6D87DC0FB6A5778633389F4453213303DA61F20BD67FC233AA33262";
			ZWEncryptedData encrypted = c.encrypt(unencrypted);
			String decrypted = c.decrypt(encrypted);

			System.out.println("unencrypted: " + unencrypted);
			System.out.println("encrypted: " + encrypted);
			System.out.println("decrypted: " + decrypted);
			/*
			System.out.println("\n\n\n");

			password = "j";
			String encryptedData = "6efeed1cab1d6eb550733eff3024b7a60f8b92cacd2d32995a20e28bdf2a02d28005bcd530219a3a79856e77907210cab513456658cd060f6d1336822e0ec42b4da80dff13b9be896c654322af3a197da503e155ed78cdc71a0f66236e2ec19a";
			String decrypted = ;
			 */

		} catch (ZWKeyCrypterException e) {
			System.out.println("error!: " + e.getMessage());
		}
	}

}