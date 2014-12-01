package com.ziftr.android.ziftrwallet.crypto;



public class ZWRequiredFieldValidationException extends ZWCoinURIParseException {

	/** To get rid of warnings. */
	private static final long serialVersionUID = 1L;

	public ZWRequiredFieldValidationException(String message) {
		super(message);
	}

	public ZWRequiredFieldValidationException(String message, Throwable e) {
		super(message, e);
	}

}