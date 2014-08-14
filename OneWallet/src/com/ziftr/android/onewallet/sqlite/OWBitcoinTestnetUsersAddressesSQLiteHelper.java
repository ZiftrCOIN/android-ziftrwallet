package com.ziftr.android.onewallet.sqlite;

import android.content.Context;

import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWCoin.Type;

public class OWBitcoinTestnetUsersAddressesSQLiteHelper extends OWUsersAddressesSQLiteHelper {

	public OWBitcoinTestnetUsersAddressesSQLiteHelper(Context context) {
		super(context);
	}

	@Override
	public Type getCoinId() {
		return OWCoin.Type.BTC_TEST; 
	}

}