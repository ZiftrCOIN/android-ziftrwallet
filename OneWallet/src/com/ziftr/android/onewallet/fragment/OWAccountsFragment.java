package com.ziftr.android.onewallet.fragment;

import java.util.Arrays;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.google.zxing.client.android.CaptureActivity;
import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.dialog.OWPassphraseDialog;
import com.ziftr.android.onewallet.dialog.OWResetPassphraseDialog;
import com.ziftr.android.onewallet.dialog.OWSimpleAlertDialog;
import com.ziftr.android.onewallet.dialog.handlers.OWNeutralDialogHandler;
import com.ziftr.android.onewallet.dialog.handlers.OWPassphraseDialogHandler;
import com.ziftr.android.onewallet.dialog.handlers.OWResetPassphraseDialogHandler;
import com.ziftr.android.onewallet.util.OWUtils;
import com.ziftr.android.onewallet.util.RequestCodes;
import com.ziftr.android.onewallet.util.ZLog;

/**
 * The OWMainActivity starts this fragment. This fragment is 
 * associated with a few buttons where the user can choose what
 * kind of wallet they want to open. 
 */
public class OWAccountsFragment extends OWSectionFragment implements 
OWPassphraseDialogHandler, OWNeutralDialogHandler, OWResetPassphraseDialogHandler {

	/** The view container for this fragment. */
	protected View rootView;

	/** The most recently clicked button. */
	private View clickedButton;

	/** The key for getting the passphrase hash from the preferences. */
	private final String PASSPHRASE_KEY = "ow_passphrase_key_1";

	/** The context for this fragment. */
	private FragmentActivity myContext;

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
		this.rootView = inflater.inflate(
				R.layout.section_accounts_layout, container, false);

		// For the bitcoinWalletButton
		View bitcoinWalletButton = rootView.findViewById(R.id.buttonBitcoinWallet);
		// Add what kind of fragment this button should create as tag
		bitcoinWalletButton.setTag(OWBitcoinWalletFragment.class); 
		// Set the listener for the clicks
		bitcoinWalletButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				OWAccountsFragment.this.clickedButton = v;

				OWPassphraseDialog passphraseDialog = new OWPassphraseDialog();

				// Set the target fragment
				passphraseDialog.setTargetFragment(OWAccountsFragment.this, 
						RequestCodes.GET_PASSPHRASE_DIALOG);

				String message = null;
				if (OWAccountsFragment.this.userHasPassphrase()) {
					message = "Please input your passphrase. ";
				} else {
					message = "Please input a passphrase. This will be used "
							+ "for securing all your wallets.";
				}
				passphraseDialog.setupDialog("OneWallet", message, 
						"Continue", null, "Cancel");
				passphraseDialog.show(OWAccountsFragment.this.getFragmentManager(), 
						"open_bitcoin_wallet");
			}
		});

		// For the getQRCodeButton
		View resetPassphraseButton = rootView.findViewById(R.id.resetPassphraseButton);
		// Set the listener for the clicks
		resetPassphraseButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (OWAccountsFragment.this.userHasPassphrase()) {
					OWResetPassphraseDialog passphraseDialog = 
							new OWResetPassphraseDialog();

					// Set the target fragment
					passphraseDialog.setTargetFragment(OWAccountsFragment.this, 
							RequestCodes.RESET_PASSPHRASE_DIALOG);
					passphraseDialog.setupDialog("OneWallet", null, 
							"Continue", null, "Cancel");
					passphraseDialog.show(OWAccountsFragment.this.getFragmentManager(), 
							"scan_qr");
				} else {
					// Make a new alert dialog
					OWSimpleAlertDialog alertUserDialog = new OWSimpleAlertDialog();
					alertUserDialog.setTargetFragment(OWAccountsFragment.this, 
							RequestCodes.ALERT_USER_DIALOG);
					// Set up the dialog with message and other info
					alertUserDialog.setupDialog("OneWallet", 
							"You must set a passphrase before you can "
									+ "reset your passphrase.", null, "OK", null);
					// Pop up the dialog
					alertUserDialog.show(OWAccountsFragment.this.getFragmentManager(), 
							"no_passphrase_currently_set_alert_dialog");
				}
			}
		});

		// For the getQRCodeButton
		View getQRCodeButton = rootView.findViewById(R.id.getQRCodeButton);
		// Set the listener for the clicks
		getQRCodeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(
						OWAccountsFragment.this.getActivity(), CaptureActivity.class);
				intent.setAction("com.google.zxing.client.android.SCAN");
				// for Regular bar code, its ÒPRODUCT_MODEÓ instead of ÒQR_CODE_MODEÓ
				intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
				intent.putExtra("SAVE_HISTORY", false);
				startActivityForResult(intent, RequestCodes.SCAN_QR_CODE);
			}
		});

		// Return the view which was inflated
		return rootView;
	}

	/**
	 * Whenever this is fragment is attached to an activity 
	 * we 
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.myContext = (FragmentActivity) activity;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (data != null && data.hasExtra("SCAN_RESULT") && 
				requestCode == RequestCodes.SCAN_QR_CODE) {
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
		} else {
			ZLog.log("Error: not everything was set correctly to scan QR codes. ");
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

			this.myContext.getSupportFragmentManager().beginTransaction(
					).replace(R.id.oneWalletBaseFragmentHolder, fragment, tagString
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

	private boolean inputHashMatchesStoredHash(byte[] inputHash) {
		byte[] storedHash = this.getStoredPassphraseHash();
		if (storedHash != null && inputHash != null) {
			return Arrays.equals(storedHash, inputHash); 
		} else {
			ZLog.log("Error, something was null that shouldn't have been.");
			return false;
		}
	}

	/**
	 * Sets the stored passphrase hash to be the specified
	 * hash.
	 * 
	 * @param inputHash - The new passphrase's Sha256 hash
	 */
	private void setPassphraseHash(byte[] inputHash) {
		SharedPreferences prefs = getActivity().getSharedPreferences(
				PASSPHRASE_KEY, Context.MODE_PRIVATE);
		Editor editor = prefs.edit();
		editor.putString(
				OWAccountsFragment.this.PASSPHRASE_KEY, 
				OWUtils.binaryToHexString(inputHash));
		editor.commit();
	}

	/**
	 * Pops up a dialog to give the user some information. 
	 * 
	 * @param message
	 * @param tag
	 */
	private void alertUser(String message, String tag) {
		OWSimpleAlertDialog alertUserDialog = new OWSimpleAlertDialog();
		alertUserDialog.setTargetFragment(this, 
				RequestCodes.ALERT_USER_DIALOG);
		// Set negative text to null to not have negative button
		alertUserDialog.setupDialog("OneWallet", message, null, "OK", null);
		alertUserDialog.show(
				this.getFragmentManager(), tag);
	}

	/**
	 * Take appropriate action when the user enters their passphrase.
	 * If it is the first time, we store the new passphrase hash.
	 */
	@Override
	public void handlePassphrasePositive(int requestCode, byte[] inputPassphrase) {
		byte[] inputHash = OWUtils.Sha256Hash(inputPassphrase);
		if (this.userHasPassphrase()) {
			if (this.inputHashMatchesStoredHash(inputHash)) {
				this.startFragmentFromButtonTag();
			} else {
				this.alertUser("Error: Passphrases don't match. ", "wrong_passphrase");
			}
		} else {
			this.setPassphraseHash(inputHash);
			this.startFragmentFromButtonTag();
		}
	}

	@Override
	public void handleResetPassphrasePositive(int requestCode,
			byte[] oldPassphrase, byte[] newPassphrase, byte[] confirmPassphrase) {
		// Can assume that there was a previously entered passphrase because 
		// of the way the dialog was set up. 

		if (Arrays.equals(newPassphrase, confirmPassphrase)) {
			byte[] inputHash = OWUtils.Sha256Hash(oldPassphrase);
			if (this.inputHashMatchesStoredHash(inputHash)) {
				// If the old matches, set the new passphrase hash
				this.setPassphraseHash(OWUtils.Sha256Hash(newPassphrase));
			} else {
				// If don't match, tell user. 
				this.alertUser("The passphrase you entered doesn't match your "
						+ "previous passphrase. Your previous passphrase is "
						+ "still in place.", "passphrases_dont_match");
			}
		} else {
			this.alertUser("The passphrases you entered don't match. Your previous "
					+ "passphrase is still in place.", "wrong_re-enter_passphrase");
		}
	}

	@Override
	public void handleNeutral(int requestCode) {
		switch(requestCode) {
		case RequestCodes.ALERT_USER_DIALOG:
			// Things for alert dialogs go here.
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
		case RequestCodes.RESET_PASSPHRASE_DIALOG:
			// Things for reset passphrase dialogs go here
			break;
		default:
			break;
		}
	}

}
