package com.ziftr.android.ziftrwallet.crypto;

import javax.crypto.SecretKey;

/**
 * An interface so that we can use different en/decryption methods.
 */
public interface OWKeyCrypter {
	
	// Make these out of the hex char range so it's not confusing when looking at contents
	// of database
	public static final char NO_ENCRYPTION = 'G';
	public static final char PBE_AES_ENCRYPTION = 'H';
	
	/**
	 * Use the key crypter to encrypt a given clear text message.
	 * 
	 * @param clearText - the message to encrypt
	 * @return
	 * @throws OWKeyCrypterException
	 */
	public OWEncryptedData encrypt(String clearText) throws OWKeyCrypterException;
	
	/**
	 * Decrypt a
	 * @param encryptedData
	 * @return
	 * @throws OWKeyCrypterException
	 */
    public String decrypt(OWEncryptedData encryptedData) throws OWKeyCrypterException;
    
    /**
     * It may not be clear if the decryption should use hex string to bytes,
     * or the string.toBytes() method. We have a helper method here so implementing
     * classes can decide. 
     * 
     * @param secret
     * @param encrypted
     * @return
     * @throws OWKeyCrypterException
     */
    public byte[] decryptToBytes(OWEncryptedData encryptedData) throws OWKeyCrypterException;

    public void setSecretKey(SecretKey secretKey);
    
    public SecretKey getSecretKey();
    
}
