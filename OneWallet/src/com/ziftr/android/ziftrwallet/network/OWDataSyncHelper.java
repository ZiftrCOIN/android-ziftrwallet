package com.ziftr.android.ziftrwallet.network;

import java.math.BigInteger;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ziftr.android.ziftrwallet.OWWalletManager;
import com.ziftr.android.ziftrwallet.crypto.OWAddress;
import com.ziftr.android.ziftrwallet.crypto.OWSha256Hash;
import com.ziftr.android.ziftrwallet.crypto.OWTransaction;
import com.ziftr.android.ziftrwallet.util.OWCoin;
import com.ziftr.android.ziftrwallet.util.ZLog;

public class OWDataSyncHelper {

	
	
	
	public static void updateTransactionHistory(OWCoin coin) {
		
		ZiftrNetworkManager.networkStarted();
		
		ArrayList<String> addresses = OWWalletManager.getInstance().getAddressList(coin, true);
		
		ZLog.log("Getting transaction history for addresses: ", addresses);
		
		ZiftrNetRequest request = OWApi.buildTransactionsRequest(coin.getType(), coin.getChain(), addresses);
		String response = request.sendAndWait();
		
		ZLog.log("Transaction history response(" + request.getResponseCode() + "): ", response);
		if(request.getResponseCode() == 200) {
			try {
				JSONArray transactions = new JSONArray(response);
				
				//we now have an array of transactions with a transaction hash and a url to get more data about the transaction
				for(int x = 0; x < transactions.length(); x++) {
					JSONObject transactionJson = transactions.getJSONObject(x);
					
					OWTransaction transaction = createTransaction(coin, transactionJson, addresses);
					OWWalletManager.getInstance().updateTransaction(transaction); //TODO -we should change it so that the create will automatically do this if needed
				}
				
				//if we got this far without any exceptions, everything went good, so let the UI know the database has changed, so it can update
				ZiftrNetworkManager.dataUpdated();
			}
			catch(Exception e) {
				ZLog.log("Exceptionn downloading transactions: ", e);
			}
		}//end if response code = 200
		
		ZiftrNetworkManager.networkStopped();
	}
	
	
	
	private static OWTransaction createTransaction(OWCoin coin, JSONObject json, ArrayList<String> addresses) throws JSONException {
		
		String hash = json.getString("hash");
		String time = json.getString("time_received");  //TODO -need to add this to transaction table
		BigInteger fees = new BigInteger(json.getString("fees"));
		BigInteger value = new BigInteger("0"); //calculate this by scanning inputs and outputs
		long confirmations = json.getLong("num_confirmations");
		ArrayList<String> displayAddresses = new ArrayList<String>();
		
		JSONArray inputs = json.getJSONArray("inputs");
		JSONArray outputs = json.getJSONArray("outputs");
		
		for(int x = 0; x < inputs.length(); x++) {
			JSONObject input = inputs.getJSONObject(x);
			JSONArray inputAddresses = input.getJSONArray("addresses");
			boolean usedValue = false;
			for(int y = 0; y < inputAddresses.length(); y++) {
				String inputAddress = inputAddresses.getString(y);
				if(addresses.contains(inputAddress)) {		
					displayAddresses.add(inputAddress);
					String outputValue = input.getString("output_value");
					if(!usedValue) {
						usedValue = true;
						value.subtract(new BigInteger(outputValue));
					}
				}
			}//end for y
		}//end for x
		
		for(int x = 0; x < outputs.length(); x++) {
			JSONObject output = outputs.getJSONObject(x);
			JSONArray outputAddresses = output.getJSONArray("addresses");
			boolean usedValue = false;
			for(int y = 0; y < outputAddresses.length(); y++) {
				String outputAddress = outputAddresses.getString(y);
				if(addresses.contains(outputAddress)) {
					if(!usedValue) {
						usedValue = true;
						String outputValue = output.getString("output_value");
						value.add(new BigInteger(outputValue));
					}
					displayAddresses.add(outputAddress);
				}
			}
		}
		
		//pick and address to use for displaying the note to the user
		String note = null; //TODO -maybe make this an array or character build to hold notes for multiple strings
		for(String noteAddress : displayAddresses) {
			OWAddress databaseAddress = OWWalletManager.getInstance().readAddress(coin, noteAddress, true);
			note = databaseAddress.getLabel();
			if(note != null && note.length() > 0) {
				break;
			}
		}
		
		
		OWTransaction transaction = OWWalletManager.getInstance().createTransaction(coin, value, fees, displayAddresses, new OWSha256Hash(hash), note, confirmations);
		
		return transaction;
	}
	
	
}
