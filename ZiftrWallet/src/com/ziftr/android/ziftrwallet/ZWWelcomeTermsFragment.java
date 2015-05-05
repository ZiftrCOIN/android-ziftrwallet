package com.ziftr.android.ziftrwallet;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

public class ZWWelcomeTermsFragment extends Fragment implements OnClickListener {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View rootView = inflater.inflate(R.layout.welcome_terms, container, false);
		
		View agreeButton = rootView.findViewById(R.id.acceptTerms);
		agreeButton.setOnClickListener(this);
		
		TextView text = (TextView) rootView.findViewById(R.id.termsDescription);
		text.setMovementMethod(LinkMovementMethod.getInstance());
		
		return rootView;
	}

	
	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.acceptTerms) {
			ZWPreferences.setUserAgreedToTos(true);
			
			ZWWelcomeActivity welcomeActivity = (ZWWelcomeActivity) this.getActivity();
			welcomeActivity.showPasswordFragment(false);
		}
	}

	
	
	
}
