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

import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.util.ZLog;


// TODO make a column for balance so that we don't have to loop through transaction table? 
public class ZWCoinTable {
	
	private static final String TABLE_NAME = "coin_status";


	public static final String COLUMN_SYMBOL = "symbol";	
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_ACTIVATED_STATUS = "activated_status";
	public static final String COLUMN_BLOCK_HEIGHT = "block_height";
	public static final String COLUMN_DEFAULT_FEE_PER_KB = "default_fee_per_kb";
	public static final String COLUMN_PUB_KEY_PREFIX = "pub_key_hash_prefix";
	public static final String COLUMN_SCRIPT_PREFIX = "script_hash_prefix";
	public static final String COLUMN_PRIV_KEY_PREFIX = "priv_key_hash_prefix";
	public static final String COLUMN_BLOCK_TIME = "block_time";
	public static final String COLUMN_RECOMMENDED_CONFIRMS = "recommended_confirmations";
	
	/** if server has enabled the coin (as opposed to activated by client)*/
	public static final String COLUMN_ENABLED = "is_enabled";
	public static final String COLUMN_HEALTH = "health";
	public static final String COLUMN_CHAIN = "chain";
	public static final String COLUMN_TYPE = "type";
	public static final String COLUMN_SCALE = "scale";
	public static final String COLUMN_SCHEME = "scheme";
	public static final String COLUMN_LOGO_URL = "logo_url";
	
	/**
	 * Each coin type has a branch of the HD wallet. 
	 */
	public static final String COLUMN_HD_COIN_ID = "hd_id";
	
	private static final int UNACTIVATED = 0; //Used for table types that just haven't been used yet.
	private static final int DEACTIVATED = 1; //Used for table types that used to be ACTIVATED, but now user deactivated them.
	private static final int ACTIVATED = 2; //Used for table types that are activated and in use by user (should always be largest value when adding other types).
	

