package com.ziftr.android.ziftrwallet.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.ziftrwallet.crypto.ZWCoin;

public abstract class ZWCoinRelativeTable {
	
	/** The first time that this transaction was seen by the network (ziftr server). */
	protected static final String COLUMN_CREATION_TIMESTAMP = "creation_timestamp";

	
	protected abstract void createBaseTable(ZWCoin coin, SQLiteDatabase database);
	protected abstract void createTableColumns(ZWCoin coin, SQLiteDatabase database);
	protected abstract String getTableName(ZWCoin coin);

	protected void create(ZWCoin coin, SQLiteDatabase db) {
		createBaseTable(coin, db);
		createTableColumns(coin, db);
	}
	
	protected int numEntries(ZWCoin coin, SQLiteDatabase db) {
		String countQuery = "SELECT  * FROM " + getTableName(coin);
		Cursor cursor = db.rawQuery(countQuery, null);
		int count = cursor.getCount();
		cursor.close();
		return count;
	}
	
	
	protected void addColumn(ZWCoin coin, String columnName, String constraints, SQLiteDatabase database) {
		
		try {
			String sql = "ALTER TABLE " + getTableName(coin) + " ADD COLUMN " + columnName + " " + constraints;
			database.execSQL(sql);
		}
		catch(Exception e) {
			//quietly fail when adding columns, they likely already exist
		}
	}

}