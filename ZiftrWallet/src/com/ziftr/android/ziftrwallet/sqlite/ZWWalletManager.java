/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */
package com.ziftr.android.ziftrwallet.sqlite;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.SecretKey;

import org.spongycastle.crypto.digests.SHA512Digest;
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.spongycastle.crypto.params.KeyParameter;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.common.base.Joiner;
import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWApplication;
import com.ziftr.android.ziftrwallet.ZWPreferences;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWEncryptedData;
import com.ziftr.android.ziftrwallet.crypto.ZWExtendedPrivateKey;
import com.ziftr.android.ziftrwallet.crypto.ZWExtendedPublicKey;
import com.ziftr.android.ziftrwallet.crypto.ZWHdAccount;
import com.ziftr.android.ziftrwallet.crypto.ZWKeyCrypter;
import com.ziftr.android.ziftrwallet.crypto.ZWNullCrypter;
import com.ziftr.android.ziftrwallet.crypto.ZWPbeAesCrypter;
import com.ziftr.android.ziftrwallet.crypto.ZWReceivingAddress;
import com.ziftr.android.ziftrwallet.crypto.ZWReceivingAddress.KeyType;
import com.ziftr.android.ziftrwallet.crypto.ZWSendingAddress;
import com.ziftr.android.ziftrwallet.crypto.ZWTransaction;
import com.ziftr.android.ziftrwallet.crypto.ZWTransactionOutput;
import com.ziftr.android.ziftrwallet.dialog.ZiftrDialogManager;
import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.sqlite.ZWReceivingAddressesTable.EncryptionStatus;
import com.ziftr.android.ziftrwallet.util.CryptoUtils;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

/** 
 * This class controls all of the wallets and is responsible
 * for setting them up and closing them down when the application exits.
 * 
 * TODO maybe this class should be a background fragment? Ask Justin. 
 * 
 * The goal is that this class will make it easy to switch between using bitocoinj
 * and switching to our API. All database access and bitcoinj access should go in here.
 * More specifically, database access should go in ZWSQLiteOpenHelper, and if the database
 * method is not quite correct, then it should be overridden in here to do somthing
 * with bitcoinj.
 * 
 * The database helper object. Open and closed in onCreate/onDestroy
 */
public class ZWWalletManager extends SQLiteOpenHelper {

	/** Database version */
	public static final int DATABASE_VERSION = 4;

	/** Following the standard, we name our wallet file as below. */
	public static final String DATABASE_NAME = "wallet.dat";

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
	protected ZWReceivingAddressesTable receivingAddressesTable;

	/** The helper for doing all things related to the sending addresses table. */
	protected ZWSendingAddressesTable sendingAddressesTable;

	protected ZWTransactionOutputsTable transactionOutputsTable;

	/** The table to keep track of coin activated/deactivated/unactivated status. */
	protected ZWCoinTable coinTable;

	/** The table to keep track of transactions. */
	protected ZWTransactionTable transactionsTable;

	/** Table with market values of currencies */
	protected ZWExchangeTable exchangeTable;

	/** Simple key value store for persistent preferences. */
	protected ZWAppDataTable appDataTable;

	/** Checkpoints in the HD wallet tree to make deriving a new address easy. */
	protected ZWAccountsTable accountsTable;

	///////////////////////////////////////////////////////
	//////////  Static Singleton Access Members ///////////
	///////////////////////////////////////////////////////

	/** The path where the database will be stored. */
	private static String databasePath;

	/** The single instance of the database helper that we use. */
	private static ZWWalletManager instance;

	private static boolean logging = true;

	private static void log(Object... messages) {
		if (logging) {
			ZLog.log(messages);
		}
	}

