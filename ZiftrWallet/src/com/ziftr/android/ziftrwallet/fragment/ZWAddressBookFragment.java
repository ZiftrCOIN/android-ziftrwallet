package com.ziftr.android.ziftrwallet.fragment;

import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.crypto.ZWAddress;
import com.ziftr.android.ziftrwallet.fragment.ZWAddressListAdapter.SortState;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public abstract class ZWAddressBookFragment extends ZWWalletUserFragment 
implements TextWatcher, OnClickListener {

	/** The root view for this application. */
	private View rootView; 

	private ListView addressListView;
	private ZWAddressListAdapter addressAdapter;

	private ImageView sortByTimeIcon;
	private ImageView sortAlphabeticallyIcon;

	public static final String SORT_STATE_KEY = "sort_state";

	@Override
	public void onResume() {
		super.onResume();
		this.getZWMainActivity().changeActionBar("ADDRESSES", false, false, false, this, this.addressAdapter);
	}

	@Override
	public void onPause() {
		this.getZWMainActivity().unregisterSearchBarTextWatcher(this);
		super.onPause();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (addressAdapter != null && addressAdapter.getSortState() != null) {
			outState.putString(SORT_STATE_KEY, addressAdapter.getSortState().toString());
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		rootView = inflater.inflate(R.layout.accounts_address_book, container, false);

		// So that on screen rotates the background view stays invisible
		getAddressBookParentFragment().setVisibility(View.INVISIBLE);

		this.populateWalletHeader(rootView.findViewById(R.id.walletHeader));

		this.initializeSortingIcons();

		// Needs the bundle because the bundle has the sort state stored in it
		this.initializeAddressList(savedInstanceState);

		return this.rootView;
	}

	@Override
	public void onClick(View v) {
		if (v == this.sortByTimeIcon) {
			this.addressAdapter.toggleSortByTime();
		} else if (v == this.sortAlphabeticallyIcon) {
			this.addressAdapter.toggleSortAlphabetically();
		}
	}

	private void initializeSortingIcons() {
		this.sortByTimeIcon = (ImageView) this.rootView.findViewById(R.id.sortByTimeIcon);
		this.sortByTimeIcon.setOnClickListener(this);

		this.sortAlphabeticallyIcon = (ImageView) this.rootView.findViewById(R.id.sortAlphabeticallyIcon);
		this.sortAlphabeticallyIcon.setOnClickListener(this);
	}

	private void initializeAddressList(Bundle savedInstanceState) {
		this.addressAdapter = new ZWAddressListAdapter(this.getZWMainActivity());
		if (savedInstanceState != null) {
			if (savedInstanceState.getString(SORT_STATE_KEY) != null) {
				this.addressAdapter.setSortState(SortState.valueOf(savedInstanceState.getString(SORT_STATE_KEY)));
			}
		}

		this.addressListView = (ListView) this.rootView.findViewById(R.id.addressBookListView);
		this.addressListView.setItemsCanFocus(true);
		this.addressListView.setAdapter(this.addressAdapter);
		this.addressListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, 
					int position, long id) {
				ZWAddress address = (ZWAddress) addressListView.getItemAtPosition(position);
				ZWAddressBookParentFragment parentFragment = getAddressBookParentFragment();
				parentFragment.acceptAddress(address.getAddress(), address.getLabel());
				parentFragment.returnToParentFragment();
			}
		});

		this.loadAddressesFromDatabase();
	}

	
	
	private void loadAddressesFromDatabase() {
		ZiftrUtils.runOnNewThread(new Runnable() {
			@Override
			public void run() {
				final List<ZWAddress> addresses = getDisplayAddresses();
				Activity a = ZWAddressBookFragment.this.getZWMainActivity();
				// It it's null then the app is dying and we do it on the next round
				if (a != null) {
					a.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							addressAdapter.getFullList().clear();
							addressAdapter.getFullList().addAll(addresses);
							addressAdapter.refreshWorkingList(true);
							addressAdapter.notifyDataSetChanged();
						}
					});
				}

			}
		});
	}

	
	/**
	 * read, load, create or otherwise obtain an array of {@link ZWAddress} objects for the 
	 * fragment to display to the user
	 * @return a list of address objects
	 */
	protected abstract List<ZWAddress> getDisplayAddresses();
	
	
	/**
	 * @return
	 */
	private ZWAddressBookParentFragment getAddressBookParentFragment() {
		return (ZWAddressBookParentFragment) getParentFragment();
	}


	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
			int arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterTextChanged(Editable s) {
		// TODO Auto-generated method stub
		this.addressAdapter.getFilter().filter(s);
	}

	@Override
	public void onDataUpdated() {
		this.loadAddressesFromDatabase();
		super.onDataUpdated();
	}

	/**
	 * This makes it so that when this address book fragment is destroyed
	 * the parent fragment comes back into the view.
	 */
	@Override
	public void onDestroyView() {
		getAddressBookParentFragment().setVisibility(View.VISIBLE);
		super.onDestroyView();
	}

}
