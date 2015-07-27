/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.fragment;

import java.util.List;

import com.ziftr.android.ziftrwallet.crypto.ZWReceivingAddress;

public class ZWReceiveAddressBookFragment extends ZWAddressBookFragment<ZWReceivingAddress> {

	@Override
	protected List<ZWReceivingAddress> getDisplayAddresses() {
		return this.getWalletManager().getAllVisibleAddresses(getSelectedCoin());
	}

}
