package com.ziftr.android.onewallet.sqlite;

import android.content.ContentValues;
import android.database.Cursor;

import com.ziftr.android.onewallet.crypto.OWAddress;
import com.ziftr.android.onewallet.exceptions.OWAddressFormatException;
import com.ziftr.android.onewallet.util.OWCoin;

public class OWSendingAddressesTable extends OWAddressesTable {

	@Override
	protected String getTablePostfix() {
		return "_sending_addresses";
	}

	@Override
	protected String getCreateTableString(OWCoin coinId) {
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE IF NOT EXISTS ").append(getTableName(coinId)).append(" (");
		sb.append(COLUMN_ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
		sb.append(COLUMN_ADDRESS).append(" TEXT NOT NULL, ");
		sb.append(COLUMN_NOTE).append(" TEXT, ");
		sb.append(COLUMN_BALANCE).append(" INTEGER, ");
		//sb.append(COLUMN_CREATION_TIMESTAMP).append(" INTEGER, ");
		sb.append(COLUMN_MODIFIED_TIMESTAMP).append(" INTEGER );");
		return sb.toString();
	}

	@Override
	protected OWAddress cursorToAddress(OWCoin coinId, Cursor c) throws OWAddressFormatException {
		OWAddress newAddress = new OWAddress(coinId, c.getString(c.getColumnIndex(COLUMN_ADDRESS)));

		// Reset all the keys parameters for use elsewhere
		newAddress.setId(c.getLong(c.getColumnIndex(COLUMN_ID)));
		newAddress.setNote(c.getString(c.getColumnIndex(COLUMN_NOTE)));
		newAddress.setLastKnownBalance(c.getInt(c.getColumnIndex(COLUMN_BALANCE)));
		newAddress.setLastTimeModifiedSeconds(c.getLong(c.getColumnIndex(COLUMN_MODIFIED_TIMESTAMP)));
		//newAddress.setCreationTimeSeconds(c.getLong(c.getColumnIndex(COLUMN_CREATION_TIMESTAMP)));
		return newAddress;
	}

	@Override
	protected ContentValues addressToContentValues(OWAddress address) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_ADDRESS, address.toString());
		values.put(COLUMN_NOTE, address.getLabel());
		values.put(COLUMN_BALANCE, address.getLastKnownBalance());
		values.put(COLUMN_MODIFIED_TIMESTAMP, address.getLastTimeModifiedSeconds());
		//values.put(COLUMN_CREATION_TIMESTAMP, address.getCreationTimeSeconds());
		return values;
	}

}