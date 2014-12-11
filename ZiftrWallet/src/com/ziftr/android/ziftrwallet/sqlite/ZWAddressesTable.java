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
		db.insert(getTableName(address.getCoinId()), 
				null, addressToContentValues(address));
	}

	
	@Override
	protected void createTableColumns(ZWCoin coin, SQLiteDatabase database) {
		
		//add label column
		addColumn(coin, COLUMN_LABEL, "TEXT", database);
		
		//add balance column
		addColumn(coin, COLUMN_BALANCE, "INTEGER", database);
		
		//add creation timestamp column
		addColumn(coin, COLUMN_CREATION_TIMESTAMP, "INTEGER", database);
		
		//add modified timestamp column
		addColumn(coin, COLUMN_MODIFIED_TIMESTAMP, "INTEGER", database);
	}

	
	protected ZWAddress getAddress(ZWCoin coinId, String address, SQLiteDatabase db) {
	
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
				ZLog.log("Error loading address from ", coinId.getShortTitle(), 
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
	 * @param coin
	 * @param addresses
	 * @param db
	 * @param receivingNotSending true
	 * @return
	 */
	protected List<ZWAddress> getAddresses(ZWCoin coin, List<String> addresses, SQLiteDatabase db) {

		if(addresses == null || addresses.size() == 0) {
			ZLog.log("Trying to get addresses with specifiying public keys. Returning all addresses");
			return getAllAddresses(coin, db);
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM ");
		sb.append(getTableName(coin));
		
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
	 
		//TODO -i think this is just not true, we also need to stop passing around this boolean which is essentially a type variable
		/*
		if(receivingNotSending) {
			//if addresses array is null and we ask for receiving addresses then assume we want all visible addresses only
			sb.append(" WHERE " + ZWReceivingAddressesTable.COLUMN_HIDDEN + " = " + ZWReceivingAddressesTable.VISIBLE_TO_USER);
		}
		*/

		sb.append(";");
		String toQuery = sb.toString();
		Cursor cursor = db.rawQuery(toQuery, null);
		
		List<ZWAddress> newAddresses = this.readAddresses(coin, cursor);

		return newAddresses;
	}

	
	protected List<ZWAddress> getAllAddresses(ZWCoin coin, SQLiteDatabase db) {
		String sql = "SELECT * FROM " + getTableName(coin);
		Cursor cursor = db.rawQuery(sql, null);
		return this.readAddresses(coin, cursor);
	}

	
	/**
	 * returns a list of Strings of all addresses for the given coin
	 * @param coin
	 * @param db
	 * @return
	 */
	protected List<String> getAddressesList(ZWCoin coin, SQLiteDatabase db) {
		
		String sql = "SELECT " + COLUMN_ADDRESS + " FROM " + getTableName(coin);
		Cursor cursor = db.rawQuery(sql, null);
		
		return this.readAddressList(coin, cursor);
	}
	
	protected void updateAddress(ZWAddress address, SQLiteDatabase db) {
		ContentValues values = addressToContentValues(address);
		String whereClause = COLUMN_ADDRESS + " = " + DatabaseUtils.sqlEscapeString(address.getAddress());
		db.update(getTableName(address.getCoinId()), values, whereClause, null);
	}

	protected void updateAddressLabel(ZWCoin coin, String address, String newLabel, SQLiteDatabase db) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_LABEL, newLabel);
		String whereClause = COLUMN_ADDRESS + " = " + DatabaseUtils.sqlEscapeString(address);
		db.update(getTableName(coin), values, whereClause, null);
	}

	/**
	 * reads through the cursor and creates {@link ZWAddress} from contents,
	 * closes cursor when done
	 * @param coin which coin the cursor is for
	 * @param cursor a cursor with data that can be converted to addresses
	 * @return a {@link List} of {@link ZWAddress} objects
	 */
	protected List<ZWAddress> readAddresses(ZWCoin coin, Cursor cursor) {
		ArrayList<ZWAddress> addresses = new ArrayList<ZWAddress>();
		
		if(cursor != null && cursor.moveToFirst()) {
			do {
				try {
					ZWAddress address = this.cursorToAddress(coin, cursor);
					addresses.add(address);
				}
				catch (ZWAddressFormatException e) {
					ZLog.log("Exception trying to Address from cursor: ", e);
				}
			}
			while(cursor.moveToNext());
		}
		
		cursor.close();
		
		return addresses;
	}
	
	
	/**
	 * reads through the cursor and pulls out public addresses, assumes address is string in first column of cursor,
	 * closes cursor when done
	 * @param coin which coin this cursor is for
	 * @param cursor a cursor with address strings in the first column
	 * @return a list of public address strings from the cursor
	 */
	protected List<String> readAddressList(ZWCoin coin, Cursor cursor) {
		ArrayList<String> addresses = new ArrayList<String>();
		
		if(cursor != null && cursor.moveToFirst()) {
			do {
				addresses.add(cursor.getString(0));
			}
			while (cursor.moveToNext());
		}
		cursor.close();
		
		return addresses;
	}

	
}