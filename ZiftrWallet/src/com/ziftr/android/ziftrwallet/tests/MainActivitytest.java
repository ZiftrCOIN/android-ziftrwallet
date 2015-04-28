package com.ziftr.android.ziftrwallet.tests;


import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWMainFragmentActivity;
import com.ziftr.android.ziftrwallet.ZWMainFragmentActivity.FragmentType;
import com.ziftr.android.ziftrwallet.ZWPreferences;
import com.ziftr.android.ziftrwallet.ZWWalletManager;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWDefaultCoins;
import com.ziftr.android.ziftrwallet.fragment.ZWReceiveCoinsFragment;

public class MainActivitytest extends ActivityInstrumentationTestCase2<ZWMainFragmentActivity> {

	private ZWMainFragmentActivity mActivity;
	private ZWWalletManager manager;

	public MainActivitytest(){
		super(ZWMainFragmentActivity.class);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		setActivityInitialTouchMode(false);
		mActivity = getActivity();
		manager = mActivity.getWalletManager();
		//clear user prefs
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		Editor editor = settings.edit();
		editor.clear();
		editor.commit();
		//clear all wallets
		for (ZWCoin coin : ZWCoin.getAllCoins()){
			manager.deactivateCoin(coin);
		}
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
	}

	public void testPreconditions(){
		assertTrue(mActivity.getWalletManager().getActivatedCoins().size() == 0);
		assertFalse(ZWPreferences.getDisabledName());
		assertNull(ZWPreferences.getUserName());
		assertTrue(mActivity.getSupportFragmentManager().getBackStackEntryCount() == 0);
	}

	public void testInitialVisibility(){
		//mActivity.getWalletManager().closeAllSetupWallets();
		LinearLayout msg = (LinearLayout) mActivity.findViewById(R.id.add_currency_message);
		assertTrue(msg.getVisibility() == View.VISIBLE || mActivity.getWalletManager().getActivatedCoins().size() != 0);
		TextView total = (TextView) mActivity.findViewById(R.id.total_label);
		assertTrue(total.getVisibility() == View.GONE || mActivity.getWalletManager().getActivatedCoins().size() != 0);
	}

	@UiThreadTest
	public void testAddWallet() {
		assertTrue(mActivity.getWalletManager().getActivatedCoins().size() == 0);
		ZWCoin btcDefault = ZWDefaultCoins.getDefaultCoins().get(0);
		createWallet(btcDefault);
		assertTrue(mActivity.getWalletManager().getActivatedCoins().size() == 1);
		//remove wallet
		mActivity.getWalletManager().deactivateCoin(btcDefault);
		assertTrue(mActivity.getWalletManager().getActivatedCoins().size() == 0);

	}