	protected void create(SQLiteDatabase db) {
		
		//create basic table with required column for coin id
		String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + COLUMN_SYMBOL + " TEXT UNIQUE NOT NULL)";
		db.execSQL(sql);

		//add all other columns
		this.addColumn(COLUMN_NAME, "TEXT", db);
		this.addColumn(COLUMN_SCHEME, "TEXT", db);
		this.addColumn(COLUMN_SCALE, "INTEGER", db);
		this.addColumn(COLUMN_ACTIVATED_STATUS, "INTEGER", db);		
		this.addColumn(COLUMN_BLOCK_HEIGHT, "INTEGER", db);
		this.addColumn(COLUMN_DEFAULT_FEE_PER_KB, "INTEGER", db);
		this.addColumn(COLUMN_PUB_KEY_PREFIX, "INTEGER", db);
		this.addColumn(COLUMN_PRIV_KEY_PREFIX, "INTEGER", db);
		this.addColumn(COLUMN_SCRIPT_PREFIX, "INTEGER", db);
		this.addColumn(COLUMN_BLOCK_TIME, "INTEGER", db);
		this.addColumn(COLUMN_RECOMMENDED_CONFIRMS, "INTEGER", db);
		this.addColumn(COLUMN_CHAIN, "TEXT", db);
		this.addColumn(COLUMN_TYPE, "TEXT", db);
		this.addColumn(COLUMN_LOGO_URL, "TEXT", db);
		
		this.addColumn(COLUMN_ENABLED, "INTEGER", db);
		this.addColumn(COLUMN_HEALTH, "TEXT", db);
		
		this.addColumn(COLUMN_HD_COIN_ID, "INTEGER", db);
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
		
	public synchronized List<ZWCoin> getNotUnactiveCoins(SQLiteDatabase db) {
		String whereClause = COLUMN_ACTIVATED_STATUS + " != " + String.valueOf(UNACTIVATED);
		return this.getCoins(whereClause, db);
	}
	
	
	protected List<ZWCoin> getActiveCoins(SQLiteDatabase db) {
		String whereClause = COLUMN_ACTIVATED_STATUS + " >= " + String.valueOf(ACTIVATED);
		return getCoins(whereClause, db);
	}
	
	
	protected List<ZWCoin> getInactiveCoins(SQLiteDatabase db, boolean includeTestnet) {
		String whereClause = "(" + COLUMN_ACTIVATED_STATUS + " IS NULL OR " + COLUMN_ACTIVATED_STATUS + " < " + String.valueOf(ACTIVATED) + ")";
		if(!includeTestnet) {
			whereClause += " AND " + COLUMN_CHAIN + " = 'main'" + " AND " + COLUMN_ENABLED + " = 1";
		}
		
		
		return getCoins(whereClause, db);
	}
	
	
	private List<ZWCoin> getCoins(String whereClause, SQLiteDatabase db) {
		ArrayList<ZWCoin> coins = new ArrayList<ZWCoin>();
		
		String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + whereClause;
		
		Cursor cursor = db.rawQuery(sql, null);
		
		if (cursor.moveToFirst()) {
			do {
				ZWCoin coin = cursorToCoin(cursor);
				coins.add(coin);
			} while (cursor.moveToNext());
		}

		// Make sure we close the cursor
		cursor.close();
		
		return coins;
	}
	
	
	protected List<ZWCoin> getAllCoins(SQLiteDatabase database) {
		ArrayList<ZWCoin> coins = new ArrayList<ZWCoin>();
		
		String sql = "SELECT * FROM " + TABLE_NAME;
		
		Cursor cursor = database.rawQuery(sql, null);
		if(cursor.moveToFirst()) {
			do {
				ZWCoin coin = cursorToCoin(cursor);
				coins.add(coin);
			}
			while(cursor.moveToNext());
		}
		cursor.close();
		
		return coins;
	}
	
	protected ZWCoin getCoin(String coinSymbol, SQLiteDatabase db){
		String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_SYMBOL + " = " + DatabaseUtils.sqlEscapeString(coinSymbol);
		
		ZWCoin coin = null;
		
		Cursor cursor = db.rawQuery(sql, null);
		if(cursor.moveToFirst()) {
			coin = cursorToCoin(cursor);
		}
		cursor.close();
		
		return coin;
	}
	
	
	
	protected boolean isCoinActivated(ZWCoin coin, SQLiteDatabase db) {
		
		boolean isActivated = false;
		
		String sql = "SELECT " + COLUMN_ACTIVATED_STATUS + " FROM " + TABLE_NAME + 
				" WHERE " + COLUMN_SYMBOL + " = " + DatabaseUtils.sqlEscapeString(coin.getSymbol());
		
		Cursor cursor = db.rawQuery(sql, null);
		if(cursor.moveToFirst()) {
			if(!cursor.isLast()) {
				ZLog.log("Multiple rows were returned from database for activation of coin: ", coin.getSymbol(), " - ", coin.getType(), " - ", coin.getChain());
			}
			
			int activatedStatus = cursor.getInt(0); //only 1 column asked for
			if(activatedStatus == ACTIVATED) {
				isActivated = true;
			}
		}
		cursor.close();
		
		return isActivated;
	}

	
	protected void activateCoin(ZWCoin coin, SQLiteDatabase db) {
		this.setActivation(coin, ACTIVATED, db);
	}
	
	protected void deactivateCoin(ZWCoin coin, SQLiteDatabase db) {
		this.setActivation(coin, DEACTIVATED, db);
	}
	
	private void setActivation(ZWCoin coin, int activationStatus, SQLiteDatabase db) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_ACTIVATED_STATUS, activationStatus);
		String whereClause = COLUMN_SYMBOL + " = " + DatabaseUtils.sqlEscapeString(coin.getSymbol());
		db.update(TABLE_NAME, values, whereClause, null);
	}
	
	
	public void upsertCoin(ZWCoin coin, SQLiteDatabase db) {
		
		ContentValues cv = getContentValues(coin);
		
		try {
			db.insertOrThrow(TABLE_NAME, null, cv);
		}
		catch(Exception e) {
			//failed to insert, likely because the coin already exists in the database, so just update it
			String where = COLUMN_SYMBOL + " = " + DatabaseUtils.sqlEscapeString(coin.getSymbol());
			db.update(TABLE_NAME, cv, where, null);
		}
		
	}

	
	private ZWCoin cursorToCoin(Cursor cursor) {
		long defaultFee = cursor.getLong(cursor.getColumnIndex(COLUMN_DEFAULT_FEE_PER_KB));
		//BigInteger defaultFeePerKb = BigInteger.valueOf(defaultFee);
		
		String name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME));
		String type = cursor.getString(cursor.getColumnIndex(COLUMN_TYPE));
		String scheme = cursor.getString(cursor.getColumnIndex(COLUMN_SCHEME));
		String logoUrl = cursor.getString(cursor.getColumnIndex(COLUMN_LOGO_URL));
		int scale = cursor.getInt(cursor.getColumnIndex(COLUMN_SCALE));
		
		byte pubKeyPrefix = (byte) cursor.getInt(cursor.getColumnIndex(COLUMN_PUB_KEY_PREFIX));
		byte privKeyPrefix = (byte) cursor.getInt(cursor.getColumnIndex(COLUMN_PRIV_KEY_PREFIX));
		byte scriptHashPrefix = (byte) cursor.getInt(cursor.getColumnIndex(COLUMN_SCRIPT_PREFIX));
		int confirmationsNeeded = cursor.getInt(cursor.getColumnIndex(COLUMN_RECOMMENDED_CONFIRMS));
		int blockGenTime = cursor.getInt(cursor.getColumnIndex(COLUMN_BLOCK_TIME));
		String chain = cursor.getString(cursor.getColumnIndex(COLUMN_CHAIN));
		
		String health = cursor.getString(cursor.getColumnIndex(COLUMN_HEALTH));
		
		int enabledInt = cursor.getInt(cursor.getColumnIndex(COLUMN_ENABLED));
		boolean enabled = enabledInt > 0;
		
		//activationStatus isn't stored as part of a ZWCoin object, so don't load that here
		//maybe it should be?
		//Integer activationStatus = cursor.getInt(cursor.getColumnIndex(COLUMN_ACTIVATED_STATUS));
		
		ZWCoin coin = new ZWCoin(name, type, chain, scheme, scale, String.valueOf(defaultFee), logoUrl, 
				pubKeyPrefix, scriptHashPrefix, privKeyPrefix, confirmationsNeeded, blockGenTime, enabled, health);
		
		return coin;
	}
	
	
	/**
	 * takes the parameters of a coin and returns a {@link ContentValues} object for insertion into a database,
	 * note: only adds "permanent" features of a coin, eg blockheight and activation status are not added
	 * @param coin ZWCoin object to get content values from
	 * @return ContentValues for the given coin
	 */
	private ContentValues getContentValues(ZWCoin coin) {
		ContentValues content = new ContentValues(16);
		
		//these are properties of the coin and extremely unlikely to change
		content.put(COLUMN_DEFAULT_FEE_PER_KB, coin.getDefaultFee().longValue());
		content.put(COLUMN_LOGO_URL, coin.getLogoUrl());
		content.put(COLUMN_NAME, coin.getName());
		content.put(COLUMN_PRIV_KEY_PREFIX, coin.getPrivKeyPrefix());
		content.put(COLUMN_PUB_KEY_PREFIX, coin.getPubKeyHashPrefix());
		content.put(COLUMN_RECOMMENDED_CONFIRMS, coin.getNumRecommendedConfirmations());
		content.put(COLUMN_SCALE, coin.getScale());
		content.put(COLUMN_SCHEME, coin.getScheme());
		content.put(COLUMN_SCRIPT_PREFIX, coin.getScriptHashPrefix());
		content.put(COLUMN_SYMBOL, coin.getSymbol());
		content.put(COLUMN_TYPE, coin.getType());
		content.put(COLUMN_BLOCK_TIME, coin.getBlockTime());
		content.put(COLUMN_CHAIN, coin.getChain());
		content.put(COLUMN_ENABLED, coin.isEnabled());
		content.put(COLUMN_HEALTH, coin.getHealth());
		
		
		return content;
	}



}
