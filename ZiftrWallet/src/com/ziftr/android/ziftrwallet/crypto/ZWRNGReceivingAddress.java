package com.ziftr.android.ziftrwallet.crypto;

import java.math.BigInteger;

import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.exceptions.ZWDataEncryptionException;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWRNGReceivingAddress extends ZWReceivingAddress {

	public ZWRNGReceivingAddress(ZWCoin coin, ZWPublicKey publicKey, ZWPrivateData privateKeyData, boolean change) throws ZWAddressFormatException {
		
		super(coin, publicKey, change);
		
		// TODO Auto-generated constructor stub
	}
	
	
	
	
	public ZWPrivateKey getPrivateKey(String password) throws ZWDataEncryptionException {
		
			//if encrypted legacy key
		if(privateKeyData.isEncrypted()) {
			privateKeyData.decrypt(password);
		}
		byte[] privBytes = ZiftrUtils.hexStringToBytes(privateKeyData.getDataString());
		ZWPrivateKey privateKey = new ZWPrivateKey(new BigInteger(1, privBytes), this.publicKey);
		
		return privateKey;
	
		
		
		//addrRet = new ZWReceivingAddress(this.coin, newKey, hidden != 0);
		
		
		//else if unencrypted legacy
		//...?
		
		
		//else if hd
	
	/***
		String hdSeed = getDecryptedHdSeed(password);
		if (hdSeed == null) {
			return null;
		}
		byte[] seed = ZiftrUtils.hexStringToBytes(hdSeed);
		ZWExtendedPrivateKey xprvkey = new ZWExtendedPrivateKey(seed);
		addr.deriveFromStoredPath(xprvkey);
		
		
		
		return (ZWPrivateKey) this.priv;
		***/
	}
	

}
