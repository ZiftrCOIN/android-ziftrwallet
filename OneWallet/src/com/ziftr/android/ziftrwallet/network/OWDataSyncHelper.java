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

	public static void sendCoinsRequest(OWCoin coin, BigInteger fee, BigInteger amount, List<String> inputs, String output, String passphrase){
		ZiftrNetworkManager.networkStarted();

		ZLog.log("Sending" + amount + "coins to :" + output);

		List<OWAddress> changeAddresses = OWWalletManager.getInstance().readHiddenUnspentAddresses(coin);
		String refundAddress;
		if (changeAddresses.size() <= 0){
			//create new address for change
			refundAddress = OWWalletManager.getInstance().createReceivingAddress(passphrase, coin, OWReceivingAddressesTable.HIDDEN_FROM_USER).toString();
		} else {
			//or reuse one that hasnt been spent from
			refundAddress = changeAddresses.get(0).getAddress();
		}
		//make request
		ZiftrNetRequest request = OWApi.makeTransactionRequest(coin.getType(), coin.getChain(), fee, refundAddress, inputs, output, amount);
		String response = request.sendAndWait();

		if (request.getResponseCode() == 200){
			try {
				JSONObject jsonRes = new JSONObject(response);
				//Sign txn and then POST to server
				ArrayList<String> addresses = new ArrayList<String>();
				for (String a : inputs){
					addresses.add(a);
				}
				addresses.add(output);
				sendCoinsTransaction(coin, jsonRes, amount, addresses);
			}catch(Exception e) {
				ZLog.log("Exception send coin request: ", e);
			}
		}
		ZiftrNetworkManager.networkStopped();
	}

	public static void sendCoinsTransaction(OWCoin coin, JSONObject response, BigInteger amount, ArrayList<String> addresses) throws JSONException{
		//sign the response
		JSONArray signedList = new JSONArray();
		JSONArray toSignList = new JSONArray(response.get("to_sign").toString());
		for (int i=0; i< toSignList.length(); i++){
			JSONObject toSign = new JSONObject(toSignList.get(i).toString());
			OWECKey key = new OWECKey();
			//uncomment when API works
			//OWECKey key = OWWalletManager.getInstance().readAddress(coin, toSign.get("address").toString(), true).getKey();
			toSign.put("pub_key", ZiftrUtils.bytesToHexString(key.getPubKey()));

			OWSha256Hash hash = new OWSha256Hash(toSign.getString("data")); 
			String rHex = ZiftrUtils.bigIntegerToString(key.sign(hash).r, 32);
			String sHex = ZiftrUtils.bigIntegerToString(key.sign(hash).s, 32);
			String hex =  rHex + sHex;
			toSign.put("sig", hex);
			signedList.put(toSign);
			ZLog.log(toSign);
		}
		response.put("to_sign", signedList);
		ZLog.log(response);

		//send txn to server after signage
		ZiftrNetRequest req = OWApi.makeTransaction(coin.getType(), coin.getChain(), response);
		String res = req.sendAndWait();
		ZLog.log(res);
		if (req.getResponseCode() == 202){
			//update DB
			createTransaction(coin, response, addresses);
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
	private static OWTransaction createTransaction(OWCoin coin, JSONObject json, ArrayList<String> addresses) throws JSONException {

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

		//pick and address to use for displaying the note to the user
		String note = null; //TODO -maybe make this an array or character build to hold notes for multiple strings
		for(String noteAddress : displayAddresses) {
			OWAddress databaseAddress = OWWalletManager.getInstance().readAddress(coin, noteAddress, true, false);
			note = databaseAddress.getLabel();
			if(note != null && note.length() > 0) {
				break;
			}
		}

		OWTransaction transaction = OWWalletManager.getInstance().createTransaction(coin, value, fees, displayAddresses, new OWSha256Hash(hash), note, confirmations);

		return transaction;
	}
	
	public static List<String> getHiddenAddresses(OWCoin coin){
		List<OWAddress> addresses = OWWalletManager.getInstance().readHiddenAddresses(coin);
		List<String> hiddenAddresses = new ArrayList<String>();
		for (OWAddress addr : addresses){
			hiddenAddresses.add(addr.getAddress());
		}
		return hiddenAddresses;
	}

}
