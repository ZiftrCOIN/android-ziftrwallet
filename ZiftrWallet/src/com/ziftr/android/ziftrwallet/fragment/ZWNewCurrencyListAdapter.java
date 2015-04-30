package com.ziftr.android.ziftrwallet.fragment;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;

/**
 * In the accounts section of the app there is a list of currencies that opens
 * when the user hits "+ New Currency". In the ListView that opens is a list
 * of currencies the user can choose from. This adapts each option to form
 * a view for each currency, reusing old views to improve efficiency. 
 */
public class ZWNewCurrencyListAdapter extends ArrayAdapter<ZWCoin> {
	
	private static final int VIEW_RES_ID = R.layout.coin_list_item;
	
	private LayoutInflater inflater;

	public ZWNewCurrencyListAdapter(Context ctx, List<ZWCoin> coins) {
		super(ctx, VIEW_RES_ID, coins);
		this.inflater = LayoutInflater.from(ctx);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// The convertView is an oldView that android is recycling.

		final ZWCoin coin = getItem(position);
		if (convertView == null) {
			// If it doesn't have an old view then we make a new one 
			convertView = this.inflater.inflate(VIEW_RES_ID, null);
		}

		// Whether or not we just created one, we reset all the resources
		// to match the newCurrencyListItem.

		// Set the coin logo image view
		ImageView coinLogo = (ImageView) convertView.findViewById(R.id.leftIcon);
		coinLogo.setImageResource(coin.getLogoResId());
		
		ImageView addIcon = (ImageView) convertView.findViewById(R.id.rightIcon);
		addIcon.setImageResource(R.drawable.add_currency_item);

		// Set the text next to logo
		TextView coinName = (TextView) convertView.findViewById(R.id.topLeftTextView);
		
		String nameText = coin.getName();
		coinName.setText(nameText);
		
		ImageView noServerImage = (ImageView) convertView.findViewById(R.id.imageViewNoServer);
		if("ok".equals(coin.getHealth())) {
			noServerImage.setVisibility(View.GONE);
		}
		else {
			noServerImage.setVisibility(View.VISIBLE);
		}
		

		return convertView;
	}
}