package com.ziftr.android.onewallet.fragment;

import com.ziftr.android.onewallet.OWMainFragmentActivity;

import android.support.v4.app.Fragment;


public class OWFragment extends Fragment {
	
	public OWMainFragmentActivity getOWMainActivity() {
		return ((OWMainFragmentActivity) this.getActivity());
	}
	
}