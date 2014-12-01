package com.ziftr.android.ziftrwallet.sqlite;

@SuppressWarnings("serial")
public class ZWNoTransactionFoundException extends RuntimeException {
	
	public ZWNoTransactionFoundException(String message) {
		super(message);
	}
	
}