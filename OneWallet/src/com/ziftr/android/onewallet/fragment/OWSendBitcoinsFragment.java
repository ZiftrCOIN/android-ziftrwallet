package com.ziftr.android.onewallet.fragment;

import java.math.BigDecimal;

import com.ziftr.android.onewallet.util.Fiat;
import com.ziftr.android.onewallet.util.Fiat.Type;


public class OWSendBitcoinsFragment extends OWSendCoinsFragment {

	@Override
	public CharSequence getDefaultFee() {
		return "0.0001";
	}

	@Override
	public String getCoinPrefix() {
		return "BTC";
	}

	@Override
	public BigDecimal getExchangeRateToFiat(Type fiatType) {
		if (fiatType == Fiat.Type.USD) {
			return new BigDecimal(618.34);
		} else if (fiatType == Fiat.Type.EUR) {
			return new BigDecimal(453.84);
		}
		return null;
	}

	@Override
	public int getNumberOfDigitsOfPrecision() {
		return 8;
	}

}