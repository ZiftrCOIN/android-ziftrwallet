package com.ziftr.android.ziftrwallet.fragment;

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
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.client.android.CaptureActivity;
import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWMainFragmentActivity;
import com.ziftr.android.ziftrwallet.ZWPreferences;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWCoinFiatTextWatcher;
import com.ziftr.android.ziftrwallet.crypto.ZWCoinURI;
import com.ziftr.android.ziftrwallet.crypto.ZWCoinURIParseException;
import com.ziftr.android.ziftrwallet.crypto.ZWConverter;
import com.ziftr.android.ziftrwallet.crypto.ZWFiat;
import com.ziftr.android.ziftrwallet.dialog.ZWSendTaskHelperFragment;
import com.ziftr.android.ziftrwallet.dialog.ZiftrDialogManager;
import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrTextWatcher;

/**
 * The section of the app where users fill out a form and click send 
 * to send coins. 
 * 
 * TODO make sure that we stop correctly when the user
 * enters non-sensical values in fields.
 */
public class ZWSendCoinsFragment extends ZWAddressBookParentFragment {

	public static final String FRAGMENT_TAG = "send_coins";
	
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

	private TextView totalTextView;
	private TextView totalFiatEquivTextView;
	private ZWCoinURI preloadedCoinUri;
	private String preloadAddress;
	

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

	}
	
	
	
	/**
	 * Inflate, initialize, and return the send coins layout.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		this.rootView = inflater.inflate(R.layout.accounts_send_coins, container, false);

		this.populateWalletHeader(rootView.findViewById(R.id.walletHeader));
		this.initializeViewFields();
		this.initializeEditText();
		if(preloadedCoinUri != null) {
			this.setupViews(preloadedCoinUri);
			preloadedCoinUri = null;
			preloadAddress = null;
		}
		else if(preloadAddress != null) {
			this.updateAddress(preloadAddress, null);
		}
		
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
		if (data != null && data.hasExtra("SCAN_RESULT")) {
			// If we have scanned this address before then we get the label for it here
			String dataFromQRScan = data.getExtras().getCharSequence("SCAN_RESULT").toString();
			
			ZWCoinURI coinUri = null;
			try {
				coinUri = new ZWCoinURI(this.getSelectedCoin(), dataFromQRScan);

			} 
			catch (ZWCoinURIParseException e) {
				ZLog.log("Excpetion parsing QR code: ", e);
				// Maybe it's just a straight non-encoded address? 
				if (this.getSelectedCoin().addressIsValid(dataFromQRScan)) {
					String address = dataFromQRScan;
					this.updateAddress(address, null);
				}
				else {
					ZiftrDialogManager.showSimpleAlert(getFragmentManager(), R.string.zw_dialog_error_qrcode_data);
				}
			} 
			catch (ZWAddressFormatException e) {
				ZiftrDialogManager.showSimpleAlert(getFragmentManager(), R.string.zw_dialog_error_qrcode_address);
			}

			if(coinUri != null) {
				setupViews(coinUri);
			}

		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	
	private void setupViews(ZWCoinURI coinUri) {
	
		String address = coinUri.getAddress();
		String label = coinUri.getLabel();
		String amount = coinUri.getAmount();

		if (label == null) {
			label = coinUri.getMessage();
		} 
		else if (label != null && coinUri.getMessage() != null) {
			label += " - " + coinUri.getMessage();
		}
	
		if (amount != null) {
			try {
				// Just to check that it's formatted correctly
				new BigDecimal(amount);
				this.coinAmountEditText.setText(amount);
			}
			catch(NumberFormatException nfe) {
				// Just don't set the amount if not formatted correctly
			}
		}
		
		this.updateAddress(address, label);
	}
	
	
	@Override
	public void onClick(View v) {
		if (v == getQRCodeButton) {
			Intent intent = new Intent(ZWSendCoinsFragment.this.getActivity(), CaptureActivity.class);
			intent.setAction("com.google.zxing.client.android.SCAN");
			// for Regular bar code, its ÒPRODUCT_MODEÓ instead of ÒQR_CODE_MODEÓ
			intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
			intent.putExtra("SAVE_HISTORY", false);
			startActivityForResult(intent, 0);
		} 
		else if (v == pasteButton) {
			// Gets a handle to the clipboard service.
			ClipboardManager clipboard = (ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);

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
		} 
		else if (v == cancelButton) {
			Activity a = getActivity();
			if (a != null) {
				a.onBackPressed();
			}
		}
		else if (v == sendButton) {
			this.doSendCoins();
		} 
		else if (v == this.getAddressBookImageView()) {
			this.openAddressBook(new ZWSendAddressBookFragment(), R.id.sendCoinBaseFrameLayout);
		} 
		else if (v == this.helpFeeButton) {
			ZiftrDialogManager.showSimpleAlert(getFragmentManager(), R.string.send_fee_info);
		}
	}

	
	
	private void doSendCoins() {
		
		ZWCoin coin = getSelectedCoin();
		
		String outputAddress = this.addressEditText.getText().toString();
		String outputLabel = labelEditText.getText().toString();
		
		BigDecimal amountDecimal = getAmountToSendFromEditText();
		BigInteger amount = coin.getAtomicUnits(amountDecimal);
		
		BigDecimal feePerKbDecimal = getFeeFromEditText();
		BigInteger feePerKb = coin.getAtomicUnits(feePerKbDecimal);
		
		ZWSendTaskHelperFragment sendTask = new ZWSendTaskHelperFragment();
		getFragmentManager().beginTransaction().add(sendTask, ZWSendTaskHelperFragment.FRAGMENT_TAG).commit();
		
		sendTask.sendCoins(coin, feePerKb, amount, outputAddress, outputLabel);
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
		this.coinAmountEditText = (EditText) 
				this.rootView.findViewById(R.id.sendAmountCoinFiatDualView).findViewById(R.id.dualTextBoxLinLayout1).findViewById(R.id.customEditText);
		coinAmountEditText.setId(R.id.zw_send_coin_amount);

		this.fiatAmountEditText = (EditText) this.rootView.findViewById(R.id.sendAmountCoinFiatDualView
				).findViewById(R.id.dualTextBoxLinLayout2).findViewById(R.id.customEditText);
		fiatAmountEditText.setId(R.id.zw_send_fiat_amount);

		this.fiatFeeEditText = 
				(EditText) this.rootView.findViewById(R.id.sendEditTextTransactionFeeFiatEquiv).findViewById(R.id.customEditText);
		fiatFeeEditText.setId(R.id.zw_send_fiat_fee);

		this.coinFeeEditText = 
				(EditText) this.rootView.findViewById(R.id.sendEditTextTransactionFee).findViewById(R.id.customEditText);
		coinFeeEditText.setId(R.id.zw_send_coin_fee);

		this.labelEditText = (EditText) this.rootView.findViewById(
				R.id.send_edit_text_reciever_name).findViewById(R.id.customEditText);
		labelEditText.setId(R.id.zw_send_name);

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

		
		ZWFiat selectedFiat = ZWPreferences.getFiatCurrency();

		this.fiatAmountLabel = (TextView) this.rootView.findViewById(R.id.amount_fiat_label);
		this.fiatFeeLabel = (TextView) this.rootView.findViewById(R.id.fee_fiat_label);
		this.fiatAmountLabel.setText(selectedFiat.getName());
		this.fiatFeeLabel.setText(selectedFiat.getName());
		this.totalTextView = (TextView) this.rootView.findViewById(R.id.sendTotalTextView);
		this.totalFiatEquivTextView = (TextView) this.rootView.findViewById(R.id.sendTotalFiatEquivTextView);

		if (ZWPreferences.getFeesAreEditable()) {
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
		fiatAmountEditText.setHint(ZWPreferences.getFiatCurrency().getFormattedAmount(BigDecimal.ZERO));
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

		feeEditText.setText(getSelectedCoin().getFormattedAmount(getSelectedCoin().getDefaultFee()));

	}

	private void initializeEditText() {
		// Why wasn't all of this stuff done in XML? Because the most edittexts are using the same style so we are
		// including the edittext layout so this has to be done programmatically in order to modify these  specific editTexts 
		// and not the default styled edittext included all over the app
		this.coinAmountEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
		this.fiatAmountEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
		this.coinFeeEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
		
		this.coinAmountEditText.setImeOptions(EditorInfo.IME_ACTION_NEXT);
		if (ZWPreferences.getFeesAreEditable()){
			this.fiatAmountEditText.setImeOptions(EditorInfo.IME_ACTION_NEXT);
			this.coinFeeEditText.setImeOptions(EditorInfo.IME_ACTION_NEXT);
		}
		this.fiatFeeEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
		
		//line wrapping edittext with next button instead of return button on keyboard
		this.addressEditText.setSingleLine(true);
		this.addressEditText.setLines(3);
		this.addressEditText.setHorizontallyScrolling(false);
		this.addressEditText.setImeOptions(EditorInfo.IME_ACTION_NEXT);
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
		this.totalTextView.setText(getSelectedCoin().getFormattedAmount(total));

		// Update the text in the total fiat equiv
		ZWFiat selectedFiat = ZWPreferences.getFiatCurrency();
		BigDecimal fiatTotal = ZWConverter.convert(total, getSelectedCoin(), selectedFiat);
		
		this.totalFiatEquivTextView.setText("(" + selectedFiat.getFormattedAmount(fiatTotal, true) + ")");
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

	
	//call before fragment loads
	public void preloadSendData(ZWCoinURI coinUri) {
		this.preloadedCoinUri = coinUri;

	}
	
	
	//call before fragment loads
	public void preloadAddress(String address) {
		this.preloadAddress = address;
	}
	
	
	//call if fragment already showing
	public void loadSendData(ZWCoinURI coinUri){
		//refresh header if selected coin changed
		this.populateWalletHeader(rootView.findViewById(R.id.walletHeader));
		this.setupViews(coinUri);
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

	
	public void finishedSending(ZWCoin coin) {
		ZWMainFragmentActivity activity = getZWMainActivity();
		if(activity != null) {
			activity.onBackPressed();
			String toastMessage = activity.getString(R.string.zw_toast_sent);
			toastMessage = String.format(toastMessage, coin.getName());
			Toast.makeText(activity, toastMessage, Toast.LENGTH_LONG).show();
		}
	}


}
