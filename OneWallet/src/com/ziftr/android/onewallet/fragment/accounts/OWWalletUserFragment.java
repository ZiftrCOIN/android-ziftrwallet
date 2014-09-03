package com.ziftr.android.onewallet.fragment.accounts;

import com.ziftr.android.onewallet.OWWalletManager;
import com.ziftr.android.onewallet.fragment.OWFragment;
import com.ziftr.android.onewallet.util.OWCoinRelative;

// TODO refactor to OWWalletManagerFragment
public abstract class OWWalletUserFragment extends OWFragment implements OWCoinRelative {
	
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
	
}