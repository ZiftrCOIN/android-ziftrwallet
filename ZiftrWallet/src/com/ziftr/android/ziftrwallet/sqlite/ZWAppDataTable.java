package com.ziftr.android.ziftrwallet.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.ziftrwallet.util.ZLog;

public class ZWAppDataTable {
	
	public static final String TABLE_NAME = "app_data";
	public static final String COLUMN_KEY = "key";
	public static final String COLUMN_VALUE = "value";
	public static final String SALT = "salt";
	public static final String PASS_KEY = "password_hash";

	
	protected void create(SQLiteDatabase db) {
		
		String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + COLUMN_KEY + " TEXT UNIQUE NOT NULL, " + COLUMN_VALUE + " TEXT)";
		db.execSQL(sql);
	}
	
	protected int upsert(String key, String val, SQLiteDatabase db){
		ContentValues values = new ContentValues();
		values.put(COLUMN_KEY, key);
		values.put(COLUMN_VALUE, val);

		if (db.update(TABLE_NAME, values, COLUMN_KEY + " = " + DatabaseUtils.sqlEscapeString(key), null) == 0){
			if (db.insert(TABLE_NAME, null, values) == -1){
				//if updating failed and inserting failed something is wrong with our database
				ZLog.log("Error trying to insert to database");
				return -1;
			}
		}
		return 1;
	}
	
	protected int upsert(String key, boolean val, SQLiteDatabase db){
		ContentValues values = new ContentValues();
		values.put(COLUMN_KEY, key);
		values.put(COLUMN_VALUE, val);

		if (db.update(TABLE_NAME, values, COLUMN_KEY + " = " + DatabaseUtils.sqlEscapeString(key), null) == 0){
			if (db.insert(TABLE_NAME, null, values) == -1){
				//if updating failed and inserting failed something is wrong with our database
				ZLog.log("Error trying to insert to database");
				return -1;
			}
		}
		return 1;
	}
	
	protected String getStringFromKey(String key, SQLiteDatabase db){
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
	
	protected Boolean getBooleanFromKey(String key, SQLiteDatabase db){
		String selectQuery = "SELECT " + COLUMN_VALUE + " FROM " + TABLE_NAME + " WHERE " + COLUMN_KEY + " = " + DatabaseUtils.sqlEscapeString(key);
		Cursor c = db.rawQuery(selectQuery, null);
		if (c.moveToFirst()) {
			int val = c.getInt(c.getColumnIndex(COLUMN_VALUE));
			c.close();
			return val == 0 ? false : true;
		} else {
			c.close();
			//no row found = no internet or api call failed when retreiving fiat rates return 0
			return null;
		}
	}	
	
}
