package com.ziftr.android.onewallet.sqlite;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.onewallet.crypto.ECKey;
import com.ziftr.android.onewallet.util.AddressFormatException;
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
public abstract class OWUsersAddressesTable implements OWCoinRelative {

	/** The postfix that assists in making the names for the users addresses table. */
	private static final String TABLE_POSTFIX = "_users_addresses";

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

	/** 
	 * This is the last time that address was used in a transaction. Note that this
	 * table may not always be up to date and this may just be the last known time
	 * that the address was used. 
	 */
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

	protected static void insert(OWCoin.Type coinId, ECKey key, SQLiteDatabase db) {
		if (key.getPrivKeyBytes() == null) {
			throw new IllegalArgumentException("Must have access to private key to insert. ");
		}

		long insertId = db.insert(getTableName(coinId), 
				null, keyToContentValues(coinId, key, true));
		key.setId(insertId);
	}

	protected static List<ECKey> readAllKeys(OWCoin.Type coinId, SQLiteDatabase db) {
		List<ECKey> keys = new ArrayList<ECKey>();

		String selectQuery = "SELECT * FROM " + getTableName(coinId) + ";";
		Cursor c = db.rawQuery(selectQuery, null);

		// Move to first returns false if cursor is empty
		if (c.moveToFirst()) {
			do {
				// Recreate key from stored private key
				// TODO deal with encryption of private key
				String encodedPrivKey = c.getString(c.getColumnIndex(COLUMN_PRIV_KEY));
				try {
					byte[] wifPrivKeyBytes = Base58.decodeChecked(encodedPrivKey);
					if (wifPrivKeyBytes[0] != coinId.getPrivKeyPrefix() && 
							wifPrivKeyBytes[0] != coinId.getPrivKeyPrefix()) {
						throw new AddressFormatException();
					}
					ECKey newKey = new ECKey(new BigInteger(1, OWUtils.wifPrivBytesToStandardPrivBytes(wifPrivKeyBytes)));

					// Reset all the keys parameters for use elsewhere
					newKey.setAddress(c.getString(c.getColumnIndex(COLUMN_ADDRESS)));
					newKey.setNote(c.getString(c.getColumnIndex(COLUMN_NOTE)));
					newKey.setLastKnownBalance(c.getInt(c.getColumnIndex(COLUMN_BALANCE)));
					newKey.setCreationTimeSeconds(c.getLong(c.getColumnIndex(COLUMN_CREATION_TIMESTAMP)));
					newKey.setLastTimeModifiedSeconds(c.getLong(c.getColumnIndex(COLUMN_MODIFIED_TIMESTAMP)));

					// Add the new key to the list
					keys.add(newKey);
				} catch (AddressFormatException e) {
					// TODO Auto-generated catch block
					ZLog.log("Error: either leading byte or checksum is not correct.");
					e.printStackTrace();
				}
			} while (c.moveToNext());
		}

		// Make sure we close the cursor
		c.close();

		return keys;
	}

	protected static int numPersonalAddresses(OWCoin.Type coinId, SQLiteDatabase db) {
		String countQuery = "SELECT  * FROM " + getTableName(coinId);
		Cursor cursor = db.rawQuery(countQuery, null);
		int count = cursor.getCount();
		cursor.close();
		return count;
	}

	protected static void updatePersonalAddress(OWCoin.Type coinId, ECKey key, SQLiteDatabase db) {
		if (key.getId() == -1) {
			// Shouldn't happen
			throw new RuntimeException("Error: id has not been set.");
		}
		ContentValues values = keyToContentValues(coinId, key, false);
		db.update(getTableName(coinId), values, COLUMN_ID + " = " + key.getId(), null);
	}

	private static ContentValues keyToContentValues(OWCoin.Type coinId, ECKey key, boolean forInsert) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_PRIV_KEY, Base58.encode(
				coinId.getPrivKeyPrefix(), key.getPrivKeyBytesForAddressEncoding()));
		
		if (forInsert) {
			// For inserting a key into the db, we need the keys but the id
			// will be generated upon insertion.
			values.put(COLUMN_PUB_KEY, OWUtils.bytesToHexString(key.getPubKey()));
			values.put(COLUMN_ADDRESS, Base58.encode(coinId.getPubKeyHashPrefix(), key.getPubKeyHash()));
		} 
		values.put(COLUMN_NOTE, key.getNote());
		values.put(COLUMN_BALANCE, key.getLastKnownBalance());
		values.put(COLUMN_CREATION_TIMESTAMP, key.getCreationTimeSeconds());
		values.put(COLUMN_MODIFIED_TIMESTAMP, key.getLastTimeModifiedSeconds());
		return values;
	}

}