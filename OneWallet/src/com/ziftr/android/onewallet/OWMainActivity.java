package com.ziftr.android.onewallet;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v7.app.ActionBarActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.ziftr.android.onewallet.util.ZLog;

public class OWMainActivity extends ActionBarActivity implements DrawerListener {
	
	/** The drawer layout menu. */
	private DrawerLayout menuDrawer;
	/** The drawer layout menu button. */
	private ImageView menuButton;

	/**
	 * Loads up the views and starts the OWHomeFragment 
	 * if necessary. 
	 * 
	 * @param savedInstanceState - The stored bundle
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
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
//		getMenuInflater().inflate(R.menu.main, menu);
//		return true;
		return false;
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
					OWMainActivity.this.closeDrawerIfOpen();
					ZLog.log("Menu item clicked. Drawer should close.");
					// TODO 
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
		}
		
		
		this.menuButton = (ImageView) this.findViewById(R.id.switchTaskMenuButton);
		this.menuButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (OWMainActivity.this.menuDrawer != null) {
					if (!OWMainActivity.this.menuDrawer.isDrawerOpen(Gravity.LEFT)) {
						OWMainActivity.this.menuDrawer.openDrawer(Gravity.LEFT);
					} else {
						OWMainActivity.this.menuDrawer.closeDrawer(Gravity.LEFT);
					}
				}
			}
		});
	}

	@Override
	public void onDrawerClosed(View arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDrawerOpened(View arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDrawerSlide(View arg0, float arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDrawerStateChanged(int arg0) {
		// TODO Auto-generated method stub
		
	}

}
