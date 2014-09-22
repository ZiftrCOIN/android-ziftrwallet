package com.ziftr.android.ziftrwallet.network;

import java.util.ArrayList;

import com.ziftr.android.ziftrwallet.OWWalletManager;
import com.ziftr.android.ziftrwallet.util.OWCoin;

public class OWDataSyncHelper {

	
	
	
	public static void updateTransactionHistory(OWCoin coin) {
		
		ArrayList<String> address = OWWalletManager.getInstance().getAddressList(coin, true);
	}
}
