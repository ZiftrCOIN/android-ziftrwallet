package com.ziftr.android.onewallet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.FragmentManager;
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
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ziftr.android.onewallet.crypto.OWTransaction;
import com.ziftr.android.onewallet.dialog.OWDialogFragment;
import com.ziftr.android.onewallet.dialog.OWSimpleAlertDialog;
import com.ziftr.android.onewallet.dialog.OWValidatePassphraseDialog;
import com.ziftr.android.onewallet.dialog.handlers.OWNeutralDialogHandler;
import com.ziftr.android.onewallet.dialog.handlers.OWResetPassphraseDialogHandler;
import com.ziftr.android.onewallet.dialog.handlers.OWValidatePassphraseDialogHandler;
import com.ziftr.android.onewallet.fragment.OWAboutFragment;
import com.ziftr.android.onewallet.fragment.OWContactFragment;
import com.ziftr.android.onewallet.fragment.OWExchangeFragment;
import com.ziftr.android.onewallet.fragment.OWSettingsFragment;
import com.ziftr.android.onewallet.fragment.OWWelcomeFragment;
import com.ziftr.android.onewallet.fragment.accounts.OWAccountsFragment;
import com.ziftr.android.onewallet.fragment.accounts.OWNewCurrencyFragment;
import com.ziftr.android.onewallet.fragment.accounts.OWReceiveCoinsFragment;
import com.ziftr.android.onewallet.fragment.accounts.OWSearchableListAdapter;
import com.ziftr.android.onewallet.fragment.accounts.OWSearchableListItem;
import com.ziftr.android.onewallet.fragment.accounts.OWSendCoinsFragment;
import com.ziftr.android.onewallet.fragment.accounts.OWTransactionDetailsFragment;
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
public class OWMainFragmentActivity extends ActionBarActivity implements DrawerListener, OWValidatePassphraseDialogHandler, OWNeutralDialogHandler, OWResetPassphraseDialogHandler {

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

	X 12. Put menu icon on the top left part of action bar rather than the right.
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

	X 16. Make the backgrounds of the grid view in the new currency dialog
	reflect which item is currenly selected.

	X 17. Add a nicer display to the dialogs than the default.

	o 18. Redesign the send coins screen now that I know a little more about 
	android layouts.

	o 19. Make it so that the wallet is actually user passphrase protected. Right
	now it just won't let you in unless you know the passphrase. We want to actually
	encrypt the wallet.

	o 20. Make the Ôadd new currency' bar a footer of list rather than last element.
	Make sure that all list views have headers/footers where appropriate rather
	than just extra views in the xml files. 

	X 21. Don't show a pending transaction bar if there aren't any pending. 

	o 22. Add descriptions to each of the resources (like @string/ resources).

	X 23. Need to remember where we were when navigating back the accounts section

	o 24. Should only need to have passphrase when sending coins.

	X 25. Need to have all the fragments start fragments by routing through
	the activity so that the activity can do more complicated things.

	o 26. BUG: sending 0 testnet coins to valid address throws IllegalStateException (actually might just be bitcoinj)

	TODO Maybe we don't want to set up all the open wallets right from the get go? 
	But we need to get the balance...
	This will be made easier with the API.

	 */

	/** The drawer layout menu. */
	private DrawerLayout menuDrawer;

	/** Use this key to save which section of the drawer menu is open. */
	private final String SELECTED_SECTION_KEY = "SELECTED_SECTION_KEY";

	/** The main view that all of the fragments will be stored in. */
	private View baseFragmentContainer;

	/** A float used in making the container move over when the drawer is moved. */
	private float lastTranslate = 0.0f;

	/** This object is responsible for all the wallets. */
	private OWWalletManager walletManager;

	/** The key for getting the passphrase hash from the preferences. */
	private final String PASSPHRASE_KEY = "ow_passphrase_key_1";

	/** Boolean determining if a dialog is shown, used to prevent overlapping dialogs */
	private boolean showingDialog = false;

