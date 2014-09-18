package com.ziftr.android.onewallet.fragment;

import java.lang.reflect.Field;

import android.support.v4.app.Fragment;
import android.view.View;

import com.ziftr.android.onewallet.OWMainFragmentActivity;
import com.ziftr.android.onewallet.util.ZLog;


public class OWFragment extends Fragment {

	public OWMainFragmentActivity getOWMainActivity() {
		return ((OWMainFragmentActivity) this.getActivity());
	}

	protected void hideWalletHeader() {
		this.getOWMainActivity().getWalletHeaderBar().setVisibility(View.GONE);
	}

	public boolean handleBackPress() {
		return false;
	}

	@Override
	public void onDetach() {
		super.onDetach();

		//TODO -have to fix a bug in the support library that doesn't properly handle nested fragments
		//this is more safe than the average android reflection hack since we control when the support library is updated
		//but it should be fixed as soon as the support library has a fix for this

		//what's happening is that the child support manager is keeping a reference (weak since it doesn't seem to leak)
		//to the activity that originally created it
		//when the main activity recreates itself, and reloads the parent fragment from it's fragment manager, 
		//the re-used parent fragment then tries to load its nested fragments, which are still referencing an
		//old activity, that is now null
		//forcing the local variable containing the entire child fragment manager to null, forces the class
		//to recreate a new child fragment manager when it's recreated with a proper handle to the right activity

		try {
			Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
			childFragmentManager.setAccessible(true);
			childFragmentManager.set(this, null);
		} catch (Exception e) {
			ZLog.log("Exception using reflection to clean up settings fragments: ", e);
		}
	}

}
