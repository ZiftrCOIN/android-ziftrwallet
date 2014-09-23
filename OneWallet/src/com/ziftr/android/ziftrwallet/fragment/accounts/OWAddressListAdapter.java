package com.ziftr.android.ziftrwallet.fragment.accounts;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.crypto.OWAddress;
import com.ziftr.android.ziftrwallet.util.ZLog;

/**
 * In the accounts section of the app there is a list of currencies that opens
 * when the user hits "+ New Currency". In the GridView that opens is a list
 * of currencies the user can choose from. This adapts each option to form
 * a view for each currency, reusing old views to improve efficiency. 
 */
public class OWAddressListAdapter extends OWSearchableListAdapter<OWAddress> {
	
	OWAddressBookFragment addressBookFragment;

	// Make this take a factory finalizable something or other
	public OWAddressListAdapter(OWAddressBookFragment addressBookFragment, Context ctx, List<OWAddress> txList) {
		super(ctx, R.layout.accounts_address_book_list_item, txList);
		this.addressBookFragment = addressBookFragment;
	}
	
	public OWAddressListAdapter(OWAddressBookFragment addressBookFragment, Context context) {
		this(addressBookFragment, context, new ArrayList<OWAddress>());
	}

	public OWAddressListAdapter(Context context) {
		// Had to add this constructor to get rid of warnings, for some reason.
		this(null, context, new ArrayList<OWAddress>());
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// The convertView is an oldView that android is recycling, which may be null.
		
		ZLog.log("Getting view at position: ", String.valueOf(position));

		OWAddress address = getItem(position);
		if (convertView == null) {
			// If it doesn't have an old view then we make a new one 
			convertView = this.getInflater().inflate(R.layout.accounts_address_book_list_item, null);
		}
		
		// Set the edit button
		ImageView editImage = (ImageView) convertView.findViewById(R.id.leftIcon);
		editImage.setImageDrawable(this.getContext().getResources().getDrawable(R.drawable.edit_clickable));

		// Set the in/out icon
		ImageView inOutImage = (ImageView) convertView.findViewById(R.id.rightIcon);
		inOutImage.setImageDrawable(this.getContext().getResources().getDrawable(this.getImgResIdForItem(address)));

		// Set top/bottom right text boxes to empty
		EditText addressLabelTextView = (EditText) convertView.findViewById(R.id.topLeftTextView);
		Object tag = addressLabelTextView.getTag();
		if (tag instanceof OWEditableTextBoxController<?>) {
			// Need to remove the textwatcher before changing the text so that 
			// we don't have a bug where the label for an address is changed incorrectly
			OWEditableTextBoxController<?> oldController = 
					(OWEditableTextBoxController<?>) addressLabelTextView.getTag();
			addressLabelTextView.removeTextChangedListener(oldController);
		}
		addressLabelTextView.setText(address.getLabel());

		// Set top/bottom right text boxes to empty
		TextView formattedAddressTextView = (TextView) convertView.findViewById(R.id.bottomLeftTextView);
		formattedAddressTextView.setText(address.toString());
		
		// Set the onclick listener to be a controller for the label text box
		OWEditableTextBoxController<OWAddress> controller = new OWEditableTextBoxController<OWAddress>(
				addressBookFragment, addressLabelTextView, editImage, address.getLabel(), address);
		editImage.setOnClickListener(controller);
		addressLabelTextView.addTextChangedListener(controller);
		addressLabelTextView.setTag(controller);
		
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