package com.ziftr.android.ziftrwallet.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.ziftr.android.ziftrwallet.ZWApplication;
import com.ziftr.android.ziftrwallet.R;


public class ZWAboutFragment extends ZWFragment implements OnClickListener{
	
	private Button feedbackButton;
	
	/**
	 * Load the view.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.section_about_layout, container, false);
		this.feedbackButton = (Button) rootView.findViewById(R.id.send_feedback);
		this.feedbackButton.setOnClickListener(this);
		
		TextView versionText = (TextView) rootView.findViewById(R.id.ziftrWalletVersionText);
		versionText.setText("v" + ZWApplication.getVersionName() + " r" + String.valueOf(ZWApplication.getVersionCode()));
		
		return rootView;
	}
	
	public void onResume() {
		super.onResume();
		this.getOWMainActivity().changeActionBar("ABOUT", true, true, false);
	}

	@Override
	public void onClick(View v) {
		if (v == this.feedbackButton){
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"customerservice@ziftr.com"});
			intent.setType("message/rfc822");
			startActivity(Intent.createChooser(intent, "Send feedback"));
		}
	}
	
}