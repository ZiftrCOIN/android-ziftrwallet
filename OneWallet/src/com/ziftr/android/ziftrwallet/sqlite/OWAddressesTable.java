package com.ziftr.android.ziftrwallet.sqlite;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.ziftrwallet.crypto.OWAddress;
import com.ziftr.android.ziftrwallet.exceptions.OWAddressFormatException;
import com.ziftr.android.ziftrwallet.util.OWCoin;
import com.ziftr.android.ziftrwallet.util.ZLog;

// TODO add a column for isChangeAddress. Many times we want to filter change addresses
// out so they are never revealed to the user
public abstract class OWAddressesTable extends OWCoinRelativeTable {

	/** 
	 * The address column. This is the encoded public key, along with coin type
	 * byte and double hash checksum.
	 */
	public static final String COLUMN_ADDRESS = "address";

	/** 
	 * The balance column. Note that the table may not always be up to date 
	 * and this may just be the last known balance. 
	 */
	public static final String COLUMN_BALANCE = "balance";

	/** 
	 * This is the last time that address was used in a transaction. Note that this
	 * table may not always be up to date and this may just be the last known time
	 * that the address was used. 
	 */
	public static final String COLUMN_MODIFIED_TIMESTAMP = "modified_timestamp";

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
	protected abstract OWAddress cursorToAddress(OWCoin coinId, Cursor c) throws OWAddressFormatException;

	/**
	 * For updating and insertion with android SQLite helper methods, we need to make a 
	 * {@link ContentValues} object. This also has to be done differently for the table of 
	 * receiving addresses and the table of sending addresses.
	 * 
	 * @param address - The address to convert.
	 * @return
	 */
	protected abstract ContentValues addressToContentValues(OWAddress address);

	protected void insert(OWAddress address, SQLiteDatabase db) {
		long insertId = db.insert(getTableName(address.getCoinId()), 
				null, addressToContentValues(address));
		address.setId(insertId);
	}

	protected OWAddress readAddress(OWCoin coinId, String address, SQLiteDatabase db) {
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
	 * Gets a list of addresses from the database. If addresses is null 
	 * then it gets all addresses from the database. 
	 * 
	 * @param coinId
	 * @param addresses
	 * @param db
	 * @return
	 */
	protected List<OWAddress> readAddresses(OWCoin coinId, List<String> addresses, SQLiteDatabase db) {
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
				sb.append(DatabaseUtils.sqlEscapeString(addresses.get(i)));
				if (i != (addresses.size() - 1)) {
					sb.append(",");
				} else {
					sb.append(")");
				}
			}
		}

		sb.append(";");
		String toQuery = sb.toString();
		Cursor c = db.rawQuery(toQuery, null);
		
		List<OWAddress> newAddresses = new ArrayList<OWAddress>();

		// Move to first returns false if cursor is empty
		if (c.moveToFirst()) {
			// Recreate key from stored private key
			try {
				do {
					// TODO deal with encryption of private key
					newAddresses.add(cursorToAddress(coinId, c));
				} while (c.moveToNext());
			} catch (OWAddressFormatException afe) {
				ZLog.log("Error loading address from ", coinId.toString(), 
						" receiving addresses database.");
				afe.printStackTrace();
			}
		}
		
		// Make sure we close the cursor
		c.close();

		return newAddresses;
	}

	protected List<OWAddress> readAllAddresses(OWCoin coinId, SQLiteDatabase db) {
		return this.readAddresses(coinId, null, db);
	}

	protected void updateAddress(OWAddress address, SQLiteDatabase db) {
		ContentValues values = addressToContentValues(address);
		String whereClause = COLUMN_ID + " = " + DatabaseUtils.sqlEscapeString(address.getAddress());
		
		db.update(getTableName(address.getCoinId()), values, whereClause, null);
	}

	protected void updateAddressLabel(OWCoin coin, String address, String newLabel, SQLiteDatabase db) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_NOTE, newLabel);
		String whereClause = COLUMN_ADDRESS + " = " + DatabaseUtils.sqlEscapeString(address);
		db.update(getTableName(coin), values, whereClause, null);
	}
	
	
	protected ArrayList<String> getAddressesList(OWCoin coin, SQLiteDatabase db) {
		
		ArrayList<String> addresses = new ArrayList<String>();
		
		String sql = "SELECT " + COLUMN_ADDRESS + " FROM " + getTableName(coin);
		
		Cursor cursor = db.rawQuery(sql, null);
		if(cursor.moveToFirst()) {
			do {
				addresses.add(cursor.getString(0));
			}
			while (cursor.moveToNext());
		}
		
		return addresses;
	}
	
	

}