package com.ziftr.android.onewallet.fragment.accounts;

import java.math.BigDecimal;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.ziftr.android.onewallet.OWWalletManager;
import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.fragment.OWFragment;
import com.ziftr.android.onewallet.sqlite.OWSQLiteOpenHelper;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWConverter;
import com.ziftr.android.onewallet.util.OWFiat;
import com.ziftr.android.onewallet.util.OWUtils;

// TODO refactor to OWWalletManagerFragment
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
	
	protected OWCoin getCurSelectedCoinType() {
		return this.getOWMainActivity().getCurSelectedCoinType();
	}
	
	protected void initializeWalletHeaderView() {
		OWCoin coinId = this.getOWMainActivity().getCurSelectedCoinType();
		
		View headerView = this.getOWMainActivity().findViewById(R.id.walletHeader);
		headerView.setVisibility(View.VISIBLE);
		ImageView coinLogo = (ImageView) (headerView.findViewById(R.id.leftIcon));
		Drawable coinImage = this.getActivity().getResources().getDrawable(
				coinId.getLogoResId());
		coinLogo.setImageDrawable(coinImage);

		TextView coinTitle = (TextView) headerView.findViewById(R.id.topLeftTextView);
		coinTitle.setText(coinId.getLongTitle());

		TextView coinUnitPriceInFiatTextView = (TextView) 
				headerView.findViewById(R.id.bottomLeftTextView);
		BigDecimal unitPriceInFiat = OWConverter.convert(
				BigDecimal.ONE, coinId, OWFiat.USD);
		coinUnitPriceInFiatTextView.setText(OWFiat.USD.getSymbol() + 
				OWFiat.formatFiatAmount(OWFiat.USD, unitPriceInFiat).toPlainString());

		TextView walletBalanceTextView = (TextView) 
				headerView.findViewById(R.id.topRightTextView);
		BigDecimal walletBallance = OWUtils.bigIntToBigDec(coinId, 
				getWalletManager().getWalletBalance(coinId, OWSQLiteOpenHelper.BalanceType.ESTIMATED));
		walletBalanceTextView.setText(
				OWCoin.formatCoinAmount(coinId, walletBallance).toPlainString());

		TextView walletBalanceFiatEquivTextView = (TextView) 
				headerView.findViewById(R.id.bottomRightTextView);
		BigDecimal walletBalanceFiatEquiv = OWConverter.convert(
				walletBallance, coinId, OWFiat.USD);
		walletBalanceFiatEquivTextView.setText(OWFiat.USD.getSymbol() + 
				walletBalanceFiatEquiv.toPlainString());

		ImageView marketIcon = (ImageView) (headerView.findViewById(R.id.rightIcon));
		Drawable marketImage = this.getActivity().getResources().getDrawable(
				R.drawable.stats_enabled);
		marketIcon.setImageDrawable(marketImage);
	}
	
}