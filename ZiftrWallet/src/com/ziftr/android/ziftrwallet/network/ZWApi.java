package com.ziftr.android.ziftrwallet.network;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.util.Base64;

import com.google.common.base.Joiner;
import com.ziftr.android.ziftrwallet.util.ZLog;


@SuppressLint("DefaultLocale")
public class ZWApi {

	
	private static final String MOCK_BASE_URL = "mocksvc.mulesoft.com/mocks/fa5dec6e-b96d-46b7-ac46-28624489f7dd";
	private static final String SANDBOX_BASE_URL = "sandbox.fpa.bz";
	private static final String LIVE_BASE_URL = "api.fpa.bz";
	
	private static final String BASE_URL = SANDBOX_BASE_URL;
	
	
	private static final String AUTHORIZATION = "Basic " + Base64.encodeToString("pub_c905a5608ce7ec9b63dbcb60c947921c:".getBytes(), Base64.DEFAULT);
	private static final String ACCEPT = "application/vnd.ziftr.fpa-0-1+json";
	
	
	private static String buildUrl(String type, String chain, String endPoint) {
		return buildUrl(type, chain, endPoint, true);
	}
	
	
	private static String getAuthorization() {
		return AUTHORIZATION;
	}
	
	
	private static String buildUrl(String type, String chain, String endPoint, boolean secure) {
		String protocol;
		if(secure) {
			protocol = "https";
		}
		else {
			protocol = "http";
		}
		
		StringBuilder url = new StringBuilder(protocol).append("://").append(BASE_URL).append("/blockchains/"); //  + type + "/" + chain + "/";
		
		if(type != null) {
			url.append(type).append("/");
		}
		if(chain != null) {
			url.append(chain).append("/");
		}
		if(endPoint != null) {
			url.append(endPoint).append("/");
		}
		
		return url.toString();
	}
	
	
	private static Map<String, String> buildGenericHeaders() {
		Map<String, String> headers = new HashMap<String, String>(2);
		
		headers.put("Accept", ACCEPT);
		headers.put("Authorization", getAuthorization());
		
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
		
		String url = buildUrl(type.toLowerCase(), chain, "transactions");
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
		
		String url = buildUrl(type.toLowerCase(), chain, "transactions/requests");
		
		ZiftrNetRequest request = ZiftrNetRequest.createRequest(url, buildGenericHeaders(), null,  spendPostData);
		return request;
	}
	
	// POST signed txn /blockchains /{type} /{chain} /transactions
	public static ZiftrNetRequest buildSpendSigningRequest(String type, String chain, JSONObject signedTxn){
		String url = buildUrl(type.toLowerCase(), chain, "transactions");
		
		ZiftrNetRequest request = ZiftrNetRequest.createRequest(url, buildGenericHeaders(), null,  signedTxn);
		return request;
	}

	
	//GET /blockchains /market_value
	public static ZiftrNetRequest buildMarketValueRequest(){
		String url = buildUrl(null, null, "market_values");
		ZiftrNetRequest request = ZiftrNetRequest.createRequest(url, buildGenericHeaders(), null);
		return request;
	}
	
	public static ZiftrNetRequest buildCoinDataRequest() {
		//coin data just comes from basic block chain request
		return buildBlockchainsRequest();
	}
	
	//GET /blockchains
	public static ZiftrNetRequest buildBlockchainsRequest() {
		String url = buildUrl(null, null, null); //blockchains is actually the base endpoint for the wallet api, so add nothing to url
		
		ZiftrNetRequest request = ZiftrNetRequest.createRequest(url, buildGenericHeaders());
		return request;
	}
	

}


