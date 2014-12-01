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

	String encryptedData;

    /**
     * Cloning constructor.
     * @param encryptedPrivateKey EncryptedPrivateKey to clone.
     */
    public ZWEncryptedData(ZWEncryptedData encryptedPrivateKey) {
    	this.encryptedData = encryptedPrivateKey.encryptedData;
    }

    /**
     * @param iv
     * @param encryptedPrivateKeys
     */
    public ZWEncryptedData(String encryptedData) {
    	this.encryptedData = encryptedData;
    }

    @Override
    public ZWEncryptedData clone() {
        return new ZWEncryptedData(this.encryptedData);
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

        return this.encryptedData.equals(other.encryptedData);
    }
    
    public String getEncryptedData() {
    	return this.encryptedData;
    }

    @Override
    public String toString() {
        return this.getEncryptedData();
    }
    
    /**
     * Clears all the encrypted contents from memory (overwriting all data).
     * WARNING - this method irreversibly deletes the encrypted information.
     */
    public void clear() {
        this.encryptedData = "";
    }
}
