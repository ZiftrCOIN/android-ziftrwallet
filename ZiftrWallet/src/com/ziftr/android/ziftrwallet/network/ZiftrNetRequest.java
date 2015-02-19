package com.ziftr.android.ziftrwallet.network;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import android.util.Log;

import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;



/**
 * This is a wrapper class around {@link HttpURLConnection} that allows for simple network calls without
 * the messy connection setup code or stream management.
 * Use the factory methods to create an instance, which is re-usable for the same url/data. It can upload a variety of data types and do 
 * network transaction on and off the current thread.
 * @author justin
 *
 */
public class ZiftrNetRequest {
	
	String urlString = null;
	InputStream uploadDataStream = null;
	String responseData = null;
	Exception error = null;
	
	// Hold on to the most recent connection
	HttpURLConnection lastConnection; 
	
	ZiftrParamList queryParameters = null;
	Map<String,String> sentHeaders = null;
	
	private String forcedRequestMethod = null;
	
	// If the network call should attempt to use the user info for authorization automatically
	private boolean useAutoAuthorization = true;
	
	// Make sure no matter what wacky stuff the server might send we never have an endless refresh loop
	// private boolean refreshLoop = false; 
 
	
	/**
	 * Create a network request for a given url
	 * @param url the url this network request is for
	 * @return a network request object with default settings pointing to the url
	 */
	public static ZiftrNetRequest createRequest(String url) {
		ZiftrNetRequest request = new ZiftrNetRequest(url);
		return request;
	}
	
	/**
	 * Create a network request for a given url
	 * @param url the url this network request is for
	 * @return a network request object with default settings pointing to the url
	 */
	public static ZiftrNetRequest createRequest(String url, Map<String,String> headers) {
		ZiftrNetRequest request = new ZiftrNetRequest(url);
		request.sentHeaders = headers;
		return request;
	}
	
	/**
	 * Create a network request for a given url with a string of post data
	 * @param url the url to send the data to
	 * @param postData post data as a String
	 * @return a network request for sending the data, request will be a POST
	 */
	public static ZiftrNetRequest createRequest(String url, String postData) {
		ZiftrNetRequest request = new ZiftrNetRequest(url);
		request.setUploadData(postData);
		return request;
	}
	
	/**
	 * Create a network request for a given url with a list of query parameters
	 * @param url the url for this network request
	 * @param queryParams a {@link ZiftrParamList} of query parameters
	 * @return a network request for the url with the query parameters added
	 */
	public static ZiftrNetRequest createRequest(String url, ZiftrParamList queryParams) {
		ZiftrNetRequest request = new ZiftrNetRequest(url);
		request.queryParameters = queryParams;
		return request;
	}
	
	
	/**
	 * Create a network request for a given url with headers query parameters and post data.
	 * @param url the url for this network request
	 * @param headers a map of headers to use, can be null for no additional headers
	 * @param queryParams a {@link ZiftrParamList} of query parameters to add to the request
	 * @param postData post data to send, can be null, request will be a GET with null post data, pass an empty string for a POST with no data
	 * @return a network request setup with the passed in parameters
	 */
	public static ZiftrNetRequest createRequest(String url, Map<String,String> headers, ZiftrParamList queryParams) {
		ZiftrNetRequest request = new ZiftrNetRequest(url);
		request.queryParameters = queryParams;
		request.sentHeaders = headers;
		return request;
	}
	
	
	/**
	 * Create a network request for a given url with headers query parameters and post data.
	 * @param url the url for this network request
	 * @param headers a map of headers to use, can be null for no additional headers
	 * @param queryParams a {@link ZiftrParamList} of query parameters to add to the request
	 * @param postData post data to send, can be null, request will be a GET with null post data, pass an empty string for a POST with no data
	 * @return a network request setup with the passed in parameters
	 */
	public static ZiftrNetRequest createRequest(String url, Map<String,String> headers, ZiftrParamList queryParams, String postData) {
		ZiftrNetRequest request = new ZiftrNetRequest(url);
		request.queryParameters = queryParams;
		request.setUploadData(postData);
		request.sentHeaders = headers;
		return request;
	}
	
