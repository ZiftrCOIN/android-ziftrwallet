package com.ziftr.android.ziftrwallet.fragment.accounts;

import java.math.BigDecimal;
import java.math.BigInteger;

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

import com.google.zxing.client.android.CaptureActivity;
import com.ziftr.android.ziftrwallet.OWPreferencesUtils;
import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.crypto.OWAddress;
import com.ziftr.android.ziftrwallet.exceptions.OWAddressFormatException;
import com.ziftr.android.ziftrwallet.exceptions.OWInsufficientMoneyException;
import com.ziftr.android.ziftrwallet.util.OWCoin;
import com.ziftr.android.ziftrwallet.util.OWCoinURI;
import com.ziftr.android.ziftrwallet.util.OWCoinURIParseException;
import com.ziftr.android.ziftrwallet.util.OWConverter;
import com.ziftr.android.ziftrwallet.util.OWFiat;
import com.ziftr.android.ziftrwallet.util.OWRequestCodes;
import com.ziftr.android.ziftrwallet.util.OWTags;
import com.ziftr.android.ziftrwallet.util.OWTextWatcher;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

/**
 * The section of the app where users fill out a form and click send 
 * to send coins. 
 * 
 * TODO make sure that we stop correctly when the user
 * enters non-sensical values in fields.
 */
public class OWSendCoinsFragment extends OWAddressBookParentFragment {

	/** A boolean to keep track of changes to disallow infinite changes. */
	private boolean changeCoinStartedFromProgram = false;

	/** A boolean to keep track of changes to disallow infinite changes. */
	private boolean changeFiatStartedFromProgram = false;

	/** The view container for this fragment. */
	private View rootView;

	private EditText coinAmountEditText;
	private EditText fiatAmountEditText;

	private EditText fiatFeeEditText;
	private EditText coinFeeEditText;

	private View getQRCodeButton;
	private View pasteButton;

	private ImageView cancelButton;
	private ImageView sendButton;

	private View sendCoinsContainingView;

	private String prefilledAddress;

