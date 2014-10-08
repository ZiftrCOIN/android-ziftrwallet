package com.ziftr.android.ziftrwallet.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ziftr.android.ziftrwallet.R;


public class OWAboutFragment extends OWFragment {
	
	/**
	 * Load the view.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.section_about_layout, container, false);
		return rootView;
	}
	
	public void onResume() {
		super.onResume();
		this.getOWMainActivity().changeActionBar("ABOUT", true, true, false);
	}
	
}