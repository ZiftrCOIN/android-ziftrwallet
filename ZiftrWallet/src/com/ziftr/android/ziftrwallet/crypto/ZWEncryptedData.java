/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */


package com.ziftr.android.ziftrwallet.crypto;

/**
 * This is mainly just used for private keys in the ziftrWALLET app. 
 * 
 * It has turned into just a wrapper around some encrypted string data, but this class
 * is still useful because it provides for some error checking in that clear text can
 * always be in strings and encrypted text can always be in this class. With this convention
 * we won't ever pass clear text into a decrypt method, or encrypt any data already encrypted.
 */
public class ZWEncryptedData {
	
	char encryptionId;
	String encryptedData;

    /**
     * Cloning constructor.
     * @param encryptedPrivateKey EncryptedPrivateKey to clone.
     */
    public ZWEncryptedData(ZWEncryptedData other) {
    	this.encryptionId = other.encryptionId;
    	this.encryptedData = other.encryptedData;
    }

    /**
     * @param encryptedData
     */
    public ZWEncryptedData(char encrId, String encryptedData) {
    	this.encryptionId = encrId;
    	this.encryptedData = encryptedData;
    }

    @Override
    public ZWEncryptedData clone() {
        return new ZWEncryptedData(this.encryptionId, this.encryptedData);
    }

    @Override
    public int hashCode() {
        return this.encryptedData.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        
        final ZWEncryptedData other = (ZWEncryptedData) obj;
        return this.encryptionId == other.encryptionId && this.encryptedData.equals(other.encryptedData);
    }
    
    public String getEncryptedData() {
    	return this.encryptedData;
    }
    
    public String toStringWithEncryptionId() {
    	return "" + this.encryptionId + this.getEncryptedData();
    }

    @Override
    public String toString() {
        return this.toStringWithEncryptionId();
    }
    
    /**
     * Clears all the encrypted contents from memory (overwriting all data).
     * WARNING - this method irreversibly deletes the encrypted information.
     */
    public void clear() {
    	this.encryptionId = ZWKeyCrypter.NO_ENCRYPTION;
        this.encryptedData = "";
    }
}
