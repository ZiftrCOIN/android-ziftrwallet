package com.ziftr.android.ziftrwallet;

import java.lang.ref.WeakReference;

import android.os.Bundle;

public class ZWMessageManager {
	private static WeakReference<ZWMessageHandler> currentHandler = new WeakReference<ZWMessageHandler>(null);
	
	public static void registerMessageHandler(ZWMessageHandler msgHandler) {
		currentHandler = new WeakReference<ZWMessageHandler>(msgHandler);
	}
	
	public static void unRegisterMessageHandler(ZWMessageHandler errorHandler) {
		
		if(currentHandler.get() != null && currentHandler.get() == errorHandler) {
			currentHandler = new WeakReference<ZWMessageHandler>(null);
		}
	}
	
	public static void alertError(String err){
		ZWMessageHandler handler = currentHandler.get();
		if(handler != null) {
				handler.handleErrorMsg(err);
		}
	}
	
	public static void alertConfirmation(int requestCode, String msg, String tag, Bundle b){
		ZWMessageHandler handler = currentHandler.get();
		if(handler != null) {
				handler.handleConfirmationMsg(requestCode, msg, tag, b);
		}
	}

}
