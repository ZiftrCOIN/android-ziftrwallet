package com.ziftr.android.ziftrwallet.dialog.handlers;


/**
 *  A simple handler for handling confirmations
 *  and cancels from dialogs.
 */
public interface ZWEditAddressLabelDialogHandler {
	
	/**
	 * Handle a change of a label for an address. 
	 * 
	 * @param requestCode - the request code to specify that this is an edit label request
	 * @param address - The address the use was editing
	 * @param label - The label associated with that address
	 * @param isReceiving - Whether the address edited is a receiving or a sending address (exclusive)
	 */
	public void handleAddressLabelChange(int requestCode, String address, String label, boolean isReceiving);

	/**
	 * Do anything necessary upon cancellation of request.
	 * 
	 * @param requestCode - the request code to specify that this is an edit label request
	 */
	public void handleNegative(int requestCode);
	
}