package com.ziftr.android.onewallet.fragment.accounts;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.bitcoin.core.Wallet.BalanceType;
import com.ziftr.android.onewallet.OWMainFragmentActivity;
import com.ziftr.android.onewallet.OWWalletManager;
import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.dialog.handlers.OWNewCurrencyDialogHandler;
import com.ziftr.android.onewallet.fragment.OWSectionFragment;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWFiat;
import com.ziftr.android.onewallet.util.OWRequestCodes;
import com.ziftr.android.onewallet.util.OWUtils;

/**
 * The OWMainActivity starts this fragment. This fragment is 
 * associated with list view of user wallets and a bar at the bottom of the list
 * view which opens a dialog to add rows to the list view.  
 */
public class OWAccountsFragment extends OWSectionFragment implements 
OWNewCurrencyDialogHandler {

	/** The view container for this fragment. */
	private View rootView;

	/** The context for this fragment. */
	private FragmentActivity mContext;

	/** The list view which shows a link to open each of the wallets/currency types. */
	private ListView currencyListView;
	/** The data set that the currencyListView uses. */
	private List<OWCurrencyListItem> userWallets;

	/** The wallet manager. This fragment works with */
	private OWWalletManager walletManager;
	
	private OWMainFragmentActivity mActivity;

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
		
		this.mActivity = (OWMainFragmentActivity) getActivity();
		
		this.walletManager = this.mActivity.getWalletManager();

		// Initialize the list of user wallets that they can open
		this.initializeCurrencyListView();

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
		this.mContext = (FragmentActivity) activity;
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

		// TODO see if we can get the constructor of OWCurrencyListItem 
		// to take very few things
		int resId = R.layout.accounts_currency_list_single_item;
		if (id == OWCoin.Type.BTC) {
			return new OWCurrencyListItem(OWCoin.Type.BTC, OWFiat.Type.USD, 
					"620.00", "0.00000000", "0.00", resId);
		} else if (id == OWCoin.Type.BTC_TEST) {
			String balance = OWUtils.bitcoinValueToFriendlyString(
					this.walletManager.getWallet(id).getBalance(BalanceType.AVAILABLE));
			return new OWCurrencyListItem(OWCoin.Type.BTC_TEST, OWFiat.Type.USD, 
					"0.00", balance, "0.00", resId);
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
		
		// Should never get here
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
							).compareTo(coinType.getLongTitle()) < 0) {
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
				new OWCurrencyListAdapter(this.mContext, userWallets));

		this.currencyListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, 
					int position, long id) {
				if (currencyListView.getAdapter().getItemViewType(
						position) == OWCurrencyListAdapter.footerType) {
					// If we have clicked the add new currency bar then
					// make the choose new currency dialog (as long as there are new
					// currencies to add
					
					// The minus one is because the list contains the add new 
					// currency bar
					if (userWallets.size()-1 < OWCoin.Type.values().length) {
						// TODO may want to change the list to just be
						// walletManager.getAllUsersWalletTypes(); but since
						// that wouldn't include temporarily added wallets we will
						// do this for now.
						List<OWCoin.Type> usersCurWallets = new ArrayList<OWCoin.Type>();
						for (OWCurrencyListItem newItem : userWallets) {
							usersCurWallets.add(newItem.getCoinId());
						}
						mActivity.showChooseNewCurrencyDialog(
								OWAccountsFragment.this, usersCurWallets);
					}
					
					// Return because in this case we don't need to 
					// actually start a wallet.  
					return;
				}
				
				OWCurrencyListItem item = (OWCurrencyListItem) 
						parent.getItemAtPosition(position);
				// If we are using the test net network then we make sure the
				// user has a passphrase and 
				mActivity.startWalletAfterValidation(item.getCoinId());
			}
		});
		
	}
	
	/**
	 * Handles negative dialog entries, like 'Cancel' or hitting off of the screen.
	 */
	@Override
	public void handleNegative(int requestCode) {
		switch(requestCode) {
		case OWRequestCodes.RESET_PASSPHRASE_DIALOG:
			// Things for reset passphrase dialogs go here
			break;
		default:
			break;
		}
	}

	@Override
	public void handleNewCurrencyPositive(int requestCode, OWCoin.Type newItem) {
		// TODO make sure that this view only has wallets
		// in it which the user do
		for (OWCurrencyListItem itemInList : userWallets) {
			if (itemInList.getCoinId() == newItem) {
				// Already in list, shouldn't ever get here though because
				// we only show currencies in the dialog which we don't have
				return;
			}
		}

		// Make sure it is added in alphabetical order
		int indexToAddAt = 
				getIndexToAlphabeticallyAddUserWallet(newItem);

		// TODO add this for all coin types eventually 
		// walletManager.setupWallet(newItem.getCoinId());
		// Right now we are just doing it for the testnet
		if (newItem == OWCoin.Type.BTC_TEST) {
			// We can assume the wallet hasn't been set up yet
			// or we wouldn't have gotten here 
			
			// TODO see java docs for this method. Is it okay to 
			// have this called here? 
			walletManager.setUpWallet(newItem);
		}
		
		userWallets.add(indexToAddAt, getItemForCoinType(newItem));
		refreshListOfUserWallets();
	}

}
