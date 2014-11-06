package com.ziftr.android.ziftrwallet.network;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ziftr.android.ziftrwallet.OWWalletManager;
import com.ziftr.android.ziftrwallet.crypto.OWAddress;
import com.ziftr.android.ziftrwallet.crypto.OWECDSASignature;
import com.ziftr.android.ziftrwallet.crypto.OWECKey;
import com.ziftr.android.ziftrwallet.crypto.OWSha256Hash;
import com.ziftr.android.ziftrwallet.crypto.OWTransaction;
import com.ziftr.android.ziftrwallet.sqlite.OWReceivingAddressesTable;
import com.ziftr.android.ziftrwallet.util.OWCoin;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class OWDataSyncHelper {

	
	public static String sendCoins(OWCoin coin, BigInteger fee, BigInteger amount, List<String> inputs, String output, final String passphrase){
		String message = "";
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
//		commented out since throws NPE if no internet
//		ZLog.forceFullLog(response);
		switch (request.getResponseCode()){
			case 200:
				try {
					JSONObject jsonRes = new JSONObject(response);
					
					ZLog.log("Send coins step 1 response: ", jsonRes.toString());
					
					message = signSentCoins(coin, jsonRes, inputs, passphrase);
				}catch(Exception e) {
					ZLog.log("Exception send coin request: ", e);
				}
				break;
			case 0:
				ZLog.log("Unable to connect to server. Please check your connection.");
				message = "Unable to connect to server. Please check your connection.";
				break;
			default:
				message = "Error, something went wrong!";
				break;
		}

		ZiftrNetworkManager.networkStopped();
		return message;
	}
	
	
	private static String signSentCoins(OWCoin coin, JSONObject serverResponse, List<String> inputs, String passphrase) {
		try {
			JSONArray toSignList = serverResponse.getJSONArray("to_sign");
			for (int i=0; i< toSignList.length(); i++){
				JSONObject toSign = toSignList.getJSONObject(i);
	
				String signingAddressPublic = toSign.optString("address");
				
				ZLog.log("Signing a transaction with address: ", signingAddressPublic);
				
				OWAddress signingAddress = OWWalletManager.getInstance().readAddress(coin, signingAddressPublic, true);
				OWECKey key = signingAddress.getKey();

				toSign.put("pubkey", ZiftrUtils.bytesToHexString(key.getPubKey()));
	
				OWSha256Hash hash = new OWSha256Hash(toSign.getString("tosign")); 
				
				OWECDSASignature signature = key.sign(hash);
				
				//signature.encodeToDER();
				
				String rHex = ZiftrUtils.bigIntegerToString(signature.r, 32);
				toSign.put("r", rHex);
				
				String sHex = ZiftrUtils.bigIntegerToString(signature.s, 32);
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
		ZLog.log("Response from signing: ", response);
		if(signingRequest.getResponseCode() == 202){
			try {
				//update DB
				
				//TODO -this is so in-efficient, but for now, just update transaction history again
				updateTransactionHistory(coin);
				
				/***
				JSONObject responseJson = new JSONObject(response);
				
				OWTransaction completedTransaction = createTransaction(coin, responseJson, inputs);
				OWWalletManager.getInstance().updateTransaction(completedTransaction); //TODO -we should change it so that the create will automatically do this if needed
				*****/
				return "";
			}
			catch(Exception e) {
				ZLog.log("Exception saving spend transaction: ", e);
			}
		}
		return "error: " + signingRequest.getResponseMessage();
		
	}


	public static List<OWCoin> getBlockChainWallets(){
		ZiftrNetworkManager.networkStarted();

		List<OWCoin> supportedCoins = new ArrayList<OWCoin>();
		
		ZiftrNetRequest request = OWApi.buildGenericApiRequest(false, "/blockchains");
		String response = request.sendAndWait();
		if (request.getResponseCode() == 200){
			JSONArray res;
			try {
				res = new JSONArray((new JSONObject(response)).getString("blockchains"));
				for (int i=0; i< res.length(); i++){
					JSONObject coinJson = res.getJSONObject(i);
					OWCoin supportedCoin;
					if (coinJson.getString("chain").contains("test")){
						supportedCoin = OWCoin.valueOf(coinJson.getString("type").toUpperCase(Locale.getDefault()) + "_TEST");
					} else {
						supportedCoin = OWCoin.valueOf(coinJson.getString("type").toUpperCase(Locale.getDefault()));
					}
					supportedCoins.add(supportedCoin);
					
					if(supportedCoin != null) {
						String defaultFee = coinJson.getString("default_fee_per_kb");
						int pubKeyPrefix = coinJson.getInt("p2pkh_byte");
						int scriptHashPrefix = coinJson.getInt("p2sh_byte");
						int privateBytePrefix = coinJson.getInt("priv_byte");
						int blockTime = coinJson.getInt("seconds_per_block_generated");
						int confirmationsNeeded = coinJson.getInt("recommended_confirmations");
					
						supportedCoin.updateCoin(defaultFee, (byte)pubKeyPrefix, (byte)scriptHashPrefix, (byte)privateBytePrefix, confirmationsNeeded, blockTime);
					}
					
						
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			ZLog.log(response);
		}
		ZiftrNetworkManager.networkStopped();
		for (OWCoin x : supportedCoins){
			ZLog.log(x.toString());
		}
		return supportedCoins;
	}
	
	public static String getMarketValue(String type, String fiat){
		ZiftrNetworkManager.networkStarted();
		String val = "";
		ZiftrNetRequest request = OWApi.buildMarketValueRequest(type, fiat);
		String response = request.sendAndWait();
		if (request.getResponseCode() == 200){
			//TODO when api market_value call works
		} else {
			String dummy_res = "{\"currency_to\": \"usd\",\"currency_from\": \"btc/main\",\"exchange_rate\": 40000, \"exchange_rate_divisor\": 100 }";
			try {
				JSONObject res = new JSONObject(dummy_res);
				BigDecimal usdRate = new BigDecimal(res.getLong("exchange_rate") / res.getLong("exchange_rate_divisor"));
				val = usdRate.toPlainString();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		ZiftrNetworkManager.networkStopped();
		return val;
	}


	public static void updateTransactionHistory(OWCoin coin) {
		ZiftrNetworkManager.networkStarted();

		ArrayList<String> addresses = OWWalletManager.getInstance().getAddressList(coin, true);

		ZLog.log("Getting transaction history for addresses: ", addresses);

		ZiftrNetRequest request = OWApi.buildTransactionsRequest(coin.getType(), coin.getChain(), addresses);
		String response = request.sendAndWait();

		ZLog.forceFullLog("Transaction history response(" + request.getResponseCode() + "): " + response);
		if(request.getResponseCode() == 200) {
			try {
				
				HashMap<String, JSONObject> parsedTransactions = new HashMap<String, JSONObject>();
				
				JSONObject responseJson = new JSONObject(response);
				JSONArray transactions = responseJson.getJSONArray("transactions");

				//we now have an array of transactions with a transaction hash and a url to get more data about the transaction
				for(int x = transactions.length() -1; x >= 0; x--) {
					JSONObject transactionJson = transactions.getJSONObject(x);
					String txid = transactionJson.getString("txid");
					parsedTransactions.put(txid, transactionJson);
					OWTransaction transaction = createTransaction(coin, transactionJson, addresses, parsedTransactions);
					OWWalletManager.getInstance().updateTransactionNumConfirmations(transaction); //TODO -we should change it so that the create will automatically do this if needed
				}

				//if we got this far without any exceptions, everything went good, so let the UI know the database has changed, so it can update
				ZiftrNetworkManager.dataUpdated();
			}
			catch(Exception e) {
				ZLog.log("Exception downloading transactions: ", e);
			}
		}//end if response code = 200

		ZiftrNetworkManager.networkStopped();
		
		ZiftrNetworkManager.dataUpdated();
	}

	
	//creates transaction in local database from json response
	private static OWTransaction createTransaction(OWCoin coin, JSONObject json, List<String> addresses, HashMap<String, JSONObject> parsedTransactions) throws JSONException {
		String hash = json.getString("txid");
		long time = json.optLong("time");
		if(time == 0) {
			//this happens when a transaction hasn't even been confirmed once
			//so just use the current time
			time = System.currentTimeMillis() / 1000; //server sends us seconds, so our hold over time should be formatted the same way
		}
		//TODO -just setting this as zero for now, need to either get server to send this, or calculate it
		//BigInteger fees = new BigInteger(json.getString("fee"));
		BigInteger fees = new BigInteger("0");
		
		BigInteger value = new BigInteger("0"); //calculate this by scanning inputs and outputs
		long confirmations = json.optLong("confirmations");
		ArrayList<String> displayAddresses = new ArrayList<String>();
		ArrayList<String> myHiddenAddresses = new ArrayList<String>();
		
		for (OWAddress addr : OWWalletManager.getInstance().readHiddenAddresses(coin)){
			myHiddenAddresses.add(addr.getAddress());
		}

		JSONArray inputs = json.getJSONArray("vin");
		JSONArray outputs = json.getJSONArray("vout");

		for(int x = 0; x < inputs.length(); x++) {
			JSONObject input = inputs.getJSONObject(x);
			
			boolean usedValue = false;
			//the json doesn't have an input address or value, so we have to look at previously parsed transactions
			//and see if this input is the output of another, which would indicate that it is us spending coins
			String inputTxid = input.getString("txid");
			
			JSONObject matchingTransaction = parsedTransactions.get(inputTxid);
			if(matchingTransaction != null) {
				//this means we're spending coins in this transaction
				int voutIndex = input.getInt("vout");
				JSONArray vouts = matchingTransaction.getJSONArray("vout");
				JSONObject vout = vouts.optJSONObject(voutIndex);
				if(vout != null) {
					JSONObject scriptPubKey = vout.getJSONObject("scriptPubKey");
					JSONArray inputAddresses = scriptPubKey.getJSONArray("addresses");
					for(int y = 0; y < inputAddresses.length(); y++) {
						String inputAddress = inputAddresses.getString(y);
						if(addresses.contains(inputAddress)) {
							String outputValue = vout.getString("value");
							if(!usedValue) {
								usedValue = true;
								//TODO -multiply hack until server fixes decimal issue
								BigInteger outputValueInt = new BigInteger(outputValue);
								value = value.subtract(outputValueInt);
							}
						}
					}//end for y
				}
			}
			
			/*******
			JSONArray inputAddresses = input.getJSONArray("addresses");
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
			**************/
			
		}//end for x

		for(int x = 0; x < outputs.length(); x++) {
			JSONObject output = outputs.getJSONObject(x);
			JSONObject scriptPubKey = output.getJSONObject("scriptPubKey");
			JSONArray outputAddresses = scriptPubKey.getJSONArray("addresses");
			boolean usedValue = false;
			for(int y = 0; y < outputAddresses.length(); y++) {
				String outputAddress = outputAddresses.getString(y);
				if(addresses.contains(outputAddress)) {
					if(!usedValue) {
						usedValue = true;
						String outputValue = output.getString("value");
						//value.add(new BigInteger(outputValue));
						BigInteger outputValueInt = new BigInteger(outputValue); 
						value = value.add(outputValueInt);
					}
				}
				
				//get the address we sent to for displaying
				JSONArray addressesSentTo = outputs.optJSONObject(y).getJSONObject("scriptPubKey").getJSONArray("addresses");
				for(int z = 0; z < addressesSentTo.length(); z++) {
					String sentToAddr = addressesSentTo.getString(z);
					OWAddress sentTo = OWWalletManager.getInstance().readAddress(coin, sentToAddr, false);
					if (sentTo != null && !myHiddenAddresses.contains(sentToAddr) && !displayAddresses.contains(sentTo.getAddress())){
						displayAddresses.add(sentTo.getAddress());
					}
				}
			}
		}
		if (displayAddresses.size() == 0){
			//API doesn't expose who sent to us addresses
			displayAddresses.add("unknown");
		}

		//pick an address to use for displaying the note to the user
		String note = "unknown"; //TODO -maybe make this an array or character build to hold notes for multiple strings
		for(String noteAddress : displayAddresses) {
			//we only see notes for addresses we sent to since the api doesn't expose addresses of who sent to us
			OWAddress databaseAddress = OWWalletManager.getInstance().readAddress(coin, noteAddress, false);
			if (databaseAddress != null){
			if(databaseAddress.getLabel() != null && databaseAddress.getLabel().length() > 0 && !databaseAddress.getLabel().equals("unknown")) {
				note = databaseAddress.getLabel();
				break;
				}
			}
		}
		time = time * 1000;
		OWTransaction transaction = OWWalletManager.getInstance().createTransaction(coin, value, fees, displayAddresses, new OWSha256Hash(hash), note, confirmations, time);

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





