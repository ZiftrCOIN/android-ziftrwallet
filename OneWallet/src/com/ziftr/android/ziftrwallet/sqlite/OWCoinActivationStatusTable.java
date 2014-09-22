package com.ziftr.android.ziftrwallet.sqlite;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.ziftrwallet.util.OWCoin;


public class OWCoinActivationStatusTable extends OWTable {

	/** The title of the column that contains a string identifying the OWCoin for the row. */
	public static final String COLUMN_COIN_ID = "coin_type";

	/** The title of the column that keeps track of the un/de/activated status. */
	public static final String COLUMN_ACTIVATED_STATUS = "status";

	protected String getTableName() {
		return "coin_activation_status";
	}

	/** 
	 * The string that can be used to create the coin activation status table. 
	 */
	protected String getCreateTableString() {
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE IF NOT EXISTS ").append(getTableName()).append(" (");
		sb.append(COLUMN_ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
		sb.append(COLUMN_COIN_ID).append(" TEXT NOT NULL, ");
		sb.append(COLUMN_ACTIVATED_STATUS).append(" INTEGER );");
		return sb.toString();
	}
	
	protected void create(SQLiteDatabase db) {
		db.execSQL(getCreateTableString());
	}
	
	protected void insert(OWCoin coinId, int status, SQLiteDatabase db) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_COIN_ID, coinId.toString());
		values.put(COLUMN_ACTIVATED_STATUS, status);

		db.insert(getTableName(), null, values);
	}

	protected List<OWCoin> getAllActivatedTypes(SQLiteDatabase db) {
		List<OWCoin> activatedCoinTypes = new ArrayList<OWCoin>();

		String selectQuery = "SELECT * FROM " + getTableName() + " WHERE " + 
				COLUMN_ACTIVATED_STATUS + " = " + OWSQLiteOpenHelper.ACTIVATED + ";";
		Cursor c = db.rawQuery(selectQuery, null);

		// Move to first returns false if cursor is empty
		if (c.moveToFirst()) {
			do {
				// Add the coin type to the list
				activatedCoinTypes.add(OWCoin.valueOf(
						c.getString(c.getColumnIndex(COLUMN_COIN_ID))));
			} while (c.moveToNext());
		}

		// Make sure we close the cursor
		c.close();

		return activatedCoinTypes;
	}

	protected int getActivatedStatus(OWCoin coinId, SQLiteDatabase db) {
		String selectQuery = "SELECT * FROM " + getTableName() + " WHERE " + 
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

	protected void update(OWCoin coinId, int status, SQLiteDatabase db) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_ACTIVATED_STATUS, status);
		db.update(getTableName(), values, COLUMN_COIN_ID + " = '" + coinId.toString() + "'", null);
	}

}