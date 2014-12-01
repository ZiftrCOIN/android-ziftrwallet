package com.ziftr.android.ziftrwallet.fragment;

import java.io.File;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ziftr.android.ziftrwallet.ZWApplication;
import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWPreferencesUtils;
import com.ziftr.android.ziftrwallet.dialog.ZWCreatePassphraseDialog;
import com.ziftr.android.ziftrwallet.dialog.ZWDialogFragment;
import com.ziftr.android.ziftrwallet.dialog.ZWResetPassphraseDialog;
import com.ziftr.android.ziftrwallet.dialog.ZWSetNameDialog;
import com.ziftr.android.ziftrwallet.util.ZLog;


public class ZWSettingsFragment extends ZWFragment implements OnClickListener{

	private RelativeLayout disablePassphrase;
	private RelativeLayout resetPassword;
	private TextView resetPasswordLabel;
	private CheckBox editableConfirmationFee;
	private RelativeLayout setFiatCurrency;
	private TextView chosenFiat;
	private RelativeLayout editableConfirmationFeeBar;
	private RelativeLayout setName;
	private RelativeLayout disableName;
	private TextView setNameLabel;
	private Button debugButton;
	private Button exportwalletButton;
	private Button exportlogsButton;
	private Button enablelogsButton;


	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

	}
	/**
	 * Load the view.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.section_settings_layout, container, false);

		this.disablePassphrase = (RelativeLayout) rootView.findViewById(R.id.disable_passphrase);
		this.disablePassphrase.setOnClickListener(this);
		this.setFiatCurrency = (RelativeLayout) rootView.findViewById(R.id.select_fiat_currency);
		this.setFiatCurrency.setOnClickListener(this);
		this.editableConfirmationFeeBar =(RelativeLayout) rootView.findViewById(R.id.editable_confirmation_fees_bar);
		this.editableConfirmationFeeBar.setOnClickListener(this);
		this.chosenFiat = (TextView) rootView.findViewById(R.id.chosen_fiat);
		this.chosenFiat.setText(ZWPreferencesUtils.getFiatCurrency().getName());
		this.resetPassword = (RelativeLayout) rootView.findViewById(R.id.reset_password_button);
		this.resetPassword.setOnClickListener(this);
		this.resetPasswordLabel = ((TextView) resetPassword.findViewById(R.id.reset_password_text));
		this.editableConfirmationFee = (CheckBox) rootView.findViewById(R.id.editable_confirmation_fees);
		this.editableConfirmationFee.setOnClickListener(this);
		this.setName = (RelativeLayout) rootView.findViewById(R.id.set_name_button);
		this.setName.setOnClickListener(this);
		this.disableName = (RelativeLayout) rootView.findViewById(R.id.disable_name_button);
		this.disableName.setOnClickListener(this);
		this.setNameLabel = (TextView) this.setName.findViewById(R.id.set_name_label);
		if (ZWPreferencesUtils.getFeesAreEditable()) {
			this.editableConfirmationFee.setChecked(true);
		} else {
			this.editableConfirmationFee.setChecked(false);
		}

		this.debugButton = (Button) rootView.findViewById(R.id.debug_button);
		this.debugButton.setOnClickListener(this);
		this.exportwalletButton = (Button) rootView.findViewById(R.id.export_db);
		this.exportwalletButton.setOnClickListener(this);
		this.exportlogsButton = (Button) rootView.findViewById(R.id.export_logs);
		this.exportlogsButton.setOnClickListener(this);
		this.enablelogsButton = (Button) rootView.findViewById(R.id.enable_logs);
		this.enablelogsButton.setOnClickListener(this);
		
		this.updateSettingsVisibility(false);
		return rootView;
	}

	public void onResume() {
		super.onResume();
		this.getZWMainActivity().changeActionBar("SETTINGS", true, true, false);
	}

	public void updateSettingsVisibility(boolean justSetPass) {
		//Show/hide options based on password settings
		if (ZWPreferencesUtils.userHasPassphrase() || justSetPass) {
			this.resetPasswordLabel.setText("Reset Passphrase");
			this.disablePassphrase.setVisibility(View.VISIBLE);
		} else if (!ZWPreferencesUtils.getPassphraseDisabled()){
			this.disablePassphrase.setVisibility(View.VISIBLE);
			this.resetPasswordLabel.setText("Set Passphrase");
		} else {
			this.resetPasswordLabel.setText("Set Passphrase");
			this.disablePassphrase.setVisibility(View.GONE);
		}
		//Show/hide options based on name settings
		if (ZWPreferencesUtils.userHasSetName()){
			this.setNameLabel.setText("Change Name");
		} else if (ZWPreferencesUtils.getDisabledName()) {
			this.setNameLabel.setText("Set Name");
		} else {
			this.setNameLabel.setText("Set Name");
		}
		
		//show debug button text based on dis/enabled
		if (!ZWPreferencesUtils.getDebugMode()){
			this.debugButton.setText("Enable debugging");
			this.exportwalletButton.setVisibility(View.GONE);
			this.exportlogsButton.setVisibility(View.GONE);
			this.enablelogsButton.setVisibility(View.GONE);

		} else {
			this.debugButton.setText("Disable debugging");
			this.exportwalletButton.setVisibility(View.VISIBLE);
			this.exportlogsButton.setVisibility(View.VISIBLE);
			this.enablelogsButton.setVisibility(View.VISIBLE);
			if (ZWPreferencesUtils.getLogToFile()){
				this.enablelogsButton.setText(" Disable File logging ");
			} else {
				this.enablelogsButton.setText(" Enable File logging ");
			}
		}
	}
	

	@Override
	public void onClick(View v) {
		if (v==this.resetPassword){
			if (ZWPreferencesUtils.userHasPassphrase()) {
				ZWResetPassphraseDialog passphraseDialog = 
						new ZWResetPassphraseDialog();
				// Set the target fragment
				passphraseDialog.setTargetFragment(ZWSettingsFragment.this, 
						ZWRequestCodes.RESET_PASSPHRASE_DIALOG);
				Bundle args = new Bundle();
				args.putInt(ZWDialogFragment.REQUEST_CODE_KEY, ZWRequestCodes.RESET_PASSPHRASE_DIALOG);
				passphraseDialog.setArguments(args);
				passphraseDialog.setupDialog("ziftrWALLET", null, 
						"Continue", null, "Cancel");
				if (!getZWMainActivity().isShowingDialog()) {
					passphraseDialog.show(ZWSettingsFragment.this.getFragmentManager(), 
							"scan_qr");
				}
			} else {
				ZWCreatePassphraseDialog createPassphraseDialog = new ZWCreatePassphraseDialog();
				createPassphraseDialog.setTargetFragment(ZWSettingsFragment.this, ZWRequestCodes.CREATE_PASSPHRASE_DIALOG);
				createPassphraseDialog.setupDialog("ziftrWALLET", "Create your passphrase", "Save", null, "Cancel");
				Bundle args = new Bundle();
				args.putInt(ZWDialogFragment.REQUEST_CODE_KEY, ZWRequestCodes.CREATE_PASSPHRASE_DIALOG);
				createPassphraseDialog.setArguments(args);

				if (!getZWMainActivity().isShowingDialog()) {
					createPassphraseDialog.show(ZWSettingsFragment.this.getFragmentManager(), 
							"create_passphrase");
				}
			}
		} else if (v==this.editableConfirmationFee){
			ZWPreferencesUtils.setFeesAreEditable(this.editableConfirmationFee.isChecked());
		} else if (v == this.disablePassphrase){
			if (ZWPreferencesUtils.userHasPassphrase()){
				this.getZWMainActivity().showGetPassphraseDialog(ZWRequestCodes.DISABLE_PASSPHRASE_DIALOG, new Bundle(), ZWTags.VALIDATE_PASS_DISABLE);
			} else {
				ZWPreferencesUtils.setPassphraseDisabled(true);
				//update settings visibility too slow with recognizing disabled password so update here
				this.disablePassphrase.setVisibility(View.GONE);
				this.resetPasswordLabel.setText("Set Passphrase");
			}
		} else if (v == this.setFiatCurrency){
			getZWMainActivity().openSetFiatCurrency();
		} else if (v == this.editableConfirmationFeeBar){
			this.editableConfirmationFee.setChecked(!this.editableConfirmationFee.isChecked());
			ZWPreferencesUtils.setFeesAreEditable(this.editableConfirmationFee.isChecked());
		} else if (v == this.disableName){
			ZWPreferencesUtils.setUserName(null);
			ZWPreferencesUtils.setDisabledName(true);
			this.updateSettingsVisibility(false);
		} else if (v == this.setName){
			ZWSetNameDialog setNameDialog = new ZWSetNameDialog();
			setNameDialog.setTargetFragment(ZWSettingsFragment.this, ZWRequestCodes.SET_NAME_DIALOG);
			setNameDialog.setupDialog("ziftrWALLET", "Enter your name.", "Save", null, "Cancel");
			Bundle args = new Bundle();
			args.putInt(ZWDialogFragment.REQUEST_CODE_KEY, ZWRequestCodes.SET_NAME_DIALOG);
			setNameDialog.setArguments(args);
			if (!getZWMainActivity().isShowingDialog()) {
				setNameDialog.show(ZWSettingsFragment.this.getFragmentManager(), 
						"set_name");
			}		
		} else if (v == this.debugButton) {
			if (ZWPreferencesUtils.getDebugMode()){
				ZWPreferencesUtils.setDebugMode(false);
				resetLoggerHelper();
				//re-init coins to not show testnet in non-debug mode
				this.getZWMainActivity().initAvailableCoins();
				this.updateSettingsVisibility(false);
			} else {
				this.getZWMainActivity().showGetPassphraseDialog(ZWRequestCodes.DEBUG_MODE_PASSPHRASE_DIALOG, new Bundle(), ZWTags.VALIDATE_PASS_DEBUG);
			}
		} else if (v == this.exportwalletButton) {
			File wallet = new File(this.getActivity().getExternalFilesDir(null), "wallet.dat");
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("plain/text");
			intent.putExtra(android.content.Intent.EXTRA_STREAM, Uri.fromFile(wallet));
			startActivity(Intent.createChooser(intent, "Send backup"));
		} else if (v == this.exportlogsButton){ 
			File log = new File(this.getActivity().getExternalFilesDir(null) + "/logs", "Zlog.txt");
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("plain/text");
			intent.putExtra(android.content.Intent.EXTRA_STREAM, Uri.fromFile(log));
			startActivity(Intent.createChooser(intent, "Send logs"));
		} else if (v == this.enablelogsButton){
			if (ZWPreferencesUtils.getLogToFile()){
				resetLoggerHelper();
			} else {
				ZWPreferencesUtils.setLogToFile(true);
				ZLog.setLogger(ZLog.FILE_LOGGER);
			}
			this.updateSettingsVisibility(false);
		}
	}
	
	private void resetLoggerHelper(){
		ZWPreferencesUtils.setLogToFile(false);
		if (ZWApplication.isDebuggable()){
			ZLog.setLogger(ZLog.ANDROID_LOGGER);
		} else {
			ZLog.setLogger(ZLog.NOOP_LOGGER);
		}
	}

}