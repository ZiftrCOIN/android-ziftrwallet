/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.fragment;

import android.os.Bundle;


/**
 * A dummy data holder class for intermediary edits in the 
 * {@link ZWEditableTextBoxController}.
 */
public class ZWEditState {
	public static final String CUR_EDITING_TEXT_KEY = "CUR_EDITING_ADDRESS_LABEL_KEY";
	//public static final String CUR_EDITING_CURSOR_START_KEY = "CUR_EDITING_CURSOR_START_KEY";
	//public static final String CUR_EDITING_CURSOR_END_KEY = "CUR_EDITING_CURSOR_END_KEY";
	
	public String text;
	//public int cursorStart;
	//public int cursorEnd;
	
	public static void saveIntoBundle(ZWEditState state, Bundle b) {
		b.putString(CUR_EDITING_TEXT_KEY, state.text);
		//b.putInt(CUR_EDITING_CURSOR_START_KEY, state.cursorStart);
		//b.putInt(CUR_EDITING_CURSOR_END_KEY, state.cursorEnd);
	}
	
	public static ZWEditState loadFromBundle(Bundle b) {
		ZWEditState curEditState = new ZWEditState();
		curEditState.text = b.getString(CUR_EDITING_TEXT_KEY);
		//curEditState.cursorStart = b.getInt(CUR_EDITING_CURSOR_START_KEY);
		//curEditState.cursorEnd = b.getInt(CUR_EDITING_CURSOR_END_KEY);
		return curEditState;
	}
	
}