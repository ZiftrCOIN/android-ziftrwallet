package com.ziftr.android.onewallet.fragment.accounts;

import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.ziftr.android.onewallet.OWWalletManager;
import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.crypto.OWAddress;
import com.ziftr.android.onewallet.util.OWEditState;
import com.ziftr.android.onewallet.util.ZLog;
import com.ziftr.android.onewallet.util.ZiftrUtils;

public class OWAddressBookFragment extends OWWalletUserFragment 
implements TextWatcher, OWEditableTextBoxController.EditHandler<OWAddress> {

	/** The root view for this application. */
	private View rootView; 

	private ListView addressListView;

	private OWAddressListAdapter addressAdapter;

	public static final String INCLUDE_RECEIVING_NOT_SENDING_ADDRESSES_KEY = "include_receiving";
	boolean includeReceivingNotSending;

	public static final String CUR_EDITING_ADDRESS_KEY = "CUR_EDITING_ADDRESS_KEY";
	private String curEditingAddress = null;
	private OWEditState curEditState = null;

	@Override
	public void onResume() {
		super.onResume();
		this.getOWMainActivity().changeActionBar("ADDRESSES", false, false, this, this.addressAdapter);
	}

	@Override
	public void onPause() {
		this.getOWMainActivity().unregisterSearchBarTextWatcher(this);
		super.onPause();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (this.curEditingAddress != null) {
			outState.putString(CUR_EDITING_ADDRESS_KEY, this.curEditingAddress);
			OWEditState.saveIntoBundle(curEditState, outState);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		rootView = inflater.inflate(R.layout.accounts_address_book, container, false);

		// So that on screen rotates the background view stays invisible
		getAddressBookParentFragment().setVisibility(View.INVISIBLE);

		this.showWalletHeader();

		this.initializeFromArguments(savedInstanceState);

		this.initializeAddressList();

		return this.rootView;
	}

	public void initializeAddressList() {
		this.addressAdapter = new OWAddressListAdapter(this, this.getActivity());

		this.addressListView = (ListView) this.rootView.findViewById(R.id.addressBookListView);
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
							addressAdapter.getFullList().addAll(addresses);
							addressAdapter.refreshWorkingList();
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
	private void initializeFromArguments(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			this.curEditingAddress = savedInstanceState.getString(CUR_EDITING_ADDRESS_KEY);
			if (this.curEditingAddress != null) {
				this.curEditState = OWEditState.loadFromBundle(savedInstanceState);
			}
		}

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



	//////////////////////////////////////////////////////
	////////// Methods for being an EditHandler //////////
	//////////////////////////////////////////////////////

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
	public void onEditStart(OWEditState state, OWAddress address) {
		ZLog.log("onEditStart called");
		this.curEditState = state;
		this.curEditingAddress = address.toString();
	}

	@Override
	public void onEdit(OWEditState state, OWAddress address) {
		if (address.toString().equals(this.curEditingAddress)) {
			ZLog.log("onEdit called");
			this.curEditState = state;
			
			// Make sure view stays up to date
			address.setNote(this.curEditState.text);

			// Get address and update 
			getWalletManager().updateAddressLabel(getSelectedCoin(), 
					address.toString(), this.curEditState.text, includeReceivingNotSending);
		}
	}

	@Override
	public void onEditEnd(OWAddress address) {
		// Only want to do anything if the data passed in is the same
		// as the data given. If we don't do this then the views 
		// don't initialize correctly, as this method is called as part
		// of the initialization process (in the adapter, the img view gets
		// a new listener every time).
		if (address.toString().equals(this.curEditingAddress)) {
			ZLog.log("onEditEnd called");
			// Need to set these back to null to indicate that we are done editing.
			this.curEditingAddress = null;
			this.curEditState = null;
		}
	}

	@Override
	public boolean isEditing(OWAddress address) {
		return this.isInEditingMode() && this.curEditingAddress.equals(address.toString());
	}

	@Override
	public boolean isInEditingMode() {
		return this.curEditingAddress != null;
	}

	@Override
	public OWEditState getCurEditState() {
		return this.curEditState;
	}

	
}
