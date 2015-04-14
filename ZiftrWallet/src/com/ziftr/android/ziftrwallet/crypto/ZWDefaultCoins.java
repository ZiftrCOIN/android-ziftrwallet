package com.ziftr.android.ziftrwallet.crypto;

import java.util.ArrayList;


/**
 * A class to store all the default coin values. 
 * Class should only be loaded if the default values are needed (such as when first initializing the coin databases)
 *
 */
public class ZWDefaultCoins {

	public ZWCoin BTC = new ZWCoin("Bitcoin", "btc", "main", "bitcoin", 8, "10000", null,
			(byte) 0, (byte) 5, (byte) 128, 6, 600, 0, false);
	public ZWCoin LTC = new ZWCoin("Litecoin", "ltc", "main", "litecoin", 8, "100000", null,
			(byte) 48, (byte) 5, (byte) 176, 12, 150, 0, false);
	public  ZWCoin DOGE = new ZWCoin("Dogecoin", "doge", "main", "dogecoin", 8, "100000000", null,
			(byte) 30, (byte) 22, (byte) 158, 6, 60, 0, false);

	public ZWCoin BTC_TEST = new ZWCoin("Bitcoin Testnet", "btc", "testnet3", "bitcoin", 8, "10000", null,
			(byte) 111, (byte) 196, (byte) 239, 6, 600, 0, false);
	public ZWCoin LTC_TEST = new ZWCoin("Litecoin Testnet", "ltc", "testnet", "litecoin", 8, "100000", null,
			(byte) 111, (byte) 196, (byte) 239, 12, 150, 0, false);
	
	
	/****
	public static ZWCoin PPC = new ZWCoin("1000000", "PPC", "Peercoin", "ppc", "main", "peercoin", 8, R.drawable.logo_peercoin,
			(byte) 55, (byte) 117, (byte) 183, 6, 600, "PPCoin Signed Message:\n");
	public static ZWCoin PPC_TEST = new ZWCoin("1000000", "PPC_TEST", "Peercoin Testnet", "ppc", "test", "peercoin", 8, R.drawable.logo_peercoin,
			(byte) 111, (byte) 196, (byte) 239, 6, 600, "PPCoin Signed Message:\n");
	public static ZWCoin DOGE_TEST = new ZWCoin("100000000", "DOGE_TEST", "Dogecoin Testnet", "doge", "test", "dogecoin", 8, R.drawable.logo_dogecoin,
			(byte) 113, (byte) 196, (byte) 241, 6, 60, "Dogecoin Signed Message:\n");
	****/
	
	private ZWDefaultCoins() {
		
	}
	
	
	public static ArrayList<ZWCoin> getDefaultCoins() {
		ZWDefaultCoins loader = new ZWDefaultCoins();
		
		ArrayList<ZWCoin> coins = new ArrayList<ZWCoin>(5);
		
		coins.add(loader.BTC);
		coins.add(loader.LTC);
		coins.add(loader.DOGE);
		coins.add(loader.BTC_TEST);
		coins.add(loader.LTC_TEST);
		
		return coins;
	}

}
