/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
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
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ziftr.android.ziftrwallet.crypto.ZWAddress;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWCoinURI;
import com.ziftr.android.ziftrwallet.crypto.ZWConverter;
import com.ziftr.android.ziftrwallet.crypto.ZWFiat;
import com.ziftr.android.ziftrwallet.crypto.ZWTransaction;
import com.ziftr.android.ziftrwallet.dialog.ZiftrDialogManager;
import com.ziftr.android.ziftrwallet.fragment.ZWAboutFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWAccountsFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWNewCurrencyFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWReceiveCoinsFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWSearchableListAdapter;
import com.ziftr.android.ziftrwallet.fragment.ZWSearchableListItem;
import com.ziftr.android.ziftrwallet.fragment.ZWSecurityFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWSendCoinsFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWSetFiatFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWSettingsFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWTermsFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWTransactionDetailsFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWWalletFragment;
import com.ziftr.android.ziftrwallet.network.ZWDataSyncHelper;
import com.ziftr.android.ziftrwallet.network.ZiftrNetworkHandler;
import com.ziftr.android.ziftrwallet.network.ZiftrNetworkManager;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

/**
 * This is the main activity of the ZiftrWallet application. It handles
 * the menu drawer and the switching between the different fragments 
 * depending on which task the user selects.
 */
