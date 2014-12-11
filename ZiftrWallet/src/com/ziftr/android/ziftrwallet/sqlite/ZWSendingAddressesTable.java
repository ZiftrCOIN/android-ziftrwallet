package com.ziftr.android.ziftrwallet.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.ziftrwallet.crypto.ZWAddress;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;

public class ZWSendingAddressesTable extends ZWAddressesTable {

	private static final String TABLE_NAME_BASE = "_sending_addresses";
	
	
	@Override
	protected String getTableName(ZWCoin coin) {
		return coin.getShortTitle() + TABLE_NAME_BASE;
	}

	
	@Override
	protected void createBaseTable(ZWCoin coin, SQLiteDatabase database) {
		String createSql = "CREATE TABLE IF NOT EXISTS " + getTableName(coin) + 
				" (" + COLUMN_ADDRESS + " TEXT UNIQUE NOT NULL)";
		
		database.execSQL(createSql);
	}

	
	@Override
	protected ZWAddress cursorToAddress(ZWCoin coinId, Cursor c) throws ZWAddressFormatException {
		ZWAddress newAddress = new ZWAddress(coinId, c.getString(c.getColumnIndex(COLUMN_ADDRESS)));

		// Reset all the keys parameters for use elsewhere
		newAddress.setLabel(c.getString(c.getColumnIndex(COLUMN_LABEL)));
		newAddress.setLastKnownBalance(c.getInt(c.getColumnIndex(COLUMN_BALANCE)));
		newAddress.setLastTimeModifiedSeconds(c.getLong(c.getColumnIndex(COLUMN_MODIFIED_TIMESTAMP)));
		//newAddress.setCreationTimeSeconds(c.getLong(c.getColumnIndex(COLUMN_CREATION_TIMESTAMP)));
		return newAddress;
	}

	@Override
	protected ContentValues addressToContentValues(ZWAddress address) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_ADDRESS, address.getAddress());
		values.put(COLUMN_LABEL, address.getLabel());
		values.put(COLUMN_BALANCE, address.getLastKnownBalance());
		values.put(COLUMN_MODIFIED_TIMESTAMP, address.getLastTimeModifiedSeconds());
		//values.put(COLUMN_CREATION_TIMESTAMP, address.getCreationTimeSeconds());
		return values;
	}

}