	/** When the user navigates to different sections of the app, this */
	private OWCoin curSelectedCoinType;

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
		},
		/** The enum to identify the welcome fragment of the app. */
		WELCOME_FRAGMENT_TYPE {
			@Override
			public Fragment getNewFragment() {
				return new OWWelcomeFragment();
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
		
		// Get the saved cur selected coin type
		this.initializeCoinType(savedInstanceState);

		// Recreate wallet manager
		this.walletManager = OWWalletManager.getInstance(this);

		// Set up the drawer and the menu button
		this.initializeDrawerLayout();

		// Make sure the base fragment view is initialized
		this.initializeBaseFragmentContainer(savedInstanceState);

		// Make sure the action bar changes with what fragment we are in
		this.initializeActionBar();

		// Hook up the search bar to show the keyboard without messing up the view
		this.initializeSearchBarText();
	}

	/**
	 * Create the options menu.
	 * Updates the icon appropriately.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//setup actionbar menu button
		ImageView menuButton = (ImageView) this.findViewById(R.id.switchTaskMenuButton);
		menuButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onActionBarMenuIconClicked();		
			}

		});

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
		outState.putString(this.SELECTED_SECTION_KEY, this.getCurrentlySelectedDrawerMenuOption());
		if (this.getCurSelectedCoinType() != null) {
			outState.putString(OWCoin.TYPE_KEY, this.getCurSelectedCoinType().toString());
		}
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
			ZLog.log(this.getSupportFragmentManager().getBackStackEntryCount());
			super.onBackPressed();
		} else {
			//Select accounts in drawer since anywhere you hit back, you will end up in accounts
			selectSingleDrawerMenuOption(findViewById(R.id.menuDrawerAccountsLayout));
			if (!getSupportFragmentManager().popBackStackImmediate(FragmentType.ACCOUNT_FRAGMENT_TYPE.toString() + "_INNER", 0)) {
				OWMainFragmentActivity.this.showFragmentFromType(
						FragmentType.ACCOUNT_FRAGMENT_TYPE, true);
				//clear back stack
				this.getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			}
		}
	}


	/**
	 * Here we need to close all the wallets. 
	 */
	@Override
	protected void onDestroy() {
		// TODO how should these combined?
		this.walletManager.closeAllSetupWallets();
		OWWalletManager.closeInstance();
		super.onDestroy();
	}

	/**
	 * Starts a new fragment in the main layout space depending
	 * on which fragment FragmentType is passed in. 
	 * 
	 * @param fragmentType - The identifier for which type of fragment to start.
	 * @param addToBackStack - boolean determining if fragment should be added to backstack
	 */
	private void showFragmentFromType(FragmentType fragmentType, Boolean addToBackStack) {
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

		this.showFragment(fragToShow, tag, R.id.oneWalletBaseFragmentHolder, addToBackStack, fragmentType.toString());
	}

	/**
	 * 
	 * @param fragmentType - 
	 * @param tag - The tag of the new fragment.
	 * @param resId - The view id to show the new fragment in.
	 */
	public void showFragment(Fragment fragToShow, String tag, 
			int resId, boolean addToBackStack, String backStackTag) {
		// The transaction that will take place to show the new fragment
		FragmentTransaction transaction = 
				this.getSupportFragmentManager().beginTransaction();
		// TODO add animation to transaciton here

		if (fragToShow.isVisible()) {
			// If the fragment is already visible, no need to do anything
			return;
		}

		transaction.replace(resId, fragToShow, tag);
		if (addToBackStack) {
			ZLog.log(backStackTag);
			transaction.addToBackStack(backStackTag);
		}
		transaction.commit();
	}



	/**
	 * A convenience method to toggle the drawer menu position.
	 * Also sets the icons to a static resource while opening/closing
	 * to avoid extra swapping.
	 */
	@SuppressLint("RtlHardcoded")
	private void toggleDrawerPosition() {
		ImageView menuButton = (ImageView) this.findViewById(R.id.switchTaskMenuButton);


		if (this.menuDrawer != null) {
			if (this.drawerMenuIsOpen()) {
				// Set the icon here to avoid extra swapping while 
				// drawer menu is closing
				menuButton.setImageResource(R.drawable.menu_white_enabled);
				// If drawer menu is open, close it
				this.menuDrawer.closeDrawer(Gravity.LEFT);
			} else {
				// Set the icon here to avoid extra swapping while 
				// drawer menu is opening
				menuButton.setImageResource(R.drawable.menu_white_disabled);
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
		this.menuDrawer = (DrawerLayout) this.findViewById(R.id.oneWalletBaseDrawer);

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
					//If accounts is currently selected, go to main accounts page and clear backstack so if user hits back, app exits.
					if (FragmentType.ACCOUNT_FRAGMENT_TYPE.toString().equals(
							OWMainFragmentActivity.this.getCurrentlySelectedDrawerMenuOption())) {
						//clear back stack
						OWMainFragmentActivity.this.getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

						OWMainFragmentActivity.this.showFragmentFromType(
								FragmentType.ACCOUNT_FRAGMENT_TYPE, false);

						//Else accounts is not selected so resume previous accounts activity
					} else if (!getSupportFragmentManager().popBackStackImmediate(
							FragmentType.ACCOUNT_FRAGMENT_TYPE.toString() + "_INNER", 0)) {
						OWMainFragmentActivity.this.showFragmentFromType(
								FragmentType.ACCOUNT_FRAGMENT_TYPE, true);
						OWMainFragmentActivity.this.getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

					}
					OWMainFragmentActivity.this.onAnyDrawerMenuItemClicked(clickedView);
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
							FragmentType.EXCHANGE_FRAGMENT_TYPE, true);
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
							FragmentType.SETTINGS_FRAGMENT_TYPE, true);
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
							FragmentType.ABOUT_FRAGMENT_TYPE, true);
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
							FragmentType.CONTACT_FRAGMENT_TYPE, true);
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
	private void initializeCoinType(Bundle args) {
		if (args != null) {
			if (args.getString(OWCoin.TYPE_KEY) != null) {
				this.curSelectedCoinType = OWCoin.valueOf(args.getString(OWCoin.TYPE_KEY));
			}
		}
	}

	private void initializeBaseFragmentContainer(Bundle savedInstanceState) {
		// Set the base fragment container
		this.baseFragmentContainer = this.findViewById(R.id.oneWalletBaseHolder);

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
			this.showFragmentFromType(FragmentType.WELCOME_FRAGMENT_TYPE, false);
		} else {
			// Here we must make sure that the drawer menu is still
			// showing which section of the app is currently open.
			String prevSelectedSectionString = savedInstanceState.getString(this.SELECTED_SECTION_KEY);
			if (prevSelectedSectionString != null) {
				FragmentType fragmentType = FragmentType.valueOf(prevSelectedSectionString);
				if (fragmentType.getDrawerMenuView() != null) {
					this.selectSingleDrawerMenuOption(fragmentType.getDrawerMenuView());
				} else {
					ZLog.log("The drawer menu was null, "
							+ "can't select... shouldn't happen1");
				}
			}
		}
	}

	private void initializeActionBar() {
		// Set up actionbar
		ActionBar actionbar = this.getActionBar();
		actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		actionbar.setCustomView(R.layout._app_header_bar);

	}

	private void initializeSearchBarText() {
		//listener for when searchBar text has focus, shows keyboard if focused and removes keyboard if not
		final EditText searchEditText = (EditText) findViewById(R.id.searchBarEditText);
		searchEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				if (hasFocus) {
					imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
				} else {
					imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
				}
			}
		});
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
	@SuppressLint("RtlHardcoded")
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
		ImageView menuButton = (ImageView) this.findViewById(R.id.switchTaskMenuButton);
		if (open) {
			menuButton.setImageResource(R.drawable.icon_menu_statelist_reverse);
		} else {
			menuButton.setImageResource(R.drawable.icon_menu_statelist);
		}
	}

	/**
	 * Sets the stored passphrase hash to be the specified
	 * hash.
	 * 
	 * @param inputHash - The new passphrase's Sha256 hash
	 */
	public void setPassphraseHash(byte[] inputHash) {
		SharedPreferences prefs = this.getSharedPreferences(
				PASSPHRASE_KEY, Context.MODE_PRIVATE);
		Editor editor = prefs.edit();
		editor.putString(
				this.PASSPHRASE_KEY, 
				OWUtils.bytesToHexString(inputHash));
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
				null : OWUtils.hexStringToBytes(storedPassphrase);
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
	public boolean userHasPassphrase() {
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
	 * called after user enters password for creating new wallet 
	 * 
	 * @param bundle with OWCoin of wallet to add
	 */
	public void addNewCurrency(Bundle info) {
		OWCoin newItem = OWCoin.valueOf(info.getString(OWCoin.TYPE_KEY));
		// TODO we can probably get rid of this if it's slowing stuff down - unnecessary check 
		// Make sure that this view only has wallets
		// in it which the user do
		for (OWCoin type : this.walletManager.getAllUsersWalletTypes()) {
			if (type == newItem) {
				// Already in list, shouldn't ever get here though because
				// we only show currencies in the dialog which we don't have
				this.onBackPressed();
				return;
			}
		}
		if (newItem == OWCoin.BTC_TEST || newItem == OWCoin.BTC) {
			// We can assume the wallet hasn't been set up yet
			// or we wouldn't have gotten here 

			if (walletManager.setUpWallet(newItem)) {
				Toast.makeText(this, "Wallet Created!", Toast.LENGTH_LONG).show();
				this.onBackPressed();
			}
		}
	}

	/**
	 * Only works for enabled types right now. 
	 * 
	 * @param typeOfWalletToStart
	 */
	public void openWalletView(OWCoin typeOfWalletToStart) {
		this.setCurSelectedCoinType(typeOfWalletToStart);

		// Temporarily need to just work for enabled types only
		OWCoin t = null;
		for (OWCoin enabledType : OWWalletManager.enabledCoinTypes) {
			if (typeOfWalletToStart == enabledType) {
				t = enabledType;
			}
		}

		// Coin type is not enabled
		if (t == null) {
			return;
		}

		String tag = "wallet_fragment";
		Fragment fragToShow = this.getSupportFragmentManager().findFragmentByTag(tag);
		if (fragToShow == null) {
			fragToShow = new OWWalletFragment();
		}

		// If we did a tablet view this might be different. 
		this.showFragment(fragToShow, tag, R.id.oneWalletBaseFragmentHolder, true, FragmentType.ACCOUNT_FRAGMENT_TYPE.toString() + "_INNER");
	}

	/**
	 * A convenience method that we can use to start a dialog from this bundle.
	 * The bundle should contain one string OWCoin.___.toString() which 
	 * can be extracted by getting from the bundle with the key OWCoin.TYPE_KEY.
	 * 
	 * @param info - The bundle used to tell which wallet to open.
	 */
	public void openWalletViewFromBundle(Bundle info) {
		if (info != null) {
			this.openWalletView(OWCoin.valueOf(info.getString(OWCoin.TYPE_KEY)));
		}
	}

	/**
	 * Only works for enabled types right now. 
	 * 
	 * @param typeOfWalletToStart
	 */
	public void openReceiveCoinsView() {
		String tag = "receive_coins_fragment";
		Fragment fragToShow = this.getSupportFragmentManager().findFragmentByTag(tag);
		if (fragToShow == null) {
			fragToShow = new OWReceiveCoinsFragment();
		}

		// If we did a tablet view this might be different. 
		this.showFragment(fragToShow, tag, R.id.oneWalletBaseFragmentHolder, true, FragmentType.ACCOUNT_FRAGMENT_TYPE.toString() + "_INNER");
	}

	/**
	 * Only works for enabled types type right now. 
	 * 
	 * @param typeOfWalletToStart
	 */
	public void openSendCoinsView() {
		String tag = "send_coins_fragment";
		Fragment fragToShow = this.getSupportFragmentManager().findFragmentByTag(tag);
		if (fragToShow == null) {
			fragToShow = new OWSendCoinsFragment();
		}

		// If we did a tablet view this might be different. 
		this.showFragment(fragToShow, tag, R.id.oneWalletBaseFragmentHolder, true, FragmentType.ACCOUNT_FRAGMENT_TYPE.toString() + "_INNER");
	}

	/**
	 * Open the view for transaction details
	 */
	public void openTxnDetails(OWTransaction txItem) {

		String tag = "txn_details_fragment";
		OWTransactionDetailsFragment fragToShow = (OWTransactionDetailsFragment) this.getSupportFragmentManager().findFragmentByTag(tag);
		if (fragToShow == null) {
			fragToShow = new OWTransactionDetailsFragment();
		}
		Bundle b = new Bundle();
		b.putParcelable("txItem", txItem);
		fragToShow.setArguments(b);
		this.showFragment(fragToShow, tag, R.id.oneWalletBaseFragmentHolder, true, 
				FragmentType.ACCOUNT_FRAGMENT_TYPE.toString() + "_INNER");
	}

	/**
	 * Open view for add new currency
	 */
	public void openAddCurrency(List<OWCoin> userCurWallets) {
		String tag = "add_new_currency";
		OWNewCurrencyFragment fragToShow = (OWNewCurrencyFragment) this.getSupportFragmentManager().findFragmentByTag(tag);
		if (fragToShow == null) {
			fragToShow = new OWNewCurrencyFragment();
		}
		// Here we get all the coins that the user doesn't currently 
		// have a wallet for because those are the ones we put in the new view.
		Set<OWCoin> coinsNotInListCurrently = new HashSet<OWCoin>(
				Arrays.asList(OWCoin.values()));
		coinsNotInListCurrently.removeAll(userCurWallets);
		Bundle b = new Bundle();
		for (OWCoin type : coinsNotInListCurrently) {
			b.putBoolean(type.toString(), true);
		}
		fragToShow.setArguments(b);
		this.showFragment(fragToShow, tag, R.id.oneWalletBaseFragmentHolder, true, 
				FragmentType.ACCOUNT_FRAGMENT_TYPE.toString() + "_INNER");

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

		alertUserDialog.setTargetFragment(null, OWRequestCodes.ALERT_USER_DIALOG);

		// Set negative text to null to not have negative button
		alertUserDialog.setupDialog("ziftrWALLET", message, null, "OK", null);
		alertUserDialog.show(this.getSupportFragmentManager(), tag);
	}
	/**
	 * generate dialog to ask for passphrase
	 * 
	 * @param requestcode = OWRequestcodes parameter to differentiate where the password dialog is
	 * @param args = bundle with OWCoinType of currency to add if user is adding currency
	 */
	public void showGetPassphraseDialog(int requestcode, Bundle args, String tag) {
		OWValidatePassphraseDialog passphraseDialog = 
				new OWValidatePassphraseDialog();

		args.putInt(OWDialogFragment.REQUEST_CODE_KEY, requestcode);

		String message = "Please input your passphrase. ";

		passphraseDialog.setArguments(args);

		passphraseDialog.setupDialog("ziftrWALLET", message, 
				"Continue", null, "Cancel");

		if (!isShowingDialog()) {
			passphraseDialog.show(this.getSupportFragmentManager(), 
					tag);
		}
	}

	///////////// Passer methods /////////////

	@Override
	public void handleNegative(int requestCode) {
		switch(requestCode) {
		case OWRequestCodes.VALIDATE_PASSPHRASE_DIALOG_SEND:
			// Nothing to do
			break;
		case OWRequestCodes.ALERT_USER_DIALOG:
			// Nothing to do
			break;
		case OWRequestCodes.RESET_PASSPHRASE_DIALOG:
			// Nothing to do
			break;
		}
	}

	@Override
	public void handlePassphrasePositive(int requestCode, 
			byte[] passphrase, Bundle info) {
		byte[] inputHash = OWUtils.Sha256Hash(passphrase);
		switch(requestCode) {
		case OWRequestCodes.VALIDATE_PASSPHRASE_DIALOG_NEW_CURRENCY:
			if (this.inputHashMatchesStoredHash(inputHash)) {
				addNewCurrency(info);
			} else {
				this.alertUser(
						"Error: Passphrases don't match. ", "wrong_passphrase");
			}
			break;
		case OWRequestCodes.VALIDATE_PASSPHRASE_DIALOG_NEW_KEY:
			if (this.inputHashMatchesStoredHash(inputHash)) {
				OWReceiveCoinsFragment frag = (OWReceiveCoinsFragment) getSupportFragmentManager(
						).findFragmentByTag("receive_coins_fragment");
				frag.loadAddressFromDatabase();
			} else {
				this.alertUser(
						"Error: Passphrases don't match. ", "wrong_passphrase");
			}
			break;
		case OWRequestCodes.VALIDATE_PASSPHRASE_DIALOG_SEND:
			if (this.inputHashMatchesStoredHash(inputHash)) {
				OWSendCoinsFragment frag = (OWSendCoinsFragment) getSupportFragmentManager(
						).findFragmentByTag("send_coins_fragment");
				frag.clickSendCoins();

			} else {
				this.alertUser(
						"Error: Passphrases don't match. ", "wrong_passphrase");
			}
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
			break;
		}
	}

	public void setShowingDialog(boolean showing) {
		this.showingDialog = showing;
	}

	public boolean isShowingDialog() {
		return this.showingDialog;
	}

	/**
	 * @return the curSelectedCoinType
	 */
	public OWCoin getCurSelectedCoinType() {
		return curSelectedCoinType;
	}

	/**
	 * @param curSelectedCoinType the curSelectedCoinType to set
	 */
	public void setCurSelectedCoinType(OWCoin curSelectedCoinType) {
		this.curSelectedCoinType = curSelectedCoinType;
	}

	/**
	 * Customize actionbar
	 * @param title - text in middle of actionbar
	 * @param menu - boolean determine if menu button to open drawer is visible, if false, the back button will be visible
	 * @param home - boolean to display home button
	 * @param search - boolean to display search button
	 */
	public void changeActionBar(String title, boolean menu, boolean home) {
		this.changeActionBar(title, menu, home, null, null);
	}

	/**
	 * Customize actionbar
	 * @param title - text in middle of actionbar
	 * @param menu - boolean determine if menu button to open drawer is visible, if false, the back button will be visible
	 * @param home - boolean to display home button
	 * @param search - boolean to display search button
	 */
	public void changeActionBar(String title, boolean menu, boolean home, 
			final TextWatcher textWatcher, 
			final OWSearchableListAdapter<? extends OWSearchableListItem> adapter) {

		ImageView menuButton = (ImageView) this.findViewById(R.id.switchTaskMenuButton);
		ImageView backButton = (ImageView) this.findViewById(R.id.actionBarBack);
		ImageView homeButton = (ImageView) this.findViewById(R.id.actionBarHome);
		ImageView searchButton = (ImageView) this.findViewById(R.id.actionBarSearch);

		final View searchBar = findViewById(R.id.searchBar);

		TextView titleview = (TextView) findViewById(R.id.actionBarTitle);
		titleview.setText(title);

		if (menu) {
			backButton.setVisibility(View.GONE);
			menuButton.setVisibility(View.VISIBLE);
		} else {
			backButton.setVisibility(View.VISIBLE);
			backButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (OWMainFragmentActivity.this.drawerMenuIsOpen()) {
						toggleDrawerPosition();
					}
					OWMainFragmentActivity.this.onBackPressed();
				}

			});
			menuButton.setVisibility(View.GONE);
		}


		if (home) {
			homeButton.setVisibility(View.VISIBLE);

			//setup actionbar home click listener
			homeButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (OWMainFragmentActivity.this.drawerMenuIsOpen()) {
						toggleDrawerPosition();
					}					
					//clear backstack
					OWMainFragmentActivity.this.getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
					//show home
					OWMainFragmentActivity.this.showFragmentFromType(
							FragmentType.ACCOUNT_FRAGMENT_TYPE, false);
				}
			});

		} else {
			homeButton.setVisibility(View.GONE);
		}

		// For search bar
		if (textWatcher != null) {
			filterAccordingToVisibility(adapter);

			this.specifySearchBarTextWatcher(textWatcher);

			final EditText searchText = (EditText) findViewById(R.id.searchBarEditText);

			View clearbutton = findViewById(R.id.clearSearchBarTextIcon);
			clearbutton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					searchText.setText("");
				}

			});

			searchButton.setVisibility(View.VISIBLE);

			searchButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (OWMainFragmentActivity.this.drawerMenuIsOpen()) {
						toggleDrawerPosition();
					}					

					if (!searchBarIsVisible()) {
						searchBar.setVisibility(View.VISIBLE);
						searchText.requestFocus();
					} else {
						searchBar.setVisibility(View.GONE);
					}

					filterAccordingToVisibility(adapter);
				}
			});

		} else {
			searchButton.setVisibility(View.GONE);
			searchBar.setVisibility(View.GONE);
		}
	}

	private void filterAccordingToVisibility(
			final OWSearchableListAdapter<? extends OWSearchableListItem> adapter) {
		final EditText searchText = (EditText) findViewById(R.id.searchBarEditText);
		if (!searchBarIsVisible()) {
			// Filter
			adapter.getFilter().filter("");
		} else {
			// Stop filtering
			adapter.getFilter().filter(searchText.getText());			
		}
	}

	public void specifySearchBarTextWatcher(TextWatcher textWatcher) {
		// called in onResume of all fragments that use the search bar
		// Can, instead, also be called by calling the change action bar method
		EditText searchText = (EditText) findViewById(R.id.searchBarEditText);
		searchText.addTextChangedListener(textWatcher);
	}

	public void unregisterSearchBarTextWatcher(TextWatcher textWatcher) {
		// TODO Called in onPause of all fragments that use the search bar
		EditText searchText = (EditText) findViewById(R.id.searchBarEditText);
		searchText.removeTextChangedListener(textWatcher);
	}

	public boolean searchBarIsVisible() {
		View search = findViewById(R.id.searchBar);
		return search.getVisibility() == View.VISIBLE;
	}

	public void hideWalletHeader() {
		View walletHeader = findViewById(R.id.walletHeader);
		walletHeader.setVisibility(View.GONE);
	}

}
