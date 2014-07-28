package com.ziftr.android.onewallet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v7.app.ActionBarActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;

import com.ziftr.android.onewallet.dialog.OWNewCurrencyDialog;
import com.ziftr.android.onewallet.dialog.OWSimpleAlertDialog;
import com.ziftr.android.onewallet.dialog.OWValidatePassphraseDialog;
import com.ziftr.android.onewallet.dialog.handlers.OWNeutralDialogHandler;
import com.ziftr.android.onewallet.dialog.handlers.OWNewCurrencyDialogHandler;
import com.ziftr.android.onewallet.dialog.handlers.OWResetPassphraseDialogHandler;
import com.ziftr.android.onewallet.dialog.handlers.OWValidatePassphraseDialogHandler;
import com.ziftr.android.onewallet.fragment.OWAboutFragment;
import com.ziftr.android.onewallet.fragment.OWContactFragment;
import com.ziftr.android.onewallet.fragment.OWExchangeFragment;
import com.ziftr.android.onewallet.fragment.OWSettingsFragment;
import com.ziftr.android.onewallet.fragment.accounts.OWAccountsFragment;
import com.ziftr.android.onewallet.fragment.accounts.OWBitcoinTestnetWalletFragment;
import com.ziftr.android.onewallet.fragment.accounts.OWWalletFragment;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWRequestCodes;
import com.ziftr.android.onewallet.util.OWUtils;
import com.ziftr.android.onewallet.util.ZLog;

/**
 * This is the main activity of the OneWallet application. It handles
 * the menu drawer and the switching between the different fragments 
 * depending on which task the user selects.
 */
public class OWMainFragmentActivity extends ActionBarActivity implements DrawerListener, OWNewCurrencyDialogHandler, OWValidatePassphraseDialogHandler, OWNeutralDialogHandler, OWResetPassphraseDialogHandler {

	/*
	--- TODO list for the OneWallet project ---

	X 1. Ability to generate new address upon user request
	and be able to turn that into a QR code

	o 2. Get transaction history for all addresses in wallet and
	be able to display them.

	o 3. Making layouts necessary for the wireframe models.

	o 4. Organizing tasks that need to be done and appr. difficulty

	o 5. Learn how to use sqlite and use it to save data about transactions, such
	as the title of the transaction.

	X 6. ZiftrUtils and Zlog for static useful methods.
	ex. ZLog.log("aa", "b"); (get's exception message, as well)
	also autotags comments with class name and shuts itself off at
	launch time for release build.

	X 7. Move all dialog stuff into dialog package and make dialogs
	persistent.

	X 8. Get QR code scanning example working

	X 9. Get a list interface on top right corner. Move over to ActionBar
	instead of custom header layout.

	X 10. Get a reset working for the passphrase.

	X 11. Get Fragment switching working with selection in the drawer layout

	o 12. Put menu icon on the top left part of action bar rather than the right.
	To do this we need to make a custom action bar. Maybe just keep it on the right?
	In general, need to figure out how to make a more customizable action bar, 
	might just have to use custom actionbar layout.

	X 13. make it so that the content is moved over whenever we click the menu 
	button.

	X 14. Turn the EditText field in the passphrase dialog into an xml just like
	the reset passphrase dialog.

	o 15. Make the IO in loading/saving a wallet into an asynchrynous task
	so as to not block the UI. Some lagging can be seen now, such as when you just
	get into the accounts section and you try to rotate the screen horizontally.

	o 16. Make the backgrounds of the grid view in the new currency dialog
	reflect which item is currenly selected.

	o 17. Add a nicer display to the dialogs than the default.

	o 18. Redesign the send coins screen now that I know a little more about 
	android layouts.

	o 19. Make it so that the wallet is actually user passphrase protected

	o 20. Make the �add new currency' bar a footer of list rather than last element.
	Make sure that all list views have headers/footers where appropriate rather
	than just extra views in the xml files. 

	o 21. Don't show a pending transaction bar if there aren't any pending. 

	o 22. Add descriptions to each of the resources (like @string/ resources).

	o 23. Need to remember where we were when navigating back the accounts section

	o 24. Should only need to have passphrase when sending coins. 

	o 25. Need to have all the fragments start fragments by routing through
	the activity so that the activity can do more complicated things.

	TODO Maybe we don't want to set up all the open wallets right from the get go? 
	But we need to get the balance...

	 */

