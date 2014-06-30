package com.ziftr.android.onewallet;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v7.app.ActionBarActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import com.ziftr.android.onewallet.util.ZLog;

/**
 * This is the main activity of the OneWallet application. It handles
 * the menu drawer and the switching between the different fragments 
 * depending on which task the user selects.
 */
public class OWMainFragmentActivity extends ActionBarActivity implements DrawerListener {

	// TODO
	// o 1. Ability to generate new address upon user request
	// and be able to turn that into a QR code
	// 
	// o 2. Get transaction history for all addresses in wallet and
	// be able to display them.
	// 
	// o 3. Start making layouts.
	// 
	// o 4. Organizing tasks that need to be done and appr. difficulty
	// 
	// o 5. OW to start all of our classes.
	// 
	// o 6. ZiftrUtils and Zlog for static useful methods.
	// ex. ZLog.log("aa", "b"); (get's exception message, as well)
	// also autotags comments with class name and shuts itself off at
	// launch time for release build.
	// 
	// X 7. Move all dialog stuff into dialog package and make dialogs
	// persistent.
	//
	// X 8. Get QR code example working
	// 
	// o 9. Get a list interface on top lefthand corner. Move over to ActionBar
	// instead of 
	//
	// X 10. Get a reset working for the passphrase.
	// 
	// TODO turn header bar into an actionbar
	// Get Fragment switching working with selection in the drawer layout 

	/** The drawer layout menu. */
	private DrawerLayout menuDrawer;
	
	/** The menu for our app. */
	private Menu drawerMenuIcon;

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
		setContentView(R.layout.activity_main);

		// Set up the drawer and the menu button
		this.initializeDrawerLayout();

		// If the app has just been launched (so the fragment
		// doesn't exist yet), we create the main fragment.
		if (savedInstanceState == null) {
			FragmentManager fragmentManager = getSupportFragmentManager();
			OWHomeFragment homeFragment = new OWHomeFragment();
			fragmentManager.beginTransaction().add(
					R.id.oneWalletBaseFragmentHolder, homeFragment).commit();
		} 
	}

	/**
	 * Create the options menu. May want to get rid of this later.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		this.getMenuInflater().inflate(R.menu.ow_action_bar_menu, menu);
		this.drawerMenuIcon = menu;
		return true;
	}

	/**
	 * A convenience method to close the drawer layout
	 * if it is open. Does nothing if it is not open. 
	 */
	private void closeDrawerIfOpen() {
		if (this.menuDrawer.isDrawerOpen(Gravity.LEFT)) {
			this.menuDrawer.closeDrawer(Gravity.LEFT);
		}
	}

	/**
	 * Does all the necessary tasks to set up the drawer layout.
	 * This incudes setting up the menu button so that it opens and
	 * closes the drawer layout appropriately.
	 */
	private void initializeDrawerLayout() {
		this.menuDrawer = 
				(DrawerLayout) this.findViewById(R.id.oneWalletBaseDrawer);

		if (this.menuDrawer != null) {
			this.menuDrawer.setDrawerListener(this);

			OnClickListener menuItemListener = new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					OWMainFragmentActivity.this.closeDrawerIfOpen();
					ZLog.log("Menu item clicked. Drawer should close.");
					// TODO start the opening of the the correct fragment here
				}
			};

			// Setup the menu choices
			View accountsMenuButton = 
					this.findViewById(R.id.menuDrawerAccountsLayout);
			accountsMenuButton.setOnClickListener(menuItemListener);

			View exchangeMenuButton = 
					this.findViewById(R.id.menuDrawerExchangeLayout);
			exchangeMenuButton.setOnClickListener(menuItemListener);

			View settingsMenuButton = 
					this.findViewById(R.id.menuDrawerSettingsLayout);
			settingsMenuButton.setOnClickListener(menuItemListener);

			View aboutMenuButton = 
					this.findViewById(R.id.menuDrawerAboutLayout);
			aboutMenuButton.setOnClickListener(menuItemListener);

			View contactMenuButton = 
					this.findViewById(R.id.menuDrawerContactLayout);
			contactMenuButton.setOnClickListener(menuItemListener);
		} else {
			ZLog.log("drawerMenu was null. ?");
		}
		
	}

	/**
	 * Gives a boolean that describes whether or not the drawer menu is
	 * currenly open from the left side of the screen. 
	 * 
	 * @return a boolean that describes whether or not the drawer menu is
	 * currenly open
	 */
	private boolean drawerMenuIsOpen() {
		return OWMainFragmentActivity.this.menuDrawer.isDrawerOpen(Gravity.LEFT);
	}

	/**
	 * Called when the drawer menu item is clicked.
	 */
	public void onDrawerMenuItemClicked() {
		MenuItem drawerMenuItem = this.drawerMenuIcon.findItem(R.id.switchTaskMenuButton);
		if (this.menuDrawer != null) {
			if (this.drawerMenuIsOpen()) {
				// If drawer menu is open, close it
				this.menuDrawer.closeDrawer(Gravity.LEFT);
				// Set the icon here to avoid extra swapping while 
				// drawer menu is closing
				drawerMenuItem.setIcon(R.drawable.icon_menu);
			} else {
				// If drawer menu is closed, open it
				this.menuDrawer.openDrawer(Gravity.LEFT);
				// Set the icon here to avoid extra swapping while 
				// drawer menu is opening
				drawerMenuItem.setIcon(R.drawable.icon_menu_pressed);
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the aciton bar items
		switch(item.getItemId()) {
		case R.id.switchTaskMenuButton:
			this.onDrawerMenuItemClicked();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onDrawerClosed(View arg0) {
		this.drawerMenuIcon.findItem(R.id.switchTaskMenuButton
				).setIcon(R.drawable.icon_menu_statelist);
	}

	@Override
	public void onDrawerOpened(View arg0) {
		this.drawerMenuIcon.findItem(R.id.switchTaskMenuButton
				).setIcon(R.drawable.icon_menu_statelist_reverse);
	}

	@Override
	public void onDrawerSlide(View arg0, float arg1) {
		// Nothing to do
	}

	@Override
	public void onDrawerStateChanged(int newState) {
		// Nothing to do
	}

}
