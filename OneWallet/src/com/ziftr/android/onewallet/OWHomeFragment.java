package com.ziftr.android.onewallet;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

public class OWHomeFragment extends Fragment implements OnClickListener {

	View rootView;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		rootView = inflater.inflate(R.layout.fragment_home, container, false);
	
		View bitcoinWalletButton = rootView.findViewById(R.id.buttonBitcoinWallet);
		bitcoinWalletButton.setOnClickListener(this);
		bitcoinWalletButton.setTag(OWBitcoinWalletFragment.class); //add what kind of fragment this button should create as tag
		
		return rootView;
	}


	@Override
	public void onClick(View v) {
		
		try {
			@SuppressWarnings("unchecked") //this is sent when the button is create (at least it better)
			Class<? extends Fragment> newFragmentClass = (Class<? extends Fragment>)v.getTag();
			
			Fragment fragment = newFragmentClass.newInstance();
			String tagString = newFragmentClass.getSimpleName();
			
			getFragmentManager().beginTransaction().replace(R.id.mainActivityContainer, fragment, tagString).addToBackStack(null).commit();
		}
		catch(Exception e) {
			ZLog.log("Exceptiong trying to load wallet fragment. Was a button not properly setup? ", e);
		}
	}

	

}