	@UiThreadTest
	public void testDrawerBackStack(){
		FragmentManager fm = mActivity.getSupportFragmentManager();
		assertEquals(0, fm.getBackStackEntryCount());
		mActivity.findViewById(R.id.menuDrawerTermsLayout).performClick();
		mActivity.onBackPressed();
		assertTrue(fm.findFragmentByTag(FragmentType.ACCOUNT_FRAGMENT_TYPE.toString()).isVisible());
		assertEquals(0, fm.getBackStackEntryCount());
		mActivity.findViewById(R.id.menuDrawerSettingsLayout).performClick();
		assertEquals(0, fm.getBackStackEntryCount());
		mActivity.findViewById(R.id.menuDrawerSecurityLayout).performClick();
		assertEquals(0, fm.getBackStackEntryCount());
		mActivity.onBackPressed();
		assertTrue(fm.findFragmentByTag(FragmentType.ACCOUNT_FRAGMENT_TYPE.toString()).isVisible());
		mActivity.findViewById(R.id.menuDrawerAboutLayout).performClick();
		assertEquals(0, fm.getBackStackEntryCount());
		mActivity.findViewById(R.id.menuDrawerAccountsLayout).performClick();
		assertEquals(0, fm.getBackStackEntryCount());
	}

	
	//this test is testing use of a method only used by this test
	//which means it's either totally pointless, or isn't testing proper data anyway
	//TODO fix this i guess?
	/****
	public void testCreateReceivingAddress(){
		createWallet(ZWCoin.DOGE);
		int currentAddresses = manager.readAllVisibleAddresses(ZWCoin.DOGE).size();
		manager.createReceivingAddress(null, ZWCoin.DOGE, ZWReceivingAddressesTable.VISIBLE_TO_USER);
		ZWAddress dogeAddress = manager.readAllVisibleAddresses(ZWCoin.DOGE).get(currentAddresses);
		assertTrue(dogeAddress.getAddress().charAt(0) == 'D');
		//size of doge visible addresses in db should be +1
		assertEquals(currentAddresses + 1, manager.readAllVisibleAddresses(ZWCoin.DOGE).size());
		manager.createReceivingAddress(null, ZWCoin.DOGE, ZWReceivingAddressesTable.HIDDEN_FROM_USER);
		//size of doge visible addresses in db should remain the same after creating a hidden address
		assertEquals(currentAddresses + 1, manager.readAllVisibleAddresses(ZWCoin.DOGE).size());
	}
	*****/

	
	@SuppressLint("NewApi")
	@UiThreadTest
	public void testQRCodeVisiblity() throws InterruptedException{
		ZWCoin btcTestDefault = ZWDefaultCoins.getDefaultCoins().get(3);
		createWallet(btcTestDefault);
		mActivity.openWalletView(btcTestDefault);
		mActivity.openReceiveCoinsView(null);
		mActivity.getSupportFragmentManager().executePendingTransactions();
		View receiveScreen  = mActivity.getSupportFragmentManager().findFragmentByTag(ZWReceiveCoinsFragment.FRAGMENT_TAG).getView();
		View qrCodeContainer = receiveScreen.findViewById(R.id.generateAddressQrCodeContainer);
		EditText label = (EditText) receiveScreen.findViewById(R.id.addressName).findViewById(R.id.customEditText);
		ImageView addAddress = (ImageView) receiveScreen.findViewById(R.id.generateNewAddressForLabel);
		ImageView qrCodeImageView = (ImageView) receiveScreen.findViewById(R.id.generateAddressQrCodeImageView);
		assertEquals((float) 0.5 , qrCodeContainer.getAlpha());
		label.setText("doges");
		assertEquals((float) 1.0 , qrCodeContainer.getAlpha());
		addAddress.performClick();
		Point screenSize = new Point();
		mActivity.getWindowManager().getDefaultDisplay().getSize(screenSize);
		assertTrue(qrCodeImageView.getWidth() <= screenSize.x);
		assertTrue(qrCodeImageView.getHeight() <= screenSize.y);
	}

	
	/*******
	//test if creating a request for sending coins provides response with to sign attributes
	@UiThreadTest
	public void testSendCoinsRequest() throws InterruptedException{
		createWallet(ZWCoin.BTC);
		ZWAddress spendFrom = manager.createReceivingAddress(null, ZWCoin.BTC, ZWReceivingAddressesTable.VISIBLE_TO_USER);
		final List<String> input = new ArrayList<String>();
		input.add(spendFrom.toString());
		final String sendToAddress = "1DBb6HuozwS6esCGiaM5iB4xxoK759BYFW";
		final BigInteger sendingAmount = BigInteger.ZERO;

		ZiftrUtils.runOnNewThread(new Runnable() {
			@Override
			public void run() {
				ZiftrNetRequest request = ZWDataSyncHelper.sendCoinsRequest(ZWCoin.BTC, BigInteger.ZERO, sendingAmount, input, sendToAddress, null);
				String response = request.sendAndWait();
				try {
					JSONObject res = new JSONObject(response);
					assertTrue(res.has("fee"));
					assertTrue(res.has("block_hash"));
					assertTrue(res.has("hash"));
					assertTrue(res.has("block_height"));
					assertTrue(res.has("inputs"));
					assertTrue(res.has("outputs"));
					assertTrue(res.has("to_sign"));
					sendCoinTxn(res, input, sendToAddress, sendingAmount);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});	
		//there should be one or more hidden unspent addresses to put change in
		Thread.sleep(500);
		assertTrue(manager.readHiddenUnspentAddresses(ZWCoin.BTC).size() >= 1);
	}
	*********/

	//test sending signed txn 
	/***
	public void sendCoinTxn(JSONObject req, List<String> inputs, String sendToAddr, BigInteger sendingAmount){
		ArrayList<String> addresses = new ArrayList<String>();
		for (String a : inputs){
			addresses.add(a);
		}
		addresses.add(sendToAddr);
		try {
			ZWDataSyncHelper.sendCoins(ZWCoin.BTC, req, sendingAmount, addresses);
			ZLog.log("Abbbb");
			assertTrue(manager.readAddress(ZWCoin.BTC, "1DBb6HuozwS6esCGiaM5iB4xxoK759BYFW", false)!=null);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	***/
	

	//this is another test for a method that no longer exists
	//TODO -re-implement transaction creation test
	/***
	@UiThreadTest
	public void testCreateTransaction(){
		createWallet(ZWCoin.BTC);
		List<String> addresses = new ArrayList<String>();
		addresses.add("1HbMfYui17L5m6sAy3L3WXAtf2P32bxJXq");
		int numTxn = manager.readAllTransactions(ZWCoin.BTC).size();
		manager.createTransaction(ZWCoin.BTC, BigInteger.ONE, BigInteger.ONE, addresses, new ZWSha256Hash(randomishHexHash()), "Halloween Costume", 3, System.currentTimeMillis());
		mActivity.openWalletView(ZWCoin.BTC);
		mActivity.getSupportFragmentManager().executePendingTransactions();
		View BTCwallet  = mActivity.getSupportFragmentManager().findFragmentByTag(ZWTags.WALLET_FRAGMENT).getView();
		ListView txns = (ListView) BTCwallet.findViewById(R.id.txListView);
		//There should be 1 more txn than numTxn + 2 dividers
		assertEquals(numTxn+3, txns.getCount());
	}
	***/

	private void createWallet(ZWCoin coin){
		//add wallet
		mActivity.addNewCurrency(coin);
	}

	

}
