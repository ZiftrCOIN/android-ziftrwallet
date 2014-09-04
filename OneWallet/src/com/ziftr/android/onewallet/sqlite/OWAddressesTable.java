package com.ziftr.android.onewallet.sqlite;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.onewallet.crypto.OWAddress;
import com.ziftr.android.onewallet.exceptions.OWAddressFormatException;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.ZLog;

public abstract class OWAddressesTable {

	/** The id column. All Tables must have this to work well with adapters. */
	public static final String COLUMN_ID = "_id";

	/** 
	 * The address column. This is the encoded public key, along with coin type
	 * byte and double hash checksum.
	 */
	public static final String COLUMN_ADDRESS = "address";

	/** The note column. This is for users to keep a string attached to an address. */
	public static final String COLUMN_NOTE = "note";

	/** 
	 * The balance column. Note that the table may not always be up to date 
	 * and this may just be the last known balance. 
	 */
	public static final String COLUMN_BALANCE = "balance";

	///** The first time that this address was seen by this phone. */
	//public static final String COLUMN_CREATION_TIMESTAMP = "creation_timestamp";

	/** 
	 * This is the last time that address was used in a transaction. Note that this
	 * table may not always be up to date and this may just be the last known time
	 * that the address was used. 
	 */
	public static final String COLUMN_MODIFIED_TIMESTAMP = "modified_timestamp";
	
	/** 
	 * The postfix that assists in making the names for the external addresses table. 
	 */
	protected abstract String getTablePostfix();
	
	/**
	 * The string that will be executed when creating a table.
	 * 
	 * @param coinId - The table specifier.
	 * @return
	 */
	protected abstract String getCreateTableString(OWCoin.Type coinId);
	
	/**
	 * A useful method to convert a cursor (pointer to a row in a table) to an address.
	 * This has to be done differently for the table of receiving addresses and the table 
	 * of sending addresses. 
	 * 
	 * @param coinId - the coin type table specifier
	 * @param c - the cursor
	 * @return
	 * @throws OWAddressFormatException
	 */
	protected abstract OWAddress cursorToAddress(OWCoin.Type coinId, Cursor c) throws OWAddressFormatException;
	
	/**
	 * For updating and insertion with android SQLite helper methods, we need to make a 
	 * {@link ContentValues} object. This also has to be done differently for the table of 
	 * receiving addresses and the table of sending addresses, and whether or not the content
	 * values is to be used in inserting or in deleting.
	 * 
	 * @param address - The address to convert.
	 * @param forInsert - either for insert/update
	 * @return
	 */
	protected abstract ContentValues addressToContentValues(OWAddress address, boolean forInsert);
	
	protected String getTableName(OWCoin.Type coinId) {
		return coinId.toString() + getTablePostfix();
	}

	protected void create(OWCoin.Type coinId, SQLiteDatabase db) {
		db.execSQL(getCreateTableString(coinId));
	}
	
	protected void insert(OWAddress address, SQLiteDatabase db) {
		long insertId = db.insert(getTableName(address.getCoinId()), 
				null, addressToContentValues(address, true));
		ZLog.log("inserted!!!!");
		address.setId(insertId);
	}
	
	protected OWAddress readAddress(OWCoin.Type coinId, String address, SQLiteDatabase db) {
		if (address == null) {
			return null;
		}

		List<String> addresses = new ArrayList<String>();
		addresses.add(address);
		
		List<OWAddress> readAddresses = readAddresses(coinId, addresses, db);
		if (readAddresses.size() == 0) {
			return null;
		} else {
			return readAddresses.get(0);
		}
	}
	
	/**
	 * Gets a list of addresses from the database.
	 * If addresses is null then
	 * 
	 * @param coinId
	 * @param addresses
	 * @param db
	 * @return
	 */
	protected List<OWAddress> readAddresses(OWCoin.Type coinId, List<String> addresses, SQLiteDatabase db) {
		if (addresses != null && addresses.size() == 0) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM ");
		sb.append(getTableName(coinId));
		
		// If null, we take that to mean that they want all addresses
		if (addresses != null) {
			sb.append(" WHERE ");
			sb.append(COLUMN_ADDRESS);
			sb.append(" IN (");
			for (int i = 0; i < addresses.size(); i++) {
				sb.append("'");
				sb.append(addresses.get(i));
				sb.append("'");
				if (i != (addresses.size() - 1)) {
					sb.append(",");
				} else {
					sb.append(")");
				}
			}
		}
		
		sb.append(";");
		String toQuery = sb.toString();
		ZLog.log("Query: " + toQuery);
		Cursor c = db.rawQuery(toQuery, null);

		List<OWAddress> newAddress = new ArrayList<OWAddress>();

		// Move to first returns false if cursor is empty
		if (c.moveToFirst()) {
			// Recreate key from stored private key
			try {
				do {
					// TODO deal with encryption of private key
					newAddress.add(cursorToAddress(coinId, c));
				} while (c.moveToNext());
			} catch (OWAddressFormatException afe) {
				ZLog.log("Error loading address from ", coinId.toString(), 
						" receiving addresses database.");
				afe.printStackTrace();
			}
		}

		// Make sure we close the cursor
		c.close();

		return newAddress;
	}
	
	protected List<OWAddress> readAllAddresses(OWCoin.Type coinId, SQLiteDatabase db) {
		return this.readAddresses(coinId, null, db);
	}
	
	protected int numAddresses(OWCoin.Type coinId, SQLiteDatabase db) {
		String countQuery = "SELECT  * FROM " + getTableName(coinId);
		Cursor cursor = db.rawQuery(countQuery, null);
		int count = cursor.getCount();
		cursor.close();
		return count;
	}

	protected void updateAddress(OWAddress address, SQLiteDatabase db) {
		if (address.getId() == -1) {
			// Shouldn't happen
			throw new RuntimeException("Error: id has not been set.");
		}
		ContentValues values = addressToContentValues(address, false);
		db.update(getTableName(address.getCoinId()), values, COLUMN_ID + " = " + address.getId(), null);
	}

}