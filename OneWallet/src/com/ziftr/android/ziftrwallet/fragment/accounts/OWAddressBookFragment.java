package com.ziftr.android.ziftrwallet.fragment.accounts;

import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;

import com.ziftr.android.ziftrwallet.OWWalletManager;
import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.crypto.OWAddress;
import com.ziftr.android.ziftrwallet.fragment.accounts.OWAddressListAdapter.SortState;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class OWAddressBookFragment extends OWWalletUserFragment 
implements TextWatcher, OnClickListener {

	/** The root view for this application. */
	private View rootView; 

	private ListView addressListView;
	private OWAddressListAdapter addressAdapter;

	private ImageView sortByTimeIcon;
	private ImageView sortAlphabeticallyIcon;

	public static final String INCLUDE_RECEIVING_NOT_SENDING_ADDRESSES_KEY = "include_receiving";
	private boolean includeReceivingNotSending;

	public static final String SORT_STATE_KEY = "sort_state";

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		this.getOWMainActivity().unregisterSearchBarTextWatcher(this);
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

		setHasOptionsMenu(true);  
		
		rootView = inflater.inflate(R.layout.accounts_address_book, container, false);

		// So that on screen rotates the background view stays invisible
		getAddressBookParentFragment().setVisibility(View.INVISIBLE);

		this.populateWalletHeader(rootView.findViewById(R.id.walletHeader));

		this.initializeFromArguments();

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
		this.addressAdapter = new OWAddressListAdapter(this.getOWMainActivity());
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
				OWAddress address = (OWAddress) addressListView.getItemAtPosition(position);
				OWAddressBookParentFragment parentFragment = getAddressBookParentFragment();
				parentFragment.acceptAddress(address.toString(), address.getLabel());
				parentFragment.returnToParentFragment();
			}
		});

		this.loadAddressesFromDatabase();
	}

	/**
	 * @return
	 */
	private void loadAddressesFromDatabase() {
		final OWWalletManager manager = this.getWalletManager();

		ZiftrUtils.runOnNewThread(new Runnable() {
			@Override
			public void run() {
				final List<OWAddress> addresses = 
						manager.readAllAddresses(getSelectedCoin(), includeReceivingNotSending);

				Activity a = OWAddressBookFragment.this.getOWMainActivity();
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
	 * @return
	 */
	private OWAddressBookParentFragment getAddressBookParentFragment() {
		return (OWAddressBookParentFragment) getParentFragment();
	}

	/**
	 * We initialize the list of coins to show in the dialog and then 
	 * for each OWCoin.___.toString() that has a boolean value
	 * of true in the bundle we add it to the list.
	 *  
	 * @param args - The bundle with the booleans put into it. The keys are 
	 * the toString()s of the different OWCoin possible values.
	 */
	private void initializeFromArguments() {
		Bundle args = this.getArguments();
		if (args != null) {
			this.includeReceivingNotSending = args.getBoolean(INCLUDE_RECEIVING_NOT_SENDING_ADDRESSES_KEY);
		}
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

	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		this.getOWMainActivity().changeActionBar("ADDRESSES", false, false, false, this, this.addressAdapter);
	}
	
}
