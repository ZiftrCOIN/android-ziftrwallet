package com.ziftr.android.onewallet.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.fragment.accounts.OWAccountsFragment;
import com.ziftr.android.onewallet.util.OWUtils;
import com.ziftr.android.onewallet.util.ZLog;


public class OWWelcomeFragment extends OWFragment {
	
	/**
	 * Load the view.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {
		this.getOWMainActivity().hideWalletHeader();
		
		
		View rootView = inflater.inflate(R.layout.welcome, container, false);
		Button setPassword = (Button) rootView.findViewById(R.id.set_password);
		final EditText passphrase = (EditText) rootView.findViewById(R.id.new_password);
		setPassword.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ZLog.log(passphrase.getText());
				byte[] inputHash = OWUtils.Sha256Hash(passphrase.getText().toString().getBytes());
				getOWMainActivity().setPassphraseHash(inputHash);
				
				OWAccountsFragment fragToShow = (OWAccountsFragment) getOWMainActivity().getSupportFragmentManager().findFragmentByTag("ACCOUNT_FRAGMENT_TYPE");
				if (fragToShow == null) {
					fragToShow = new OWAccountsFragment();
				}
				getOWMainActivity().showFragment(fragToShow, "ACCOUNT_FRAGMENT_TYPE", R.id.oneWalletBaseFragmentHolder, false, null);
			}
		});
		
		return rootView;
	}
	public void onResume() {
		super.onResume();
		this.getOWMainActivity().changeActionBar("ziftrWALLET",true, false);
	}

	
}