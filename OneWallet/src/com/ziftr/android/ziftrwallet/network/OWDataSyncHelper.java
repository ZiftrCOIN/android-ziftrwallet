package com.ziftr.android.ziftrwallet.network;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.ziftr.android.ziftrwallet.OWWalletManager;
import com.ziftr.android.ziftrwallet.util.OWCoin;
import com.ziftr.android.ziftrwallet.util.ZLog;

public class OWDataSyncHelper {

	
	
	
	public static void updateTransactionHistory(OWCoin coin) {
		
		ArrayList<String> addresses = OWWalletManager.getInstance().getAddressList(coin, true);
		
		ZiftrNetRequest request = OWApi.buildTransactionsRequest(coin.getType(), coin.getChain(), addresses);
		String response = request.sendAndWait();
		if(request.getResponseCode() == 200) {
			try {
				JSONArray transactions = new JSONArray(response);
				
				//we now have an array of transactions with a transaction hash and a url to get more data about the transaction
				for(int x = 0; x < transactions.length(); x++) {
					JSONObject transactionObject = transactions.getJSONObject(x);
					String transactionId = transactionObject.getString("tx_id");
					String transactionUrl = transactionObject.getString("tx_url");
				}
			}
			catch(Exception e) {
				ZLog.log("Exceptionn downloading transactions: ", e);
			}
		}
	}
}
