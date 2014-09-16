package com.ziftr.android.onewallet.fragment;

import android.support.v4.app.Fragment;
import android.view.View;

import com.ziftr.android.onewallet.OWMainFragmentActivity;


public class OWFragment extends Fragment {

	public OWMainFragmentActivity getOWMainActivity() {
		return ((OWMainFragmentActivity) this.getActivity());
	}

	protected void hideWalletHeader() {
		this.getOWMainActivity().getWalletHeaderBar().setVisibility(View.GONE);
	}
}