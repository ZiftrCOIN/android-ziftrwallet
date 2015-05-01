/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.sqlite;

@SuppressWarnings("serial")
public class ZWNoTransactionFoundException extends RuntimeException {
	
	public ZWNoTransactionFoundException(String message) {
		super(message);
	}
	
}