package com.ziftr.android.ziftrwallet.tests;


import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ziftr.android.ziftrwallet.OWMainFragmentActivity;
import com.ziftr.android.ziftrwallet.OWMainFragmentActivity.FragmentType;
import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.fragment.accounts.OWNewCurrencyListItem;
import com.ziftr.android.ziftrwallet.sqlite.OWSQLiteOpenHelper;
import com.ziftr.android.ziftrwallet.util.OWCoin;
import com.ziftr.android.ziftrwallet.util.OWPreferencesUtils;

public class MainActivitytest extends ActivityInstrumentationTestCase2<OWMainFragmentActivity> {

	private OWMainFragmentActivity mActivity;

	public MainActivitytest(){
		super(OWMainFragmentActivity.class);

	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		setActivityInitialTouchMode(false);
		mActivity = getActivity();
		//clear user prefs
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		Editor editor = settings.edit();
		editor.clear();
		editor.commit();
		//clear all wallets
		//mActivity.getWalletManager().closeAllSetupWallets();
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
	}

	public void testPreconditions(){
		assertTrue(mActivity.getWalletManager().getAllSetupWalletTypes().size() == 0);
		assertFalse(OWPreferencesUtils.getDisabledName(mActivity));
		assertNull(OWPreferencesUtils.getUserName(mActivity));
		assertTrue(mActivity.getSupportFragmentManager().getBackStackEntryCount() == 0);
	}

	public void testInitialVisibility(){
		//mActivity.getWalletManager().closeAllSetupWallets();
		LinearLayout msg = (LinearLayout) mActivity.findViewById(R.id.add_currency_message);
		assertTrue(msg.getVisibility() == View.VISIBLE || mActivity.getWalletManager().getAllSetupWalletTypes().size() != 0);
		TextView total = (TextView) mActivity.findViewById(R.id.total_label);
		assertTrue(total.getVisibility() == View.GONE || mActivity.getWalletManager().getAllSetupWalletTypes().size() != 0);
	}
	
	@UiThreadTest
	public void testAddWallet() {
		assertTrue(mActivity.getWalletManager().getAllSetupWalletTypes().size() == 0);
		createBTCwallet();
		assertTrue(mActivity.getWalletManager().getAllSetupWalletTypes().size() == 1);
		//remove wallet
		mActivity.getWalletManager().updateTableActivitedStatus(OWCoin.BTC, OWSQLiteOpenHelper.DEACTIVATED);
		//mActivity.getWalletManager().closeWallet(OWCoin.BTC);
	}
	
	@UiThreadTest
	public void testDrawerBackStack(){
		FragmentManager fm = mActivity.getSupportFragmentManager();
		assertEquals(fm.getBackStackEntryCount(), 0);
		mActivity.findViewById(R.id.menuDrawerTermsLayout).performClick();
		mActivity.onBackPressed();
		assertTrue(fm.findFragmentByTag(FragmentType.ACCOUNT_FRAGMENT_TYPE.toString()).isVisible());
		assertEquals(fm.getBackStackEntryCount(), 0);
		mActivity.findViewById(R.id.menuDrawerSettingsLayout).performClick();
		assertEquals(fm.getBackStackEntryCount(), 0);
		mActivity.findViewById(R.id.menuDrawerSecurityLayout).performClick();
		assertEquals(fm.getBackStackEntryCount(), 0);
		mActivity.onBackPressed();
		assertTrue(fm.findFragmentByTag(FragmentType.ACCOUNT_FRAGMENT_TYPE.toString()).isVisible());
		mActivity.findViewById(R.id.menuDrawerAboutLayout).performClick();
		assertEquals(fm.getBackStackEntryCount(), 0);
		mActivity.findViewById(R.id.menuDrawerAccountsLayout).performClick();
		assertEquals(fm.getBackStackEntryCount(), 0);
	}
	
	@UiThreadTest
	public void testCreateAddress() throws InterruptedException{
		createBTCwallet();
		mActivity.openWalletView(OWCoin.BTC);
		mActivity.openReceiveCoinsView();
	}
	
	@UiThreadTest
	private void createBTCwallet(){
		//add wallet
		ImageView addCurrency = (ImageView) mActivity.findViewById(R.id.actionBarAddCurrency);
		addCurrency.performClick();
		OWNewCurrencyListItem btc = new OWNewCurrencyListItem(OWCoin.BTC);
		mActivity.addNewCurrency(btc);
	}

}
