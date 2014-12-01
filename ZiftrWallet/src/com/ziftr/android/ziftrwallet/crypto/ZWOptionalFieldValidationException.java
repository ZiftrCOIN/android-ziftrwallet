package com.ziftr.android.ziftrwallet.crypto;



public class ZWOptionalFieldValidationException extends ZWCoinURIParseException {

	/** To get rid of warnings. */
	private static final long serialVersionUID = 1L;
	
	public ZWOptionalFieldValidationException(String message) {
		super(message);
	}
	
	public ZWOptionalFieldValidationException(String message, Throwable e) {
		super(message, e);
	}

}