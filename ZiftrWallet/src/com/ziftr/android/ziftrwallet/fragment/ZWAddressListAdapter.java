package com.ziftr.android.ziftrwallet.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ziftr.android.ziftrwallet.ZWMainFragmentActivity;
import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.crypto.ZWAddress;
import com.ziftr.android.ziftrwallet.dialog.ZWEditAddressLabelDialog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

/**
 * In the accounts section of the app there is a list of currencies that opens
 * when the user hits "+ New Currency". In the GridView that opens is a list
 * of currencies the user can choose from. This adapts each option to form
 * a view for each currency, reusing old views to improve efficiency. 
 */
public class ZWAddressListAdapter extends ZWSearchableListAdapter<ZWAddress> {

	private ZWMainFragmentActivity activity;
	
	/**
	 * This sort state is relative to the top of the screen. Example:
	 * 
	 *   a
	 *   b
	 *   c
	 * 
	 * is sorted by SortState.ALPHABETICALLY_A_TO_Z
	 */
	protected enum SortState {
		UNSORTED, 
		TIME_NEW_TO_OLD, 
		TIME_OLD_TO_NEW, 
		ALPHABETICALLY_A_TO_Z, 
		ALPHABETICALLY_Z_TO_A
	}
	private SortState sortState = SortState.UNSORTED;

	// Make this take a factory finalizable something or other
	public ZWAddressListAdapter(ZWMainFragmentActivity activity, List<ZWAddress> txList) {
		super(activity, R.layout.accounts_address_book_list_item, txList);
		this.activity = activity;
	}

	public ZWAddressListAdapter(ZWMainFragmentActivity activity) {
		this(activity, new ArrayList<ZWAddress>());
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ZWAddress address = getItem(position);

		// The convertView is an oldView that android is recycling, or null if not recycling yet
		if (convertView == null) {
			// If it doesn't have an old view then we make a new one 
			convertView = this.getInflater().inflate(R.layout.accounts_address_book_list_item, parent, false);
		}

		// Get all the references
		ImageView editImage = (ImageView) convertView.findViewById(R.id.leftIcon);
		editImage.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Bundle args = new Bundle();
				args.putBoolean(ZWEditAddressLabelDialog.IS_RECEIVING_NOT_SENDING_KEY, address.isPersonalAddress());
				args.putString(ZWEditAddressLabelDialog.CURRENT_ENTERED_TEXT_KEY, address.getLabel());
				args.putString(ZWEditAddressLabelDialog.ADDRESS_KEY, address.toString());
				activity.showEditAddressLabelDialog(ZWRequestCodes.EDIT_ADDRESS_LABEL_FROM_ADDRESS_BOOK, 
						address.isPersonalAddress(), args, ZWTags.EDIT_ADDRESS_LABEL_FROM_ADDRESS_BOOK);
			}
		});

		ImageView inOutImage = (ImageView) convertView.findViewById(R.id.rightIcon);
		inOutImage.setImageDrawable(this.getContext().getResources().getDrawable(this.getImgResIdForItem(address)));

		TextView addressLabelTextView = (TextView) convertView.findViewById(R.id.topLeftTextView);
		addressLabelTextView.setText(address.getLabel());

		TextView formattedAddressTextView = (TextView) convertView.findViewById(R.id.bottomLeftTextView);
		formattedAddressTextView.setText(address.toString());

		return convertView;
	}

	/**
	 * @param txListItem
	 * @return
	 */
	private int getImgResIdForItem(ZWAddress address) {
		if (address.isPersonalAddress()) {
			return R.drawable.received_enabled;
		} else {
			return R.drawable.sent_enabled;
		}
	}
	
	public void toggleSortByTime() {
		this.sortState = this.getSortState() != SortState.TIME_NEW_TO_OLD ? 
				SortState.TIME_NEW_TO_OLD : SortState.TIME_OLD_TO_NEW;
		this.sortByTime(this.getSortState() == SortState.TIME_NEW_TO_OLD);
	}
	
	public void toggleSortAlphabetically() {
		this.sortState = this.getSortState() != SortState.ALPHABETICALLY_A_TO_Z ? 
				SortState.ALPHABETICALLY_A_TO_Z : SortState.ALPHABETICALLY_Z_TO_A;
		this.sortAlphabeticall(this.getSortState() == SortState.ALPHABETICALLY_A_TO_Z);
	}

	private void sortByTime(boolean newerFirst) {
		// TODO do on a different thread with a callback
		final int multiplier = newerFirst ? 1 : -1;
		Comparator<ZWAddress> comparator = new Comparator<ZWAddress>() {
			@Override
			public int compare(ZWAddress lhs, ZWAddress rhs) {
				if (lhs.getLastTimeModifiedSeconds() < rhs.getLastTimeModifiedSeconds()) {
					return multiplier * 1;
				} else if (lhs.getLastTimeModifiedSeconds() > rhs.getLastTimeModifiedSeconds()) {
					return multiplier * -1;
				} else {
					return 0;
				}
			}
		};
		this.sort(comparator);
	}
	
	private void sortAlphabeticall(boolean aToZ) {
		// TODO do on a different thread with a callback 
		final int multiplier = aToZ ? 1 : -1;
		Comparator<ZWAddress> comparator = new Comparator<ZWAddress>() {
			@Override
			public int compare(ZWAddress lhs, ZWAddress rhs) {
				return multiplier * lhs.getLabel().compareToIgnoreCase(rhs.getLabel());
			}
		};
		this.sort(comparator);
	}
	
	private void sort(final Comparator<ZWAddress> comparator) {
		// Have to do the sorting on a different thread in case it takes  along time
		ZiftrUtils.runOnNewThread(new Runnable() {
			@Override
			public void run() {
				// Sort the list
				Collections.sort(getFullList(), comparator);

				if (activity != null) {
					activity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							refreshWorkingList(false);
						}
					});
				}
			}
		});
	}

	/**
	 * @return the sortState
	 */
	public SortState getSortState() {
		return sortState;
	}

	/**
	 * @param sortState the sortState to set
	 */
	public void setSortState(SortState sortState) {
		this.sortState = sortState;
	}

	@Override
	public void applySortToFullList() {
		if (this.sortState == SortState.UNSORTED) {
			return;
		} else if (this.sortState == SortState.ALPHABETICALLY_A_TO_Z) {
			this.sortAlphabeticall(true);
		} else if (this.sortState == SortState.ALPHABETICALLY_Z_TO_A) {
			this.sortAlphabeticall(false);
		} else if (this.sortState == SortState.TIME_NEW_TO_OLD) {
			this.sortByTime(true);
		} else if (this.sortState == SortState.TIME_OLD_TO_NEW) {
			this.sortByTime(false);
		}
	}

}