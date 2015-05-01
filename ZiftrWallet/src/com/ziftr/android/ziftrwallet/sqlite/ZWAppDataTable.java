/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.ziftrwallet.ZWApplication;
import com.ziftr.android.ziftrwallet.util.ZLog;

public class ZWAppDataTable {
	
	private static final String TABLE_NAME = "app_data";
	private static final String COLUMN_KEY = "key";
	private static final String COLUMN_VALUE = "value";

	
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
		}
		else {
			c.close();
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
		} 
		else {
			c.close();
			return null;
		}
	}
	
	
	
	/**
	 * Older versions of the app used SharedPreferences to hold preference data.
	 * Since we need to be absolutely sure some of the data isn't lost, and we're using sqlite already,
	 * it makes sense to just store all preferences in the sqlite database
	 * That has the added benefit of making it easier to backup the user's preferences as well.
	 */
	public void upgradeFromOldPreferences(SQLiteDatabase db) {
		SharedPreferences prefs = ZWApplication.getApplication().getSharedPreferences("ziftrWALLET_Prefs", Context.MODE_PRIVATE);
		String oldSalt = prefs.getString("ziftrWALLET_salt_key", null);
		String oldPasswordHash = prefs.getString("ow_passphrase_key_1", null);
		
		if(oldSalt != null && oldPasswordHash != null) {
			this.upsert("salt", oldSalt, db);
			this.upsert("password_hash", oldPasswordHash, db);
		}
		
		String oldName = prefs.getString("ow_name_key", null);
		this.upsert("user_name", oldName, db);
	}
	
}
