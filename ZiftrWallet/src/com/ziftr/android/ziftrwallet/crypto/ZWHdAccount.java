package com.ziftr.android.ziftrwallet.crypto;

import com.ziftr.android.ziftrwallet.util.CryptoUtils;

public class ZWHdAccount {

	public String symbol;
	public ZWHdChildNumber purpose;
	public ZWHdChildNumber coinType;
	public ZWHdChildNumber account;
	public ZWExtendedPublicKey xpubkey;
	
	public ZWHdAccount(String symbol, ZWHdChildNumber purpose, ZWHdChildNumber coinType, ZWHdChildNumber account, ZWExtendedPublicKey xpubkey) {
		this.symbol = symbol;
		this.purpose = purpose;
		this.coinType = coinType;
		this.account = account;
		this.xpubkey = xpubkey;
	}
	
	public String getPath() {
		String sep = CryptoUtils.HD_PATH_DELIMITER;
		return "m" + sep + purpose.toString() + sep + coinType.toString() + sep + account.toString();
	}
	
	public boolean isTestnet() { 
		return this.symbol.endsWith("_TEST");
	}
	
}
