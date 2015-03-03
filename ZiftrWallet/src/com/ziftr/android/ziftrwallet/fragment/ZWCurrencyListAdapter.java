package com.ziftr.android.ziftrwallet.fragment;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

import android.content.Context;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWPreferencesUtils;
import com.ziftr.android.ziftrwallet.ZWWalletManager;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWConverter;
import com.ziftr.android.ziftrwallet.crypto.ZWFiat;

/**
 * In the accounts section of the app there is a list of currencies/accounts
 * that the user has set up. This adapter gives views from instances of 
 * {@link ZWCurrencyListItem}. It also makes the last item a footer type,
 * with an add new currency bar instead of the usual coinType view. 
 */
public class ZWCurrencyListAdapter extends ArrayAdapter<ZWCoin> {
	private LayoutInflater inflater;
	private Context context;

	/** Standard entries for wallets. */
	public static final int coinType = 0;
	/** The add new bar at the bottom. */
	public static final int footerType = 1;

	public ZWCurrencyListAdapter(Context ctx, List<ZWCoin> objects) {
		super(ctx, 0, objects);
		this.inflater = LayoutInflater.from(ctx);
		this.context = ctx;
	}

	/**
	 * Either coinType or footerType. Only footerType if it is 
	 * the last element in the list. 
	 */
	@Override
	public int getItemViewType(int position) {
		if (position == (getCount()-1)) {
			return footerType;
		} else {
			return coinType;
		}
	}

	/**
	 * We have two types, coinType and footerType. 
	 */
	@Override
	public int getViewTypeCount() {
		// 2 because we have 2 wallet types, coinType and footerType
		return 2;
	}

	/**
	 * Given the position of the item in the list, we get the list item
	 * and recreate the view from it. Note that this method recycles 
	 * the convertView when we have enough list elements. 
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// The convertView is an oldView that android is recycling.

		ZWCoin coin = getItem(position);
		if (convertView == null) {
			// If it doesn't have an old view then we make a new one 
			convertView = this.inflater.inflate(R.layout.coin_list_item, null);
		}
		
		ZWFiat fiatType =ZWPreferencesUtils.getFiatCurrency();
		String fiatSymbol = fiatType.getSymbol();
		
		convertView.findViewById(R.id.market_graph_icon).setVisibility(View.VISIBLE);
		
		// Whether or not we just created one, we reset all the resources
		// to match the currencyListItem.
		TextView coinName = (TextView) 
				convertView.findViewById(R.id.topLeftTextView);
		String nameText = coin.getName(); 
		coinName.setText(nameText);

		TextView coinValue = (TextView) 
				convertView.findViewById(R.id.bottomLeftTextView);

		BigDecimal unitPriceInFiat = ZWConverter.convert(BigDecimal.ONE, coin, fiatType);
		SpannableString displayMarketFiat = fiatType.getDisplayString(unitPriceInFiat, true, currencyListItem.getCoinId().getFiatEquivPrecision());

			coinValue.setText(displayMarketFiat);
		
		ImageView noServerImage = (ImageView) convertView.findViewById(R.id.imageViewNoServer);
		if(coin.isEnabled()) {
			noServerImage.setVisibility(View.GONE);
		} else {
			noServerImage.setVisibility(View.VISIBLE);
		}
		
		BigInteger amountAtomic = ZWWalletManager.getInstance().getWalletBalance(coin);
		BigDecimal amount = coin.getAmount(amountAtomic);
		TextView walletTotal = (TextView) convertView.findViewById(R.id.topRightTextView);
		walletTotal.setText(amount.setScale(4, RoundingMode.HALF_UP).toPlainString());

		TextView walletTotalFiatEquiv = (TextView) convertView.findViewById(R.id.bottomRightTextView);
		
		SpannableString displayWalletTotalFiat = fiatType.getDisplayString(ZWConverter.convert(amount, coin, fiatType), true);
		
		walletTotalFiatEquiv.setText(displayWalletTotalFiat);

		ImageView coinLogo = (ImageView) convertView.findViewById(R.id.leftIcon);
		coinLogo.setImageResource(coin.getLogoResId());

		ImageView nextArrow = (ImageView) convertView.findViewById(R.id.rightIcon);
		nextArrow.setImageResource(R.drawable.next_down_2);

		return convertView;

	}
}