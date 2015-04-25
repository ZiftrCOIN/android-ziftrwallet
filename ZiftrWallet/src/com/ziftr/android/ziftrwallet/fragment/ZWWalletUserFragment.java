package com.ziftr.android.ziftrwallet.fragment;

import android.view.View;

import com.ziftr.android.ziftrwallet.ZWWalletManager;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.network.ZWDataSyncHelper;

public abstract class ZWWalletUserFragment extends ZWFragment {
	
	protected View walletHeader;
	/**
	 * Gives the globally accessible wallet manager. All coin-network related
	 * things should be done through this class as it chooses whether to use
	 * (temporary) bitcoinj or to use the SQLite tables and API (future). 
	 * 
	 * @return the wallet manager
	 */
	protected ZWWalletManager getWalletManager() {
		return this.getZWMainActivity().getWalletManager();
	}

	protected ZWCoin getSelectedCoin() {
		return this.getZWMainActivity().getSelectedCoin();
	}
	
	public void populateWalletHeader(View v) {
		this.walletHeader = v;
		this.getZWMainActivity().populateWalletHeaderView(v);
	}
	
	@Override
	public void onDataUpdated(){
		super.onDataUpdated();
		if (this.walletHeader != null){
			this.getZWMainActivity().updateWalletHeaderView(this.walletHeader);
		}
	}
	
	@Override
	public void refreshData(final boolean autorefresh) {
		ZWDataSyncHelper.updateTransactionHistory(getZWMainActivity().getSelectedCoin(), autorefresh);
	}

}
