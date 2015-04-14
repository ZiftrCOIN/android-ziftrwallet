package com.ziftr.android.ziftrwallet.network;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;

import com.ziftr.android.ziftrwallet.ZWMessageManager;
import com.ziftr.android.ziftrwallet.ZWPreferencesUtils;
import com.ziftr.android.ziftrwallet.ZWWalletManager;
import com.ziftr.android.ziftrwallet.crypto.ZWAddress;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWECDSASignature;
import com.ziftr.android.ziftrwallet.crypto.ZWECKey;
import com.ziftr.android.ziftrwallet.crypto.ZWTransaction;
import com.ziftr.android.ziftrwallet.crypto.ZWTransactionOutput;
import com.ziftr.android.ziftrwallet.fragment.ZWRequestCodes;
import com.ziftr.android.ziftrwallet.fragment.ZWTags;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWDataSyncHelper {
	
	//key to save the server response of a send transaction to sign
	public static final String toSignResponseKey = "SERVER_RESPONSE_TO_SIGN";
	
	//last time we refreshed transaction history 
	public static HashMap<String, Long> lastRefreshed = new HashMap<String, Long>();
	
	//seconds to wait before autorefreshing
	public static final long REFRESH_TIME = 60;
	
	public static JSONObject sendCoins(ZWCoin coin, BigInteger fee, BigInteger amount, List<String> inputs, String output, final String passphrase){
		String message = "";
		ZLog.log("Sending " + amount + " coins to " + output);
		ZiftrNetworkManager.networkStarted();
		
		//find (or create) an address for change that hasn't been spent from
		List<String> changeAddresses = ZWWalletManager.getInstance().getHiddenAddressList(coin, false);
		String changeAddress;
		if (changeAddresses.size() <= 0){
			//create new address for change
			changeAddress = ZWWalletManager.getInstance().createChangeAddress(passphrase, coin).getAddress();
			ZLog.log(changeAddress);
		} else {
			//or reuse one that hasnt been spent from
			changeAddress = changeAddresses.get(0);
		}
		
		JSONObject spendJson = buildSpendPostData(inputs, output, amount, fee.toString(), changeAddress);
		
		ZLog.log("Spending coins with json: ", spendJson.toString());
		
		ZiftrNetRequest request = ZWApi.buildSpendRequest(coin.getType(), coin.getChain(), spendJson);
		
		String response = request.sendAndWait();
		ZLog.forceFullLog(response);		
		
		switch (request.getResponseCode()){
			case 200:
				try {
					JSONObject jsonRes = new JSONObject(response);
					
					ZLog.log("Spend transaction data to sign: ", jsonRes.toString());
					ZiftrNetworkManager.networkStopped();
					return jsonRes;

				}
				catch(Exception e) {
					ZLog.log("Exception send coin request: ", e);
					message = "Error, something went wrong! " + request.getResponseCode() + " " + request.getResponseMessage();
				}
				break;
			case 0:
				ZLog.log("Unable to connect to server. Please check your connection.");
				message = "Unable to connect to server. Please check your connection.";
				break;
			default:
				ZLog.log("Default error sending");
				message = "Error, something went wrong! " + request.getResponseCode() + " " + request.getResponseMessage();
				break;
		}

		ZiftrNetworkManager.networkStopped();
		if (!message.isEmpty() && request.getResponseCode() != 200) {
			ZWMessageManager.alertError(message);
		}
		return null;
	}
	
	static boolean checkSpendingUnconfirmedTxn(ZWCoin coin, JSONObject serverResponse, List<String> inputs, String passphrase){
		List<ZWTransaction> spendingFromTxns = new ArrayList<ZWTransaction>();
		try {
			JSONArray vin = serverResponse.getJSONArray("vin");
			for (int i=0; i< vin.length(); i++){
				JSONObject input = vin.optJSONObject(i);
				String txid = input.optString("txid");
				ZWTransaction txn = ZWWalletManager.getInstance().readTransactionByHash(coin, txid);
				spendingFromTxns.add(txn);
			}
			
			for (ZWTransaction txn : spendingFromTxns){
				if (txn.isPending()){
					//warn user of spending unconfirmed txn
					Bundle b = new Bundle();
					b.putString(toSignResponseKey, serverResponse.toString());
					b.putString(ZWPreferencesUtils.BUNDLE_PASSPHRASE_KEY, passphrase);
					b.putString(ZWCoin.TYPE_KEY, coin.getSymbol());
					ZWMessageManager.alertConfirmation(ZWRequestCodes.CONTINUE_SENDING_UNCONFIRMED, "Warning: this spend transaction will use" +
					" unconfirmed coins!", ZWTags.CONTINUE_SPENDING_UNCONFIRMED, b);
					return true;
				}
			}
		} catch (JSONException e1) {
			ZLog.log("Exception getting txns spending from before signing: ", e1);
		}
		return false;
	}
	
	public static boolean signSentCoins(ZWCoin coin, JSONObject serverResponse, String passphrase) {
		Set<String> addressesSpentFrom = new HashSet<String>();
		ZiftrNetworkManager.networkStarted();
		try {
			JSONArray toSignList = serverResponse.getJSONArray("to_sign");
			for (int i=0; i< toSignList.length(); i++){
				JSONObject toSign = toSignList.getJSONObject(i);
	
				String signingAddressPublic = toSign.optString("address");
				addressesSpentFrom.add(signingAddressPublic);
				
				ZLog.log("Signing a transaction with address: ", signingAddressPublic);
				
				ZWECKey key = ZWWalletManager.getInstance().getKeyForAddress(coin, signingAddressPublic, passphrase);
				toSign.put("pubkey", ZiftrUtils.bytesToHexString(key.getPubKey()));
	
				String stringToSign = toSign.getString("tosign");
				ZWECDSASignature signature = key.sign(stringToSign);
				
				String rHex = ZiftrUtils.bigIntegerToString(signature.r, 32);
				toSign.put("r", rHex);
				
				String sHex = ZiftrUtils.bigIntegerToString(signature.s, 32);
				toSign.put("s", sHex);
			}
		}
		catch(Exception e) {
			ZLog.log("Exception attempting to sign transactions: ", e);
		}
		ZLog.log("sending  this signed tx to server :" + serverResponse);
		//now that we've packed all the signing into a json object send it off to the server
		ZiftrNetRequest signingRequest = ZWApi.buildSpendSigningRequest(coin.getType(), coin.getChain(), serverResponse);
		
		String response = signingRequest.sendAndWait();
		ZLog.log(signingRequest.getResponseCode());
		ZLog.log("Response from signing: ", response);
		ZiftrNetworkManager.networkStopped();
		if(signingRequest.getResponseCode() == 202){
			try {
				//flag any addresses we spent from as having been spent from (so we don't reuse change addresses)
				if (addressesSpentFrom.size() <= 0){
					ZLog.log("addresses we spent from weren't in our db ERROR!");
				} else {
					for (String addr : addressesSpentFrom){
						ZLog.log(addr + "changed to spent");
						ZWAddress address = ZWWalletManager.getInstance().getAddress(coin, addr, true);
						address.setSpentFrom(true);
						ZWWalletManager.getInstance().updateAddress(address);
					}
				}
				
				//update the transactions table immediately with the data from this transaction
				try {
					JSONObject responseJson = new JSONObject(response);
					ZLog.log("Response from completed signing: ", responseJson);
					
					//createTransactionFromSpendResponse(coin, responseJson, null);
					//createTransaction(coin, responseJson, inputs, new HashMap<String, JSONObject>());
					
					//TODO -once create transaction works properly remove this
					updateTransactionHistory(coin, false);
				}
				catch(Exception e2) {
					ZLog.log("Exception parsing successful spend response: ", e2);
					
					//if the server said the send was successful, but we couldn't handle the response, 
					//then we just update our history and hope it's there
					updateTransactionHistory(coin, false);
				}
			} catch(Exception e) {
				ZLog.log("Exception saving spend transaction: ", e);
			}
			return true;
		} else {
			ZWMessageManager.alertError("error: " + signingRequest.getResponseMessage());
			return false;
		}
		
	}


	
	public static void downloadCoinData(){
		ZiftrNetworkManager.networkStarted();
		
		ZiftrNetRequest request = ZWApi.buildCoinDataRequest();
		String response = request.sendAndWait();
		if (request.getResponseCode() == 200){

			ZLog.log("Coin data response: ", response);
			try {
				JSONObject jsonResponse = new JSONObject(response);
				JSONArray coinsArray = jsonResponse.getJSONArray("blockchains");
				
				for (int i=0; i< coinsArray.length(); i++){
					JSONObject coinJson = coinsArray.getJSONObject(i).getJSONObject("blockchain");
	
					String name = coinJson.optString("name");
	
					long defaultFee = coinJson.getLong("default_fee_per_kb");
					String feeString = String.valueOf(defaultFee);
					
					byte pubKeyPrefix = (byte) coinJson.getInt("p2pkh_byte");
					byte scriptHashPrefix = (byte) coinJson.getInt("p2sh_byte");
					byte privateBytePrefix = (byte) coinJson.getInt("priv_byte");
					int blockTime = coinJson.getInt("seconds_per_block_generated");
					int confirmationsNeeded = coinJson.getInt("recommended_confirmations");
					String chain = coinJson.getString("chain");
					String type = coinJson.getString("type");
					
					int blockNum = coinJson.optInt("height");
					boolean isEnabled = coinJson.optBoolean("is_enabled");
					
					
					String scheme = coinJson.optString("scheme");
					if(scheme == null || scheme.length() == 0) {
						//TODO -temp hack until api gives us coin scheme (maybe this is just always true?)
						String schemeBase = name;
						if(schemeBase.contains(" ")) {
							schemeBase = schemeBase.substring(0, schemeBase.indexOf(" "));
						}
						scheme = schemeBase.toLowerCase(Locale.US);

					}
					
					int scale = coinJson.optInt("scale");
					if(scale == 0) {
						//TODO -temp hack until api sends us proper coin scale
						scale = 8;
					}

					String logoUrl = null; //TODO -server doesn't send us logos yet (coins handle this by using backup resource ids instead)
					
					if(name != null && name.length() > 0) {
						ZWCoin coin = new ZWCoin(name, type, chain, scheme, scale, feeString, logoUrl, pubKeyPrefix, 
								scriptHashPrefix, privateBytePrefix, confirmationsNeeded, blockTime, isEnabled);
						ZWWalletManager.getInstance().updateCoin(coin);
					}
				
				}
			} catch (Exception e) {
				ZLog.log("Exception parsing coin data: ", e);
			}
		} else {
			ZLog.log("error getting /blockchains wallets: " + response);
		}
		
		//now load any coin changes into the coins class for faster access later
		ZWCoin.loadCoins(ZWWalletManager.getInstance().getCoins());
		
		ZiftrNetworkManager.networkStopped();
	}
	
	public static void getMarketValue(){
		ZiftrNetworkManager.networkStarted();
		ZiftrNetRequest request = ZWApi.buildMarketValueRequest();
		String response = request.sendAndWait();
		if (request.getResponseCode() == 200){
			ZLog.log("Market Value Response: ", response);
			try {
				JSONObject marketJson = new JSONObject(response);
				JSONArray marketRes = marketJson.getJSONArray("market_values");
				for (int i=0; i< marketRes.length(); i++){
					JSONObject marketVal = marketRes.getJSONObject(i).getJSONObject("market_value");
					String convertingFrom = parseCurrency(marketVal.getString("currency_from"));
					String convertingTo = parseCurrency(marketVal.getString("currency_to"));

					BigDecimal rate = new BigDecimal(marketVal.getLong("exchange_rate")).divide(new BigDecimal(marketVal.getLong("exchange_rate_divisor")), MathContext.DECIMAL64);
									
					ZLog.log("Market value of " + convertingFrom + " is : " + rate.toString() + " " + convertingTo);
					ZWWalletManager.getInstance().upsertExchangeValue(convertingFrom, convertingTo, rate.toString());
					//if we got a positive exchange rate and the inverse value is 0 (default) then update the inverse exchange rate
					if (rate.compareTo(BigDecimal.ZERO) == 1 && ZWWalletManager.getInstance().getExchangeValue(convertingTo, convertingFrom).equals("0")){
						ZLog.log("Inserted default Market value of " + convertingTo + " is : " + BigDecimal.ONE.divide(rate, MathContext.DECIMAL64).toString() + " " + convertingFrom);
						ZWWalletManager.getInstance().upsertExchangeValue(convertingTo, convertingFrom, BigDecimal.ONE.divide(rate, MathContext.DECIMAL64).toString());
					}
				
				}
			} catch (JSONException e) {
				ZLog.log("Market Value Response wasn't json array?!");
				e.printStackTrace();
			}
		} else {
			ZLog.log("Market Value Error: ", response);
		}
		ZiftrNetworkManager.networkStopped();
	}
	
	//helper method to convert server market value string to ZWCurrency
	private static String parseCurrency(String currencyString){
		
		if(currencyString.contains("/")) {
			//coin type
			String coinString = currencyString.split("/")[0];
			if(currencyString.contains("test")) {
				coinString += "_TEST";
			}
			return coinString.toUpperCase(Locale.US);
		}
		else {
			//fiat type
			return currencyString.toUpperCase(Locale.ENGLISH);
		}
	}


	public static void updateTransactionHistory(ZWCoin coin, boolean autorefresh) {
		if (autorefresh && ZWDataSyncHelper.lastRefreshed.containsKey(coin.getSymbol())){
			if (ZWDataSyncHelper.lastRefreshed.get(coin.getSymbol()) > (System.currentTimeMillis() / 1000) - REFRESH_TIME){
				ZLog.log("history not refreshed" );
				return;
			}
		}
		List<String> addresses = ZWWalletManager.getInstance().getAddressList(coin, true);
		if (addresses.size() <= 0){
			ZLog.log("No addresses to get transaction history for");
			return;
		}
		ZiftrNetworkManager.networkStarted();

		ZLog.log("Getting transaction history for addresses: ", addresses);

		ZiftrNetRequest request = ZWApi.buildTransactionsRequest(coin.getType(), coin.getChain(), addresses);
		String response = request.sendAndWait();

		ZLog.forceFullLog("Transaction history response(" + request.getResponseCode() + "): " + response);
		if(request.getResponseCode() == 200) {
			try {
				JSONObject responseJson = new JSONObject(response);
				JSONArray transactions = responseJson.getJSONArray("transactions");

				//we now have an array of transactions with a transaction hash and a url to get more data about the transaction
				for(int x = transactions.length() -1; x >= 0; x--) {
					JSONObject transactionInfoJson = transactions.getJSONObject(x);
					JSONObject transactionJson = transactionInfoJson.getJSONObject("transaction");
					createTransaction(coin, transactionJson);
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
		ZWDataSyncHelper.lastRefreshed.put(coin.getSymbol(), System.currentTimeMillis() / 1000);
	}

	
	//creates transaction in local database from json response
	private static void createTransaction(ZWCoin coin, JSONObject json) throws JSONException {	
		String transactionId = json.getString("txid");
		ZWTransaction transaction = new ZWTransaction(coin, transactionId);
		
		ArrayList<ZWAddress> displayAddresses = new ArrayList<ZWAddress>();
		ArrayList<ZWTransactionOutput> receivedOutputs = new ArrayList<ZWTransactionOutput>();
		//ArrayList<ZWTransactionOutput> spentOutputs = new ArrayList<ZWTransactionOutput>();
		
		BigInteger inputCoin = new BigInteger("0");
		boolean unknownInputs = false;
		boolean isSpending = false;
		
		BigInteger receivedCoins = new BigInteger("0");
		BigInteger spentCoins = new BigInteger("0");
		
		long time = json.optLong("time");
		if(time == 0) {
			//this happens when a transaction hasn't even been confirmed once
			//so just use the current time
			time = System.currentTimeMillis() / 1000; //server sends us seconds, so our hold over time should be formatted the same way
		}
		transaction.setTxTime(time * 1000);
		transaction.setConfirmationCount(json.optLong("confirmations"));
	
		
		//go through each input of this transaction to see if it might be from us (a spend)
		JSONArray inputs = json.getJSONArray("vin");
		for(int x = 0; x < inputs.length(); x++) {
			JSONObject input = inputs.getJSONObject(x);
			
			String inputTxid = input.getString("txid");
			int outputIndex = input.getInt("vout");
			
			ArrayList<ZWTransactionOutput> matchingOutputs = ZWWalletManager.getInstance().getTransactionOutputs(coin, inputTxid, outputIndex);
			if(matchingOutputs.size() > 0) {
				//this means we have an output that matches this input, so it's a transaction where we are spending coins
				if(matchingOutputs.size() > 1) {
					//TODO -if there is more than one then this is some kind of multisig transaction
				}
				
				//these outputs by definition have to have the same value
				//so if there are more than one outputs, just get the first one and use it 
				ZWTransactionOutput matchingOutput = matchingOutputs.get(0);
				
				inputCoin = inputCoin.add(matchingOutput.getValue());
			}
			else {
				unknownInputs = true;
			}

		}//end for x
		
		//if any of the inputs are ours, then we are spending coins, regardless of what the outputs looks like
		if(inputCoin.compareTo(BigInteger.ZERO) > 0) {
			isSpending = true;
		}

		
		//read through all outputs
		JSONArray outputs = json.getJSONArray("vout");
		for(int x = 0; x < outputs.length(); x++) {
			
			JSONObject output = outputs.getJSONObject(x);
			JSONObject scriptPubKey = output.getJSONObject("scriptPubKey");
			JSONArray outputAddresses = scriptPubKey.getJSONArray("addresses");
			
			boolean usedValue = false;
			boolean isMultiSig = false;
		
			int addressCount = outputAddresses.length();
			
			if(outputAddresses.length() > 1) {
				isMultiSig = true;
			}
			
			for(int addressIndex = 0; addressIndex < addressCount; addressIndex++) {
				String outputAddressString = outputAddresses.getString(addressIndex);
				
				boolean ownedAddress = true;
				ZWAddress outputAddress = ZWWalletManager.getInstance().getAddress(coin, outputAddressString, true);
				if(outputAddress == null) {
					outputAddress = ZWWalletManager.getInstance().getAddress(coin, outputAddressString, false);
					ownedAddress = false;
				}
				
				if(outputAddress != null) {
					//this address is in our database, so we need to track the transaction data
					String valueString = output.getString("value");
					BigInteger value = convertToBigIntSafe(coin, valueString);
					
					int outputIndex = output.getInt("n");
					
					
					
					if(!outputAddress.isHidden()) {
						
						//also if we haven't already set some sort of note, use this address's label
						if(transaction.getNote() == null || transaction.getNote().length() == 0){
							transaction.setNote(outputAddress.getLabel());
						}
					}
					
					
					
					ZWTransactionOutput transactionOutput = new ZWTransactionOutput(outputAddressString, transactionId, outputIndex, value, isMultiSig);
					
					if(ownedAddress) {
						receivedOutputs.add(transactionOutput);
						if(!usedValue) {
							receivedCoins = receivedCoins.add(value);
						}
						if(!isSpending) {
							//if we're receiving coins, we display our address they're received on
							displayAddresses.add(outputAddress);
						}
					}
					else {
						if(!usedValue) {
							spentCoins = receivedCoins.add(value);
						}
						if(isSpending) {
							//if we're spending coins, we display the address we sent to
							displayAddresses.add(outputAddress);
						}
					}
				}
				else {
					//it's an address we don't know about, probably someone else's change, we don't care
				}
				
			}

		}
		

		//calculate total value of transaction and fees (if possible)
		if(isSpending) {

			//if we know about ALL the inputs, then we can calculate the fee
			if(!unknownInputs) {
				BigInteger fee = inputCoin.subtract(spentCoins).subtract(receivedCoins);
				transaction.setFee(fee);
			}
			else {
				//if this happens this is a very strange transaction
				//likely caused by importing private keys
				//there is no way we can know the fee, so just set to zero
				transaction.setFee(BigInteger.ZERO);
			}
			
			//spending is stored in the database as negative number, so subtract from zero
			transaction.setAmount(BigInteger.ZERO.subtract(inputCoin)); 
		}
		else {
			//we're receiving coins
			//we have no idea what the fee is
			transaction.setAmount(receivedCoins);
			transaction.setFee(BigInteger.ZERO);
		}

		
		//TODO here check if we have display address, unhide hidden addresses if needed
		
		ArrayList<String> displayStrings = new ArrayList<String>();
		for(ZWAddress address : displayAddresses) {
			
			if(!address.isHidden()) {
				
				displayStrings.add(address.getAddress());
				
				//also if we haven't already set some sort of note, use this address's label
				if(transaction.getNote() == null || transaction.getNote().length() == 0){
					transaction.setNote(address.getLabel());
				}
				
			}
		}
		
		if(displayStrings.size() == 0) {
			//if we have no display strings, it could be because we only have hidden addresses
			//this means that someone "returned" coins to one of our change addresses, or something strange like that
			//if this happens we can still use the coins, so unhide the address
			for(ZWAddress address : displayAddresses) {
				address.setHidden(false);
				address.setLabel("Returned Coins");
				ZWWalletManager.getInstance().updateAddress(address);
				
				displayStrings.add(address.getAddress());
			}
			
			if(displayStrings.size() > 0) {
				transaction.setNote("Returned Coins");
			}
			else {
				//we have no display addresses so we have no idea what's up with this transaction
				transaction.setNote("Unknown Transaction");
			}
		}
		
		//TODO -now we need to save the outputs we're using
		for(ZWTransactionOutput output : receivedOutputs) {
			ZWWalletManager.getInstance().addTransactionOutput(coin, output);
		}
		
		ZWWalletManager.getInstance().addTransaction(transaction);
	}
	
	//create transaction immediately after sending
	//need fee before we can use
	private static void createTransactionFromSpendResponse(ZWCoin coin, JSONObject json, String sentTo) throws JSONException{
		String hash = json.getString("txid");
		ZWTransaction transaction = new ZWTransaction(coin, hash);
		
		//send transaction just happened
		long time = System.currentTimeMillis() / 1000;
		transaction.setTxTime(time * 1000);
		transaction.setConfirmationCount(json.optLong("confirmations"));
		
		BigInteger fees = new BigInteger("0");
		BigInteger value = new BigInteger("0"); //calculate this by scanning inputs
		
		//set the display address to the address we sent to
		ArrayList<String> displayAddresses = new ArrayList<String>();
		displayAddresses.add(sentTo);
		
		ZWAddress sentToAddress = ZWWalletManager.getInstance().getAddress(coin, sentTo, false);
		if(sentTo != null) {
			if(transaction.getNote() == null || transaction.getNote().length() == 0){
				transaction.setNote(sentToAddress.getLabel());
			}
		}

		//we know this is a sending txn
		JSONArray outputs = json.getJSONArray("vout");
		for(int x = 0; x < outputs.length(); x++) {
			JSONObject output = outputs.getJSONObject(x);
			JSONObject scriptPubKey = output.getJSONObject("scriptPubKey");
			JSONArray outputAddresses = scriptPubKey.getJSONArray("addresses");
			boolean usedValue = false;
			for(int y = 0; y < outputAddresses.length(); y++) {
				String outputAddress = outputAddresses.getString(y);
				//if this output is to the address we sent to then minus it from the txn val
				if(outputAddress.equals(sentTo)) {
					if(!usedValue) {
						usedValue = true;
						String outputValue = output.getString("value");
						BigInteger outputValueInt = convertToBigIntSafe(coin, outputValue); 
						value = value.add(outputValueInt);
					}
				}
			} //endy
		} //end x
		transaction.setAmount(value);
		transaction.setFee(fees);
		transaction.setDisplayAddresses(displayAddresses);
		ZWWalletManager.getInstance().addTransaction(transaction);
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

	
	/**
	 * There seems to be a server bug where sometimes it's sending values as decimals instead of whole satoshis
	 * @param amount a string representing a number in either satoshis or coins
	 * @return a {@link BigInteger} for the string in satoshis
	 */
	private static BigInteger convertToBigIntSafe(ZWCoin coin, String amount) {
		
		if(amount.contains(".")) {
			if(amount.contains("e")) {
				BigInteger sciNotationFix = new BigDecimal(amount).toBigInteger();
				return sciNotationFix;
			}
			else {
				return coin.getAtomicUnits(new BigDecimal(amount));
			}
		}
		
		return new BigInteger(amount);
	}
	
	
}





