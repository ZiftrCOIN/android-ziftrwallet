/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */


package com.ziftr.android.ziftrwallet.crypto;

import javax.crypto.SecretKey;

import com.ziftr.android.ziftrwallet.ZWPreferences;
import com.ziftr.android.ziftrwallet.exceptions.ZWDataEncryptionException;

/**
 * This is mainly just used for private keys in the ziftrWALLET app. 
 * 
 * It has turned into just a wrapper around some encrypted string data, but this class
 * is still useful because it provides for some error checking in that clear text can
 * always be in strings and encrypted text can always be in this class. With this convention
 * we won't ever pass clear text into a decrypt method, or encrypt any data already encrypted.
 */
public class ZWPrivateData {
	
	// Make these out of the hex char range so it's not confusing when looking at contents
	// of database
	private static final char NO_ENCRYPTION = 'G';
	private static final char PBE_AES_ENCRYPTION = 'H';
	
	
	char encryptionIdentifier;
	String rawData;

	
	/**
	 * create a new {@link ZWPrivateData} object from an existing private data string,
	 * @param privateData the private data string to use to create the object, it should have an encryption id as the first character
	 * this string typically comes from the {@link #getStorageString()} method of a previous ZWPrivateData object
	 * @return a new ZWPrivateData object with the encryption status set based on the data passed in, or null if privateData is empty
	 */
	public static ZWPrivateData createFromPrivateDataString(String privateData) {
		if(privateData == null || privateData.isEmpty()) {
			return null;
		}
		
		return new ZWPrivateData(privateData);
	}


	/**
	 * creates a new {@link ZWPrivateData} object from raw unencrypted string data
	 * @param data raw unencrypted data to be part of this private data object
	 * @return a new ZWPrivateData object with encryption status set to not encrypted, or null if data is empty
	 */
	public static ZWPrivateData createFromUnecryptedData(String data) {
		if(data == null || data.isEmpty()) {
			return null;
		}
		
		return new ZWPrivateData(NO_ENCRYPTION, data);
	}

	
    /**
     * @param encryptedData
     */
    private ZWPrivateData(char encrId, String encryptedData) {
    	this.encryptionIdentifier = encrId;
    	this.rawData = encryptedData;
    }
    
    private ZWPrivateData(String dataIncludingEncryptionId) {
    	this(dataIncludingEncryptionId.charAt(0), dataIncludingEncryptionId.substring(1));
    }
    

    @Override
    public ZWPrivateData clone() {
        return new ZWPrivateData(this.encryptionIdentifier, this.rawData);
    }

    @Override
    public int hashCode() {
        return this.rawData.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        
        final ZWPrivateData other = (ZWPrivateData) obj;
        return this.encryptionIdentifier == other.encryptionIdentifier && this.rawData.equals(other.rawData);
    }
    
    
    /**
     * gets the data from this object (not including encryption identifier)
     * @return the raw data (which may or may not be encrypted) as a String
     */
    public String getDataString() {
    	return this.rawData;
    }
    
    
    /**
     * gets the data from this object and include an encryption identifier byte
     * note the returned string cannot be decrypted alone, but is meant for storing or recreating
     * this data object
     * @return a String representing this private data object
     */
    public String getStorageString() {
    	return this.encryptionIdentifier + this.getDataString();
    }

    @Override
    public String toString() {
        return this.getStorageString();
    }

    
    public boolean isEncrypted() {
    	if(this.encryptionIdentifier == PBE_AES_ENCRYPTION) {
    		return true;
    	}
    	
    	return false;
    }
    
    
    public synchronized void encrypt(SecretKey secretKey) throws ZWKeyCrypterException, ZWDataEncryptionException {

    	if(secretKey == null) {
    		//no password, no encryption, so we're done
			return;
    	}
    	
    	if(this.isEncrypted()) {
			throw new ZWDataEncryptionException("Data is already encrypted.");
		}
    	
    	String encryptedData = ZWPbeAesCrypterUtils.encrypt(secretKey, this.rawData);
		this.rawData = encryptedData;
		this.encryptionIdentifier = PBE_AES_ENCRYPTION;
		
    }
    
	public synchronized void encrypt(String password) throws ZWKeyCrypterException, ZWDataEncryptionException {
		
		SecretKey secretKey = null;
		if(password != null && password.length() > 0) {
			secretKey = ZWPbeAesCrypterUtils.generateSecretKey(password, ZWPreferences.getSalt());
		}
		
		this.encrypt(secretKey);
	}
    
	
	public synchronized ZWPrivateData decrypt(SecretKey secretKey) throws ZWDataEncryptionException {

		if(secretKey == null) {
			//if there's no secret key passed in, assume it's not encrypted
			//if this is wrong, it's the same as having passed in the wrong password
			return this;
		}
		
		if(!this.isEncrypted()) {
    		throw new ZWDataEncryptionException("Trying to decrypt unecrypted data.");
    	}
    		
		String decryptedData = ZWPbeAesCrypterUtils.decrypt(secretKey, this.rawData);
		this.rawData = decryptedData;
		this.encryptionIdentifier = NO_ENCRYPTION;
		
    	return this;
	}
    
	
	/**
	 * Decrypts this data object so that it's data string can be read
	 * @param password the password that was used originally to encrypt this data
	 * @return this {@link ZWPrivateData} instance, so it can easily be chained with data reading
	 * @throws ZWDataEncryptionException
	 */
    public synchronized ZWPrivateData decrypt(String password) throws ZWDataEncryptionException {
    	SecretKey secretKey = null;
    	if(password != null && password.length() > 0) {
    		secretKey = ZWPbeAesCrypterUtils.generateSecretKey(password, ZWPreferences.getSalt());
    	}
    	
    	return this.decrypt(secretKey);
    }
    
}



