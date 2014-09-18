package com.ziftr.android.onewallet;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.ziftr.android.onewallet.util.ZiftrUtils;

public class OWWelcomeActivity extends FragmentActivity{
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Here we just load the splash screen and then start the app one
		// second later.

		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.welcome);
		Button setPassword = (Button) this.findViewById(R.id.set_password);
		Button skipPassword = (Button) this.findViewById(R.id.skip_password);
		final EditText passphrase = (EditText) this.findViewById(R.id.new_password);
		setPassword.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (saved.isChecked()){
				byte[] inputHash = ZiftrUtils.Sha256Hash(passphrase.getText().toString().getBytes());
		    	Intent main = new Intent(OWWelcomeActivity.this, 
		    			OWMainFragmentActivity.class);
		    	main.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		    	main.putExtra("SET_PASSPHRASE", inputHash);
		        startActivity(main);
				}
			}
		});
		
		skipPassword.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
		    	Intent main = new Intent(OWWelcomeActivity.this, 
		    			OWMainFragmentActivity.class);
		    	main.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		        startActivity(main);
				}
		});



	}
}
