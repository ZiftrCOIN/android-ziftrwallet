package com.ziftr.android.ziftrwallet.crypto;

import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.exceptions.ZWDataEncryptionException;
import com.ziftr.android.ziftrwallet.sqlite.ZWWalletManager;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWHDReceivingAddress extends ZWReceivingAddress {
	
	private Integer hdAccount;
	private Integer hdIndex;
	private String path;

	
	public ZWHDReceivingAddress(ZWCoin coin, ZWPublicKey publicKey, boolean change, Integer hdAccount,Integer hdIndex) throws ZWAddressFormatException {
		super(coin, publicKey, change);
		
		this.hdAccount = hdAccount;
		this.hdIndex = hdIndex;
		this.path = "[m/44'/" + coin.getHdId() + "'/" + hdAccount + "']/" + change + "/" + hdIndex;
	}
	

	
	public Integer getHdAccount() {
		return this.hdAccount;
	}
	
	
	public Integer getHdIndex() {
		return this.hdIndex;
	}



	@Override
	public ZWPrivateKey getPrivateKey(String password) throws ZWDataEncryptionException {
		
		//TODO -this is weird/bad encapsulation of data, addresses shouldn't have any knowledge of which database they're in to ask about a seed
		ZWPrivateData hdSeedData = ZWWalletManager.getInstance().getHdSeedData();
		String hdSeed = hdSeedData.decrypt(password).getDataString();
		byte[] seedBytes = ZiftrUtils.hexStringToBytes(hdSeed);
		ZWExtendedPrivateKey masterPrivateKey = new ZWExtendedPrivateKey(seedBytes);
		
		//TODO -this probably isn't the most efficient way to do this since we know the index and that it's a private key
		ZWExtendedPrivateKey privateKey = (ZWExtendedPrivateKey) masterPrivateKey.deriveChild(path);
		
		return privateKey;
	}


}


