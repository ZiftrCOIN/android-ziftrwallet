/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.fragment;


public interface ZWSearchableListItem {

	/**
	 * This method is used to determine if an item should be added to a list view
	 * while being searched. The nextItem is added because sometimes whether or 
	 * not an item should be added depends on what items surround it.
	 * 
	 * @param constraint - The constraint used for searching.
	 * @param nextItem - The next item which passes the search.
	 * @return
	 */
	public boolean matches(CharSequence constraint, ZWSearchableListItem nextItem);
}