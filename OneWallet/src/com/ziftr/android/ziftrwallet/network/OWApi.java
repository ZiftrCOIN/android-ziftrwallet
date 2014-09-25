package com.ziftr.android.ziftrwallet.network;

import java.util.List;

import com.google.common.base.Joiner;
import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.util.ZLog;


public class OWApi {

	
	private static final String BASE_URL = "http://mocksvc.mulesoft.com/mocks/027762f2-ebf1-4866-a693-0f320b288ede/mocks/1e572f44-bcd7-479b-9b8f-cd1f6e6a07e0";
	
	
	
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
		query.add("addresses", "mocktest");
		
		String url = buildBaseUrl(type.toUpperCase(), chain) + "transactions";
		ZLog.log("Transaction request url: ", url);
		ZiftrNetRequest request = ZiftrNetRequest.createRequest(url, query);
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
