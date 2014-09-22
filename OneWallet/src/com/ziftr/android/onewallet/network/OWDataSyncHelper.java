package com.ziftr.android.onewallet.network;

import java.util.ArrayList;

import com.ziftr.android.onewallet.OWWalletManager;
import com.ziftr.android.onewallet.util.OWCoin;

public class OWDataSyncHelper {

	
	
	
	public static void updateTransactionHistory(OWCoin coin) {
		
		ArrayList<String> address = OWWalletManager.getInstance().getAddressList(coin, true);
	}
}
