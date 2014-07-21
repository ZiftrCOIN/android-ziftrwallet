package com.ziftr.android.onewallet.dialog.handlers;

import com.ziftr.android.onewallet.util.OWCoin;

/**
 *  A simple handler for handling a dialog where the
 *  user picks a new currency to make a wallet for.
 */
public interface OWNewCurrencyDialogHandler {
	
	/**
	 * Implement this to determine what the app does when
	 * the positive (yes) button on the dialog is clicked.
	 * 
	 * @param requestCode - A bit of extra information so that if a fragment
	 * or activity uses multiple dialogs, we have a way of determining which 
	 * dialog is returning.
	 * @param id - The type of currency that the user created. 
	 */
	public void handleNewCurrencyPositive(int requestCode, OWCoin.Type id);
	
	/**
	 * Implement this method to determine what the app does when
	 * the cancel button is clicked instead of going forward with
	 * entering the passphrase.
	 * 
	 * @param requestCode - A bit of extra information so that if a fragment
	 * or activity uses multiple dialogs, we have a way of determining which 
	 * dialog is returning.
	 */
	public void handleNegative(int requestCode);
	
}