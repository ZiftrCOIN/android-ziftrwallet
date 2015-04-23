package com.ziftr.android.ziftrwallet.dialog;

import android.support.v4.app.DialogFragment;

public interface ZiftrDialogHandler {
	
	public void handleDialogYes(DialogFragment fragment);
	public void handleDialogNo(DialogFragment fragment);
	public void handleDialogCancel(DialogFragment fragment);

}
