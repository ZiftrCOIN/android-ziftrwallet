package com.ziftr.android.onewallet.network;

import java.util.List;

public class OWApi {

	
	private static final String BASE_URL = "http://mocksvc.mulesoft.com/mocks/1e572f44-bcd7-479b-9b8f-cd1f6e6a07e0";
	
	
	
	private static String buildBaseUrl(String type, String chain) {
		return BASE_URL + "/" + type + "/" + chain + "/";
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
	public static ZiftrNetRequest buildTransactionsRequest(String type, String chain, List<String> address) {
		
		ZiftrParamList query = new ZiftrParamList();
		query.add("", "");
		
		ZiftrNetRequest request = ZiftrNetRequest.createRequest(buildBaseUrl(type, chain), query);
		return request;
	}









}
