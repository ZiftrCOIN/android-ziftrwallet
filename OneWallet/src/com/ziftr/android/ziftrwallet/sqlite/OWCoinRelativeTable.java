package com.ziftr.android.ziftrwallet.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.ziftrwallet.util.OWCoin;

public abstract class OWCoinRelativeTable extends OWTable {
	
	/** The first time that this transaction was seen by the network (ziftr server). */
	public static final String COLUMN_CREATION_TIMESTAMP = "creation_timestamp";
	
	/**
	 * The string that will be executed when creating a table.
	 * 
	 * @param coinId - The table specifier.
	 * @return
	 */
	protected abstract String getCreateTableString(OWCoin coinId);

	/** 
	 * The postfix that assists in making the names for the external addresses table. 
	 */
	protected abstract String getTablePostfix();

	protected String getTableName(OWCoin coinId) {
		return coinId.toString() + getTablePostfix();
	}

	protected void create(OWCoin coinId, SQLiteDatabase db) {
		db.execSQL(getCreateTableString(coinId));
	}
	
	protected int numEntries(OWCoin coinId, SQLiteDatabase db) {
		String countQuery = "SELECT  * FROM " + getTableName(coinId);
		Cursor cursor = db.rawQuery(countQuery, null);
		int count = cursor.getCount();
		cursor.close();
		return count;
	}

}