	/**
	 * Create a network request for a given url with headers query parameters and post data. 
	 * Takes a map of post data which will be formatted properly instead of a String
	 * @param url the url for this network request
	 * @param headers a map of headers to use, can be null for no additional headers
	 * @param queryParams a {@link ZiftrParamList} of query parameters to add to the request
	 * @param postData post data to send, can be null, request will be a GET with null post data
	 * @return a network request setup with the passed in parameters
	 */
	public static ZiftrNetRequest createRequest(String url, Map<String,String> headers, ZiftrParamList queryParams, Map<String,String> postData) {
		ZiftrNetRequest request = new ZiftrNetRequest(url);
		request.queryParameters = queryParams;
		request.setUploadData(postData);
		request.sentHeaders = headers;
		return request;
	}
	
	
	/**
	 * Create a network request for a given url with headers query paramters and using json as the post data.
	 * @param url the url for this network request
	 * @param headers headers a map of headers to use, can be null for no additional headers
	 * @param queryParams a {@link ZiftrParamList} of query parameters to add to the request
	 * @param postData a {@link JSONObject} containing the data to send
	 * @return
	 */
	public static ZiftrNetRequest createRequest(String url, Map<String,String> headers, ZiftrParamList queryParams, JSONObject postData) {
		ZiftrNetRequest request = new ZiftrNetRequest(url);
		request.queryParameters = queryParams;
		if (headers != null){
			request.sentHeaders = headers;
		}
		if(postData != null) {
			request.setUploadData(postData.toString());
			if(request.sentHeaders != null && request.sentHeaders.get("Content-Type") == null) {
				request.sentHeaders.put("Content-Type", "application/json");
			}
		}
		
		return request;
	}
	
	
	
	private ZiftrNetRequest(String url) {
		this.urlString = url;
	}
	
	
	/**
	 * forces this request to use a different request method eg POST or GET
	 * @param requestMethod the request type as a String
	 */
	public void forceRequestMethod(String requestMethod) {
		this.forcedRequestMethod = requestMethod;
	}
	
	
	public void setContentType(String contentType) {
		
	}
	
	
	public void setUploadData(String data) {
		
		if (data != null) {
			try {
				this.uploadDataStream = new ByteArrayInputStream(data.getBytes("UTF-8"));
			} 
			catch (UnsupportedEncodingException e) {
				this.error = e;
			}
		}
	}
	
	public void setUploadData(Map<String, String> data) {
		
		if (data != null) {
			ArrayList<NameValuePair> postParams = new ArrayList<NameValuePair>(data.size());
			for(Map.Entry<String, String> entry : data.entrySet()) {
				postParams.add( new BasicNameValuePair(entry.getKey(), entry.getValue()));
			}
	
			this.setUploadData(postParams);
		}
		
	}
	
	public void setUploadData(NameValuePair data) {
		
		if (data != null) {
			ArrayList<NameValuePair> arrayList = new ArrayList<NameValuePair>(1);
			arrayList.add(data);
			
			this.setUploadData(data);
		}
	}
	
	public void setUploadData(List<NameValuePair> data) {
		if (data != null) {
			UrlEncodedFormEntity entityData;
			try {
				entityData = new UrlEncodedFormEntity(data);
				
				//this is the proper way to debug this and have a developer read this data
				//however make SURE it's commented out before any release as it would print usernames and passwords to the android log
				//which any app can read
				//String uploadDataString = ZiftrUtils.streamToString(entityData.getContent());
				//ZLog.log("POST Body: ", uploadDataString);
				
				this.uploadDataStream = entityData.getContent();
			} 
			catch (IOException e) {
				this.error = e;
			}
		}
	}
	
	public void setUploadData(InputStream data) {
		this.uploadDataStream = data;
	}
	
	
	public void setUseAutoAuthorization(boolean useAutoAuthorization) {
		this.useAutoAuthorization = useAutoAuthorization;
	}
	
	public boolean getUseAutoAuthorization() {
		return this.useAutoAuthorization;
	}
	
