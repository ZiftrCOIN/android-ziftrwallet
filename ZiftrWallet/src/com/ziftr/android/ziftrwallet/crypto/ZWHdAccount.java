package com.ziftr.android.ziftrwallet.crypto;

import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;

public class ZWHdAccount {

	public String symbol;
	public ZWHdChildNumber purpose;
	public ZWHdChildNumber coinType;
	public ZWHdChildNumber account;
	public ZWExtendedPublicKey xpubkey;
	
	public ZWHdAccount(String symbol, ZWExtendedPublicKey xpubkey) {
		// path should look like "[m/44'/0'/0']"
		ZWHdPath path = xpubkey.path;
		if (path.resolvesToPrivateKey() || path.derivedFromPublicKey() || 
				path.getRelativeToPrv().size() != 3 || path.getRelativeToPub().size() != 0)
			throw new ZWHdWalletException("Illegal path");
		this.symbol = symbol;
		this.purpose = path.getRelativeToPrv().get(0);
		this.coinType= path.getRelativeToPrv().get(1);
		this.account = path.getRelativeToPrv().get(2);
		this.xpubkey = xpubkey;
		
	}
	
	public ZWHdAccount(String symbol, int purpose, int coinType, int account, String xpub) throws ZWAddressFormatException {
		this.symbol = symbol;
		this.purpose = new ZWHdChildNumber(purpose, true);
		this.coinType = new ZWHdChildNumber(coinType, true);
		this.account = new ZWHdChildNumber(account, true);
		this.xpubkey = new ZWExtendedPublicKey(this.getPath(), xpub);
	}
	
	public boolean isMainnet() { 
		return !this.symbol.endsWith("_TEST");
	}
	
	public String xpub() {
		return this.xpubkey.xpub(this.isMainnet());
	}
	
	public ZWHdPath getPath() {
		return new ZWHdPath("[m/" + purpose + "/" + coinType + "/" + account + "]");
	}
	
}
