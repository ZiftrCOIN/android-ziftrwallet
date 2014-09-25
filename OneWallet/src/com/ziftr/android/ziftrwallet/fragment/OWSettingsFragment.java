package com.ziftr.android.ziftrwallet.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.dialog.OWCreatePassphraseDialog;
import com.ziftr.android.ziftrwallet.dialog.OWDialogFragment;
import com.ziftr.android.ziftrwallet.dialog.OWResetPassphraseDialog;
import com.ziftr.android.ziftrwallet.util.OWRequestCodes;


public class OWSettingsFragment extends OWFragment implements OnClickListener{

	private boolean userHasPassphrase;
	private RelativeLayout disablePassword;
	private RelativeLayout resetPassword;
	private TextView resetPasswordLabel;
	private CheckBox editableConfirmationFee;

	/**
	 * Load the view.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {
		this.hideWalletHeader();
		this.userHasPassphrase = this.getOWMainActivity().userHasPassphrase();

		View rootView = inflater.inflate(R.layout.section_settings_layout, container, false);

		this.disablePassword = (RelativeLayout) rootView.findViewById(R.id.disable_passphrase);
		this.resetPassword = (RelativeLayout) rootView.findViewById(R.id.reset_password_button);
		this.resetPassword.setOnClickListener(this);
		this.resetPasswordLabel = ((TextView) resetPassword.findViewById(R.id.reset_password_text));
		this.editableConfirmationFee = (CheckBox) rootView.findViewById(R.id.editable_confirmation_fees);
		this.editableConfirmationFee.setOnClickListener(this);
		if (this.getOWMainActivity().getFeesAreEditable()){
			this.editableConfirmationFee.setChecked(true);
		} else {
			this.editableConfirmationFee.setChecked(false);

		}
		this.updateSettingsVisibility();
		return rootView;
	}
	
	public void onResume() {
		super.onResume();
		this.getOWMainActivity().changeActionBar("SETTINGS", true, true);
	}

	public void updateSettingsVisibility(){
		if (this.userHasPassphrase){
			this.disablePassword.setVisibility(View.GONE);
			this.resetPasswordLabel.setText("Reset Passphrase");
		} else {
			this.disablePassword.setVisibility(View.VISIBLE);
			this.resetPasswordLabel.setText("Set Passphrase");

		}
	}
	@Override
	public void onClick(View v) {
		if (v==this.resetPassword){
			if (userHasPassphrase) {
				OWResetPassphraseDialog passphraseDialog = 
						new OWResetPassphraseDialog();
				// Set the target fragment
				passphraseDialog.setTargetFragment(OWSettingsFragment.this, 
						OWRequestCodes.RESET_PASSPHRASE_DIALOG);
				Bundle args = new Bundle();
				args.putInt(OWDialogFragment.REQUEST_CODE_KEY, OWRequestCodes.RESET_PASSPHRASE_DIALOG);
				passphraseDialog.setArguments(args);
				passphraseDialog.setupDialog("ziftrWALLET", null, 
						"Continue", null, "Cancel");
				if (!getOWMainActivity().isShowingDialog()) {
					passphraseDialog.show(OWSettingsFragment.this.getFragmentManager(), 
							"scan_qr");
				}
			} else {
				OWCreatePassphraseDialog createPassphraseDialog = new OWCreatePassphraseDialog();
				createPassphraseDialog.setTargetFragment(OWSettingsFragment.this, OWRequestCodes.CREATE_PASSPHRASE_DIALOG);
				createPassphraseDialog.setupDialog("ziftrWALLET", "Create your passphrase", "Save", null, "Cancel");
				Bundle args = new Bundle();
				args.putInt(OWDialogFragment.REQUEST_CODE_KEY, OWRequestCodes.CREATE_PASSPHRASE_DIALOG);
				createPassphraseDialog.setArguments(args);

				if (!getOWMainActivity().isShowingDialog()) {
					createPassphraseDialog.show(OWSettingsFragment.this.getFragmentManager(), 
							"create_passphrase");
				}
				updateSettingsVisibility();
			}
		} else if (v==this.editableConfirmationFee){
			if (this.editableConfirmationFee.isChecked()){
				getOWMainActivity().setFeesAreEditable(true);
			} else {
				getOWMainActivity().setFeesAreEditable(false);

			}
		}
	}

}