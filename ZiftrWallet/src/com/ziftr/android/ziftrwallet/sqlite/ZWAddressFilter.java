package com.ziftr.android.ziftrwallet.sqlite;

import com.ziftr.android.ziftrwallet.crypto.ZWSendingAddress;

/**
 * An interface useful for parameterizing an SQLite query
 * with a where statement and then filtering the results further
 * by custom logic.
 */
public interface ZWAddressFilter<A extends ZWSendingAddress> {
	
	public boolean include(A address);
	public String where();
	
}
