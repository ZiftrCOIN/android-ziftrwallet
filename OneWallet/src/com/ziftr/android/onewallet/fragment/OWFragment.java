package com.ziftr.android.onewallet.fragment;

import android.support.v4.app.Fragment;
import android.view.View;

import com.ziftr.android.onewallet.OWMainFragmentActivity;
import com.ziftr.android.onewallet.R;


public class OWFragment extends Fragment {
	
	public OWMainFragmentActivity getOWMainActivity() {
		return ((OWMainFragmentActivity) this.getActivity());
	}
	
	public boolean handleBackPress() {
		return false;
	}

}