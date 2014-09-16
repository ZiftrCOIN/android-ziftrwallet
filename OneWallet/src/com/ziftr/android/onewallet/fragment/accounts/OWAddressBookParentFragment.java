package com.ziftr.android.onewallet.fragment.accounts;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.ziftr.android.onewallet.util.OWTags;

public abstract class OWAddressBookParentFragment extends OWWalletUserFragment {
	
	protected void openAddressBook(boolean includeReceivingNotSending, int baseLayout) {
		// The transaction that will take place to show the new fragment
		FragmentTransaction transaction = this.getChildFragmentManager().beginTransaction();
		
		// TODO maybe add animation to transaciton here?
		
		Fragment fragToShow = new OWAddressBookFragment();
		
		Bundle b = new Bundle();
		b.putBoolean(OWAddressBookFragment.INCLUDE_RECEIVING_NOT_SENDING_ADDRESSES_KEY, includeReceivingNotSending);
		fragToShow.setArguments(b);

		transaction.replace(baseLayout, fragToShow, OWTags.ADDRESS_BOOK);
		transaction.addToBackStack(null);
		transaction.commit();
	}
	
	protected abstract void acceptAddress(String address);
	
}