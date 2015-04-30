package com.ziftr.android.ziftrwallet.dialog;

import java.math.BigInteger;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWApplication;
import com.ziftr.android.ziftrwallet.ZWPreferences;
import com.ziftr.android.ziftrwallet.ZWWalletManager;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWRawTransaction;
import com.ziftr.android.ziftrwallet.fragment.ZWSendCoinsFragment;
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
		}
		else {
			startSending();
		}
	}
	
	
	private void startSending() {
		
		//get our list of inputs to use for spending
		final List<String> inputs = ZWWalletManager.getInstance().getAddressList(coin, false);
		
		//now we either have the user's password, or they don't have one set,
		//so we need to contact the server and have a transaction built
		ZiftrTaskDialogFragment buildingTransactionFragment = new ZiftrTaskDialogFragment() {
			
			@Override
			protected boolean doTask() {
				
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) { 
					// TODO Auto-generated catch block
					e.printStackTrace();
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
				handleRawTransaction();
			}
		});
		
		buildingTransactionFragment.show(getFragmentManager(), DIALOG_BUILDING_TRANSACTION);
	}

	
	private void handleRawTransaction() {
		
		if(rawTransaction == null) {
			ZiftrDialogManager.showSimpleAlert(getFragmentManager(), R.string.zw_dialog_error_send_noconnection);
		}
		else if(rawTransaction.getErrorCode() == 0 && rawTransaction.getErrorMessage() == null) {
			
			//TODO build dialog from raw transaction data, then show confirmation dialog
			//show follow  up confirmation if user has warnings for unconfirmed transactions turned on and this transaction needs them
			//then actually sign the transaction
			
			
			
			Context appContext = ZWApplication.getApplication(); //use app context incase we're in the background when this apps
			
			String title = appContext.getString(R.string.zw_app_name);
			String send = appContext.getString(R.string.zw_dialog_send);
			String cancel = appContext.getString(R.string.zw_dialog_cancel);
			
			String message = appContext.getString(R.string.zw_dialog_send_confirmation);
			String transactionCost = rawTransaction.getTotalSpent() + " " + rawTransaction.getCoin().getSymbol();
			if(rawTransaction.getFee() != null) {
				String feeString = appContext.getString(R.string.zw_dialog_send_fee);
				feeString = String.format(feeString, rawTransaction.getFee());
				transactionCost += feeString;
			}
			message = String.format(message, transactionCost, rawTransaction.getAddress());
			
			ZiftrDialogFragment confirmDialog = new ZiftrDialogFragment();
			confirmDialog.setupDialog(title, message, send, cancel);
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
		ZiftrDialogFragment unconfirmedDialog = new ZiftrDialogFragment();
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
					startSending();
				}
				else {
					ZiftrDialogFragment wrongPassword = new ZiftrDialogFragment();
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
