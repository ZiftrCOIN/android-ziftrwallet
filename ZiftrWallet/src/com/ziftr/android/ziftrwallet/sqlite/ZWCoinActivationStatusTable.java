package com.ziftr.android.ziftrwallet.sqlite;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.util.ZLog;


// TODO make a column for balance so that we don't have to loop through transaction table? 
public class ZWCoinActivationStatusTable {

	/** The title of the column that contains a string identifying the ZWCoin for the row. */
	public static final String COLUMN_COIN_ID = "coin_type";

	/** The title of the column that keeps track of the un/de/activated status. */
	public static final String COLUMN_ACTIVATED_STATUS = "status";
	
	/** latest blockchain received from server */
	public static final String COLUMN_LATEST_BLOCKCHAIN = "blockchain";
	
	protected String getTableName() {
		return "coin_activation_status";
	}

	/** 
	 * The string that can be used to create the coin activation status table. 
	 */
	protected String getCreateTableString() {
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE IF NOT EXISTS ").append(getTableName()).append(" (");

		sb.append(COLUMN_COIN_ID).append(" TEXT UNIQUE NOT NULL, ");
		sb.append(COLUMN_ACTIVATED_STATUS).append(" INTEGER, ");
		sb.append(COLUMN_LATEST_BLOCKCHAIN).append(" INTEGER );");
		return sb.toString();
	}
	
	protected void create(SQLiteDatabase db) {
		db.execSQL(getCreateTableString());
	}
	
	protected void insert(ZWCoin coinId, int status, int blockNum, SQLiteDatabase db) {
		try {
			ContentValues values = new ContentValues();
			values.put(COLUMN_COIN_ID, coinId.getShortTitle());
			values.put(COLUMN_ACTIVATED_STATUS, status);
			values.put(COLUMN_LATEST_BLOCKCHAIN, blockNum);
			db.insert(getTableName(), null, values);
		}
		catch(Exception e) {
			//fail quietly, this could just be duplicate table issues during app setup
		}
	}

	protected List<ZWCoin> getTypes(SQLiteDatabase db, String specifier) {
		List<ZWCoin> coinTypes = new ArrayList<ZWCoin>();

		StringBuilder selectQuery = new StringBuilder();
		selectQuery.append("SELECT ").append(COLUMN_COIN_ID).append(" FROM ").append(getTableName()).append(" WHERE ");
		selectQuery.append(specifier);
		selectQuery.append(";");
		Cursor c = db.rawQuery(selectQuery.toString(), null);

		// Move to first returns false if cursor is empty
		if (c.moveToFirst()) {
			do {
				// Add the coin type to the list
				coinTypes.add(ZWCoin.valueOf(c.getString(c.getColumnIndex(COLUMN_COIN_ID))));
			} while (c.moveToNext());
		}

		// Make sure we close the cursor
		c.close();

		return coinTypes;
	}

	protected int getActivatedStatus(ZWCoin coin, SQLiteDatabase db) {
		String selectQuery = "SELECT * FROM " + getTableName() + " WHERE " + 
				COLUMN_COIN_ID + " = '" + coin.getShortTitle() + "';";
		Cursor c = db.rawQuery(selectQuery, null);

		// Move to first returns false if cursor is empty
		if (c.moveToFirst()) {
			if (!c.isLast()) {
				//c.close();
				//throw new RuntimeException("There was more than one row in sql query.");
				//no... don't crash the app because of possible database issues (especially since this would only effect display)
				ZLog.log("Multiple rows were returned from database for activation of coin: ", coin.getShortTitle(), " - ", coin.getType(), " - ", coin.getChain());
			}
			
			int activatedStatus = c.getInt(c.getColumnIndex(COLUMN_ACTIVATED_STATUS));
			c.close();
			return activatedStatus;
		
		} else {
			c.close();
			//TODO -the constants for activation should be part of the table class, not the sqlite helper
			//don't throw runtime exceptions for bad data
			//throw new RuntimeException("Row does not exist in table.");
			return ZWSQLiteOpenHelper.DEACTIVATED; //assume it's not activated
		}
	}

	protected void updateActivated(ZWCoin coinId, int status, SQLiteDatabase db) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_ACTIVATED_STATUS, status);
		db.update(getTableName(), values, COLUMN_COIN_ID + " = '" + coinId.getShortTitle() + "'", null);
	}
	
	protected void updateLatestBlock(ZWCoin coinId, int blockNum, SQLiteDatabase db){
		ContentValues values = new ContentValues();
		values.put(COLUMN_LATEST_BLOCKCHAIN, blockNum);
		db.update(getTableName(), values, COLUMN_COIN_ID + " = '" + coinId.getShortTitle() + "'", null);
	}

}