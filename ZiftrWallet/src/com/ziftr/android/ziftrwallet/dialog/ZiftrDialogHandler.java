/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.dialog;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

public interface ZiftrDialogHandler {
	
	public void handleDialogYes(DialogFragment fragment);
	public void handleDialogNo(DialogFragment fragment);
	public void handleDialogCancel(DialogFragment fragment);
	
	public FragmentManager getSupportFragmentManager();
}
