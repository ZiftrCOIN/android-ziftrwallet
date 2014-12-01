package com.ziftr.android.ziftrwallet.fragment;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.client.android.CaptureActivity;
import com.ziftr.android.ziftrwallet.ZWPreferencesUtils;
import com.ziftr.android.ziftrwallet.ZWWalletManager;
import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.crypto.ZWAddress;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWCoinFiatTextWatcher;
import com.ziftr.android.ziftrwallet.crypto.ZWCoinURI;
import com.ziftr.android.ziftrwallet.crypto.ZWCoinURIParseException;
import com.ziftr.android.ziftrwallet.crypto.ZWConverter;
import com.ziftr.android.ziftrwallet.crypto.ZWFiat;
import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.exceptions.ZWInsufficientMoneyException;
import com.ziftr.android.ziftrwallet.exceptions.ZWSendAmountException;
import com.ziftr.android.ziftrwallet.network.ZWDataSyncHelper;
import com.ziftr.android.ziftrwallet.util.ZiftrTextWatcher;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

/**
 * The section of the app where users fill out a form and click send 
 * to send coins. 
 * 
 * TODO make sure that we stop correctly when the user
 * enters non-sensical values in fields.
 */
public class ZWSendCoinsFragment extends ZWAddressBookParentFragment {

	/** The view container for this fragment. */
	private View rootView;

	private EditText fiatFeeEditText;
	private EditText coinFeeEditText;

	private TextView fiatAmountLabel;
	private TextView fiatFeeLabel;

	private View getQRCodeButton;
	private View pasteButton;
	private View helpFeeButton;

	private ImageView cancelButton;
	private ImageView sendButton;

	private View sendCoinsContainingView;

	private String prefilledAddress;
	private String prefilledAmount;

