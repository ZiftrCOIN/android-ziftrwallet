package com.ziftr.android.ziftrwallet.sqlite;

@SuppressWarnings("serial")
public class OWNoTransactionFoundException extends RuntimeException {
	
	public OWNoTransactionFoundException(String message) {
		super(message);
	}
	
}