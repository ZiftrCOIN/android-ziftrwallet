/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.fragment;

import java.io.InputStream;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWApplication;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;


public class ZWTermsFragment extends ZWFragment {
	
	/**
	 * Load the view.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View rootView = inflater.inflate(R.layout.section_terms_layout, container, false);
		
		try {
			TextView termsTextView = (TextView) rootView.findViewById(R.id.termsText);
			InputStream textStream = ZWApplication.getApplication().getResources().openRawResource(R.raw.terms);
			termsTextView.setText(ZiftrUtils.streamToString(textStream));
		}
		catch(Exception e) {
			ZLog.log("Excpetion loading terms and conditions: ", e);
		}
		
		return rootView;
	}
	
	
	public void onResume() {
		super.onResume();
		this.getZWMainActivity().changeActionBar("TERMS", true, true, false);
	}

	
}