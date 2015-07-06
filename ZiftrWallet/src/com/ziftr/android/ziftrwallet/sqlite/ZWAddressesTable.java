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

import com.google.common.base.Joiner;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWSendingAddress;
import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.util.ZLog;

/**
 * TODO add a column for isChangeAddress. Many times we want to filter change addresses
 * out so they are never revealed to the user
 */
public abstract class ZWAddressesTable<A extends ZWSendingAddress> extends ZWCoinSpecificTable {

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
	 */
	protected abstract A cursorToAddress(ZWCoin coinId, Cursor c) throws ZWAddressFormatException;

	/** 
	 * Do the reverse of cursorToAddress.  
	 */
	protected ContentValues addressToContentValues(A address, boolean forInsert) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_ADDRESS, address.getAddress());
		values.put(COLUMN_LABEL, address.getLabel());
		values.put(COLUMN_BALANCE, address.getLastKnownBalance());
		values.put(COLUMN_MODIFIED_TIMESTAMP, address.getLastTimeModifiedSeconds());
		return values;
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

	protected A getAddress(ZWCoin coinId, String address, SQLiteDatabase db) {

		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM ");
		sb.append(getTableName(coinId));
		sb.append(" WHERE ").append(COLUMN_ADDRESS).append(" = ");
		sb.append(DatabaseUtils.sqlEscapeString(address));

		String toQuery = sb.toString();
		Cursor c = db.rawQuery(toQuery, null);

		A foundAddress = null;

		if (c.moveToFirst()) {
			// Recreate key from stored private key
			try {
				foundAddress = cursorToAddress(coinId, c);
			}
			catch (ZWAddressFormatException afe) {
				ZLog.log("Error loading address from ", coinId.getSymbol(), " receiving addresses database.");
				ZLog.log(afe);
			}
		}

		// Make sure we close the cursor
		c.close();

		return foundAddress;
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
	protected List<A> getAddresses(ZWCoin coin, ZWAddressFilter<A> filter, SQLiteDatabase db) {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM ");
		sb.append(getTableName(coin));
		if (filter != null) {
			sb.append(" WHERE ");
			sb.append(filter.where());
		}
		sb.append(";");
		String toQuery = sb.toString();
		Cursor cursor = db.rawQuery(toQuery, null);

		List<A> newAddresses = this.readAddresses(coin, cursor, filter);

		return newAddresses;
	}
	
	protected List<A> getAddresses(ZWCoin coin, List<A> addresses, SQLiteDatabase db) {
		
		if (addresses == null)
			return null;
		
		if (addresses.size() == 0) {
			return new ArrayList<A>();
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(COLUMN_ADDRESS);
		sb.append(" IN (");
		sb.append(Joiner.on(",").join(addresses));
		sb.append(")");
		final String where = sb.toString();
		
		ZWAddressFilter<A> filter = new ZWAddressFilter<A>() {
			@Override public boolean include(A address) { return true; }
			@Override public String where() { return where; }
		};
		
		return this.getAddresses(coin, filter, db);
	}

	protected List<A> getAllAddresses(ZWCoin coin, SQLiteDatabase db) {
		return this.getAddresses(coin, (ZWAddressFilter<A>)null, db);
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

	protected void updateAddress(A address, SQLiteDatabase db) {
		ContentValues values = addressToContentValues(address, false);
		String whereClause = COLUMN_ADDRESS + " = " + DatabaseUtils.sqlEscapeString(address.getAddress());
		db.update(getTableName(address.getCoin()), values, whereClause, null);
	}

	protected void updateAddressLabel(ZWCoin coin, String address, String newLabel, SQLiteDatabase db) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_LABEL, newLabel);
		String whereClause = COLUMN_ADDRESS + " = " + DatabaseUtils.sqlEscapeString(address);
		db.update(getTableName(coin), values, whereClause, null);
	}

	protected long insert(A address, SQLiteDatabase db) {
		ContentValues values = this.addressToContentValues(address, true);

		// The id will be generated upon insertion.
		return db.insert(getTableName(address.getCoin()), null, values);
	}

	/**
	 * reads through the cursor and creates {@link ZWSendingAddress} from contents,
	 * closes cursor when done
	 * @param coin which coin the cursor is for
	 * @param cursor a cursor with data that can be converted to addresses
	 * @return a {@link List} of {@link ZWSendingAddress} objects
	 */
	protected List<A> readAddresses(ZWCoin coin, Cursor cursor, ZWAddressFilter<A> filter) {
		ArrayList<A> addresses = new ArrayList<A>();

		if(cursor != null && cursor.moveToFirst()) {
			do {
				try {
					A address = this.cursorToAddress(coin, cursor);
					if (filter == null || filter.include(address)) {
						addresses.add(address);
					}
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
	
	protected List<A> readAddresses(ZWCoin coin, Cursor cursor) {
		return this.readAddresses(coin, cursor, null);
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