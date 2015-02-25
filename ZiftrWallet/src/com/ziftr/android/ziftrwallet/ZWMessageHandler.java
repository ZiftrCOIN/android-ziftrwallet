package com.ziftr.android.ziftrwallet;

import android.os.Bundle;

public interface ZWMessageHandler {

	
	/**
	 * called when messagemanager wants to alert error message to handler
	 * @param err
	 */
	public void handleErrorMsg(String err);
	
	/**
	 * called when a message and option to cancel/continue are to be displayed by handler
	 * @param msg
	 */
	public void handleConfirmationMsg(int requestCode, String msg, String tag, Bundle b);
	
}
