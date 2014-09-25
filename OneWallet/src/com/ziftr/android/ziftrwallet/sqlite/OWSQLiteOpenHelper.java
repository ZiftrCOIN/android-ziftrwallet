package com.ziftr.android.ziftrwallet.sqlite;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.crypto.OWAddress;
import com.ziftr.android.ziftrwallet.crypto.OWSha256Hash;
import com.ziftr.android.ziftrwallet.crypto.OWTransaction;
import com.ziftr.android.ziftrwallet.exceptions.OWAddressFormatException;
import com.ziftr.android.ziftrwallet.fragment.accounts.OWWalletTransactionListAdapter;
import com.ziftr.android.ziftrwallet.util.OWCoin;
import com.ziftr.android.ziftrwallet.util.OWFiat;
import com.ziftr.android.ziftrwallet.util.ZLog;

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

	/** The table to keep track of coin activated/deactivated/unactivated status. */
	private OWCoinActivationStatusTable coinActivationTable;

	/** The table to keep track of transactions. */
	private OWWalletTransactionTable transactionsTable;

	///////////////////////////////////////////////////////
	//////////  Boiler plate SQLite Table Stuff ///////////
	///////////////////////////////////////////////////////

	protected OWSQLiteOpenHelper(Context context, String databasePath) {
		// If the database path is null then an in memory database is used
		super(context, databasePath, null, DATABASE_VERSION);

		this.sendingAddressesTable = new OWSendingAddressesTable();
		this.receivingAddressesTable = new OWReceivingAddressesTable();
		this.coinActivationTable = new OWCoinActivationStatusTable();
		this.transactionsTable = new OWWalletTransactionTable(
				this.sendingAddressesTable, this.receivingAddressesTable);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		ZLog.log("onCreate for helper called");

		// Make table of coin activation statuses
		this.coinActivationTable.create(db);

		// Fill in the table with all coin types, using UNACTIVATED as the status
		for (OWCoin t : OWCoin.values()) {
			this.coinActivationTable.insert(t, UNACTIVATED, db);
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
	public void ensureCoinTypeActivated(OWCoin coinId) {
		// Safety check
		if (typeIsActivated(coinId)) {
			return;
		}

		// TODO create tables for coin type
		this.receivingAddressesTable.create(coinId, this.getWritableDatabase());
		this.sendingAddressesTable.create(coinId, this.getWritableDatabase());
		this.transactionsTable.create(coinId, this.getWritableDatabase());

		// Update table to match activated status
		this.updateTableActivitedStatus(coinId, ACTIVATED);
	}

	public boolean typeIsActivated(OWCoin coinId) {
		return this.coinActivationTable.getActivatedStatus(
				coinId, getReadableDatabase()) == ACTIVATED;
	}

	public List<OWCoin> readAllActivatedTypes() {
		// TODO use this method in wallet manager rather (/ in addition to?) bitcoinj
		return this.coinActivationTable.getAllActivatedTypes(this.getReadableDatabase());
	}

	public void updateTableActivitedStatus(OWCoin coinId, int status) {
		this.coinActivationTable.update(coinId, status, getWritableDatabase());
	}

	/*
	 * NOTE:
	 * Shouldn't ever insert/delete from activated status table, so we don't
	 * supply insert/delete convenience methods here, only an update.
	 */

	//////////////////////////////////////////////////////////////////
	//////////  Interface for CRUDing receiving addresses  ///////////
	//////////////////////////////////////////////////////////////////

	private OWAddressesTable getTable(boolean receivingNotSending) {
		return receivingNotSending ? this.receivingAddressesTable : this.sendingAddressesTable;
	}

	/**
	 * As part of the C in CRUD, this method adds a receiving (owned by the user)
	 * address to the correct table within our database.
	 * 
	 * Default values will be used in this method for the note, balance, and status.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 */
	public OWAddress createReceivingAddress(OWCoin coinId) {
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
	public OWAddress createReceivingAddress(OWCoin coinId, String note) {
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
	public OWAddress createReceivingAddress(OWCoin coinId, String note, 
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
	 * As part of the R in CRUD, this method gets an address from the 
	 * database for the given coin type and table boolean. Returns null if
	 * no such address is found
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @param address - The list of 1xyz... (Base58) encoded address in the database.
	 * @param receivingNotSending - If true, uses receiving table. If false, sending table. 
	 */
	public OWAddress readAddress(OWCoin coinId, String address, boolean receivingNotSending) {
		return this.getTable(receivingNotSending).readAddress(coinId, address, getReadableDatabase());
	}

	/**
	 * As part of the R in CRUD, this method gets an address from the 
	 * database for the given coin type and table boolean.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @param addresses - The list of 1xyz... (Base58) encoded address in the database. 
	 * @param receivingNotSending - If true, uses receiving table. If false, sending table. 
	 */
	public List<OWAddress> readAddresses(OWCoin coinId, List<String> addresses, boolean receivingNotSending) {
		return this.getTable(receivingNotSending).readAddresses(coinId, addresses, getReadableDatabase());
	}

	/**
	 * As part of the R in CRUD, this method gets all the addresses from the 
	 * database for the given coin type and table boolean.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @param receivingNotSending - If true, uses receiving table. If false, sending table. 
	 * @return List of {@link OWAddress} objects. See {@link #getAddressList(OWCoin, boolean)} for list of Address strings.
	 */
	public List<OWAddress> readAllAddresses(OWCoin coinId, boolean receivingNotSending) {
		return this.getTable(receivingNotSending).readAllAddresses(coinId, getReadableDatabase());
	}

	/**
	 * As part of the R in CRUD, this method gives the number of entries
	 * in the addresses table for the coin type specified and table boolean. 
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @param receivingNotSending - If true, uses receiving table. If false, sending table.
	 */
	public int readNumAddresses(OWCoin coinId, boolean receivingNotSending) {
		return this.getTable(receivingNotSending).numEntries(coinId, getReadableDatabase());
	}

	/**
	 * As part of the U in CRUD, this method updates the addresses table in 
	 * the database for the given coin type and key and table boolean.
	 * 
	 * TODO is there a way to make sure that the new private key matches the old,
	 * even on encryption state changes?
	 * 
	 * @param addess - the address to fully update 
	 * @param receivingNotSending - If true, uses receiving table. If false, sending table.
	 */
	public void updateAddress(OWAddress address) {
		this.getTable(address.isPersonalAddress()).updateAddress(address, getWritableDatabase());
	}

	/**
	 * As part of the U in CRUD, this method updates the label for the given address.
	 * 
	 * @param coin - The coin type
	 * @param address - The address to update
	 * @param newLabel - the label to change the address to
	 * @param receivingNotSending - If true, uses receiving table. If false, sending table.
	 */
	public void updateAddressLabel(OWCoin coin, String address, String newLabel, boolean receivingNotSending) {
		this.getTable(receivingNotSending).updateAddressLabel(coin, address, newLabel, getWritableDatabase());
	}

	/* 
	 * NOTE:
	 * Note that we don't give the method to delete an address, shouldn't ever
	 * delete an address just in case someone sends coins to an old address.
	 */

	/////////////////////////////////////////////////////////////////
	//////////  Interface for creating sending addresses  ///////////
	/////////////////////////////////////////////////////////////////

	/**
	 * As part of the C in CRUD, this method adds a sending (not owned by the user)
	 * address to the correct table within our database.
	 * 
	 * Default values will be used in this method for the note, balance, and status.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @throws OWAddressFormatException 
	 */
	public OWAddress createSendingAddress(OWCoin coinId, String addressString) 
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
	public OWAddress createSendingAddress(OWCoin coinId, String addressString, String note) throws OWAddressFormatException {
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
	public OWAddress createSendingAddress(OWCoin coinId, String addressString, String note, 
			long balance, long modified) throws OWAddressFormatException {
		OWAddress address = new OWAddress(coinId, addressString);
		address.setNote(note);
		address.setLastKnownBalance(balance);
		//address.getKey().setCreationTimeSeconds(creation);
		address.setLastTimeModifiedSeconds(modified);
		this.sendingAddressesTable.insert(address, this.getWritableDatabase());
		return address;
	}

	//////////////////////////////////////////////////////////
	//////////  Helpful method for combining lists  //////////
	//////////////////////////////////////////////////////////

	/**
	 * As part of the R in CRUD, this method gets all the known external (not 
	 * owned) addresses from the database for the given coin type.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 */
	public List<OWAddress> readAllAddresses(OWCoin coinId) {
		List<OWAddress> addresses = this.readAllAddresses(coinId, false);
		addresses.addAll(this.readAllAddresses(coinId, true));
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
	 * @param txAmount - See {@link OWTransaction}
	 * @param txFee- See {@link OWTransaction}
	 * @param displayAddress - See {@link OWTransaction}
	 */
	public OWTransaction createTransaction(OWCoin coinId, BigInteger txAmount,
			BigInteger txFee, List<String> displayAddress) {
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
	 * @param txAmount - See {@link OWTransaction}
	 * @param txFee- See {@link OWTransaction}
	 * @param displayAddress - See {@link OWTransaction}
	 */
	public OWTransaction createTransaction(OWCoin coinId, BigInteger txAmount,
			BigInteger txFee, List<String> displayAddresses, OWSha256Hash hash, 
			String note, int numConfirmations) {
		OWTransaction tx = new OWTransaction(coinId, note, System.currentTimeMillis() / 100, txAmount);
		tx.setSha256Hash(hash);
		tx.setDisplayAddresses(displayAddresses);
		tx.setNumConfirmations(numConfirmations);
		this.transactionsTable.insertTx(tx, getWritableDatabase());
		return tx;
	}
	
	protected OWTransaction createTransaction(OWTransaction tx) {
		this.transactionsTable.insertTx(tx, getWritableDatabase());
		return tx;
	}

	public OWTransaction readTransactionByHash(OWCoin coinId, String hash) {
		if (hash == null) {
			return null;
		}

		List<String> hashes = new ArrayList<String>();
		hashes.add(hash);

		List<OWTransaction> readAddresses = 
				this.transactionsTable.readTransactionsByHash(coinId, hashes, this.getReadableDatabase());
		if (readAddresses.size() == 0) {
			return null;
		} else {
			return readAddresses.get(0);
		}
	}

	/**
	 * Gets a list of txs from the database that have the address specified. 
	 * If address is null then it gets all addresses from the database.
	 * Orders them by most recent. 
	 * 
	 * @param coinId
	 * @param address
	 * @param db
	 * @return
	 */
	public List<OWTransaction> readTransactionsByAddress(OWCoin coinId, String address) {
		return transactionsTable.readTransactionsByAddress(coinId, address, getReadableDatabase());
	}

	/**
	 * Gets a list of txs from the database that have the hashes specified. 
	 * If hashes is null then it gets all addresses from the database.
	 * Orders them by most recent. 
	 * 
	 * @param coinId
	 * @param addresses
	 * @param db
	 * @return
	 */
	public List<OWTransaction> readTransactionsByHash(OWCoin coinId, List<String> hashes) {
		return transactionsTable.readTransactionsByHash(coinId, hashes, getReadableDatabase());
	}

	public List<OWTransaction> readAllTransactions(OWCoin coinId) {
		return this.transactionsTable.readTransactions(coinId, null, getReadableDatabase());
	}

	public void updateTransaction(OWTransaction tx) {
		this.transactionsTable.updateTransaction(tx, getWritableDatabase());
	}

	public void updateTransactionNumConfirmations(OWTransaction tx) {
		this.transactionsTable.updateTransactionNumConfirmations(tx, getReadableDatabase());
	}


	protected void deleteTransaction(OWTransaction tx) {
		this.transactionsTable.deleteTransaction(tx, getWritableDatabase());
	}

	public BigInteger getWalletBalance(OWCoin coinId, BalanceType bType) {
		BigInteger balance = BigInteger.ZERO;
		List<OWTransaction> txs = this.readAllTransactions(coinId);

		for (OWTransaction tx : txs) {
			if (bType == BalanceType.AVAILABLE) {
				if (!tx.isPending()) {
					balance = balance.add(tx.getTxAmount());
				}
			} else if (tx.isBroadcasted()) {
				balance = balance.add(tx.getTxAmount());
			}
		}
		return balance;
	}

	public List<OWTransaction> getPendingTransactions(OWCoin coinId) {
		return transactionsTable.readPendingTransactions(coinId, getReadableDatabase());
	}

	public List<OWTransaction> getConfirmedTransactions(OWCoin coinId) {
		return transactionsTable.readConfirmedTransactions(coinId, getReadableDatabase());
	}

	public void updateTransactionNote(OWTransaction tx) {
		this.transactionsTable.updateTransactionNote(tx, getWritableDatabase());
	}
	
	
	public ArrayList<String> getAddressList(OWCoin coin, boolean receivingAddresses) {
		ArrayList<String> addresses;
		
		if(receivingAddresses) {
			addresses = this.receivingAddressesTable.getAddressesList(coin, getWritableDatabase());
		}
		else {
			addresses = this.sendingAddressesTable.getAddressesList(coin, getWritableDatabase());
		}
		
		return addresses;
	}

}