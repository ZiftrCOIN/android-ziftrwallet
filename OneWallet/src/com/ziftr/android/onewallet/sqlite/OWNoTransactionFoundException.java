package com.ziftr.android.onewallet.sqlite;

@SuppressWarnings("serial")
public class OWNoTransactionFoundException extends RuntimeException {
	
	public OWNoTransactionFoundException(String message) {
		super(message);
	}
	
}