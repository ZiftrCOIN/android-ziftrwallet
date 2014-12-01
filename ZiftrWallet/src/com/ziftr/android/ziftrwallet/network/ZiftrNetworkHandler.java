package com.ziftr.android.ziftrwallet.network;

public interface ZiftrNetworkHandler {
	

	/**
	 * Called when the network detects an expired login token (if token use is supported).
	 * The underlying network code will attempt to automatically acquire a new token if possible before calling this.
	 * At this point another method of getting a new token must be used (such as having the user login again).
	 */
	public void handleExpiredLoginToken();
	
	/**
	 * Background data has been updated and the implementing class should inform the relevant parts of the app (such as the UI) that it may need to refresh.
	 */
	public void handleDataUpdated();
	
	
	/**
	 * Informs the implementing class that network activity (or other long running background tasks) has started.
	 * The implementing class can use this method to do things such as begin an animation or disable certain UI elements
	 * during the network access.
	 * Note: This is only called once regardless of number of actual connections made and is not nesting. For example if
	 * three background threads "simultaneously" begin network activity this will only be called a single time.
	 */
	public void networkStarted();
	
	
	/**
	 * Informs the implementing class that all network activity has stopped.
	 * Implementing classes can use this method to do work such as stopping animations or enabling UI.
	 * Note: This is only called once and is not nesting. Even if there were m threads performing
	 * long running tasks this method would only be called when the last one finishes.
	 */
	public void networkStopped();


	public void handleNetworkError(String errorMessage);
	
	
}
