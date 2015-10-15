package com.ziftr.android.ziftrwallet.crypto;

import java.math.BigInteger;

import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.exceptions.ZWDataEncryptionException;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWRNGReceivingAddress extends ZWReceivingAddress {
	
	
	public ZWRNGReceivingAddress(ZWCoin coin, ZWPublicKey publicKey, ZWPrivateData privateKeyData, boolean change) throws ZWAddressFormatException {
		super(coin, publicKey, change);
		this.privateKeyData = privateKeyData;
	}

	public ZWPrivateKey getPrivateKey(String password) throws ZWDataEncryptionException {
		
		//if encrypted legacy key
		if(privateKeyData.isEncrypted()) {
			privateKeyData.decrypt(password);
		}
		byte[] privBytes = ZiftrUtils.hexStringToBytes(privateKeyData.getDataString());
		ZWPrivateKey privateKey = new ZWPrivateKey(new BigInteger(1, privBytes), this.getPublicKey());
		
		return privateKey;
	
	}
	

}