	/**
	 * Inflate, initialize, and return the send coins layout.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {


		this.rootView = inflater.inflate(R.layout.accounts_send_coins, container, false);

		this.populateWalletHeader(rootView.findViewById(R.id.walletHeader));

		this.initializeViewFields();

		this.initializeEditText();
		return this.rootView;
	}

	public void onViewStateRestored(Bundle savedInstanceState) {
		// Whenever one of the amount views changes, all the other views should
		// change to constant show an updated version of what the user will send.
		super.onViewStateRestored(savedInstanceState);
		this.initializeTextViewDependencies();
		this.labelEditText.requestFocus();
	}

	/** 
	 * When a barcode is scanned we change the text of the 
	 * receiving address to be the contents of the scan.
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (data != null && data.hasExtra("SCAN_RESULT") && requestCode == ZWRequestCodes.SCAN_QR_CODE) {
			// If we have scanned this address before then we get the label for it here
			String dataFromQRScan = data.getExtras().getCharSequence("SCAN_RESULT").toString();
			String address = null;
			String txNote = null; // (Not the address label)
			String amount = null;
			try {
				ZWCoinURI coinUri = new ZWCoinURI(this.getSelectedCoin(), dataFromQRScan);
				address = coinUri.getAddress();
				txNote = coinUri.getLabel();
				amount = coinUri.getAmount();

				if (txNote == null) {
					txNote = coinUri.getMessage();
				} else if (txNote != null && coinUri.getMessage() != null) {
					txNote += " - " + coinUri.getMessage();
				}

			} catch (ZWCoinURIParseException e) {
				// Maybe it's just a straight non-encoded address? 
				if (this.getSelectedCoin().addressIsValid(dataFromQRScan)) {
					address = dataFromQRScan;
				} else {
					this.getOWMainActivity().alertUser("There was an error in the scanned data.", "error_in_request");
				}
			} catch (ZWAddressFormatException e) {
				this.getOWMainActivity().alertUser("The scanned address is not valid.", "invalid_scanned_address_dialog");
			}

			if (address != null) {
				ZWAddress owAddress = this.getWalletManager().readAddress(this.getSelectedCoin(), address, false);
				if (owAddress != null) {
					// If there wasn't a note given in URI then we pre-fill from address
					txNote = txNote == null ? owAddress.getLabel() : txNote;
				}
			}

			if (amount != null) {
				try {
					// Just to check that it's formatted correctly
					new BigDecimal(amount);
					this.coinAmountEditText.setText(amount);
				} catch(NumberFormatException nfe) {
					// Just don't set the amount if not formatted correctly
				}
			}
			this.acceptAddress(address, txNote);

		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onClick(View v) {
		if (v == getQRCodeButton) {
			Intent intent = new Intent(
					ZWSendCoinsFragment.this.getActivity(), CaptureActivity.class);
			intent.setAction("com.google.zxing.client.android.SCAN");
			// for Regular bar code, its ÒPRODUCT_MODEÓ instead of ÒQR_CODE_MODEÓ
			intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
			intent.putExtra("SAVE_HISTORY", false);
			startActivityForResult(intent, ZWRequestCodes.SCAN_QR_CODE);
		} else if (v == pasteButton) {
			// Gets a handle to the clipboard service.
			ClipboardManager clipboard = (ClipboardManager)
					getActivity().getSystemService(Context.CLIPBOARD_SERVICE);

			String textToPaste = null;
			if (clipboard.hasPrimaryClip()) {
				ClipData clip = clipboard.getPrimaryClip();
				// if you need text data only, use:
				if (clip.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
					// The item could cantain URI that points to the text data.
					// In this case the getText() returns null
					CharSequence text = clip.getItemAt(0).getText();
					if (text != null) {
						textToPaste = text.toString();
					}
				}
			}
			if (textToPaste != null && !textToPaste.isEmpty()) {
				addressEditText.setText(textToPaste);
			}
		} else if (v == cancelButton) {
			Activity a = getActivity();
			if (a != null) {
				a.onBackPressed();
			}
		} else if (v == sendButton) {
			if (ZWPreferencesUtils.userHasPassphrase()) {
				Bundle b = new Bundle();
				b.putString(ZWCoin.TYPE_KEY, getSelectedCoin().toString());
				getOWMainActivity().showGetPassphraseDialog(
						ZWRequestCodes.VALIDATE_PASSPHRASE_DIALOG_SEND, b, ZWTags.VALIDATE_PASS_SEND);
			} else {
				this.onClickSendCoins(null);
			}
		} else if (v == this.getAddressBookImageView()) {
			this.openAddressBook(false, R.id.sendCoinBaseFrameLayout);
		} else if (v == this.helpFeeButton) {
			getOWMainActivity().alertUser(
					"Why is there a fee?\n\nThis fee is NOT paid to Ziftr. "
							+ "The fee goes to the miner that secures your transaction as a reward for their service. "
							+ "This is standard for all cryptocurrency applications. Without it, your transaction would "
							+ "likely stay as pending for an inconvenient length of time.", 
					"fee_help_dialog");
		}
	}

	public void onClickSendCoins(String passphrase) {
		// Need to make sure amount to send is less than balance
		BigInteger amountSending = getSelectedCoin().getAtomicUnits(getAmountToSendFromEditText());
		
		BigInteger feeSending = getSelectedCoin().getAtomicUnits(getFeeFromEditText());
		
		String addressToSendTo = this.addressEditText.getText().toString();
		String addressName = labelEditText.getText().toString();
		ZWWalletManager manager = getWalletManager();
		try {
			sendCoins(addressToSendTo, amountSending, feeSending, passphrase);
			
			if (manager.readAddress(getSelectedCoin(), addressToSendTo, false) != null){
				manager.updateAddressLabel(getSelectedCoin(), addressToSendTo, addressName, false);
			} else {
				manager.createSendingAddress(getSelectedCoin(), addressToSendTo, addressName);
			}
		} catch(ZWAddressFormatException afe) {
			this.getOWMainActivity().alertUser(
					"The address is not formatted correctly. Please try again. ", 
					"address_format_error_dialog");
			return;
		} catch(ZWInsufficientMoneyException ime) {
			this.getOWMainActivity().alertUser(
					"The current wallet does not have enough coins to send the amount requested. ", 
					"insufficient_funds_dialog");
			return;
		} catch(ZWSendAmountException e){
			this.getOWMainActivity().alertUser(
					e.getMessage(), 
					"error_sending_amount_dialog");
		} catch(Exception e) {
			// Shouldn't really happen, just helpful for debugging
			ZLog.log("Exception trying to send coin: ", e);
			this.getOWMainActivity().alertUser(
					"There was an error with your request. \n" + (e.getMessage() == null ? "" : e.getMessage()) + 
					"\nAmount: " + amountSending + 
					". \nFee: " + feeSending + 
					". \nAddress: " + addressToSendTo + ".",
					"error_in_send_dialog");
			return;
		}

	}

	/**
	 * Gets all the important views for use in the fragment.
	 */
	private void initializeViewFields() {
		super.initializeViewFields(this.rootView, R.id.sendGetAddressFromHistoryIcon);

		this.sendCoinsContainingView = this.rootView.findViewById(R.id.sendCoinsContainingView);

		//note the strange way of initializing the text views
		//this is because due to xml reuse in the layout files all the edit texts have the same id
		//so they need to be dug out and explicitly have a unique id set
		//otherwise their contents will not be stored automatically during a suspend/resume cycle
		this.coinAmountEditText = (EditText) this.rootView.findViewById(R.id.sendAmountCoinFiatDualView
				).findViewById(R.id.dualTextBoxLinLayout1).findViewWithTag(ZWTags.OW_EDIT_TEXT);
		coinAmountEditText.setId(R.id.ow_send_coin_amount);

		this.fiatAmountEditText = (EditText) this.rootView.findViewById(R.id.sendAmountCoinFiatDualView
				).findViewById(R.id.dualTextBoxLinLayout2).findViewWithTag(ZWTags.OW_EDIT_TEXT);
		fiatAmountEditText.setId(R.id.ow_send_fiat_amount);

		this.fiatFeeEditText = (EditText) this.rootView.findViewById(
				R.id.sendEditTextTransactionFeeFiatEquiv).findViewWithTag(ZWTags.OW_EDIT_TEXT);
		fiatFeeEditText.setId(R.id.ow_send_fiat_fee);

		this.coinFeeEditText = (EditText) this.rootView.findViewById(
				R.id.sendEditTextTransactionFee).findViewWithTag(ZWTags.OW_EDIT_TEXT);
		coinFeeEditText.setId(R.id.ow_send_coin_fee);

		this.labelEditText = (EditText) this.rootView.findViewById(
				R.id.send_edit_text_reciever_name).findViewWithTag(ZWTags.OW_EDIT_TEXT);
		labelEditText.setId(R.id.ow_send_name);

		this.addressEditText = (EditText) this.rootView.findViewById(
				R.id.sendEditTextReceiverAddress);

		this.getQRCodeButton = this.rootView.findViewById(R.id.send_qr_icon);
		this.getQRCodeButton.setOnClickListener(this);

		this.helpFeeButton = (ImageView) this.rootView.findViewById(R.id.help_fee_button);
		helpFeeButton.setOnClickListener(this);

		this.pasteButton = this.rootView.findViewById(R.id.send_paste_icon);
		this.pasteButton.setOnClickListener(this);

		this.cancelButton = (ImageView) this.rootView.findViewById(R.id.cancel_button);
		this.cancelButton.setOnClickListener(this);

		this.sendButton = (ImageView) this.rootView.findViewById(R.id.send_button);
		this.sendButton.setOnClickListener(this);


		if (this.prefilledAddress != null) {
			addressEditText.setText(this.prefilledAddress);
		}
		if (this.prefilledAmount != null) {
			coinAmountEditText.setText(this.prefilledAmount);
			fiatAmountEditText.setText(ZWConverter.convert(new BigDecimal(this.prefilledAmount), this.getSelectedCoin(), 
					ZWPreferencesUtils.getFiatCurrency()).toPlainString());
		}
		ZWFiat selectedFiat = ZWPreferencesUtils.getFiatCurrency();

		this.fiatAmountLabel = (TextView) this.rootView.findViewById(R.id.amount_fiat_label);
		this.fiatFeeLabel = (TextView) this.rootView.findViewById(R.id.fee_fiat_label);
		this.fiatAmountLabel.setText(selectedFiat.getName());
		this.fiatFeeLabel.setText(selectedFiat.getName());

		if (ZWPreferencesUtils.getFeesAreEditable()) {
			this.coinFeeEditText.setFocusable(true);
			this.coinFeeEditText.setFocusableInTouchMode(true);
			this.coinFeeEditText.setClickable(true);
			this.fiatFeeEditText.setFocusable(true);
			this.fiatFeeEditText.setFocusableInTouchMode(true);
			this.fiatFeeEditText.setClickable(true);
		} else {
			this.coinFeeEditText.clearFocus();
			this.fiatFeeEditText.clearFocus();

			this.coinFeeEditText.setFocusable(false);
			this.coinFeeEditText.setFocusableInTouchMode(false);
			this.coinFeeEditText.setClickable(false);
			this.fiatFeeEditText.setFocusable(false);
			this.fiatFeeEditText.setFocusableInTouchMode(false);
			this.fiatFeeEditText.setClickable(false);
		}
	}


