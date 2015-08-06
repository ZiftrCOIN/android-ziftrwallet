/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.fragment;

import java.util.List;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWPreferences;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.dialog.ZiftrDialogManager;
import com.ziftr.android.ziftrwallet.dialog.ZiftrTaskDialogFragment;
import com.ziftr.android.ziftrwallet.dialog.ZiftrTextDialogFragment;
import com.ziftr.android.ziftrwallet.sqlite.ZWWalletManager;
import com.ziftr.android.ziftrwallet.util.ZLog;

public class ZWNewCurrencyFragment extends ZWFragment implements OnItemClickListener {

	public static final String FRAGMENT_TAG = "new_currency";
	
	public static final String DIALOG_ENTER_PASSWORD_TAG = "new_currency_add_coin_get_password";
	public static final String DIALOG_NEW_ADDRESS = "new_currency_creating_address";
	
	private ZWNewCurrencyListAdapter currencyAdapter;
	
	/** The root view for this application. */
	private View rootView;


	private ListView listView;

	@Override
	public void onResume() {
		super.onResume();
		this.getZWMainActivity().changeActionBar("CURRENCY", true, true, false);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		this.rootView = inflater.inflate(R.layout.currency_get_new_list, container, false);

		boolean includeTestnet = ZWPreferences.getDebugMode();
		List<ZWCoin> unactivatedCoins = this.getZWMainActivity().getWalletManager().getInactiveCoins(includeTestnet);
		
		if (unactivatedCoins.size() <= 0){
			this.rootView.findViewById(R.id.no_currency_message).setVisibility(View.VISIBLE);
		} else {
			this.rootView.findViewById(R.id.no_currency_message).setVisibility(View.GONE);
		}
		
		initializeCurrencyList(unactivatedCoins);
		return this.rootView;
	}

	public void initializeCurrencyList(List<ZWCoin> unactivatedCoins) {
		this.currencyAdapter = new ZWNewCurrencyListAdapter(this.getActivity(), unactivatedCoins);
		this.listView = (ListView) this.rootView.findViewById(R.id.currencyListView);
		listView.setFooterDividersEnabled(false);
		listView.setAdapter(this.currencyAdapter);
		listView.setOnItemClickListener(this);
	}


	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ZWCoin coin = (ZWCoin) listView.getItemAtPosition(position);
		
		// TODO we can probably get rid of this if it's slowing stuff down - unnecessary check 
		// Make sure that this view only has wallets
		if (this.getZWMainActivity().getWalletManager().isCoinActivated(coin)) {
			// Already in list, shouldn't ever get here though because
			// we only show currencies in the dialog which we don't have
			this.getZWMainActivity().onBackPressed();
			return;
		}
		
		String password = ZWPreferences.getCachedPassword();
		if (ZWPreferences.userHasPassword() && password == null) {
			showEnterPasswordDialog(coin);
		}
		else {
			makeNewAccount(coin, password);
		}
		
	}
	
	private void showEnterPasswordDialog(ZWCoin coin) {
		ZiftrTextDialogFragment passwordDialog = new ZiftrTextDialogFragment();
		passwordDialog.setupDialog(R.string.zw_dialog_enter_password);
		passwordDialog.addEmptyTextbox(true);
		passwordDialog.setData(coin);
		passwordDialog.show(getFragmentManager(), DIALOG_ENTER_PASSWORD_TAG);
	}
	
	public void makeNewAccount(final ZWCoin coin, final String password) {

		//now we either have the user's password, or they don't have one set,
		//so we need to contact the server and have a transaction built
		ZiftrTaskDialogFragment creatingAccountFragment = new ZiftrTaskDialogFragment() {
			@Override
			protected boolean doTask() {
				long start = System.currentTimeMillis();
				
				try {
					ZWWalletManager.getInstance().activateCoin(coin, password);
				} 
				catch(Exception e) {
					ZLog.log("Exception activating coin: ", e);
					return false;
				}
				
				long end = System.currentTimeMillis();
				long minTime = 500;
				if ((end - start) < minTime) {
					try {
						Thread.sleep(minTime - (end - start)); //quick sleep so the UI doesn't "flash" the loading message
					} catch (InterruptedException e) {
						//do nothing, just quick sleep
					}
				}
				
				return true;
			}
		};
	
		// If doTask returns true, then which == BUTTON_POSITIVE (and vice versa)
		creatingAccountFragment.setupDialog(R.string.zw_dialog_new_account);
		creatingAccountFragment.setOnClickListener(new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					String toastText = getZWMainActivity().getString(R.string.zw_toast_wallet_created);
					Toast.makeText(ZWNewCurrencyFragment.this.getZWMainActivity(), toastText, Toast.LENGTH_LONG).show();
					ZWNewCurrencyFragment.this.getZWMainActivity().onBackPressed();
				}
				else if (which == DialogInterface.BUTTON_NEGATIVE) {
					ZiftrDialogManager.showSimpleAlert(getFragmentManager(), R.string.zw_dialog_error_in_account_creation);
				}
			}
		});

		creatingAccountFragment.show(getFragmentManager(), DIALOG_NEW_ADDRESS);
	}

}
