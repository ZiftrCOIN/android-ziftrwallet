package com.ziftr.android.ziftrwallet.util;


public class OWOptionalFieldValidationException extends OWCoinURIParseException {

	/** To get rid of warnings. */
	private static final long serialVersionUID = 1L;
	
	public OWOptionalFieldValidationException(String message) {
		super(message);
	}
	
	public OWOptionalFieldValidationException(String message, Throwable e) {
		super(message, e);
	}

}