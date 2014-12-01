package com.ziftr.android.ziftrwallet.network;

import java.lang.ref.WeakReference;

public class ZiftrNetworkManager {
	
	private static WeakReference<ZiftrNetworkHandler> currentHandler = new WeakReference<ZiftrNetworkHandler>(null);
	
	private static boolean pendingExpiredLoginError = false;
	
	private static int networkCounter = 0;
	private static boolean networkActive = false;
	private static Thread stopThread = null;
	private static long stopTime = 0;
	
	
	/**
	 * registers the passed in object as the network handler, which will be notified of important network events
	 * note only one object can be the registered handler so setting this will clear any existing ones
	 * also a weak reference to the network handler is kept (to prevent leaking since the network manager lives for
	 * the entire lifetime of the app), so avoid using anonymous inner classes as the handler
	 * @param networkHandler
	 */
	public static void registerNetworkHandler(ZiftrNetworkHandler networkHandler) {
		currentHandler = new WeakReference<ZiftrNetworkHandler>(networkHandler);
		
		if(pendingExpiredLoginError) {
			showLoginExpired();
		}
		if(networkActive) {
			networkHandler.networkStarted();
		}
		else {
			networkHandler.networkStopped();
		}
	}
	
	
	public static void unRegisterNetworkHandler(ZiftrNetworkHandler errorHandler) {
		
		if(currentHandler.get() != null && currentHandler.get() == errorHandler) {
			currentHandler = new WeakReference<ZiftrNetworkHandler>(null);
		}
	}
	
	
	public static void showGenericError(String errorMessage) {
		//TODO handle different error types
	}
	
	public static void showNoNetworkError() {
		
	}
	
	public static void showLoginExpired() {
		ZiftrNetworkHandler handler = currentHandler.get();
		if(handler != null) {
			handler.handleExpiredLoginToken();
			pendingExpiredLoginError = false;
		}
		else {
			pendingExpiredLoginError = true;
		}
	}

	public static void dataUpdated() {

		ZiftrNetworkHandler handler = currentHandler.get();
		if(handler != null) {
				handler.handleDataUpdated();
		}
		
	}

	/**
	 * tell the registered network handler that network activity has started, note this is thread safe using a counter,
	 * so this should be called by any thread whenever it uses the network (if it wants the the handler to know about it)
	 */
	public static synchronized void networkStarted() {
		networkCounter++;
		if(networkCounter > 0 && !networkActive) {
			networkActive = true;
			ZiftrNetworkHandler handler = currentHandler.get();
			if(handler != null) {
				handler.networkStarted();
			}
		}
	}
	
	
	/**
	 * tell the registered network handler that network activity has finished, note the message is only delivered to the network handler when
	 * all network activity (from all threads) has stopped, any call to {@link ZiftrNetworkManager}{@link #networkStarted()} should have a corresponding call
	 * to this, otherwise the network handler may think network activity is constant
	 */
	public static synchronized void networkStopped() {
		networkCounter--;
		if(networkCounter <= 0 && networkActive) {
		
			//networkActive = false; //don't do this here, do it in the delayed stop
			
			delayedStop();
		}
	}
	
	
	private static synchronized void delayedStop() {
		
		if(stopThread == null) {
			stopThread = new Thread( new Runnable() {
				
				@Override
				public void run() {
					stopTime = System.currentTimeMillis() + 1800;
					while(System.currentTimeMillis() < stopTime) {
						try {
							Thread.sleep(100);
						}
						catch(Exception e) {}
					}
					synchronized(ZiftrNetworkManager.class) {
						stopThread = null;
						
						if(networkCounter <= 0 && networkActive) {
							networkActive = false;
							ZiftrNetworkHandler handler = currentHandler.get();
							if(handler != null) {
								handler.networkStopped();
							}
						}
					}
					
					
				}
			});
			stopThread.start();
		}
		else {
			stopTime = System.currentTimeMillis() + 1800;
		}
		
	}

	
	
	
}