	/**
	 * Inflate, initialize, and return the send coins layout.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {

		this.rootView = inflater.inflate(R.layout.accounts_send_coins, container, false);

		this.showWalletHeader();

		this.initializeViewFields();

		this.initializeEditText();

		// Whenever one of the amount views changes, all the other views should
		// change to constant show an updated version of what the user will send.
		this.initializeTextViewDependencies();

		return this.rootView;
	}

	/** 
	 * When a barcode is scanned we change the text of the 
	 * receiving address to be the contents of the scan.
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (data != null && data.hasExtra("SCAN_RESULT") && requestCode == OWRequestCodes.SCAN_QR_CODE) {
			// If we have scanned this address before then we get the label for it here
			String dataFromQRScan = data.getExtras().getCharSequence("SCAN_RESULT").toString();
			String address = null;
			String txNote = null; // (Not the address label)
			String amount = null;
			try {
				OWCoinURI coinUri = new OWCoinURI(this.getSelectedCoin(), dataFromQRScan);
				address = coinUri.getAddress();
				txNote = coinUri.getLabel();
				amount = coinUri.getAmount();

				if (txNote == null) {
					txNote = coinUri.getMessage();
				} else if (txNote != null && coinUri.getMessage() != null) {
					txNote += " - " + coinUri.getMessage();
				}

			} catch (OWCoinURIParseException e) {
				// Maybe it's just a straight uri non-encoded address? 
				if (this.getSelectedCoin().addressIsValid(dataFromQRScan)) {
					address = dataFromQRScan;
				}
				ZLog.log("Error: ", e.getMessage());
				e.printStackTrace();
			} catch (OWAddressFormatException e) {
				ZLog.log("Error: ", e.getMessage());
				e.printStackTrace();
			}

			if (address != null) {
				OWAddress owAddress = this.getWalletManager().readAddress(this.getSelectedCoin(), address, false);
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
					OWSendCoinsFragment.this.getActivity(), CaptureActivity.class);
			intent.setAction("com.google.zxing.client.android.SCAN");
			// for Regular bar code, its ÒPRODUCT_MODEÓ instead of ÒQR_CODE_MODEÓ
			intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
			intent.putExtra("SAVE_HISTORY", false);
			startActivityForResult(intent, OWRequestCodes.SCAN_QR_CODE);
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
			if (OWPreferencesUtils.userHasPassphrase(getOWMainActivity())) {
				Bundle b = new Bundle();
				b.putString(OWCoin.TYPE_KEY, getSelectedCoin().toString());
				getOWMainActivity().showGetPassphraseDialog(
						OWRequestCodes.VALIDATE_PASSPHRASE_DIALOG_SEND, b, OWTags.VALIDATE_PASS_SEND);
			} else {
				this.onClickSendCoins();
			}
		} else if (v == this.getAddressBookImageView()) {
			this.openAddressBook(false, R.id.sendCoinBaseFrameLayout);
		}
	}

	public void onClickSendCoins() {
		// Need to make sure amount to send is less than balance
		BigInteger amountSending = ZiftrUtils.bigDecToBigInt(getSelectedCoin(), getAmountToSendFromEditText());
		BigInteger feeSending = ZiftrUtils.bigDecToBigInt(getSelectedCoin(), getFeeFromEditText());
		String addressToSendTo = this.addressEditText.getText().toString();
		try {
			getWalletManager().sendCoins(getSelectedCoin(), addressToSendTo, 
					amountSending, feeSending);
		} catch(OWAddressFormatException afe) {
			this.getOWMainActivity().alertUser(
					"The address is not formatted correctly. Please try again. ", 
					"address_format_error_dialog");
			return;
		} catch(OWInsufficientMoneyException ime) {
			this.getOWMainActivity().alertUser(
					"The current wallet does not have enough coins to send the amount requested. ", 
					"insufficient_funds_dialog");
			return;
		} catch(Exception e) {
			// Shouldn't really happen, just helpful for debugging
			this.getOWMainActivity().alertUser(
					"There was an error with your request. \n" + (e.getMessage() == null ? "" : e.getMessage()) + 
					"\nAmount: " + amountSending + 
					". \nFee: " + feeSending + 
					". \nAddress: " + addressToSendTo + ".",
					"error_in_send_dialog");
			return;
		}

		// To exit out of the send coins fragment
		Activity a = getActivity();
		if (a != null) {
			a.onBackPressed();
		}
	}

	/**
	 * Gets all the important views for use in the fragment.
	 */
	private void initializeViewFields() {
		super.initializeViewFields(this.rootView, R.id.sendGetAddressFromHistoryIcon);

		this.sendCoinsContainingView = this.rootView.findViewById(R.id.sendCoinsContainingView);

		this.coinAmountEditText = (EditText) this.rootView.findViewById(
				R.id.sendEditTextAmount).findViewById(R.id.ow_editText);
		this.fiatAmountEditText = (EditText) this.rootView.findViewById(
				R.id.sendEditTextAmountFiatEquiv).findViewById(R.id.ow_editText);
		this.fiatFeeEditText = (EditText) this.rootView.findViewById(
				R.id.sendEditTextTransactionFeeFiatEquiv).findViewById(R.id.ow_editText);
		this.coinFeeEditText = (EditText) this.rootView.findViewById(
				R.id.sendEditTextTransactionFee).findViewById(R.id.ow_editText);
		this.labelEditText = (EditText) this.rootView.findViewById(
				R.id.send_edit_text_reciever_name).findViewById(R.id.ow_editText);
		this.addressEditText = (EditText) this.rootView.findViewById(
				R.id.sendEditTextReceiverAddress);

		this.getQRCodeButton = this.rootView.findViewById(R.id.send_qr_icon);
		this.getQRCodeButton.setOnClickListener(this);

		this.pasteButton = this.rootView.findViewById(R.id.send_paste_icon);
		this.pasteButton.setOnClickListener(this);

		this.cancelButton = (ImageView) this.rootView.findViewById(R.id.cancel_button);
		this.cancelButton.setOnClickListener(this);

		this.sendButton = (ImageView) this.rootView.findViewById(R.id.send_button);
		this.sendButton.setOnClickListener(this);

		if (this.prefilledAddress != null){
			addressEditText.setText(this.prefilledAddress);
		}

		if (OWPreferencesUtils.getFeesAreEditable(this.getActivity())) {
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
		this.bindEditTextValues(this.coinAmountEditText, this.fiatAmountEditText);

		// Bind the fee values so that when one changes the other changes
		// according to the current exchange rate.
		EditText feeTextView = this.coinFeeEditText;
		this.bindEditTextValues(feeTextView, this.fiatFeeEditText);

		TextWatcher refreshTotalTextWatcher = new OWTextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				refreshTotal();
			}
		};

