/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */
package com.ziftr.android.ziftrwallet;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.ziftr.android.ziftrwallet.ZWMainFragmentActivity.FragmentType;
import com.ziftr.android.ziftrwallet.crypto.ZWAddress;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.dialog.ZiftrDialogHandler;
import com.ziftr.android.ziftrwallet.dialog.ZiftrDialogManager;
import com.ziftr.android.ziftrwallet.dialog.ZiftrSimpleDialogFragment;
import com.ziftr.android.ziftrwallet.dialog.ZiftrTextDialogFragment;
import com.ziftr.android.ziftrwallet.exceptions.ZWDataEncryptionException;
import com.ziftr.android.ziftrwallet.fragment.ZWAddressListAdapter;
import com.ziftr.android.ziftrwallet.fragment.ZWManageHdAccountFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWNewCurrencyFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWReceiveCoinsFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWSettingsFragment;
import com.ziftr.android.ziftrwallet.network.ZWDataSyncHelper;
import com.ziftr.android.ziftrwallet.network.ZiftrNetworkManager;
import com.ziftr.android.ziftrwallet.sqlite.ZWWalletManager;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWActivityDialogHandler implements ZiftrDialogHandler {

	public static final String DIALOG_DATABASE_ERROR_TAG = "activity_database_error";
	public static final String DIALOG_OLD_PASSWORD_TAG = "activity_old_password";

	FragmentActivity activity;

	public ZWActivityDialogHandler(FragmentActivity activity) {
		this.activity = activity;
	}


	public FragmentManager getSupportFragmentManager() {
		return this.activity.getSupportFragmentManager();
	}


	public Fragment getFragment(FragmentType fragmentType) {
		return this.getSupportFragmentManager().findFragmentByTag(fragmentType.toString());
	}


	
	public static boolean changePassword(final FragmentActivity activity, final String oldPassword, final String newPassword) {
		
		try {
			boolean encryptionChanged = ZWWalletManager.getInstance().changeEncryptionOfReceivingAddresses(oldPassword, newPassword);
			
			//tried to set password on encrypted database private keys
			if (!encryptionChanged){
				activity.runOnUiThread(new Runnable(){
					@Override
					public void run() {
						ZiftrDialogManager.showSimpleAlert(activity.getSupportFragmentManager(), R.string.zw_dialog_old_encryption_fatal);	
					}

				});

			} 
			else {
				//set password
				if (ZWPreferences.setStoredPassword(newPassword) <= 0) {
					//if we failed setting password, something could be wrong with db
					ZiftrSimpleDialogFragment dbErrorFragment = new ZiftrTextDialogFragment();
					dbErrorFragment.setupDialog(R.string.zw_app_name, 
							R.string.zw_dialog_database_error, 
							R.string.zw_dialog_restart, 
							R.string.zw_dialog_cancel);

					dbErrorFragment.show(activity.getSupportFragmentManager(), ZWActivityDialogHandler.DIALOG_DATABASE_ERROR_TAG);
				}
				else {
					//if we actually managed to successfully change the password
					return true;
				}
			}
		} 
		catch (ZWDataEncryptionException e) {
			ZLog.log("Exception trying to change encryption: ", e);
			
			//this can happen if an older version of the database somehow loses it's encryption flag
			//(typically caused by users attempting manual backups with running app)
			//if that's the case the user may be able to recover by entering their old password
			activity.runOnUiThread(new Runnable(){
				@Override
				public void run() {
					ZiftrTextDialogFragment oldPasswordDialog = new ZiftrTextDialogFragment();
					oldPasswordDialog.setupDialog(R.string.zw_dialog_old_encryption);
					oldPasswordDialog.addEmptyTextbox(true);

					oldPasswordDialog.show(activity.getSupportFragmentManager(), ZWActivityDialogHandler.DIALOG_OLD_PASSWORD_TAG);	
				}

			});
		}
		
		return false;

	}
	


	@Override
	public void handleDialogYes(DialogFragment fragment) {
		if(ZWReceiveCoinsFragment.DIALOG_NEW_ADDRESS_TAG.equals(fragment.getTag())) {
			//user is creating a new address
			ZWReceiveCoinsFragment receiveFragment = 
					(ZWReceiveCoinsFragment) getSupportFragmentManager().findFragmentByTag(ZWReceiveCoinsFragment.FRAGMENT_TAG);
			receiveFragment.createNewAddress();
		}
		else if(ZWNewCurrencyFragment.DIALOG_ENTER_PASSWORD_TAG.equals(fragment.getTag())) {
			String enteredPassword = ((ZiftrTextDialogFragment)fragment).getEnteredText(0);
			if (ZWPreferences.inputPasswordMatchesStoredPassword(enteredPassword)) {
				ZWPreferences.setCachedPassword(enteredPassword);
				ZWNewCurrencyFragment newCurrFragment = 
						(ZWNewCurrencyFragment) getSupportFragmentManager().findFragmentByTag(ZWNewCurrencyFragment.FRAGMENT_TAG);
				if (newCurrFragment != null) {
					newCurrFragment.makeNewAccount((ZWCoin)((ZiftrTextDialogFragment)fragment).getData(), enteredPassword);
				}
			}
			else {
				ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_incorrect_password);
			}
		}
		else if(ZWReceiveCoinsFragment.DIALOG_ENTER_PASSWORD_TAG.equals(fragment.getTag())) {
			String enteredPassword = ((ZiftrTextDialogFragment)fragment).getEnteredText(0);
			if (ZWPreferences.inputPasswordMatchesStoredPassword(enteredPassword)) {
				ZWPreferences.setCachedPassword(enteredPassword);
				ZWReceiveCoinsFragment receiveFragment = 
						(ZWReceiveCoinsFragment) getSupportFragmentManager().findFragmentByTag(ZWReceiveCoinsFragment.FRAGMENT_TAG);
				if (receiveFragment != null){
					receiveFragment.createNewAddress();
				}
			}
			else {
				ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_incorrect_password);
			}
		}
		else if(ZWSettingsFragment.DIALOG_ENABLE_DEBUG_TAG.equals(fragment.getTag())) {
			ZWPreferences.setDebugMode(true);
			ZWDataSyncHelper.updateCoinData();
			ZWSettingsFragment settingsFragment = (ZWSettingsFragment)this.getFragment(FragmentType.SETTINGS_FRAGMENT_TYPE);
			if(settingsFragment != null) {
				settingsFragment.updateSettingsVisibility();
			}
		}
		else if(ZWSettingsFragment.DIALOG_CREATE_PASSWORD_TAG.equals(fragment.getTag())) {
			ZiftrTextDialogFragment passwordFragment = (ZiftrTextDialogFragment) fragment;
			String password = passwordFragment.getEnteredText(0);
			String confirmedPassword = passwordFragment.getEnteredText(1);

			if(password.equals(confirmedPassword)) {
				if (ZWActivityDialogHandler.changePassword(this.activity, null, password)) {
					ZWPreferences.setPasswordWarningDisabled(true);
					ZWSettingsFragment settingsFragment = (ZWSettingsFragment)this.getFragment(FragmentType.SETTINGS_FRAGMENT_TYPE);
					if(settingsFragment != null) {
						settingsFragment.updateSettingsVisibility();	
					}
					ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_set_password_complete);
				} 
				else {
					//this is an epic failure, it basically means the database is corrupted, or somehow double encrypted
					//the change password method will alert the user with an error
					return;
				}
			}
			else {
				ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_dialog_new_password_match);
			}
		}
		else if(ZWSettingsFragment.DIALOG_CHANGE_PASSWORD_TAG.equals(fragment.getTag())) {
			ZiftrTextDialogFragment passwordFragment = (ZiftrTextDialogFragment) fragment;
			String oldPassword = passwordFragment.getEnteredText(0);
			String newPassword = passwordFragment.getEnteredText(1);
			String confirmPassword = passwordFragment.getEnteredText(2);

			if(newPassword == confirmPassword || newPassword.equals(confirmPassword)) {

				if (ZWPreferences.inputPasswordMatchesStoredPassword(oldPassword)) {
					//everything looks ok, try and change passwords
					if (ZWActivityDialogHandler.changePassword(this.activity, oldPassword, newPassword)) {
						ZWPreferences.setPasswordWarningDisabled(true);
						ZWSettingsFragment settingsFragment = (ZWSettingsFragment)this.getFragment(FragmentType.SETTINGS_FRAGMENT_TYPE);
						if(settingsFragment != null) {
							settingsFragment.updateSettingsVisibility();	
						}

						//if newpassword is null, it means the password is being removed
						if(newPassword == null) {
							ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_disabled_password);
						}
						else {
							ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_set_password_complete);
						}

					} 
					else {
						//this is an epic failure, it basically means the database is corrupted, or somehow double encrypted
						//user will be alerted by change password error handling
						return;
					}
				}
				else {
					//entered password wrong
					ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_incorrect_reset_password);
				}
			}
			else {
				//new passwords don't match
				ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_incorrect_reset_password_match);
			}

		}
		else if(ZWActivityDialogHandler.DIALOG_OLD_PASSWORD_TAG.equals(fragment.getTag())) {
			ZiftrTextDialogFragment passwordFragment = (ZiftrTextDialogFragment) fragment;
			String password = passwordFragment.getEnteredText(0);

			boolean encryptionChanged = false;
			
			try {
				encryptionChanged = ZWWalletManager.getInstance().changeEncryptionOfReceivingAddresses(password, null);
			} 
			catch (ZWDataEncryptionException e) {
				//this would only happen if some of the data is somehow not encrypted correctly
				//it's a fatal error, so show user message, but not much else can be done
				ZLog.log("Exception removing password: ", e);
			}
			
			if (encryptionChanged){
				ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_dialog_old_encryption_changed);
			} 
			else {
				ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_dialog_old_encryption_failed);
			}

		}
		else if(ZWActivityDialogHandler.DIALOG_DATABASE_ERROR_TAG.equals(fragment.getTag())) {
			//user pressed the restart button
			System.exit(0);
		}
		else if(ZWSettingsFragment.DIALOG_SET_NAME_TAG.equals(fragment.getTag())) {
			ZiftrTextDialogFragment nameFragment = (ZiftrTextDialogFragment) fragment;
			String enteredName = nameFragment.getEnteredText(0);

			if(enteredName == null || enteredName.length() == 0) {
				ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_dialog_blank_name);
			}
			else {
				ZWPreferences.setUserName(enteredName);
				ZWSettingsFragment settingsFragment = (ZWSettingsFragment) getFragment(FragmentType.SETTINGS_FRAGMENT_TYPE);
				if(settingsFragment != null) {
					settingsFragment.updateSettingsVisibility();
				}

				//for the name confirmation dialog we need to assemble the strings before we create the dialog
				//this way we can append the user's entered name
				ZiftrSimpleDialogFragment nameSetDialog = new ZiftrSimpleDialogFragment();

				String dialogTitle = activity.getString(R.string.zw_app_name);
				String message = activity.getString(R.string.zw_dialog_set_name_complete) + enteredName;
				String dialogYes = activity.getString(R.string.zw_dialog_continue);

				nameSetDialog.setupDialog(dialogTitle, message, dialogYes, null);
				nameSetDialog.show(getSupportFragmentManager(), "activity_set_name_complete");
			}
		}
		else if(ZWAddressListAdapter.DIALOG_EDIT_ADDRESS_TAG.equals(fragment.getTag())) {
			ZiftrTextDialogFragment editLabelFragment = (ZiftrTextDialogFragment) fragment;

			final String newLabel = editLabelFragment.getEnteredText(0);
			final ZWAddress editedAddress = (ZWAddress) editLabelFragment.getData();

			ZiftrUtils.runOnNewThread(new Runnable() {

				@Override
				public void run() {

					if(editedAddress != null) {
						ZWWalletManager.getInstance().updateAddressLabel(editedAddress, newLabel);

						//in this case the user updated the data directly instead of data downloaded from the internet, 
						//but this will let the rest of the UI know
						ZiftrNetworkManager.dataUpdated();
					}
				}
			});

		}
		else if(ZWSettingsFragment.DIALOG_CHANGE_SERVER_TAG.equals(fragment.getTag())) {
			ZiftrTextDialogFragment changeServerDialog = (ZiftrTextDialogFragment) fragment;
			String customServer = changeServerDialog.getEnteredText(0);
			ZWPreferences.setCustomAPIServer(customServer);
		}
		else if(ZWManageHdAccountFragment.DIALOG_READ_HD_SENTENCE_TAG.equals(fragment.getTag())) {
			String enteredPassword = ((ZiftrTextDialogFragment)fragment).getEnteredText(0);
			if (ZWPreferences.inputPasswordMatchesStoredPassword(enteredPassword)) {
				ZWPreferences.setCachedPassword(enteredPassword);
				ZWManageHdAccountFragment manageHdAccountFragment = 
						(ZWManageHdAccountFragment) getSupportFragmentManager().findFragmentByTag(ZWManageHdAccountFragment.FRAGMENT_TAG);
				if (manageHdAccountFragment != null){
					manageHdAccountFragment.readMnemonic(enteredPassword);
				}
			}
			else {
				ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_incorrect_password);
			}
		}
		else if(ZWManageHdAccountFragment.DIALOG_WRITE_HD_SENTENCE_TAG.equals(fragment.getTag())) {
			String enteredPassword = ((ZiftrTextDialogFragment)fragment).getEnteredText(0);
			if (ZWPreferences.inputPasswordMatchesStoredPassword(enteredPassword)) {
				ZWPreferences.setCachedPassword(enteredPassword);
				ZWManageHdAccountFragment manageHdAccountFragment = 
						(ZWManageHdAccountFragment) getSupportFragmentManager().findFragmentByTag(ZWManageHdAccountFragment.FRAGMENT_TAG);
				if (manageHdAccountFragment != null){
					manageHdAccountFragment.writeMnemonic(enteredPassword);
				}
			}
			else {
				ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_incorrect_password);
			}
		}

	}

	@Override
	public void handleDialogNo(DialogFragment fragment) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleDialogCancel(DialogFragment fragment) {
		// TODO Auto-generated method stub

	}

}
