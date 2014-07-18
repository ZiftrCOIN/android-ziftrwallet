package com.ziftr.android.onewallet.fragment.accounts;

import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ziftr.android.onewallet.R;

/**
 * In the accounts section of the app there is a list of currencies/accounts
 * that the user has set up. This adapter gives views from instances of 
 * {@link OWCurrencyListItem}. 
 */
public class OWCurrencyListAdapter extends ArrayAdapter<OWCurrencyListItem> {
	private int resourceId;
	private LayoutInflater inflater;
	private Context context;
	
	public OWCurrencyListAdapter(Context ctx, int resourceId, 
			List<OWCurrencyListItem> objects) {
		super(ctx, resourceId, objects);
		this.resourceId = resourceId;
		this.inflater = LayoutInflater.from(ctx);
		this.context = ctx;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// The convertView is an oldView that android is recycling.

		OWCurrencyListItem currencyListItem = getItem(position);
		if (convertView == null) {
			// If it doesn't have an old view then we make a new one 
        	convertView = (RelativeLayout) this.inflater.inflate(this.resourceId, null);
        }
		
		String fiatSymbol = currencyListItem.getFiatType().getSymbol();
		
		// Whether or not we just created one, we reset all the resources
		// to match the currencyListItem.
		TextView coinName = (TextView) 
				convertView.findViewById(R.id.coinName);
		coinName.setText(currencyListItem.getCoinId().getTitle());

		TextView coinValue = (TextView) 
				convertView.findViewById(R.id.coinUnitValue);
		coinValue.setText(fiatSymbol + currencyListItem.getUnitFiatMarketValue());
		
		TextView walletTotal = (TextView) 
				convertView.findViewById(R.id.coinWalletTotal);
		walletTotal.setText(currencyListItem.getWalletTotal());
		
		TextView walletTotalFiatEquiv = (TextView) 
				convertView.findViewById(R.id.coinWalletTotalFiatEquiv);
		walletTotalFiatEquiv.setText(
				fiatSymbol + currencyListItem.getWalletTotalFiatEquiv());

		ImageView coinLogo = (ImageView) convertView.findViewById(R.id.coinLogo);
		Drawable image = context.getResources().getDrawable(
				currencyListItem.getCoinLogoId());
		coinLogo.setImageDrawable(image);
		
		return convertView;
	}
}