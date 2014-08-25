package com.ziftr.android.onewallet.sqlite;

import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWCoin.Type;

public class OWBitcoinTestnetUsersAddressesSQLiteHelper extends OWUsersAddressesTable {

	public OWBitcoinTestnetUsersAddressesSQLiteHelper() {
		super();
	}

	@Override
	public Type getCoinId() {
		return OWCoin.Type.BTC_TEST; 
	}

}