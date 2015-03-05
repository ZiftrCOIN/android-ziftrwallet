package com.ziftr.android.ziftrwallet;

import java.lang.ref.WeakReference;
import java.util.LinkedList;

import android.os.Bundle;

public class ZWMessageManager {
	private static WeakReference<ZWMessageHandler> currentHandler = new WeakReference<ZWMessageHandler>(null);
	
	/** Queue with bundles storing alert parameters to be sent to messagehandler if delayed
	 * */
	private static LinkedList<Bundle>msgQueue = new LinkedList<Bundle>();
	
	/** used to save which message type for bundles */
	private static final int ALERT = 0;
	private static final int CONFIRM = 1;
	
	public static void registerMessageHandler(ZWMessageHandler msgHandler) {
		currentHandler = new WeakReference<ZWMessageHandler>(msgHandler);
		if (msgQueue.size() > 0){
			//send all alerts at once, should be ok, there is really no way for the queue to have more than one alert in it
			for (Bundle b : msgQueue){
				executeAlertFromBundle(b);
			}
			msgQueue.clear();
		}
	}
	
	public static void unregisterMessageHandler(ZWMessageHandler errorHandler) {
		
		if(currentHandler.get() != null && currentHandler.get() == errorHandler) {
			currentHandler = new WeakReference<ZWMessageHandler>(null);
		}
	}
	
	public static void alertError(String err){
		ZWMessageHandler handler = currentHandler.get();
		if(handler != null) {
			handler.handleErrorMsg(err);
		} else {
			Bundle alertMsg = new Bundle();
			alertMsg.putInt("type", ALERT);
			alertMsg.putString("message", err);
			msgQueue.add(alertMsg);
		}
	}
	
	public static void alertConfirmation(int requestCode, String msg, String tag, Bundle b){
		
		ZWMessageHandler handler = currentHandler.get();
		if(handler != null) {
			handler.handleConfirmationMsg(requestCode, msg, tag, b);
		} else {
			Bundle alertMsg = new Bundle();
			alertMsg.putInt("type", CONFIRM);
			alertMsg.putString("message", msg);
			alertMsg.putInt("request_code", requestCode);
			alertMsg.putString("tag", tag);
			alertMsg.putBundle("bundle", b);
			msgQueue.add(alertMsg);
		}
	}
	
	private static void executeAlertFromBundle(Bundle b){
		switch(b.getInt("type")){
			case ALERT:
				alertError(b.getString("message"));
				break;
			case CONFIRM:
				int requestCode = b.getInt("request_code");
				String msg = b.getString("message");
				String tag = b.getString("tag");
				Bundle confirmBundle = b.getBundle("bundle"); 
				alertConfirmation(requestCode, msg, tag, confirmBundle);
				break;
		}
	}

}