	/** The drawer layout menu. */
	private DrawerLayout menuDrawer;

	/** The menu for our app. */
	private Menu actionBarMenu;

	/** Use this key to save which section of the drawer menu is open. */
	private final String SELECTED_SECTION_KEY = "SELECTED_SECTION_KEY";

	/** The main view that all of the fragments will be stored in. */
	private View baseFragmentContainer;

	/** A float used in making the container move over when the drawer is moved. */
	private float lastTranslate = 0.0f;

	/** This object is responsible for all the wallets. */
	private OWWalletManager walletManager;

	private OWNewCurrencyDialogHandler newCurrencyDialogHandler;
	private OWValidatePassphraseDialogHandler validatePassphraseDialogHandler;

	/** The key for getting the passphrase hash from the preferences. */
	private final String PASSPHRASE_KEY = "ow_passphrase_key_1";

	/**
	 * This is an enum to differentiate between the different
	 * sections of the app. Each enum also holds specific information related
	 * to each of fragment types.
	 */
	private enum FragmentType {
		/** The enum to identify the accounts fragment of the app. */
		ACCOUNT_FRAGMENT_TYPE {
			@Override
			public Fragment getNewFragment() {
				return new OWAccountsFragment();
			}
		}, 
		/** The enum to identify the exchange fragment of the app. */
		EXCHANGE_FRAGMENT_TYPE {
			@Override
			public Fragment getNewFragment() {
				return new OWExchangeFragment();
			}
		}, 
		/** The enum to identify the settings fragment of the app. */
		SETTINGS_FRAGMENT_TYPE {
			@Override
			public Fragment getNewFragment() {
				return new OWSettingsFragment();
			}
		}, 
		/** The enum to identify the about fragment of the app. */
		ABOUT_FRAGMENT_TYPE {
			@Override
			public Fragment getNewFragment() {
				return new OWAboutFragment();
			}
		},
		/** The enum to identify the contact fragment of the app. */
		CONTACT_FRAGMENT_TYPE {
			@Override
			public Fragment getNewFragment() {
				return new OWContactFragment();
			}
		};

		/** The drawer menu view associated with this section of the app. */
		private View drawerMenuView;

		/**
		 * @return a new fragment of the type specified.
		 */
		public abstract Fragment getNewFragment();

		/**
		 * @return the drawerMenuView
		 */
		public View getDrawerMenuView() {
			return drawerMenuView;
		}

		/**
		 * @param drawerMenuView the drawerMenuView to set
		 */
		public void setDrawerMenuView(View drawerMenuView) {
			this.drawerMenuView = drawerMenuView;
		}
	};

	/**
	 * Loads up the views and starts the OWHomeFragment 
	 * if necessary. 
	 * 
	 * @param savedInstanceState - The stored bundle
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Everything is held within this main activity layout
		this.setContentView(R.layout.activity_main);

		// Recreate wallet manager
		this.walletManager = new OWWalletManager(this);

		// Set up the drawer and the menu button
		this.initializeDrawerLayout();

		// Make sure the base fragment view is initialized
		this.initizalizeBaseFragmentView(savedInstanceState);
	}

	/**
	 * Create the options menu.
	 * Updates the icon appropriately.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		this.getMenuInflater().inflate(R.menu.ow_action_bar_menu, menu);
		this.actionBarMenu = menu;

		// Make sure the icon matches the current open/close state
		this.setActionBarMenuIcon(this.drawerMenuIsOpen());
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * Here we save save which part of the app is currently open in
	 * the drawer.
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		// Save which part of the app is currently open.
		outState.putString(this.SELECTED_SECTION_KEY, 
				this.getCurrentlySelectedDrawerMenuOption());
	}

	/**
	 * When the back button is pressed from this activity we
	 * go to the accounts fragment if we are somewhere else, and from
	 * the accounts fragment we exit the app. 
	 */
	@Override
	public void onBackPressed() {
		String curSelected = getCurrentlySelectedDrawerMenuOption();
		if (curSelected == null) {
			ZLog.log("No selected section... Why did this happen?");
			super.onBackPressed();
		} else if (FragmentType.ACCOUNT_FRAGMENT_TYPE.toString().equals(curSelected)) {
			super.onBackPressed();
		} else {
			OWMainFragmentActivity.this.showFragmentFromType(
					FragmentType.ACCOUNT_FRAGMENT_TYPE);
		}
	}

