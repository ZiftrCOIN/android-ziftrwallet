/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.crypto;

import javax.crypto.SecretKey;

/**
 * An interface so that we can use different en/decryption methods.
 */
public interface ZWKeyCrypter {
	
	// Make these out of the hex char range so it's not confusing when looking at contents
	// of database
	public static final char NO_ENCRYPTION = 'G';
	public static final char PBE_AES_ENCRYPTION = 'H';
	
	/**
	 * Get the type of encryption this crypter supports.
	 * 
	 * @return
	 */
	public char getEncryptionIdentifier();
	
	/**
	 * Use the key crypter to encrypt a given clear text message.
	 * 
	 * @param clearText - the message to encrypt
	 * @return
	 * @throws ZWKeyCrypterException
	 */
	public ZWEncryptedData encrypt(String clearText) throws ZWKeyCrypterException;
	
	/**
	 * Decrypt a
	 * @param encryptedData
	 * @return
	 * @throws ZWKeyCrypterException
	 */
    public String decrypt(ZWEncryptedData encryptedData) throws ZWKeyCrypterException;
    
    /**
     * It may not be clear if the decryption should use hex string to bytes,
     * or the string.toBytes() method. We have a helper method here so implementing
     * classes can decide. 
     * 
     * @param secret
     * @param encrypted
     * @return
     * @throws ZWKeyCrypterException
     */
    public byte[] decryptToBytes(ZWEncryptedData encryptedData) throws ZWKeyCrypterException;

    public void setSecretKey(SecretKey secretKey);
    
    public SecretKey getSecretKey();
    
}
