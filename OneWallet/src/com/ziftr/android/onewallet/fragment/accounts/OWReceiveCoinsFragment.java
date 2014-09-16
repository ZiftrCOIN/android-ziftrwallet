package com.ziftr.android.onewallet.fragment.accounts;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.crypto.OWAddress;
import com.ziftr.android.onewallet.sqlite.OWSQLiteOpenHelper;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWRequestCodes;
import com.ziftr.android.onewallet.util.OWTags;
import com.ziftr.android.onewallet.util.OWUtils;
import com.ziftr.android.onewallet.util.QRCodeEncoder;

public class OWReceiveCoinsFragment extends OWWalletUserFragment implements OnClickListener, TextWatcher {

	/** The key used to save the current address in bundles. */
	private static final String KEY_ADDRESS = "KEY_ADDRESS";

	private static final int FADE_DURATION = 500;

	/** The view container for this fragment. */
	private View rootView;

	private EditText labelEditText;
	private EditText addressEditText;
	private View copyButton;
	private ImageView addressBookImageView;
	private ImageView qrCodeImageView;
	private View qrCodeContainer;


	/** The address that will be displayed in the QR code and where others can send coins to us at. */
	//private String addressToReceiveOn;


	private boolean qrCodeGenerated = false;

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

	/**
	 * Inflate, initialize, and return the send coins layout.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.setQrCodeGenerated(false);

		this.initializeViewFields(inflater, container);

		this.initializeQrCodeFromBundle(savedInstanceState);

		this.showWalletHeader();

		return this.rootView;
	}

	/**
	 * @param inflater
	 * @param container
	 */
	private void initializeViewFields(LayoutInflater inflater,
			ViewGroup container) {
		this.rootView = inflater.inflate(R.layout.accounts_receive_coins, container, false);
		this.addressEditText = (EditText) this.rootView.findViewById(R.id.addressValueTextView);
		this.labelEditText = (EditText) this.rootView.findViewById(R.id.addressNameEditText);
		this.labelEditText.addTextChangedListener(this);

		this.copyButton = this.rootView.findViewById(R.id.receiveCopyIcon);
		this.copyButton.setOnClickListener(this);

		this.qrCodeImageView = (ImageView) this.rootView.findViewById(R.id.generateAddressQrCodeImageView);
		this.qrCodeImageView.setOnClickListener(this);

		this.addressBookImageView = (ImageView) this.rootView.findViewById(R.id.recallAddressFromHistoryIcon);
		this.addressBookImageView.setOnClickListener(this);

		this.qrCodeContainer = this.rootView.findViewById(R.id.generateAddressQrCodeContainer);
	}

	@Override
	public void onStop() {
		this.setQrCodeGenerated(false);
		super.onStop();
	}

	@Override
	public void onResume() {
		super.onResume();
		this.getOWMainActivity().changeActionBar("RECEIVE", false, true);
	}

	@Override
	public void onClick(View v) {

		if(v == this.copyButton) {
			if(this.addressEditText.getText().toString().length() > 0) {
				// Gets a handle to the clipboard service.
				ClipboardManager clipboard = (ClipboardManager)
						getActivity().getSystemService(Context.CLIPBOARD_SERVICE);

				ClipData clip = ClipData.newPlainText(KEY_ADDRESS, addressEditText.getText().toString());
				clipboard.setPrimaryClip(clip);

				Toast.makeText(getActivity(), "Text copied.", Toast.LENGTH_SHORT).show();
			}
		} else if (v == this.qrCodeImageView) {
			if(this.addressEditText.getText().toString().length() == 0) {
				if (getOWMainActivity().userHasPassphrase()) {
					Bundle b = new Bundle();
					b.putString(OWCoin.TYPE_KEY, getCurSelectedCoinType().toString());
					getOWMainActivity().showGetPassphraseDialog(
							OWRequestCodes.VALIDATE_PASSPHRASE_DIALOG_NEW_KEY, b, 
							OWTags.VALIDATE_PASS_RECEIVE);
				} else {
					loadAddressFromDatabase();
				}
			}
		} else if (v == this.addressBookImageView) {
			getOWMainActivity().openAddressBook(true);
		}
		
	}

	private void generateQrCode(boolean withAnimation) {
		String addressString = this.addressEditText.getText().toString();

		this.qrCodeContainer.setVisibility(View.VISIBLE);
		this.qrCodeImageView.setPadding(0, 0, 0, 0);

		int qrCodeDimention = this.qrCodeImageView.getWidth();
		QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(addressString, null,
				Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimention);

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
		String addressString = this.addressEditText.getText().toString();
		if (addressString.length() > 0) {
			// We have to set it to be invisible temporarily to not have a blue flash,
			// will be drawn correctly once we can get the measurement of the qr code
			this.qrCodeContainer.setBackgroundColor(getResources().getColor(R.color.transparent));
			this.qrCodeContainer.setVisibility(View.INVISIBLE);
		} 

		final ImageView imageLayout = this.qrCodeImageView;
		final ViewTreeObserver viewObserver = imageLayout.getViewTreeObserver();
		viewObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				if (fragmentHasAddress() && !isQrCodeGenerated()) {
					generateQrCode(false);
				}
			}
		});
	}

	public boolean fragmentHasAddress() {
		return addressEditText.getText().toString().length() > 0;
	}

	/**
	 * This method gets the database from the activity on the UI thread,
	 * gets a new key from the database helper on an extra thread, and
	 * then updates the UI with the new address on the UI thread. 
	 */
	public void loadAddressFromDatabase() {
		// Get the database from the activity on the UI thread
		final OWSQLiteOpenHelper database = this.getWalletManager();
		// Run database IO on new thread
		OWUtils.runOnNewThread(new Runnable() {
			@Override
			public void run() {
				String addressLabel = addressEditText.getText().toString();

				final OWAddress address = database.createReceivingAddress(getCurSelectedCoinType(), addressLabel);

				// Run the updating of the UI on the UI thread
				OWReceiveCoinsFragment.this.getOWMainActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						String newAddress = address.toString();
						addressEditText.setText(newAddress);
						generateQrCode(true);
					}
				});
			}
		});
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub
	}

	@Override
	public void afterTextChanged(Editable s) {

	}


}