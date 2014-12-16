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
public class ZWCoinTable {
	
	private static final String TABLE_NAME = "coin_status";

	/** The title of the column that contains a string identifying the ZWCoin for the row. */
	public static final String COLUMN_COIN_ID = "coin_id";

	/** The title of the column that keeps track of the un/de/activated status. */
	public static final String COLUMN_ACTIVATED_STATUS = "activated_status";
	
	/** latest blockchain received from server */
	public static final String COLUMN_LATEST_BLOCKCHAIN = "blockchain";
	
	public static final String COLUMN_DEFAULT_FEE_PER_KB = "default_fee_per_kb";
	
	public static final String COLUMN_PUB_KEY_PREFIX = "pub_key_hash_prefix";
	
	public static final String COLUMN_SCRIPT_PREFIX = "script_hash_prefix";

	public static final String COLUMN_PRIV_KEY_PREFIX = "priv_key_hash_prefix";
	
	/** Average seconds per block solve */
	public static final String COLUMN_BLOCK_TIME = "block_time";

	public static final String COLUMN_RECOMMENDED_CONFIRMS = "recommended_confirmations";
	
	/** if server has enabled the coin (as opposed to activated by client)*/
	public static final String COLUMN_ENABLED = "is_enabled";

	public static final String COLUMN_CHAIN = "chain";
	
	public static final String COLUMN_TYPE = "type";
	
	/** Used for table types that just haven't been used yet. */
	public static final int UNACTIVATED = 0;

	/** Used for table types that are activated and in use by user. */
	public static final int ACTIVATED = 1;

	/** Used for table types that used to be ACTIVATED, but now user deactivated them. */
	public static final int DEACTIVATED = 2;
	

	protected void create(SQLiteDatabase db) {
		
		//create basic table with required column for coin id
		String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + COLUMN_COIN_ID + " TEXT UNIQUE NOT NULL)";
		db.execSQL(sql);
		
		//add activation status column
		this.addColumn(COLUMN_ACTIVATED_STATUS, "INTEGER", db);
		
		//add latest block chain height column
		this.addColumn(COLUMN_LATEST_BLOCKCHAIN, "INTEGER", db);
		
		this.addColumn(COLUMN_DEFAULT_FEE_PER_KB, "INTEGER", db);
		
		this.addColumn(COLUMN_PUB_KEY_PREFIX, "INTEGER", db);
		
		this.addColumn(COLUMN_PRIV_KEY_PREFIX, "INTEGER", db);

		this.addColumn(COLUMN_SCRIPT_PREFIX, "INTEGER", db);

		this.addColumn(COLUMN_BLOCK_TIME, "INTEGER", db);
		
		this.addColumn(COLUMN_RECOMMENDED_CONFIRMS, "INTEGER", db);
		
		this.addColumn(COLUMN_ENABLED, "INTEGER", db);
		
		this.addColumn(COLUMN_CHAIN, "TEXT", db);
		
		this.addColumn(COLUMN_TYPE, "TEXT", db);

	}
		
	protected void insertDefault(ZWCoin coin, SQLiteDatabase db) {
		String sql = "INSERT OR IGNORE INTO " + TABLE_NAME + 
				"(" + COLUMN_COIN_ID + "," + COLUMN_ACTIVATED_STATUS + "," + COLUMN_LATEST_BLOCKCHAIN + "," + COLUMN_ENABLED +
				") VALUES (" + DatabaseUtils.sqlEscapeString(coin.getShortTitle()) + "," + String.valueOf(UNACTIVATED) + "," + "0, 0)";
		try {
			db.execSQL(sql);
		}
		catch(Exception e) {
		}
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
	
	public void updateCoinDb(ZWCoin coinId, int blockNum, int defaultFee, int pubKeyPrefix, int scriptHashPrefix, int privKeyPrefix, 
			int confirmationsNeeded, int blockGenTime, String chain, String type, boolean enabled, SQLiteDatabase db){
		ContentValues values = new ContentValues();
		values.put(COLUMN_LATEST_BLOCKCHAIN, blockNum);
		values.put(COLUMN_DEFAULT_FEE_PER_KB, defaultFee);
		values.put(COLUMN_PUB_KEY_PREFIX, pubKeyPrefix);
		values.put(COLUMN_SCRIPT_PREFIX, scriptHashPrefix);
		values.put(COLUMN_PRIV_KEY_PREFIX, privKeyPrefix);
		values.put(COLUMN_RECOMMENDED_CONFIRMS, confirmationsNeeded);
		values.put(COLUMN_BLOCK_TIME, blockGenTime);
		values.put(COLUMN_CHAIN, chain);
		values.put(COLUMN_TYPE, type);
		values.put(COLUMN_ENABLED, enabled);
		db.update(TABLE_NAME, values, COLUMN_COIN_ID + " = " + DatabaseUtils.sqlEscapeString(coinId.getShortTitle()), null);
	}
	
	public void updateCoin(ZWCoin coin, SQLiteDatabase db){
		String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_COIN_ID + " = " + DatabaseUtils.sqlEscapeString(coin.getShortTitle());
		Cursor cursor = db.rawQuery(sql, null);
		if (cursor != null && cursor.moveToFirst()){
			int defaultFee = cursor.getInt(cursor.getColumnIndex(COLUMN_DEFAULT_FEE_PER_KB));
			byte pubKeyPrefix = (byte) cursor.getInt(cursor.getColumnIndex(COLUMN_PUB_KEY_PREFIX));
			byte privKeyPrefix = (byte) cursor.getInt(cursor.getColumnIndex(COLUMN_PRIV_KEY_PREFIX));
			byte scriptHashPrefix = (byte) cursor.getInt(cursor.getColumnIndex(COLUMN_SCRIPT_PREFIX));
			int confirmationsNeeded = cursor.getInt(cursor.getColumnIndex(COLUMN_RECOMMENDED_CONFIRMS));
			int blockGenTime = cursor.getInt(cursor.getColumnIndex(COLUMN_BLOCK_TIME));
			String chain = cursor.getString(cursor.getColumnIndex(COLUMN_CHAIN));
			coin.updateCoin(defaultFee, pubKeyPrefix, scriptHashPrefix, privKeyPrefix, confirmationsNeeded, blockGenTime, chain);
			ZLog.log(coin.getShortTitle() + " " + coin.getChain());
		}
	}
	
	public List<ZWCoin> getEnabledCoins(SQLiteDatabase db) {
		String sql = "SELECT " + COLUMN_COIN_ID + " FROM " + TABLE_NAME + " WHERE " + COLUMN_ENABLED + " = 1";
		ArrayList<ZWCoin> enabledCoins = new ArrayList<ZWCoin>();

		Cursor cursor = db.rawQuery(sql, null);
		
		if(cursor != null && cursor.moveToFirst()) {
			do {
				ZWCoin coin = ZWCoin.valueOf(cursor.getString(cursor.getColumnIndex(COLUMN_COIN_ID)));
				enabledCoins.add(coin);
			}
			while(cursor.moveToNext());
		}
		
		cursor.close();		
		return enabledCoins;
	}
	
	private void addColumn(String columnName, String constraints, SQLiteDatabase database) {
		try {
			String sql = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + columnName + " " + constraints;
			database.execSQL(sql);
		}
		catch(Exception e) {
			//quietly fail when adding columns, they likely already exist
		}
	}

}