	public String sendAndWait() {
		
		try {
			ZiftrNetworkManager.networkStarted();
			this.doNetworkRequest();
			
		} 
		catch (IOException e) {
			this.error = e;
		}
		
		ZiftrNetworkManager.networkStopped();
		
		return responseData;
	}
	
	
	public void send(final ZiftrNetworkCallback callback) {
		
		ZiftrUtils.runOnNewThread(new Runnable() {
			
			@Override
			public void run() {
				String responseString = ZiftrNetRequest.this.sendAndWait();
				callback.networkRequestFinished(responseString);
			}
		});
	}
	
	
	public void downloadToFile(ZiftrNetworkCallback callback) {
		//TODO -implement this when/if we need to download files for our application to use
		ZLog.log("Direct file downloading using ZiftrNetRequest not yet implemented.");
	}
	
	
	
	
	private void doNetworkRequest() throws IOException {
	
		HttpURLConnection connection = null;
		
		boolean needsRefresh = false;
		
		try {
			
			//boolean tokenSet = false;
			
			String fullUrl = urlString;
			
			if (queryParameters != null) {
				//create arguments string
				StringBuilder args = new StringBuilder("?");
				for(int x = 0; x < queryParameters.size(); x++) {
					args.append(queryParameters.getName(x));
					String value = queryParameters.getValue(x);
					if (value != null) {
						args.append("=").append(URLEncoder.encode(value, "UTF-8"));
					}
					args.append("&");
				}
				
				//remove either the last &, or if the arguments list was empty the unnecessary ?
				args.deleteCharAt(args.length()-1);
				
				fullUrl += args.toString();
			}
			
			URL url = new URL(fullUrl);
			
			ZLog.log("Opening http connection to: ", fullUrl);
			connection = (HttpURLConnection) url.openConnection();
			
			//TODO -short hack for now setting the user-agent to the "default android user agent"
			String userAgent = System.getProperty("http.agent");
			//userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/534.55.3 (KHTML, like Gecko) Version/5.1.3 Safari/534.53.10";
			connection.setRequestProperty("User-Agent", userAgent);
			
			//setup authorization if the user is currently logged in
			//TODO -code for handling using login auth here, we don't need this, yet...
			/**********
			if (useAutoAuthorization && ZiftrUser.isLoggedIn()) {
				//only auto-include authorization on secure calls, this way basic calls won't fail due to expired login credentials
				if (fullUrl.startsWith("https")) {
					tokenSet = true;
					String token = ZiftrUser.getUserToken();
					connection.setRequestProperty("Authorization", "Bearer " + token);
				}
			}
			********/
			
			if (sentHeaders != null) {
				for(String key : sentHeaders.keySet()) {
					connection.setRequestProperty(key, sentHeaders.get(key));
				}
			}
			
			if (forcedRequestMethod != null) {
				connection.setRequestMethod(forcedRequestMethod);
			}
			
			
			if (this.uploadDataStream != null) {
				//if we're sending data, make sure we're a post (if need be)
				if (forcedRequestMethod == null) {
					connection.setRequestMethod("POST");
				}
				connection.setRequestProperty("Content-Type", "application/json");
				connection.setDoOutput(true);
				
				OutputStream outputStream = new BufferedOutputStream(connection.getOutputStream());
				
				//String test = ZiftrUtils.streamToString(this.uploadDataStream);
				
				//pipe our uploadData to the output stream
				//note, this starts the connection, so no more changes can be made to it after this
				ZiftrUtils.pipeStreams(this.uploadDataStream, outputStream);
			}
			
			
			String response = null;
			
			try {
				InputStream stream = connection.getInputStream();
				response = ZiftrUtils.streamToString(stream);
			} catch(Exception e) {
				try {
					response = ZiftrUtils.streamToString(connection.getErrorStream());
				} catch(Exception e2) {
					//don't care much about this, just trying to see if we can extra response info
				}
				
				if(response == null || response.length() == 0) {
					try {
						int responseCode = connection.getResponseCode();
						String responseMessage = connection.getResponseMessage();
						response = "Error: " + String.valueOf(responseCode) + " - " + responseMessage;
					}
					catch(Exception e3) {
						//dont' care just trying to get failure information
					}
					
					if(response == null || response.length() == 0) {
						//if the response is still null, just jam the exception into it
						response = Log.getStackTraceString(e);
						ZLog.log("Exception getting response from server: ", e);
					}
				}
				
				
				//TODO -code for handling expired login here, we don't need this, yet...
				/***
				if (tokenSet && ZiftrUser.isLoggedIn() && connection.getResponseCode() == 403 && !refreshLoop) {
					//if this happened, our token probably expired, let's take a minute to try and get a new one
					ZLog.log("Current user token has expired. Attempting automatic refresh...");
					boolean refreshed = refreshUserToken();
					if (!refreshed) {
						ZiftrUser.deleteUser();
						ZiftrNetworkManager.showLoginExpired();
					}
					else {
						refreshLoop = true;
						needsRefresh = true;
					}
				}
				****/
			}
		
			this.responseData = response;
		}
		catch(Exception e) {
			//to get here it means there's a problem in the code itself (malformed url for example), so just log the exception
			ZLog.log("Ziftr Network Request Exception: ", e);
		}
		finally {
			try {
				this.lastConnection = connection;
				connection.disconnect();
			}
			catch(Exception e) {
				//just don't explode, it's ok if disconnecting failed
			}
			
			if (needsRefresh) {
				doNetworkRequest(); //recursively try again, but just once due to refreshLoop lock
			} else {
				//unlock
				//refreshLoop = false;
			}
		}
	
	}
	
	
	//TODO this method is from the ZiftrAlerts app but wasn't well encapsulated, leaving it here as our API likely will need user tokens of some sort eventually
	/*******************************************************
	private boolean refreshUserToken() {
		
		ZiftrNetRequest request = ZiftrApi.buildRefreshTokenRequest(ZiftrUser.getRefreshToken());
		request.setUseAutoAuthorization(false);
		
		String tokenJsonString = request.sendAndWait();
		JSONObject tokenJson = null;
		
		try{
			tokenJson = new JSONObject(tokenJsonString);
		}
		catch(Exception e) {
			ZLog.log("Excpetion parsing token refresh response: ", e);
		}
		
		if (tokenJson != null) {
			String accessToken = tokenJson.optString("access_token");
			String refreshToken = tokenJson.optString("refresh_token");
			long expirationTime = tokenJson.optLong("expires_in");
			expirationTime = expirationTime * 1000; //we want ms, server gives us seconds
			expirationTime += System.currentTimeMillis(); //server sends us back a duration, not an exact time
			
			if (accessToken !=  null) {
				//verify the token and get the rest of the user's login data
				ZiftrNetRequest verifyRequest = ZiftrApi.buildVerifyRequest(accessToken);
				String verifyResponse = verifyRequest.sendAndWait();
				
				JSONObject verificationJson = null;
				
				try {
					verificationJson = new JSONObject(verifyResponse);
				}
				catch(Exception e) {
					ZLog.log("Excpetion parsing token refresh response: ", e);
				}
				
				if (verificationJson != null) {
					
					JSONObject user = verificationJson.optJSONObject("User");
					if (user != null) {
						String id = user.optString("id");
						String email = user.optString("mail");
						String displayName = user.optString("display_name");
						String firstName = user.optString("first_name");
						String lastName = user.optString("last_name");
						
						ZiftrUser.setupUser(id, displayName, email, firstName, lastName, accessToken, refreshToken, expirationTime);
						return true;
					}
				}
			}
		}
		return false;
	}
	********************************/
	
	
	public String getResponseData() {
		return responseData;
	}
	
	
	public int getResponseCode() {
		if (this.lastConnection != null) {
			try {
				return this.lastConnection.getResponseCode();
			} 
			catch (IOException e) {
				ZLog.log(e);
			}
		}
		
		ZLog.log("Trying to get response code before a connection was made, or when a connection failed.");
		return -1;
	}
	
	
	public String getResponseMessage() {
		if (this.lastConnection != null) {
			try {
				return this.lastConnection.getResponseMessage();
			} 
			catch (IOException e) {
				ZLog.log(e);
			}
		}
		
		ZLog.log("Trying to get response message before a connection was made, or when connection failed.");
		return "";
	}
	
	public Map<String, List<String>> getResponseHeaders() {
		if (this.lastConnection != null) {
			return this.lastConnection.getHeaderFields();
		}
		
		ZLog.log("Trying to get response headers before a connection was made, or when connection failed.");
		return null;
	}
	
}
