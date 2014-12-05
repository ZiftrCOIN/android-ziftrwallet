package com.ziftr.android.ziftrwallet.network;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import android.annotation.SuppressLint;

import com.google.common.base.Joiner;
import com.ziftr.android.ziftrwallet.util.ZLog;


@SuppressLint("DefaultLocale")
public class ZWApi {

	
	private static final String MOCK_BASE_URL = "mocksvc.mulesoft.com/mocks/fa5dec6e-b96d-46b7-ac46-28624489f7dd";
	private static final String SANDBOX_BASE_URL = "sandbox.fpa.bz";
	private static final String LIVE_BASE_URL = "api.fpa.bz";
	
	private static final String BASE_URL = SANDBOX_BASE_URL;
	
	
	private static final String AUTHORIZATION = "Basic dGVzdGtleTo=";
	private static final String ACCEPT = "application/vnd.ziftr.fpa.v1+json";
	
	
	private static String buildBaseUrl(String type, String chain) {
		return buildBaseUrl(type, chain, false);
	}
	
	private static String buildBaseUrl(String type, String chain, boolean secure) {
		String protocol;
		if(secure) {
			protocol = "https";
		}
		else {
			protocol = "http";
		}
		
		return protocol + "://" + BASE_URL + "/blockchains/" + type + "/" + chain + "/";
	}
	
	
	private static Map<String, String> buildGenericHeaders() {
		Map<String, String> headers = new HashMap<String, String>(2);
		
		headers.put("Accept", ACCEPT);
		headers.put("Authorization", AUTHORIZATION);
		
		return headers;
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
		
		//TODO use URLEncoder.encode for each address, this convenient shortcut would break stuff
		//if there's ever a coin with illegal characters allowed in the addresses for some reason
		String addressesList = Joiner.on(',').join(addresses);
		
		ZiftrParamList query = new ZiftrParamList();
		query.add("addresses", addressesList);
		
		String url = buildBaseUrl(type.toLowerCase(), chain) + "transactions";
		ZLog.log("Transaction request url: ", url);
		ZiftrNetRequest request = ZiftrNetRequest.createRequest(url, buildGenericHeaders(), query);
		return request;
	}
	
	
	
	
	/**
	 * create transaction request blockchains /{type} /{chain} /transactions /requests POST
	 * @param type
	 * @param chain
	 * @return
	 */
	public static ZiftrNetRequest buildSpendRequest(String type, String chain, JSONObject spendPostData){
		
		String url = buildBaseUrl(type.toLowerCase(), chain) + "transactions/requests";
		
		ZiftrNetRequest request = ZiftrNetRequest.createRequest(url, buildGenericHeaders(), null,  spendPostData);
		return request;
	}
	
	// POST signed txn /blockchains /{type} /{chain} /transactions
	public static ZiftrNetRequest buildSpendSigningRequest(String type, String chain, JSONObject signedTxn){
		String url = buildBaseUrl(type.toLowerCase(), chain) + "transactions";
		
		ZiftrNetRequest request = ZiftrNetRequest.createRequest(url, buildGenericHeaders(), null,  signedTxn);
		return request;
	}

	//GET /blockchains /market_value
	public static ZiftrNetRequest buildMarketValueRequest(){
		String url = "http://" + BASE_URL + "/blockchains/market_values";

		ZiftrNetRequest request = ZiftrNetRequest.createRequest(url, buildGenericHeaders(), null);
		return request;
	}
	
	
	
	/**
	 * builds a network request for any url by appending the passed in string to the API's base url
	 * @param urlPart the part of the url to append to the base api url to create the network request
	 * @return a network request object for the full url, will be a GET and have no additional parameters
	 */
	public static ZiftrNetRequest buildGenericApiRequest(boolean secure, String urlPart) {
		String protocol;
		protocol = secure ? "https" : "http";
		ZiftrNetRequest request = ZiftrNetRequest.createRequest(buildGenericHeaders() ,protocol + "://" + BASE_URL + urlPart);
		return request;
	}

}
