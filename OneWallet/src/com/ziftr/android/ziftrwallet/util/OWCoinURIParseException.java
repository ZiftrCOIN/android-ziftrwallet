package com.ziftr.android.ziftrwallet.util;


public class OWCoinURIParseException extends Exception {

	/** To get rid of warnings. */
	private static final long serialVersionUID = 1L;
	
	public OWCoinURIParseException(String message) {
		super(message);
	}
	
	public OWCoinURIParseException(String message, Throwable e) {
		super(message, e);
	}

}