	public static synchronized ZWWalletManager getInstance() {
		if (instance == null) {
			try {
				Application applicationContext = ZWApplication.getApplication();
				// Here we build the path for the first time if have not yet already
				if (databasePath == null) {
					File externalDirectory = applicationContext.getExternalFilesDir(null);
					if (externalDirectory != null) {
						databasePath = new File(externalDirectory, DATABASE_NAME).getAbsolutePath();
					} else {
						// If we couldn't get the external directory the user is doing something weird with their sd card
						// Leaving databaseName as null will let the database exist in memory

						//TODO -at flag and use it to trigger UI to let user know they are running on an in memory database
						log("CANNOT ACCESS LOCAL STORAGE!");
					}
				}

				instance = new ZWWalletManager(applicationContext);
			} catch (NullPointerException e){
				ZLog.log("applicationContext was null");
				return null;
			}
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
		} 
		else {
			// If used right shouldn't happen because get instance should always
			// be called before every close call.
			log("instance was null when we called closeInstance...");
		}
		instance = null;
	}

	/**
	 * Make a new manager. This context should be cleared and then re-added when
	 * saving and bringing back the wallet manager. 
	 */
	private ZWWalletManager(Application context) {
		// super constructor opens up a connection with the database.
		// If the database path is null then an in memory database is used
		super(context, databasePath, ZiftrCursorFactory.newFactory(), DATABASE_VERSION);

		this.sendingAddressesTable = new ZWSendingAddressesTable();
		this.receivingAddressesTable = new ZWReceivingAddressesTable();
		this.transactionOutputsTable = new ZWTransactionOutputsTable();
		this.coinTable = new ZWCoinTable();
		this.transactionsTable = new ZWTransactionTable();
		this.exchangeTable = new ZWExchangeTable();
		this.appDataTable = new ZWAppDataTable();
		this.accountsTable = new ZWAccountsTable();
	}


	
	/////////////////////////////////////////////////
	//////////  Boiler plate SQLite stuff ///////////
	/////////////////////////////////////////////////

	@Override
	public void onOpen(SQLiteDatabase db) {
		// Make table of coin activation statuses
		this.coinTable.create(db);

		// If we have any coins in our table load them into the ZWCoin class
		List<ZWCoin> coins = coinTable.getAllCoins(db);
		ZWCoin.loadCoins(coins);

		// Make exchange table
		this.exchangeTable.create(db);
		// Make misc table
		this.appDataTable.create(db);
		// Make accounts table
		this.accountsTable.create(db);
	}


	@Override
	public void onCreate(SQLiteDatabase db) {

		//do nothing, handled in onOpen (so that it's guaranteed to be there)
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		if (oldVersion < newVersion) {
			//upgrading versions usually requires adding new tables for any active coins
			//note, don't put ZLog message here, fixed an issue where ZLog was trying to read the database
			//while being loaded by the class loader, which was happening here due to being called first
			//this caused a recursive getDatabase call, the issue has been fixed but best to not 
			//depend on any static classes here

			this.accountsTable.create(db);
			this.coinTable.create(db); //make sure coin table is up to date
			List<ZWCoin> coins = this.coinTable.getNotUnactiveCoins(db);
			for(ZWCoin coin : coins) {
				this.receivingAddressesTable.create(coin, db);
				this.sendingAddressesTable.create(coin, db);
				this.transactionsTable.create(coin, db);
				this.transactionOutputsTable.create(coin, db);
			}
		}


		if (oldVersion < 2) {
			//db version 1 stored user preferences and passwords hashes in SharedPreferences
			//the old version also stored user preferences and passwords hashes in SharedPreferences
			//that's all stored in the sqlite database now, so coppy important data over
			this.appDataTable.create(db);
			this.appDataTable.upgradeFromOldPreferences(db);
		}

	}


	//////////////////////////////////////////////////////////////////
	//////////  Interface for receiving addresses table  /////////////
	//////////////////////////////////////////////////////////////////

	@SuppressWarnings("rawtypes")
	private synchronized ZWAddressesTable getTable(boolean receivingNotSending) {
		return receivingNotSending ? this.receivingAddressesTable : this.sendingAddressesTable;
	}

	public ZWReceivingAddress createReceivingAddress(ZWCoin coinId, int account, boolean change) throws ZWAddressFormatException {
		return this.createReceivingAddress(coinId, account, change, "");
	}

