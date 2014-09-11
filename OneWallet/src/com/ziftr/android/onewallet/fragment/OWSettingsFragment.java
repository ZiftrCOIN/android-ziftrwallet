package com.ziftr.android.onewallet.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.dialog.OWResetPassphraseDialog;
import com.ziftr.android.onewallet.dialog.OWSimpleAlertDialog;
import com.ziftr.android.onewallet.util.OWRequestCodes;


public class OWSettingsFragment extends OWFragment {
	
	/**
	 * Load the view.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {
		this.getOWMainActivity().hideWalletHeader();
		
		View rootView = inflater.inflate(R.layout.section_settings_layout, container, false);
		Button resetPassword = (Button) rootView.findViewById(R.id.reset_password_button);
		resetPassword.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (OWSettingsFragment.this.getOWMainActivity().userHasPassphrase()) {
					OWResetPassphraseDialog passphraseDialog = 
							new OWResetPassphraseDialog();

					// Set the target fragment
					passphraseDialog.setTargetFragment(OWSettingsFragment.this, 
							OWRequestCodes.RESET_PASSPHRASE_DIALOG);
					passphraseDialog.setupDialog("ziftrWALLET", null, 
							"Continue", null, "Cancel");
					if (!getOWMainActivity().showingDialog()){
					passphraseDialog.show(OWSettingsFragment.this.getFragmentManager(), 
							"scan_qr");
					}
				} else {
					// Make a new alert dialog
					OWSimpleAlertDialog alertUserDialog = new OWSimpleAlertDialog();
					alertUserDialog.setTargetFragment(OWSettingsFragment.this, 
							OWRequestCodes.ALERT_USER_DIALOG);
					// Set up the dialog with message and other info
					alertUserDialog.setupDialog("OneWallet", 
							"You must set a passphrase before you can "
									+ "reset your passphrase.", null, "OK", null);
					// Pop up the dialog
					alertUserDialog.show(OWSettingsFragment.this.getFragmentManager(), 
							"no_passphrase_currently_set_alert_dialog");
				}
			}
		});
		
		return rootView;
	}
	public void onResume(){
		super.onResume();
		this.getOWMainActivity().changeActionBar("SETTINGS", true, true);
	}

	
}