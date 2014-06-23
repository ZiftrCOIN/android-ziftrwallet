package com.ziftr.android.onewallet;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

/**
 * The OWMainActivity starts this fragment. This fragment is 
 * associated with a few buttons where the user can choose what
 * kind of wallet they want to open.
 */
public class OWHomeFragment extends Fragment implements OnClickListener {

	/** The view container for this fragment. */
	protected View rootView;
	
	/** 
	 * Placeholder for later, doesn't do anything other than 
	 * what parent method does right now.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	/**
	 * To create the view for this fragment, we start with the 
	 * basic fragment_home view which just has a few buttons that
	 * open up different coin-type wallets.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_home, container, false);
	
		View bitcoinWalletButton = rootView.findViewById(
				R.id.buttonBitcoinWallet);
		// We are the listener for clicks
		bitcoinWalletButton.setOnClickListener(this);
		// Add what kind of fragment this button should create as tag
		bitcoinWalletButton.setTag(OWBitcoinWalletFragment.class); 
		
		// Return the view which was inflated
		return rootView;
	}

	/**
	 * 
	 */
	@Override
	public void onClick(View v) {
		try {
			//this is sent when the button is create (at least it better)
			@SuppressWarnings("unchecked") 
			Class<? extends Fragment> newFragmentClass = (
					Class<? extends Fragment>) v.getTag();

			Fragment fragment = newFragmentClass.newInstance();
			String tagString = newFragmentClass.getSimpleName();
			
			getFragmentManager().beginTransaction().replace(
					R.id.mainActivityContainer, fragment, tagString
					).addToBackStack(null).commit();
		} catch(Exception e) {
			ZLog.log("Exceptiong trying to load wallet fragment. Was a "
					+ "button not properly setup? ", e);
		}
	}

}
