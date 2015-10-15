/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.sqlite;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWHDReceivingAddress;
import com.ziftr.android.ziftrwallet.crypto.ZWPrivateData;
import com.ziftr.android.ziftrwallet.crypto.ZWPublicKey;
import com.ziftr.android.ziftrwallet.crypto.ZWRNGReceivingAddress;
import com.ziftr.android.ziftrwallet.crypto.ZWReceivingAddress;
import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.exceptions.ZWDataEncryptionException;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWReceivingAddressesTable extends ZWAddressesTable<ZWReceivingAddress> {

	/** The postfix that assists in making the names for the users addresses table. */
	private static final String TABLE_NAME_BASE = "_receiving_addresses";

	/** The private key encoded as a string using WIF. */
	public static final String COLUMN_PRIV_KEY = "priv_key";

	/** The public key, encoded as a string. */
	public static final String COLUMN_PUB_KEY = "pub_key";

	/** whether a receiving address is hidden from the user or not (for change addresses) */
	public static final String COLUMN_HIDDEN = "hidden";

	/** whether an address has spent money or not */
	public static final String COLUMN_SPENT_FROM = "spent_From";

	/** Each key generated has an index relative to its parent. */
	public static final String COLUMN_HD_INDEX = "hd_counter";
	public static final String COLUMN_HD_ACCOUNT = "account";

	public static int VISIBLE_TO_USER = 0;
	public static int HIDDEN_FROM_USER = 1;

	public static int UNSPENT_FROM = 0;
	public static int SPENT_FROM = 1;

	public static enum EncryptionStatus{
		SUCCESS,
		ALREADY_ENCRYPTED, //first address was already encrypted
		ERROR //non-first address was already encrypted
	};

	@Override
	protected String getTableName(ZWCoin coin) {
		return coin.getSymbol() + TABLE_NAME_BASE;
	}


	@Override
	protected void createBaseTable(ZWCoin coin, SQLiteDatabase database) {

		String createSql = "CREATE TABLE IF NOT EXISTS " + getTableName(coin) + 
				" (" + COLUMN_PRIV_KEY + " TEXT UNQIUE NOT NULL, " + 
				COLUMN_PUB_KEY + " TEXT UNIQUE NOT NULL, " + 
				COLUMN_ADDRESS + " TEXT UNIQUE NOT NULL)";

		database.execSQL(createSql);
	}

	@Override
	protected void createTableColumns(ZWCoin coin, SQLiteDatabase database) {

		//create columns that all address tables use
		super.createTableColumns(coin, database);

		addColumn(coin, COLUMN_SPENT_FROM, "INTEGER", database);
		addColumn(coin, COLUMN_HIDDEN, "INTEGER", database);
		addColumn(coin, COLUMN_HD_INDEX, "INTEGER", database);
		addColumn(coin, COLUMN_HD_ACCOUNT, "INTEGER", database);

		// "A unique constraint is satisfied if and only if no two rows in a table 
		// have the same values and have non-null values in the unique columns."
		// Hence, we are okay to add this here. The standard private keys that were
		// derived without HD wallets will have null in both the COLUMN_HD_INDEX and
		// the COLUMN_HD_ACCOUNT column, but non-null in COLUMN_HIDDEN.
		try {
			String rawQuery = "CREATE UNIQUE INDEX unique_account_change_counter on ";
			rawQuery += this.getTableName(coin);
			rawQuery += "(";
			rawQuery += COLUMN_HD_ACCOUNT + ", ";
			rawQuery += COLUMN_HIDDEN + ", ";
			rawQuery += COLUMN_HD_INDEX;
			rawQuery += ");";
			database.rawQuery(rawQuery, null);
		} catch(Exception e) {
			// Okay if this throws, just means the unique index already exists
		}
	}

	@Override
	protected ZWReceivingAddress cursorToAddress(ZWCoin coin, Cursor c) throws ZWAddressFormatException {
		String dataInPrivColumn = c.getString(c.getColumnIndex(COLUMN_PRIV_KEY));

		// If a constructor is used that doesn't pass the pubKeyBytes it recalculates them
		// here and that is pretty slow. Hence, whenever loading in addresses from the db, 
		// use a constructor where you can provide the pubKeyBytes.
		byte[] pubKeyBytes = ZiftrUtils.hexStringToBytes(c.getString(c.getColumnIndex(COLUMN_PUB_KEY)));
		ZWPublicKey pubkey = new ZWPublicKey(pubKeyBytes);

		// Hidden column is used even in non-HD address rows
		boolean hidden = getBooleanValue(c, COLUMN_HIDDEN);
		
		
		ZWReceivingAddress returnAddress = null; // = new ZWReceivingAddress(coin, pubkey, privateData, hidden, hdAccount, hdIndex);
		
		if (dataInPrivColumn == null || dataInPrivColumn.isEmpty()) {
			//this address is derived from an HD seed and the private key can be calculated later
			Integer hdAccount = getIntegerValue(c, COLUMN_HD_ACCOUNT);
			Integer hdIndex = getIntegerValue(c, COLUMN_HD_INDEX);
			
			returnAddress = new ZWHDReceivingAddress(coin, pubkey, hidden, hdAccount, hdIndex);
		}
		else {
			//otherwise it's an address where the private key is known and stored (a legacy address)
			ZWPrivateData privateData = ZWPrivateData.createFromPrivateDataString(dataInPrivColumn);
			returnAddress = new ZWRNGReceivingAddress(coin, pubkey, privateData, hidden);
		}
		
		

		returnAddress.setCreationTimeSeconds(c.getLong(c.getColumnIndex(COLUMN_CREATION_TIMESTAMP)));
		returnAddress.setSpentFrom(c.getInt(c.getColumnIndex(COLUMN_SPENT_FROM)) != 0);

		// All addresses ahve these columns
		returnAddress.setLabel(c.getString(c.getColumnIndex(COLUMN_LABEL)));
		returnAddress.setLastKnownBalance(c.getInt(c.getColumnIndex(COLUMN_BALANCE)));
		returnAddress.setLastTimeModifiedSeconds(c.getLong(c.getColumnIndex(COLUMN_MODIFIED_TIMESTAMP)));

		return returnAddress;
	}
	

	//called in wrapper with transaction begin and transaction end to ensure atomicity
	protected boolean recryptAllAddresses(ZWCoin coin, SecretKey oldKey, SecretKey newKey, SQLiteDatabase db) throws ZWDataEncryptionException {

		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ");
		sb.append(COLUMN_PRIV_KEY);
		sb.append(" FROM ");
		sb.append(getTableName(coin));
		sb.append(";");
		Cursor c = db.rawQuery(sb.toString(), null);

		List<String> oldPrivKeysInDb = new ArrayList<String>();
		//read private keys
		if (c.moveToFirst()) {
			do {
				String val = c.getString(c.getColumnIndex(COLUMN_PRIV_KEY));
				if (val != null && !val.isEmpty()) {
					oldPrivKeysInDb.add(val);
				}
			} while (c.moveToNext());
		}
		c.close();

		// Decrypt private keys
		for (int i = 0; i < oldPrivKeysInDb.size(); i++){
			String oldPrivKeyValInDb = oldPrivKeysInDb.get(i);
			
			ZWPrivateData keyData = ZWPrivateData.createFromPrivateDataString(oldPrivKeyValInDb);
			
			keyData.decrypt(oldKey);
			keyData.encrypt(newKey);

			ContentValues cv = new ContentValues();
			cv.put(COLUMN_PRIV_KEY, keyData.toString());
			String whereClause = COLUMN_PRIV_KEY + " = " + DatabaseUtils.sqlEscapeString(oldPrivKeyValInDb);

			int numUpdated = db.update(getTableName(coin), cv, whereClause, null);

			if (numUpdated != 1) {
				return false;
			}
		}
		
		return true;
	}


	protected List<String> getHiddenAddresses(ZWCoin coin, SQLiteDatabase db, boolean includeSpentFrom){

		String sql = "SELECT " + COLUMN_ADDRESS + " FROM " + getTableName(coin) + " WHERE " + COLUMN_HIDDEN + " = " + HIDDEN_FROM_USER;
		if(!includeSpentFrom) {
			sql += " AND " + COLUMN_SPENT_FROM + " = " + UNSPENT_FROM;
		}

		Cursor cursor = db.rawQuery(sql, null);
		return this.readAddressList(coin, cursor);
	}

	protected List<ZWReceivingAddress> getAllVisibleAddresses(ZWCoin coin, SQLiteDatabase db) {
		ZWAddressFilter<ZWReceivingAddress> filter = new ZWAddressFilter<ZWReceivingAddress>() {
			@Override public boolean include(ZWReceivingAddress address) { return true; }
			@Override public String where() { return COLUMN_HIDDEN + " = " + VISIBLE_TO_USER; }
		};
		return this.getAddresses(coin, filter, db);
	}

	
	@Override
	protected ContentValues addressToContentValues(ZWReceivingAddress address, boolean forInsert) {
		ContentValues values = super.addressToContentValues(address, forInsert);

		String privateKey = null;
		Integer hdIndex = null;
		Integer hdAccount = null;
		
		if(address instanceof ZWHDReceivingAddress) {
			hdIndex = ((ZWHDReceivingAddress) address).getHdIndex();
			hdAccount = ((ZWHDReceivingAddress) address).getHdAccount();
			privateKey = "";
			
		}
		else {
			ZWPrivateData addressPrivateData = address.getPrivateKeyData();
			if(addressPrivateData != null) {
				privateKey = addressPrivateData.getStorageString();
			}
		}

		values.put(COLUMN_CREATION_TIMESTAMP, address.getCreationTimeSeconds());
		values.put(COLUMN_PUB_KEY, address.getPublicKey().getPubHex());
		values.put(COLUMN_SPENT_FROM, address.isSpentFrom());
		
		values.put(COLUMN_HIDDEN, address.isHidden() ? 1 : 0);
		
		if (forInsert) {
			// Do not update these, they cannot be changed after insertion
			values.put(COLUMN_HD_INDEX, hdIndex);
			values.put(COLUMN_HD_ACCOUNT, hdAccount);
			
			//we may someday want to be able to update this (for example for cacheing hd wallet private keys)
			//but if we add that, we need to add code to check to make sure we never accidently wipe out private keys
			values.put(COLUMN_PRIV_KEY, privateKey);
		}

		return values;
	}
	
	

	protected int nextUnusedIndex(ZWCoin coin, boolean change, int account, SQLiteDatabase db) {
		String[] selectionArgs = new String[] {
				String.valueOf(change ? HIDDEN_FROM_USER : VISIBLE_TO_USER),
				String.valueOf(account)
		};
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT MAX(").append(COLUMN_HD_INDEX).append(") FROM ").append(getTableName(coin));
		sb.append(" WHERE ").append(COLUMN_HIDDEN).append(" = ?");
		sb.append(" AND ").append(COLUMN_HD_ACCOUNT).append(" = ?");
		Cursor c = db.rawQuery(sb.toString(), selectionArgs);
		return !c.moveToFirst() || c.getCount() == 0 ? 0 : c.getInt(0) + 1;
	}

}