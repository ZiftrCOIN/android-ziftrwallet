package com.ziftr.android.ziftrwallet.dialog;

import java.lang.ref.WeakReference;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWApplication;

import android.content.Context;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

public abstract class ZiftrDialogManager {
	
	private static WeakReference<ZiftrDialogHandler> handlerHolder = new WeakReference<ZiftrDialogHandler>(null);
	
	public static synchronized void registerHandler(ZiftrDialogHandler handler) {
		handlerHolder = new WeakReference<ZiftrDialogHandler>(handler);
	}
	
	
	public static void dialogClickedYes(DialogFragment fragment) {
		ZiftrDialogHandler handler = handlerHolder.get();
		if(handler != null) {
			handler.handleDialogYes(fragment);
		}
	}

	
	public static void dialogClickedNo(DialogFragment fragment) {
		ZiftrDialogHandler handler = handlerHolder.get();
		if(handler != null) {
			handler.handleDialogNo(fragment);
		}
	}
	
	public static void dialogCancelled(DialogFragment fragment) {
		ZiftrDialogHandler handler = handlerHolder.get();
		if(handler != null) {
			handler.handleDialogCancel(fragment);
		}
	}
	
	

	/**
	 * Construct and immediately display a dialog with a simple message and an OK button.
	 * No handling of button clicks is done, this is simply for displaying quick info to the user
	 * @param fragmentManager the fragment manager used to display this dialog fragment
	 * @param messageResId the resource id of the string to be displayed
	 */
	public static void showSimpleAlert(FragmentManager fragmentManager, int messageResId) {
		ZiftrDialogFragment dialog = new ZiftrDialogFragment();
		dialog.setupDialog(0, messageResId, R.string.zw_dialog_ok, 0);
		
		dialog.show(fragmentManager, "simple_alert_dialog");
	}
	
	
	public static void showSimpleAlert(FragmentManager fragmentManager, String message) {
		Context appContext = ZWApplication.getApplication();
		
		ZiftrDialogFragment dialog = new ZiftrDialogFragment();
		String title = appContext.getString(R.string.zw_app_name);
		String ok = appContext.getString(R.string.zw_dialog_ok);
		
		dialog.setupDialog(title, message, ok, null);
		
		dialog.show(fragmentManager, "simple_alert_dialog");
	}

	
}
