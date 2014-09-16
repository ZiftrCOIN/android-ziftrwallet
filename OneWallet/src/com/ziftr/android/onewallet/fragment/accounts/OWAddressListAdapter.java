package com.ziftr.android.onewallet.fragment.accounts;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.crypto.OWAddress;

/**
 * In the accounts section of the app there is a list of currencies that opens
 * when the user hits "+ New Currency". In the GridView that opens is a list
 * of currencies the user can choose from. This adapts each option to form
 * a view for each currency, reusing old views to improve efficiency. 
 */
public class OWAddressListAdapter extends OWSearchableListAdapter<OWAddress> {

	public OWAddressListAdapter(Context ctx, List<OWAddress> txList) {
		super(ctx, R.layout.accounts_address_book_list_item, txList);
	}

	public OWAddressListAdapter(Context context) {
		// Had to add this constructor to get rid of warnings, for some reason.
		this(context, new ArrayList<OWAddress>());
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// The convertView is an oldView that android is recycling.

		OWAddress address = getItem(position);
		if (convertView == null) {
			// If it doesn't have an old view then we make a new one 
			convertView = this.getInflater().inflate(R.layout.accounts_address_book_list_item, null);
		}

		// Set the edit button
		ImageView addImage = (ImageView) convertView.findViewById(R.id.leftIcon);
		addImage.setImageDrawable(this.getContext().getResources().getDrawable(R.drawable.edit_clickable));

		// Set the in/out icon
		ImageView inOutImage = (ImageView) convertView.findViewById(R.id.rightIcon);
		inOutImage.setImageDrawable(this.getContext().getResources().getDrawable(this.getImgResIdForItem(address)));

		// Set top/bottom right text boxes to empty
		TextView topLeft = (TextView) convertView.findViewById(R.id.topLeftTextView);
		topLeft.setText(address.getLabel());

		// Set top/bottom right text boxes to empty
		TextView bottomLeft = (TextView) convertView.findViewById(R.id.bottomLeftTextView);
		bottomLeft.setText(address.toString());

		return convertView;
	}

	/**
	 * @param txListItem
	 * @return
	 */
	private int getImgResIdForItem(OWAddress address) {
		if (address.isPersonalAddress()) {
			return R.drawable.received_enabled;
		} else {
			return R.drawable.sent_enabled;
		}
	}

}