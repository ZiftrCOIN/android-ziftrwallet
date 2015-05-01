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

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWApplication;
import com.ziftr.android.ziftrwallet.ZWPreferences;
import com.ziftr.android.ziftrwallet.dialog.ZiftrDialogFragment;
import com.ziftr.android.ziftrwallet.dialog.ZiftrTextDialogFragment;
import com.ziftr.android.ziftrwallet.network.ZWDataSyncHelper;
import com.ziftr.android.ziftrwallet.util.ZLog;


public class ZWSettingsFragment extends ZWFragment implements OnClickListener{

	public static final String DIALOG_ENABLE_DEBUG_TAG = "settings_enable_debug";
	public static final String DIALOG_CREATE_PASSWORD_TAG = "settings_create_password";
	public static final String DIALOG_CHANGE_PASSWORD_TAG = "settings_change_password";
	public static final String DIALOG_SET_NAME_TAG = "settings_set_name";
	public static final String DIALOG_CHANGE_SERVER_TAG = "settings_change_server";
	
	//TODO -there's no need to keep all these around, the click handler could just use view ids
	
	private RelativeLayout disablePassword;
	private RelativeLayout resetPassword;
	private TextView resetPasswordLabel;
	private CheckBox editableConfirmationFee;
	private CheckBox warnUnconfirmedSpending;
	private RelativeLayout setFiatCurrency;
	private TextView chosenFiat;
	private RelativeLayout editableConfirmationFeeBar;
	private RelativeLayout enableMempoolSpendingBar;
	private RelativeLayout setName;
	private RelativeLayout disableName;
	private TextView setNameLabel;
	private Button debugButton;
	private Button exportwalletButton;
	private Button exportlogsButton;
	private Button enablelogsButton;
	private Button setCustomServer;


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

		this.disablePassword = (RelativeLayout) rootView.findViewById(R.id.disable_passphrase);
		this.disablePassword.setOnClickListener(this);
		this.setFiatCurrency = (RelativeLayout) rootView.findViewById(R.id.select_fiat_currency);
		this.setFiatCurrency.setOnClickListener(this);
		this.editableConfirmationFeeBar =(RelativeLayout) rootView.findViewById(R.id.editable_confirmation_fees_bar);
		this.editableConfirmationFeeBar.setOnClickListener(this);
		this.enableMempoolSpendingBar =(RelativeLayout) rootView.findViewById(R.id.enable_mempool_spending_bar);
		this.enableMempoolSpendingBar.setOnClickListener(this);

		this.chosenFiat = (TextView) rootView.findViewById(R.id.chosen_fiat);
		this.chosenFiat.setText(ZWPreferences.getFiatCurrency().getName());
		this.resetPassword = (RelativeLayout) rootView.findViewById(R.id.reset_password_button);
		this.resetPassword.setOnClickListener(this);
		this.resetPasswordLabel = ((TextView) resetPassword.findViewById(R.id.reset_password_text));
		this.editableConfirmationFee = (CheckBox) rootView.findViewById(R.id.editable_confirmation_fees);
		this.editableConfirmationFee.setOnClickListener(this);
		this.warnUnconfirmedSpending = (CheckBox) rootView.findViewById(R.id.enable_mempool_spending);
		this.warnUnconfirmedSpending.setOnClickListener(this);
		this.setName = (RelativeLayout) rootView.findViewById(R.id.set_name_button);
		this.setName.setOnClickListener(this);
		this.disableName = (RelativeLayout) rootView.findViewById(R.id.disable_name_button);
		this.disableName.setOnClickListener(this);
		this.setNameLabel = (TextView) this.setName.findViewById(R.id.set_name_label);
		
		if (ZWPreferences.getFeesAreEditable()) {
			this.editableConfirmationFee.setChecked(true);
		} else {
			this.editableConfirmationFee.setChecked(false);
		}
		
		if (ZWPreferences.getWarningUnconfirmed()) {
			this.warnUnconfirmedSpending.setChecked(true);
		} else {
			this.warnUnconfirmedSpending.setChecked(false);
		}

