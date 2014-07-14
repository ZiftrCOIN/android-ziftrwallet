package com.ziftr.android.onewallet.fragment;

import java.math.BigDecimal;
import java.math.MathContext;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.zxing.client.android.CaptureActivity;
import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.util.Fiat;
import com.ziftr.android.onewallet.util.OWRequestCodes;
import com.ziftr.android.onewallet.util.OWUtils;
import com.ziftr.android.onewallet.util.ZLog;

public abstract class OWSendCoinsFragment extends Fragment {

	private boolean changeCoinStartedFromProgram = false;
	private boolean changeFiatStartedFromProgram = false;

	/** The view container for this fragment. */
	protected View rootView;

	/**
	 * This method can be used to get the default fee
	 * for the type of coin that this fragment will be sending.
	 */
	public abstract CharSequence getDefaultFee();

	/**
	 * Get the market exchange prefix for the 
	 * actual sub-classing coin fragment type.
	 * 
	 * @return "BTC" for Bitcoin, "LTC" for Litecoin, etc.
	 */
	public abstract String getCoinPrefix();

	/** 
	 * Useful method to convert this method to any type of fiat currency.
	 * May want to abstract this to somewhere else eventually.
	 * 
	 * @param fiatType - The fiat type to convert to
	 * @return a big decimal that desribes how many of the given fiat type
	 * of currency is equal to 1 coin of the actual subclassing type.
	 */
	public abstract BigDecimal getExchangeRateToFiat(Fiat.Type fiatType);

	/**
	 * This is an okay place to get this information for now, but will likely 
	 * want to get this info somewhere else eventually. This gets the number of 
	 * decimal places of precision, just in case coins have different precision
	 * amounts. For exmaple, for Bitcoin this returns 8 because 1/10^8 is the 
	 * smallest unit of bitcoin.
	 * 
	 * @return as above
	 */
	public abstract int getNumberOfDigitsOfPrecision(); 

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
	 * Sets up the cancel and the send buttons with their appropriate actions.
	 * The cancel buttons quits the send coins fragment and goes back to wherever
	 * the user was before. The send button validates the values in the form and
	 * initizializes the sending of the coins if everything is correct.
	 */
	private void initializeButtons() {
		Button cancelButton = (Button) 
				this.rootView.findViewById(R.id.cancelSendCoinsButton);
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Activity a = getActivity();
				if (a != null) {
					a.onBackPressed();
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
		feeTextView.setText(this.getDefaultFee());
		changeFiatStartedFromProgram = false;

		changeCoinStartedFromProgram = true;
		amountTextView.setText("0");
		changeCoinStartedFromProgram = false;
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
				// for Regular bar code, its ÒPRODUCT_MODEÓ instead of ÒQR_CODE_MODEÓ
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
					//					BigDecimal oldFiatVal = null;
					BigDecimal newFiatVal = null;
					try {
						newCoinVal = new BigDecimal(newVal);
						//						oldFiatVal = new BigDecimal(
						//								"0" + fiatValEditText.getText().toString());
						newFiatVal = convertCoinToFiat(newCoinVal);
					} catch(NumberFormatException nfe) {
						// If a value can't be parsed then we just do nothing
						// and leave the other box with what was already in it.
						return;
					} catch (ArithmeticException ae) {
						// Shouldn't happen
						ZLog.log("Shouldn't have happened, but it did!");
						return;
					}

					//					if (!newFiatVal.equals(oldFiatVal) || 
					//							newFiatVal.doubleValue() == 0.0) {
					changeFiatStartedFromProgram = true;
					fiatValEditText.setText(formatFiatAmount(newFiatVal).toPlainString());
					changeFiatStartedFromProgram = false;
					//					}
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
					//					BigDecimal oldCoinVal = null;
					BigDecimal newCoinVal = null;
					try {
						newFiatVal = new BigDecimal(newVal);
						//						oldCoinVal = new BigDecimal(
						//								"0" + coinValEditText.getText().toString());
						newCoinVal = newFiatVal.divide(
								getExchangeRateToFiat(Fiat.Type.USD), 
								MathContext.DECIMAL64);
					} catch(NumberFormatException nfe) {
						// If a value can't be parsed then we just do nothing
						// and leave the other box with what was already in it.
						return;
					} catch (ArithmeticException ae) {
						// Shouldn't happen
						ZLog.log("Shouldn't have happened, but it did!");
						return;
					}

					//					if (!newCoinVal.equals(oldCoinVal) || 
					//							newCoinVal.doubleValue() == 0.0) {
					changeCoinStartedFromProgram = true;
					coinValEditText.setText(formatCoinAmount(newCoinVal).toPlainString());
					changeCoinStartedFromProgram = false;
					//					}
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
		totalTextView.setText(this.formatCoinAmount(total).toPlainString());

		// Update the text in the total fiat equiv
		TextView totalEquivTextView = (TextView) this.rootView.findViewById(
				R.id.send_total_fiat_equiv);
		totalEquivTextView.setText("(" + this.formatFiatAmount(this.convertCoinToFiat(
				total)).toPlainString() + ")");
	}

	/**
	 * Gets the net amount that the user will be sending as a BigDecimal.
	 * Units are the units of the coin (decimal, not atomic units).
	 * 
	 * @return as above
	 */
	private BigDecimal getTotalAmountToSend() {
		EditText basicAmountTextBox = getCoinAmountEditText();
		EditText feeTextBox = getCoinFeeEditText();

		BigDecimal basicAmount = null;
		BigDecimal fee = null;
		try {
			basicAmount = new BigDecimal(basicAmountTextBox.getText().toString());
			fee = new BigDecimal(feeTextBox.getText().toString());
		} catch (NumberFormatException nfe) {
			// If any of the textboxes contain values that can't be parsed
			// then just return 0
			return new BigDecimal(0);
		}
		return basicAmount.add(fee);
	}

	/**
	 * A convenience method to format a BigDecimal. In Standard use,
	 * one can just use standard BigDecimal methods and use this
	 * right before formatting for displaying a coin value.
	 * 
	 * @param toFormat - The BigDecimal to format.
	 * @return as above
	 */
	private BigDecimal formatCoinAmount(BigDecimal toFormat) {
		return OWUtils.formatToNDecimalPlaces(
				this.getNumberOfDigitsOfPrecision(), toFormat);
	}

	/**
	 * A convenience method to format a BigDecimal. In Standard use,
	 * one can just use standard BigDecimal methods and use this
	 * right before formatting for displaying a fiat value. 
	 * 
	 * @param toFormat - The BigDecimal to format.
	 * @return as above
	 */
	private BigDecimal formatFiatAmount(BigDecimal toFormat) {
		return OWUtils.formatToNDecimalPlaces(2, toFormat);
	}

	/**
	 * Gives the Fiat equivalent of the BigDecimal given, represented
	 * as another BigDecimal. 
	 * 
	 * TODO make this work for fiats other than just USD
	 * 
	 * @param coinVal - The amount to convert.
	 * @return as above
	 */
	private BigDecimal convertCoinToFiat(BigDecimal coinVal) {
		// 16 digit precision is more than enough
		return coinVal.multiply(getExchangeRateToFiat(Fiat.Type.USD), 
				MathContext.DECIMAL64);
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