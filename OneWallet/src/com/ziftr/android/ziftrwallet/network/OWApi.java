package com.ziftr.android.ziftrwallet.network;

import java.math.BigInteger;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Joiner;
import com.ziftr.android.ziftrwallet.util.ZLog;


public class OWApi {

	
	private static final String BASE_URL = "http://mocksvc.mulesoft.com/mocks/fa5dec6e-b96d-46b7-ac46-28624489f7dd";
	
	
	
	private static String buildBaseUrl(String type, String chain) {
		return BASE_URL + "/blockchains/" + type + "/" + chain + "/";
	}
	
	
	/**
	 * Description
	 * 
	 * create network request for blockchains /{type} /{chain} /transactions GET
	 * 
	 * @param type
	 * @param chain
	 * @param address
	 * @return
	 */
	public static ZiftrNetRequest buildTransactionsRequest(String type, String chain, List<String> addresses) {
		
		String addressesList = Joiner.on(',').join(addresses);
		
		ZiftrParamList query = new ZiftrParamList();
		query.add("addresses", addressesList);
		
		String url = buildBaseUrl(type.toUpperCase(), chain) + "transactions";
		ZLog.log("Transaction request url: ", url);
		ZiftrNetRequest request = ZiftrNetRequest.createRequest(url, query);
		return request;
	}
	/**
	 * create transaction request blockchains /{type} /{chain} /requests POST
	 * @param type
	 * @param chain
	 * @param fee = fee per kb
	 * @param refundAddress = newly generated hidden address where change from spent from address goes
	 * @param inputs = addresses we are sending from
	 * @param output = address to send to
	 * @return
	 */
	public static ZiftrNetRequest makeTransactionRequest(String type, String chain, BigInteger fee, 
			String refundAddress, List<String> inputs, String output, BigInteger amount){
		String url = buildBaseUrl(type.toUpperCase(), chain) + "transactions/requests";
		try {
			JSONObject body = new JSONObject();
			body.put("fee_per_kb", fee);
			body.put("surplus_refund_address", refundAddress);
			JSONArray inputAddresses = new JSONArray();
			for (String addr : inputs){
				inputAddresses.put(addr);
			}
			body.put("input_addresses", inputAddresses);
			JSONArray outputAddresses = new JSONArray();
			outputAddresses.put(new JSONObject().put("address", output).put("amount", amount));
			body.put("output_addresses", outputAddresses);
			ZiftrNetRequest request = ZiftrNetRequest.createRequest(url, null, null,  body);
			return request;
		} catch (Exception e) {
			ZLog.log("something is not working JSON");		
			e.printStackTrace();
		}
		return null;
	}
	
	// POST signed txn /blockchains /{type} /{chain} /transactions
	public static ZiftrNetRequest makeTransaction(String type, String chain, JSONObject signedTxn){
		String url = buildBaseUrl(type.toUpperCase(), chain) + "transactions";
		ZiftrNetRequest request = ZiftrNetRequest.createRequest(url, null, null,  signedTxn);
		return request;
	}

	
	/**
	 * builds a network request for any url by appending the passed in string to the API's base url
	 * @param urlPart the part of the url to append to the base api url to create the network request
	 * @return a network request object for the full url, will be a GET and have no additional parameters
	 */
	public static ZiftrNetRequest buildGenericApiRequest(String urlPart) {
		ZiftrNetRequest request = ZiftrNetRequest.createRequest(BASE_URL + urlPart);
		return request;
	}

}
