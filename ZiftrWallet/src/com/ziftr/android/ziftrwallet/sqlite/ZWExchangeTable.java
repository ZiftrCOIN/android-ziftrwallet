package com.ziftr.android.ziftrwallet.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;


public class ZWExchangeTable {
	
	/** The name of the table in the sqlite database */
	private static final String TABLE_NAME = "currency_exchange";
	
	/** The title of the column that contains a string identifying the ZWCurrency for the row. */
	public static final String COLUMN_CURRENCY_NAME= "name";

	/** String identifying what currency we are converting to */
	public static final String COLUMN_CURRENCY_TO = "currency_to";
	
	/** Value representing how many currency_to in 1 currency_name*/
	public static final String COLUMN_EXCHANGE_VALUE = "value";
	
	/** the column for displaying whether the exchange rate has gone up, down, or stated the same (+1, -1, 0 respectively) */
	public static final String COLUMN_RATE_CHANGE = "rate_change";
	
	
	private String getCreateTableString() {
		return "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + COLUMN_CURRENCY_NAME + " TEXT NOT NULL, " + COLUMN_CURRENCY_TO + 
		" TEXT NOT NULL, " + COLUMN_EXCHANGE_VALUE + " TEXT NOT NULL );";
	}
	
	protected void create(SQLiteDatabase db) {
		db.execSQL(getCreateTableString());
		
		try {
			String sql = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_RATE_CHANGE + " INTEGER";
			db.execSQL(sql);
		}
		catch(Exception e) {
			//just blindly throw alter table statements incase the user is using an older version
		}
	}
	
	protected void upsert(String currency, String convertTo , String val, SQLiteDatabase db) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_CURRENCY_NAME, currency);
		values.put(COLUMN_CURRENCY_TO, convertTo);
		values.put(COLUMN_EXCHANGE_VALUE, val);
		if (db.update(TABLE_NAME, values, whereIdentifier(currency, convertTo), null) == 0){
			db.insert(TABLE_NAME, null, values);
		}
	}
	
	protected String getExchangeVal(String currency, String convertTo, SQLiteDatabase db){
		String selectQuery = "SELECT " + COLUMN_EXCHANGE_VALUE + " FROM " + TABLE_NAME + " WHERE " + whereIdentifier(currency, convertTo);
		Cursor c = db.rawQuery(selectQuery, null);
		if (c.moveToFirst()) {
			if (!c.isLast()) {
				
			} 
			
			String val = c.getString(c.getColumnIndex(COLUMN_EXCHANGE_VALUE));
			c.close();
			return val;
		
		} else {
			c.close();
			//no row found = no internet or api call failed when retreiving fiat rates return 0
			return "0";
		}
	}
	
	private String whereIdentifier(String currency, String convertTo){
		return COLUMN_CURRENCY_NAME + " = " + DatabaseUtils.sqlEscapeString(currency) + " AND " + COLUMN_CURRENCY_TO + " = " + DatabaseUtils.sqlEscapeString(convertTo);
	}

}

