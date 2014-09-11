package com.ziftr.android.onewallet.fragment.accounts;

import android.os.Bundle;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.util.ZLog;

public abstract class OWAddressBookFragment extends OWWalletUserFragment implements TextWatcher {

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
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {

		rootView = inflater.inflate(R.layout.currency_get_new_list, container, false);

		initializeFromBundle(this.getArguments());

		initializeAddressList();

		return this.rootView;
	}

	public void initializeAddressList() {

		this.addressAdapter = new OWAddressListAdapter(getActivity());
		
//		this.getWalletManager().readAll

		this.addressListView = (ListView) this.rootView.findViewById(R.id.addressBookListView);
		this.addressListView.setAdapter(this.addressAdapter);
		this.addressListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, 
					int position, long id) {
				//TODO Open the send/receive screen
				ZLog.log("Address clicked.");
			}
		});
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

}
