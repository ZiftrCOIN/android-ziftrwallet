package com.ziftr.android.ziftrwallet.util;

import com.ziftr.android.ziftrwallet.OWMainFragmentActivity.FragmentType;

public class OWTags {
	
	//FRAGMENT TAGS
	
	public static final String TXN_DETAILS = "txn_details_fragment";
	
	public static final String WALLET_FRAGMENT = "wallet_fragment";
	
	public static final String RECIEVE_FRAGMENT = "receive_coins_fragment";
	
	public static final String SEND_FRAGMENT = "send_coins_fragment";
	
	public static final String ADD_CURRENCY = "add_new_currency";
	
	public static final String ADDRESS_BOOK = "address_book";

	public static final String ACCOUNTS_TAG = FragmentType.ACCOUNT_FRAGMENT_TYPE.toString();
	
	public static final String ACCOUNTS_INNER =  ACCOUNTS_TAG + "_INNER";
	
	public static final String DEACTIVATE_WALLET = "confirmation_dialog_deactivate";
	
	//DIALOG TAGS
	public static final String VALIDATE_PASS_RECEIVE = "validate_passphrase_dialog_new_key";
	
	public static final String VALIDATE_PASS_SEND = "validate_passphrase_dialog_send";
	
	public static final String EDIT_ADDRESS_LABEL_FROM_ADDRESS_BOOK = "edit_address_label_address_book";
	
	public static final String CONFIRM_NEW_ADDRESS = "confirm_new_address_dialog";

}
