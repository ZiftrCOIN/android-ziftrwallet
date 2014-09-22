package com.ziftr.android.onewallet.util;

import android.os.Bundle;

import com.ziftr.android.onewallet.fragment.accounts.OWEditableTextBoxController;

/**
 * A dummy data holder class for intermediary edits in the 
 * {@link OWEditableTextBoxController}.
 */
public class OWEditState {
	public static final String CUR_EDITING_TEXT_KEY = "CUR_EDITING_ADDRESS_LABEL_KEY";
	public static final String CUR_EDITING_CURSOR_START_KEY = "CUR_EDITING_CURSOR_START_KEY";
	public static final String CUR_EDITING_CURSOR_END_KEY = "CUR_EDITING_CURSOR_END_KEY";
	
	public String text;
	public int cursorStart;
	public int cursorEnd;
	
	public static void saveIntoBundle(OWEditState state, Bundle b) {
		b.putString(CUR_EDITING_TEXT_KEY, state.text);
		b.putInt(CUR_EDITING_CURSOR_START_KEY, state.cursorStart);
		b.putInt(CUR_EDITING_CURSOR_END_KEY, state.cursorEnd);
	}
	
	public static OWEditState loadFromBundle(Bundle b) {
		OWEditState curEditState = new OWEditState();
		curEditState.text = b.getString(CUR_EDITING_TEXT_KEY);
		curEditState.cursorStart = b.getInt(CUR_EDITING_CURSOR_START_KEY);
		curEditState.cursorEnd = b.getInt(CUR_EDITING_CURSOR_END_KEY);
		return curEditState;
	}
	
}