package com.ziftr.android.ziftrwallet.fragment.accounts;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;

import com.ziftr.android.ziftrwallet.util.OWTags;
import com.ziftr.android.ziftrwallet.util.ZLog;

public abstract class OWAddressBookParentFragment extends OWWalletUserFragment implements OnClickListener {

	private ImageView addressBookImageView;

	protected EditText labelEditText;
	protected EditText addressEditText;

	public abstract View getContainerView();
	public abstract String getActionBarTitle();

	protected void initializeViewFields(View rootView, int addressBookId) {
		this.addressBookImageView = (ImageView) rootView.findViewById(addressBookId);
		this.addressBookImageView.setOnClickListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		this.setActionBar();
	}

	public void openAddressBook(boolean includeReceivingNotSending, int baseLayout) {
		// The transaction that will take place to show the new fragment
		FragmentTransaction transaction = this.getChildFragmentManager().beginTransaction();

		// TODO maybe add animation to transaciton here?

		Fragment childFragment = new OWAddressBookFragment();

		Bundle b = new Bundle();
		b.putBoolean(OWAddressBookFragment.INCLUDE_RECEIVING_NOT_SENDING_ADDRESSES_KEY, includeReceivingNotSending);
		childFragment.setArguments(b);

		// TODO add or replace?
		transaction.add(baseLayout, childFragment, OWTags.ADDRESS_BOOK);
		//transaction.replace(baseLayout, childFragment, OWTags.ADDRESS_BOOK);
		transaction.addToBackStack(null);
		transaction.commit();
	}

	public void setVisibility(int visibility) {
		getContainerView().setVisibility(visibility);
		if (visibility == View.VISIBLE) {
			this.setActionBar();
		}
	}

	/**
	 * Updates the action bar for this fragment.
	 */
	protected void setActionBar() {
		if (!this.showingChildFragment()) {
			this.getOWMainActivity().changeActionBar(getActionBarTitle(), false, true);
		}
	}

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
		return childFragment != null && !childFragment.isDetached() && childFragment.isVisible();  
	}

	@Override
	public boolean handleBackPress() {
		boolean willSucceed = this.showingChildFragment();
		if (willSucceed) {
			this.returnToParentFragment();
		}
		return willSucceed;
	}

	/**
	 * @return the addressBookImageView
	 */
	public ImageView getAddressBookImageView() {
		return addressBookImageView;
	}

	/////////////////////////////////////////////////
	//////////  Methods for Address/Label  //////////
	/////////////////////////////////////////////////

	public void acceptAddress(String address, String label) {
		// The order that the next two steps are done is actually important because the 
		// label text box has this as a textwatcher which updates the database, and the 
		// address has to be set before we know which address to update in the db.
		// If the order were reversed here then we might changed the note on an 
		// address that doesn't correspond.
		addressEditText.setText(address);
		labelEditText.setText(label);
	}

	public boolean fragmentHasAddress() {
		return !addressEditText.getText().toString().isEmpty();
	}

	/**
	 * A convenience method to update the database from this class.
	 */
	protected void updateAddressLabelInDatabase() {
		String label = this.labelEditText.getText().toString();
		String address = this.addressEditText.getText().toString();
		getWalletManager().updateAddressLabel(getSelectedCoin(), address, label, true);
	}

}