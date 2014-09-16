package com.ziftr.android.onewallet.fragment;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ziftr.android.onewallet.R;


public class OWAboutFragment extends OWFragment {
	
	/**
	 * Load the view.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {
		this.hideWalletHeader();
		View rootView = inflater.inflate(R.layout.section_about_layout, container, false);
		TextView websiteLink = (TextView) rootView.findViewById(R.id.about_website_link);
		websiteLink.setMovementMethod(LinkMovementMethod.getInstance());
		return rootView;
	}
	
	public void onResume() {
		super.onResume();
		this.getOWMainActivity().changeActionBar("ABOUT", true, true);
	}
	
}