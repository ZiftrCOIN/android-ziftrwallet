package com.ziftr.android.ziftrwallet.sqlite;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;

import com.ziftr.android.ziftrwallet.util.ZLog;

import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQuery;
import android.util.Log;

public class ZiftrDebugSQLiteCursor extends SQLiteCursor {

	private static ArrayList<WeakReference<ZiftrDebugSQLiteCursor>> activeCursors = new ArrayList<WeakReference<ZiftrDebugSQLiteCursor>>();
	
	private static int openedCursors = 0;
	private static int explicitlyClosedCursors = 0;
	
	private String cursorCreationStack = "";
	
	public ZiftrDebugSQLiteCursor(SQLiteCursorDriver driver, String editTable, SQLiteQuery query) {
		super(driver, editTable, query);
		
		WeakReference<ZiftrDebugSQLiteCursor> reference = new WeakReference<ZiftrDebugSQLiteCursor>(this);
		
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		for(int x = 3; x < stack.length; x++) {
			cursorCreationStack += stack[x].toString() + "\n";
		}
		
		addActiveCursor(reference);
		openedCursors++;
	}
	
	
	private synchronized static void addActiveCursor(WeakReference<ZiftrDebugSQLiteCursor> cursorReference) {
		for(int x = 0; x < activeCursors.size(); x++) {
			WeakReference<ZiftrDebugSQLiteCursor> ref = activeCursors.get(x);
			if(ref.get() == null) {
				activeCursors.remove(x);
				x--;
			}
		}
		
		activeCursors.add(cursorReference);
		ZLog.log("\n Tracking " + String.valueOf(activeCursors.size()) + " active cursors. \n");
	}
	
	
	public ZiftrDebugSQLiteCursor(SQLiteDatabase db, SQLiteCursorDriver driver, String editTable, SQLiteQuery query) {
		this(driver, editTable, query);
	}


	@Override
	public void close() {
		explicitlyClosedCursors++;
		super.close();
		
		ZLog.log("Closed Ziftr Cursor: \n" + String.valueOf(openedCursors - explicitlyClosedCursors) + " cursors remain open.");
	}


	@Override
	protected void finalize() {
		
		if(!this.isClosed()) {
			try {
				Field mQuery = this.getClass().getSuperclass().getDeclaredField("mQuery");
				mQuery.setAccessible(true);
				SQLiteQuery query = (SQLiteQuery) mQuery.get(this);
				mQuery.setAccessible(false);
				String sql = query.toString();
				
				ZLog.log("Failed to explicitly close a cursor: \n", sql);
				ZLog.log("Cursor creation: \n", cursorCreationStack);
			}
			catch(Exception e) {
				ZLog.log("Exception getting cursor log data: ", e);
			}
		}
		
		super.finalize();
		
		if(!this.isClosed()) {
			Log.e("ZiftrSQLiteCursor", "WARNING! A cursor was not automatically closed!");
		}
	}


	
	

}
