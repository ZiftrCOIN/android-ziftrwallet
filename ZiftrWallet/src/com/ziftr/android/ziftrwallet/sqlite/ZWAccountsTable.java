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

import com.ziftr.android.ziftrwallet.crypto.ZWHdAccount;
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
				COLUMN_HD_ACCOUNT + " INTEGER NOT NULL, " +
				"UNIQUE (" + COLUMN_SYMBOL + ", " + COLUMN_HD_ACCOUNT + ")" +
				") ";
		db.execSQL(sql);

		// Add all other columns
		this.addColumn(COLUMN_HD_PURPOSE, "INTEGER NOT NULL DEFAULT 44", db);
		this.addColumn(COLUMN_HD_COIN_TYPE, "INTEGER NOT NULL DEFAULT -1", db);
		this.addColumn(COLUMN_HD_XPUB, "TEXT NOT NULL DEFAULT " + DatabaseUtils.sqlEscapeString(""), db);
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

	private String whereSymbolAccount(String coinSymbol, int accountNumber) {
		return COLUMN_SYMBOL + " = " + DatabaseUtils.sqlEscapeString(coinSymbol) + 
				" AND " + COLUMN_HD_ACCOUNT + " = " + String.valueOf(accountNumber);
	}

	protected List<ZWHdAccount> getAllAccounts(String where, SQLiteDatabase db) {
		ArrayList<ZWHdAccount> accounts = new ArrayList<ZWHdAccount>();

		String sql = "SELECT * FROM " + TABLE_NAME;
		if (where != null && !where.isEmpty())
			sql += " WHERE " + where;

		Cursor cursor = db.rawQuery(sql, null);

		if (cursor.moveToFirst()) {
			do {
				accounts.add(cursorToAccount(cursor));
			} while (cursor.moveToNext());
		}

		// Make sure we close the cursor
		cursor.close();

		return accounts;
	}

	protected List<ZWHdAccount> getAccountsByCoin(String symbol, SQLiteDatabase db) {
		return this.getAllAccounts(COLUMN_SYMBOL + " = " + DatabaseUtils.sqlEscapeString(symbol), db);
	}

	protected ZWHdAccount getAccount(String coinSymbol, int accountNumber, SQLiteDatabase db) {
		List<ZWHdAccount> all = this.getAllAccounts(this.whereSymbolAccount(coinSymbol, accountNumber), db);
		return all.size() > 0 ? all.get(0) : null;
	}

	public void insertAccount(ZWHdAccount account, SQLiteDatabase db) {
		ContentValues cv = getContentValues(account);
		db.insertOrThrow(TABLE_NAME, null, cv);
	}

	private ZWHdAccount cursorToAccount(Cursor cursor) {
		try {
			String symbol = cursor.getString(cursor.getColumnIndex(COLUMN_SYMBOL));
			int purpose = cursor.getInt(cursor.getColumnIndex(COLUMN_HD_PURPOSE));
			int coinType = cursor.getInt(cursor.getColumnIndex(COLUMN_HD_COIN_TYPE));
			int account = cursor.getInt(cursor.getColumnIndex(COLUMN_HD_ACCOUNT));
			String xpub = cursor.getString(cursor.getColumnIndex(COLUMN_HD_XPUB));
			return new ZWHdAccount(symbol, purpose, coinType, account, xpub);
		} catch (ZWAddressFormatException e) {
			return null;
		}
	}

	private ContentValues getContentValues(ZWHdAccount account) {
		ContentValues content = new ContentValues();

		content.put(COLUMN_SYMBOL, account.symbol);
		content.put(COLUMN_HD_PURPOSE, account.purpose.getIndex());
		content.put(COLUMN_HD_COIN_TYPE, account.coinType.getIndex());
		content.put(COLUMN_HD_ACCOUNT, account.account.getIndex());
		content.put(COLUMN_HD_XPUB, account.xpubkey.xpub(account.isMainnet()));

		return content;
	}

}
