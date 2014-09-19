package com.ziftr.android.onewallet.util;

import android.text.TextWatcher;

/**
 * In most use cases of this app we only need to override one method
 * of textwatcher, this class helps to clean up some code where we make
 * anonymous text watchers so that we don't have a bunch of empty methods
 * everywhere.
 */
public abstract class OWTextWatcher implements TextWatcher {

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// Override if you need to do anything here
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// Override if you need to do anything here
	}

}