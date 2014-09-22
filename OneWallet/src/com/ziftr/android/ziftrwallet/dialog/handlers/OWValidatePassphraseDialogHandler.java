package com.ziftr.android.ziftrwallet.dialog.handlers;

import android.os.Bundle;

/**
 *  A simple handler for handling a dialog where the
 *  user enters their passphrase.
 */
public interface OWValidatePassphraseDialogHandler {
	
	/**
	 * Implement this to determine what the app does when
	 * the positive (yes) button on the dialog is clicked.
	 * 
	 * @param requestCode - A bit of extra information so that if a fragment
	 * or activity uses multiple dialogs, we have a way of determining which 
	 * dialog is returning.
	 * @param passPhrase - The passphrase is given to the activity/fragment
	 * so that they can use the result appropriately.
	 */
	public void handlePassphrasePositive(int requestCode, byte[] passphrase, Bundle info);
	
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