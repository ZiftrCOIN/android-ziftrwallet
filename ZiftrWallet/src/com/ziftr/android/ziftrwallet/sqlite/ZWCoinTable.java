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
	public static final String COLUMN_CHAIN = "chain";
	public static final String COLUMN_TYPE = "type";
	public static final String COLUMN_SCALE = "scale";
	public static final String COLUMN_SCHEME = "scheme";
	public static final String COLUMN_LOGO_URL = "logo_url";
	
	
	public static final int UNACTIVATED = 1; //Used for table types that just haven't been used yet.
	public static final int ACTIVATED = 2; //Used for table types that are activated and in use by user.
	public static final int DEACTIVATED = 4; //Used for table types that used to be ACTIVATED, but now user deactivated them.
	

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
		this.addColumn(COLUMN_ENABLED, "INTEGER", db);
		this.addColumn(COLUMN_CHAIN, "TEXT", db);
		this.addColumn(COLUMN_TYPE, "TEXT", db);
		this.addColumn(COLUMN_LOGO_URL, "TEXT", db);

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
		
	
	
	protected void insertDefault(ZWCoin defaultCoin, SQLiteDatabase db) {
	
		ContentValues baseCoinValues = getContentValues(defaultCoin);
		db.insert(TABLE_NAME, null, baseCoinValues); //this may fail, that's ok
		
		ContentValues defaults = new ContentValues(2);
		defaults.put(COLUMN_BLOCK_HEIGHT, 0);
		defaults.put(COLUMN_ACTIVATED_STATUS, UNACTIVATED);
		String where = COLUMN_SYMBOL + " = " + DatabaseUtils.sqlEscapeString(defaultCoin.getSymbol());
		
		db.update(TABLE_NAME, defaults, where, null);
	}
	
	
	/**
	 * query the activation database for a list of coins with specific status,
	 * will use bitwise operators allowing querying for multiple status,
	 * eg ACTIVATED | DEACTIVATED
	 * @param activationStatus the activation status (or bitwise of multiple status) to query for
	 * @param db the db to query
	 * @return list of {@link ZWCoin} with the matching status(es)
	 */
	protected List<ZWCoin> getCoinsByStatus(int activationStatus, SQLiteDatabase db, boolean includeTestnet) {
		ArrayList<ZWCoin> coins = new ArrayList<ZWCoin>();
		
		String sql = "SELECT * FROM " + TABLE_NAME + 
				" WHERE (" + COLUMN_ACTIVATED_STATUS + " & " + String.valueOf(activationStatus) +
				") != 0";
		if(!includeTestnet) {
			sql += " AND " + COLUMN_CHAIN + " = 'main'";
		}
		
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
		
		
		return coins;
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

	protected void setActivation(ZWCoin coin, int activationStatus, SQLiteDatabase db) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_ACTIVATED_STATUS, activationStatus);
		String whereClause = COLUMN_SYMBOL + " = " + DatabaseUtils.sqlEscapeString(coin.getSymbol());
		db.update(TABLE_NAME, values, whereClause, null);
	}
	
	
	public void updateCoin(ZWCoin coin, SQLiteDatabase db) {
		
		ContentValues cv = getContentValues(coin);
		
		long insertRow = db.insert(TABLE_NAME, null, cv);
		if(insertRow < 0) {
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
		
		int enabledInt = cursor.getInt(cursor.getColumnIndex(COLUMN_ENABLED));
		boolean enabled = enabledInt > 0;
		
		ZWCoin coin = new ZWCoin(name, type, chain, scheme, scale, String.valueOf(defaultFee), logoUrl, 
				pubKeyPrefix, scriptHashPrefix, privKeyPrefix, confirmationsNeeded, blockGenTime, enabled);
		
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
		
		
		return content;
	}



}
