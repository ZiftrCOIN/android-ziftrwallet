/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.fragment;

import java.math.BigDecimal;
import java.math.BigInteger;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWMainFragmentActivity;
import com.ziftr.android.ziftrwallet.ZWPreferences;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWCoinFiatTextWatcher;
import com.ziftr.android.ziftrwallet.crypto.ZWCoinURI;
import com.ziftr.android.ziftrwallet.crypto.ZWReceivingAddress;
import com.ziftr.android.ziftrwallet.dialog.ZiftrDialogManager;
import com.ziftr.android.ziftrwallet.dialog.ZiftrSimpleDialogFragment;
import com.ziftr.android.ziftrwallet.dialog.ZiftrTaskDialogFragment;
import com.ziftr.android.ziftrwallet.dialog.ZiftrTextDialogFragment;
import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.sqlite.ZWWalletManager;
import com.ziftr.android.ziftrwallet.util.QRCodeEncoder;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrTextWatcher;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWReceiveCoinsFragment extends ZWAddressBookParentFragment {

	/** The key used to save the current address in bundles. */
	private static final String KEY_ADDRESS = "KEY_ADDRESS";
	public static final String FRAGMENT_TAG = "receive_coins_fragment";
	public static final String DIALOG_NEW_ADDRESS_TAG = "receive_new_address";
	public static final String DIALOG_ENTER_PASSWORD_TAG = "receive_enter_password";
	public static final String DIALOT_TASK_CREATE_HD_ACCOUNT = "receive_create_hd_account";

	private static final int FADE_DURATION = 500;

	/** The view container for this fragment. */
	private View rootView;

	private ImageView copyButton;
	private ImageView qrCodeImageView;
	private View qrCodeContainer;
	private View scrollView;
	private ImageView generateAddressForLabel;
	private EditText messageEditText;
	private ImageView helpButton;

	private ZWReceivingAddress prefilledAddress;

	private boolean qrCodeGenerated = false;

	/**
	 * Inflate, initialize, and return the send coins layout.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.qrCodeGenerated = false;

		this.rootView = inflater.inflate(R.layout.accounts_receive_coins, container, false);
		this.populateWalletHeader(rootView.findViewById(R.id.walletHeader));

		this.initializeViewFields(inflater, container);

		this.initializeQrCodeFromBundle(savedInstanceState);

		return this.rootView;
	}

	@Override
	public void onResume(){
		super.onResume();
		refreshAddNewAddressButtonsEnabled();
	}

	@Override
	public void onStop() {
		this.qrCodeGenerated = false;
		super.onStop();
	}

	@Override
	public void onClick(View v) {

		if (v == this.copyButton) {
			if(this.fragmentHasAddress()) {
				// Gets a handle to the clipboard service.
				ClipboardManager clipboard = (ClipboardManager)
						getActivity().getSystemService(Context.CLIPBOARD_SERVICE);

				ClipData clip = ClipData.newPlainText(KEY_ADDRESS, addressEditText.getText().toString());
				clipboard.setPrimaryClip(clip);

				Toast.makeText(getActivity(), "Text copied.", Toast.LENGTH_SHORT).show();
			}
		} 
		else if (v == this.qrCodeImageView && !fragmentHasAddress()) {
			//only generate a new address if there isn't already one generated (in the address box)
			this.generateNewAddress();
		} 
		else if (v == this.getAddressBookImageView()) {
			this.openAddressBook(new ZWReceiveAddressBookFragment(), R.id.receiveCoinBaseFrameLayout);
		} 
		else if (v == this.generateAddressForLabel) {
			this.generateNewAddress();
		} 
		else if (v == this.helpButton){
			ZiftrDialogManager.showSimpleAlert(getFragmentManager(), R.string.zw_dialog_receive_help);
		}

	}



	private void generateNewAddress() {
		
		showNewAddressConfirmationDialog();
		
		/**
		if (ZWWalletManager.getInstance().hasHdAccount(this.getSelectedCoin())){
			showNewAddressConfirmationDialog();
		}
		else {
			//if we don't have an hd account for this coin, we need to create one
			//which may require a password
			String cachedPassword = ZWPreferences.getCachedPassword();
			if(ZWPreferences.userHasPassword() && cachedPassword == null){
				showEnterPasswordDialog();
			} 
			else {
				showNewAddressConfirmationDialog();
			}
		}
		**/
	}
	
	
	private void showEnterPasswordDialog() {
		
		ZiftrTextDialogFragment passwordDialog = new ZiftrTextDialogFragment();
		passwordDialog.setupDialog(R.string.zw_dialog_enter_password);
		passwordDialog.addEmptyTextbox(true);
		
		passwordDialog.show(getFragmentManager(), DIALOG_ENTER_PASSWORD_TAG);
	}
	

	private void showNewAddressConfirmationDialog() {
		ZiftrSimpleDialogFragment dialog = ZiftrSimpleDialogFragment.buildContinueCancelDialog(R.string.zw_dialog_create_address);
		dialog.show(getFragmentManager(), DIALOG_NEW_ADDRESS_TAG);
	}



	/**
	 * @param inflater
	 * @param container
	 */
	private void initializeViewFields(LayoutInflater inflater, ViewGroup container) {
		super.initializeViewFields(this.rootView, R.id.recallAddressFromHistoryIcon);

		this.scrollView = this.rootView.findViewById(R.id.receiveCoinsContainingScrollView);


		// For the amounts and the binding
		this.coinAmountEditText = (EditText) this.rootView.findViewById(R.id.receiveAmountCoinFiatDualView
				).findViewById(R.id.dualTextBoxLinLayout1).findViewById(R.id.customEditText);
		this.coinAmountEditText.setId(R.id.zw_receive_coin_amount);
		this.coinAmountEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);

		this.fiatAmountEditText = (EditText) this.rootView.findViewById(R.id.receiveAmountCoinFiatDualView
				).findViewById(R.id.dualTextBoxLinLayout2).findViewById(R.id.customEditText);
		this.fiatAmountEditText.setId(R.id.zw_receive_fiat_amount);
		this.fiatAmountEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);

		ZWCoinFiatTextWatcher coinTextWatcher = new ZWCoinFiatTextWatcher(getSelectedCoin(), coinAmountEditText, fiatAmountEditText);
		coinAmountEditText.addTextChangedListener(coinTextWatcher);
		fiatAmountEditText.addTextChangedListener(coinTextWatcher);

		this.helpButton = (ImageView) this.rootView.findViewById(R.id.help_msg_button);
		this.helpButton.setOnClickListener(this);
		// For the message edit text
		this.messageEditText = 
				(EditText) this.rootView.findViewById(R.id.receiveMessageContainer).findViewById(R.id.customEditText);
		this.messageEditText.setId(R.id.zw_receive_message);

		this.addressEditText = (EditText) this.rootView.findViewById(R.id.addressValueTextView);

		this.copyButton = (ImageView) this.rootView.findViewById(R.id.receiveCopyIcon);
		this.copyButton.setOnClickListener(this);

		this.qrCodeImageView = (ImageView) this.rootView.findViewById(R.id.generateAddressQrCodeImageView);
		this.qrCodeImageView.setOnClickListener(this);
		if (fragmentHasAddress()) {
			this.qrCodeImageView.setScaleType(ScaleType.FIT_XY);
		}
		this.qrCodeContainer = this.rootView.findViewById(R.id.generateAddressQrCodeContainer);

		this.generateAddressForLabel = (ImageView) this.rootView.findViewById(R.id.generateNewAddressForLabel);
		this.generateAddressForLabel.setOnClickListener(this);

		this.labelEditText = 
				(EditText) this.rootView.findViewById(R.id.addressName).findViewById(R.id.customEditText);
		this.labelEditText.setId(R.id.zw_receive_address_label);
		this.labelEditText.requestFocus();
		this.labelEditText.addTextChangedListener(new ZiftrTextWatcher() {
			@Override
			public void afterTextChanged(Editable arg0) {
				if (fragmentHasAddress()) {
					updateAddressLabelInDatabase();
				}
				refreshAddNewAddressButtonsEnabled();
			}
		});

		if (this.prefilledAddress != null) {
			this.addressEditText.setText(this.prefilledAddress.getAddress());
			this.labelEditText.setText(this.prefilledAddress.getLabel());
		}

		this.refreshAddNewAddressButtonsEnabled();
	}

	private void generateQrCode(boolean withAnimation) {
		String addressString = this.addressEditText.getText().toString();

		String amountString = this.coinAmountEditText.getText().toString();
		BigInteger amountVal = null;
		try {
			amountVal = getSelectedCoin().getAtomicUnits(new BigDecimal(amountString));
		} catch(NumberFormatException nfe) {
			// Amount is just left null
		}
		if (amountVal != null && (amountVal.compareTo(BigInteger.ZERO) < 0)) {
			amountVal = null;
		}
		String label = this.labelEditText.getText().toString();
		if (label.isEmpty()){
			//if no label, use a user's name
			label = ZWPreferences.getUserName();
		}
		String message = this.messageEditText.getText().toString();
		message = message != null && message.isEmpty() ? null : message;
		String qrCodeData = ZWCoinURI.convertToCoinURI(getSelectedCoin(), addressString, amountVal, 
				label, message);
		this.qrCodeContainer.setVisibility(View.VISIBLE);
		this.qrCodeImageView.setScaleType(ScaleType.FIT_XY);
		this.qrCodeImageView.setPadding(0, 0, 0, 0);

		int qrCodeDimension = this.qrCodeImageView.getWidth();
		qrCodeDimension = Math.min(qrCodeDimension, rootView.getWidth());
		qrCodeDimension = Math.min(qrCodeDimension, rootView.getHeight());
		QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(qrCodeData, null,
				Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension/2);
		this.qrCodeImageView.getLayoutParams().width = qrCodeDimension;
		this.qrCodeImageView.getLayoutParams().height = qrCodeDimension;

		try {
			Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
			if (withAnimation) {
				Drawable[] layers = new Drawable[2];
				layers[0] = this.qrCodeImageView.getDrawable();
				layers[1] = new BitmapDrawable(getResources(), bitmap);
				TransitionDrawable transitionDrawable = new TransitionDrawable(layers);
				this.qrCodeImageView.setImageDrawable(transitionDrawable);
				transitionDrawable.startTransition(FADE_DURATION);
			} else {
				this.qrCodeImageView.setImageBitmap(bitmap);
			}
		} catch (WriterException e) {
			e.printStackTrace();
		}

		this.qrCodeGenerated = true;
	}

	/**
	 * Refreshes the display with the new address. Should only be run on 
	 * the UI thread. 
	 * 
	 * @param newAddress
	 */
	private void initializeQrCodeFromBundle(Bundle savedInstanceState) {
		if (this.fragmentHasAddress()) {
			// We have to set it to be invisible temporarily to not have a blue flash,
			// will be drawn correctly once we can get the measurement of the qr code
			this.qrCodeContainer.setVisibility(View.INVISIBLE);
		} 

		ViewTreeObserver viewObserver = qrCodeImageView.getViewTreeObserver();
		viewObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				if (getActivity() != null) {
					if (fragmentHasAddress() && !qrCodeGenerated) {
						generateQrCode(false);
					}
				}
			}
		});
	}
	
	
	
	
	public void createNewAddress() {
		
		ZWCoin coin = getSelectedCoin();
		if (!ZWWalletManager.getInstance().hasHdAccount(coin)){
			this.upgradeToHdAccount(coin);
		}
		else {
			ZiftrUtils.runOnNewThread(new Runnable() {
				
				@Override
				public void run() {
					createNewAddressFromDatabase();
				}
			});
		}
			
	}
	
	
	private void upgradeToHdAccount(final ZWCoin coin) {
		
		ZiftrTaskDialogFragment newAddressTaskFragment = new ZiftrTaskDialogFragment() {
			@Override
			protected boolean doTask() {
				long start = System.currentTimeMillis();
				try {	
					//if the user doesn't have an HD account, we can't create an address for them, so we need to make one
					String cachedPassword = ZWPreferences.getCachedPassword();
					if(ZWPreferences.userHasPassword() && cachedPassword == null){
						//if they have a password and it's not cached, we need them to enter it, so 
						//show a dialog for that and stop the rest
						showEnterPasswordDialog();
						return true;
					} 
					else {
						ZWWalletManager.getInstance().activateHd(coin, cachedPassword);
						createNewAddressFromDatabase();
					}
				
				} 
				catch(Exception e) {
					return false;
				}
				
				long end = System.currentTimeMillis();
				long minTime = 500;
				if ((end - start) < minTime) {
					try {
						Thread.sleep(minTime - (end - start)); //quick sleep so the UI doesn't "flash" the loading message
					} 
					catch (InterruptedException e) {
						//do nothing, just quick sleep
					}
				}
				return true;
			}
		};
		
		newAddressTaskFragment.setupDialog(R.string.zw_dialog_new_account);
		
		newAddressTaskFragment.setOnClickListener(new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_NEGATIVE) {
					//if it was successful (BUTTON_POSITIVE), we just dismiss it and show the address
					//otherwise, show the error
					ZiftrDialogManager.showSimpleAlert(getFragmentManager(), R.string.zw_dialog_error_in_account_creation);
				}
			}
		});

		newAddressTaskFragment.show(getFragmentManager(), DIALOT_TASK_CREATE_HD_ACCOUNT);
	}

	
	

	/**
	 * This method gets the database from the activity on the UI thread,
	 * gets a new key from the database helper on an extra thread, and
	 * then updates the UI with the new address on the UI thread. 
	 */
	private void createNewAddressFromDatabase() {

		ZWReceivingAddress address = null;

		try {
			String addressLabel = addressEditText.getText().toString();
			address = getWalletManager().createReceivingAddress(getSelectedCoin(), 0, false, addressLabel);
		} 
		catch (ZWAddressFormatException e) {
			ZLog.log("Should not have happened, wallet corruption?");
		}
		
		if(address == null) {
			//TODO -add this message to strings resource file instead of hardcoding
			ZiftrDialogManager.showSimpleAlert("Could not create a new receiving address!");
		}
		else {
			String addressString = address.getAddress();
			updateAddressUI(addressString);
		}

	}
	
	
	private void updateAddressUI(final String newAddress) {
		ZWMainFragmentActivity activity = ZWReceiveCoinsFragment.this.getZWMainActivity();
		if(activity != null) {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					addressEditText.setText(newAddress);
					updateAddressLabelInDatabase();
					generateQrCode(true);
				}
			});
		}
	}
	
	
	

	@Override
	public void updateAddress(String address, String label) {
		super.updateAddress(address, label);
		this.qrCodeGenerated = false;
	}

	@Override
	public int getActionBarTitleResId() {
		return R.string.zw_actionbar_receive; 
	}

	@Override
	public View getContainerView() {
		return this.scrollView;
	}

	public void setReceiveAddress(ZWReceivingAddress address){
		this.prefilledAddress = address;
	}

	public void refreshAddNewAddressButtonsEnabled() {
		if (!labelEditText.getText().toString().isEmpty() || fragmentHasAddress()) {
			qrCodeContainer.setAlpha(1);
			addressEditText.setAlpha(1);
			messageEditText.setAlpha(1);
			fiatAmountEditText.setAlpha(1);
			coinAmountEditText.setAlpha(1);
			qrCodeImageView.setClickable(true);
			generateAddressForLabel.setClickable(true);
			messageEditText.setFocusable(true);
			fiatAmountEditText.setFocusable(true);
			coinAmountEditText.setFocusable(true);
			messageEditText.setFocusableInTouchMode(true);
			fiatAmountEditText.setFocusableInTouchMode(true);
			coinAmountEditText.setFocusableInTouchMode(true);
		} else {
			messageEditText.setAlpha((float) 0.5);
			fiatAmountEditText.setAlpha((float) 0.5);
			coinAmountEditText.setAlpha((float) 0.5);
			qrCodeContainer.setAlpha((float) 0.5);
			addressEditText.setAlpha((float) 0.5);
			qrCodeImageView.setClickable(false);
			generateAddressForLabel.setClickable(false);
			messageEditText.setFocusable(false);
			fiatAmountEditText.setFocusable(false);
			coinAmountEditText.setFocusable(false);
			messageEditText.setFocusableInTouchMode(false);
			fiatAmountEditText.setFocusableInTouchMode(false);
			coinAmountEditText.setFocusableInTouchMode(false);
		}
	}

	public View getWalletHeaderView(){
		return this.rootView.findViewById(R.id.walletHeader);
	}

}
