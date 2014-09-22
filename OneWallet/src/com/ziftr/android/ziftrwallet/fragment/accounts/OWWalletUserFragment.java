package com.ziftr.android.ziftrwallet.fragment.accounts;

import android.view.View;

import com.ziftr.android.ziftrwallet.OWWalletManager;
import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.fragment.OWFragment;
import com.ziftr.android.ziftrwallet.util.OWCoin;

public abstract class OWWalletUserFragment extends OWFragment {

	/**
	 * Gives the globally accessible wallet manager. All coin-network related
	 * things should be done through this class as it chooses whether to use
	 * (temporary) bitcoinj or to use the SQLite tables and API (future). 
	 * 
	 * @return the wallet manager
	 */
	protected OWWalletManager getWalletManager() {
		return this.getOWMainActivity().getWalletManager();
	}

	protected OWCoin getSelectedCoin() {
		return this.getOWMainActivity().getSelectedCoin();
	}
	

	public View getHeaderView() {
		return this.getOWMainActivity().findViewById(R.id.walletHeader);
	}
	
	public void showWalletHeader() {
		this.getOWMainActivity().showWalletHeader();
	}
		
}
