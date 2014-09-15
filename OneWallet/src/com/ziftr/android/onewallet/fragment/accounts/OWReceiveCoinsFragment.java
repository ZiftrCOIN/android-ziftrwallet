package com.ziftr.android.onewallet.fragment.accounts;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
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
import com.ziftr.android.onewallet.util.OWUtils;
import com.ziftr.android.onewallet.util.QRCodeEncoder;
import com.ziftr.android.onewallet.util.ZLog;

public class OWReceiveCoinsFragment extends OWWalletUserFragment {

	/** The view container for this fragment. */
	private View rootView;

	/** The address that will be displayed in the QR code and where others can send coins to us at. */
	private String addressToReceiveOn;

	/** The key used to save the current address in bundles. */
	private static final String KEY_ADDRESS = "KEY_ADDRESS";

	private static final int FADE_DURATION = 500;

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

		this.rootView = inflater.inflate(R.layout.accounts_receive_coins, container, false);

		ZLog.log("oncreate 1");
		this.initializeQrCodeFromBundle(savedInstanceState);
		ZLog.log("oncreate 2");

		// Initialize the icons so they do the correct things onClick
		this.initializeAddressUtilityIcons();

		this.initializeWalletHeaderView();

		return this.rootView;
	}

	/**
	 * @return
	 */
	private ImageView getAddressQrCodeImageView() {
		return (ImageView) this.rootView.findViewById(R.id.generateAddressQrCodeImageView);
	}

	private View getAddressQrCodeContainer() {
		return this.rootView.findViewById(R.id.generateAddressQrCodeContainer);
	}
	
	@Override
	public void onStop() {
		this.setQrCodeGenerated(false);
		super.onStop();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (this.fragmentHasAddress()) {
			outState.putString(KEY_ADDRESS, this.addressToReceiveOn);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onResume() {
		super.onResume();
		this.getOWMainActivity().changeActionBar("RECEIVE", false, true);
	}

	private void initializeAddressUtilityIcons() {
		getAddressQrCodeImageView().setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!fragmentHasAddress()) {
					if (getOWMainActivity().userHasPassphrase()) {
						Bundle b = new Bundle();
						b.putString(OWCoin.TYPE_KEY, getCurSelectedCoinType().toString());
						getOWMainActivity().showGetPassphraseDialog(
								OWRequestCodes.VALIDATE_PASSPHRASE_DIALOG_NEW_KEY, b, 
								"validate_passphrase_dialog_new_key");
					} else {
						loadAddressFromDatabase();
					}
				}
			}
		});

		// For the 'copy' button
		View copyButton = this.rootView.findViewById(R.id.receiveCopyIcon);
		// Set the listener for the clicks
		copyButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (fragmentHasAddress()) {
					// Gets a handle to the clipboard service.
					ClipboardManager clipboard = (ClipboardManager)
							getActivity().getSystemService(Context.CLIPBOARD_SERVICE);

					ClipData clip = ClipData.newPlainText(KEY_ADDRESS, addressToReceiveOn);
					clipboard.setPrimaryClip(clip);

					Toast.makeText(getActivity(), "Text copied.", Toast.LENGTH_SHORT).show();
				}
			}
		});
	}

	private void generateQrCode(boolean withAnimation) {
		generateQrCode(this.addressToReceiveOn, withAnimation);
	}

	private void generateQrCode(String newAddress, boolean withAnimation) {
		ZLog.log("generateQrCode");
		ZLog.log("curAdd: " + this.addressToReceiveOn);
		ZLog.log("curAddInET: " + this.getAddressEditText().getText().toString());
		
		this.addressToReceiveOn = newAddress;

		this.getAddressQrCodeContainer().setVisibility(View.VISIBLE);

		ImageView imageView = this.getAddressQrCodeImageView();
		imageView.setPadding(0, 0, 0, 0);

		int qrCodeDimention = imageView.getWidth();
		QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(this.addressToReceiveOn, null,
				Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimention);

		try {
			Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();

			if (withAnimation) {
				Drawable[] layers = new Drawable[2];
				layers[0] = imageView.getDrawable();
				layers[1] = new BitmapDrawable(getResources(), bitmap);
				TransitionDrawable transitionDrawable = new TransitionDrawable(layers);
				imageView.setImageDrawable(transitionDrawable);
				transitionDrawable.startTransition(FADE_DURATION);
			} else {
				imageView.setImageBitmap(bitmap);
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
		if (savedInstanceState == null) {
			ZLog.log("savedInstanceState == null");
		} else if (savedInstanceState.getString(KEY_ADDRESS) == null) {
			ZLog.log("savedInstanceState.getString(KEY_ADDRESS) == null");
		} else {
			ZLog.log("address from savedInstanceState: " + savedInstanceState.getString(KEY_ADDRESS));
		}
		
		
		boolean initializedWithAddress = savedInstanceState != null && 
				savedInstanceState.getString(KEY_ADDRESS) != null;

		if (initializedWithAddress) {
			// We have to set it to be invisible temporarily to not have a blue flash,
			// will be drawn correctly once we can get the measurement of the qr code
			this.getAddressQrCodeContainer().setBackgroundColor(getResources().getColor(R.color.transparent));
			this.getAddressQrCodeContainer().setVisibility(View.INVISIBLE);

			this.addressToReceiveOn = savedInstanceState.getString(KEY_ADDRESS);
		} else {
			this.addressToReceiveOn = "";
		}
		
		ZLog.log("curAddText: " + this.getAddressEditText().getText().toString());

		putCurAddressIntoEditText();
		
		ZLog.log("curAddText: " + this.getAddressEditText().getText().toString());

		final ImageView imageLayout = getAddressQrCodeImageView();
		final ViewTreeObserver viewObserver = imageLayout.getViewTreeObserver();
		viewObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				if (fragmentHasAddress() && !isQrCodeGenerated()) {
					generateQrCode(false);
					ZLog.log("onGlobalLayout");
				}
				
//				if (!getAddressEditText().getText().toString().equals(addressToReceiveOn)) {
//					getAddressEditText().setText(addressToReceiveOn);
//				}
			}
		});
	}

	public boolean fragmentHasAddress() {
		return this.addressToReceiveOn != null && !this.addressToReceiveOn.isEmpty();
	}

	/**
	 * 
	 */
	private void setAddressInEditText(String newAddress) {
		ZLog.log("asdf addressToReceiveOn: " + this.addressToReceiveOn);
		ZLog.log("asdf newAddress: " + newAddress);
		
		this.addressToReceiveOn = newAddress;
		getAddressEditText().setText(this.addressToReceiveOn);
	}

	/**
	 * @return
	 */
	private EditText getAddressEditText() {
		return (EditText) this.rootView.findViewById(R.id.addressValueTextView);
	}

	/**
	 * 
	 */
	private void putCurAddressIntoEditText() {
		this.setAddressInEditText(addressToReceiveOn);
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
				final OWAddress address = database.createReceivingAddress(getCurSelectedCoinType());

				// Run the updating of the UI on the UI thread
				OWReceiveCoinsFragment.this.getOWMainActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						String newAddress = address.toString();
						setAddressInEditText(newAddress);
						generateQrCode(newAddress, true);
					}
				});
			}
		});
	}

}