package com.ziftr.android.ziftrwallet.sqlite;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.ziftrwallet.crypto.ZWAddress;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.util.ZLog;

// TODO add a column for isChangeAddress. Many times we want to filter change addresses
// out so they are never revealed to the user
public abstract class ZWAddressesTable extends ZWCoinRelativeTable {

	/** 
	 * The address column. This is the encoded public key, along with coin type
	 * byte and double hash checksum.
	 */
	public static final String COLUMN_ADDRESS = "address";
	
	/** The label column. This is for users to keep a string attached to an entry. */
	public static final String COLUMN_LABEL = "label";

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
	 * @throws ZWAddressFormatException
	 */
	protected abstract ZWAddress cursorToAddress(ZWCoin coinId, Cursor c) throws ZWAddressFormatException;

	/**
	 * For updating and insertion with android SQLite helper methods, we need to make a 
	 * {@link ContentValues} object. This also has to be done differently for the table of 
	 * receiving addresses and the table of sending addresses.
	 * 
	 * @param address - The address to convert.
	 * @return
	 */
	protected abstract ContentValues addressToContentValues(ZWAddress address);

	protected void insert(ZWAddress address, SQLiteDatabase db) {
		long insertId = db.insert(getTableName(address.getCoinId()), 
				null, addressToContentValues(address));
		address.setId(insertId);
	}


	
	protected ZWAddress readAddress(ZWCoin coinId, String address, SQLiteDatabase db, boolean receivingNotSending) {
	
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM ");
		sb.append(getTableName(coinId));
		sb.append(" WHERE ").append(COLUMN_ADDRESS).append(" = ");
		sb.append(DatabaseUtils.sqlEscapeString(address));
		
		String toQuery = sb.toString();
		Cursor c = db.rawQuery(toQuery, null);

		ZWAddress foundAddress = null;
		
		if (c.moveToFirst()) {
			// Recreate key from stored private key
			try {
				foundAddress = cursorToAddress(coinId, c);
				return foundAddress;

			}
			catch (ZWAddressFormatException afe) {
				ZLog.log("Error loading address from ", coinId.toString(), 
						" receiving addresses database.");
				ZLog.log(afe);
			}
		}
		
		// Make sure we close the cursor
		c.close();

		return null;
		
	}
	

	/**
	 * Gets a list of addresses from the database. If addresses is null 
	 * then it gets all addresses from the database. 
	 * 
	 * @param coinId
	 * @param addresses
	 * @param db
	 * @param receivingNotSending true
	 * @return
	 */
	protected List<ZWAddress> readAddresses(ZWCoin coinId, List<String> addresses, SQLiteDatabase db, boolean receivingNotSending) {

		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM ");
		sb.append(getTableName(coinId));
		
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
		else if (receivingNotSending) {
			//if addresses array is null and we ask for receiving addresses then assume we want all visible addresses only
			sb.append(" WHERE " + ZWReceivingAddressesTable.COLUMN_HIDDEN + " = " + ZWReceivingAddressesTable.VISIBLE_TO_USER);
		}

		sb.append(";");
		String toQuery = sb.toString();
		Cursor c = db.rawQuery(toQuery, null);
		
		List<ZWAddress> newAddresses = new ArrayList<ZWAddress>();

		// Move to first returns false if cursor is empty
		if (c.moveToFirst()) {
			// Recreate key from stored private key
			try {
				do {
					// TODO deal with encryption of private key
					newAddresses.add(cursorToAddress(coinId, c));
				} while (c.moveToNext());
			} catch (ZWAddressFormatException afe) {
				ZLog.log("Error loading address from ", coinId.toString(), 
						" receiving addresses database.");
				afe.printStackTrace();
			}
		}
		
		// Make sure we close the cursor
		c.close();

		return newAddresses;
	}

	protected List<ZWAddress> readAllVisibleAddresses(ZWCoin coinId, SQLiteDatabase db, boolean receivingNotSending) {
		return this.readAddresses(coinId, null, db, receivingNotSending);
	}

	protected void updateAddress(ZWAddress address, SQLiteDatabase db) {
		ContentValues values = addressToContentValues(address);
		String whereClause = COLUMN_ID + " = " + DatabaseUtils.sqlEscapeString(address.getAddress());
		
		db.update(getTableName(address.getCoinId()), values, whereClause, null);
	}

	protected void updateAddressLabel(ZWCoin coin, String address, String newLabel, SQLiteDatabase db) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_LABEL, newLabel);
		String whereClause = COLUMN_ADDRESS + " = " + DatabaseUtils.sqlEscapeString(address);
		db.update(getTableName(coin), values, whereClause, null);
	}
	
	/**
	 * 
	 * @param coin
	 * @param db
	 * @return
	 */
	protected ArrayList<String> getAddressesList(ZWCoin coin, SQLiteDatabase db) {
		
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