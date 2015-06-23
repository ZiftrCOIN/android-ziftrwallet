/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.sqlite;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.ziftrwallet.crypto.ZWExtendedPublicKey;
import com.ziftr.android.ziftrwallet.crypto.ZWHdAccount;
import com.ziftr.android.ziftrwallet.crypto.ZWHdChildNumber;
import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;


// TODO make a column for balance so that we don't have to loop through transaction table? 
public class ZWAccountsTable {
	
	private static final String TABLE_NAME = "hd_accounts";

	public static final String COLUMN_SYMBOL = "symbol";	
	public static final String COLUMN_HD_PURPOSE = "purpose";
	public static final String COLUMN_HD_COIN_TYPE = "coin_type";
	public static final String COLUMN_HD_ACCOUNT = "account";
	public static final String COLUMN_HD_XPUB = "xpub";

	protected void create(SQLiteDatabase db) {
		
		// Create table with required columns coin symbol and account #, where each pair is unique
		String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + 
					COLUMN_SYMBOL + " TEXT NOT NULL," +
					COLUMN_HD_ACCOUNT + " TEXT NOT NULL, " +
					"UNIQUE (" + COLUMN_SYMBOL + ", " + COLUMN_HD_ACCOUNT + ")" +
				") ";
		db.execSQL(sql);

		// Add all other columns
		this.addColumn(COLUMN_SYMBOL, "TEXT NOT NULL", db);
		this.addColumn(COLUMN_HD_PURPOSE, "TEXT NOT NULL", db);
		this.addColumn(COLUMN_HD_PURPOSE, "TEXT NOT NULL", db);
		this.addColumn(COLUMN_HD_XPUB, "TEXT NOT NULL", db);
	}
	
	private void addColumn(String columnName, String constraints, SQLiteDatabase database) {
		try {
			String sql = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + columnName + " " + constraints;
			database.execSQL(sql);
		}
		catch(Exception e) {
			// Quietly fail when adding columns, they likely already exist
		}
	}
	
	protected List<ZWHdAccount> getAllAccounts(String where, SQLiteDatabase db) {
		ArrayList<ZWHdAccount> accounts = new ArrayList<ZWHdAccount>();
		
		String sql = "SELECT * FROM " + TABLE_NAME;
		if (where != null && !where.isEmpty())
			sql += " WHERE " + where;
		
		Cursor cursor = db.rawQuery(sql, null);
		
		if (cursor.moveToFirst()) {
			do {
				ZWHdAccount account = cursorToAccount(cursor);
				accounts.add(account);
			} while (cursor.moveToNext());
		}

		// Make sure we close the cursor
		cursor.close();
		
		return accounts;
	}
	
	protected ZWHdAccount getAccount(String coinSymbol, SQLiteDatabase db){
		String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_SYMBOL + " = " + DatabaseUtils.sqlEscapeString(coinSymbol);
		
		ZWHdAccount account = null;
		
		Cursor cursor = db.rawQuery(sql, null);
		if (cursor.moveToFirst()) {
			account = cursorToAccount(cursor);
		}
		cursor.close();
		
		return account;
	}

	public void upsertAccount(ZWHdAccount account, SQLiteDatabase db) {
		
		ContentValues cv = getContentValues(account);
		
		try {
			db.insertOrThrow(TABLE_NAME, null, cv);
		} 
		catch(Exception e) {
			//failed to insert, likely because the coin already exists in the database, so just update it
			String where = COLUMN_SYMBOL + " = " + DatabaseUtils.sqlEscapeString(account.symbol) + 
					" AND " + COLUMN_HD_ACCOUNT + " = " + DatabaseUtils.sqlEscapeString(account.account.toString());
			db.update(TABLE_NAME, cv, where, null);
		}
	}

	private ZWHdAccount cursorToAccount(Cursor cursor) {
		try {
			String symbol = cursor.getString(cursor.getColumnIndex(COLUMN_SYMBOL));
			String purpose = cursor.getString(cursor.getColumnIndex(COLUMN_HD_PURPOSE));
			String coinType = cursor.getString(cursor.getColumnIndex(COLUMN_HD_COIN_TYPE));
			String account = cursor.getString(cursor.getColumnIndex(COLUMN_HD_ACCOUNT));
			String xpub = cursor.getString(cursor.getColumnIndex(COLUMN_HD_XPUB));
			return new ZWHdAccount(symbol, 
					new ZWHdChildNumber(purpose), 
					new ZWHdChildNumber(coinType), 
					new ZWHdChildNumber(account), 
					new ZWExtendedPublicKey(xpub));
		} catch (ZWAddressFormatException e) {
			return null;
		}
	}
	
	private ContentValues getContentValues(ZWHdAccount account) {
		ContentValues content = new ContentValues();
		
		content.put(COLUMN_SYMBOL, account.symbol);
		content.put(COLUMN_HD_PURPOSE, account.purpose.toString());
		content.put(COLUMN_HD_COIN_TYPE, account.coinType.toString());
		content.put(COLUMN_HD_ACCOUNT, account.account.toString());
		content.put(COLUMN_HD_XPUB, account.xpubkey.xpub(account.isTestnet()));
		
		//these are properties of the coin and extremely unlikely to change
		return content;
	}

}