		this.debugButton = (Button) rootView.findViewById(R.id.debug_button);
		this.debugButton.setOnClickListener(this);
		this.exportwalletButton = (Button) rootView.findViewById(R.id.export_db);
		this.exportwalletButton.setOnClickListener(this);
		this.exportlogsButton = (Button) rootView.findViewById(R.id.export_logs);
		this.exportlogsButton.setOnClickListener(this);
		this.enablelogsButton = (Button) rootView.findViewById(R.id.enable_logs);
		this.enablelogsButton.setOnClickListener(this);
		this.setCustomServer = (Button) rootView.findViewById(R.id.change_server);
		this.setCustomServer.setOnClickListener(this);
		
		this.updateSettingsVisibility();
		return rootView;
	}

	public void onResume() {
		super.onResume();
		this.getZWMainActivity().changeActionBar("SETTINGS", true, true, false);
	}

	public void updateSettingsVisibility() {
		//Show/hide options based on password settings
		if (ZWPreferences.userHasPassword()) {
			this.resetPasswordLabel.setText(R.string.zw_reset_password);
			this.disablePassword.setVisibility(View.VISIBLE);
		} else if (!ZWPreferences.getPasswordWarningDisabled()){
			this.disablePassword.setVisibility(View.VISIBLE);
			this.resetPasswordLabel.setText(R.string.zw_set_password);
		} else {
			this.resetPasswordLabel.setText(R.string.zw_set_password);
			this.disablePassword.setVisibility(View.GONE);
		}
		//Show/hide options based on name settings
		if (ZWPreferences.userHasSetName()){
			this.setNameLabel.setText("Change Name");
			this.disableName.setVisibility(View.VISIBLE);
		} else if (ZWPreferences.getDisabledName()) {
			this.setNameLabel.setText("Set Name");
			this.disableName.setVisibility(View.GONE);
		} else {
			this.setNameLabel.setText("Set Name");
			this.disableName.setVisibility(View.VISIBLE);
		}
		
		//show debug button text based on dis/enabled
		if (!ZWPreferences.getDebugMode()){
			this.debugButton.setText("Enable debugging");
			this.exportwalletButton.setVisibility(View.GONE);
			this.exportlogsButton.setVisibility(View.GONE);
			this.enablelogsButton.setVisibility(View.GONE);
			this.setCustomServer.setVisibility(View.GONE);

		} else {
			this.debugButton.setText("Disable debugging");
			this.exportwalletButton.setVisibility(View.VISIBLE);
			this.exportlogsButton.setVisibility(View.VISIBLE);
			this.enablelogsButton.setVisibility(View.VISIBLE);
			this.setCustomServer.setVisibility(View.VISIBLE);
			if (ZWPreferences.getLogToFile()){
				this.enablelogsButton.setText(" Disable File logging ");
			} 
			else {
				this.enablelogsButton.setText(" Enable File logging ");
			}
		}
	}
	

	@Override
	public void onClick(View v) {
		if (v==this.resetPassword) {
			if (ZWPreferences.userHasPassword()) {
				ZiftrTextDialogFragment changePasswordDialog = new ZiftrTextDialogFragment();
				changePasswordDialog.setupDialog(R.string.zw_dialog_change_password);
				changePasswordDialog.setupTextboxes(R.string.zw_old_passphrase_hint, 
													R.string.zw_dialog_new_passphrase_hint, 
													R.string.zw_dialog_confirm_password_hint);
				
				changePasswordDialog.show(getFragmentManager(), DIALOG_CHANGE_PASSWORD_TAG);
			} 
			else {
				ZiftrTextDialogFragment createPasswordDialog = new ZiftrTextDialogFragment();
				createPasswordDialog.setupDialog(0, R.string.zw_dialog_create_password, R.string.zw_dialog_save, R.string.zw_dialog_cancel);
				createPasswordDialog.setupTextboxes(R.string.zw_dialog_new_passphrase_hint, R.string.zw_dialog_confirm_password_hint, 0);
				
				createPasswordDialog.show(getFragmentManager(), DIALOG_CREATE_PASSWORD_TAG);
			}
		} 
		else if (v==this.editableConfirmationFee){
			ZWPreferences.setFeesAreEditable(this.editableConfirmationFee.isChecked());
		} 
		else if (v == this.disablePassword){
			if (ZWPreferences.userHasPassword()){
				ZiftrTextDialogFragment disablePasswordFragment = new ZiftrTextDialogFragment();
				disablePasswordFragment.setupDialog(R.string.zw_dialog_enter_password);
				disablePasswordFragment.setupTextboxes(R.string.zw_empty_string, 0, 0);
				
				//note the tag, removing a password is just changing it to an empty password
				disablePasswordFragment.show(getFragmentManager(), DIALOG_CHANGE_PASSWORD_TAG);
			} 
			else {
				ZWPreferences.setPasswordWarningDisabled(true);
				//update settings visibility too slow with recognizing disabled password so update here
				this.disablePassword.setVisibility(View.GONE);
				this.resetPasswordLabel.setText(R.string.zw_set_password);
			}
		} 
		else if (v == this.setFiatCurrency){
			getZWMainActivity().openSetFiatCurrency();
		} 
		else if (v == this.editableConfirmationFeeBar){
			this.editableConfirmationFee.setChecked(!this.editableConfirmationFee.isChecked());
			ZWPreferences.setFeesAreEditable(this.editableConfirmationFee.isChecked());
		}
		else if (v == this.warnUnconfirmedSpending){
			ZWPreferences.setWarningUnconfirmed(this.warnUnconfirmedSpending.isChecked());
		}
		else if (v == this.enableMempoolSpendingBar) {
			this.warnUnconfirmedSpending.setChecked(!this.warnUnconfirmedSpending.isChecked());
			ZWPreferences.setWarningUnconfirmed(this.warnUnconfirmedSpending.isChecked());
		}
		else if (v == this.disableName){
			ZWPreferences.setUserName(null);
			ZWPreferences.setDisabledName(true);
			this.updateSettingsVisibility();
		}
		else if (v == this.setName){
			
			ZiftrTextDialogFragment setNameDialog = new ZiftrTextDialogFragment();
			setNameDialog.setupTextboxes();
			
			setNameDialog.setupDialog(R.string.zw_app_name, R.string.zw_dialog_enter_name, R.string.zw_dialog_save, R.string.zw_dialog_cancel);
			
			setNameDialog.show(getFragmentManager(), DIALOG_SET_NAME_TAG);
		}
		else if (v == this.debugButton) {
			if (ZWPreferences.getDebugMode()){
				ZWPreferences.setDebugMode(false);
				resetLoggerHelper();
				//re-init coins to not show testnet in non-debug mode
				ZWDataSyncHelper.updateCoinData();
				this.updateSettingsVisibility();
			}
			else {
				ZiftrDialogFragment fragment = ZiftrDialogFragment.buildContinueCancelDialog(R.string.debug_warning);
				fragment.show(getFragmentManager(), DIALOG_ENABLE_DEBUG_TAG);
			}
		} 
		else if (v == this.exportwalletButton) {
			File wallet = new File(this.getActivity().getExternalFilesDir(null), "wallet.dat");
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("plain/text");
			intent.putExtra(android.content.Intent.EXTRA_STREAM, Uri.fromFile(wallet));
			startActivity(Intent.createChooser(intent, "Send backup"));
		}
		else if (v == this.exportlogsButton){ 
			File log = new File(this.getActivity().getExternalFilesDir(null) + "/logs", "Zlog.txt");
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("plain/text");
			intent.putExtra(android.content.Intent.EXTRA_STREAM, Uri.fromFile(log));
			startActivity(Intent.createChooser(intent, "Send logs"));
		}
		else if (v == this.enablelogsButton){
			if (ZWPreferences.getLogToFile()){
				resetLoggerHelper();
			}
			else {
				ZWPreferences.setLogToFile(true);
				ZLog.setLogger(ZLog.FILE_LOGGER);
			}
			this.updateSettingsVisibility();
		}
		else if(v == this.setCustomServer) {
			ZiftrTextDialogFragment customServerDialog = new ZiftrTextDialogFragment();
			customServerDialog.setupDialog(R.string.zw_debug_change_server);
			customServerDialog.setupTextboxTop(ZWPreferences.getCustomAPIServer(), null);
			
			customServerDialog.show(getFragmentManager(), DIALOG_CHANGE_SERVER_TAG);
		}
	}
	
	private void resetLoggerHelper(){
		ZWPreferences.setLogToFile(false);
		if (ZWApplication.isDebuggable() || ZWPreferences.getDebugMode()){
			ZLog.setLogger(ZLog.ANDROID_LOGGER);
		} else {
			ZLog.setLogger(ZLog.NOOP_LOGGER);
		}
	}

}