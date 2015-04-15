package com.ziftr.android.ziftrwallet.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

public class ZWMiscTable {
	
	public static final String TABLE_NAME = "miscellaneous";
	public static final String COLUMN_KEY = "key";
	public static final String COLUMN_VALUE = "value";
	public static final String SALT = "salt";
	public static final String PASS_KEY = "passphrase_hash";

	
	protected void create(SQLiteDatabase db) {
		
		String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + COLUMN_KEY + " TEXT UNIQUE NOT NULL, " + COLUMN_VALUE + " TEXT)";
		db.execSQL(sql);
	}
	
	protected void upsert(String key, String val, SQLiteDatabase db){
		ContentValues values = new ContentValues();
		values.put(COLUMN_KEY, key);
		values.put(COLUMN_VALUE, val);

		if (db.update(TABLE_NAME, values, COLUMN_KEY + " = " + DatabaseUtils.sqlEscapeString(key), null) == 0){
			db.insert(TABLE_NAME, null, values);
		}
	}
	
	protected String getValFromKey(String key, SQLiteDatabase db){
		String selectQuery = "SELECT " + COLUMN_VALUE + " FROM " + TABLE_NAME + " WHERE " + COLUMN_KEY + " = " + DatabaseUtils.sqlEscapeString(key);
		Cursor c = db.rawQuery(selectQuery, null);
		if (c.moveToFirst()) {
			String val = c.getString(c.getColumnIndex(COLUMN_VALUE));
			c.close();
			return val;
		
		} else {
			c.close();
			//no row found = no internet or api call failed when retreiving fiat rates return 0
			return null;
		}
	}	
	
}
