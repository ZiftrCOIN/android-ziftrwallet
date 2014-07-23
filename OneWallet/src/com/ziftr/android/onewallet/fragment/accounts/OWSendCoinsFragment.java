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
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.InsufficientMoneyException;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.utils.Threading;
import com.google.zxing.client.android.CaptureActivity;
import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWConverter;
import com.ziftr.android.onewallet.util.OWFiat;
import com.ziftr.android.onewallet.util.OWRequestCodes;
import com.ziftr.android.onewallet.util.ZLog;

/**
 * The section of the app where users fill out a form and click send 
 * to send coins. 
 * 
 * TODO make sure that we stop correctly when the user
 * enters non-sensical values in fields.
 */
public abstract class OWSendCoinsFragment extends OWWalletUserFragment {

	/** A boolean to keep track of changes to disallow infinite changes. */
	private boolean changeCoinStartedFromProgram = false;

	/** A boolean to keep track of changes to disallow infinite changes. */
	private boolean changeFiatStartedFromProgram = false;

	/** The view container for this fragment. */
	private View rootView;

	/**
	 * This is a placeholder for a spot to determine if a given address
	 * is valid.
	 * 
	 * @param address - The address to verify the validity of.
	 * @return as above
	 */
	public abstract boolean addressIsValid(String address);

