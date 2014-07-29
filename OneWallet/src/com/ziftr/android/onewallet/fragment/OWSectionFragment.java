package com.ziftr.android.onewallet.fragment;

import com.ziftr.android.onewallet.OWMainFragmentActivity;

import android.support.v4.app.Fragment;

/**
 * Maybe this should be OWFragment.java? Almost any fragments might 
 * need this stuff. 
 */
public class OWSectionFragment extends Fragment {
	
	public OWMainFragmentActivity getOWMainActivity() {
		return ((OWMainFragmentActivity) this.getActivity());
	}
	
}