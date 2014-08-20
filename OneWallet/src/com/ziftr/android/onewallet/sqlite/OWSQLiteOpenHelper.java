package com.ziftr.android.onewallet.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;

public abstract class OWSQLiteOpenHelper extends SQLiteOpenHelper {
	
	public static final String DATABASE_NAME = "wallet.dat";
	
	public static final int DATABASE_VERSION = 1;
	
	public OWSQLiteOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
}