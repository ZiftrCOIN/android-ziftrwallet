package com.ziftr.android.onewallet.sqlite;

import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.ziftr.android.onewallet.crypto.ECKey;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.ZLog;


//TODO
// Use singleton patern where must close the database before 
// you can get a new OWSQLiteOpenHelper

// TODO add method to ECKey (if not exists) to determine if it has private key

// TODO add a long click to list items to delete coin types

// TODO how does the encrypting of the private keys work into this?

public class OWSQLiteOpenHelper extends SQLiteOpenHelper {

	/** Following the standard, we name our wallet file as below. */
	public static final String DATABASE_NAME = "wallet.dat";

	/** The current database version, may change in later versions of app. */
	public static final int DATABASE_VERSION = 1;

	/** Used for table types that just haven't been used yet. */
	public static final int UNACTIVATED = 0;

	/** Used for table types that are activated and in use by user. */
	public static final int ACTIVATED = 1;

	/** Used for table types that used to be ACTIVATED, but now user deactivated them. */
	public static final int DEACTIVATED = 2;


	///////////////////////////////////////////////////////
	//////////  Static Singleton Access Members ///////////
	///////////////////////////////////////////////////////

	/** The single instance of the database helper that we use. */
	private static OWSQLiteOpenHelper instance;

	/** 
	 * Gets the singleton instance of the database helper, making it if necessary. 
	 */
	public static synchronized OWSQLiteOpenHelper getInstance(Context context) {
		if (instance == null) {
			instance = new OWSQLiteOpenHelper(context);
		} else {
			// If used right shouldn't happen because close should always
			// be called after every get instance call.
			ZLog.log("instance wasn't null and we called getInstance...");
		}
		return instance;
	}

	/**
	 * Closes the database helper instance. Also resets the singleton instance
	 * to be null.
	 */
	public static synchronized void closeInstance() {
		if (instance != null) {
			instance.close();
		} else {
			// If used right shouldn't happen because get instance should always
			// be called before every close call.
			ZLog.log("instance was null when we called getInstance...");
		}
		instance = null;
	}

	///////////////////////////////////////////////////////
	//////////  Boiler plate SQLite Table Stuff ///////////
	///////////////////////////////////////////////////////

	private OWSQLiteOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		ZLog.log("onCreate for helper called");

		// Make table of coin activation statuses
		OWCoinActivationStatusTable.create(db);

		// Fill in the table with all coin types, using UNACTIVATED as the status
		for (OWCoin.Type t : OWCoin.Type.values()) {
			OWCoinActivationStatusTable.insert(db, t, UNACTIVATED);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		ZLog.log("onUpgrade for helper called");
		// Nothing to do for Version 1
	}

	////////////////////////////////////////////////////////////////////
	//////////  Interface for CRUDing ActivatedTables table  ///////////
	////////////////////////////////////////////////////////////////////

	/**
	 * Should only be called if typeIsActivated returns false. 
	 * 
	 * @param coinId
	 */
	public void createTableSet(OWCoin.Type coinId) {
		// Safety check
		if (typeIsActivated(coinId)) {
			ZLog.log("createTableSet was called even though ", 
					coinId.toString(), " has already been activated.");
			return;
		}

		// TODO create tables for coin type
		
		// Update table to match activated status
		this.updateTableActivitedStatus(coinId, ACTIVATED);
	}

	public List<OWCoin.Type> readAllActivatedTypes() {
		return OWCoinActivationStatusTable.getAllActivatedTypes(this.getReadableDatabase());
	}

	public boolean typeIsActivated(OWCoin.Type coinId) {
		return OWCoinActivationStatusTable.getActivatedStatus(
				getReadableDatabase(), coinId) == ACTIVATED;
	}

	public void updateTableActivitedStatus(OWCoin.Type coinId, int status) {
		OWCoinActivationStatusTable.update(coinId, status, getWritableDatabase());
	}
	
	/*
	 * NOTE:
	 * Shouldn't ever insert/delete from activated status table, so we don't
	 * supply insert/delete convenience methods here, only an update.
	 */

	/////////////////////////////////////////////////////////////////
	//////////  Interface for CRUDing personal addresses  ///////////
	/////////////////////////////////////////////////////////////////

	/**
	 * As part of the C in CRUD, this method adds a personal (owned by the user)
	 * address to the correct table within our database.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @param key - The key to use.
	 */
	public void createPersonal(OWCoin.Type coinId, ECKey key) {
		OWUsersAddressesTable.insert(coinId, key, this.getWritableDatabase());
	}

	/**
	 * As part of the R in CRUD, this method gets all the ECKeys from the 
	 * database for the given coin type.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 */
	public List<ECKey> readAllPersonal(OWCoin.Type coinId) {
		// TODO
		return null;
	}

	/**
	 * As part of the R in CRUD, this method gives the number of entries
	 * in the personal addresses table for the coin type specified. 
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 */
	public int getNumPersonal(OWCoin.Type coinId) {
		// TODO
		return -1;
	}

	/**
	 * As part of the U in CRUD, this method updates the personal addresses table in 
	 * the database for the given coin type and key. Doesn't update any pub/address 
	 * related data, just the private key (if encryption phrase changes) and the other 
	 * data associated with this key, such as timestamp, note, etc. 
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 */
	public void updatePersonal(OWCoin.Type coinId, ECKey key) {
		// TODO check that public keys match, if not throw error
		// update all other info that doesn't relate to key stuff
	}

	/*
	 * NOTE:
	 * Note that we don't give the option to delete an address, shouldn't ever
	 * delete anything.
	 */

}