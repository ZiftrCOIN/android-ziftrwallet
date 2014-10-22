package com.ziftr.android.ziftrwallet.fragment.accounts;

import android.view.View;

import com.ziftr.android.ziftrwallet.OWWalletManager;
import com.ziftr.android.ziftrwallet.fragment.OWFragment;
import com.ziftr.android.ziftrwallet.network.OWDataSyncHelper;
import com.ziftr.android.ziftrwallet.util.OWCoin;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

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

	protected OWCoin getSelectedCoin() {
		return this.getOWMainActivity().getSelectedCoin();
	}
	
	public void populateWalletHeader(View v) {
		this.getOWMainActivity().populateWalletHeaderView(v);
	}
	
	
	//Note: at some point we may want to completely handle the header views inside the fragments
	//leaving this code here for awhile in case we need it
	/*****
	void populateWalletHeaderView(View headerView) {
		
		OWCoin selectedCoin = this.getSelectedCoin();
		OWFiat selectedFiat = OWPreferencesUtils.getFiatCurrency(this.getOWMainActivity());

		ImageView coinLogo = (ImageView) (headerView.findViewById(R.id.leftIcon));
		coinLogo.setImageResource(selectedCoin.getLogoResId());

		TextView coinTitle = (TextView) headerView.findViewById(R.id.topLeftTextView);
		coinTitle.setText(selectedCoin.getLongTitle());

		TextView fiatExchangeRateText = (TextView) headerView.findViewById(R.id.bottomLeftTextView);
		BigDecimal unitPriceInFiat = OWConverter.convert(BigDecimal.ONE, selectedCoin, selectedFiat);
		fiatExchangeRateText.setText(OWFiat.formatFiatAmount(selectedFiat, unitPriceInFiat, true));

		TextView walletBalanceTextView = (TextView) headerView.findViewById(R.id.topRightTextView);
		BigInteger atomicUnits = getWalletManager().getWalletBalance(selectedCoin, OWSQLiteOpenHelper.BalanceType.ESTIMATED);
		BigDecimal walletBalance = ZiftrUtils.bigIntToBigDec(selectedCoin, atomicUnits);

		walletBalanceTextView.setText(OWCoin.formatCoinAmount(selectedCoin, walletBalance).toPlainString());

		TextView walletBalanceInFiatText = (TextView) headerView.findViewById(R.id.bottomRightTextView);
		BigDecimal walletBalanceInFiat = OWConverter.convert(walletBalance, selectedCoin, selectedFiat);
		walletBalanceInFiatText.setText(OWFiat.formatFiatAmount(selectedFiat, walletBalanceInFiat, true));

		ImageView syncButton = (ImageView) headerView.findViewById(R.id.rightIcon);
		syncButton.setImageResource(R.drawable.icon_sync_button_statelist);
		
		//TODO -this is hacky, if we use this method, fix this
		syncButton.setOnClickListener((View.OnClickListener)this.getOWMainActivity());
	}
	*****/
	@Override
	public void refreshData() {
		ZiftrUtils.runOnNewThread(new Runnable() {
			
			@Override
			public void run() {
				OWDataSyncHelper.updateTransactionHistory(getOWMainActivity().getSelectedCoin());

			}
		});
	}
}
