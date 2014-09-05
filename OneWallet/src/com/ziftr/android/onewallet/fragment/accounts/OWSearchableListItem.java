package com.ziftr.android.onewallet.fragment.accounts;

public interface OWSearchableListItem {

	public boolean matches(CharSequence constraint);

	// Maybe we can make another method that also takes the 
	// immediately following item in the list. Would be useful for 
	// the pending bar.

	//	public boolean matches(CharSequence constraint, T nextItem);

	//	@Override
	//	public boolean matches(CharSequence constraint, OWWalletTransaction nextItem) {
	//		// Here we are assuming that there will always be something after the pending
	//		// bar. Okay because we have the histor bar at least.
	//		if (this.getTxViewType() == ) {
	//			return true;
	//		} else {
	//			return this.getTxNote().toLowerCase(Locale.ENGLISH).contains(constraint);
	//		}
	//	}

}