package com.ziftr.android.ziftrwallet.util;

/**
 * In some cases it is useful to be able to keep a reference to a boolean field 
 * (such in an anonymous inner class). For example, when binding values in edit texts
 * to respond to the contents of one-another. This is used in the fiat-coin binded 
 * edit texts. 
 */
public class OWBoolean {
	
	boolean b = false;
	
	public boolean get() {
		return b;
	}
	
	public void set(boolean b) {
		this.b = b;
	}
	
}