package com.ziftr.android.ziftrwallet.network;

public interface ZiftrNetworkHandler {
	
	
	public void handleExpiredLoginToken();
	
	public void handleGenericError(String errorMessage);
	
	/**
	 * Background data has been updated and the implementing class should inform the UI that it may need to refresh.
	 * If notifyNetworkManager is true, the network manager has background threads waiting for the UI to update based on this data change.
	 * Note: It's the responsibility of the implementing class to call back to the network manager when the UI is done.
	 * @param notifyNetworkManager should the calling code call back to the network manager when done updating UI
	 */
	public void handleDataUpdated();
	
	
	public void networkStarted();
	public void networkStopped();

}
