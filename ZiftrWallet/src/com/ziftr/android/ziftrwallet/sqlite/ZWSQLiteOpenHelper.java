/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.sqlite;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.spongycastle.crypto.CryptoException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWApplication;
import com.ziftr.android.ziftrwallet.crypto.ZWAddress;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWDefaultCoins;
import com.ziftr.android.ziftrwallet.crypto.ZWECKey;
import com.ziftr.android.ziftrwallet.crypto.ZWKeyCrypter;
import com.ziftr.android.ziftrwallet.crypto.ZWTransaction;
import com.ziftr.android.ziftrwallet.crypto.ZWTransactionOutput;
import com.ziftr.android.ziftrwallet.dialog.ZiftrDialogManager;
import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.sqlite.ZWReceivingAddressesTable.EncryptionStatus;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

/**
 * This class gives the app access to a database that will persist in different
 * sessions of the app being opened. 
 * 
 * To get an instance of this class call ZWSQLiteOpenHelper.getInstance(Context).
 * When finished accessing the database, call ZWSQLiteOpenHelper.closeInstance().
 */
public class ZWSQLiteOpenHelper extends SQLiteOpenHelper {

	public static final int DATABASE_VERSION = 3;

	
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
		 * make available, which by default means all confirmed transaction outputs minus pending spend transactions.
		 */
		AVAILABLE
	}

	/** The helper for doing all things related to the receiving addresses table. */
	private ZWReceivingAddressesTable receivingAddressesTable;

	/** The helper for doing all things related to the sending addresses table. */
	private ZWAddressesTable sendingAddressesTable;

	private ZWTransactionOutputsTable transactionOutputsTable;
	
	/** The table to keep track of coin activated/deactivated/unactivated status. */
	private ZWCoinTable coinTable;

	/** The table to keep track of transactions. */
	private ZWTransactionTable transactionsTable;
	
	/** Table with market values of currencies */
	private ZWExchangeTable exchangeTable;
	
	private ZWAppDataTable appDataTable;

	private SecureRandom secureRandom;
	
	///////////////////////////////////////////////////////
	//////////  Boiler plate SQLite Table Stuff ///////////
	///////////////////////////////////////////////////////

	protected ZWSQLiteOpenHelper(Context context, String databasePath) {
		// If the database path is null then an in memory database is used
		super(context, databasePath, ZiftrCursorFactory.newFactory(), DATABASE_VERSION);

		this.sendingAddressesTable = new ZWSendingAddressesTable();
		this.receivingAddressesTable = new ZWReceivingAddressesTable();
		this.transactionOutputsTable = new ZWTransactionOutputsTable();
		this.coinTable = new ZWCoinTable();
		this.transactionsTable = new ZWTransactionTable();
		this.exchangeTable = new ZWExchangeTable();
		this.appDataTable = new ZWAppDataTable();
	}

	
	@Override
	public void onOpen(SQLiteDatabase db) {
		// Make table of coin activation statuses
		this.coinTable.create(db);
		
		//if we have any coins in our table load them into the ZWCoin class
		//otherwise load insert the default hardcoded coins into the table and load them into the ZWCoin class
		List<ZWCoin> coins = coinTable.getAllCoins(db);
		if(coins.size() == 0) {
			
			// fill in table with default coin, using UNACTIVATED as the status
			coins = ZWDefaultCoins.getDefaultCoins();
			for (ZWCoin coin : coins) {
				this.coinTable.insertDefault(coin, db);
			}
		}
		
		ZWCoin.loadCoins(coins);
		
		//Make exchange table
		this.exchangeTable.create(db);
		//Make misc table
		this.appDataTable.create(db);
	}
	
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		
		//do nothing, handled in onOpen (so that it's guaranteed to be there)
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
		if(oldVersion < newVersion) {
			//upgrading versions usually requires adding new tables for any active coins
			
			//note, don't put ZLog message here, fixed an issue where ZLog was trying to read the database
			//while being loaded by the class loader, which was happening here due to being called first
			//this caused a recursive getDatabase call, the issue has been fixed but best to not 
			//depend on any static classes here
			
			this.coinTable.create(db); //make sure coin table is up to date
			List<ZWCoin> coins = this.coinTable.getNotUnactiveCoins(db);
			for(ZWCoin coin : coins) {
				this.receivingAddressesTable.create(coin, db);
				this.sendingAddressesTable.create(coin, db);
				this.transactionsTable.create(coin, db);
				this.transactionOutputsTable.create(coin, db);
			}
			
		}
		
		
		if(oldVersion < 2) {
			//db version 1 stored user preferences and passwords hashes in SharedPreferences
			//that's all stored in the sqlite database now, so coppy important data over
			this.appDataTable.create(db);
			this.appDataTable.upgradeFromOldPreferences(db);
		}
		
	}

	
	
	public SecureRandom getSecureRandom() {
		if(this.secureRandom == null) {
			this.secureRandom = ZiftrUtils.createTrulySecureRandom();
			if(this.secureRandom == null) {
				String rngError = ZWApplication.getApplication().getString(R.string.zw_dialog_error_rng);
				ZiftrDialogManager.showSimpleAlert(rngError);
			}
		}
		
		return this.secureRandom;
	}
	

	//////////////////////////////////////////////////////////////////
	//////////  Interface for receiving addresses table  ///////////
	//////////////////////////////////////////////////////////////////

	private synchronized ZWAddressesTable getTable(boolean receivingNotSending) {
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
	protected ZWAddress createChangeAddress(ZWKeyCrypter crypter, ZWCoin coinId) {
		long time = System.currentTimeMillis() / 1000;
		return createReceivingAddress(crypter, coinId, "", 0, time, time, true, false);
	}


	/**
	 * this method creates a receiving (owned by the user) {@link ZWAddress} object and adds it 
	 * to the correct table within our database
	 * 
	 * @param crypter
	 * @param coinId
	 * @param note
	 * @param balance
	 * @param creation
	 * @param modified
	 * @param hidden
	 * @param spentFrom 
	 * @return
	 */
	protected synchronized ZWAddress createReceivingAddress(ZWKeyCrypter crypter, ZWCoin coinId, String note, 
			long balance, long creation, long modified, boolean hidden, boolean spentFrom) {
		
		try {
			ZWECKey ecKey = new ZWECKey(getSecureRandom());
			ZWAddress address = new ZWAddress(coinId, ecKey);
			address.getKey().setKeyCrypter(crypter);
			address.setLabel(note);
			address.setLastKnownBalance(balance);
			address.getKey().setCreationTimeSeconds(creation);
			address.setLastTimeModifiedSeconds(modified);
			address.setHidden(hidden);
			address.setSpentFrom(spentFrom);
			this.receivingAddressesTable.insert(address, this.getWritableDatabase());
	
			return address;
		}
		catch(Exception e) {
			ZLog.log("Exception creating new address: ", e);
			return null;
		}
	}

	protected synchronized EncryptionStatus changeEncryptionOfReceivingAddresses(ZWKeyCrypter oldCrypter, ZWKeyCrypter newCrypter) {

		EncryptionStatus returnStatus = EncryptionStatus.ERROR;
		
		SQLiteDatabase db = this.getWritableDatabase();
		db.beginTransaction();
		try {
			ZWReceivingAddressesTable receiveTable = this.receivingAddressesTable;
			
			for (ZWCoin coin : this.getNotUnactivatedCoins()) {
				EncryptionStatus status = receiveTable.recryptAllAddresses(coin, oldCrypter, newCrypter, db);
				if (status == EncryptionStatus.ALREADY_ENCRYPTED || status == EncryptionStatus.ERROR){
					return status;
				}
			}
			
			//make sure to do no database work after this
			db.setTransactionSuccessful();
		} 
		catch(Exception e) {
			ZLog.log("Exception trying to change encryption of receiving addresses: ", e);
			return EncryptionStatus.ERROR;
		}
		finally {
			// If setTransactionSuccessful is not called, then this will roll back to not do 
			// any of the changes since there was. If it was called then it finalizes everything.
			db.endTransaction();
		}
		
		return EncryptionStatus.SUCCESS;
	}
	
	protected synchronized boolean attemptDecrypt(ZWKeyCrypter crypter){
		SQLiteDatabase db = this.getWritableDatabase();
		ZWReceivingAddressesTable receiveTable = this.receivingAddressesTable;
		for (ZWCoin coin: this.getNotUnactivatedCoins()){
			if (!receiveTable.attemptDecryptAllAddresses(coin, crypter, db)){
				return false;
			}
		}
		return true;
	}

	
	/**
	 * As part of the U in CRUD, this method updates the addresses table in 
	 * the database for the given coin type and key and table boolean.
	 * 
	 * TODO is there a way to make sure that the new private key matches the old,
	 * even on encryption state changes?
	 * 
	 * @param address - the address to fully update 
	 */
	public synchronized void updateAddress(ZWAddress address) {
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
	public synchronized void updateAddressLabel(ZWCoin coin, String address, String newLabel, boolean receivingNotSending) {
		this.getTable(receivingNotSending).updateAddressLabel(coin, address, newLabel, getWritableDatabase());
	}

	/* 
	 * NOTE:
	 * Note that we don't give the method to delete an address, shouldn't ever
	 * delete an address just in case someone sends coins to an old address.
	 */



	/**
	 * As part of the C in CRUD, this method adds a sending (not owned by the user)
	 * address to the correct table within our database.
	 * 
	 * Default values will be used in this method for the balance and status.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @param note - The note that the user associates with this address.
	 * @throws ZWAddressFormatException 
	 */
	public ZWAddress createSendingAddress(ZWCoin coin, String addressString, String note) throws ZWAddressFormatException {
		long time = System.currentTimeMillis() / 1000;
		return createSendingAddress(coin, addressString, note, 0, time);
	}

	/**
	 * As part of the C in CRUD, this method adds a sending (not owned by the user)
	 * address to the correct table within our database.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @param key - The key to use.
	 * @throws ZWAddressFormatException 
	 */
	public synchronized ZWAddress createSendingAddress(ZWCoin coin, String addressString, String note, 
			long balance, long modified) throws ZWAddressFormatException {
		ZWAddress address = new ZWAddress(coin, addressString);
		address.setLabel(note);
		address.setLastKnownBalance(balance);
		//address.getKey().setCreationTimeSeconds(creation);
		address.setLastTimeModifiedSeconds(modified);
		long rowId = this.sendingAddressesTable.insert(address, this.getWritableDatabase());
		if(rowId == -1) {
			//this address already exists, so update it
			updateAddress(address);
		}
		return address;
	}


	///////////////////////////////////////////////////////////
	//////////  Interface for  transactions  ///////////
	///////////////////////////////////////////////////////////

	
	/**
	 * add the transaction to the local database,
	 * does an insert if the transaction is new otherwise updates the existing transaction
	 * @param transaction {@link ZWTransaction} to be inserted/updated in the database
	 */
	public synchronized void addTransaction(ZWTransaction transaction) {
		this.transactionsTable.addTransaction(transaction, getWritableDatabase());
	}


	public synchronized ZWTransaction getTransaction(ZWCoin coin, String transactionId) {
		if (transactionId == null) {
			return null;
		}

		List<String> hashes = new ArrayList<String>();
		hashes.add(transactionId);

		List<ZWTransaction> readAddresses = 
				this.transactionsTable.readTransactionsByHash(coin, hashes, this.getReadableDatabase());
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
	public synchronized List<ZWTransaction> getTransactionsByAddress(ZWCoin coin, String address) {
		return transactionsTable.readTransactionsByAddress(coin, address, getReadableDatabase());
	}

	/**
	 * Gets a list of txs from the database that have the hashes specified. 
	 * If hashes is null then it gets all addresses from the database.
	 * Orders them by most recent. 
	 * 
	 * @param coin
	 * @param addresses
	 * @param db
	 * @return
	 */
	public synchronized List<ZWTransaction> getTransactions(ZWCoin coin, List<String> hashes) {
		return transactionsTable.readTransactionsByHash(coin, hashes, getReadableDatabase());
	}

	public synchronized List<ZWTransaction> getAllTransactions(ZWCoin coin) {
		return this.transactionsTable.readTransactions(coin, null, getReadableDatabase());
	}


	protected synchronized void deleteTransaction(ZWTransaction tx) {
		this.transactionsTable.deleteTransaction(tx, getWritableDatabase());
	}
	
	public synchronized BigInteger getWalletBalance(ZWCoin coin) {
		
		return getWalletBalance(coin, BalanceType.ESTIMATED);
		
		//return ZWPreferences.getWarningUnconfirmed() ? getWalletBalance(coin, BalanceType.ESTIMATED) : getWalletBalance(coin, BalanceType.AVAILABLE);
	}

	public synchronized BigInteger getWalletBalance(ZWCoin coinId, BalanceType balanceType) {
		BigInteger balance = BigInteger.ZERO;
		List<ZWTransaction> txs = this.getAllTransactions(coinId);

		for (ZWTransaction tx : txs) {
			if (balanceType == BalanceType.AVAILABLE) {
				if (!tx.isPending() || tx.getAmount().compareTo(BigInteger.ZERO) == -1) {
					balance = balance.add(tx.getAmount());
				}
			} 
			else if (balanceType == BalanceType.ESTIMATED) {
				balance = balance.add(tx.getAmount());
			}
		}
		return balance;
	}

	public synchronized List<ZWTransaction> getPendingTransactions(ZWCoin coinId) {
		return transactionsTable.readPendingTransactions(coinId, getReadableDatabase());
	}

	public synchronized List<ZWTransaction> getConfirmedTransactions(ZWCoin coinId) {
		return transactionsTable.readConfirmedTransactions(coinId, getReadableDatabase());
	}

	public synchronized void updateTransactionNote(ZWTransaction tx) {
		this.transactionsTable.updateTransactionNote(tx, getWritableDatabase());
	}

	
	/**
	 * gets a list of Strings of the public addresses for all of the users addresses
	 * @param coin which coin to get addresses for
	 * @param includeEmpty should this list include addresses with no known balance (empty)
	 * @return a list of strings representing the public addresses
	 */
	public synchronized List<String> getAddressList(ZWCoin coin, boolean includeEmpty) {
		List<String> addresses;

		addresses = this.receivingAddressesTable.getAddressesList(coin, getWritableDatabase());
		
		return addresses;
	}
	
	
	/**
	 * gets an address from the 
	 * database for the given coin type and table boolean. Returns null if
	 * no such address is found
	 * 
	 * @param coin - The coin type to determine which table we use. 
	 * @param address - The list of 1xyz... (Base58) encoded address in the database.
	 * @param receivingNotSending - If true, uses receiving table. If false, sending table. 
	 */
	public synchronized ZWAddress getAddress(ZWCoin coin, String address, boolean receivingNotSending) {
		return this.getTable(receivingNotSending).getAddress(coin, address, getReadableDatabase());
	}

	
	/**
	 * gets a list of {@link ZWAddress} objects from the database based on a list of public address strings
	 * @param coin - The coin type to determine which table we use. 
	 * @param addresses - The list of 1xyz... (Base58) encoded address in the database. 
	 * @param receivingNotSending - If true, uses receiving table. If false, sending table. 
	 */
	public synchronized List<ZWAddress> getAddresses(ZWCoin coin, List<String> addresses, boolean receivingNotSending) {
		return this.getTable(receivingNotSending).getAddresses(coin, addresses, getReadableDatabase());
	}
	
	
	/**
	 * gets all {@link ZWAddress} from the receiving table for the given coin
	 * @param coin which coin to get addresses for
	 * @param includeSpentFrom should "unsafe" addresses (which have previously been spent from) be included
	 * @return a list of address objects
	 */
	public synchronized List<String> getHiddenAddressList(ZWCoin coin, boolean includeSpentFrom) {
		return this.receivingAddressesTable.getHiddenAddresses(coin, getReadableDatabase(), true);
	}

	
	/**
	 * gets all the visible (not hidden) addresses from the 
	 * database for the given coin type and table boolean.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @param receivingNotSending - If true, uses receiving table. If false, sending table. 
	 * @return List of {@link ZWAddress} objects. See {@link #getAddressList(ZWCoin, boolean)} for list of Address strings.
	 */
	public synchronized List<ZWAddress> getAllVisibleAddresses(ZWCoin coin) {
		return this.receivingAddressesTable.getAllVisibleAddresses(coin, getReadableDatabase());
	}
	
	
	public synchronized List<ZWAddress> getAllSendAddresses(ZWCoin coin) {
		return this.sendingAddressesTable.getAllAddresses(coin, getReadableDatabase());
	}
	
	
	public synchronized String getExchangeValue(String from, String to){
		return this.exchangeTable.getExchangeVal(from, to, getReadableDatabase());
	}
	
	public synchronized void upsertExchangeValue(String from, String to, String val){
		this.exchangeTable.upsert(from, to, val, getWritableDatabase());
	}

		
	public synchronized void updateCoin(ZWCoin coin){
		this.coinTable.upsertCoin(coin, getWritableDatabase());
	}
	
	public synchronized List<ZWCoin> getCoins() {
		return this.coinTable.getAllCoins(getReadableDatabase());
	}
	
	public synchronized ZWCoin getCoin(String coinSymbol){
		return this.coinTable.getCoin(coinSymbol, getReadableDatabase());
	}

	/**
	* Call this method to activate a coin type if it is not already activated.
	* If it has not been activated then this method will create the necessary tables 
	* for the coin type given and update the activated statuses table appropriately.
	* 
	* @param coinId
	*/
	public synchronized void activateCoin(ZWCoin coin) {
		// Safety check
		if (isCoinActivated(coin)) {
			return;
		}
		
		this.receivingAddressesTable.create(coin, this.getWritableDatabase());
		this.sendingAddressesTable.create(coin, this.getWritableDatabase());
		this.transactionsTable.create(coin, this.getWritableDatabase());
		this.transactionOutputsTable.create(coin, this.getWritableDatabase());
		
		// Update table to match activated status
		this.coinTable.activateCoin(coin, this.getWritableDatabase());
	}
	
	
	public synchronized boolean isCoinActivated(ZWCoin coin) {
		return this.coinTable.isCoinActivated(coin, getReadableDatabase());
	}
	
	
	public synchronized List<ZWCoin> getActivatedCoins() {
		return this.coinTable.getActiveCoins(getReadableDatabase());
	}
	
	public synchronized List<ZWCoin> getInactiveCoins(boolean includeTestnets) {
		return this.coinTable.getInactiveCoins(getReadableDatabase(), includeTestnets);
	}
	
	
	/**
	* gets a list of every coin that has been, or currently is, activated,
	* basically excluded coins that are "un-activated",
	* the current use case for this is changing passwords, where private keys 
	* that are in deactivated coin's databases must also be updated
	* @return a list of activated and deactivated coins
	*/
	public synchronized List<ZWCoin> getNotUnactivatedCoins() {
		return this.coinTable.getNotUnactiveCoins(getReadableDatabase());
	}
	
	
	public synchronized void deactivateCoin(ZWCoin coin) {
		this.coinTable.deactivateCoin(coin, getWritableDatabase());
	}
	
	//returns 1 if successful, -1 if error
	public synchronized int upsertAppDataVal(String key, String val){
		return this.appDataTable.upsert(key, val, getWritableDatabase());
	}
	
	//returns 1 if successful, -1 if error
	public synchronized int upsertAppDataVal(String key, boolean val){
		return this.appDataTable.upsert(key, val, getWritableDatabase());
	}
	
	public synchronized String getAppDataString(String key){
		return this.appDataTable.getStringFromKey(key, getReadableDatabase());
	}
	
	public synchronized Boolean getAppDataBoolean(String key){
		return this.appDataTable.getBooleanFromKey(key, getReadableDatabase());
	}

	/**
	 * Get any outputs that are from the given transaction and index. Most of the time should only return 1
	 * output, but with multisig it's possible there could be multiple output addresses matching a single index
	 * @param transactionId the transaction id these outputs are for
	 * @param outputIndex the index of outputs these outputs are for
	 * @return 
	 */
	public ArrayList<ZWTransactionOutput> getTransactionOutputs(ZWCoin coin, String transactionId, int outputIndex) {
		return this.transactionOutputsTable.getTransactionOutputs(coin, transactionId, outputIndex, this.getReadableDatabase());
	}

	/**
	 * 
	 * @param coin
	 * @param output
	 */
	public void addTransactionOutput(ZWCoin coin, ZWTransactionOutput output) {
		this.transactionOutputsTable.addTransactionOutput(coin, output, this.getWritableDatabase());
	}
	
	
	public void setSyncedBlockHeight(ZWCoin coin, long syncedHeight) {
		if(syncedHeight > 0) {
			coin.setSyncedHeight(syncedHeight);
			this.coinTable.setSyncedBlockHeight(coin, syncedHeight, this.getWritableDatabase());
		}
	}
	
	
	public void resetAllSyncedData() {
		
		SQLiteDatabase database = this.getWritableDatabase();
		database.beginTransaction();
		
		try {
			List<ZWCoin> coins = this.getNotUnactivatedCoins();
			
			for(ZWCoin coin : coins) {
				coin.setSyncedHeight(-1);
				this.coinTable.setSyncedBlockHeight(coin, -1, database);
				this.transactionsTable.deleteAll(coin, database);
				this.transactionOutputsTable.deleteAll(coin, database);
			}
			
			database.setTransactionSuccessful();
		}
		catch(Exception e) {
			
		}
		finally {
			database.endTransaction();
		}
		
	}
	
}



