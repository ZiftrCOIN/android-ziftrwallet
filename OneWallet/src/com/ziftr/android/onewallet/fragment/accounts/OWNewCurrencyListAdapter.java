package com.ziftr.android.onewallet.fragment.accounts;

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
import com.ziftr.android.onewallet.util.OWCoin;

/**
 * In the accounts section of the app there is a list of currencies that opens
 * when the user hits "+ New Currency". In the GridView that opens is a list
 * of currencies the user can choose from. This adapts each option to form
 * a view for each currency, reusing old views to improve efficiency. 
 */
public class OWNewCurrencyListAdapter extends ArrayAdapter<OWNewCurrencyListItem> {
	private int resourceId;
	private LayoutInflater inflater;
	private Context context;
	
	private static OWNewCurrencyListItem[] coins = new OWNewCurrencyListItem[] {
		// Mainnet coins
		new OWNewCurrencyListItem(R.drawable.logo_bitcoin, OWCoin.Type.BTC),
		new OWNewCurrencyListItem(R.drawable.logo_dogecoin, OWCoin.Type.DOGE),
		new OWNewCurrencyListItem(R.drawable.logo_litecoin, OWCoin.Type.LTC),
		new OWNewCurrencyListItem(R.drawable.logo_peercoin, OWCoin.Type.PPC),
		// Testnet coins
		new OWNewCurrencyListItem(R.drawable.logo_bitcoin, OWCoin.Type.BTC_TEST),
		new OWNewCurrencyListItem(R.drawable.logo_dogecoin, OWCoin.Type.DOGE_TEST),
		new OWNewCurrencyListItem(R.drawable.logo_litecoin, OWCoin.Type.LTC_TEST),
		new OWNewCurrencyListItem(R.drawable.logo_peercoin, OWCoin.Type.PPC_TEST),
	};
	
	public OWNewCurrencyListAdapter(Context ctx, int resourceId) {
		super(ctx, resourceId, coins);
		this.resourceId = resourceId;
		this.inflater = LayoutInflater.from(ctx);
		this.context = ctx;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// The convertView is an oldView that android is recycling.

		OWNewCurrencyListItem newCurrencyListItem = getItem(position);
		if (convertView == null) {
			// If it doesn't have an old view then we make a new one 
        	convertView = (RelativeLayout) this.inflater.inflate(this.resourceId, null);
        }
		
		// Whether or not we just created one, we reset all the resources
		// to match the newCurrencyListItem.

		// Set the coin logo image view
		ImageView coinLogo = (ImageView) 
				convertView.findViewById(R.id.coinLogoCentralImage);
		Drawable image = context.getResources().getDrawable(
				newCurrencyListItem.getCoinLogoResId());
		coinLogo.setImageDrawable(image);
		
		// Set the text under the logo
		TextView coinName = (TextView) 
				convertView.findViewById(R.id.coinTitleTextView);
		coinName.setText(newCurrencyListItem.getCoinId().getTitle());

		return convertView;
	}
}