	/**
	 * Here we need to close all the wallets. 
	 */
	@Override
	protected void onDestroy() {
		this.walletManager.closeAllSetupWallets();
		super.onDestroy();
	}

	/**
	 * Starts a new fragment in the main layout space depending
	 * on which fragment FragmentType is passed in. 
	 * 
	 * @param fragmentType - The identifier for which type of fragment to start.
	 */
	private void showFragmentFromType(FragmentType fragmentType) {
		// Go through menu options and select the right one, note  
		// that not all menu options can be selected
		if (fragmentType.getDrawerMenuView() != null) {
			this.selectSingleDrawerMenuOption(fragmentType.getDrawerMenuView());
		} else {
			ZLog.log("The drawer menu was null, can't select... shouldn't happen2");
		}

		// The tag for fragments of this fragmentType
		String tag = fragmentType.toString();

		// Make the new fragment of the correct type
		Fragment fragToShow = this.getSupportFragmentManager().findFragmentByTag(tag);
		if (fragToShow == null) {
			// If the fragment doesn't exist yet, make a new one
			fragToShow = fragmentType.getNewFragment(); 
		}

		this.showFragment(fragToShow, tag, R.id.oneWalletBaseFragmentHolder);
	}

	/**
	 * 
	 * @param fragmentType - 
	 * @param tag - The tag of the new fragment.
	 * @param resId - The view id to show the new fragment in.
	 */
	private void showFragment(Fragment fragToShow, String tag, int resId) {
		// The transaction that will take place to show the new fragment
		FragmentTransaction transaction = 
				this.getSupportFragmentManager().beginTransaction();

		// TODO add animation to transaciton here

		if (fragToShow.isVisible()) {
			// If the fragment is already visible, no need to do anything
			return;
		}

		transaction.replace(R.id.oneWalletBaseFragmentHolder, fragToShow, tag);
		transaction.commit();
	}

	/**
	 * A convenience method to toggle the drawer menu position.
	 * Also sets the icons to a static resource while opening/closing
	 * to avoid extra swapping.
	 */
	private void toggleDrawerPosition() {
		MenuItem drawerMenuItem = this.actionBarMenu.findItem(R.id.switchTaskMenuButton);

		if (this.menuDrawer != null) {
			if (this.drawerMenuIsOpen()) {
				// Set the icon here to avoid extra swapping while 
				// drawer menu is closing
				drawerMenuItem.setIcon(R.drawable.menu_white_enabled);
				// If drawer menu is open, close it
				this.menuDrawer.closeDrawer(Gravity.LEFT);
			} else {
				// Set the icon here to avoid extra swapping while 
				// drawer menu is opening
				drawerMenuItem.setIcon(R.drawable.menu_white_disabled);
				// If drawer menu is closed, open it
				this.menuDrawer.openDrawer(Gravity.LEFT);
			}
		}

	}

	/**
	 * Should be called after the view is initialized only.
	 * Returns a list of the views that are in the drawer menu.
	 * 
	 * @return a list of the views that are in the drawer menu.
	 */
	private List<View> getDrawerMenuItems() {
		// Make a new list
		List<View> selectionItems = new ArrayList<View>();

		// Add all the selection views to it
		selectionItems.add(this.findViewById(R.id.menuDrawerAccountsLayout));
		selectionItems.add(this.findViewById(R.id.menuDrawerExchangeLayout));
		selectionItems.add(this.findViewById(R.id.menuDrawerSettingsLayout));
		selectionItems.add(this.findViewById(R.id.menuDrawerAboutLayout));
		selectionItems.add(this.findViewById(R.id.menuDrawerContactLayout));

		// return the result
		return selectionItems;
	}

