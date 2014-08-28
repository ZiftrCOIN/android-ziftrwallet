package com.ziftr.android.onewallet.fragment.accounts;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
		
		// Make the image view have the data bitmap
		this.initializeQrCode(newAddress);
		
		return this.rootView;
	}
	
	@Override
	public void onResume(){
		super.onResume();
		this.getOWMainActivity().changeActionBar("RECEIVE", false, true, false);
	}


	private void initializeAddress(String newAddress) {
		TextView addressTextView = (TextView) 
				this.rootView.findViewById(R.id.addressValueTextView);
		addressTextView.setText(newAddress);
	}

	private void initializeQrCode(String newAddress) {
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