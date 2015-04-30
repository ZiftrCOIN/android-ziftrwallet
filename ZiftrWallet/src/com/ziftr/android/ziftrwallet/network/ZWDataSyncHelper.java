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

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWApplication;
import com.ziftr.android.ziftrwallet.ZWWalletManager;
import com.ziftr.android.ziftrwallet.crypto.ZWAddress;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWECDSASignature;
import com.ziftr.android.ziftrwallet.crypto.ZWECKey;
import com.ziftr.android.ziftrwallet.crypto.ZWRawTransaction;
import com.ziftr.android.ziftrwallet.crypto.ZWTransaction;
import com.ziftr.android.ziftrwallet.crypto.ZWTransactionOutput;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWDataSyncHelper {
	
	//key to save the server response of a send transaction to sign
	public static final String toSignResponseKey = "SERVER_RESPONSE_TO_SIGN";
	
	//last time we refreshed transaction history 
	public static HashMap<String, Long> lastRefreshed = new HashMap<String, Long>();
	
	//seconds to wait before autorefreshing
	public static final long REFRESH_TIME = 60;
	
	
	public static ZWRawTransaction fetchRawTransaction(ZWCoin coin, BigInteger feePerKb, BigInteger amount, List<String> inputs, String output, String changeAddress){
		ZLog.log("Sending " + amount + " coins to " + output);
		ZiftrNetworkManager.networkStarted();
		
		ZWRawTransaction rawTransaction = null;
		
		JSONObject spendJson = buildSpendPostData(inputs, output, amount, feePerKb.longValue(), changeAddress);
		
		ZLog.log("Spending coins with json: ", spendJson.toString());
		
		ZiftrNetRequest request = ZWApi.buildSpendRequest(coin.getType(), coin.getChain(), spendJson);
		
		String response = request.sendAndWait();
		ZLog.forceFullLog(response);		
		
		if( responseCodeOk(request.getResponseCode()) ) {
			try {
				JSONObject jsonResponse = new JSONObject(response);
				rawTransaction = createRawTransaction(coin, jsonResponse, amount, changeAddress, output);
			} 
			catch (Exception e) {
				ZLog.log("Exception creating raw transaction: ", e);
			}
			
			if(rawTransaction == null) {
				//this means we were able to talk to the server successfully, but for some reason
				//we were unable to create a proper raw transaction from the response
				//TODO -we can add more fine-grained messages here if we need
				rawTransaction = new ZWRawTransaction(coin, output, ZWRawTransaction.ERROR_CODE_BAD_DATA, null);
			}
		}
		else {
			rawTransaction = new ZWRawTransaction(coin, output, request.getResponseCode(), request.getResponseMessage());
		}

		ZiftrNetworkManager.networkStopped();

		return rawTransaction;
	}
	
	
	
	public static boolean signRawTransaction(ZWRawTransaction rawTransaction, String password) {
		
		ZiftrNetworkManager.networkStarted();
		
		Set<String> addressesSpentFrom = new HashSet<String>();
		boolean signingSuccessful = false;
		
		ZWCoin coin = rawTransaction.getCoin();
		JSONObject signedData = new JSONObject();
		
		try {
			JSONObject transaction = rawTransaction.getRawData().getJSONObject("transaction");
			JSONArray toSignList = transaction.getJSONArray("to_sign");
			for (int i=0; i< toSignList.length(); i++){
				JSONObject toSign = toSignList.getJSONObject(i);
	
				String signingAddressPublic = toSign.optString("address");
				addressesSpentFrom.add(signingAddressPublic);
				
				ZWECKey key = ZWWalletManager.getInstance().getKeyForAddress(coin, signingAddressPublic, password);
				toSign.put("pubkey", ZiftrUtils.bytesToHexString(key.getPubKey()));
	
				String stringToSign = toSign.getString("tosign");
				ZWECDSASignature signature = key.sign(stringToSign);
				
				String rHex = ZiftrUtils.bigIntegerToString(signature.r, 32);
				toSign.put("r", rHex);
				
				String sHex = ZiftrUtils.bigIntegerToString(signature.s, 32);
				toSign.put("s", sHex);
			}
			

			//now pack all the signing into a json object to send it off to the server
			signedData.put("transaction", transaction);
			ZLog.log("sending  this signed transaction to server :" + signedData);
		}
		catch(Exception e) {
			ZLog.log("Exception attempting to sign transactions: ", e);
		}
		
		
		ZiftrNetRequest signingRequest = ZWApi.buildSpendSigningRequest(coin.getType(), coin.getChain(), signedData);
		
		String response = signingRequest.sendAndWait();
		
		ZLog.log(signingRequest.getResponseCode());
		ZLog.log("Response from signing: ", response);
		
		if( responseCodeOk(signingRequest.getResponseCode()) ){
			try {
				//flag any addresses we spent from as having been spent from (so we don't reuse change addresses)
				for(String addressString : addressesSpentFrom) {
					ZWAddress address = ZWWalletManager.getInstance().getAddress(coin, addressString, true);
					address.setSpentFrom(true);
					ZWWalletManager.getInstance().updateAddress(address);
				}
				
				//update the transactions table with the data from this transaction
				try {
					JSONObject responseJson = new JSONObject(response);
					ZLog.log("Response from completed signing: ", responseJson);
					
					
					JSONObject transactionJson = responseJson.getJSONObject("transaction");
					createTransaction(coin, transactionJson);
					signingSuccessful = true;
				}
				catch(Exception e2) {
					ZLog.log("Exception parsing successful spend response: ", e2);
					
					//if the server said the send was successful, but we couldn't handle the response, 
					//then we just update our history and hope it's there
					downloadTransactionHistory(coin, false);
				}
			}
			catch(Exception e) {
				ZLog.log("Exception saving spend transaction: ", e);
			}
		
		}
	

		ZiftrNetworkManager.networkStopped();
		
		
		return signingSuccessful;
	}

	
	private static boolean responseCodeOk(int responseCode) {
		if(responseCode >= 200 && responseCode < 300) {
			return true;
		}
		
		return false;
	}
	
	
	public static void updateCoinData() {
		ZiftrUtils.runOnNewThread(new Runnable() {
			
			@Override
			public void run() {
				downloadCoinTypes();
				downloadMarketValues();
				
				ZiftrNetworkManager.dataUpdated();
			}
		});
	}

	
	private static void downloadCoinTypes(){
		ZiftrNetworkManager.networkStarted();
		
		ZiftrNetRequest request = ZWApi.buildCoinDataRequest();
		String response = request.sendAndWait();
		if (responseCodeOk(request.getResponseCode())){

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
	
	private static void downloadMarketValues(){
		ZiftrNetworkManager.networkStarted();
		ZiftrNetRequest request = ZWApi.buildMarketValueRequest();
		String response = request.sendAndWait();
		if ( responseCodeOk(request.getResponseCode()) ){
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

	
	public static void updateTransactionHistory(final List<ZWCoin> coins, final boolean autorefresh) {
		ZiftrUtils.runOnNewThread(new Runnable() {
			
			@Override
			public void run() {
				for(ZWCoin coin : coins) {
					downloadTransactionHistory(coin, autorefresh);
				}
				ZiftrNetworkManager.dataUpdated();
			}
		});
	}
	
	
	public static void updateTransactionHistory(final ZWCoin coin, final boolean autorefresh) {
		ZiftrUtils.runOnNewThread(new Runnable() {
			
			@Override
			public void run() {
				downloadTransactionHistory(coin, autorefresh);
				ZiftrNetworkManager.dataUpdated();
			}
		});
		
	}
	

	public static void downloadTransactionHistory(ZWCoin coin, boolean autorefresh) {
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
		if( responseCodeOk(request.getResponseCode()) ) {
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
		}//end if response code

		ZiftrNetworkManager.networkStopped();
		ZWDataSyncHelper.lastRefreshed.put(coin.getSymbol(), System.currentTimeMillis() / 1000);
	}

	
	//creates transaction in local database from json response
	private static void createTransaction(ZWCoin coin, JSONObject json) throws JSONException {	
		String transactionId = json.getString("txid");
		ZWTransaction transaction = new ZWTransaction(coin, transactionId);
		
		ArrayList<ZWAddress> displayAddresses = new ArrayList<ZWAddress>();
		ArrayList<ZWTransactionOutput> receivedOutputs = new ArrayList<ZWTransactionOutput>();
		//ArrayList<ZWTransactionOutput> spentOutputs = new ArrayList<ZWTransactionOutput>();
		
		BigInteger inputCoins = new BigInteger("0");
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
				
				inputCoins = inputCoins.add(matchingOutput.getValue());
			}
			else {
				unknownInputs = true;
			}

		}//end for x
		
		//if any of the inputs are ours, then we are spending coins, regardless of what the outputs looks like
		if(inputCoins.compareTo(BigInteger.ZERO) > 0) {
			isSpending = true;
		}

		
		//read through all outputs
		JSONArray outputs = json.getJSONArray("vout");
		for(int x = 0; x < outputs.length(); x++) {
			
			JSONObject output = outputs.getJSONObject(x);
			JSONObject scriptPubKey = output.getJSONObject("script_pub_key");
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
					transaction.setMultisig(isMultiSig);
					
					if(ownedAddress) {
						receivedOutputs.add(transactionOutput);
						if(!usedValue) {
							receivedCoins = receivedCoins.add(value);
							usedValue = true;
						}
						if(!isSpending) {
							//if we're receiving coins, we display our address they're received on
							displayAddresses.add(outputAddress);
						}
					}
					else {
						if(!usedValue) {
							spentCoins = spentCoins.add(value);
							usedValue = true;
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
				BigInteger fee = inputCoins.subtract(spentCoins).subtract(receivedCoins);
				transaction.setFee(fee);
			}
			else {
				//if this happens this is a very strange transaction
				//likely caused by importing private keys
				//there is no way we can know the fee, so just set to zero
				transaction.setFee(BigInteger.ZERO);
			}
			
			//we can't just use spent coins, because we also "spent" a fee
			BigInteger totalSpent = inputCoins.subtract(receivedCoins); 
			
			//spending is stored in the database as negative number, so subtract from zero
			transaction.setAmount(BigInteger.ZERO.subtract(totalSpent)); 
		}
		else {
			//we're receiving coins
			//we have no idea what the fee is
			transaction.setAmount(receivedCoins);
			transaction.setFee(BigInteger.ZERO);
		}

		//go through our used addresses and choose the ones to display in the UI
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
				address.setLabel(ZWApplication.getApplication().getString(R.string.zw_transaction_returned_coins));
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
		
		transaction.setDisplayAddresses(displayStrings);
		
		//now we need to save the outputs we're using
		for(ZWTransactionOutput output : receivedOutputs) {
			ZWWalletManager.getInstance().addTransactionOutput(coin, output);
		}
		
		ZWWalletManager.getInstance().addTransaction(transaction);
	}
	
	
	
	
	private static ZWRawTransaction createRawTransaction(ZWCoin coin, JSONObject json, BigInteger amount, String changeAddress, String outputAddress) throws JSONException {
		
		JSONObject transactionJson = json.getJSONObject("transaction");
		
		BigInteger inputCoins = new BigInteger("0");
		BigInteger change = new BigInteger("0");
		
		boolean spendOk = false;
		boolean changeOk = false;
		boolean hexOk = false;
		
		boolean unconfirmedInputs = false;
		
		
		//first go through all of the inputs and see how much we're spending
		JSONArray inputs = transactionJson.getJSONArray("vin");
		for(int x = 0; x < inputs.length(); x++) {
			JSONObject input = inputs.getJSONObject(x);
			
			String inputTxid = input.getString("txid");
			int outputIndex = input.getInt("vout");
			
			ArrayList<ZWTransactionOutput> matchingOutputs = ZWWalletManager.getInstance().getTransactionOutputs(coin, inputTxid, outputIndex);
			if(matchingOutputs.size() > 0) {
				
				//this means we found the output we're going to use to spend coins in this transaction
				
				if(matchingOutputs.size() > 1) {
					//if there is more than one then this is some kind of multisig transaction
					//we don't yet support this, but we continue building the raw transaction anyway, since there is no way it can harm the user
				}
				
				ZWTransactionOutput matchingOutput = matchingOutputs.get(0);
				
				//check to see if the transaction this output is part of has been fully confirmed
				ZWTransaction matchingOutputTransaction = ZWWalletManager.getInstance().getTransaction(coin, matchingOutput.getTransactionId());
				if(matchingOutputTransaction == null || matchingOutputTransaction.isPending()) {
					unconfirmedInputs = true;
				}
				
				inputCoins = inputCoins.add(matchingOutput.getValue());
			}
			else {
				//TODO this either means there is something wrong with our local database (so we'll be unable to sign this transaction)
				//or it means we're signing some sort of multi-sig transaction, which isn't supported yet
				ZLog.log("Warning: Unknown inputs in a transaction we're signing.");
			}

		}
		
		
		//now we have to go through all the outputs and make sure that this transaction is what the user actually asked for
		JSONArray outputs = transactionJson.getJSONArray("vout");
		
		int numOutputs = outputs.length();
		
		if(numOutputs == 1) {
			changeOk = true; //if there's only one output, just assume exact change
		}
		else if(numOutputs > 2) {
			//there should be at most two outputs, one for change, another for the recipient
			//if there are more something bad is happening
			return null;
		}
		for(int x = 0; x < outputs.length(); x++) {
			
			JSONObject output = outputs.getJSONObject(x);
			JSONObject scriptPubKey = output.getJSONObject("script_pub_key");
			JSONArray outputAddresses = scriptPubKey.getJSONArray("addresses");
			
			if(outputAddresses.length() != 1) {
				//if we have strange outputs with multiple addresses, that's unexpected behavior
				return null;
			}
		
			String addressString = outputAddresses.getString(0);
			
			String outputValueString = output.getString("value");
			BigInteger outputValue = convertToBigIntSafe(coin, outputValueString);
			
			if(addressString.equals(outputAddress)) {
				//this is the output for the address we're sending coins to
				//we need to make sure it all lines up
				
				if(outputValue.compareTo(amount) != 0) {
					//if the raw transaction output value to the new recipient doesn't match the expect one, that's a problem
					return null;
				}
				else {
					spendOk = true;
				}
			}
			else if(addressString.equals(changeAddress)) {
				change = outputValue;
				changeOk = true;
			}


		}
		
		String hex = transactionJson.getString("hex");
		if(hex.length() > 0) {
			hexOk = true;
			//TODO -we really need to verify this hex to be 100% sure everything is on the up and up
		}
		
		if(spendOk && changeOk && hexOk) {
			ZWRawTransaction rawTransaction = new ZWRawTransaction(coin, inputCoins, amount, change, outputAddress);
			rawTransaction.setRawData(json);
			rawTransaction.setUsingUnconfirmedInputs(unconfirmedInputs);
			return rawTransaction;
		}
		
		return null;
	}
	
	
	private static JSONObject buildSpendPostData(List<String> inputs, String output, BigInteger amount, long feePerKb, String changeAddress) {
		JSONObject postData = new JSONObject();
		JSONObject transaction = new JSONObject();
		try {
			transaction.put("fee_per_kb", feePerKb);
			transaction.put("surplus_refund_address", changeAddress);
			JSONArray inputAddresses = new JSONArray();
			for (String addr : inputs){
				inputAddresses.put(addr);
			}
			
			transaction.put("input_addresses", inputAddresses);
			JSONObject outputAddresses = new JSONObject();
			
			outputAddresses.put(output, amount.longValue());
			
			transaction.put("output_addresses", outputAddresses);
			
			//now put the entire transaction object into the jsonData object to be sent to the server
			postData.put("transaction", transaction);
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





