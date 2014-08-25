package com.ziftr.android.onewallet.sqlite;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.ziftr.android.onewallet.crypto.ECKey;
import com.ziftr.android.onewallet.util.OWCoin;

public class OWSQLiteOpenHelper extends SQLiteOpenHelper {

	public static final String DATABASE_NAME = "wallet.dat";

	public static final int DATABASE_VERSION = 1;

	public OWSQLiteOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// 
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
	}

	//////////////////////////////////////////////////////////////////////
	//////////  Interface for working with personal addresses  ///////////
	//////////////////////////////////////////////////////////////////////

	public void insert(OWCoin.Type coinId, ECKey key) {
		OWUsersAddressesTable.insert(coinId, key, this.getWritableDatabase());
	}

	/**
	 * Make sure to call cursor.close() on the result of this method. 
	 * 
	 * @return
	 */
	public Cursor getAllUsersAddresses(OWCoin.Type coinId) {
		Cursor cursor = OWUsersAddressesTable.getAllUserAddresses(coinId, this.getReadableDatabase());
		cursor.moveToFirst();
		return cursor;
	}

}