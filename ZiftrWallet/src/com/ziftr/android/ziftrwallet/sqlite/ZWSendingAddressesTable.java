/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWSendingAddress;
import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;

public class ZWSendingAddressesTable extends ZWAddressesTable<ZWSendingAddress> {

	private static final String TABLE_NAME_BASE = "_sending_addresses";

	@Override
	protected String getTableName(ZWCoin coin) {
		return coin.getSymbol() + TABLE_NAME_BASE;
	}


	@Override
	protected void createBaseTable(ZWCoin coin, SQLiteDatabase database) {
		String createSql = "CREATE TABLE IF NOT EXISTS " + getTableName(coin) + 
				" (" + COLUMN_ADDRESS + " TEXT UNIQUE NOT NULL)";

		database.execSQL(createSql);
	}

	@Override
	protected ZWSendingAddress cursorToAddress(ZWCoin coinId, Cursor c) throws ZWAddressFormatException {
		ZWSendingAddress newAddress = new ZWSendingAddress(coinId, c.getString(c.getColumnIndex(COLUMN_ADDRESS)));

		// Reset all the keys parameters for use elsewhere
		newAddress.setLabel(c.getString(c.getColumnIndex(COLUMN_LABEL)));
		newAddress.setLastKnownBalance(c.getInt(c.getColumnIndex(COLUMN_BALANCE)));
		newAddress.setLastTimeModifiedSeconds(c.getLong(c.getColumnIndex(COLUMN_MODIFIED_TIMESTAMP)));
		return newAddress;
	}

}