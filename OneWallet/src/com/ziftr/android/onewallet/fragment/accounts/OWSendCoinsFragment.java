package com.ziftr.android.onewallet.fragment.accounts;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.zxing.client.android.CaptureActivity;
import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.crypto.OWAddress;
import com.ziftr.android.onewallet.exceptions.OWAddressFormatException;
import com.ziftr.android.onewallet.exceptions.OWInsufficientMoneyException;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWConverter;
import com.ziftr.android.onewallet.util.OWFiat;
import com.ziftr.android.onewallet.util.OWRequestCodes;
import com.ziftr.android.onewallet.util.OWTags;
import com.ziftr.android.onewallet.util.OWTextWatcher;
import com.ziftr.android.onewallet.util.ZLog;
import com.ziftr.android.onewallet.util.ZiftrUtils;

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

	private Button cancelButton;
	private Button sendButton;

	private View sendCoinsContainingView;

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
			OWAddress a = this.getWalletManager().readAddress(
					this.getSelectedCoin(), data.getExtras().getCharSequence("SCAN_RESULT").toString(), false);
			String label = a == null ? "" : a.getLabel();
			this.acceptAddress(a.toString(), label);
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
			if (this.getSelectedCoin().addressIsValid(addressEditText.getText().toString())) {
				if (getOWMainActivity().userHasPassphrase()) {
					Bundle b = new Bundle();
					b.putString(OWCoin.TYPE_KEY, getSelectedCoin().toString());
					getOWMainActivity().showGetPassphraseDialog(
							OWRequestCodes.VALIDATE_PASSPHRASE_DIALOG_SEND, b, OWTags.VALIDATE_PASS_SEND);
				} else {
					this.onClickSendCoins();
				}
			}
		} else if (v == this.getAddressBookImageView()) {
			this.openAddressBook(false, R.id.sendCoinBaseFrameLayout);
		}

	}

	public void onClickSendCoins() {
		// Need to make sure amount to send is less than balance
		BigInteger amountSending = ZiftrUtils.bigDecToBigInt(getSelectedCoin(), 
				getAmountToSendFromEditText());
		BigInteger feeSending = ZiftrUtils.bigDecToBigInt(getSelectedCoin(), 
				getFeeFromEditText());
		try {
			getWalletManager().sendCoins(getSelectedCoin(), this.addressEditText.getText().toString(), 
					amountSending, feeSending);
		} catch(OWAddressFormatException afe) {
			// TODO alert user if address has errors
			ZLog.log("There was a problem with the address.");
		} catch(OWInsufficientMoneyException ime) {
			// TODO alert user if not enough money in wallet.
			ZLog.log("There was not enough money in the wallet.");
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

		this.cancelButton = (Button) this.rootView.findViewById(R.id.leftButton);
		cancelButton.setText("CANCEL");
		this.cancelButton.setOnClickListener(this);

		this.sendButton = (Button) this.rootView.findViewById(R.id.rightButton);
		sendButton.setText("SEND");
		this.sendButton.setOnClickListener(this);
	}

	/**
	 * The form needs to add the totals and update the total text view and
	 * ensure that the coin and fiat equivalents are always in sync. This method
	 * sets up all the text views to do that. 
	 */
	private void initializeTextViewDependencies() {
		// Bind the amount values so that when one changes the other changes
		// according to the current exchange rate.
		this.bindEditTextValues(coinAmountEditText, this.fiatAmountEditText);

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
		// TODO why wasn't all of this stuff done in XML?
		this.coinAmountEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
		this.fiatAmountEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
		this.coinFeeEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
		this.fiatFeeEditText.setFocusable(false);
		this.fiatFeeEditText.setClickable(false);
		this.fiatFeeEditText.setCursorVisible(false);
		this.fiatFeeEditText.setFocusableInTouchMode(false);
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
			basicAmount = new BigDecimal(coinAmountEditText.getText().toString());
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
			fee = new BigDecimal(coinFeeEditText.getText().toString());
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

}
