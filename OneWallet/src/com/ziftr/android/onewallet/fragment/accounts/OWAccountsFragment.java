package com.ziftr.android.onewallet.fragment.accounts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListView;

import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.dialog.OWPassphraseDialog;
import com.ziftr.android.onewallet.dialog.OWSimpleAlertDialog;
import com.ziftr.android.onewallet.dialog.handlers.OWNeutralDialogHandler;
import com.ziftr.android.onewallet.dialog.handlers.OWPassphraseDialogHandler;
import com.ziftr.android.onewallet.dialog.handlers.OWResetPassphraseDialogHandler;
import com.ziftr.android.onewallet.fragment.OWSectionFragment;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWFiat;
import com.ziftr.android.onewallet.util.OWRequestCodes;
import com.ziftr.android.onewallet.util.OWUtils;
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
	private Class<? extends Fragment> classToOpenFragmentFor;

	/** The key for getting the passphrase hash from the preferences. */
	private final String PASSPHRASE_KEY = "ow_passphrase_key_1";

	/** The context for this fragment. */
	private FragmentActivity myContext;

	/** The list view which shows a link to open each of the wallets/currency types. */
	private ListView currencyListView;
	private List<OWCurrencyListItem> userWallets;

	/** The grid view which lets users add new wallet types.*/
	private GridView newCurrencyGridView;
	/** The string to save the visibility of the grid on rotation changes. */
	private static final String NEW_CURRENCY_GRID_VISIBILITY = 
			"NEW_CURRENCY_GRID_VISIBILITY";



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

		// Initialize the list of user wallets that they can open
		this.initializeCurrencyListView();

		this.initializeNewCurrencyGridView(savedInstanceState);

		/*
		// For the bitcoinWalletButton
		View bitcoinWalletButton = rootView.findViewById(R.id.buttonBitcoinWallet);
		// Add what kind of fragment this button should create as tag
		bitcoinWalletButton.setTag(OWBitcoinWalletFragment.class); 
		// Set the listener for the clicks
		bitcoinWalletButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startBitcoinTestnetWallet();
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
							OWRequestCodes.RESET_PASSPHRASE_DIALOG);
					passphraseDialog.setupDialog("OneWallet", null, 
							"Continue", null, "Cancel");
					passphraseDialog.show(OWAccountsFragment.this.getFragmentManager(), 
							"scan_qr");
				} else {
					// Make a new alert dialog
					OWSimpleAlertDialog alertUserDialog = new OWSimpleAlertDialog();
					alertUserDialog.setTargetFragment(OWAccountsFragment.this, 
							OWRequestCodes.ALERT_USER_DIALOG);
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
				startActivityForResult(intent, OWRequestCodes.SCAN_QR_CODE);
			}
		});
		 */
		// Return the view which was inflated
		return rootView;
	}

	/**
	 * Whenever this is fragment is attached to an activity 
	 * we save a reference to the activity.
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
				requestCode == OWRequestCodes.SCAN_QR_CODE) {
			// Get the result of the QR scan
			String result = data.getStringExtra("SCAN_RESULT");

			OWSimpleAlertDialog alertUserDialog = new OWSimpleAlertDialog();
			alertUserDialog.setTargetFragment(this, 
					OWRequestCodes.ALERT_USER_DIALOG);
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

	@Override
	public void onSaveInstanceState(Bundle b) {
		if (this.newCurrencyGridView != null) {
			b.putString(NEW_CURRENCY_GRID_VISIBILITY, 
					String.valueOf(this.newCurrencyGridView.getVisibility()));
		}
	}

	/**
	 * @param savedInstanceState 
	 */
	private void initializeNewCurrencyGridView(Bundle savedInstanceState) {
		this.newCurrencyGridView = (GridView)
				this.rootView.findViewById(R.id.addNewCurrencyGridView);
		this.newCurrencyGridView.setAdapter(
				new OWNewCurrencyListAdapter(this.myContext, R.layout._coin_with_title));

		if (savedInstanceState != null && 
				savedInstanceState.getString(NEW_CURRENCY_GRID_VISIBILITY) != null) {
			try {
				// If the visibility was saved we bring it back here.
				// Note that I used a string to save the int because I needed to know
				// if it had been set or not. Default for getInt from a bundle is 
				// zero which is the same as View.Visible, so we wouldn't be able 
				// to tell if it had been set or not. 
				int visibility = Integer.parseInt(
						savedInstanceState.getString(NEW_CURRENCY_GRID_VISIBILITY));
				ZLog.log("Visibility: " + (visibility));
				ZLog.log("Visibility == Gone: " + (visibility == View.GONE));
				ZLog.log("Visibility == Visible: " + (visibility == View.VISIBLE));
				this.newCurrencyGridView.setVisibility(visibility);
			} catch (NumberFormatException nfe) {
				// Default, if no visibility was saved, is to be gone. Then the user
				// can click the new currency bar and open it up. 
				this.newCurrencyGridView.setVisibility(View.GONE);
			}
		} else {
			this.newCurrencyGridView.setVisibility(View.GONE);
		}

		this.newCurrencyGridView.setOnItemClickListener(new OnItemClickListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onItemClick(AdapterView<?> parent, View view, 
					int position, long id) {
				OWNewCurrencyListItem newItem = (OWNewCurrencyListItem) 
						parent.getItemAtPosition(position);

				for (OWCurrencyListItem itemInList : userWallets) {
					if (itemInList.getCoinId() == newItem.getCoinId()) {
						// Already in list
						return;
					}
				}
				
				int indexToAddAt = 0;
				if (!userWallets.isEmpty()) {
					// We order these alphabetically
					boolean notOutOfBounds = indexToAddAt < userWallets.size();
					while (notOutOfBounds && 
							userWallets.get(indexToAddAt).getCoinId().getTitle(
									).compareTo(newItem.getCoinId().getTitle()) > 0) {
						indexToAddAt++;
						notOutOfBounds = indexToAddAt < userWallets.size();
					}
				}
				
				// TODO export all of this to a method and see if we can 
				// get the constructor of OWCurrencyListItem to take very few things
				// i.e. put a lot of the info into the OWCoin.Types and then it
				// will be easy to store what types of wallets the user has in 
				// preferences. After getting value from preferences, can use the 
				// enum.valueOf to get back the correct enum and re-add to the list
				// when view is first initialzed in this method. 
				if (newItem.getCoinId() == OWCoin.Type.BTC) {
					userWallets.add(indexToAddAt, 
							new OWCurrencyListItem(R.drawable.logo_bitcoin,
									OWCoin.Type.BTC, OWFiat.Type.USD, 
									"620.00", "0.00000000", "0.00"));
				} else if (newItem.getCoinId() == OWCoin.Type.BTC_TEST) {
					userWallets.add(indexToAddAt, 
							new OWCurrencyListItem(R.drawable.logo_bitcoin, 
									OWCoin.Type.BTC_TEST, OWFiat.Type.USD, 
									"0.00", "0.00000000", "0.00"));
				} else if (newItem.getCoinId() == OWCoin.Type.DOGE) {
					userWallets.add(indexToAddAt, 
							new OWCurrencyListItem(R.drawable.logo_dogecoin,
									OWCoin.Type.DOGE, OWFiat.Type.USD, 
									"0.0023", "0.00000000", "0.00"));
				} else if (newItem.getCoinId() == OWCoin.Type.DOGE_TEST) {
					userWallets.add(indexToAddAt, 
							new OWCurrencyListItem(R.drawable.logo_dogecoin,
									OWCoin.Type.DOGE_TEST, OWFiat.Type.USD, 
									"0.00", "0.00000000", "0.00"));
				}
				
				((ArrayAdapter<OWCurrencyListItem>) 
						currencyListView.getAdapter()).notifyDataSetChanged();
			}
		});

		View newCurrencyBar = this.rootView.findViewById(R.id.addNewWalletBar);
		newCurrencyBar.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (newCurrencyGridView.getVisibility() == View.GONE) {
					newCurrencyGridView.setVisibility(View.VISIBLE);
				} else if (newCurrencyGridView.getVisibility() == View.VISIBLE) {
					newCurrencyGridView.setVisibility(View.GONE);
				}
			}
		});
	}

	/**
	 * 
	 */
	private void initializeCurrencyListView() {
		// Add a few wallets for testing, will need to get these
		// values somewhere eventually
		this.userWallets = new ArrayList<OWCurrencyListItem>();

		this.currencyListView = (ListView) 
				this.rootView.findViewById(R.id.listOfUserWallets);
		this.currencyListView.setAdapter(new OWCurrencyListAdapter(
				this.myContext, R.layout.accounts_single_currency, userWallets));

		this.currencyListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, 
					int position, long id) {
				OWCurrencyListItem item = (OWCurrencyListItem) 
						parent.getItemAtPosition(position);
				if (OWCoin.Type.BTC_TEST == item.getCoinId()) {
					// If we are using the test net network then we make sure the
					// user has a passphrase and 
					startBitcoinTestnetWallet();
				} 
				// TODO add other cases
			}
		});
	}

	private void startBitcoinTestnetWallet() {
		// TODO is there a better way to do this? 
		this.classToOpenFragmentFor = OWBitcoinTestnetWalletFragment.class;

		OWPassphraseDialog passphraseDialog = new OWPassphraseDialog();

		// Set the target fragment
		passphraseDialog.setTargetFragment(OWAccountsFragment.this, 
				OWRequestCodes.GET_PASSPHRASE_DIALOG);

		// TODO separate this into two dailos, enter passphrase dialog
		// and a create new passphrase dialog.
		String message = "Please input your passphrase. ";
		if (!OWAccountsFragment.this.userHasPassphrase()) {
			message += "This will be used for securing all your wallets.";
		}
		passphraseDialog.setupDialog("OneWallet", message, 
				"Continue", null, "Cancel");
		passphraseDialog.show(OWAccountsFragment.this.getFragmentManager(), 
				"open_bitcoin_wallet");
	}

	/**
	 * Uses the tag of the current clickedButton field to 
	 * make a new fragment and then transition to that fragment.
	 */
	private void startFragmentFromButtonTag() {
		try {
			Fragment fragment = this.classToOpenFragmentFor.newInstance();
			String tagString = this.classToOpenFragmentFor.getSimpleName();

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
				OWRequestCodes.ALERT_USER_DIALOG);
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
		case OWRequestCodes.ALERT_USER_DIALOG:
			// Things for alert dialogs go here.
			break;
		default:
			break;
		}
	}

	@Override
	public void handleNegative(int requestCode) {
		switch(requestCode) {
		case OWRequestCodes.ALERT_USER_DIALOG:
			// Things for alert dialogs go here.
			break;
		case OWRequestCodes.GET_PASSPHRASE_DIALOG:
			// Things for get passphrase dialogs go here.
			break;
		case OWRequestCodes.RESET_PASSPHRASE_DIALOG:
			// Things for reset passphrase dialogs go here
			break;
		default:
			break;
		}
	}

}
