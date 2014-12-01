package com.ziftr.android.ziftrwallet.dialog.handlers;

/**
 *  A simple handler for handling confirmations
 *  and cancels from dialogs.
 */
public interface OWPositiveDialogHandler {
	
	/**
	 * Implement this to determine what the app does when
	 * the positive (yes) button on the dialog is clicked.
	 * 
	 * @param requestCode - A bit of extra information so that if a fragment
	 * or activity uses multiple dialogs, we have a way of determining which 
	 * dialog is returning.
	 */
	public void handlePositive(int requestCode);
	
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