package com.ziftr.android.ziftrwallet.network;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ziftr.android.ziftrwallet.OWWalletManager;
import com.ziftr.android.ziftrwallet.crypto.OWAddress;
import com.ziftr.android.ziftrwallet.crypto.OWECKey;
import com.ziftr.android.ziftrwallet.crypto.OWSha256Hash;
import com.ziftr.android.ziftrwallet.crypto.OWTransaction;
import com.ziftr.android.ziftrwallet.sqlite.OWReceivingAddressesTable;
import com.ziftr.android.ziftrwallet.util.OWCoin;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class OWDataSyncHelper {

	
	public static void sendCoins(OWCoin coin, BigInteger fee, BigInteger amount, List<String> inputs, String output, final String passphrase){
	
		ZLog.log("Sending " + amount + " coins to : ." + output);
		ZiftrNetworkManager.networkStarted();
		
		//find (or create) an address for change that hasn't been spent from
		List<OWAddress> changeAddresses = OWWalletManager.getInstance().readHiddenUnspentAddresses(coin);
		String refundAddress;
		if (changeAddresses.size() <= 0){
			//create new address for change
			refundAddress = OWWalletManager.getInstance().createReceivingAddress(passphrase, coin, OWReceivingAddressesTable.HIDDEN_FROM_USER).toString();
			ZLog.log(refundAddress);
		} else {
			//or reuse one that hasnt been spent from
			refundAddress = changeAddresses.get(0).getAddress();
		}
		
		JSONObject spendJson = buildSpendPostData(inputs, output, amount, fee.toString(), refundAddress);
		
		ZLog.log("Spending coins with json: ", spendJson.toString());
		
		ZiftrNetRequest request = OWApi.buildSpendRequest(coin.getType(), coin.getChain(), spendJson);
		
		String response = request.sendAndWait();
		ZLog.forceFullLog(response);
		if (request.getResponseCode() == 200){
			try {
				JSONObject jsonRes = new JSONObject(response);
				
				ZLog.log("Send coins step 1 response: ", jsonRes.toString());
				
				signSentCoins(coin, jsonRes, inputs, passphrase);
			}catch(Exception e) {
				ZLog.log("Exception send coin request: ", e);
			}
		}

		ZiftrNetworkManager.networkStopped();
	}
	
	
	
	private static void signSentCoins(OWCoin coin, JSONObject serverResponse, List<String> inputs, String passphrase) {
		try {
			JSONArray toSignList = serverResponse.getJSONArray("to_sign");
			for (int i=0; i< toSignList.length(); i++){
				JSONObject toSign = toSignList.getJSONObject(i);
	
				String signingAddressPublic = toSign.optString("address");
				
				//TODO -remove when api works
				signingAddressPublic = "n1DmeLFrEiR2AUZZfqbwRgMJCKS1CF4ACF";
				
				OWAddress signingAddress = OWWalletManager.getInstance().readAddress(coin, signingAddressPublic, true);
				OWECKey key = signingAddress.getKey();
	
				toSign.put("pub_key", ZiftrUtils.bytesToHexString(key.getPubKey()));
	
				OWSha256Hash hash = new OWSha256Hash(toSign.getString("tosign")); 
				
				String rHex = ZiftrUtils.bigIntegerToString(key.sign(hash).r, 32);
				toSign.put("r", rHex);
				
				String sHex = ZiftrUtils.bigIntegerToString(key.sign(hash).s, 32);
				toSign.put("s", sHex);
				
				ZLog.log(toSign);
			}
		}
		catch(Exception e) {
			ZLog.log("Exeption attemting to sign transactions: ", e);
		}
		
		//now that we've packed all the signing into a json object send it off to the server
		ZiftrNetRequest signingRequest = OWApi.buildSpendSigningRequest(coin.getType(), coin.getChain(), serverResponse);
		
		String response = signingRequest.sendAndWait();
		if(signingRequest.getResponseCode() == 202){
			try {
				//update DB
				
				JSONObject responseJson = new JSONObject(response);
				
				OWTransaction completedTransaction = createTransaction(coin, responseJson, inputs);
				OWWalletManager.getInstance().updateTransaction(completedTransaction); //TODO -we should change it so that the create will automatically do this if needed
			}
			catch(Exception e) {
				ZLog.log("Exception saving spend transaction: ", e);
			}
		}
	}
	

	
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
				ZLog.log("Exception downloading transactions: ", e);
			}
		}//end if response code = 200

		ZiftrNetworkManager.networkStopped();
	}

	
	//creates transaction in local database from json response
	private static OWTransaction createTransaction(OWCoin coin, JSONObject json, List<String> addresses) throws JSONException {

		String hash = json.getString("hash");
		//String time = json.getString("time_received");  //TODO -need to add this to transaction table
		BigInteger fees = new BigInteger(json.getString("fee"));
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
					String outputValue = input.getString("output_value");
					if(!usedValue) {
						usedValue = true;
						value.subtract(new BigInteger(outputValue));
					}
					displayAddresses.add(inputAddress);
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

		//pick an address to use for displaying the note to the user
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
	

	
	private static JSONObject buildSpendPostData(List<String> inputs, String output, BigInteger amount, String fee, String changeAddress) {
		JSONObject postData = new JSONObject();
		try {
			postData.put("fee_per_kb", fee);
			postData.put("surplus_refund_address", changeAddress);
			JSONArray inputAddresses = new JSONArray();
			for (String addr : inputs){
				inputAddresses.put(addr);
			}
			
			postData.put("input_addresses", inputAddresses);
			JSONObject outputAddresses = new JSONObject();
			
			outputAddresses.put(output, amount.longValue());
			
			postData.put("output_addresses", outputAddresses);
		}
		catch(Exception e) {
			ZLog.log("Exception build spend JSON: ", e);
		}
		
		return postData;
	}

}





