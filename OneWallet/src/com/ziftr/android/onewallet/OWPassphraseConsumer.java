package com.ziftr.android.onewallet;

/**
 * This interface will be implemented by any classes
 * which need to do a task when the dialog returns with
 * the user's passphrase in it.
 */
public interface OWPassphraseConsumer {
	
	/** 
	 * Accepts a passphrase and takes an appropriate action 
	 * depending on the value of that passphrase.
	 * 
	 * @param inputHash - The hash of the passphrase the user inputs.
	 * Using the ZiftrUtil function Sha256Hash may be helpful.
	 */
	void consumePassphraseHash(byte[] inputHash);
	
}