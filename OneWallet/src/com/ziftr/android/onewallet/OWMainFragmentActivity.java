package com.ziftr.android.onewallet;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v7.app.ActionBarActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import com.ziftr.android.onewallet.fragment.OWAccountsFragment;
import com.ziftr.android.onewallet.util.ZLog;

/**
 * This is the main activity of the OneWallet application. It handles
 * the menu drawer and the switching between the different fragments 
 * depending on which task the user selects.
 *                                                                                           
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
	
	X 8. Get QR code example working
	
	X 9. Get a list interface on top right corner. Move over to ActionBar
	instead of 
	
	X 10. Get a reset working for the passphrase.
	
	o 11. Get Fragment switching working with selection in the drawer layout
	
	*/
	
	/** The drawer layout menu. */
	private DrawerLayout menuDrawer;

	/** The menu for our app. */
	private Menu actionBarMenu;
	
	/** The fragment in the accounts section, selected from drawer menu. */
	private Fragment accountsFragment;
	/** The fragment in the exchange section, selected from drawer menu. */
	private Fragment exchangeFragment;
	/** The fragment in the settings section, selected from drawer menu. */
	private Fragment settingsFragment;
	/** The fragment in the about section, selected from drawer menu. */
	private Fragment aboutFragment;
	/** The fragment in the contact section, selected from drawer menu. */
	private Fragment contactFragment;
	
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
			this.accountsFragment = new OWAccountsFragment();
			fragmentManager.beginTransaction().add(
					R.id.oneWalletBaseFragmentHolder, this.accountsFragment).commit();
		} 
	}

	/**
	 * Create the options menu. May want to get rid of this later.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		this.getMenuInflater().inflate(R.menu.ow_action_bar_menu, menu);
		this.actionBarMenu = menu;
		return true;
	}

	/**
	 * A convenience method to close the drawer layout
	 * if it is open. Does nothing if it is not open. 
	 */
	private void toggleDrawerPosition() {
//		if (this.menuDrawer.isDrawerOpen(Gravity.LEFT)) {
//			this.menuDrawer.closeDrawer(Gravity.LEFT);
//		}
		
		MenuItem drawerMenuItem = this.actionBarMenu.findItem(R.id.switchTaskMenuButton);
		
		if (this.menuDrawer != null) {
			if (this.drawerMenuIsOpen()) {
				// Set the icon here to avoid extra swapping while 
				// drawer menu is closing
				drawerMenuItem.setIcon(R.drawable.icon_menu);
				// If drawer menu is open, close it
				this.menuDrawer.closeDrawer(Gravity.LEFT);
			} else {
				// Set the icon here to avoid extra swapping while 
				// drawer menu is opening
				drawerMenuItem.setIcon(R.drawable.icon_menu_pressed);
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
	 */
	private void initializeDrawerLayout() {
		this.menuDrawer = 
				(DrawerLayout) this.findViewById(R.id.oneWalletBaseDrawer);

		if (this.menuDrawer != null) {
			this.menuDrawer.setDrawerListener(this);

			// Setup the menu choices
			View accountsMenuButton = 
					this.findViewById(R.id.menuDrawerAccountsLayout);
			accountsMenuButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View clickedView) {
					OWMainFragmentActivity.this.onAnyDrawerMenuItemClicked(clickedView);
				}
			});

			View exchangeMenuButton = 
					this.findViewById(R.id.menuDrawerExchangeLayout);
			exchangeMenuButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View clickedView) {
					OWMainFragmentActivity.this.onAnyDrawerMenuItemClicked(clickedView);
				}
			});

			View settingsMenuButton = 
					this.findViewById(R.id.menuDrawerSettingsLayout);
			settingsMenuButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View clickedView) {
					OWMainFragmentActivity.this.onAnyDrawerMenuItemClicked(clickedView);
				}
			});

			View aboutMenuButton = 
					this.findViewById(R.id.menuDrawerAboutLayout);
			aboutMenuButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View clickedView) {
					OWMainFragmentActivity.this.onAnyDrawerMenuItemClicked(clickedView);
				}
			});

			View contactMenuButton = 
					this.findViewById(R.id.menuDrawerContactLayout);
			contactMenuButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View clickedView) {
					OWMainFragmentActivity.this.onAnyDrawerMenuItemClicked(clickedView);
				}
			});
		} else {
			ZLog.log("drawerMenu was null. ?");
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
		
		// Deselect all the other views
		for (View selectionView : this.getDrawerMenuItems()) {
			selectionView.setSelected(false);
		}
		
		// Mark this one as selected to have green selector view
		// next to it in the drawer layout
		selectedView.setSelected(true);
		
		// Menu must have been open, so calling this will just 
		// result in a close of the menu.
		this.toggleDrawerPosition();;
		
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
	 * there is the menu in the action bar that has a menu in it, of 
	 * which one of the items is a clickable that opens the drawer menu.
	 */
	private void onDrawerMenuActionBarItemClicked() {
		this.toggleDrawerPosition();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the aciton bar items
		switch(item.getItemId()) {
		case R.id.switchTaskMenuButton:
			this.onDrawerMenuActionBarItemClicked();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onDrawerClosed(View arg0) {
		this.actionBarMenu.findItem(R.id.switchTaskMenuButton
				).setIcon(R.drawable.icon_menu_statelist);
	}

	@Override
	public void onDrawerOpened(View arg0) {
		this.actionBarMenu.findItem(R.id.switchTaskMenuButton
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
