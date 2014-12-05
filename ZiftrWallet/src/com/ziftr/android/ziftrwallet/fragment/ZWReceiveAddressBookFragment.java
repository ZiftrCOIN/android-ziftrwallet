package com.ziftr.android.ziftrwallet.fragment;

import java.util.List;

import android.view.View;

import com.ziftr.android.ziftrwallet.crypto.ZWAddress;

public class ZWReceiveAddressBookFragment extends ZWAddressBookFragment {

	@Override
	protected List<ZWAddress> getDisplayAddresses() {
		return this.getWalletManager().getAllVisibleAddresses(getSelectedCoin());
	}

}
