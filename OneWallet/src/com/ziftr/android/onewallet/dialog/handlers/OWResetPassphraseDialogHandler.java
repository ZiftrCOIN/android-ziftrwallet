package com.ziftr.android.onewallet.dialog.handlers;

/**
 *  A simple handler for handling a dialog where the
 *  user enters their passphrase, a new passphrase
 *  and a confirmation of that passphrase to reset 
 *  the passphrase for their wallet.
 */
public interface OWResetPassphraseDialogHandler {
	
	/**
	 * Implement this to determine what the app does when
	 * the positive (yes) button on the dialog is clicked.
	 * 
	 * @param requestCode - A bit of extra information so that if a fragment
	 * or activity uses multiple dialogs, we have a way of determining which 
	 * dialog is returning.
	 * @param oldPassphrase - The previously set passphrase
	 * @param newPassphrase - The new passphrase to set
	 * @param confirmPassphrase - The (should be) matching new passphrase
	 */
	public void handleResetPassphrasePositive(int requestCode, 
			byte[] oldPassphrase, byte[] newPassphrase, byte[] confirmPassphrase);
	
	/**
	 * Implement this method to determine what the app does when
	 * the cancel button is clicked instead of going forward with
	 * entering the new passphrase.
	 * 
	 * @param requestCode - A bit of extra information so that if a fragment
	 * or activity uses multiple dialogs, we have a way of determining which 
	 * dialog is returning.
	 */
	public void handleNegative(int requestCode);
	
}