		// Add the listener to the amount text view
		coinAmountEditText.addTextChangedListener(refreshTotalTextWatcher);

		// Add the listener to the fee text view
		feeTextView.addTextChangedListener(refreshTotalTextWatcher);

		// Set the fee to the default and set send amount to 0 initially 
		changeFiatStartedFromProgram = true;
		feeTextView.setText(this.getSelectedCoin().getDefaultFeePerKb());
		changeFiatStartedFromProgram = false;

		changeCoinStartedFromProgram = true;
		coinAmountEditText.setText("0");
		changeCoinStartedFromProgram = false;
	}

	private void initializeEditText() {
		// Why wasn't all of this stuff done in XML? Because the most edittexts are using the same style so we are
		// including the edittext layout so this has to be done programmatically in order to modify these  specific editTexts 
		// and not the default styled edittext included all over the app
		this.coinAmountEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
		this.fiatAmountEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
		this.coinFeeEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
	}

	/**
	 * This is a useful method that binds two textviews together with
	 * TextWatchers in such a way that when one changes the other also 
	 * changes. One EditText contains coin values and the other should 
	 * contain fiat values.
	 * 
	 * @param coinValEditText - The coin EditText
	 * @param fiatValEditText - The fiat EditText
	 */
	private void bindEditTextValues(final EditText coinValEditText, 
			final EditText fiatValEditText) {
		TextWatcher updateFiat = new OWTextWatcher() {
			@Override
			public void afterTextChanged(Editable text) {
				if (!changeCoinStartedFromProgram) {
					String newVal = text.toString();
					BigDecimal newCoinVal = null;
					BigDecimal newFiatVal = null;
					try {
						newCoinVal = new BigDecimal(newVal);
						// TODO make this not only be USD
						newFiatVal = OWConverter.convert(
								newCoinVal, getSelectedCoin(), OWFiat.USD); 
					} catch(NumberFormatException nfe) {
						// If a value can't be parsed then we just do nothing
						// and leave the other box with what was already in it.
						return;
					} catch (ArithmeticException ae) {
						// Shouldn't happen
						ZLog.log("User entered non-sensical value. Returning for now.");
						return;
					}

					changeFiatStartedFromProgram = true;
					fiatValEditText.setText(OWFiat.formatFiatAmount(
							OWFiat.USD, newFiatVal, false));
					changeFiatStartedFromProgram = false;
				}
			}
		};
		coinValEditText.addTextChangedListener(updateFiat);

		TextWatcher updateCoin = new OWTextWatcher() {
			@Override
			public void afterTextChanged(Editable text) {
				if (!changeFiatStartedFromProgram) {
					String newVal = text.toString();
					BigDecimal newFiatVal = null;
					BigDecimal newCoinVal = null;
					try {
						newFiatVal = new BigDecimal(newVal);
						newCoinVal =  OWConverter.convert(
								newFiatVal, OWFiat.USD, getSelectedCoin());
					} catch(NumberFormatException nfe) {
						// If a value can't be parsed then we just do nothing
						// and leave the other box with what was already in it.
						return;
					} catch (ArithmeticException ae) {
						// Shouldn't happen
						ZLog.log("Shouldn't have happened, but it did!");
						return;
					}

					changeCoinStartedFromProgram = true;
					coinValEditText.setText(OWCoin.formatCoinAmount(
							getSelectedCoin(), newCoinVal).toPlainString());
					changeCoinStartedFromProgram = false;
				}
			}
		};
		fiatValEditText.addTextChangedListener(updateCoin);
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
		totalTextView.setText(OWCoin.formatCoinAmount(
				getSelectedCoin(), total).toPlainString());

		// Update the text in the total fiat equiv
		TextView totalEquivTextView = (TextView) this.rootView.findViewById(
				R.id.sendTotalFiatEquivTextView);
		BigDecimal fiatTotal = OWConverter.convert(total, OWFiat.USD, getSelectedCoin());
		totalEquivTextView.setText("(" + OWFiat.formatFiatAmount(OWFiat.USD, fiatTotal, false) + ")");
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

	public void setSendToAddress(String address){
		this.prefilledAddress = address;
	}

}
