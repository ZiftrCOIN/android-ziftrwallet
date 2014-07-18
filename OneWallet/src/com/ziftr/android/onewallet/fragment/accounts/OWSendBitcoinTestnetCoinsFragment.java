package com.ziftr.android.onewallet.fragment.accounts;

import java.math.BigDecimal;

import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWFiat;
import com.ziftr.android.onewallet.util.OWFiat.Type;


public class OWSendBitcoinTestnetCoinsFragment extends OWSendCoinsFragment {

	@Override
	public OWCoin.Type getCoinId() {
		return OWCoin.Type.BTC_TEST;
	}

	@Override
	public BigDecimal getExchangeRateToFiat(Type fiatType) {
		// Not actually 1, just doing this to avoid errors for testing
		if (fiatType == OWFiat.Type.USD) {
			return new BigDecimal("1");
		} else if (fiatType == OWFiat.Type.EUR) {
			return new BigDecimal("1");
		}
		return null;
	}

	@Override
	public boolean addressIsValid(String address) {
		return address != null && 27 <= address.length() && address.length() <= 34; 
	}

}