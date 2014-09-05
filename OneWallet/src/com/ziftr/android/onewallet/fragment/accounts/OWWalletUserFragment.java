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
import com.ziftr.android.onewallet.util.OWCoinRelative;
import com.ziftr.android.onewallet.util.OWConverter;
import com.ziftr.android.onewallet.util.OWFiat;
import com.ziftr.android.onewallet.util.OWUtils;

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
	
	protected void initializeWalletHeaderView() {
		View headerView = this.getOWMainActivity().findViewById(R.id.walletHeader);
		headerView.setVisibility(View.VISIBLE);
		ImageView coinLogo = (ImageView) (headerView.findViewById(R.id.leftIcon));
		Drawable coinImage = this.getActivity().getResources().getDrawable(
				getCoinId().getLogoResId());
		coinLogo.setImageDrawable(coinImage);

		TextView coinTitle = (TextView) headerView.findViewById(R.id.topLeftTextView);
		coinTitle.setText(getCoinId().getLongTitle());

		TextView coinUnitPriceInFiatTextView = (TextView) 
				headerView.findViewById(R.id.bottomLeftTextView);
		BigDecimal unitPriceInFiat = OWConverter.convert(
				BigDecimal.ONE, getCoinId(), OWFiat.Type.USD);
		coinUnitPriceInFiatTextView.setText(OWFiat.Type.USD.getSymbol() + 
				OWFiat.formatFiatAmount(OWFiat.Type.USD, unitPriceInFiat).toPlainString());

		TextView walletBalanceTextView = (TextView) 
				headerView.findViewById(R.id.topRightTextView);
		BigDecimal walletBallance = OWUtils.bigIntToBigDec(getCoinId(), 
				getWalletManager().getWalletBalance(getCoinId(), OWSQLiteOpenHelper.BalanceType.ESTIMATED));
		walletBalanceTextView.setText(
				OWCoin.formatCoinAmount(getCoinId(), walletBallance).toPlainString());

		TextView walletBalanceFiatEquivTextView = (TextView) 
				headerView.findViewById(R.id.bottomRightTextView);
		BigDecimal walletBalanceFiatEquiv = OWConverter.convert(
				walletBallance, getCoinId(), OWFiat.Type.USD);
		walletBalanceFiatEquivTextView.setText(OWFiat.Type.USD.getSymbol() + 
				walletBalanceFiatEquiv.toPlainString());

		ImageView marketIcon = (ImageView) (headerView.findViewById(R.id.rightIcon));
		Drawable marketImage = this.getActivity().getResources().getDrawable(
				R.drawable.stats_enabled);
		marketIcon.setImageDrawable(marketImage);
	}
	
}