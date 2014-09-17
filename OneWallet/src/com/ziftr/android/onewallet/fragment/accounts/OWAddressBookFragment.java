package com.ziftr.android.onewallet.fragment.accounts;

import java.util.List;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.crypto.OWAddress;
import com.ziftr.android.onewallet.util.ZLog;

public class OWAddressBookFragment extends OWWalletUserFragment implements TextWatcher {

	/** The root view for this application. */
	private View rootView; 

	private ListView addressListView;

	private OWAddressListAdapter addressAdapter;

	public static final String INCLUDE_RECEIVING_NOT_SENDING_ADDRESSES_KEY = "include_receiving";

	boolean includeReceivingNotSending;

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
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {
		
		// So that on screen rotates the background view stays invisible
		getAddressBookParentFragment().setVisibility(View.INVISIBLE);

		rootView = inflater.inflate(R.layout.accounts_address_book, container, false);

		this.showWalletHeader();

		initializeFromBundle(this.getArguments());

		initializeAddressList();

		return this.rootView;
	}

	public void initializeAddressList() {
		// TODO do this in a non UI blocking way
		List<OWAddress> addresses;
		if (this.includeReceivingNotSending) {
			addresses = this.getWalletManager().readAllReceivingAddresses(getCurSelectedCoinType());
		} else {
			addresses = this.getWalletManager().readAllSendingAddresses(getCurSelectedCoinType());
		}
		this.addressAdapter = new OWAddressListAdapter(this.getActivity(), addresses);

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
	private void initializeFromBundle(Bundle args) {
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
	public void onDetach() {
		ZLog.log("detatched!!!!   :"); 
		getAddressBookParentFragment().setVisibility(View.VISIBLE);
		super.onDetach();
	}
	
	@Override
	public void onDestroy() {
		ZLog.log("destroyed!!!!   :"); 
		getAddressBookParentFragment().setVisibility(View.VISIBLE);
		super.onDestroy();
	}
	
}
