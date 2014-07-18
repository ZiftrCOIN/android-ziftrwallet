package com.ziftr.android.onewallet.fragment.accounts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListView;

import com.ziftr.android.onewallet.OWMainFragmentActivity;
import com.ziftr.android.onewallet.OWWalletManager;
import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.dialog.OWPassphraseDialog;
import com.ziftr.android.onewallet.dialog.OWSimpleAlertDialog;
import com.ziftr.android.onewallet.dialog.handlers.OWNeutralDialogHandler;
import com.ziftr.android.onewallet.dialog.handlers.OWPassphraseDialogHandler;
import com.ziftr.android.onewallet.fragment.OWSectionFragment;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWFiat;
import com.ziftr.android.onewallet.util.OWRequestCodes;
import com.ziftr.android.onewallet.util.OWUtils;
import com.ziftr.android.onewallet.util.ZLog;

/**
 * The OWMainActivity starts this fragment. This fragment is 
 * associated with list view of user wallets and a bar at the bottom of the list
 * view which opens a dialog to add rows to the list view.  
 */
public class OWAccountsFragment extends OWSectionFragment implements 
OWPassphraseDialogHandler, OWNeutralDialogHandler {

	/** The view container for this fragment. */
	private View rootView;

	/** The most recently clicked button. */
	private OWWalletFragment fragmentToOpenAfterHandlePassphrase;

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
	private static final String NEW_CURRENCY_GRID_VISIBILITY_KEY = 
			"NEW_CURRENCY_GRID_VISIBILITY";

	private OWWalletManager walletManager;

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

		this.walletManager = ((OWMainFragmentActivity) getActivity()).getWalletManager();

		this.rootView = inflater.inflate(
				R.layout.section_accounts_layout, container, false);

		// Initialize the list of user wallets that they can open
		this.initializeCurrencyListView();

		this.initializeNewCurrencyGridView(savedInstanceState);

		/*

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

	/**
	 * We need to save the grid visibility. 
	 * TODO change to a dialog so that never need grid visibility.
	 */
	@Override
	public void onSaveInstanceState(Bundle b) {
		if (this.newCurrencyGridView != null) {
			b.putString(NEW_CURRENCY_GRID_VISIBILITY_KEY, 
					String.valueOf(this.newCurrencyGridView.getVisibility()));
		}
	}
	
	/**
	 * Creates the adapter for the grid view and sets up 
	 * the click listener for the grid view.
	 */
	private void initializeNewCurrencyGridView(Bundle savedInstanceState) {
		this.newCurrencyGridView = (GridView)
				this.rootView.findViewById(R.id.addNewCurrencyGridView);
		this.newCurrencyGridView.setAdapter(
				new OWNewCurrencyListAdapter(this.myContext, R.layout._coin_with_title));

		if (savedInstanceState != null && 
				savedInstanceState.getString(NEW_CURRENCY_GRID_VISIBILITY_KEY) != null) {
			try {
				// If the visibility was saved we bring it back here.
				// Note that I used a string to save the int because I needed to know
				// if it had been set or not. Default for getInt from a bundle is 
				// zero which is the same as View.Visible, so we wouldn't be able 
				// to tell if it had been set or not. 
				int visibility = Integer.parseInt(
						savedInstanceState.getString(NEW_CURRENCY_GRID_VISIBILITY_KEY));
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
			@Override
			public void onItemClick(AdapterView<?> parent, View view, 
					int position, long id) {
				OWNewCurrencyListItem newItem = (OWNewCurrencyListItem) 
						parent.getItemAtPosition(position);

				// Make sure we don't add the same item twice
				for (OWCurrencyListItem itemInList : userWallets) {
					if (itemInList.getCoinId() == newItem.getCoinId()) {
						// Already in list
						return;
					}
				}

				// Make sure it is added in alphabetical order
				int indexToAddAt = 
						getIndexToAlphabeticallyAddUserWallet(newItem.getCoinId());

				// TODO add this for all coin types eventually 
				// walletManager.setupWallet(newItem.getCoinId());
				// Right now we are just doing it for the testnet
				if (newItem.getCoinId() == OWCoin.Type.BTC_TEST) {
					// We can assume the wallet hasn't been set up yet
					// or we wouldn't have gotten here 
					walletManager.setupWallet(newItem.getCoinId());
				}
				userWallets.add(indexToAddAt, getItemForCoinType(newItem.getCoinId()));
				refreshListOfUserWallets();
			}

		});
		
	}

	/**
	 * A messy method which is just used to not have huge if else
	 * blocks elsewhere. Gets the item that we will add to the currency
	 * list when the user adds a new currency.
	 * 
	 * @param id - The type of coin to get a new {@link OWCurrencyListItem} for. 
	 * @return as above
	 */
	private OWCurrencyListItem getItemForCoinType(OWCoin.Type id) {
		// TODO need to get market values from some sort of an API
		//
		// TODO export all of this to a method and see if we can 
		// get the constructor of OWCurrencyListItem to take very few things
		int resId = R.layout.accounts_currency_list_single_item;
		if (id == OWCoin.Type.BTC) {
			return new OWCurrencyListItem(OWCoin.Type.BTC, OWFiat.Type.USD, 
					"620.00", "0.00000000", "0.00", resId);
		} else if (id == OWCoin.Type.BTC_TEST) {
			return new OWCurrencyListItem(OWCoin.Type.BTC_TEST, OWFiat.Type.USD, 
					"0.00", "0.00000000", "0.00", resId);
		} else if (id == OWCoin.Type.LTC) {
			return new OWCurrencyListItem(OWCoin.Type.LTC, OWFiat.Type.USD, 
					"6.70", "0.00000000", "0.00", resId);
		} else if (id == OWCoin.Type.LTC_TEST) {
			return new OWCurrencyListItem(OWCoin.Type.LTC_TEST, OWFiat.Type.USD, 
					"0.00", "0.00000000", "0.00", resId);
		} else if (id == OWCoin.Type.PPC) {
			return new OWCurrencyListItem(OWCoin.Type.PPC, OWFiat.Type.USD, 
					"1.40", "0.00000000", "0.00", resId);
		} else if (id == OWCoin.Type.PPC_TEST) {
			return new OWCurrencyListItem(OWCoin.Type.PPC_TEST, OWFiat.Type.USD, 
					"0.00", "0.00000000", "0.00", resId);
		} else if (id == OWCoin.Type.DOGE) {
			return new OWCurrencyListItem(OWCoin.Type.DOGE, OWFiat.Type.USD, 
					"0.0023", "0.00000000", "0.00", resId);
		} else if (id == OWCoin.Type.DOGE_TEST) {
			return new OWCurrencyListItem(OWCoin.Type.DOGE_TEST, OWFiat.Type.USD, 
					"0.00", "0.00000000", "0.00", resId);
		}  
		return null;
	}

	/**
	 * When we add an item to the list of currencies we make sure they are 
	 * alphabetized. This helps to get the index that new coin types
	 * should be added at. 
	 * 
	 * @param coinType - The coin type of the list element we are going 
	 * to be adding.
	 * @return as above
	 */
	private int getIndexToAlphabeticallyAddUserWallet(OWCoin.Type coinType) {
		int indexToAddAt = 0;
		if (!userWallets.isEmpty()) {
			// The minus one is because the add new bar is a list item but we
			// don't want it to count as one for alphabetizing.
			boolean notOutOfBounds = indexToAddAt < (userWallets.size()-1);
			while (notOutOfBounds && 
					userWallets.get(indexToAddAt).getCoinId().getLongTitle(
							).compareTo(coinType.getLongTitle()) > 0) {
				indexToAddAt++;
				notOutOfBounds = indexToAddAt < (userWallets.size()-1);
			}
		}
		return indexToAddAt;
	}

	/**
	 * Notifies the list view adapter that the data set has changed
	 * to initialize a redraw. 
	 */
	@SuppressWarnings("unchecked")
	private void refreshListOfUserWallets() {
		((ArrayAdapter<OWCurrencyListItem>) 
				this.currencyListView.getAdapter()).notifyDataSetChanged();
	}

	/**
	 * Sets up the data set, the adapter, and the onclick for 
	 * the list view of currencies that the user currently has a 
	 * wallet for.
	 */
	private void initializeCurrencyListView() {
		// Get the values from the manager and initialize the list view from them
		this.userWallets = new ArrayList<OWCurrencyListItem>();
		for (OWCoin.Type type : this.walletManager.getAllUsersWalletTypes()) {
			this.userWallets.add(this.getItemForCoinType(type));
		}
		// The bar at the bottom
		this.userWallets.add(new OWCurrencyListItem(
				null, null, null, null, null, R.layout.accounts_currency_list_add_new));
		
		this.currencyListView = (ListView) 
				this.rootView.findViewById(R.id.listOfUserWallets);
		this.currencyListView.setAdapter(
				new OWCurrencyListAdapter(this.myContext, userWallets));

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
				// TODO add other cases eventually
				
				// If we have clicked the add new currency bar then
				// we toggle the view
				// TODO change this to making a dialog.
				if (currencyListView.getAdapter().getItemViewType(
						position) == OWCurrencyListAdapter.footerType) {
					if (newCurrencyGridView.getVisibility() == View.GONE) {
						newCurrencyGridView.setVisibility(View.VISIBLE);
					} else if (newCurrencyGridView.getVisibility() == View.VISIBLE) {
						newCurrencyGridView.setVisibility(View.GONE);
					}
				}
			}
		});
		
	}

	/**
	 * Causes a dialog to pop up asking the user for his/her
	 * passphrase. If the passphrase is successfully verified then
	 * a {@link OWWalletFragment} fragment is started. 
	 */
	private void startBitcoinTestnetWallet() {
		// TODO is there a better way to do this? 
		this.fragmentToOpenAfterHandlePassphrase = (OWWalletFragment) this.getActivity(
				).getSupportFragmentManager().findFragmentByTag(
						OWCoin.Type.BTC_TEST.getShortTitle() + "_wallet_fragment");
		if (this.fragmentToOpenAfterHandlePassphrase == null) {
			this.fragmentToOpenAfterHandlePassphrase = 
					new OWBitcoinTestnetWalletFragment();
		}

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
	 * Starts the fragmentToOpenAfterHandlePassphrase fragment in
	 * the oneWalletBaseFragmentHolder.
	 */
	private void startFragmentToOpenOnCorrectPassphrase() {
		try {
			FragmentTransaction tx = 
					this.myContext.getSupportFragmentManager().beginTransaction();
			tx.replace(R.id.oneWalletBaseFragmentHolder, 
					this.fragmentToOpenAfterHandlePassphrase,
					this.fragmentToOpenAfterHandlePassphrase.getCoinId(
							).getShortTitle() + "_wallet_fragment");
			tx.addToBackStack(null);
			tx.commit();
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
	 * Tells if the given hash matches the stored passphrase
	 * hash.
	 * 
	 * @param inputHash - the hash to check agains
	 * @return as above
	 */
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
				this.startFragmentToOpenOnCorrectPassphrase();
			} else {
				// TODO maybe make a OWTags.java class that keeps track of 
				// tags for different fragments? Could handle making the tags
				// from OWCoin.Types as well. 
				this.alertUser("Error: Passphrases don't match. ", "wrong_passphrase");
			}
		} else {
			this.setPassphraseHash(inputHash);
			this.startFragmentToOpenOnCorrectPassphrase();
		}
	}

	/**
	 * By positive, this means that the user has selected 'Yes' or 'Okay' 
	 * rather than cancel or exiting the dialog. This is called when
	 * the user opens a reset passphrase dialog and 
	 */
	/*
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
	}*/

	/**
	 * Handles neutral dialog entries, like 'OK'. 
	 */
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

	/**
	 * Handles negative dialog entries, like 'Cancel' or hitting off of the screen.
	 */
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
