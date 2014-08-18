package com.ziftr.android.onewallet.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.onewallet.crypto.ECKey;
import com.ziftr.android.onewallet.util.Base58;
import com.ziftr.android.onewallet.util.OWCoinRelative;
import com.ziftr.android.onewallet.util.OWUtils;

public abstract class OWUsersAddressesSQLiteHelper extends OWSQLiteOpenHelper 
implements OWCoinRelative {

	/** The name of the table this will create. */
	public final String USER_ADDRESSES_TABLE = 
			this.getCoinId().getShortTitle() + "_users_addresses";

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
	public static final String COLUMN_TIME = "time";

	public final String CREATE_TABLE = 
			"CREATE TABLE IF NOT EXISTS " + USER_ADDRESSES_TABLE + " (" + 
					COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
					COLUMN_PRIV_KEY + " TEXT NOT NULL, " + 
					COLUMN_PUB_KEY + " TEXT NOT NULL, " +
					COLUMN_ADDRESS + " TEXT NOT NULL, " + 
					COLUMN_NOTE + " TEXT, " + 
					COLUMN_BALANCE + " INTEGER, " +
					COLUMN_TIME + " INTEGER );";

	/**
	 * Used to help subclasses (since this is an abstract class) creat a new 
	 * user addresses table. 
	 *  
	 * @param context
	 */
	public OWUsersAddressesSQLiteHelper(Context context) {
		super(context);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Used for porting old versions of an app to a newer version and retaining
		// the data in the old database even when the format of the new database
		// changes.

		//db.execSQL("DROP TABLE IF EXISTS " + USER_ADDRESSES_TABLE + ";");
		//db.execSQL(CREATE_TABLE);
	}
	
	public void insert(ECKey key) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_PRIV_KEY, OWUtils.binaryToHexString(key.getPrivKeyBytes()));
		values.put(COLUMN_PUB_KEY, OWUtils.binaryToHexString(key.getPubKey()));
//		values.put(COLUMN_ADDRESS, Base58.encode(getCoinId().getPubKeyHashPrefix(), key.get));
	}

}