	public ZWReceivingAddress createReceivingAddress(ZWCoin coinId, int account, boolean change, String note) throws ZWAddressFormatException {
		long time = System.currentTimeMillis() / 1000;
		ZWHdAccount hdAccount = this.accountsTable.getAccount(coinId.getSymbol(), account, getReadableDatabase());
		int nextUnused = this.receivingAddressesTable.nextUnusedIndex(coinId, change, account, getReadableDatabase());
		ZWExtendedPublicKey xpubkeyNewAddress = hdAccount.xpubkey.deriveChild("M/" + (change ? 1 : 0) + "/" + nextUnused);
		ZWReceivingAddress address = new ZWReceivingAddress(coinId, xpubkeyNewAddress, xpubkeyNewAddress.getPath());
		address.setLabel(note);
		address.setLastKnownBalance(0);
		address.setCreationTimeSeconds(time);
		address.setLastTimeModifiedSeconds(time);
		address.setSpentFrom(false);
		this.receivingAddressesTable.insert(address, this.getWritableDatabase());

		return address;
	}

	/**
	 * Caution! Only use this method if you know that the oldPassphrase is the string
	 * that has encrypted all of the private keys in the wallets and the salt is the correct
	 * salt that was used in the encryption. In addition, ensure that null is passed in for 
	 * the old passphrase if there was no previous encryption.
	 * 
	 * This could have very bad effects if the preconditions are not met!
	 * 
	 * @param curPassphrase - null if no encryption
	 * @param salt
	 */
	public synchronized EncryptionStatus changeEncryptionOfReceivingAddresses(String oldPassphrase, String newPassphrase) {
		ZWKeyCrypter oldCrypter = passwordToCrypter(oldPassphrase);
		ZWKeyCrypter newCrypter = passwordToCrypter(newPassphrase);

		String decryptedSeed = this.getDecryptedHdSeed(oldCrypter);
		if (ZWPreferences.getHdWalletSeed() == null){
			createNewHdWalletSeed(newPassphrase);
		} 
		else if (decryptedSeed == null) {
			ZLog.log("ERROR: Mismatch in oldCrypter and status of HD seed");
			return EncryptionStatus.ERROR;
		}
		else {
			String newSeedVal = newCrypter.encrypt(decryptedSeed).toStringWithEncryptionId();
			ZWPreferences.setHdWalletSeed(newSeedVal);
		}
		
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

	/**
	 * @param oldCrypter
	 * @return
	 */
	private String getDecryptedHdSeed(ZWKeyCrypter oldCrypter) {
		String hdSeed = ZWPreferences.getHdWalletSeed();
		if (hdSeed == null || hdSeed.isEmpty())
			return null;

		String decryptedSeed; 
		if (oldCrypter == null && hdSeed.charAt(0) == ZWKeyCrypter.NO_ENCRYPTION) {
			decryptedSeed = hdSeed.substring(1);
		} else if (oldCrypter != null && oldCrypter.getEncryptionIdentifier() == hdSeed.charAt(0)) {
			decryptedSeed = oldCrypter.decrypt(new ZWEncryptedData(hdSeed.charAt(0), hdSeed.substring(1)));
		} else {
			return null;
		}
		return decryptedSeed;
	}

	/**
	 * this is used to test passphrases when trying to recover from a state where a user
	 * tried to reencrypt already encrypted password and is entering his/her old passphrase
	 * to decrypt keys
	 */
	public synchronized boolean attemptDecrypt(String passphrase) {
		return this.changeEncryptionOfReceivingAddresses(passphrase, null) == EncryptionStatus.SUCCESS;
		//		ZWKeyCrypter crypter = this.passphraseToCrypter(passphrase);
		//		SQLiteDatabase db = this.getWritableDatabase();
		//		ZWReceivingAddressesTable receiveTable = this.receivingAddressesTable;
		//		for (ZWCoin coin: this.getNotUnactivatedCoins()){
		//			try {
		//				if (receiveTable.recryptAllAddresses(coin, crypter, null, db) != reencryptionStatus.success) {
		//					return false;
		//				}
		//			} catch (CryptoException e) {
		//				ZLog.log("ERROR Could not decrypt wallet!");
		//				return false;
		//			}
		//		}
		//		return true;
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
	@SuppressWarnings("unchecked")
	public synchronized void updateAddress(ZWSendingAddress address, boolean receivingNotSending) {
		this.getTable(receivingNotSending).updateAddress(address, getWritableDatabase());
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
	public ZWSendingAddress createSendingAddress(ZWCoin coin, String addressString, String note) throws ZWAddressFormatException {
		return createSendingAddress(coin, addressString, note, 0);
	}

	/**
	 * As part of the C in CRUD, this method adds a sending (not owned by the user)
	 * address to the correct table within our database.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @param pub - The key to use.
	 * @throws ZWAddressFormatException 
	 */
	public synchronized ZWSendingAddress createSendingAddress(ZWCoin coin, String addressString, String label, 
			long balance) throws ZWAddressFormatException {
		ZWSendingAddress address = new ZWSendingAddress(coin, addressString);
		address.setLabel(label);
		address.setLastKnownBalance(balance);
		address.setLastTimeModifiedSeconds(System.currentTimeMillis() / 1000);
		long rowId = this.sendingAddressesTable.insert(address, this.getWritableDatabase());
		if (rowId == -1) {
			// This address already exists, so update it
			this.updateAddress(address, false);
		}
		return address;
	}

	/**
	 * gets a list of Strings of the public addresses for all of the users addresses
	 * @param coin which coin to get addresses for
	 * @return a list of strings representing the public addresses
	 */
	@SuppressWarnings("unchecked")
	public synchronized List<String> getAddressList(ZWCoin coin, boolean receivingNotSending) {
		return this.getTable(receivingNotSending).getAddressesList(coin, getWritableDatabase());
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
	public synchronized ZWSendingAddress getAddress(ZWCoin coin, String address, boolean receivingNotSending) {
		return this.getTable(receivingNotSending).getAddress(coin, address, getReadableDatabase());
	}

	public synchronized ZWReceivingAddress getDecryptedReceivingAddress(ZWCoin coin, String address, String password) {
		ZWReceivingAddress addr = this.receivingAddressesTable.getAddress(coin, address, getReadableDatabase());
		ZWKeyCrypter crypter = passwordToCrypter(password);
		KeyType type = addr.getKeyType();
		switch(type) {
		case DERIVABLE_PATH:
			String hdSeed = getDecryptedHdSeed(crypter);
			if (hdSeed == null) {
				return null;
			}
			byte[] seed = ZiftrUtils.hexStringToBytes(hdSeed);
			ZWExtendedPrivateKey xprvkey = new ZWExtendedPrivateKey(seed);
			addr.deriveFromStoredPath(xprvkey);
			break;
		case ENCRYPTED:
			addr.decrypt(crypter);
			break;
		case EXTENDED_UNENCRYPTED:
			// Private key already available
			break;
		case STANDARD_UNENCRYPTED:
			// Private key already available
			break;
		}
		return addr;
	}

	/**
	 * gets a list of {@link ZWSendingAddress} objects from the database based on a list of public address strings
	 * @param coin - The coin type to determine which table we use. 
	 * @param addresses - The list of 1xyz... (Base58) encoded address in the database. 
	 * @param receivingNotSending - If true, uses receiving table. If false, sending table. 
	 */
	@SuppressWarnings("unchecked")
	public synchronized List<ZWSendingAddress> getAddresses(ZWCoin coin, List<String> addresses, boolean receivingNotSending) {
		return this.getTable(receivingNotSending).getAddresses(coin, addresses, getReadableDatabase());
	}

	/**
	 * gets all {@link ZWSendingAddress} from the receiving table for the given coin
	 * @param coin which coin to get addresses for
	 * @param includeSpentFrom should "unsafe" addresses (which have previously been spent from) be included
	 * @return a list of address objects
	 */
	public synchronized List<String> getHiddenAddressList(ZWCoin coin, boolean includeSpentFrom) {
		return this.receivingAddressesTable.getHiddenAddresses(coin, getReadableDatabase(), includeSpentFrom);
	}

	/**
	 * gets all the visible (not hidden) addresses from the 
	 * database for the given coin type and table boolean.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @param receivingNotSending - If true, uses receiving table. If false, sending table. 
	 * @return List of {@link ZWSendingAddress} objects. See {@link #getAddressList(ZWCoin, boolean)} for list of Address strings.
	 */
	public synchronized List<ZWReceivingAddress> getAllVisibleAddresses(ZWCoin coin) {
		return this.receivingAddressesTable.getAllVisibleAddresses(coin, getReadableDatabase());
	}


	public synchronized List<ZWSendingAddress> getAllSendAddresses(ZWCoin coin) {
		return this.sendingAddressesTable.getAllAddresses(coin, getReadableDatabase());
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
	public synchronized void activateCoin(ZWCoin coin, String password) {

		this.receivingAddressesTable.create(coin, this.getWritableDatabase());
		this.sendingAddressesTable.create(coin, this.getWritableDatabase());
		this.transactionsTable.create(coin, this.getWritableDatabase());
		this.transactionOutputsTable.create(coin, this.getWritableDatabase());

		// Update table to match activated status
		this.coinTable.activateCoin(coin, this.getWritableDatabase());

		this.activateHd(coin, password);
	}
	
	public synchronized void activateHd(ZWCoin coin, String password){
		// Make sure the seed data for the wallet exists, and read it
		ZWKeyCrypter crypter = passwordToCrypter(password);
		
		String seedStr = ZWPreferences.getHdWalletSeed();
		
		if (seedStr == null) {
			seedStr = createNewHdWalletSeed(password);
		} 
		
		byte[] seed = ZiftrUtils.hexStringToBytes(crypter.decrypt(new ZWEncryptedData(crypter.getEncryptionIdentifier(), seedStr.substring(1))));
		if (seed == null) {
			throw new RuntimeException("ERROR: was not able to decrypt seed data, this is bad!");
		}
	
		
		// Test if account exists before creating the new one to save time, as the derivation is CPU intensive
		if (!this.hasHdAccount(coin)) {
			ZWExtendedPrivateKey m = new ZWExtendedPrivateKey(seed);
			String path = "[m/44'/" + coin.getHdId() + "'/0']";
			ZWExtendedPublicKey mPub = (ZWExtendedPublicKey) m.deriveChild(path);
			ZWHdAccount account = new ZWHdAccount(coin.getSymbol(), mPub);
			this.accountsTable.insertAccount(account, this.getWritableDatabase());
		}
	}
	
	public synchronized boolean hasHdAccount(ZWCoin coin){
		return this.accountsTable.getAccount(coin.getSymbol(), 0, this.getWritableDatabase()) != null;
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

	
	public static synchronized String[] createHdWalletMnemonic(byte[] entropy) {
		
		int entropySize = Byte.SIZE * entropy.length; //bits in each byte * number of bytes
		
		if(entropySize < 128 || entropySize % 32 != 0) {
			//TODO error, must be at least 128 bits of entropy and must be a multiple of 32
		}
		
		byte[] sha256hash = CryptoUtils.Sha256Hash(entropy);
		int checksumSize = entropySize / 32;
		
		boolean[] checksumBits = ZiftrUtils.bytesToBits(sha256hash, true);
		boolean[] mnemonicBits = ZiftrUtils.bytesToBits(entropy, true);
		
		
		mnemonicBits = Arrays.copyOf(mnemonicBits, entropySize + checksumSize);
		
		//append the first bits of the checksum to the end of the original entropy, per bip-39
		for(int x = 0; x < checksumSize; x++) {
			mnemonicBits[entropySize + x] = checksumBits[x];
		}
		
		
		//create an array of indexes into the word list
		int[] mnemonicWordIndexes = new int[(entropySize + checksumSize) / 11]; 
		
		for(int x = 0; x < mnemonicWordIndexes.length; x++) {
			int startBitIndex = x * 11;
			int endBitIndex = startBitIndex + 11;
			
			boolean[] wordIndexBits = Arrays.copyOfRange(mnemonicBits, startBitIndex, endBitIndex);
			mnemonicWordIndexes[x] = ZiftrUtils.bitsToInt(wordIndexBits, true);
		}
		
		ArrayList<String> wordList = getWordList();
		
		String[] mnemonicSentence = new String[mnemonicWordIndexes.length];
		for(int x = 0; x < mnemonicSentence.length; x++) {
			mnemonicSentence[x] = wordList.get(mnemonicWordIndexes[x]);
		}
		
		ZLog.log("Generated mnemonic sentence: ", Arrays.toString(mnemonicSentence));
		
		return mnemonicSentence;
	}
	
	
	public static ArrayList<String> getWordList() {
		
		ArrayList<String> wordList = new ArrayList<String>(2048);
		
		try {
			InputStream wordlistInputStream = ZWApplication.getApplication().getResources().openRawResource(R.raw.english);
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(wordlistInputStream));
			String word = reader.readLine();
			while(word != null) {
				wordList.add(word.trim());
				word = reader.readLine();
			}
			
			reader.close();
		}
		catch(Exception e) {
			ZLog.log("Exception loading bip39 word list: ", e);
		}
		
		
		return wordList;
	}
	
	
	
	public static byte[] generateHdSeed(String[] mnemonicSentence, String seedPassword) {
	
		if(seedPassword == null) {
			seedPassword = "";
		}
		else {
			//we need to make sure the password is ok for this particular version of Android
			//see: http://android-developers.blogspot.com/2013/12/changes-to-secretkeyfactory-api-in.html
			//best solution is to just not allow users to use non-ascii characters in their password for 4.3 and earlier versions
			//this way there's no issues of possible compatibility with other bip 39 hd wallet implementations
			
			//note: using spongycastle now so we don't need to worry about this
			/**
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
				for(char c : seedPassword.toCharArray()) {
					if((c & 0x7F) != c) {
						return null;
					}
				}
			}
			**/
		}
		
		
		String mnemonicString = Joiner.on(" ").join(mnemonicSentence);
		mnemonicString = Normalizer.normalize(mnemonicString, Normalizer.Form.NFKD);
		
		String salt = "mnemonic" + seedPassword;
		salt = Normalizer.normalize(salt, Normalizer.Form.NFKD);
		

		int keySize = 512;
		int iterations = 2048;
		
		try {
			byte[] mnemonicBytes = mnemonicString.getBytes("UTF-8");
			byte[] saltBytes = salt.getBytes("UTF-8");
			
			
			PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA512Digest());
			generator.init(mnemonicBytes, saltBytes, iterations);
			
			KeyParameter keyParams = (KeyParameter) generator.generateDerivedParameters(keySize);
			byte[] derivedKeyBytes = keyParams.getKey();
			
			return derivedKeyBytes;
		} 
		catch (Exception e) {
			ZLog.log("Excpetion generating HD Seed: ", e);
		}
		
		
		return null;
	}
	
	
	
	
	private static synchronized String createNewHdWalletSeed(String password) {
		//we need to re-check the current one, incase multiple threads try to do this at the same time
		String seedString = ZWPreferences.getHdWalletSeed();
		if (seedString == null) {
			byte[] seed = new byte[32];
			getSecureRandom().nextBytes(seed);
			
			ZWKeyCrypter crypter = passwordToCrypter(password);
			
			seedString = crypter.encrypt(ZiftrUtils.bytesToHexString(seed)).toString();
			ZWPreferences.setHdWalletSeed(seedString);
		}
		
		return seedString;
	}

	
	/**
	 * @param password
	 * @return
	 */
	private static ZWKeyCrypter passwordToCrypter(String password) {
		ZWKeyCrypter crypter = null;
		if (password != null && password.length() > 0) {
			try {
				SecretKey secretKey = ZWPbeAesCrypter.generateSecretKey(password, ZWPreferences.getSalt());
				crypter = new ZWPbeAesCrypter(secretKey);
			}
			catch(Exception e) {
				ZLog.log("Exception creating key crypter: ", e);
			}
		} else {
			crypter = new ZWNullCrypter();
		}
		return crypter;
	}
	
	
	
	private static SecureRandom getSecureRandom() {
		SecureRandom random = ZiftrUtils.createTrulySecureRandom();
		if(random == null) {
			String rngError = ZWApplication.getApplication().getString(R.string.zw_dialog_error_rng);
			ZiftrDialogManager.showSimpleAlert(rngError);
		}

		return random;
	}

	

	@Override
	protected void finalize() throws Throwable {
		try {
			this.close();
		}
		catch(Exception e) {
			//just making sure the database is properly closed
		}
	}

}


