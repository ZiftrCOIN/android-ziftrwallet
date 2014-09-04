package com.ziftr.android.onewallet.sqlite;

import java.math.BigInteger;
import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.ziftr.android.onewallet.crypto.OWAddress;
import com.ziftr.android.onewallet.crypto.OWSha256Hash;
import com.ziftr.android.onewallet.exceptions.OWAddressFormatException;
import com.ziftr.android.onewallet.fragment.accounts.OWWalletTransaction;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.ZLog;

/**
 * This class gives the app access to a database that will persist in different
 * sessions of the app being opened. 
 * 
 * To get an instance of this class call OWSQLiteOpenHelper.getInstance(Context).
 * When finished accessing the database, call OWSQLiteOpenHelper.closeInstance().
 * 
 * TODO add a long click to list items to delete coin types
 * TODO how does the encrypting of the private keys work into this?
 */
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

	/**
	 * <p>It's possible to calculate a wallets balance from multiple points of view. This enum specifies which
	 * of the two methods the getWallatBalance() should use.</p>
	 *
	 * <p>Consider a real-world example: you buy a snack costing $5 but you only have a $10 bill. At the start you have
	 * $10 viewed from every possible angle. After you order the snack you hand over your $10 bill. From the
	 * perspective of your wallet you have zero dollars (AVAILABLE). But you know in a few seconds the shopkeeper
	 * will give you back $5 change so most people in practice would say they have $5 (ESTIMATED).</p>
	 */
	public enum BalanceType {
		/**
		 * Balance calculated assuming all pending transactions are in fact included into the best chain by miners.
		 * This includes the value of immature coinbase transactions.
		 */
		ESTIMATED,

		/**
		 * Balance that can be safely used to create new spends. This is whatever the default coin selector would
		 * make available, which by default means transaction outputs with at least 1 confirmation and pending
		 * transactions created by our own wallet which have been propagated across the network.
		 */
		AVAILABLE
	}

	/** The helper for doing all things related to the receiving addresses table. */
	private OWAddressesTable receivingAddressesTable;
	
	/** The helper for doing all things related to the sending addresses table. */
	private OWAddressesTable sendingAddressesTable;

	///////////////////////////////////////////////////////
	//////////  Boiler plate SQLite Table Stuff ///////////
	///////////////////////////////////////////////////////

	protected OWSQLiteOpenHelper(Context context, String databasePath) {
		// If the database path is null then an in memory database is used
		super(context, databasePath, null, DATABASE_VERSION);
		
		this.sendingAddressesTable = new OWSendingAddressesTable();
		this.receivingAddressesTable = new OWReceivingAddressesTable();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		ZLog.log("onCreate for helper called");

		// Make table of coin activation statuses
		OWCoinActivationStatusTable.create(db);

		// Fill in the table with all coin types, using UNACTIVATED as the status
		for (OWCoin.Type t : OWCoin.Type.values()) {
			OWCoinActivationStatusTable.insert(t, UNACTIVATED, db);
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
	 * Call this method to activate a coin type if it is not already activated.
	 * If it has not been activated then this method will create the necessary tables 
	 * for the coin type given and update the activated statuses table appropriately.
	 * 
	 * @param coinId
	 */
	public void ensureCoinTypeActivated(OWCoin.Type coinId) {
		// Safety check
		if (typeIsActivated(coinId)) {
			return;
		}

		// TODO create tables for coin type
		this.receivingAddressesTable.create(coinId, getWritableDatabase());
		this.sendingAddressesTable.create(coinId, getWritableDatabase());

		// Update table to match activated status
		this.updateTableActivitedStatus(coinId, ACTIVATED);
	}

	public boolean typeIsActivated(OWCoin.Type coinId) {
		return OWCoinActivationStatusTable.getActivatedStatus(
				coinId, getReadableDatabase()) == ACTIVATED;
	}

	public List<OWCoin.Type> readAllActivatedTypes() {
		return OWCoinActivationStatusTable.getAllActivatedTypes(this.getReadableDatabase());
	}

	public void updateTableActivitedStatus(OWCoin.Type coinId, int status) {
		OWCoinActivationStatusTable.update(coinId, status, getWritableDatabase());
	}

	/*
	 * NOTE:
	 * Shouldn't ever insert/delete from activated status table, so we don't
	 * supply insert/delete convenience methods here, only an update.
	 */

	//////////////////////////////////////////////////////////////////
	//////////  Interface for CRUDing receiving addresses  ///////////
	//////////////////////////////////////////////////////////////////

	/**
	 * As part of the C in CRUD, this method adds a receiving (owned by the user)
	 * address to the correct table within our database.
	 * 
	 * Default values will be used in this method for the note, balance, and status.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 */
	public OWAddress createReceivingAddress(OWCoin.Type coinId) {
		return createReceivingAddress(coinId, "");
	}

	/**
	 * As part of the C in CRUD, this method adds a receiving (owned by the user)
	 * address to the correct table within our database.
	 * 
	 * Default values will be used in this method for the balance and status.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @param note - The note that the user associates with this address.
	 */
	public OWAddress createReceivingAddress(OWCoin.Type coinId, String note) {
		long time = System.currentTimeMillis() / 1000;
		return createReceivingAddress(coinId, note, 0, time, time);
	}

	/**
	 * As part of the C in CRUD, this method adds a receiving (owned by the user)
	 * address to the correct table within our database.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @param key - The key to use.
	 */
	public OWAddress createReceivingAddress(OWCoin.Type coinId, String note, 
			long balance, long creation, long modified) {
		OWAddress address = new OWAddress(coinId);
		address.setNote(note);
		address.setLastKnownBalance(balance);
		address.getKey().setCreationTimeSeconds(creation);
		address.setLastTimeModifiedSeconds(modified);
		this.receivingAddressesTable.insert(address, this.getWritableDatabase());
		return address;
	}
	
	/**
	 * As part of the R in CRUD, this method gets a receiving address from the 
	 * database for the given coin type.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @param address - The list of 1xyz... (Base58) encoded address in the database. 
	 */
	public OWAddress readReceivingAddress(OWCoin.Type coinId, String address) {
		return this.receivingAddressesTable.readAddress(coinId, address, getReadableDatabase());
	}
	
	/**
	 * As part of the R in CRUD, this method gets a receiving address from the 
	 * database for the given coin type.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @param address - The 1xyz... (Base58) encoded address in the database. 
	 */
	public List<OWAddress> readReceivingAddresses(OWCoin.Type coinId, List<String> addresses) {
		return this.receivingAddressesTable.readAddresses(coinId, addresses, getReadableDatabase());
	}

	/**
	 * As part of the R in CRUD, this method gets all the receiving addresses from the 
	 * database for the given coin type.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 */
	public List<OWAddress> readAllReceivingAddresses(OWCoin.Type coinId) {
		return this.receivingAddressesTable.readAllAddresses(coinId, getReadableDatabase());
	}

	/**
	 * As part of the R in CRUD, this method gives the number of entries
	 * in the receiving addresses table for the coin type specified. 
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 */
	public int readNumReceivingAddresses(OWCoin.Type coinId) {
		return this.receivingAddressesTable.numAddresses(coinId, getReadableDatabase());
	}

	/**
	 * As part of the U in CRUD, this method updates the  addresses table in 
	 * the database for the given coin type and key. Doesn't update any pub/address 
	 * related data, just the private key (if encryption phrase changes) and the other 
	 * data associated with this key, such as timestamp, note, etc.
	 * 
	 * TODO is there a way to make sure that the new private key matches the old,
	 * even on encryption state changes?
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 */
	public void updateReceivingAddress(OWAddress address) {
		this.receivingAddressesTable.updateAddress(address, getWritableDatabase());
	}

	/* 
	 * NOTE:
	 * Note that we don't give the option to delete an address, shouldn't ever
	 * delete an address just in case someone sends coins to an old address.
	 */

	////////////////////////////////////////////////////////////////
	//////////  Interface for CRUDing sending addresses  ///////////
	////////////////////////////////////////////////////////////////

	/**
	 * As part of the C in CRUD, this method adds a sending (not owned by the user)
	 * address to the correct table within our database.
	 * 
	 * Default values will be used in this method for the note, balance, and status.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @throws OWAddressFormatException 
	 */
	public OWAddress createSendingAddress(OWCoin.Type coinId, String addressString) 
			throws OWAddressFormatException {
		return createSendingAddress(coinId, addressString, "");
	}

	/**
	 * As part of the C in CRUD, this method adds a sending (not owned by the user)
	 * address to the correct table within our database.
	 * 
	 * Default values will be used in this method for the balance and status.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @param note - The note that the user associates with this address.
	 * @throws OWAddressFormatException 
	 */
	public OWAddress createSendingAddress(OWCoin.Type coinId, String addressString, String note) throws OWAddressFormatException {
		long time = System.currentTimeMillis() / 1000;
		return createSendingAddress(coinId, addressString, note, 0, time);
	}

	/**
	 * As part of the C in CRUD, this method adds a sending (not owned by the user)
	 * address to the correct table within our database.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @param key - The key to use.
	 * @throws OWAddressFormatException 
	 */
	public OWAddress createSendingAddress(OWCoin.Type coinId, String addressString, String note, 
			long balance, long modified) throws OWAddressFormatException {
		OWAddress address = new OWAddress(coinId, addressString);
		address.setNote(note);
		address.setLastKnownBalance(balance);
		//address.getKey().setCreationTimeSeconds(creation);
		address.setLastTimeModifiedSeconds(modified);
		this.sendingAddressesTable.insert(address, this.getWritableDatabase());
		return address;
	}
	
	/**
	 * As part of the R in CRUD, this method gets a receiving address from the 
	 * database for the given coin type.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @param address - The 1xyz... (Base58) encoded address in the database. 
	 */
	public OWAddress readSendingAddress(OWCoin.Type coinId, String address) {
		return this.sendingAddressesTable.readAddress(coinId, address, getReadableDatabase());
	}
	
	/**
	 * As part of the R in CRUD, this method gets a sending address from the 
	 * database for the given coin type.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @param address - The list of 1xyz... (Base58) encoded address in the database. 
	 */
	public List<OWAddress> readSendingAddresses(OWCoin.Type coinId, List<String> addresses) {
		return this.sendingAddressesTable.readAddresses(coinId, addresses, getReadableDatabase());
	}

	/**
	 * As part of the R in CRUD, this method gets all the known external (not 
	 * owned) addresses from the database for the given coin type.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 */
	public List<OWAddress> readAllSendingAddresses(OWCoin.Type coinId) {
		return this.sendingAddressesTable.readAllAddresses(coinId, getReadableDatabase());
	}

	/**
	 * As part of the R in CRUD, this method gives the number of entries
	 * in the sending addresses table for the coin type specified. 
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 */
	public int readNumSendingAddresses(OWCoin.Type coinId) {
		return this.sendingAddressesTable.numAddresses(coinId, getReadableDatabase());
	}
	
	/**
	 * As part of the U in CRUD, this method updates the sending addresses table in 
	 * the database for the given address. Doesn't the actual address, just the data 
	 * associated with this key, such as timestamp, note, etc. We don't update the
	 * actual address string because addresses should not be deleted, only added.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 */
	public void updateSendingAddress(OWAddress address) {
		this.sendingAddressesTable.updateAddress(address, getWritableDatabase());
	}
	
	/* 
	 * NOTE:
	 * Note that we don't currently give the option to delete a sending address. Might 
	 * want to add this feature? Maybe a long click in the address list?
	 */
	
	//////////////////////////////////////////////////////////
	//////////  Helpful method for combining lists  //////////
	//////////////////////////////////////////////////////////
	
	/**
	 * As part of the R in CRUD, this method gets all the known external (not 
	 * owned) addresses from the database for the given coin type.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 */
	public List<OWAddress> readAllAddresses(OWCoin.Type coinId) {
		List<OWAddress> addresses = this.readAllReceivingAddresses(coinId);
		addresses.addAll(this.readAllSendingAddresses(coinId));
		return addresses;
	}
	
	///////////////////////////////////////////////////////////
	//////////  Interface for CRUDing transactions  ///////////
	///////////////////////////////////////////////////////////

	/**
	 * As part of the C in CRUD, this method adds a new transaction
	 * to the correct table within our database.
	 * 
	 * Default values will be used in this method for the hahs, note, time, 
	 * and numConfirmations.
	 * 
	 * @param coinId - The coin type to determine which table we use.
	 * @param txAmount - See {@link OWWalletTransaction}
	 * @param txFee- See {@link OWWalletTransaction}
	 * @param displayAddress - See {@link OWWalletTransaction}
	 */
	public OWWalletTransaction createTransaction(OWCoin.Type coinId, BigInteger txAmount,
			BigInteger txFee, OWAddress displayAddress) {
		return createTransaction(coinId, txAmount, txFee, 
				displayAddress, new OWSha256Hash(""), "", -1);
	}

	/**
	 * As part of the C in CRUD, this method adds a new transaction
	 * to the correct table within our database.
	 * 
	 * Default values will be used in this method for the hahs, note, time, 
	 * and numConfirmations.
	 * 
	 * @param coinId - The coin type to determine which table we use.
	 * @param txAmount - See {@link OWWalletTransaction}
	 * @param txFee- See {@link OWWalletTransaction}
	 * @param displayAddress - See {@link OWWalletTransaction}
	 */
	public OWWalletTransaction createTransaction(OWCoin.Type coinId, BigInteger txAmount,
			BigInteger txFee, OWAddress displayAddress, OWSha256Hash hash, 
			String note, int numConfirmations) {
		// TODO 
		return null;
	}

	public BigInteger getWalletBalance(OWCoin.Type coinId, BalanceType bType) {
		// TODO 
		return null;
	}

	public List<OWWalletTransaction> getPendingTransactions(OWCoin.Type coinId) {
		// TODO 
		return null;
	}

	public List<OWWalletTransaction> getConfirmedTransactions(OWCoin.Type coinId) {
		// TODO 
		return null;
	}

	// TODO Maybe make a method to get a list of all transactions?

}