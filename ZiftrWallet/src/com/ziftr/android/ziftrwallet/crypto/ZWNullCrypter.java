package com.ziftr.android.ziftrwallet.crypto;

import javax.crypto.SecretKey;

import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWNullCrypter implements ZWKeyCrypter {

	@Override
	public ZWEncryptedData encrypt(String clearText)
			throws ZWKeyCrypterException {
		return new ZWEncryptedData(ZWKeyCrypter.NO_ENCRYPTION, clearText);
	}

	@Override
	public String decrypt(ZWEncryptedData encryptedData)
			throws ZWKeyCrypterException {
		if (encryptedData.encryptionId != ZWKeyCrypter.NO_ENCRYPTION){
			throw new ZWKeyCrypterException("Cannot decrypt");
		}
		return encryptedData.getEncryptedData();
		
	}

	@Override
	public byte[] decryptToBytes(ZWEncryptedData encryptedData)
			throws ZWKeyCrypterException {
		return ZiftrUtils.hexStringToBytes(decrypt(encryptedData));
	}

	@Override
	public void setSecretKey(SecretKey secretKey) {
		//no secret key
		return;
	}

	@Override
	public SecretKey getSecretKey() {
		//no secret key
		return null;
	}

	@Override
	public char getEncryptionIdentifier() {
		return ZWKeyCrypter.NO_ENCRYPTION;
	}

}
