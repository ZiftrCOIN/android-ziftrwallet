package com.ziftr.android.ziftrwallet.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQuery;


public class ZiftrCursorFactory implements SQLiteDatabase.CursorFactory {

	public static ZiftrCursorFactory newFactory() {
		
		//if we want to use a debugging cursor, or some other custom cursor, return our factory
		//return new ZiftrCursorFactory();
		
		//otherwise, for default behavior, return null and let the sqlite libraries handle factory
		return null;
	}
	
	
	private ZiftrCursorFactory() {
		
	}
	
	
	@Override
	public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery, String editTable, SQLiteQuery query) {
		return new ZiftrDebugSQLiteCursor(masterQuery, editTable, query);
	}

}
