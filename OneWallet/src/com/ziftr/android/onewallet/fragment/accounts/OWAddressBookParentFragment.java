package com.ziftr.android.onewallet.fragment.accounts;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.ziftr.android.onewallet.util.OWTags;
import com.ziftr.android.onewallet.util.ZLog;

public abstract class OWAddressBookParentFragment extends OWWalletUserFragment {

	public void openAddressBook(boolean includeReceivingNotSending, int baseLayout) {
		// The transaction that will take place to show the new fragment
		FragmentTransaction transaction = this.getChildFragmentManager().beginTransaction();

		// TODO maybe add animation to transaciton here?

		Fragment childFragment = new OWAddressBookFragment();

		Bundle b = new Bundle();
		b.putBoolean(OWAddressBookFragment.INCLUDE_RECEIVING_NOT_SENDING_ADDRESSES_KEY, includeReceivingNotSending);
		childFragment.setArguments(b);

		transaction.replace(baseLayout, childFragment, OWTags.ADDRESS_BOOK);
		transaction.addToBackStack(null);
		transaction.commit();
	}

	public abstract void setVisibility(int visibility);

	public abstract void acceptAddress(String address, String label);

	/**
	 * @return a boolean describing whether or not the fragment transaction
	 * was successful.
	 */
	public void returnToParentFragment() {
		Fragment childFragment = this.getChildFragmentManager().findFragmentByTag(OWTags.ADDRESS_BOOK);

		if (childFragment != null) {
			// The transaction that will take place to show the new fragment
			FragmentTransaction transaction = this.getChildFragmentManager().beginTransaction();

			// TODO maybe add animation to transaciton here?

			transaction.detach(childFragment);
			transaction.remove(childFragment);
			transaction.commit();
		} else {
			ZLog.log("Child fragment was null...");
		}
	}

	public boolean showingChildFragment() {
		Fragment childFragment = this.getChildFragmentManager().findFragmentByTag(OWTags.ADDRESS_BOOK);
		return childFragment != null && !childFragment.isDetached();  
	}

	@Override
	public boolean handleBackPress() {
		boolean willSucceed = this.showingChildFragment();
		this.returnToParentFragment();
		return willSucceed;
	}

}