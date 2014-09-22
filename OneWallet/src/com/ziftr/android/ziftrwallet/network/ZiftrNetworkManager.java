package com.ziftr.android.ziftrwallet.network;

import java.lang.ref.WeakReference;

public class ZiftrNetworkManager {
	
	private static WeakReference<ZiftrNetworkHandler> currentHandler = new WeakReference<ZiftrNetworkHandler>(null);
	
	private static boolean pendingExpiredLoginError = false;
	
	private static int networkCounter = 0;
	private static boolean networkActive = false;
	private static Thread stopThread = null;
	private static long stopTime = 0;
	
	
	public static void registerNetworkHandler(ZiftrNetworkHandler errorHandler) {
		currentHandler = new WeakReference<ZiftrNetworkHandler>(errorHandler);
		
		if(pendingExpiredLoginError) {
			showLoginExpired();
		}
		if(networkActive) {
			errorHandler.networkStarted();
		}
		else {
			errorHandler.networkStopped();
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








