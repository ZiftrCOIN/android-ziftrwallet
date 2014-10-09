package com.ziftr.android.ziftrwallet.fragment.accounts;

import java.math.BigDecimal;
import java.math.BigInteger;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import com.ziftr.android.ziftrwallet.OWWalletManager;
import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.crypto.OWAddress;
import com.ziftr.android.ziftrwallet.util.OWCoin;
import com.ziftr.android.ziftrwallet.util.OWCoinURI;
import com.ziftr.android.ziftrwallet.util.OWPreferencesUtils;
import com.ziftr.android.ziftrwallet.util.OWRequestCodes;
import com.ziftr.android.ziftrwallet.util.OWTags;
import com.ziftr.android.ziftrwallet.util.OWTextWatcher;
import com.ziftr.android.ziftrwallet.util.QRCodeEncoder;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class OWReceiveCoinsFragment extends OWAddressBookParentFragment {

	/** The key used to save the current address in bundles. */
	private static final String KEY_ADDRESS = "KEY_ADDRESS";

	private static final int FADE_DURATION = 500;

	/** The view container for this fragment. */
	private View rootView;

	private ImageView copyButton;
	private ImageView qrCodeImageView;
	private View qrCodeContainer;
	private View scrollView;
	private ImageView generateAddressForLabel;
	private EditText messageEditText;

	private boolean qrCodeGenerated = false;

	/**
	 * Inflate, initialize, and return the send coins layout.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.setQrCodeGenerated(false);

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
		this.setQrCodeGenerated(false);
		super.onStop();
	}

	@Override
	public void onClick(View v) {

		if(v == this.copyButton) {
			if(this.fragmentHasAddress()) {
				// Gets a handle to the clipboard service.
				ClipboardManager clipboard = (ClipboardManager)
						getActivity().getSystemService(Context.CLIPBOARD_SERVICE);

				ClipData clip = ClipData.newPlainText(KEY_ADDRESS, addressEditText.getText().toString());
				clipboard.setPrimaryClip(clip);

				Toast.makeText(getActivity(), "Text copied.", Toast.LENGTH_SHORT).show();
			}
		} else if (v == this.qrCodeImageView) {
			this.conditionallyGenerateNewAddress(true);
		} else if (v == this.getAddressBookImageView()) {
			this.openAddressBook(true, R.id.receiveCoinBaseFrameLayout);
		} else if (v == this.generateAddressForLabel) {
			this.conditionallyGenerateNewAddress(false);
		}

	}

	private void conditionallyGenerateNewAddress(boolean requireFragHasAddress) {
		if(!(requireFragHasAddress && this.fragmentHasAddress()) && !this.labelEditText.getText().toString().isEmpty()) {
			// TODO make the passphrase diaog have extra information about how they 
			// won't be able to delete the address they are about to make
			if (OWPreferencesUtils.userHasPassphrase(getOWMainActivity())) {
				Bundle b = new Bundle();
				b.putString(OWCoin.TYPE_KEY, getSelectedCoin().toString());
				getOWMainActivity().showGetPassphraseDialog(
						OWRequestCodes.VALIDATE_PASSPHRASE_DIALOG_NEW_KEY, b, 
						OWTags.VALIDATE_PASS_RECEIVE);
			} else {
				getOWMainActivity().alertConfirmation(OWRequestCodes.CONFIRM_CREATE_NEW_ADDRESS, 
						"Addresses cannot be deleted once they are created. Are you sure?", 
						OWTags.CONFIRM_NEW_ADDRESS, new Bundle());
			}
		}
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
				).findViewById(R.id.dualTextBoxLinLayout1).findViewWithTag(OWTags.OW_EDIT_TEXT);
		this.coinAmountEditText.setId(R.id.ow_receive_coin_amount);
		this.coinAmountEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
		this.coinAmountEditText.addTextChangedListener(new OWTextWatcher() {
			@Override
			public void afterTextChanged(Editable arg0) {
				// If we just set this to false, then the global layout listener should
				// redraw the qr code
				setQrCodeGenerated(false);
			}
		});

		this.fiatAmountEditText = (EditText) this.rootView.findViewById(R.id.receiveAmountCoinFiatDualView
				).findViewById(R.id.dualTextBoxLinLayout2).findViewWithTag(OWTags.OW_EDIT_TEXT);
		this.fiatAmountEditText.setId(R.id.ow_receive_fiat_amount);
		this.fiatAmountEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);

		this.bindEditTextValues(this.coinAmountEditText, this.fiatAmountEditText, 
				this.changeCoinStartedFromProgram, this.changeFiatStartedFromProgram);
		
		// For the message edit text
		this.messageEditText = (EditText) this.rootView.findViewById(
				R.id.receiveMessageContainer).findViewWithTag(OWTags.OW_EDIT_TEXT);
		this.messageEditText.setId(R.id.ow_receive_message);
		this.messageEditText.addTextChangedListener(new OWTextWatcher() {
			@Override
			public void afterTextChanged(Editable arg0) {
				// If we just set this to false, then the global layout listener should
				// redraw the qr code
				setQrCodeGenerated(false);
			}
		});

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

		this.labelEditText = (EditText) this.rootView.findViewById(R.id.addressName).findViewWithTag(OWTags.OW_EDIT_TEXT);
		this.labelEditText.setId(R.id.ow_receive_address_label);
		this.labelEditText.requestFocus();
		this.labelEditText.addTextChangedListener(new OWTextWatcher() {
			@Override
			public void afterTextChanged(Editable arg0) {
				if (fragmentHasAddress()) {
					updateAddressLabelInDatabase();
				}
				refreshAddNewAddressButtonsEnabled();
			}
		});

		this.refreshAddNewAddressButtonsEnabled();
	}

	private void generateQrCode(boolean withAnimation) {
		String addressString = this.addressEditText.getText().toString();

		String amountString = this.coinAmountEditText.getText().toString();
		BigInteger amountVal = null;
		try {
			amountVal = ZiftrUtils.bigDecToBigInt(getSelectedCoin(), new BigDecimal(amountString));
		} catch(NumberFormatException nfe) {
			// Amount is just left null
		}
		if (amountVal != null && (amountVal.compareTo(BigInteger.ZERO) < 0)) {
			amountVal = null;
		}

		String message = this.messageEditText.getText().toString();
		message = message != null && message.isEmpty() ? null : message;
		String qrCodeData = OWCoinURI.convertToCoinURI(getSelectedCoin(), addressString, amountVal, 
				OWPreferencesUtils.getUserName(getActivity()), message);

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

		this.setQrCodeGenerated(true);
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
			this.qrCodeContainer.setBackgroundColor(getResources().getColor(R.color.transparent));
			this.qrCodeContainer.setVisibility(View.INVISIBLE);
		} 

		ViewTreeObserver viewObserver = qrCodeImageView.getViewTreeObserver();
		viewObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				if (getActivity() != null) {
					if (fragmentHasAddress() && !isQrCodeGenerated()) {
						generateQrCode(false);
					}
				}
			}
		});
	}

	/**
	 * This method gets the database from the activity on the UI thread,
	 * gets a new key from the database helper on an extra thread, and
	 * then updates the UI with the new address on the UI thread. 
	 */
	public void loadNewAddressFromDatabase(final String passphrase) {
		// Get the database from the activity on the UI thread
		final OWWalletManager database = this.getWalletManager();
		final String addressLabel = addressEditText.getText().toString();
		// Run database IO on new thread
		ZiftrUtils.runOnNewThread(new Runnable() {
			@Override
			public void run() {
				final OWAddress address = database.createReceivingAddress(passphrase, getSelectedCoin(), addressLabel);

				// Run the updating of the UI on the UI thread
				OWReceiveCoinsFragment.this.getOWMainActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						String newAddress = address.toString();
						addressEditText.setText(newAddress);
						updateAddressLabelInDatabase();
						generateQrCode(true);
					}
				});
			}
		});
	}

	/**
	 * @return the qrCodeGenerated
	 */
	public boolean isQrCodeGenerated() {
		return qrCodeGenerated;
	}

	/**
	 * @param qrCodeGenerated the qrCodeGenerated to set
	 */
	public void setQrCodeGenerated(boolean qrCodeGenerated) {
		this.qrCodeGenerated = qrCodeGenerated;
	}

	@Override
	public void acceptAddress(String address, String label) {
		super.acceptAddress(address, label);
		this.messageEditText.setText("");
		this.coinAmountEditText.setText("");
		this.fiatAmountEditText.setText("");
		this.setQrCodeGenerated(false);
	}

	@Override
	public String getActionBarTitle() {
		return "RECEIVE"; 
	}

	@Override
	public View getContainerView() {
		return this.scrollView;
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

}

