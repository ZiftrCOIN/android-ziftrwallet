/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.crypto;


public class ZWCoinURIParseException extends Exception {

	/** To get rid of warnings. */
	private static final long serialVersionUID = 1L;
	
	public ZWCoinURIParseException(String message) {
		super(message);
	}
	
	public ZWCoinURIParseException(String message, Throwable e) {
		super(message, e);
	}

}