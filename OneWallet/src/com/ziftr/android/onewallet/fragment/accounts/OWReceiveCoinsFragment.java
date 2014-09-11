package com.ziftr.android.onewallet.fragment.accounts;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
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

public abstract class OWReceiveCoinsFragment extends OWWalletUserFragment {

	/** The view container for this fragment. */
	private View rootView;

	/** The address that will be displayed in the QR code and where others can send coins to us at. */
	private String addressToReceiveOn;
	
	/** The key used to save the current address in bundles. */
	private static final String KEY_ADDRESS = "KEY_ADDRESS";

	/**
	 * Inflate, initialize, and return the send coins layout.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.rootView = inflater.inflate(R.layout.accounts_receive_coins, container, false);
		// Initialize the icons so they do the correct things onClick
		this.initializeAddressUtilityIcons();

		this.initializeWalletHeaderView();
		if (savedInstanceState == null) {
			// Set the address to initially be empty
			this.refresh("");
		} else {
			// Get the saved address from bundle
			this.refresh(savedInstanceState.getString(KEY_ADDRESS));
		}

		return this.rootView;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (this.addressToReceiveOn != null) {
			outState.putString(KEY_ADDRESS, this.addressToReceiveOn);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onResume(){
		super.onResume();
		this.getOWMainActivity().changeActionBar("RECEIVE", false, true);
	}

	private void initializeAddressUtilityIcons() {
		// TODO
		ImageView newAddressIcon = (ImageView) this.rootView.findViewById(R.id.generateNewAddressIcon);
		newAddressIcon.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Bundle b = new Bundle();
				b.putString(OWCoin.TYPE_KEY, getCoinId().toString());
				getOWMainActivity().showGetPassphraseDialog(OWRequestCodes.VALIDATE_PASSPHRASE_DIALOG_NEW_KEY, b, 
						"validate_passphrase_dialog_new_key");
			}
		});

		// For the 'copy' button
		View copyButton = this.rootView.findViewById(R.id.receiveCopyIcon);
		// Set the listener for the clicks
		copyButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (addressToReceiveOn != null && !addressToReceiveOn.isEmpty()) {
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

	private void generateQrCode() {
		// TODO make it so that whenever the text changes the bit map changes
		ImageView imageView = (ImageView) this.rootView.findViewById(R.id.show_qr_img);

		int qrCodeDimention = 500;

		QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(this.addressToReceiveOn, null,
				Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimention);

		try {
			Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
			imageView.setImageBitmap(bitmap);
		} catch (WriterException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Refreshes the display with the new address. Should only be run on 
	 * the UI thread. 
	 * 
	 * @param newAddress
	 */
	private void refresh(String newAddress) {
		this.addressToReceiveOn = newAddress;

		TextView addressTextView = (EditText) 
				this.rootView.findViewById(R.id.addressValueTextView);
		addressTextView.setText(this.addressToReceiveOn);
		// initialize the icons that 
		// Make the image view have the data bitmap
		this.generateQrCode();
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
				final OWAddress address = database.createReceivingAddress(getCoinId());

				// Run the updating of the UI on the UI thread
				OWReceiveCoinsFragment.this.getOWMainActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						refresh(address.toString());
					}
				});
			}
		});
	}

}