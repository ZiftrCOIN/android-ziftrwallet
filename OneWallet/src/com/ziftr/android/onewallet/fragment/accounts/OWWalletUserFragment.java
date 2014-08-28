package com.ziftr.android.onewallet.fragment.accounts;

import java.io.File;

import com.google.bitcoin.core.Wallet;
import com.ziftr.android.onewallet.OWMainFragmentActivity;
import com.ziftr.android.onewallet.OWWalletManager;
import com.ziftr.android.onewallet.fragment.OWFragment;
import com.ziftr.android.onewallet.util.OWCoinRelative;

public abstract class OWWalletUserFragment extends OWFragment implements OWCoinRelative {
	
	/** 
	 * Gets the wallet from the wallet manager, which is stored in the activity.
	 * Note: the activity using this fragment must be a {@link OWMainFragmentActivity}.
	 * 
	 * @return The wallet of type getCoinId()
	 */
	protected Wallet getWallet() {
		OWWalletManager m = ((OWMainFragmentActivity) this.getActivity()
				).getWalletManager();
		if (m != null) {
			return m.getWallet(getCoinId());
		}

		return null;
	}
	
	/**
	 * Gets the wallet file from the wallet manager, which is store din the activity. 
	 * Note: the activity using this fragment must be a {@link OWMainFragmentActivity}.
	 * 
	 * @return The file where the wallet of type getCoinId() is stored, if 
	 * there is one. Returns null if no wallet file for the type exists. 
	 */
	protected File getWalletFile() {
		OWWalletManager m = ((OWMainFragmentActivity) this.getActivity()
				).getWalletManager();
		if (m != null) {
			return m.getWalletFile(getCoinId());
		}
		return null;
	}
	
}