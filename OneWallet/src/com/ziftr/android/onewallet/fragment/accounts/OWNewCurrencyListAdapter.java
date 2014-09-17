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
 * In the accounts section of the app there is a list of currencies that opens
 * when the user hits "+ New Currency". In the ListView that opens is a list
 * of currencies the user can choose from. This adapts each option to form
 * a view for each currency, reusing old views to improve efficiency. 
 */
public class OWNewCurrencyListAdapter extends ArrayAdapter<OWNewCurrencyListItem> {
	private int resourceId;
	private LayoutInflater inflater;
	private Context context;
	
	public OWNewCurrencyListAdapter(Context ctx, int resourceId, 
			List<OWNewCurrencyListItem> coins) {
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
		ImageView coinLogo = (ImageView) convertView.findViewById(R.id.leftIcon);
		Drawable image = context.getResources().getDrawable(newCurrencyListItem.getCoinId().getLogoResId());
		coinLogo.setImageDrawable(image);
		
		// Set the text next to logo
		TextView coinName = (TextView) convertView.findViewById(R.id.topLeftTextView);
		coinName.setText(newCurrencyListItem.getCoinId().getLongTitle());
		
		//Set the add button image
		ImageView addImage = (ImageView) convertView.findViewById(R.id.rightIcon);
		addImage.setImageDrawable(context.getResources().getDrawable(R.drawable.next_down_2));

		return convertView;
	}
}