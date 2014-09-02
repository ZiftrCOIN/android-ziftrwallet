package com.ziftr.android.onewallet.sqlite;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.onewallet.crypto.Address;
import com.ziftr.android.onewallet.crypto.AddressFormatException;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWCoinRelative;
import com.ziftr.android.onewallet.util.ZLog;


/**
 * Make one table accessing class per coin type. Could those all subclass 
 * 
 * TODO should have a method that inserts a new key and returns the key.
 * Get rid of duplicate strings in strings.xml.
 * 
 * TODO lots of work in here
 */
public abstract class OWSendingAddressesTable implements OWCoinRelative {

	/** The postfix that assists in making the names for the external addresses table. */
	private static final String TABLE_POSTFIX = "_sending_addresses";

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

	/** The most recent time that this address was seen by this phone. */
	public static final String COLUMN_MODIFIED_TIMESTAMP = "modified_timestamp";

	protected static void create(OWCoin.Type coinId, SQLiteDatabase db) {
		db.execSQL(getCreateTableString(coinId));
	}

	protected static String getCreateTableString(OWCoin.Type coinId) {
		return "CREATE TABLE IF NOT EXISTS " + getTableName(coinId) + " (" + 
				COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				COLUMN_ADDRESS + " TEXT NOT NULL, " + 
				COLUMN_NOTE + " TEXT, " + 
				COLUMN_BALANCE + " INTEGER, " +
				//COLUMN_CREATION_TIMESTAMP + " INTEGER, " +
				COLUMN_MODIFIED_TIMESTAMP + " INTEGER );";
	}

	protected static String getTableName(OWCoin.Type coinId) {
		return coinId.toString() + TABLE_POSTFIX;
	}

	protected static void insert(Address address, SQLiteDatabase db) {
		long insertId = db.insert(getTableName(address.getCoinId()), 
				null, keyToContentValues(address, true));
		address.setId(insertId);
	}

	protected static List<Address> readAllAddresses(OWCoin.Type coinId, SQLiteDatabase db) {
		List<Address> addresses = new ArrayList<Address>();

		String selectQuery = "SELECT * FROM " + getTableName(coinId) + ";";
		Cursor c = db.rawQuery(selectQuery, null);

		// Move to first returns false if cursor is empty
		if (c.moveToFirst()) {
			do {
				try {
					Address newAddress = cursorToAddress(coinId, c);

					// Add the new key to the list
					addresses.add(newAddress);
				} catch(AddressFormatException afe) {
					ZLog.log("Error loading address from ", coinId.toString(), 
							" sending addresses database.");
					afe.printStackTrace();
				}
				
			} while (c.moveToNext());
		}

		// Make sure we close the cursor
		c.close();

		return addresses;
	}

	protected static int numAddresses(OWCoin.Type coinId, SQLiteDatabase db) {
		String countQuery = "SELECT  * FROM " + getTableName(coinId);
		Cursor cursor = db.rawQuery(countQuery, null);
		int count = cursor.getCount();
		cursor.close();
		return count;
	}

	protected static void updateAddress(Address address, SQLiteDatabase db) {
		if (address.getId() == -1) {
			// Shouldn't happen
			throw new RuntimeException("Error: id has not been set.");
		}
		ContentValues values = keyToContentValues(address, false);
		db.update(getTableName(address.getCoinId()), values, COLUMN_ID + " = " + address.getId(), null);
	}

	/**
	 * @param coinId
	 * @param c
	 * @return
	 * @throws AddressFormatException
	 */
	private static Address cursorToAddress(OWCoin.Type coinId, Cursor c) throws AddressFormatException {
		Address newAddress = new Address(coinId, c.getString(c.getColumnIndex(COLUMN_ADDRESS)));

		// Reset all the keys parameters for use elsewhere
		newAddress.setId(c.getLong(c.getColumnIndex(COLUMN_ID)));
		newAddress.setNote(c.getString(c.getColumnIndex(COLUMN_NOTE)));
		newAddress.setLastKnownBalance(c.getInt(c.getColumnIndex(COLUMN_BALANCE)));
		newAddress.setLastTimeModifiedSeconds(c.getLong(c.getColumnIndex(COLUMN_MODIFIED_TIMESTAMP)));
		//newAddress.setCreationTimeSeconds(c.getLong(c.getColumnIndex(COLUMN_CREATION_TIMESTAMP)));
		return newAddress;
	}

	private static ContentValues keyToContentValues(Address address, boolean forInsert) {
		ContentValues values = new ContentValues();

		if (forInsert) {
			// For inserting a key into the db, we need the keys but the id
			// will be generated upon insertion.
			values.put(COLUMN_ADDRESS, address.toString());
		} 
		values.put(COLUMN_NOTE, address.getNote());
		values.put(COLUMN_BALANCE, address.getLastKnownBalance());
		values.put(COLUMN_MODIFIED_TIMESTAMP, address.getLastTimeModifiedSeconds());
		//values.put(COLUMN_CREATION_TIMESTAMP, address.getCreationTimeSeconds());
		return values;
	}

}