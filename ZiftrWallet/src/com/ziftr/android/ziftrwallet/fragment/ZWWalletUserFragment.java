package com.ziftr.android.ziftrwallet.fragment;

import android.view.View;

import com.ziftr.android.ziftrwallet.ZWWalletManager;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.network.ZWDataSyncHelper;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public abstract class ZWWalletUserFragment extends ZWFragment {
	
	protected View walletHeader;
	/**
	 * Gives the globally accessible wallet manager. All coin-network related
	 * things should be done through this class as it chooses whether to use
	 * (temporary) bitcoinj or to use the SQLite tables and API (future). 
	 * 
	 * @return the wallet manager
	 */
	protected ZWWalletManager getWalletManager() {
		return this.getZWMainActivity().getWalletManager();
	}

	protected ZWCoin getSelectedCoin() {
		return this.getZWMainActivity().getSelectedCoin();
	}
	
	public void populateWalletHeader(View v) {
		this.walletHeader = v;
		this.getZWMainActivity().populateWalletHeaderView(v);
	}
	
	@Override
	public void onDataUpdated(){
		super.onDataUpdated();
		if (this.walletHeader != null){
			this.getZWMainActivity().updateWalletHeaderView(this.walletHeader);
		}
	}
	
	//Note: at some point we may want to completely handle the header views inside the fragments
	//leaving this code here for awhile in case we need it
	/*****
	void populateWalletHeaderView(View headerView) {
		
		ZWCoin selectedCoin = this.getSelectedCoin();
		ZWFiat selectedFiat = ZWPreferencesUtils.getFiatCurrency(this.getZWMainActivity());

		ImageView coinLogo = (ImageView) (headerView.findViewById(R.id.leftIcon));
		coinLogo.setImageResource(selectedCoin.getLogoResId());

		TextView coinTitle = (TextView) headerView.findViewById(R.id.topLeftTextView);
		coinTitle.setText(selectedCoin.getLongTitle());

		TextView fiatExchangeRateText = (TextView) headerView.findViewById(R.id.bottomLeftTextView);
		BigDecimal unitPriceInFiat = ZWConverter.convert(BigDecimal.ONE, selectedCoin, selectedFiat);
		fiatExchangeRateText.setText(ZWFiat.formatFiatAmount(selectedFiat, unitPriceInFiat, true));

		TextView walletBalanceTextView = (TextView) headerView.findViewById(R.id.topRightTextView);
		BigInteger atomicUnits = getWalletManager().getWalletBalance(selectedCoin, ZWSQLiteOpenHelper.BalanceType.ESTIMATED);
		BigDecimal walletBalance = ZiftrUtils.bigIntToBigDec(selectedCoin, atomicUnits);

		walletBalanceTextView.setText(ZWCoin.formatCoinAmount(selectedCoin, walletBalance).toPlainString());

		TextView walletBalanceInFiatText = (TextView) headerView.findViewById(R.id.bottomRightTextView);
		BigDecimal walletBalanceInFiat = ZWConverter.convert(walletBalance, selectedCoin, selectedFiat);
		walletBalanceInFiatText.setText(ZWFiat.formatFiatAmount(selectedFiat, walletBalanceInFiat, true));

		ImageView syncButton = (ImageView) headerView.findViewById(R.id.rightIcon);
		syncButton.setImageResource(R.drawable.icon_sync_button_statelist);
		
		//TODO -this is hacky, if we use this method, fix this
		syncButton.setOnClickListener((View.OnClickListener)this.getZWMainActivity());
	}
	*****/
	@Override
	public void refreshData(final boolean autorefresh) {
		ZiftrUtils.runOnNewThread(new Runnable() {
			
			@Override
			public void run() {
				ZWDataSyncHelper.updateTransactionHistory(getZWMainActivity().getSelectedCoin(), autorefresh);
			}

		});
	}
}
