package com.ziftr.android.ziftrwallet.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.spongycastle.crypto.CryptoException;

import android.annotation.SuppressLint;

import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class OWCrypter {

	// 128 is in default android, to get 256 AES encryption you have to use the JCE 
	public static final int KEY_LENGTH = 128;
	public static final int SALT_LENGTH = KEY_LENGTH / 8;
	
	// 100 is pretty low but we want this to be fast on mobile phones
	public static final int PBE_ITERATION_COUNT = 100;

	public static final String HASH_ALGORITHM = "SHA-256";
	public static final String PBE_ALGORITHM = "PBKDF2WithHmacSHA256"; //"PBEWithSHA256And256BitAES";
	
	// TODO use this in the case that the phone doesn't come with 
	public static final String SECONDARY_PBE_ALGORITHM = "PBKDF2WithHmacSHA1";
	public static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
	public static final String SECRET_KEY_ALGORITHM = "AES";

	@SuppressLint("TrulyRandom")
	public static String encrypt(SecretKey secret, String clearText) throws CryptoException {
		try {
			Cipher encryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM);
			
			byte[] iv = generateIv(encryptionCipher.getBlockSize());
			String ivHex = ZiftrUtils.bytesToHexString(iv); 
			IvParameterSpec ivSpec = new IvParameterSpec(iv);

			encryptionCipher.init(Cipher.ENCRYPT_MODE, secret, ivSpec);
			byte[] encryptedText = encryptionCipher.doFinal(clearText.getBytes("UTF-8"));
			String encryptedHex = ZiftrUtils.bytesToHexString(encryptedText);

			// Why are we appending these values?
			return ivHex + encryptedHex;
		} catch (Exception e) {
			System.out.println("error message: " + e.getMessage());
			throw new CryptoException("Unable to encrypt", e);
		}
	}

	public static String decrypt(SecretKey secret, String encrypted) throws CryptoException {
		try {
			Cipher decryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM);
			int ivLength = decryptionCipher.getBlockSize();
			String ivHex = encrypted.substring(0, ivLength * 2);
			String encryptedHex = encrypted.substring(ivLength * 2);
			
			IvParameterSpec ivspec = new IvParameterSpec(ZiftrUtils.hexStringToBytes(ivHex));
			decryptionCipher.init(Cipher.DECRYPT_MODE, secret, ivspec);
			byte[] decryptedText = decryptionCipher.doFinal(ZiftrUtils.hexStringToBytes(encryptedHex));
			String decrypted = new String(decryptedText, "UTF-8");
			return decrypted;
		} catch (Exception e) {
			throw new CryptoException("Unable to decrypt", e);
		}
	}

	public static SecretKey getSecretKey(String password, String salt) throws CryptoException {
		try {
			PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), ZiftrUtils.hexStringToBytes(salt), PBE_ITERATION_COUNT, KEY_LENGTH);
			SecretKeyFactory factory = SecretKeyFactory.getInstance(PBE_ALGORITHM);
			SecretKey tmp = factory.generateSecret(pbeKeySpec);
			SecretKey secret = new SecretKeySpec(tmp.getEncoded(), SECRET_KEY_ALGORITHM);
			return secret;
		} catch (Exception e) {
			throw new CryptoException("Unable to get secret key", e);
		}
	}

	public static String getHash(String password, String salt) throws CryptoException {
		try {
			String input = password + salt;
			MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
			byte[] out = md.digest(input.getBytes("UTF-8"));
			return ZiftrUtils.bytesToHexString(out);
		} catch (Exception e) {
			throw new CryptoException("Unable to get hash", e);
		}
	}

	public static String generateSalt() throws CryptoException {
		try {
			SecureRandom random = new SecureRandom();
			byte[] salt = new byte[SALT_LENGTH];
			random.nextBytes(salt);
			String saltHex = ZiftrUtils.bytesToHexString(salt); 
			return saltHex;
		} catch (Exception e) {
			throw new CryptoException("Unable to generate salt", e);
		}
	}

	private static byte[] generateIv(int ivLength) throws NoSuchAlgorithmException, NoSuchProviderException {
		SecureRandom random = new SecureRandom();
		byte[] iv = new byte[ivLength];
		random.nextBytes(iv);
		return iv;
	}
	
	public static void main(String[] args) {
		try {
			String password = "password";
			String salt = generateSalt();
			
			String unencrypted = "E9873D79C6D87DC0FB6A5778633389F4453213303DA61F20BD67FC233AA33262";
			String encrypted = encrypt(getSecretKey(password, salt), unencrypted);
			String decrypted = decrypt(getSecretKey(password, salt), encrypted);
			
			System.out.println("unencrypted: " + unencrypted);
			System.out.println("encrypted: " + encrypted);
			System.out.println("decrypted: " + decrypted);
		} catch (CryptoException e) {
			System.out.println("error!: " + e.getMessage());
		}
	}

}