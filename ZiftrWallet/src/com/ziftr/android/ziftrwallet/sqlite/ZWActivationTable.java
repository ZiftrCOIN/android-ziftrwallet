package com.ziftr.android.ziftrwallet.sqlite;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.util.ZLog;


// TODO make a column for balance so that we don't have to loop through transaction table? 
public class ZWActivationTable {
	
	private static final String TABLE_NAME = "coin_activation_status";

	/** The title of the column that contains a string identifying the ZWCoin for the row. */
	public static final String COLUMN_COIN_ID = "coin_type";

	/** The title of the column that keeps track of the un/de/activated status. */
	public static final String COLUMN_ACTIVATED_STATUS = "status";
	
	/** is the service side service/api available for this coin */
	public static final String COLUMN_AVAILABLE_STATUS = "is_available";
	
	/** latest blockchain received from server */
	public static final String COLUMN_LATEST_BLOCKCHAIN = "blockchain";
	
	
	/** Used for table types that just haven't been used yet. */
	public static final int UNACTIVATED = 0;

	/** Used for table types that are activated and in use by user. */
	public static final int ACTIVATED = 1;

	/** Used for table types that used to be ACTIVATED, but now user deactivated them. */
	public static final int DEACTIVATED = 2;
	

	protected void create(SQLiteDatabase db) {
		//db.execSQL(getCreateTableString());
		
		//create basic table with required column for coin id
		String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + COLUMN_COIN_ID + " TEXT UNIQUE NOT NULL)";
		db.execSQL(sql);
		
		//add activation status column
		try {
			sql = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_ACTIVATED_STATUS + " INTEGER";
			db.execSQL(sql);
		}
		catch(Exception e) {
			//just blindly throw alter table statements incase the user is using an older version
		}
		
		//add latest block chain height column
		try {
			sql = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_LATEST_BLOCKCHAIN + " INTEGER";
			db.execSQL(sql);
		}
		catch(Exception e) {
			//just blindly throw alter table statements incase the user is using an older version
		}
		
		//add availability column
		try {
			sql = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_AVAILABLE_STATUS + " INTEGER";
			db.execSQL(sql);
		}
		catch(Exception e) {
			//just blindly throw alter table statements incase the user is using an older version
		}
	}
	
	
	protected void insertDefault(ZWCoin coin, SQLiteDatabase db) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_COIN_ID, coin.getShortTitle());
		values.put(COLUMN_ACTIVATED_STATUS, UNACTIVATED);
		values.put(COLUMN_LATEST_BLOCKCHAIN, 0);
		values.put(COLUMN_AVAILABLE_STATUS, false);
		db.insert(TABLE_NAME, null, values);
	}
	
	
	/**
	 * query the activation database for a list of coins with specific status,
	 * will use bitwise operators allowing querying for multiple status,
	 * eg ACTIVATED | DEACTIVATED
	 * @param activationStatus the activation status (or bitwise of multiple status) to query for
	 * @param db the db to query
	 * @return list of {@link ZWCoin} with the matching status(es)
	 */
	protected List<ZWCoin> getCoinsByStatus(int activationStatus, SQLiteDatabase db) {
		ArrayList<ZWCoin> coins = new ArrayList<ZWCoin>();
		
		String sql = "SELECT " + COLUMN_COIN_ID + " FROM " + TABLE_NAME + 
				" WHERE (" + COLUMN_ACTIVATED_STATUS + " | " + String.valueOf(activationStatus) +
				") = " + COLUMN_ACTIVATED_STATUS;
		
		Cursor cursor = db.rawQuery(sql, null);
		
		if (cursor.moveToFirst()) {
			do {
				// Add the coin type to the list
				ZWCoin coin = ZWCoin.valueOf(cursor.getString(0));
				coins.add(coin);
			} while (cursor.moveToNext());
		}

		// Make sure we close the cursor
		cursor.close();
		
		return coins;
	}
	

	
	protected boolean isCoinActivated(ZWCoin coin, SQLiteDatabase db) {
		
		boolean isActivated = false;
		
		String sql = "SELECT " + COLUMN_ACTIVATED_STATUS + " FROM " + TABLE_NAME + 
				" WHERE " + COLUMN_COIN_ID + " = " + DatabaseUtils.sqlEscapeString(coin.getShortTitle());
		
		Cursor cursor = db.rawQuery(sql, null);
		if(cursor.moveToFirst()) {
			if(!cursor.isLast()) {
				ZLog.log("Multiple rows were returned from database for activation of coin: ", coin.getShortTitle(), " - ", coin.getType(), " - ", coin.getChain());
			}
			
			int activatedStatus = cursor.getInt(0); //only 1 column asked for
			if(activatedStatus == ACTIVATED) {
				isActivated = true;
			}
		}

		cursor.close();
		
		return isActivated;
	}


	protected void setActivation(ZWCoin coin, int activationStatus, SQLiteDatabase db) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_ACTIVATED_STATUS, activationStatus);
		String whereClause = COLUMN_COIN_ID + " = " + DatabaseUtils.sqlEscapeString(coin.getShortTitle());
		db.update(TABLE_NAME, values, whereClause, null);
	}
	
	protected void updateLatestBlock(ZWCoin coinId, int blockNum, SQLiteDatabase db){
		ContentValues values = new ContentValues();
		values.put(COLUMN_LATEST_BLOCKCHAIN, blockNum);
		db.update(TABLE_NAME, values, COLUMN_COIN_ID + " = '" + coinId.getShortTitle() + "'", null);
	}
	
	
	
	
	

}









