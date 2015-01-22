package com.ziftr.android.ziftrwallet.fragment;

import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
	
	private static final int VIEW_RES_ID = R.layout.accounts_new_currency_list_item;
	
	private LayoutInflater inflater;
	private Context context;

	public ZWNewCurrencyListAdapter(Context ctx, List<ZWCoin> coins) {
		super(ctx, VIEW_RES_ID, coins);
		this.inflater = LayoutInflater.from(ctx);
		this.context = ctx;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// The convertView is an oldView that android is recycling.

		final ZWCoin coin = getItem(position);
		if (convertView == null) {
			// If it doesn't have an old view then we make a new one 
			convertView = (LinearLayout) this.inflater.inflate(this.VIEW_RES_ID, null);
		}

		// Whether or not we just created one, we reset all the resources
		// to match the newCurrencyListItem.

		// Set the coin logo image view
		ImageView coinLogo = (ImageView) convertView.findViewById(R.id.leftIcon);
		coinLogo.setImageResource(coin.getLogoResId());

		// Set the text next to logo
		TextView coinName = (TextView) convertView.findViewById(R.id.topLeftTextView);
		
		String nameText = coin.getName();
		if(!coin.isEnabled()) {
			nameText += " (server unavailable)";
		}
		coinName.setText(nameText);

		return convertView;
	}
}