package com.ziftr.android.onewallet;

import java.util.Arrays;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.google.zxing.client.android.CaptureActivity;
import com.ziftr.android.onewallet.dialog.OWPassphraseDialog;
import com.ziftr.android.onewallet.dialog.OWSimpleAlertDialog;
import com.ziftr.android.onewallet.dialog.handlers.OWNeutralDialogHandler;
import com.ziftr.android.onewallet.dialog.handlers.OWPassphraseDialogHandler;
import com.ziftr.android.onewallet.util.OWUtils;
import com.ziftr.android.onewallet.util.RequestCodes;
import com.ziftr.android.onewallet.util.ZLog;

/**
 * The OWMainActivity starts this fragment. This fragment is 
 * associated with a few buttons where the user can choose what
 * kind of wallet they want to open.
 */
public class OWHomeFragment extends Fragment 
implements OWPassphraseDialogHandler, OWNeutralDialogHandler {

	/** The view container for this fragment. */
	protected View rootView;

	/** The most recently clicked button. */
	private View clickedButton;

	/** The key for getting the hash from the preferences. */
	private final String PASSPHRASE_KEY = "ow_passphrase_key_1";
	
	/** 
	 * Placeholder for later, doesn't do anything other than 
	 * what parent method does right now.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	/**
	 * To create the view for this fragment, we start with the 
	 * basic fragment_home view which just has a few buttons that
	 * open up different coin-type wallets.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_home, container, false);

		// For the bitcoinWalletButton
		View bitcoinWalletButton = rootView.findViewById(R.id.buttonBitcoinWallet);
		// Add what kind of fragment this button should create as tag
		bitcoinWalletButton.setTag(OWBitcoinWalletFragment.class); 
		// Set the listener for the clicks
		bitcoinWalletButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				OWHomeFragment.this.clickedButton = v;
				
				OWPassphraseDialog passphraseDialog = new OWPassphraseDialog();
				
				// Set the target fragment
				passphraseDialog.setTargetFragment(OWHomeFragment.this, 
						RequestCodes.GET_PASSPHRASE_DIALOG);
				
				String message = "Please input your passphrase. ";
				if (OWHomeFragment.this.userHasPassphrase()) {
					message += "This will be used for securing all your wallets.";
				}
				passphraseDialog.setupDialog("OneWallet", message, 
						"Continue", null, "Cancel");
				passphraseDialog.show(OWHomeFragment.this.getFragmentManager(), 
						"wrong_passphrase");
			}
		});

		// For the getQRCodeButton
		View getQRCodeButton = rootView.findViewById(R.id.getQRCodeButton);
		// Set the listener for the clicks
		getQRCodeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(
						OWHomeFragment.this.getActivity(), CaptureActivity.class);
				intent.setAction("com.google.zxing.client.android.SCAN");
				// for Regular bar code, its ÒPRODUCT_MODEÓ instead of ÒQR_CODE_MODEÓ
				intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
				intent.putExtra("SAVE_HISTORY", false);
				startActivityForResult(intent, 0);
			}
		});

		// Return the view which was inflated
		return rootView;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (data != null && data.hasExtra("SCAN_RESULT")) {
			// Get the result of the QR scan
			String result = data.getStringExtra("SCAN_RESULT");

			OWSimpleAlertDialog alertUserDialog = new OWSimpleAlertDialog();
			alertUserDialog.setTargetFragment(this, 
					RequestCodes.ALERT_USER_DIALOG);
			// Set up the dialog, displaying the resulting scanned data.
			// Set negative text to null to not have negative button
			alertUserDialog.setupDialog("OneWallet", 
					"The scanned data is:\n" + result, null, "OK", null);
			// Pop up the dialog
			alertUserDialog.show(this.getFragmentManager(), "show_qr_result");
		}
	}

	/**
	 * Uses the tag of the current clickedButton field to 
	 * make a new fragment and then transition to that fragment.
	 */
	private void startFragmentFromButtonTag() {
		try {
			@SuppressWarnings("unchecked") 
			Class<? extends Fragment> newFragmentClass = (
					Class<? extends Fragment>) this.clickedButton.getTag();

			Fragment fragment = newFragmentClass.newInstance();
			String tagString = newFragmentClass.getSimpleName();

			getFragmentManager().beginTransaction().replace(
					R.id.mainActivityContainer, fragment, tagString
					).addToBackStack(null).commit();
		} catch(Exception e) {
			ZLog.log("Exceptiong trying to load wallet fragment. Was a "
					+ "button not properly setup? ", e);
		}
	}
	
	/**
	 * Gets the stored hash of the users passphrase, if there is one.
	 * 
	 * @return the stored hash of the users passphrase, if there is one.
	 */
	private byte[] getStoredPassphraseHash() {
		// Get the preferences
		SharedPreferences prefs = getActivity().getSharedPreferences(
				PASSPHRASE_KEY, Context.MODE_PRIVATE);
		// Get the passphrase hash
		String storedPassphrase = prefs.getString(
				this.PASSPHRASE_KEY, null);
		// If it's not null, convert it back to a byte array
		byte[] storedHash = (storedPassphrase == null) ?
				null : OWUtils.hexStringToBinary(storedPassphrase);
		// Return the result
		return storedHash;
	}

	/**
	 * Get a boolean describing whether or not the user 
	 * has entered a passphrase before.
	 * 
	 * @return a boolean describing whether or not the user 
	 * has entered a passphrase before.
	 */
	private boolean userHasPassphrase() {
		return this.getStoredPassphraseHash() != null;
	}

	/**
	 * Take appropriate action when the user enters their passphrase.
	 * If it is the first time, we store the new passphrase hash.
	 */
	@Override
	public void handlePassphraseEnter(int requestCode, byte[] inputPassphrase) {
		byte[] inputHash = OWUtils.Sha256Hash(inputPassphrase);
		if (this.userHasPassphrase()) {
			byte[] storedHash = this.getStoredPassphraseHash();
			if (storedHash != null && inputHash != null) {
				if (Arrays.equals(storedHash, inputHash)) {
					this.startFragmentFromButtonTag();
				} else {
					OWSimpleAlertDialog alertUserDialog = new OWSimpleAlertDialog();
					alertUserDialog.setTargetFragment(this, 
							RequestCodes.ALERT_USER_DIALOG);
					// Set negative text to null to not have negative button
					alertUserDialog.setupDialog("OneWallet", 
							"Passphrases don't match\n" + 
									Arrays.toString(inputHash) + "\n" + 
									Arrays.toString(storedHash), null, "OK", null);
					alertUserDialog.show(
							this.getChildFragmentManager(), "wrong_passphrase");
				}
			} else {
				ZLog.log("Error, something was null that shouldn't have been.");
			}
		} else {
			SharedPreferences prefs = getActivity().getSharedPreferences(
					PASSPHRASE_KEY, Context.MODE_PRIVATE);
			Editor editor = prefs.edit();
			editor.putString(
					OWHomeFragment.this.PASSPHRASE_KEY, 
					OWUtils.binaryToHexString(inputHash));
			editor.commit();
			this.startFragmentFromButtonTag();
		}
	}

	@Override
	public void handleNeutral(int requestCode) {
		switch(requestCode) {
		case RequestCodes.ALERT_USER_DIALOG:
			// Things for alert dialogs go here.
			break;
		case RequestCodes.GET_PASSPHRASE_DIALOG:
			// Things for get passphrase dialogs go here.
			break;
		default:
			break;
		}
	}

	@Override
	public void handleNegative(int requestCode) {
		switch(requestCode) {
		case RequestCodes.ALERT_USER_DIALOG:
			// Things for alert dialogs go here.
			break;
		case RequestCodes.GET_PASSPHRASE_DIALOG:
			// Things for get passphrase dialogs go here.
			break;
		default:
			break;
		}
	}

}
