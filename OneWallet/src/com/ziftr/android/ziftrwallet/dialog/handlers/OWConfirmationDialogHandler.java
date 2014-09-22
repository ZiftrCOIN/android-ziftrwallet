package com.ziftr.android.ziftrwallet.dialog.handlers;

import android.os.Bundle;

/**
 *  A simple handler for handling confirmations
 *  and cancels from dialogs.
 */
public interface OWConfirmationDialogHandler {
	
	/**
	 * Implement this to determine what the app does when
	 * the positive (yes) button on the dialog is clicked.
	 * 
	 * @param requestCode - A bit of extra information so that if a fragment
	 * or activity uses multiple dialogs, we have a way of determining which 
	 * dialog is returning.
	 * @param info - extra info
	 */
	public void handleConfirmationPositive(int requestCode, Bundle info);
	
	/**
	 * Implement this to determine what the app does when
	 * the negative (no) button on the dialog is clicked.
	 * 
	 * @param requestCode - A bit of extra information so that if a fragment
	 * or activity uses multiple dialogs, we have a way of determining which 
	 * dialog is returning.
	 */
	public void handleNegative(int requestCode);
	
}