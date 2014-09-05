package com.ziftr.android.onewallet.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.onewallet.util.OWCoin;

public abstract class OWCoinRelativeTable extends OWTable {
	
	/** The note column. This is for users to keep a string attached to an entry. */
	public static final String COLUMN_NOTE = "note";
	
	/** The first time that this transaction was seen by the network (ziftr server). */
	public static final String COLUMN_CREATION_TIMESTAMP = "creation_timestamp";
	
	/**
	 * The string that will be executed when creating a table.
	 * 
	 * @param coinId - The table specifier.
	 * @return
	 */
	protected abstract String getCreateTableString(OWCoin.Type coinId);

	/** 
	 * The postfix that assists in making the names for the external addresses table. 
	 */
	protected abstract String getTablePostfix();

	protected String getTableName(OWCoin.Type coinId) {
		return coinId.toString() + getTablePostfix();
	}

	protected void create(OWCoin.Type coinId, SQLiteDatabase db) {
		db.execSQL(getCreateTableString(coinId));
	}
	
	protected int numEntries(OWCoin.Type coinId, SQLiteDatabase db) {
		String countQuery = "SELECT  * FROM " + getTableName(coinId);
		Cursor cursor = db.rawQuery(countQuery, null);
		int count = cursor.getCount();
		cursor.close();
		return count;
	}

}