	/**
	 * Inflate, initialize, and return the send coins layout.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {

		this.rootView = inflater.inflate(
				R.layout.accounts_send_coins, container, false);

		// Sets the onclicks for qr code icon, copy/paste icon, any help icons.
		this.initializeIcons();

		// Whenever one of the amount views changes, all the other views should
		// change to constant show an updated version of what the user will send.
		this.initializeTextViewDependencies();

		// Set up the cancel and send buttons
		this.initializeButtons();

		return this.rootView;
	}

	/** 
	 * When a barcode is scanned we change the text of the 
	 * receiving address to be the contents of the scan.
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (data != null && data.hasExtra("SCAN_RESULT") && 
				requestCode == OWRequestCodes.SCAN_QR_CODE) {
			EditText addressToSendCoinsToEditText = this.getSendToAddressEditText();
			addressToSendCoinsToEditText.setText(
					data.getExtras().getCharSequence("SCAN_RESULT"));
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Sets up all the useful icons in the form. Currently sets up the paste
	 * icon and the 'get qr code' icon. 
	 */
	private void initializeIcons() {
		// For the 'get QR code' button
		View getQRCodeButton = this.rootView.findViewById(R.id.send_qr_icon);
		// Set the listener for the clicks
		getQRCodeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(
						OWSendCoinsFragment.this.getActivity(), CaptureActivity.class);
				intent.setAction("com.google.zxing.client.android.SCAN");
				// for Regular bar code, its �PRODUCT_MODE� instead of �QR_CODE_MODE�
				intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
				intent.putExtra("SAVE_HISTORY", false);
				startActivityForResult(intent, OWRequestCodes.SCAN_QR_CODE);
			}
		});

		// For the 'paste' button
		View pasteButton = this.rootView.findViewById(R.id.send_paste_icon);
		// Set the listener for the clicks
		pasteButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Gets a handle to the clipboard service.
				ClipboardManager clipboard = (ClipboardManager)
						getActivity().getSystemService(Context.CLIPBOARD_SERVICE);

				String textToPaste = null;
				if (clipboard.hasPrimaryClip()) {
					ClipData clip = clipboard.getPrimaryClip();

					// if you need text data only, use:
					if (clip.getDescription().hasMimeType(
							ClipDescription.MIMETYPE_TEXT_PLAIN)) {
						// The item could cantain URI that points to the text data.
						// In this case the getText() returns null
						CharSequence text = clip.getItemAt(0).getText();
						if (text != null) {
							textToPaste = text.toString();
						}
					}
				}

				if (textToPaste != null && !textToPaste.isEmpty()) {
					getSendToAddressEditText().setText(textToPaste);
				} else {
					ZLog.log("Text to paste: '", textToPaste, "'");
				}

			}
		});
	}

	/**
	 * The form needs to add the totals and update the total text view and
	 * ensure that the coin and fiat equivalents are always in sync. This method
	 * sets up all the text views to do that. 
	 */
	private void initializeTextViewDependencies() {
		// Bind the amount values so that when one changes the other changes
		// according to the current exchange rate.
		EditText amountTextView = this.getCoinAmountEditText();
		this.bindEditTextValues(amountTextView, this.getFiatAmountEditText());

		// Bind the fee values so that when one changes the other changes
		// according to the current exchange rate.
		EditText feeTextView = this.getCoinFeeEditText();
		this.bindEditTextValues(feeTextView, this.getFiatFeeEditText());

		TextWatcher refreshTotalTextWatcher = new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence text, int start, 
					int before, int count) {
				refreshTotal();
			}

			@Override
			public void beforeTextChanged(CharSequence text, int start, 
					int count, int after) {
				// Nothing to do 
			}

			@Override
			public void afterTextChanged(Editable text) {
				// Nothing to do 
			}
		};

		// Add the listener to the amount text view
		amountTextView.addTextChangedListener(refreshTotalTextWatcher);

		// Add the listener to the fee text view
		feeTextView.addTextChangedListener(refreshTotalTextWatcher);

		// Set the fee to the default and set send amount to 0 initially 
		changeFiatStartedFromProgram = true;
		feeTextView.setText(this.getCoinId().getDefaultFeePerKb());
		changeFiatStartedFromProgram = false;

		changeCoinStartedFromProgram = true;
		amountTextView.setText("0");
		changeCoinStartedFromProgram = false;
	}

	/**
	 * Sets up the cancel and the send buttons with their appropriate actions.
	 * The cancel buttons quits the send coins fragment and goes back to wherever
	 * the user was before. The send button validates the values in the form and
	 * initizializes the sending of the coins if everything is correct.
	 */
	private void initializeButtons() {
		Button cancelButton = (Button) 
				this.rootView.findViewById(R.id.leftButton);
		cancelButton.setText("CANCEL");
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Activity a = getActivity();
				if (a != null) {
					a.onBackPressed();
				}
			}
		});

		Button sendButton = (Button) 
				this.rootView.findViewById(R.id.rightButton);
		sendButton.setText("SEND");
		sendButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (addressIsValid(getSendToAddressEditText().getText().toString())) {
					// Need to make sure amount to send is less than balance
					BigInteger amountSending = getAmountToSendFromEditText().multiply(
							(new BigDecimal("10").pow(getCoinId(
									).getNumberOfDigitsOfPrecision()))).toBigInteger();
					BigInteger feeSending = getFeeFromEditText().multiply(
							(new BigDecimal("10").pow(getCoinId(
									).getNumberOfDigitsOfPrecision()))).toBigInteger();
					try {
						sendCoins(getSendToAddressEditText().getText().toString(), 
								amountSending, feeSending);

						// To exit out of the send coins fragment
						getActivity().onBackPressed();
					} catch(AddressFormatException afe) {
						// TODO alert user if address has errors
						ZLog.log("There was a problem with the address.");
					} catch(InsufficientMoneyException ime) {
						// TODO alert user if not enough money in wallet.
						ZLog.log("There was not enough money in the wallet.");
					} 
				}
			}
		});
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
	private void sendCoins(String address, BigInteger value, BigInteger feePerKb) 
			throws AddressFormatException, InsufficientMoneyException {

		Wallet wallet = this.getWallet();

		// Create an address object based on network parameters in use 
		// and the entered address. This is the address we will send coins to.
		Address sendAddress = new Address(wallet.getNetworkParameters(), address);

		// Use the address and the value to create a new send request object
		Wallet.SendRequest sendRequest = Wallet.SendRequest.to(sendAddress, value);


		// If the wallet is encrypted then we have to load the AES key so that the
		// wallet can get at the private keys and sign the data.
		if (wallet.isEncrypted()) {
			sendRequest.aesKey = wallet.getKeyCrypter().deriveKey("password");
		}

		// Make sure we aren't adding any additional fees
		sendRequest.ensureMinRequiredFee = false;

		// Set a fee for the transaction, always the minimum for now.
		// sendRequest.fee = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;
		sendRequest.fee = BigInteger.ZERO;

		// TODO make the rest of the code have variable names that reflect that this
		// is actually a fee per kilobyte.
		sendRequest.feePerKb = feePerKb;

		// I don't think we want to do this. 
		//wallet.decrypt(null);

		Wallet.SendResult sendResult = wallet.sendCoins(sendRequest);
		sendResult.broadcastComplete.addListener(new Runnable() {
			@Override
			public void run() {
				// TODO if we want something to be done here that relates
				// to the gui thread, how do we do that?
				ZLog.log("Successfully sent coins to address!");
			}
		}, Threading.SAME_THREAD); // changed from MoreExecutors.sameThreadExecutor()

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
		TextWatcher updateFiat = new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence text, int start, 
					int before, int count) {
				if (!changeCoinStartedFromProgram) {
					String newVal = text.toString();
					BigDecimal newCoinVal = null;
					BigDecimal newFiatVal = null;
					try {
						newCoinVal = new BigDecimal(newVal);
						// TODO make this not only be USD
						newFiatVal = OWConverter.convert(
								newCoinVal, getCoinId(), OWFiat.Type.USD); 
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
							OWFiat.Type.USD, newFiatVal).toPlainString());
					changeFiatStartedFromProgram = false;
				}
			}

			@Override
			public void beforeTextChanged(CharSequence text, int start, 
					int count, int after) {
				// Nothing to do 
			}

			@Override
			public void afterTextChanged(Editable text) {
				// Nothing to do 
			}
		};
		coinValEditText.addTextChangedListener(updateFiat);

		TextWatcher updateCoin = new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence text, int start, 
					int before, int count) {
				if (!changeFiatStartedFromProgram) {
					String newVal = text.toString();
					BigDecimal newFiatVal = null;
					BigDecimal newCoinVal = null;
					try {
						newFiatVal = new BigDecimal(newVal);
						newCoinVal =  OWConverter.convert(
								newFiatVal, OWFiat.Type.USD, getCoinId());
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
							getCoinId(), newCoinVal).toPlainString());
					changeCoinStartedFromProgram = false;
				}
			}

			@Override
			public void beforeTextChanged(CharSequence text, int start, 
					int count, int after) {
				// Nothing to do 
			}

			@Override
			public void afterTextChanged(Editable text) {
				// Nothing to do 
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
				R.id.send_total);
		totalTextView.setText(OWCoin.formatCoinAmount(
				getCoinId(), total).toPlainString());

		// Update the text in the total fiat equiv
		TextView totalEquivTextView = (TextView) this.rootView.findViewById(
				R.id.send_total_fiat_equiv);
		BigDecimal fiatTotal = OWConverter.convert(total, OWFiat.Type.USD, getCoinId());
		totalEquivTextView.setText("(" + OWFiat.formatFiatAmount(OWFiat.Type.USD, fiatTotal).toPlainString() + ")");
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
		EditText basicAmountTextBox = getCoinAmountEditText();

		BigDecimal basicAmount = null;
		try {
			basicAmount = new BigDecimal(basicAmountTextBox.getText().toString());
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
		EditText feeTextBox = getCoinFeeEditText();

		BigDecimal fee = null;
		try {
			fee = new BigDecimal(feeTextBox.getText().toString());
		} catch (NumberFormatException nfe) {
			// If any of the textboxes contain values that can't be parsed
			// then just return 0
			return new BigDecimal(0);
		}
		return fee;
	}

	/**
	 * A convenience method to get the edit text which contains the address
	 * that the user is about to send coins to. 
	 * 
	 * @return as above
	 */
	private EditText getSendToAddressEditText() {
		return (EditText) this.rootView.findViewById(
				R.id.send_edit_text_receiver_address);
	}

	/**
	 * @return
	 */
	private EditText getFiatFeeEditText() {
		return (EditText) this.rootView.findViewById(
				R.id.send_edit_text_transaction_fee_fiat_equiv);
	}

	/**
	 * @return
	 */
	private EditText getCoinFeeEditText() {
		return (EditText) this.rootView.findViewById(
				R.id.send_edit_text_transaction_fee);
	}

	/**
	 * @return
	 */
	private EditText getFiatAmountEditText() {
		return (EditText) this.rootView.findViewById(
				R.id.send_edit_text_amount_fiat_equiv);
	}

	/**
	 * @return
	 */
	private EditText getCoinAmountEditText() {
		return (EditText) this.rootView.findViewById(
				R.id.send_edit_text_amount);
	}

}