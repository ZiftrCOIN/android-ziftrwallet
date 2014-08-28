package com.ziftr.android.onewallet.sqlite;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.onewallet.util.OWCoin;


public class OWCoinActivationStatusTable {

	/** The id column. All Tables must have this to work well with adapters. */
	public static final String COLUMN_ID = "_id";

	/** The title of the column that contains a string identifying the OWCoin.Type for the row. */
	public static final String COLUMN_COIN_ID = "coin_type";

	/** The title of the column that keeps track of the un/de/activated status. */
	public static final String COLUMN_ACTIVATED_STATUS = "status";

	/** The name of the table to store the status of all the coin types. */
	public static final String TABLE_NAME = "coin_activation_status";

	/** The string that can be used to create the coin activation status table. */
	public static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + 
			COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
			COLUMN_COIN_ID + " TEXT NOT NULL, " + 
			COLUMN_ACTIVATED_STATUS + " INTEGER );";

	protected static void create(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE);
	}

	protected static void insert(SQLiteDatabase db, OWCoin.Type coinId, int status) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_COIN_ID, coinId.toString());
		values.put(COLUMN_ACTIVATED_STATUS, status);

		db.insert(TABLE_NAME, null, values);
	}

	protected static List<OWCoin.Type> getAllActivatedTypes(SQLiteDatabase db) {
		List<OWCoin.Type> activatedCoinTypes = new ArrayList<OWCoin.Type>();

		String selectQuery = "SELECT * FROM " + TABLE_NAME + " WHERE " + 
				COLUMN_ACTIVATED_STATUS + " = " + OWSQLiteOpenHelper.ACTIVATED + ";";
		Cursor c = db.rawQuery(selectQuery, null);

		// Move to first returns false if cursor is empty
		if (c.moveToFirst()) {
			do {
				// Add the coin type to the list
				activatedCoinTypes.add(OWCoin.Type.valueOf(
						c.getString(c.getColumnIndex(COLUMN_COIN_ID))));
			} while (c.moveToNext());
		}

		// Make sure we close the cursor
		c.close();

		return activatedCoinTypes;
	}

	protected static int getActivatedStatus(SQLiteDatabase db, OWCoin.Type coinId) {
		String selectQuery = "SELECT * FROM " + TABLE_NAME + " WHERE " + 
				COLUMN_COIN_ID + " = '" + coinId.toString() + "';";
		Cursor c = db.rawQuery(selectQuery, null);

		// Move to first returns false if cursor is empty
		if (c.moveToFirst()) {
			if (!c.isLast()) {
				c.close();
				throw new RuntimeException("There was more than one row in sql query.");
			} else {
				int activatedStatus = c.getInt(c.getColumnIndex(COLUMN_ACTIVATED_STATUS));
				c.close();
				return activatedStatus;
			}
		} else {
			c.close();
			throw new RuntimeException("Row does not exist in table.");
		}
	}

	protected static void update(OWCoin.Type coinId, int status, SQLiteDatabase db) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_ACTIVATED_STATUS, status);
		db.update(TABLE_NAME, values, COLUMN_COIN_ID + " = '" + coinId.toString() + "'", null);
	}

}