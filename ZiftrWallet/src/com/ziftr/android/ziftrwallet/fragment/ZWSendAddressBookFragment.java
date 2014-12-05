package com.ziftr.android.ziftrwallet.fragment;

import java.util.List;

import android.view.View;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.crypto.ZWAddress;

public class ZWSendAddressBookFragment extends ZWAddressBookFragment {

	@Override
	protected List<ZWAddress> getDisplayAddresses() {
		return this.getWalletManager().getAllSendAddresses(getSelectedCoin());
	}
}
