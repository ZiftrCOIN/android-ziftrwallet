package com.ziftr.android.ziftrwallet.tests;


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
import android.widget.ListView;
import android.widget.TextView;

import com.ziftr.android.ziftrwallet.OWMainFragmentActivity;
import com.ziftr.android.ziftrwallet.OWMainFragmentActivity.FragmentType;
import com.ziftr.android.ziftrwallet.OWWalletManager;
import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.crypto.OWAddress;
import com.ziftr.android.ziftrwallet.crypto.OWSha256Hash;
import com.ziftr.android.ziftrwallet.fragment.accounts.OWNewCurrencyListItem;
import com.ziftr.android.ziftrwallet.sqlite.OWReceivingAddressesTable;
import com.ziftr.android.ziftrwallet.sqlite.OWSQLiteOpenHelper;
import com.ziftr.android.ziftrwallet.util.OWCoin;
import com.ziftr.android.ziftrwallet.util.OWPreferencesUtils;
import com.ziftr.android.ziftrwallet.util.OWTags;

public class MainActivitytest extends ActivityInstrumentationTestCase2<OWMainFragmentActivity> {

	private OWMainFragmentActivity mActivity;
	private OWWalletManager manager;

	public MainActivitytest(){
		super(OWMainFragmentActivity.class);
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
		for (OWCoin type : OWCoin.TYPES){
			manager.updateTableActivatedStatus(type, OWSQLiteOpenHelper.DEACTIVATED);
		}
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
	}

	public void testPreconditions(){
		assertTrue(mActivity.getWalletManager().getAllSetupWalletTypes().size() == 0);
		assertFalse(OWPreferencesUtils.getDisabledName());
		assertNull(OWPreferencesUtils.getUserName());
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
		createWallet(OWCoin.BTC);
		assertTrue(mActivity.getWalletManager().getAllSetupWalletTypes().size() == 1);
		//remove wallet
		mActivity.getWalletManager().updateTableActivatedStatus(OWCoin.BTC, OWSQLiteOpenHelper.DEACTIVATED);
		assertTrue(mActivity.getWalletManager().getAllSetupWalletTypes().size() == 0);

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

	@UiThreadTest
	public void testCreateReceivingAddress(){
		createWallet(OWCoin.DOGE);
		int currentAddresses = manager.readAllVisibleAddresses(OWCoin.DOGE).size();
		manager.createReceivingAddress(null, OWCoin.DOGE, OWReceivingAddressesTable.VISIBLE_TO_USER);
		OWAddress dogeAddress = manager.readAllVisibleAddresses(OWCoin.DOGE).get(currentAddresses);
		assertTrue(dogeAddress.getAddress().charAt(0) == 'D');
		//size of doge visible addresses in db should be +1
		assertEquals(currentAddresses + 1, manager.readAllVisibleAddresses(OWCoin.DOGE).size());
		manager.createReceivingAddress(null, OWCoin.DOGE, OWReceivingAddressesTable.HIDDEN_FROM_USER);
		//size of doge visible addresses in db should remain the same after creating a hidden address
		assertEquals(currentAddresses + 1, manager.readAllVisibleAddresses(OWCoin.DOGE).size());
	}

	@SuppressLint("NewApi")
	@UiThreadTest
	public void testQRCodeVisiblity() throws InterruptedException{
		createWallet(OWCoin.BTC_TEST);
		mActivity.openWalletView(OWCoin.BTC_TEST);
		mActivity.openReceiveCoinsView(null);
		mActivity.getSupportFragmentManager().executePendingTransactions();
		View receiveScreen  = mActivity.getSupportFragmentManager().findFragmentByTag(OWTags.RECIEVE_FRAGMENT).getView();
		View qrCodeContainer = receiveScreen.findViewById(R.id.generateAddressQrCodeContainer);
		EditText label = (EditText) receiveScreen.findViewById(R.id.addressName).findViewWithTag(OWTags.OW_EDIT_TEXT);
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
		createWallet(OWCoin.BTC);
		OWAddress spendFrom = manager.createReceivingAddress(null, OWCoin.BTC, OWReceivingAddressesTable.VISIBLE_TO_USER);
		final List<String> input = new ArrayList<String>();
		input.add(spendFrom.toString());
		final String sendToAddress = "1DBb6HuozwS6esCGiaM5iB4xxoK759BYFW";
		final BigInteger sendingAmount = BigInteger.ZERO;

		ZiftrUtils.runOnNewThread(new Runnable() {
			@Override
			public void run() {
				ZiftrNetRequest request = OWDataSyncHelper.sendCoinsRequest(OWCoin.BTC, BigInteger.ZERO, sendingAmount, input, sendToAddress, null);
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
		assertTrue(manager.readHiddenUnspentAddresses(OWCoin.BTC).size() >= 1);
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
			OWDataSyncHelper.sendCoins(OWCoin.BTC, req, sendingAmount, addresses);
			ZLog.log("Abbbb");
			assertTrue(manager.readAddress(OWCoin.BTC, "1DBb6HuozwS6esCGiaM5iB4xxoK759BYFW", false)!=null);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	***/
	

	@UiThreadTest
	public void testCreateTransaction(){
		createWallet(OWCoin.BTC);
		List<String> addresses = new ArrayList<String>();
		addresses.add("1HbMfYui17L5m6sAy3L3WXAtf2P32bxJXq");
		int numTxn = manager.readAllTransactions(OWCoin.BTC).size();
		manager.createTransaction(OWCoin.BTC, BigInteger.ONE, BigInteger.ONE, addresses, new OWSha256Hash(randomishHexHash()), "Halloween Costume", 3, System.currentTimeMillis());
		mActivity.openWalletView(OWCoin.BTC);
		mActivity.getSupportFragmentManager().executePendingTransactions();
		View BTCwallet  = mActivity.getSupportFragmentManager().findFragmentByTag(OWTags.WALLET_FRAGMENT).getView();
		ListView txns = (ListView) BTCwallet.findViewById(R.id.txListView);
		//There should be 1 more txn than numTxn + 2 dividers
		assertEquals(numTxn+3, txns.getCount());
	}

	private void createWallet(OWCoin type){
		//add wallet
		OWNewCurrencyListItem coin = new OWNewCurrencyListItem(type);
		mActivity.addNewCurrency(coin);
	}

	private String randomishHexHash(){
		StringBuilder sb = new StringBuilder();
		Random r = new Random();
		for (int i=0; i<64; i++){
			int rand = r.nextInt(16);
			if (rand < 6){
				char c = (char) (rand + 97);
				sb.append(c);
			} else {
				sb.append(r.nextInt(10));
			}
		}
		return sb.toString();
	}

}
