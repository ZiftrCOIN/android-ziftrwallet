package com.ziftr.android.onewallet.sqlite;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.onewallet.crypto.Address;
import com.ziftr.android.onewallet.crypto.AddressFormatException;
import com.ziftr.android.onewallet.crypto.ECKey;
import com.ziftr.android.onewallet.util.Base58;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWCoinRelative;
import com.ziftr.android.onewallet.util.OWUtils;
import com.ziftr.android.onewallet.util.ZLog;


/**
 * Make one table accessing class per coin type. Could those all subclass 
 * 
 * TODO should have a method that inserts a new key and returns the key.
 * Get rid of duplicate strings in strings.xml.
 */
public abstract class OWReceivingAddressesTable implements OWCoinRelative {

	/** The postfix that assists in making the names for the users addresses table. */
	private static final String TABLE_POSTFIX = "_receiving_addresses";

	/** The id column. All Tables must have this to work well with adapters. */
	public static final String COLUMN_ID = "_id";

	/** The private key encoded as a string using WIF. */
	public static final String COLUMN_PRIV_KEY = "priv_key";

	/** The public key, encoded as a string. */
	public static final String COLUMN_PUB_KEY = "pub_key";

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

	/** This is the timestamp when the address was created. */
	public static final String COLUMN_CREATION_TIMESTAMP = "creation_timestamp";

	/** 
	 * This is the last time that address was used in a transaction. Note that this
	 * table may not always be up to date and this may just be the last known time
	 * that the address was used. 
	 */
	public static final String COLUMN_MODIFIED_TIMESTAMP = "modified_timestamp";

	protected static void create(OWCoin.Type coinId, SQLiteDatabase db) {
		db.execSQL(getCreateTableString(coinId));
	}

	protected static String getCreateTableString(OWCoin.Type coinId) {
		return "CREATE TABLE IF NOT EXISTS " + getTableName(coinId) + " (" + 
				COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				COLUMN_PRIV_KEY + " TEXT NOT NULL, " + 
				COLUMN_PUB_KEY + " TEXT NOT NULL, " +
				COLUMN_ADDRESS + " TEXT NOT NULL, " + 
				COLUMN_NOTE + " TEXT, " + 
				COLUMN_BALANCE + " INTEGER, " +
				COLUMN_CREATION_TIMESTAMP + " INTEGER, " +
				COLUMN_MODIFIED_TIMESTAMP + " INTEGER );";
	}

	protected static String getTableName(OWCoin.Type coinId) {
		return coinId.toString() + TABLE_POSTFIX;
	}

	protected static void insert(Address address, SQLiteDatabase db) {
		if (!address.isPersonalAddress()) {
			throw new IllegalArgumentException("Must have access to private key to insert. ");
		}

		long insertId = db.insert(getTableName(address.getCoinId()), 
				null, addressToContentValuesMap(address, true));
		address.setId(insertId);
	}

	protected static List<Address> readAllAddresses(OWCoin.Type coinId, SQLiteDatabase db) {
		List<Address> addresses = new ArrayList<Address>();

		String selectQuery = "SELECT * FROM " + getTableName(coinId) + ";";
		Cursor c = db.rawQuery(selectQuery, null);

		// Move to first returns false if cursor is empty
		if (c.moveToFirst()) {
			do {
				// Recreate key from stored private key
				try {
					// TODO deal with encryption of private key
					Address newAddress = cursorToAddress(coinId, c);
					addresses.add(newAddress);
				} catch (AddressFormatException afe) {
					ZLog.log("Error loading address from ", coinId.toString(), 
							" receiving addresses database.");
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
		ContentValues values = addressToContentValuesMap(address, false);
		db.update(getTableName(address.getCoinId()), values, COLUMN_ID + " = " + address.getId(), null);
	}

	/**
	 * @param coinId
	 * @param c
	 * @return
	 * @throws AddressFormatException
	 */
	private static Address cursorToAddress(OWCoin.Type coinId, Cursor c) throws AddressFormatException {
		String encodedPrivKey = c.getString(c.getColumnIndex(COLUMN_PRIV_KEY));
		byte[] wifPrivKeyBytes = Base58.decodeChecked(encodedPrivKey);
		ECKey newKey = new ECKey(new BigInteger(1, OWUtils.stripVersionAndChecksum(wifPrivKeyBytes)));
		Address newAddress = new Address(coinId, newKey);

		// Reset all the address' parameters for use elsewhere
		newAddress.setNote(c.getString(c.getColumnIndex(COLUMN_NOTE)));
		newAddress.setLastKnownBalance(c.getInt(c.getColumnIndex(COLUMN_BALANCE)));
		newAddress.getKey().setCreationTimeSeconds(c.getLong(c.getColumnIndex(COLUMN_CREATION_TIMESTAMP)));
		newAddress.setLastTimeModifiedSeconds(c.getLong(c.getColumnIndex(COLUMN_MODIFIED_TIMESTAMP)));
		return newAddress;
	}

	private static ContentValues addressToContentValuesMap(Address address, boolean forInsert) {
		// Can assume here that address.getKey() doesn't return null.

		ContentValues values = new ContentValues();
		// TODO can we do this as a blob?
		values.put(COLUMN_PRIV_KEY, Base58.encode(
				address.getCoinId().getPrivKeyPrefix(), address.getKey().getPrivKeyBytesForAddressEncoding()));

		// Either for insert or for update
		if (forInsert) {
			// For inserting a key into the db, we need the keys
			values.put(COLUMN_PUB_KEY, OWUtils.bytesToHexString(address.getKey().getPubKey()));
			values.put(COLUMN_ADDRESS, Base58.encode(address.getCoinId().getPubKeyHashPrefix(), address.getHash160()));
		} 
		values.put(COLUMN_NOTE, address.getNote());
		values.put(COLUMN_BALANCE, address.getLastKnownBalance());
		values.put(COLUMN_CREATION_TIMESTAMP, address.getKey().getCreationTimeSeconds());
		values.put(COLUMN_MODIFIED_TIMESTAMP, address.getLastTimeModifiedSeconds());

		// The id will be generated upon insertion.
		return values;
	}

}