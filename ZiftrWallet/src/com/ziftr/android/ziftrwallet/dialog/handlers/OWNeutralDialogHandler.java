package com.ziftr.android.ziftrwallet.dialog.handlers;

/**
 *  A simple handler for handling a confirmation
 *  from dialogs.
 */
public interface OWNeutralDialogHandler {
	
	/**
	 * Implement this to determine what the app does when
	 * the positive (okay) button on the dialog is clicked.
	 * 
	 * @param requestCode - A bit of extra information so that if a fragment
	 * or activity uses multiple dialogs, we have a way of determining which 
	 * dialog is returning.
	 */
	public void handleNeutral(int requestCode);
	
	/**
	 * Implement this to determine what the app does when
	 * the user clicks outside the dialog.
	 * 
	 * @param requestCode - A bit of extra information so that if a fragment
	 * or activity uses multiple dialogs, we have a way of determining which 
	 * dialog is returning.
	 */
	public void handleNegative(int requestCode);
	
}