	/**
	 * The form needs to add the totals and update the total text view and
	 * ensure that the coin and fiat equivalents are always in sync. This method
	 * sets up all the text views to do that. 
	 */
	private void initializeTextViewDependencies() {
		// Bind the amount values so that when one changes the other changes
		// according to the current exchange rate.
		ZWCoinFiatTextWatcher coinTextWatcher = new ZWCoinFiatTextWatcher(getSelectedCoin(), this.coinAmountEditText, this.fiatAmountEditText);
		coinAmountEditText.addTextChangedListener(coinTextWatcher);
		fiatAmountEditText.addTextChangedListener(coinTextWatcher);
		
		//set the hint text to zeros
		fiatAmountEditText.setHint(ZWPreferencesUtils.getFiatCurrency().getFormattedAmount(BigDecimal.ZERO));
		coinAmountEditText.setHint(getSelectedCoin().getFormattedAmount(BigDecimal.ZERO));

		// Bind the fee values so that when one changes the other changes
		// according to the current exchange rate.
		EditText feeEditText = this.coinFeeEditText;
		
		ZWCoinFiatTextWatcher feeTextWatcher = new ZWCoinFiatTextWatcher(getSelectedCoin(), feeEditText, this.fiatFeeEditText);
		feeEditText.addTextChangedListener(feeTextWatcher);

		TextWatcher refreshTotalTextWatcher = new ZiftrTextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				refreshTotal();
			}
		};

		// Add the listener to the amount text view
		coinAmountEditText.addTextChangedListener(refreshTotalTextWatcher);

		// Add the listener to the fee text view
		feeEditText.addTextChangedListener(refreshTotalTextWatcher);

		feeEditText.setText(getSelectedCoin().getFormattedAmount(getSelectedCoin().getDefaultFeePerKb()));

	}

	private void initializeEditText() {
		// Why wasn't all of this stuff done in XML? Because the most edittexts are using the same style so we are
		// including the edittext layout so this has to be done programmatically in order to modify these  specific editTexts 
		// and not the default styled edittext included all over the app
		this.coinAmountEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
		this.fiatAmountEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
		this.coinFeeEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
		this.fiatFeeEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
	}

	/**
	 * Updates the two text view that contain to the total and the 
	 * fiat equivalent of the total to match what the user has currently entered.
	 * Sets the values in the text boxes to be zero if the value that the user
	 * has entered is non-sensical (doesn't parse correctly).
	 */
	private void refreshTotal() {
		// Get the total amount of coin to send
		BigDecimal total = this.getTotalAmountToSend();

		// Update the text in the total text view
		TextView totalTextView = (TextView) this.rootView.findViewById(
				R.id.sendTotalTextView);
		totalTextView.setText(getSelectedCoin().getFormattedAmount(total));

		// Update the text in the total fiat equiv
		TextView totalEquivTextView = (TextView) this.rootView.findViewById(
				R.id.sendTotalFiatEquivTextView);
		ZWFiat selectedFiat = ZWPreferencesUtils.getFiatCurrency();
		BigDecimal fiatTotal = ZWConverter.convert(total, getSelectedCoin(), selectedFiat);
		totalEquivTextView.setText("(" + selectedFiat.getFormattedAmount(fiatTotal, true) + ")");
	}

	/**
	 * Gets the net amount that the user will be sending as a BigDecimal.
	 * Units are the units of the coin (decimal, not atomic units).
	 * 
	 * @return as above
	 */
	private BigDecimal getTotalAmountToSend() {
		return this.getAmountToSendFromEditText().add(this.getFeeFromEditText());
	}

	/**
	 * Gets the amount that is in the amount text box. The user can change 
	 * this either through changing the fiat equivalent textbox or by changing
	 * the 
	 * 
	 * @return A {@link BigDecimal} with the amount the user would like to send.
	 */
	private BigDecimal getAmountToSendFromEditText() {
		BigDecimal basicAmount = null;
		try {
			String amt = coinAmountEditText.getText().toString();
			basicAmount = new BigDecimal(amt);
		} catch (NumberFormatException nfe) {
			// If any of the textboxes contain values that can't be parsed
			// then just return 0
			return new BigDecimal(0);
		}
		return basicAmount;
	}

	/**
	 * Gets the amount that is in the fee text box. This will be set in the 
	 * text box to the default of this coin type if the user has not 
	 * purposefully changed it.
	 * 
	 * @return A {@link BigDecimal} with the fee the user would like to send.
	 */
	private BigDecimal getFeeFromEditText() {
		BigDecimal fee = null;
		try {
			String amt = coinFeeEditText.getText().toString();
			fee = new BigDecimal(amt);
		} catch (NumberFormatException nfe) {
			// If any of the textboxes contain values that can't be parsed
			// then just return 0
			return new BigDecimal(0);
		}
		return fee;
	}

	@Override
	public String getActionBarTitle() {
		return "SEND";
	}

	@Override
	public View getContainerView() {
		return this.sendCoinsContainingView;
	}

	public void setSendToAddress(String address) {
		this.prefilledAddress = address;
	}
	
	public void setAmount(String amount){
		this.prefilledAmount = amount;
	}
	
	/**
	 * Sends the type of coin that this thread actually represents to 
	 * the specified address. 
	 * 
	 * @param address - The address to send coins to.
	 * @param value - The number of atomic units (satoshis for BTC) to send
	 * @throws AddressFormatException
	 * @throws InsufficientMoneyException
	 */
	public void sendCoins(final String address, final BigInteger value, 
			final BigInteger feePerKb, final String passphrase) 
			throws ZWAddressFormatException, Exception {
		
		final ZWCoin coinId = getSelectedCoin();
		
		if (!coinId.addressIsValid(address)){
			throw new ZWAddressFormatException();
		} else if (value.signum() != 1){
			//user wants to send <=0 coins
			throw new ZWSendAmountException("Error: Cannot send 0 coins!");
		} else if (feePerKb.compareTo(coinId.getDefaultFeePerKb()) == 0 && value.compareTo(feePerKb) < 1){
			throw new ZWSendAmountException("Error: The desired amount to send is too small!");
		}
		
		//inputs = the user's receiving addresses, including hidden change addresses that he will spend from
		final List<String> inputs = new ArrayList<String>();
		
		//send all user's addresses to the server
		for (ZWAddress addr : ZWWalletManager.getInstance().readAddresses(coinId, null, true)){
			inputs.add(addr.toString());
		}
		
		
		ZiftrUtils.runOnNewThread(new Runnable() {
			@Override
			public void run() {
				
				final String message = ZWDataSyncHelper.sendCoins(coinId, feePerKb, value, inputs, address, passphrase);
				getOWMainActivity().runOnUiThread(new Runnable(){

					@Override
					public void run() {
						if (!message.isEmpty()){
							getOWMainActivity().alertUser(message, "error_sending_coins");
						} else {
							getOWMainActivity().onBackPressed();
							Toast.makeText(getOWMainActivity(), coinId.toString() + " sent!", Toast.LENGTH_LONG).show();
						}
					}
				
					});

			}
		});
	}

}
