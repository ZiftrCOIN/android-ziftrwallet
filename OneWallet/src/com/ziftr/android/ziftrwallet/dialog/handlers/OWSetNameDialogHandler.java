package com.ziftr.android.ziftrwallet.dialog.handlers;

public interface OWSetNameDialogHandler {

	
	public void handleSetNamePositive(int requestCode, String newName);

	public void handleNegative(int requestCode);
}