public class ZWMainFragmentActivity extends ActionBarActivity 
implements DrawerListener, OnClickListener, ZiftrNetworkHandler {
	
	private static final String TAG_BACKSTACK =  "accounts_inner";

	/** The drawer layout menu. */
	private DrawerLayout menuDrawer;
	
	/** Use this key to save which section of the drawer menu is open. */
	private static final String KEY_SELECTED_SECTION = "SELECTED_SECTION_KEY";

	/** Use this key to save whether or not the search bar is visible (vs gone). */
	private static final String KEY_SEARCH_BAR_VISIBILITY = "SEARCH_BAR_VISIBILITY_KEY";
	
	private static final String KEY_SELECTED_COIN = "selected_coin";

	/** The main view that all of the fragments will be stored in. */
	private View baseFragmentContainer;

	/** A float used in making the container move over when the drawer is moved. */
	private float lastTranslate = 0.0f;

	/** This object is responsible for all the wallets. */
	private ZWWalletManager walletManager;

	/** reference to searchBarEditText */
	private EditText searchEditText;
	

	private ZWCoin selectedCoin;
	private ImageView syncButton; //the button in various fragments users can press to sync their data
	private boolean isSyncing = false;
	
	private ZWActivityDialogHandler dialogHandler;
	private boolean consumedIntent = false;
	
	/**
	 * This is an enum to differentiate between the different
	 * sections of the app. Each enum also holds specific information related
	 * to each of fragment types.
	 */
	public enum FragmentType {
		/** The enum to identify the accounts fragment of the app. */
		ACCOUNT_FRAGMENT_TYPE {
			@Override
			public Fragment getNewFragment() {
				return new ZWAccountsFragment();
			}
		}, 
		/** The enum to identify the terms fragment of the app. */
		TERMS_FRAGMENT_TYPE {
			@Override
			public Fragment getNewFragment() {
				return new ZWTermsFragment();
			}
		}, 
		/** The enum to identify the settings fragment of the app. */
		SETTINGS_FRAGMENT_TYPE {
			@Override
			public Fragment getNewFragment() {
				return new ZWSettingsFragment();
			}
		}, 
		/** The enum to identify the about fragment of the app. */
		ABOUT_FRAGMENT_TYPE {
			@Override
			public Fragment getNewFragment() {
				return new ZWAboutFragment();
			}
		},
		/** The enum to identify the contact fragment of the app. */
		SECURITY_FRAGMENT_TYPE {
			@Override
			public Fragment getNewFragment() {
				return new ZWSecurityFragment();
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
		
		public boolean matches(Object object) {
			if(object == null) {
				return false;
			}
			else if(this == object) {
				return true;
			}
			else if(this.toString().equals(object.toString())) {
				return true;
			}
			else if(object instanceof Fragment) {
				Fragment fragment = (Fragment) object;
				if(this.toString().equals(fragment.getTag())) {
					return true;
				}
			}
			
			return false;
		}

		/**
		 * @param drawerMenuView the drawerMenuView to set
		 */
		public void setDrawerMenuView(View drawerMenuView) {
			this.drawerMenuView = drawerMenuView;
		}
	};

	/**
	 * Loads up the views and starts the ZWHomeFragment 
	 * if necessary. 
	 * 
	 * @param savedInstanceState - The stored bundle
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		dialogHandler = new ZWActivityDialogHandler(this);
		ZiftrDialogManager.registerHandler(dialogHandler);
		
		if (ZWPreferences.getLogToFile()){
			ZLog.setLogger(ZLog.FILE_LOGGER);
		}
		else if(ZWPreferences.getDebugMode()) {
			ZLog.setLogger(ZLog.ANDROID_LOGGER);
		}
		
		
		ZLog.log("\nMain Activity Created  " + (new Date()) + "\n");
		if(savedInstanceState != null) {
			consumedIntent = savedInstanceState.getBoolean("consumedIntent");
		}
		
		
		// Everything is held within this main activity layout
		this.setContentView(R.layout.activity_main);

		// Recreate wallet manager
		this.walletManager = ZWWalletManager.getInstance();
		
		//TODO -probably need to add a timer or something to this so we don't do it constantly when a user rotates their screen
		//load available coins from API blockchains
		ZWDataSyncHelper.updateCoinData();
		ZWDataSyncHelper.checkForUpdates();
		
		// Get the saved cur selected coin type
		this.initializeCoinType(savedInstanceState);
		
		// Set up header and visibility of header
		this.initializeHeaderViewsVisibility(savedInstanceState);

		// Set up the drawer and the menu button
		this.initializeDrawerLayout();

		// Make sure the base fragment view is initialized
		this.initializeBaseFragmentContainer(savedInstanceState);

		// Make sure the action bar changes with what fragment we are in
		this.initializeActionBar();

		// Hook up the search bar to show the keyboard without messing up the view
		this.initializeSearchBarText();
		
		ZiftrNetworkManager.registerNetworkHandler(this);
	}

	@Override
	public void onPause(){
		super.onPause();
	}

	
	@Override
	protected void onNewIntent (Intent intent) {
		consumedIntent = false;
		this.setIntent(intent);
	}
	
	
	@Override
	public void onResume(){
		super.onResume();
		this.handleIntent(getIntent());
		ZiftrDialogManager.showWaitingDialogs();
	}
	
	@Override
	public void onPostResume(){
		super.onPostResume();
	}
	
	
	private void handleIntent(Intent intent) {
		
		
		//Check if loaded from widget
		//TODO -removing incomplete widget features for first release
		/***
		if (getIntent().hasExtra(ZWWalletWidget.WIDGET_RECEIVE) || getIntent().hasExtra(ZWWalletWidget.WIDGET_SEND)){
			this.setSelectedCoin(ZWCoin.getCoin(ZWPreferencesUtils.getWidgetCoin()));
			if (this.selectedCoin != null && this.getWalletManager().isCoinActivated(this.selectedCoin)) {
				if (getIntent().hasExtra(ZWWalletWidget.WIDGET_SEND)){
					getIntent().removeExtra(ZWWalletWidget.WIDGET_SEND);
					openSendCoinsView(null);
				} else{
					getIntent().removeExtra(ZWWalletWidget.WIDGET_RECEIVE);
					openReceiveCoinsView(null);
				}
			}
		
		} 
		***/
		
		
		if (intent != null && !consumedIntent && 
				intent.getAction() == Intent.ACTION_VIEW &&
				(intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0 ){
			
			//if we have an intent, and we haven't consumed it, and it's view type, and it's not from the history
			//this we were just launched from a coin uri, so handle it appropriately
			
			String data = intent.getDataString();
			
			//clear the intent so it doesn't keep doing this
			consumedIntent = true;
			this.setIntent(null);
			
			
			String[] dataStrings = data.split(":");
			String scheme = dataStrings[0];
			String address = dataStrings[1];
			if(address.contains("?")) {
				address = address.substring(0, address.indexOf("?"));
			}
			
			
			ZWCoin coin = ZWCoin.getCoin(scheme, address);
			if(coin == null) {
				ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_invalid_address_uri);
			}
			else if(!getWalletManager().isCoinActivated(coin)) {
				String notActiveError = getString(R.string.zw_dialog_error_activate_coin);
				notActiveError = String.format(notActiveError, coin.getName());
				
				ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), notActiveError);
			}
			else {
				
				this.setSelectedCoin(coin);
				try {
					ZWCoinURI uri = new ZWCoinURI(coin, data);
					
					openSendCoinsView(uri);
				} 
				catch (Exception e) {
					ZLog.log("Exception opening send coins view: ", e);
					ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_invalid_address_uri);
				}
			}
			
		}
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
		outState.putString(KEY_SELECTED_SECTION, this.getCurrentlySelectedDrawerMenuOption());
		outState.putInt(KEY_SEARCH_BAR_VISIBILITY, getSearchBar().getVisibility());
		if (this.getSelectedCoin() != null) {
			outState.putString(KEY_SELECTED_COIN, this.getSelectedCoin().getSymbol());
		}
		
		outState.putBoolean("consumedIntent", consumedIntent);
		
	}

	/**
	 * When the back button is pressed from this activity we
	 * go to the accounts fragment if we are somewhere else, and from
	 * the accounts fragment we exit the app. 
	 */
	@Override
	public void onBackPressed() {
		
		//in case the device has a hardware back button, make sure virtual keyboard is closed
		InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
		
		if (drawerMenuIsOpen()){
			this.menuDrawer.closeDrawer(Gravity.LEFT);
			return;
		}
		ZWFragment topFragment = this.getTopDisplayedFragment();
		if (topFragment != null && topFragment.handleBackPress()) {
			return;
		}

		String curSelected = getCurrentlySelectedDrawerMenuOption();
		if (curSelected == null) {
			super.onBackPressed();
		} 
		else if (FragmentType.ACCOUNT_FRAGMENT_TYPE.toString().equals(curSelected)) {
			super.onBackPressed();
			//if we are in set fiat screen, back should go back to settings
		} 
		else if (topFragment.getTag().equals(ZWSetFiatFragment.FRAGMENT_TAG)) {
			super.onBackPressed();
		} 
		else {
			//Select accounts in drawer since anywhere you hit back, you will end up in accounts
			selectSingleDrawerMenuOption(findViewById(R.id.menuDrawerAccountsLayout));
			
			if (!getSupportFragmentManager().popBackStackImmediate(TAG_BACKSTACK, 0)) {
				ZWMainFragmentActivity.this.showFragmentFromType(FragmentType.ACCOUNT_FRAGMENT_TYPE, true);
				
				//clear back stack
				this.getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			}
		}
	}

	private ZWFragment getTopDisplayedFragment() {
		return (ZWFragment) this.getSupportFragmentManager().findFragmentById(R.id.oneWalletBaseFragmentHolder);
	}

	/**
	 * Here we need to close all the wallets. 
	 */
	@Override
	protected void onDestroy() {
		ZWWalletManager.closeInstance();
		super.onDestroy();
	}

	/**
	 * When the user hits the sync button, we get the top fragment
	 * and refresh it's data.
	 * 
	 * @param v
	 */
	@Override
	public void onClick(View v) {
		if (v == syncButton && !isSyncing) {
			ZWFragment top = this.getTopDisplayedFragment();
			if (top != null) {
				top.refreshData(false);
			}
		}
	}

	/**
	 * Starts a new fragment in the main layout space depending
	 * on which fragment FragmentType is passed in. 
	 * 
	 * @param fragmentType - The identifier for which type of fragment to start.
	 * @param addToBackStack - boolean determining if fragment should be added to backstack
	 */
	public void showFragmentFromType(FragmentType fragmentType, Boolean addToBackStack) {
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
	public void showFragment(Fragment fragToShow, String tag, int resId, boolean addToBackStack, String backStackTag) {
		// The transaction that will take place to show the new fragment
		FragmentTransaction transaction = this.getSupportFragmentManager().beginTransaction();

		// TODO add animation to transaciton here
		transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left, R.anim.slide_in_right, R.anim.slide_out_right);
		
		if (fragToShow.isVisible()) {
			// If the fragment is already visible, no need to do anything
			return;
		}

		transaction.replace(resId, fragToShow, tag);
		if (addToBackStack) {
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
		selectionItems.add(this.findViewById(R.id.menuDrawerTermsLayout));
		selectionItems.add(this.findViewById(R.id.menuDrawerSettingsLayout));
		selectionItems.add(this.findViewById(R.id.menuDrawerAboutLayout));
		selectionItems.add(this.findViewById(R.id.menuDrawerSecurityLayout));

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
							ZWMainFragmentActivity.this.getCurrentlySelectedDrawerMenuOption())) {
						//clear back stack
						ZWMainFragmentActivity.this.getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

						ZWMainFragmentActivity.this.showFragmentFromType(
								FragmentType.ACCOUNT_FRAGMENT_TYPE, false);

						//Else accounts is not selected so resume previous accounts activity
					} 
					else if (!getSupportFragmentManager().popBackStackImmediate(TAG_BACKSTACK, 0)) {
						ZWMainFragmentActivity.this.showFragmentFromType(FragmentType.ACCOUNT_FRAGMENT_TYPE, true);
						ZWMainFragmentActivity.this.getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
					}
					
					ZWMainFragmentActivity.this.onAnyDrawerMenuItemClicked(clickedView);
				}
			});

			// Set up for terms section
			View termsMenuButton = 
					this.findViewById(R.id.menuDrawerTermsLayout);
			FragmentType.TERMS_FRAGMENT_TYPE.setDrawerMenuView(
					termsMenuButton);
			termsMenuButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View clickedView) {
					ZWMainFragmentActivity.this.onAnyDrawerMenuItemClicked(clickedView);
					ZWMainFragmentActivity.this.showFragmentFromType(
							FragmentType.TERMS_FRAGMENT_TYPE, true);
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
					ZWMainFragmentActivity.this.onAnyDrawerMenuItemClicked(clickedView);
					ZWMainFragmentActivity.this.showFragmentFromType(
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
					ZWMainFragmentActivity.this.onAnyDrawerMenuItemClicked(clickedView);
					ZWMainFragmentActivity.this.showFragmentFromType(
							FragmentType.ABOUT_FRAGMENT_TYPE, true);
				}
			});

			// Set up for contact section
			View securityMenuButton = 
					this.findViewById(R.id.menuDrawerSecurityLayout);
			FragmentType.SECURITY_FRAGMENT_TYPE.setDrawerMenuView(
					securityMenuButton);
			securityMenuButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View clickedView) {
					ZWMainFragmentActivity.this.onAnyDrawerMenuItemClicked(clickedView);
					ZWMainFragmentActivity.this.showFragmentFromType(
							FragmentType.SECURITY_FRAGMENT_TYPE, true);
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
			if (args.getString(KEY_SELECTED_COIN) != null) {
				// Need to do this so that we populate the wallet header view as well
				this.setSelectedCoin(ZWCoin.getCoin(args.getString(KEY_SELECTED_COIN)));
			}
		}
	}

	private void initializeHeaderViewsVisibility(Bundle args) {
		if (args != null) {
			if (args.getInt(KEY_SEARCH_BAR_VISIBILITY, -1) != -1) {
				this.getSearchBar().setVisibility(args.getInt(KEY_SEARCH_BAR_VISIBILITY));
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
			this.showFragmentFromType(FragmentType.ACCOUNT_FRAGMENT_TYPE, false);
		} else {
			// Here we must make sure that the drawer menu is still
			// showing which section of the app is currently open.
			String prevSelectedSectionString = savedInstanceState.getString(KEY_SELECTED_SECTION);
			if (prevSelectedSectionString != null) {
				FragmentType fragmentType = FragmentType.valueOf(prevSelectedSectionString);
				if (fragmentType.getDrawerMenuView() != null) {
					this.selectSingleDrawerMenuOption(fragmentType.getDrawerMenuView());
				} else {
					ZLog.log("The drawer menu was null, can't select... shouldn't happen1");
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
		this.searchEditText = (EditText) findViewById(R.id.searchBarContainer).findViewById(R.id.customEditText);
		this.searchEditText.clearFocus();
		this.searchEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
		this.searchEditText.setHint(getResources().getString(R.string.zw_searchbar_hint));
		this.searchEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				if (hasFocus) {
					//make keyboard overlap the send/receive buttons
					getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
					imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
				} else {
					getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
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
	 * called after user enters password for creating new wallet 
	 * 
	 * @param bundle with ZWCoin of wallet to add
	 */
	public void addNewCurrency(ZWCoin newCoin) {
		// TODO we can probably get rid of this if it's slowing stuff down - unnecessary check 
		// Make sure that this view only has wallets
		// in it which the user do
		if(this.walletManager.isCoinActivated(newCoin)) {
			// Already in list, shouldn't ever get here though because
			// we only show currencies in the dialog which we don't have
			this.onBackPressed();
			return;
		}
		
		walletManager.activateCoin(newCoin);
		
		Toast.makeText(this, "Wallet Created!", Toast.LENGTH_LONG).show();
		this.onBackPressed();

	}

	/**
	 * Only works for enabled types right now. 
	 * 
	 * @param typeOfWalletToStart
	 */
	public void openWalletView(ZWCoin typeOfWalletToStart) {
		
		this.setSelectedCoin(typeOfWalletToStart);
		Fragment fragToShow = this.getSupportFragmentManager().findFragmentByTag(ZWWalletFragment.FRAGMENT_TAG);
		if (fragToShow == null) {
			fragToShow = new ZWWalletFragment();
		}

		// If we did a tablet view this might be different. 
		this.showFragment(fragToShow, ZWWalletFragment.FRAGMENT_TAG, R.id.oneWalletBaseFragmentHolder, true, TAG_BACKSTACK);
	}

	

	/**
	 * Only works for enabled types right now. 
	 * 
	 * @param typeOfWalletToStart
	 */
	public void openReceiveCoinsView(ZWAddress address) {
		Fragment fragToShow = this.getSupportFragmentManager().findFragmentByTag(ZWReceiveCoinsFragment.FRAGMENT_TAG);
		if (fragToShow == null) {
			fragToShow = new ZWReceiveCoinsFragment();
		}

		// If we did a tablet view this might be different. 
		this.showFragment(fragToShow, ZWReceiveCoinsFragment.FRAGMENT_TAG, R.id.oneWalletBaseFragmentHolder, true, TAG_BACKSTACK);
		
		if (address != null){
			((ZWReceiveCoinsFragment) fragToShow).setReceiveAddress(address);		
		}
	}

	/**
	 * Only works for enabled types type right now. 
	 * 
	 * @param typeOfWalletToStart
	 */
	public void openSendCoinsView(Object preloadData) {
		Fragment fragToShow = this.getSupportFragmentManager().findFragmentByTag(ZWSendCoinsFragment.FRAGMENT_TAG);

		if (fragToShow == null) {
			fragToShow = new ZWSendCoinsFragment();
			
			if(preloadData != null) {
				//note, instanceof is a bit hacky, but best to keep all this fragment loading code in one method
				if(preloadData instanceof ZWCoinURI) {
					((ZWSendCoinsFragment) fragToShow).preloadSendData((ZWCoinURI) preloadData);
				} 
				else {
					((ZWSendCoinsFragment) fragToShow).preloadAddress(preloadData.toString());
				}
			}
		
			// If we did a tablet view this might be different. 
			this.showFragment(fragToShow, ZWSendCoinsFragment.FRAGMENT_TAG, R.id.oneWalletBaseFragmentHolder, true, TAG_BACKSTACK);
		} 
		else {
			//we are already showing sendcoins fragment
			if(preloadData instanceof ZWCoinURI) {
				((ZWSendCoinsFragment) fragToShow).loadSendData((ZWCoinURI) preloadData);
			}
		}

	}
	

	
	/**
	 * Open the view for transaction details
	 */
	public void openTxnDetails(ZWTransaction txItem) {
		ZWTransactionDetailsFragment fragToShow = (ZWTransactionDetailsFragment) 
				this.getSupportFragmentManager().findFragmentByTag(ZWTransactionDetailsFragment.FRAGMENT_TAG);
		if (fragToShow == null) {
			fragToShow = new ZWTransactionDetailsFragment();
		}
		Bundle b= new Bundle();
		b.putString(ZWTransactionDetailsFragment.TX_ITEM_HASH_KEY, txItem.getSha256Hash());
		fragToShow.setArguments(b);
		this.showFragment(fragToShow, ZWTransactionDetailsFragment.FRAGMENT_TAG, R.id.oneWalletBaseFragmentHolder, true, TAG_BACKSTACK);
		
	}

	/**
	 * Open view for add new currency
	 */
	public void openAddCurrency() {
		if (drawerMenuIsOpen()){
			this.menuDrawer.closeDrawer(Gravity.LEFT);
		}
		ZWNewCurrencyFragment fragToShow = (ZWNewCurrencyFragment) 
				this.getSupportFragmentManager().findFragmentByTag(ZWNewCurrencyFragment.FRAGMENT_TAG);
		if (fragToShow == null) {
			fragToShow = new ZWNewCurrencyFragment();
		}

		this.showFragment(fragToShow, ZWNewCurrencyFragment.FRAGMENT_TAG, R.id.oneWalletBaseFragmentHolder, true, TAG_BACKSTACK);

	}

	
	/**
	 * Open View for selecting fiat currency in settings
	 */
	public void openSetFiatCurrency(){
		ZWSetFiatFragment fragToShow = (ZWSetFiatFragment) this.getSupportFragmentManager().findFragmentByTag(ZWSetFiatFragment.FRAGMENT_TAG);
		if (fragToShow == null){
			fragToShow = new ZWSetFiatFragment();
		}
		
		this.showFragment(fragToShow, ZWSetFiatFragment.FRAGMENT_TAG, R.id.oneWalletBaseFragmentHolder, true, ZWSetFiatFragment.FRAGMENT_TAG);
	}

	/**
	 * @return the walletManager
	 */
	public ZWWalletManager getWalletManager() {
		return walletManager;
	}

	/**
	 * @return the headerBar
	 */
	public View getSearchBar() {
		return this.findViewById(R.id.searchBar);
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

	public void updateTopFragmentView() {
		// If already on the UI thread, then it just does this right now
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				ZWFragment topFragment = getTopDisplayedFragment();
				if (topFragment != null) {
					topFragment.onDataUpdated();
				}
			}
		});
	}


	/**
	 * @return the curSelectedCoinType
	 */
	public ZWCoin getSelectedCoin() {
		return this.selectedCoin;
	}

	/**
	 * @param selectedCoin the curSelectedCoinType to set
	 */
	public void setSelectedCoin(ZWCoin selectedCoin) {
		this.selectedCoin = selectedCoin;
	}

	/**
	 * Making this a separate method to increase abstraction and just in
	 * case we ever need to call this by itself. 
	 */
	public void populateWalletHeaderView(View headerView) {
		//now that a coin type is set, setup the header view for it
		ImageView coinLogo = (ImageView) (headerView.findViewById(R.id.leftIcon));

		coinLogo.setImageResource(this.selectedCoin.getLogoResId());
		
		headerView.findViewById(R.id.market_graph_icon).setVisibility(View.VISIBLE);
		
		TextView coinTitle = (TextView) headerView.findViewById(R.id.topLeftTextView);
		coinTitle.setText(this.selectedCoin.getName());
		
		updateWalletHeaderView(headerView);
		
		syncButton = (ImageView) headerView.findViewById(R.id.rightIcon);
		syncButton.setImageResource(R.drawable.icon_sync_button_statelist);
		syncButton.setOnClickListener(this);
		if(isSyncing) {
			startSyncAnimation();
		}
	}
	
	//update coin market val & wallet coin and fiat balances
	public void updateWalletHeaderView(View headerView){
		ZWFiat selectedFiat = ZWPreferences.getFiatCurrency();
		
		TextView fiatExchangeRateText = (TextView) headerView.findViewById(R.id.bottomLeftTextView);
		
		String fiatString = selectedFiat.getUnitPrice(getSelectedCoin());
		fiatExchangeRateText.setText(ZiftrUtils.getCurrencyDisplayString(fiatString));

		TextView walletBalanceTextView = (TextView) headerView.findViewById(R.id.topRightTextView);
		BigInteger atomicUnits =  getWalletManager().getWalletBalance(getSelectedCoin());
		BigDecimal walletBalance = getSelectedCoin().getAmount(atomicUnits);

		walletBalanceTextView.setText(getSelectedCoin().getFormattedAmount(walletBalance));

		TextView walletBalanceInFiatText = (TextView) headerView.findViewById(R.id.bottomRightTextView);
		BigDecimal walletBalanceInFiat = ZWConverter.convert(walletBalance, getSelectedCoin(), selectedFiat);
		
		String balanceString = selectedFiat.getFormattedAmount(walletBalanceInFiat, true);
		walletBalanceInFiatText.setText(ZiftrUtils.getCurrencyDisplayString(balanceString));
	}
	

	/**
	 * Customize actionbar
	 * @param title - text in middle of actionbar
	 * @param menu - boolean determine if menu button to open drawer is visible, if false, 
	 * then the back button will be visible.
	 * @param home - boolean to display home button
	 * @param search - boolean to display search button
	 * @param addCurrency - boolean to display + button to add new currency
	 */
	public void changeActionBar(String title, boolean menu, boolean home, boolean addCurrency) {
		this.changeActionBar(title, menu, home, addCurrency, null, null);
	}

	/**
	 * Customize actionbar
	 * @param title - text in middle of actionbar
	 * @param menu - boolean determine if menu button to open drawer is visible, if false, 
	 * then the back button will be visible.
	 * @param home - boolean to display home button
	 * @param search - boolean to display search button
	 * @param addCurrency - boolean to display + button to add new currency
	 */
	public void changeActionBar(String title, boolean menu, boolean home, boolean addCurrency,
			final TextWatcher textWatcher, 
			final ZWSearchableListAdapter<? extends ZWSearchableListItem> adapter) {

		ImageView menuButton = (ImageView) this.findViewById(R.id.switchTaskMenuButton);
		ImageView backButton = (ImageView) this.findViewById(R.id.actionBarBack);
		ImageView homeButton = (ImageView) this.findViewById(R.id.actionBarHome);
		ImageView searchButton = (ImageView) this.findViewById(R.id.actionBarSearch);
		ImageView addCurrencyButton = (ImageView) this.findViewById(R.id.actionBarAddCurrency);

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
					if (ZWMainFragmentActivity.this.drawerMenuIsOpen()) {
						toggleDrawerPosition();
					}
					ZWMainFragmentActivity.this.onBackPressed();
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
					goHome();
				}
			});

		} else {
			homeButton.setVisibility(View.GONE);
		}

		if (addCurrency){
			addCurrencyButton.setVisibility(View.VISIBLE);
			addCurrencyButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					openAddCurrency();
				}
			});
		} else {
			addCurrencyButton.setVisibility(View.GONE);
		}

		// For search bar
		if (textWatcher != null) {
			filterAccordingToVisibility(adapter);

			this.registerSearchBarTextWatcher(textWatcher);

			View clearbutton = findViewById(R.id.clearSearchBarTextIcon);
			clearbutton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (searchEditText.getText().toString().isEmpty()){
						searchBar.setVisibility(View.GONE);
					} else {
						searchEditText.setText("");
					}
				}

			});

			searchButton.setVisibility(View.VISIBLE);

			searchButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (ZWMainFragmentActivity.this.drawerMenuIsOpen()) {
						toggleDrawerPosition();
					}					

					if (!searchBarIsVisible()) {
						searchBar.setVisibility(View.VISIBLE);
						searchEditText.requestFocus();
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
	
	
	public void goHome() {
		if (ZWMainFragmentActivity.this.drawerMenuIsOpen()) {
			toggleDrawerPosition();
		}					
		//clear backstack
		ZWMainFragmentActivity.this.getSupportFragmentManager().popBackStackImmediate(
				null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		//show home
		ZWMainFragmentActivity.this.showFragmentFromType(
				FragmentType.ACCOUNT_FRAGMENT_TYPE, false);
	}
	

	private void filterAccordingToVisibility(
			final ZWSearchableListAdapter<? extends ZWSearchableListItem> adapter) {
		if (!searchBarIsVisible()) {
			// Filter
			adapter.getFilter().filter("");
		} else {
			// Stop filtering
			adapter.getFilter().filter(this.searchEditText.getText());

		}
	}

	public void registerSearchBarTextWatcher(TextWatcher textWatcher) {
		// called in onResume of all fragments that use the search bar
		// Can, instead, also be called by calling the change action bar method
		this.searchEditText.addTextChangedListener(textWatcher);
	}

	public void unregisterSearchBarTextWatcher(TextWatcher textWatcher) {
		// TODO Called in onPause of all fragments that use the search bar
		this.searchEditText.removeTextChangedListener(textWatcher);
	}

	public boolean searchBarIsVisible() {
		View search = findViewById(R.id.searchBar);
		return search.getVisibility() == View.VISIBLE;
	}

	@Override
	public void handleExpiredLoginToken() {
		//TODO -wallet app doesn't yet use login tokens
	}

	@Override
	public void handleNetworkError(String errorMessage) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleDataUpdated() {
		this.updateTopFragmentView();
	}

	@Override
	public void networkStarted() {
		
		ZLog.log("Network started.......");
		
		this.runOnUiThread( new Runnable() {
			@Override
			public void run() {
				startSyncAnimation();
			}
		});
		
	}

	@Override
	public void networkStopped() {

		ZLog.log("Network stopped.......");
		
		this.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				stopSyncAnimation();
			}
		});
		
	}

	private synchronized void startSyncAnimation() {
		isSyncing = true;
		if(syncButton != null && syncButton.getVisibility() == View.VISIBLE) {
			syncButton.clearAnimation(); //clear out any old animations before starting the new one
			
			Animation rotation = AnimationUtils.loadAnimation(ZWMainFragmentActivity.this, R.anim.rotation);
			rotation.setRepeatCount(Animation.INFINITE);
			syncButton.startAnimation(rotation);
		}
	}
	
	private synchronized void stopSyncAnimation() {
		isSyncing = false;
		if(syncButton != null && syncButton.getVisibility() == View.VISIBLE) {
			Animation rotation = syncButton.getAnimation();
			if(rotation != null) {
				rotation.setRepeatCount(0);
			}
		}
	}
	
}
