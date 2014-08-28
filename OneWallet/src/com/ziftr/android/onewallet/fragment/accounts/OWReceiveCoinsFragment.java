package com.ziftr.android.onewallet.fragment.accounts;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.crypto.ECKey;
import com.ziftr.android.onewallet.sqlite.OWSQLiteOpenHelper;
import com.ziftr.android.onewallet.util.Base58;
import com.ziftr.android.onewallet.util.QRCodeEncoder;

public abstract class OWReceiveCoinsFragment extends OWWalletUserFragment {

	/** The view container for this fragment. */
	private View rootView;

	/**
	 * Inflate, initialize, and return the send coins layout.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {

		this.rootView = inflater.inflate(R.layout.accounts_receive_coins, container, false);

		// TODO get database from activity and have activity open/close
		// Use callbacks so that UI can load quickly
		OWSQLiteOpenHelper database = OWSQLiteOpenHelper.getInstance(getActivity());
		ECKey key = null;
		if (database.getNumPersonalAddresses(getCoinId()) >= 1) {
			key = database.readAllPersonalAddresses(getCoinId()).get(0);
		} else {
			key = database.createPersonal(getCoinId());
		}
		OWSQLiteOpenHelper.closeInstance();
		String newAddress = Base58.encode(getCoinId().getPubKeyHashPrefix(), key.getPubKeyHash()); 
		
		// initialize the displaying of the address.
		this.initializeAddress(newAddress);
		
		// initialize the icons that 
		// Make the image view have the data bitmap
		this.initializeQrCode(newAddress);

		// Initialize the icons so they do the correct things onClick
		this.initializeAddressUtilityIcons();

		return this.rootView;
	}

	@Override
	public void onResume(){
		super.onResume();
		this.getOWMainActivity().changeActionBar("RECEIVE", false, true, false);
	}


	private void initializeAddress(String newAddress) {
		TextView addressTextView = (EditText) 
				this.rootView.findViewById(R.id.addressValueTextView);
		addressTextView.setText(newAddress);
	}

	private void initializeAddressUtilityIcons() {
		// The copy icon
		final ImageView copyIcon = (ImageView) this.rootView.findViewById(R.id.receiveCopyIcon);
		copyIcon.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				copyIcon.setSelected(arg1.getAction()==MotionEvent.ACTION_DOWN);
				return true;
			}
		});
	}

	private void initializeQrCode(String newAddress) {
		// TODO make it so that whenever the text changes the bit map changes
		ImageView imageView = (ImageView) this.rootView.findViewById(R.id.show_qr_img);

		int qrCodeDimention = 500;

		QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(newAddress, null,
				Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimention);

		try {
			Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
			imageView.setImageBitmap(bitmap);
		} catch (WriterException e) {
			e.printStackTrace();
		}

	}

}