	/**
	 * Does all the necessary tasks to set up the drawer layout.
	 * This incudes setting up the menu button so that it opens and
	 * closes the drawer layout appropriately.
	 * 
	 * @param savedInstanceState - The saved instance state 
	 */
	private void initializeDrawerLayout() {
		this.menuDrawer = 
				(DrawerLayout) this.findViewById(R.id.oneWalletBaseDrawer);

		if (this.menuDrawer != null) {
			this.menuDrawer.setDrawerListener(this);

			// Set up for accounts section
			View accountsMenuButton = 
					this.findViewById(R.id.menuDrawerAccountsLayout);
			FragmentType.ACCOUNT_FRAGMENT_TYPE.setDrawerMenuView(
					accountsMenuButton);
			accountsMenuButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View clickedView) {
					OWMainFragmentActivity.this.onAnyDrawerMenuItemClicked(clickedView);
					OWMainFragmentActivity.this.showFragmentFromType(
							FragmentType.ACCOUNT_FRAGMENT_TYPE);
				}
			});

			// Set up for exchange section
			View exchangeMenuButton = 
					this.findViewById(R.id.menuDrawerExchangeLayout);
			FragmentType.EXCHANGE_FRAGMENT_TYPE.setDrawerMenuView(
					exchangeMenuButton);
			exchangeMenuButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View clickedView) {
					OWMainFragmentActivity.this.onAnyDrawerMenuItemClicked(clickedView);
					OWMainFragmentActivity.this.showFragmentFromType(
							FragmentType.EXCHANGE_FRAGMENT_TYPE);
				}
			});

			// Set up for settings section
			View settingsMenuButton = 
					this.findViewById(R.id.menuDrawerSettingsLayout);
			FragmentType.SETTINGS_FRAGMENT_TYPE.setDrawerMenuView(
					settingsMenuButton);
			settingsMenuButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View clickedView) {
					OWMainFragmentActivity.this.onAnyDrawerMenuItemClicked(clickedView);
					OWMainFragmentActivity.this.showFragmentFromType(
							FragmentType.SETTINGS_FRAGMENT_TYPE);
				}
			});

			// Set up for about section
			View aboutMenuButton = 
					this.findViewById(R.id.menuDrawerAboutLayout);
			FragmentType.ABOUT_FRAGMENT_TYPE.setDrawerMenuView(
					aboutMenuButton);
			aboutMenuButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View clickedView) {
					OWMainFragmentActivity.this.onAnyDrawerMenuItemClicked(clickedView);
					OWMainFragmentActivity.this.showFragmentFromType(
							FragmentType.ABOUT_FRAGMENT_TYPE);
				}
			});

			// Set up for contact section
			View contactMenuButton = 
					this.findViewById(R.id.menuDrawerContactLayout);
			FragmentType.CONTACT_FRAGMENT_TYPE.setDrawerMenuView(
					contactMenuButton);
			contactMenuButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View clickedView) {
					OWMainFragmentActivity.this.onAnyDrawerMenuItemClicked(clickedView);
					OWMainFragmentActivity.this.showFragmentFromType(
							FragmentType.CONTACT_FRAGMENT_TYPE);
				}
			});

		} else {
			ZLog.log("drawerMenu was null. ?");
		}

	}

	/**
	 * Initializes the drawer layout. Sets up some view fields
	 * and opens the right fragment if the bundle has extra information
	 * in it.
	 */
	private void initizalizeBaseFragmentView(Bundle savedInstanceState) {
		// Set the base fragment container
		this.baseFragmentContainer = this.findViewById(R.id.oneWalletBaseFragmentHolder);

		// To make sure the content doesn't go behind the drawer menu
		this.baseFragmentContainer.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						// At this point the layout is complete and the 
						// dimensions of the view and any child views are known.
						float offset;
						if (drawerMenuIsOpen()) {
							offset = menuDrawer.findViewById(
									R.id.menuDrawerScrollView).getWidth();
						} else {
							offset = 0;
						}
						baseFragmentContainer.setTranslationX(offset);
					}
				});

		// If the app has just been launched (so the fragment
		// doesn't exist yet), we create the main fragment.
		if (savedInstanceState == null) {
			this.showFragmentFromType(FragmentType.ACCOUNT_FRAGMENT_TYPE);
		} else {
			// Here we must make sure that the drawer menu is still
			// showing which section of the app is currently open.
			String prevSelectedSectionString = 
					savedInstanceState.getString(this.SELECTED_SECTION_KEY);
			if (prevSelectedSectionString != null) {
				FragmentType fragmentType = 
						FragmentType.valueOf(prevSelectedSectionString);
				if (fragmentType.getDrawerMenuView() != null) {
					this.selectSingleDrawerMenuOption(
							fragmentType.getDrawerMenuView());
				} else {
					ZLog.log("The drawer menu was null, "
							+ "can't select... shouldn't happen1");
				}
			}
		}
	}

	/**
	 * Should be called whenever any of the menu items in the
	 * drawer menu are selected. Basically just contains code 
	 * common to all of the tasks so that we can avoid code
	 * duplication. 
	 * 
	 * @param selectedView The view selected.
	 */
	private void onAnyDrawerMenuItemClicked(View selectedView) {
		// Select the view and deselect all the other views
		this.selectSingleDrawerMenuOption(selectedView);

		// Menu must have been open, so calling this will just 
		// result in a close of the menu.
		this.toggleDrawerPosition();;

	}

	/**
	 * Selects the drawer menu option given, deselects all
	 * the other drawer menu options.
	 * 
	 * @param selectedView - The view to select.
	 */
	private void selectSingleDrawerMenuOption(View selectedView) {
		// Deselect all the other views
		for (View selectionView : this.getDrawerMenuItems()) {
			selectionView.setSelected(false);
		}

		// Mark this one as selected to have green selector view
		// next to it in the drawer layout
		selectedView.setSelected(true);
	}

	/**
	 * Gets the string identifier of the currently selected
	 * drawer menu option.
	 * For example, if we are in the accounts section, this
	 * method will return "ACCOUNT_FRAGMENT_TYPE".
	 */
	private String getCurrentlySelectedDrawerMenuOption() {
		// Deselect all the other views
		for (FragmentType fragType : FragmentType.values()) {
			if (fragType.getDrawerMenuView() != null && 
					fragType.getDrawerMenuView().isSelected()) {
				return fragType.toString();
			}
		}
		return null;
	}

	/**
	 * Gives a boolean that describes whether or not the drawer menu is
	 * currenly open from the left side of the screen. 
	 * 
	 * @return a boolean that describes whether or not the drawer menu is
	 * currenly open
	 */
	private boolean drawerMenuIsOpen() {
		return this.menuDrawer.isDrawerOpen(Gravity.LEFT);
	}

	/**
	 * Called when the drawer menu item in the action bar is clicked.
	 * 
	 * This name is confusing because there is a drawer menu and then 
	 * there is the menu in the action bar that has a menu icon in it, 
	 * which is a clickable that opens the drawer menu.
	 */
	private void onActionBarMenuIconClicked() {
		this.toggleDrawerPosition();
	}

	/**
	 * Sets the icon to the appropriate statelist, depending on
	 * the given boolean to determine. Usually the boolean will
	 * be given by whether or not the drawer menu is open.
	 * 
	 * @param open - whether or not the drawer menu is open
	 */
	private void setActionBarMenuIcon(boolean open) {
		if (open) {
			this.actionBarMenu.findItem(R.id.switchTaskMenuButton
					).setIcon(R.drawable.icon_menu_statelist_reverse);
		} else {
			this.actionBarMenu.findItem(R.id.switchTaskMenuButton
					).setIcon(R.drawable.icon_menu_statelist);
		}
	}

	/**
	 * Sets the stored passphrase hash to be the specified
	 * hash.
	 * 
	 * @param inputHash - The new passphrase's Sha256 hash
	 */
	private void setPassphraseHash(byte[] inputHash) {
		SharedPreferences prefs = this.getSharedPreferences(
				PASSPHRASE_KEY, Context.MODE_PRIVATE);
		Editor editor = prefs.edit();
		editor.putString(
				this.PASSPHRASE_KEY, 
				OWUtils.binaryToHexString(inputHash));
		editor.commit();
	}

	/**
	 * Gets the stored hash of the users passphrase, if there is one.
	 * 
	 * @return the stored hash of the users passphrase, if there is one.
	 */
	private byte[] getStoredPassphraseHash() {
		// Get the preferences
		SharedPreferences prefs = this.getSharedPreferences(
				PASSPHRASE_KEY, Context.MODE_PRIVATE);
		// Get the passphrase hash
		String storedPassphrase = prefs.getString(this.PASSPHRASE_KEY, null);
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
	 * Causes a dialog to pop up asking the user for his/her
	 * passphrase. If the passphrase is successfully verified then
	 * a {@link OWWalletFragment} fragment is started. 
	 */
	public void startWalletAfterValidation(OWCoin.Type typeToStart) {
		if (typeToStart == OWCoin.Type.BTC_TEST) {
			// tag: OWCoin.Type.BTC_TEST.getShortTitle() + "_wallet_fragment"
			OWValidatePassphraseDialog passphraseDialog = 
					new OWValidatePassphraseDialog();

			Bundle b = new Bundle();
			b.putBoolean(OWCoin.Type.BTC_TEST.toString(), true);
			passphraseDialog.setArguments(b);

			// TODO make a create new passphrase dialog
			String message = "Please input your passphrase. ";

			passphraseDialog.setupDialog("OneWallet", message, 
					"Continue", null, "Cancel");
			passphraseDialog.show(this.getSupportFragmentManager(), 
					"open_bitcoin_wallet");
		}
	}

	/**
	 * Only works for BTC_TEST type right now. 
	 * 
	 * @param typeOfWalletToStart
	 */
	public void openWalletView(OWCoin.Type typeOfWalletToStart) {
		String tag = typeOfWalletToStart.getShortTitle() + "_wallet_fragment";
		Fragment fragToShow = null;
		if (typeOfWalletToStart == OWCoin.Type.BTC_TEST) {
			fragToShow = this.getSupportFragmentManager().findFragmentByTag(tag);
			if (fragToShow == null) {
				fragToShow = new OWBitcoinTestnetWalletFragment();
			}
		} else {
			return;
		}
		
		// If we did a tablet view this might be different. 
		this.showFragment(fragToShow, tag, R.id.oneWalletBaseFragmentHolder);
	}

	/**
	 * Creates and shows a dialog where the user picks which type of currency 
	 * to add to the wallet. 
	 * @param userWallets - The list of wallets that the user already has. This 
	 * list is used to determine which wallets to show in the dialog.
	 */
	public void showChooseNewCurrencyDialog(OWNewCurrencyDialogHandler handler, 
			List<OWCoin.Type> userWallets) {
		this.newCurrencyDialogHandler = handler;

		OWNewCurrencyDialog passphraseDialog = new OWNewCurrencyDialog();

		// Here we get all the coins that the user doesn't currently 
		// have a wallet for because those are the ones we put in the dialog.
		Set<OWCoin.Type> coinsNotInListCurrently = new HashSet<OWCoin.Type>(
				Arrays.asList(OWCoin.Type.values()));
		for (OWCoin.Type cListItem : userWallets) {
			coinsNotInListCurrently.remove(cListItem);
		}

		// We communicate which items to show through the use of a bundle.
		// All keys in the bundle for which a true boolean is put are ones
		// that the dialog should include in the selection grid.
		Bundle b = new Bundle();
		for (OWCoin.Type type : coinsNotInListCurrently) {
			b.putBoolean(type.toString(), true);
		}
		passphraseDialog.setArguments(b);

		String message = "Please select the currency that "
				+ "you would like to add.";
		passphraseDialog.setupDialog("OneWallet", message, 
				"Select", null, "Cancel");
		passphraseDialog.show(this.getSupportFragmentManager(), 
				"get_new_currency_dialog");
	}

	/**
	 * @return the walletManager
	 */
	public OWWalletManager getWalletManager() {
		return walletManager;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Placeholder for anything we might need to do with the results

		// Goes here first, then this calls the current fragment's on activity result
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the aciton bar items
		switch(item.getItemId()) {
		case R.id.switchTaskMenuButton:
			this.onActionBarMenuIconClicked();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onDrawerClosed(View arg0) {
		this.setActionBarMenuIcon(false);
	}

	@Override
	public void onDrawerOpened(View arg0) {
		this.setActionBarMenuIcon(true);
	}

	@Override
	public void onDrawerSlide(View drawerView, float slideOffset) {
		// Here we need to 
		float moveFactor = (drawerView.getWidth() * (slideOffset - this.lastTranslate));
		this.baseFragmentContainer.setTranslationX(moveFactor);
	}

	@Override
	public void onDrawerStateChanged(int newState) {
		// Nothing to do
	}

	/**
	 * Pops up a dialog to give the user some information. 
	 * 
	 * @param message
	 * @param tag
	 */
	public void alertUser(String message, String tag) {
		OWSimpleAlertDialog alertUserDialog = new OWSimpleAlertDialog();

		// Set negative text to null to not have negative button
		alertUserDialog.setupDialog("OneWallet", message, null, "OK", null);
		alertUserDialog.show(this.getSupportFragmentManager(), tag);
	}

	///////////// Passer methods /////////////

	@Override
	public void handleNewCurrencyPositive(int requestCode, OWCoin.Type id) {
		switch(requestCode) {
		case OWRequestCodes.NEW_CURRENCY_DIALOG:
			// Just need to pass the result to the Accounts fragment
			// (the newCurencyDialogHandler is the accounts fragment, most likely)
			this.newCurrencyDialogHandler.handleNewCurrencyPositive(requestCode, id);
			break;
		}
	}

	@Override
	public void handleNegative(int requestCode) {
		switch(requestCode) {
		case OWRequestCodes.NEW_CURRENCY_DIALOG:
			this.newCurrencyDialogHandler.handleNegative(requestCode);
			break;
		case OWRequestCodes.VALIDATE_PASSPHRASE_DIALOG:
			this.validatePassphraseDialogHandler.handleNegative(requestCode);
			break;
		}
	}

	@Override
	public void handlePassphrasePositive(int requestCode, 
			byte[] passphrase, Bundle info) {

		switch(requestCode) {
		case OWRequestCodes.VALIDATE_PASSPHRASE_DIALOG:
			if (this.validatePassphraseDialogHandler != null) {
				this.validatePassphraseDialogHandler.handlePassphrasePositive(
						requestCode, passphrase, info);
				break;
			}

			byte[] inputHash = OWUtils.Sha256Hash(passphrase);
			if (this.userHasPassphrase()) {
				if (this.inputHashMatchesStoredHash(inputHash)) {
					if (info != null) {
						OWCoin.Type typeOfWalletToStart = null;
						for (OWCoin.Type type : OWCoin.Type.values()) {
							if (info.getBoolean(type.toString())) {
								typeOfWalletToStart = type;
								break;
							}
						}
						this.openWalletView(typeOfWalletToStart);
					}
				} else {
					// TODO maybe make a OWTags.java class that keeps track of 
					// tags for different fragments? Could handle making the tags
					// from OWCoin.Types as well. 
					this.alertUser(
							"Error: Passphrases don't match. ", "wrong_passphrase");
				}
			} 
//			else {
//			this.setPassphraseHash(inputHash);
//			this.startFragmentToOpenOnCorrectPassphrase();
//		}
			break;
		}
	}

	/**
	 * By positive, this means that the user has selected 'Yes' or 'Okay' 
	 * rather than cancel or exiting the dialog. This is called when
	 * the user opens a reset passphrase dialog and 
	 */
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
			// Nothing to do 
			break;
		}
	}

}
