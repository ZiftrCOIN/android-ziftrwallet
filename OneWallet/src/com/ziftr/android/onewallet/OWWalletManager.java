package com.ziftr.android.onewallet;

import java.util.HashMap;
import java.util.Map;

import com.google.bitcoin.core.Wallet;
import com.ziftr.android.onewallet.util.OWCoin;

public class OWWalletManager {
	
	/** The map which holds all of the wallets. */
	private Map<OWCoin.Type, Wallet> walletMap = new HashMap<OWCoin.Type, Wallet>();
	
	/**
	 * Placeholder constructor for making a new manager. Doesn't do 
	 * anything right now.
	 */
	public OWWalletManager() {
		
	}
	
	public void addWallet(OWCoin.Type id, Wallet walletToAdd) {
		this.walletMap.put(id, walletToAdd);
	}
	
	public Wallet getWallet(OWCoin.Type id) {
		return this.walletMap.get(id);
	}
	
}