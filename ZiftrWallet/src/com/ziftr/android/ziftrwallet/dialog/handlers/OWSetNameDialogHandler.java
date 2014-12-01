package com.ziftr.android.ziftrwallet.dialog.handlers;

public interface OWSetNameDialogHandler {

	/**
	 * 
	 * @param requestCode
	 * @param newName = new name user set
	 */
	public void handleSetNamePositive(int requestCode, String newName);

	/**
	 * 
	 * @param requestCode
	 */
	public void handleNegative(int requestCode);
}
