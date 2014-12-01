package com.ziftr.android.ziftrwallet.util;


public class OWRequiredFieldValidationException extends OWCoinURIParseException {

	/** To get rid of warnings. */
	private static final long serialVersionUID = 1L;

	public OWRequiredFieldValidationException(String message) {
		super(message);
	}

	public OWRequiredFieldValidationException(String message, Throwable e) {
		super(message, e);
	}

}