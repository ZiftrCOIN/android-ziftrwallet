package com.ziftr.android.ziftrwallet.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ziftr.android.ziftrwallet.R;


public class OWTermsFragment extends OWFragment {
	
	/**
	 * Load the view.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.section_terms_layout, container, false);
	}
	public void onResume() {
		super.onResume();
		this.getOWMainActivity().changeActionBar("TERMS", true, true, false);
	}

	
}