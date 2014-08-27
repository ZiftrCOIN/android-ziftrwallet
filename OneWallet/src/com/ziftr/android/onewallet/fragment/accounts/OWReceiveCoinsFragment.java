package com.ziftr.android.onewallet.fragment.accounts;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.bitcoin.core.ECKey;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.util.QRCodeEncoder;

public abstract class OWReceiveCoinsFragment extends OWWalletUserFragment {
	
	/** The view container for this fragment. */
	private View rootView;
	
	private String newAddress;

	/**
	 * Inflate, initialize, and return the send coins layout.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {

		this.rootView = inflater.inflate(
				R.layout.accounts_receive_coins, container, false);
		
		ECKey key = getWallet().getKeys().get(0);
		this.newAddress = key.toAddress(getCoinId().getNetworkParameters()).toString();

		// initialize the displaying of the address.
		this.initializeAddress();
		
		// Make the image view have the data bitmap
		this.initializeQrCode();
		
		return this.rootView;
	}
	
	@Override
	public void onResume(){
		super.onResume();
		this.getOWMainActivity().editTitle("RECEIVE");
	}


	private void initializeAddress() {
		TextView addressTextView = (TextView) 
				this.rootView.findViewById(R.id.addressValueTextView);
		addressTextView.setText(this.newAddress);
	}

	private void initializeQrCode() {
		ImageView imageView = (ImageView) this.rootView.findViewById(R.id.show_qr_img);

		int qrCodeDimention = 500;
		
		QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(this.newAddress, null,
		        Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimention);

		try {
		    Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
		    imageView.setImageBitmap(bitmap);
		} catch (WriterException e) {
		    e.printStackTrace();
		}
	}
	
}