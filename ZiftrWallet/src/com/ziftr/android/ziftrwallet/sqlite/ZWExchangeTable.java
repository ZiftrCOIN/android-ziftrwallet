package com.ziftr.android.ziftrwallet.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.ziftrwallet.crypto.ZWCurrency;


public class ZWExchangeTable {
	
	/** The title of the column that contains a string identifying the ZWCurrency for the row. */
	public static final String COLUMN_CURRENCY_NAME= "name";

	/** String identifying what currency we are converting to */
	public static final String COLUMN_CURRENCY_TO = "currency_to";
	
	/** Value representing how many currency_to in 1 currency_name*/
	public static final String COLUMN_EXCHANGE_VALUE = "value";
	
	protected String getTableName() {
		return "currency_exchange";
	}
	
	protected String getCreateTableString() {
		return "CREATE TABLE IF NOT EXISTS " + getTableName() + " (" + COLUMN_CURRENCY_NAME + " TEXT NOT NULL, " + COLUMN_CURRENCY_TO + 
		" TEXT NOT NULL, " + COLUMN_EXCHANGE_VALUE + " TEXT NOT NULL );";
	}
	
	protected void create(SQLiteDatabase db) {
		db.execSQL(getCreateTableString());
	}
	
	protected void upsert(ZWCurrency currency, ZWCurrency convertTo , String val, SQLiteDatabase db) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_CURRENCY_NAME, currency.getShortTitle());
		values.put(COLUMN_CURRENCY_TO, convertTo.getShortTitle());
		values.put(COLUMN_EXCHANGE_VALUE, val);
		if (db.update(getTableName(), values, whereIdentifier(currency, convertTo), null) == 0){
			db.insert(getTableName(), null, values);
		}
	}
	
	protected String getExchangeVal(ZWCurrency currency, ZWCurrency convertTo,SQLiteDatabase db){
		String selectQuery = "SELECT " + COLUMN_EXCHANGE_VALUE + " FROM " + getTableName() + " WHERE " + whereIdentifier(currency, convertTo);
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
	
	private String whereIdentifier(ZWCurrency currency, ZWCurrency convertTo){
		return COLUMN_CURRENCY_NAME + " = '" + currency.getShortTitle() + "' AND " + COLUMN_CURRENCY_TO + " = '" + convertTo.getShortTitle() + "'";
	}

}

