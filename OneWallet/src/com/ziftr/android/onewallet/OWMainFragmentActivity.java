package com.ziftr.android.onewallet;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Build;
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

import com.ziftr.android.onewallet.fragment.OWAboutFragment;
import com.ziftr.android.onewallet.fragment.OWContactFragment;
import com.ziftr.android.onewallet.fragment.OWExchangeFragment;
import com.ziftr.android.onewallet.fragment.OWSettingsFragment;
import com.ziftr.android.onewallet.fragment.accounts.OWAccountsFragment;
import com.ziftr.android.onewallet.util.ZLog;

/**
 * This is the main activity of the OneWallet application. It handles
 * the menu drawer and the switching between the different fragments 
 * depending on which task the user selects.
 */
public class OWMainFragmentActivity extends ActionBarActivity implements DrawerListener {

	/*
	--- TODO list for the OneWallet project ---

	o 1. Ability to generate new address upon user request
	and be able to turn that into a QR code

	o 2. Get transaction history for all addresses in wallet and
	be able to display them.

	o 3. Making layouts necessary for the wireframe models.

	o 4. Organizing tasks that need to be done and appr. difficulty

	o 5. OW to start all of our classes.

	o 6. ZiftrUtils and Zlog for static useful methods.
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

	X 13. make it so that the content is moved over whenever we click the menu 
	button.

	o 14. Turn the EditText field in the passphrase dialog into an xml just like
	the reset passphrase dialog.
	
	o 15. Make the IO in loading/saving a wallet into an asynchrynous task
	so as to not block the UI. Some lagging can be seen now, such as when you just
	get into the accounts section and you try to rotate the screen horizontally.
	
	o 16. Make the backgrounds of the grid view in the new currency dialog
	reflect which item is currenly selected.
	
	o 17. Add a nicer display to the dialogs than the default.

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
		
		this.walletManager = new OWWalletManager(this);

		// Everything is held within this main activity layout
		this.setContentView(R.layout.activity_main);

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
		} else if (FragmentType.ACCOUNT_FRAGMENT_TYPE.toString(
				).equals(curSelected)) {
			super.onBackPressed();
		} else {
			OWMainFragmentActivity.this.showFragment(
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
	private void showFragment(FragmentType fragmentType) {
		// Go through menu options and select the right one, note  
		// that not all menu options can be selected
		if (fragmentType.getDrawerMenuView() != null) {
			this.selectSingleDrawerMenuOption(fragmentType.getDrawerMenuView());
		} else {
			ZLog.log("The drawer menu was null, can't select... shouldn't happen2");
		}

		// The tag for fragments of this fragmentType
		String tag = fragmentType.toString();
		// The transaction that will take place to show the new fragment
		FragmentTransaction transaction = 
				this.getSupportFragmentManager().beginTransaction();

		// TODO add animation to transaciton here

		// Make the new fragment of the correct type
		Fragment fragToShow = this.getSupportFragmentManager().findFragmentByTag(tag);
		if (fragToShow == null) {
			// If the fragment doesn't exist yet, make a new one
			fragToShow = fragmentType.getNewFragment(); 
		} else if (fragToShow.isVisible()) {
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
				drawerMenuItem.setIcon(R.drawable.menu_up);
				// If drawer menu is open, close it
				this.menuDrawer.closeDrawer(Gravity.LEFT);
			} else {
				// Set the icon here to avoid extra swapping while 
				// drawer menu is opening
				drawerMenuItem.setIcon(R.drawable.menu_down);
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
					OWMainFragmentActivity.this.showFragment(
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
					OWMainFragmentActivity.this.showFragment(
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
					OWMainFragmentActivity.this.showFragment(
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
					OWMainFragmentActivity.this.showFragment(
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
					OWMainFragmentActivity.this.showFragment(
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
			this.showFragment(FragmentType.ACCOUNT_FRAGMENT_TYPE);
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

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			this.baseFragmentContainer.setTranslationX(moveFactor);
		} else {
			ZLog.log("Older than honeycomb, which we don't support");
		}
	}

	@Override
	public void onDrawerStateChanged(int newState) {
		// Nothing to do
	}

}
