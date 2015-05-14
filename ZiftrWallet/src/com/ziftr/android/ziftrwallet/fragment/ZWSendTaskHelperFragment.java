/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.fragment;

import java.math.BigInteger;
import java.util.List;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWApplication;
import com.ziftr.android.ziftrwallet.ZWPreferences;
import com.ziftr.android.ziftrwallet.ZWWalletManager;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWRawTransaction;
import com.ziftr.android.ziftrwallet.dialog.ZWSendConfirmationDialog;
import com.ziftr.android.ziftrwallet.dialog.ZiftrDialogManager;
import com.ziftr.android.ziftrwallet.dialog.ZiftrSimpleDialogFragment;
import com.ziftr.android.ziftrwallet.dialog.ZiftrTaskDialogFragment;
import com.ziftr.android.ziftrwallet.dialog.ZiftrTextDialogFragment;
import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.network.ZWDataSyncHelper;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWSendTaskHelperFragment extends Fragment {
	
	public  static final String FRAGMENT_TAG = "send_task_helper";
	
	private final String DIALOG_BUILDING_TRANSACTION = "building_transaction";
	private final String DIALOG_SIGNING_TRANSACTION = "signing_transaction";
	private final String DIALOG_ENTER_PASSWORD = "enter_password";
	private final String DIALOG_WRONG_PASSWORD = "wrong_password";
	private final String DIALOG_CONFIRM_SENDING = "confirm_sending";
	private final String DIALOG_WARNING_UNCONFIRMED = "unconfirmed_warning";
	
	private ZWCoin coin; //coin type being sent
	private BigInteger feePerKb; //user's preferred feePerKb for this transaction
	private String outputAddress; //the public address the user is sending coins to
	private String outputAddressLabel; //the lable the user put for the address they're sending to (when it auto saves to address book)
	private BigInteger amount; //the number of coins the user wants to send
	private String password;
	
	private ZWRawTransaction rawTransaction;
	
	public ZWSendTaskHelperFragment() {
		this.setRetainInstance(true);
	}
	

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	public void onDetach() {
		// TODO Auto-generated method stub
		super.onDetach();
	}

	
	
	
	public void sendCoins(ZWCoin coin, BigInteger feePerKb, BigInteger amount, String outputAddress, String outputLabel){
		
		//set all the variables the task needs then start the actual sending process
		this.coin = coin;
		this.feePerKb = feePerKb;
		this.amount = amount;
		this.outputAddress = outputAddress;
		this.outputAddressLabel = outputLabel;
		this.password = null;
		
		//now we need to get the user's password (if they have one set)
		//this authorizes the transaction, but it's also needed to create change addresses
		if(ZWPreferences.userHasPassword()) {
			this.password = ZWPreferences.getCachedPassword();
			if(password == null) {
				getPassword();
			}
			else {
				startSending();
			}
		}
		else {
			startSending();
		}
	}
	
	
	private void startSending() {
		
		//make sure the user isn't trying to send 0 coins
		if(amount.compareTo(BigInteger.ZERO) == 0) {
			ZiftrDialogManager.showSimpleAlert(getFragmentManager(), R.string.zw_dialog_send_zero);
			return;
		}
		
		//get our list of inputs to use for spending
		final List<String> inputs = ZWWalletManager.getInstance().getAddressList(coin, false);
		
		//now we either have the user's password, or they don't have one set,
		//so we need to contact the server and have a transaction built
		ZiftrTaskDialogFragment buildingTransactionFragment = new ZiftrTaskDialogFragment() {
			
			@Override
			protected boolean doTask() {

				try {
					Thread.sleep(700); //quick sleep so the UI doesn't "flash" the loading message
				} 
				catch (InterruptedException e) {
					//do nothing, just quick sleep
				}
				
				//first save this sending address into the user's address book
				try {
					ZWWalletManager.getInstance().createSendingAddress(coin, outputAddress, outputAddressLabel);
				} 
				catch (ZWAddressFormatException e) {
					return false;
				}
				
				//we'll need to get a change address to store any change from the transaction
				//find (or create) an address for change that hasn't been spent from
				List<String> changeAddresses = ZWWalletManager.getInstance().getHiddenAddressList(coin, false);
				String changeAddress;
				if (changeAddresses.size() <= 0){
					//create new address for change
					changeAddress = ZWWalletManager.getInstance().createChangeAddress(password, coin).getAddress();
					ZLog.log(changeAddress);
				} else {
					//or reuse one that hasnt been spent from
					changeAddress = changeAddresses.get(0);
				}
				
				rawTransaction = ZWDataSyncHelper.fetchRawTransaction(coin, feePerKb, amount, inputs, outputAddress, changeAddress);
				return true;
			}
		};
		
		buildingTransactionFragment.setupDialog(R.string.zw_dialog_sending);
		buildingTransactionFragment.setOnClickListener(new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//this is where we handle the response from getting the raw transaction data from the server
				if(which == DialogInterface.BUTTON_POSITIVE) {
					handleRawTransaction();
				}
				else if(which == DialogInterface.BUTTON_NEGATIVE) {
					ZiftrDialogManager.showSimpleAlert(getFragmentManager(), R.string.zw_dialog_error_address_format);
				}
			}
		});
		
		buildingTransactionFragment.show(getFragmentManager(), DIALOG_BUILDING_TRANSACTION);
	}

	
	private void handleRawTransaction() {
		
		if(rawTransaction == null) {
			ZiftrDialogManager.showSimpleAlert(getFragmentManager(), R.string.zw_dialog_error_send_noconnection);
		}
		else if(rawTransaction.getErrorCode() == 0 && rawTransaction.getErrorMessage() == null) {
			
			ZWSendConfirmationDialog confirmDialog = new ZWSendConfirmationDialog();
			//confirmDialog.setupDialog(title, message, send, cancel);
			confirmDialog.setupConfirmationDetails(rawTransaction.getCoin().getSymbol(), rawTransaction.getTotalSpent(), rawTransaction.getFee(), rawTransaction.getAddress());
			confirmDialog.setCancelable(false);
			
			confirmDialog.setOnClickListener(new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
					if(which == DialogInterface.BUTTON_POSITIVE) {
						//if the user clicked "send"
						
						if(rawTransaction.usingUnconfirmedInputs() && ZWPreferences.getWarningUnconfirmed()) {
							showUnconfirmedInputsWarning();
						}
						else {
							signRawTransaction();
						}
					}
					
				}
			});
			
			confirmDialog.show(getFragmentManager(), DIALOG_CONFIRM_SENDING);
		}
		else {
			int error = rawTransaction.getErrorCode();
			if(error == ZWRawTransaction.ERROR_CODE_BAD_DATA) {
				ZiftrDialogManager.showSimpleAlert(getFragmentManager(), R.string.zw_dialog_error_send_badtransaction);
			}
			else if(error == -1) {
				//probably a generic network error
				ZiftrDialogManager.showSimpleAlert(getFragmentManager(), R.string.zw_dialog_error_send_noconnection);
			}
			else {
				String errorMessage = ZWApplication.getApplication().getString(R.string.zw_dialog_error_send_returnederror);
				errorMessage = String.format(errorMessage, String.valueOf(rawTransaction.getErrorCode()), rawTransaction.getErrorMessage());
				ZiftrDialogManager.showSimpleAlert(getFragmentManager(), errorMessage);
			}
		}
	}

	
	
	private void showUnconfirmedInputsWarning() {
		ZiftrSimpleDialogFragment unconfirmedDialog = new ZiftrSimpleDialogFragment();
		unconfirmedDialog.setupDialog(R.string.zw_app_name, R.string.zw_dialog_warning_unconfirmed, R.string.zw_dialog_send, R.string.zw_dialog_cancel);
		unconfirmedDialog.setCancelable(false);
		
		unconfirmedDialog.setOnClickListener(new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(which == DialogInterface.BUTTON_POSITIVE) {
					signRawTransaction();
				}
			}
		});
		
		unconfirmedDialog.show(getFragmentManager(), DIALOG_WARNING_UNCONFIRMED);
	}
	
	
	private void signRawTransaction() {
		
		ZiftrTaskDialogFragment signTaskFragment = new ZiftrTaskDialogFragment() {
			
			@Override
			protected boolean doTask() {
				boolean signingSuccessful = ZWDataSyncHelper.signRawTransaction(rawTransaction, password);
				return signingSuccessful;
			}
		};
		
		signTaskFragment.setupDialog(R.string.zw_dialog_sending);
		signTaskFragment.setOnClickListener(new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				//TODO these need to be implemented for real, just throwing them in for testing/debuggin
				if(which == DialogInterface.BUTTON_POSITIVE) {
					taskFinished();
				}
				else {
					ZiftrDialogManager.showSimpleAlert(getFragmentManager(), "Failed to sign transaction.");
				}
				
			}
		});
		
		signTaskFragment.show(getFragmentManager(), DIALOG_SIGNING_TRANSACTION);
	}
	
	
	
	/**
	 * Has the user enter their password, then, if correct, re-calls sendCoins()
	 */
	private void getPassword() {
		final ZiftrTextDialogFragment passwordDialog = new ZiftrTextDialogFragment();
		passwordDialog.setupDialog(R.string.zw_dialog_enter_password);
		passwordDialog.setupTextboxes();
		passwordDialog.setOnClickListener(new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String enteredPassword = passwordDialog.getEnteredTextTop();
				byte[] inputHash = ZiftrUtils.saltedHash(enteredPassword);
				
				if (ZWPreferences.inputHashMatchesStoredHash(inputHash)) {
					ZWPreferences.setCachedPassword(enteredPassword);
					ZWSendTaskHelperFragment.this.password = enteredPassword;
					startSending();
				}
				else {
					ZiftrSimpleDialogFragment wrongPassword = new ZiftrSimpleDialogFragment();
					wrongPassword.setupDialog(0, R.string.zw_incorrect_password, R.string.zw_dialog_ok, 0);
					wrongPassword.show(getFragmentManager(), DIALOG_WRONG_PASSWORD);
				}
			}
		});
		
		passwordDialog.show(getFragmentManager(), DIALOG_ENTER_PASSWORD);
	}

	
	private void taskFinished() {
		getFragmentManager().beginTransaction().remove(this).commit();
		
		ZWSendCoinsFragment sendFragment = (ZWSendCoinsFragment) getFragmentManager().findFragmentByTag(ZWSendCoinsFragment.FRAGMENT_TAG);
		if(sendFragment != null) {
			sendFragment.finishedSending(rawTransaction.getCoin());
		}
	}
}
