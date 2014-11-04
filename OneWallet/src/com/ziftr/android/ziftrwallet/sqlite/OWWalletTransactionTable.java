package com.ziftr.android.ziftrwallet.sqlite;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.ziftr.android.ziftrwallet.crypto.OWSha256Hash;
import com.ziftr.android.ziftrwallet.crypto.OWTransaction;
import com.ziftr.android.ziftrwallet.util.OWCoin;
import com.ziftr.android.ziftrwallet.util.ZLog;

public class OWWalletTransactionTable extends OWCoinRelativeTable {
	
	/** The note column. This is for users to keep a string attached to an entry. */
	public static final String COLUMN_NOTE = "note";

	/** The hash column. Contains the hash of a transaction. This is unique per transaction (row). */
	public static final String COLUMN_HASH = "hash";

	/** 
	 * The amount column. This is the net amount that this transaction 
	 * caused the wallet balance to change by. So, if this is a sending transaction
	 * then the fee is included. If it is a receiving transaction, however, the fee is
	 * not included. 
	 * 
	 * Note: units here are always the atomic unit for the coin type. e.g. Satoshis 
	 * for bitcoin, etc.
	 */
	public static final String COLUMN_AMOUNT = "amount";

	/**
	 * The fee column. This is the fee we pay in sending transactions. In receiving
	 * transactions, this is the fee that the sender paid. May not be updated, possibly,
	 * if we are not the sender of this transaction. 
	 * 
	 * Note: units here are always the atomic unit for the coin type. e.g. Satoshis 
	 * for bitcoin, etc.
	 */
	public static final String COLUMN_FEE = "fee";

	/**
	 * The number of confirmations column. This is used to keep track of whether
	 * or not transactions are pending. When a transaction has at least 
	 * {@link OWCoin#getNumRecommendedConfirmations()} then it can be considered save.
	 */
	public static final String COLUMN_NUM_CONFIRMATIONS = "num_confirmations";

	/**
	 * Within the transactions' output addresses, we get all the addresses that are 
	 * relevent to us (known to us) and put them here.
	 */
	public static final String COLUMN_DISPLAY_ADDRESSES = "display_addresses";

	private OWAddressesTable sendingAddressesTable;
	private OWAddressesTable receivingAddressesTable;

	protected OWWalletTransactionTable(OWAddressesTable sendingAddressesTable,
			OWAddressesTable receivingAddressesTable) {
		this.sendingAddressesTable = sendingAddressesTable;
		this.receivingAddressesTable = receivingAddressesTable;

	}

	@Override
	protected String getTablePostfix() {
		return "_transactions";
	}

	@Override
	protected String getCreateTableString(OWCoin coinId) {
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE IF NOT EXISTS ").append(getTableName(coinId)).append(" (");
		sb.append(COLUMN_ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
		sb.append(COLUMN_HASH).append(" TEXT NOT NULL UNIQUE, ");
		sb.append(COLUMN_AMOUNT).append(" INTEGER NOT NULL, ");
		sb.append(COLUMN_FEE).append(" INTEGER, ");
		sb.append(COLUMN_NOTE).append(" TEXT, ");
		sb.append(COLUMN_NUM_CONFIRMATIONS).append(" INTEGER, ");
		sb.append(COLUMN_CREATION_TIMESTAMP).append(" INTEGER, ");
		sb.append(COLUMN_DISPLAY_ADDRESSES).append(" TEXT ); ");
		return sb.toString();
	}

	protected void insertTx(OWTransaction tx, SQLiteDatabase db) {
		db.insert(getTableName(tx.getCoinId()), null, txToContentValues(tx));
	}

	//	protected void updateNumConfirmationsOrInsert(OWTransaction tx, SQLiteDatabase db) {
	//		try {
	//			updateTransactionNumConfirmations(tx, db);
	//		} catch(OWNoTransactionFoundException ntfe) {
	//			this.insertTx(tx, db);
	//		}
	//	}

	protected OWTransaction readTransactionByHash(OWCoin coinId, String hash, SQLiteDatabase db) {
		if (hash == null) {
			return null;
		}

		List<String> hashes = new ArrayList<String>();
		hashes.add(hash);

		List<OWTransaction> readAddresses = readTransactionsByHash(coinId, hashes, db);
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
	protected List<OWTransaction> readTransactionsByAddress(OWCoin coinId, 
			String address, SQLiteDatabase db) {

		if (address != null && address.trim().isEmpty()) {
			return null;
		}

		StringBuilder where = new StringBuilder("");

		if (address != null) {
			where.append(COLUMN_DISPLAY_ADDRESSES);
			where.append(" LIKE '%,");
			where.append(address);
			where.append(",%'");
		}

		return this.readTransactions(coinId, where.toString(), db);
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
	protected List<OWTransaction> readTransactionsByHash(OWCoin coinId, 
			List<String> hashes, SQLiteDatabase db) {

		if (hashes != null && hashes.size() == 0) {
			return null;
		}

		StringBuilder where = new StringBuilder("");

		if (hashes != null) {
			where.append(COLUMN_HASH);
			where.append(" IN (");
			for (int i = 0; i < hashes.size(); i++) {
				where.append("'");
				where.append(hashes.get(i));
				where.append("'");
				if (i != (hashes.size() - 1)) {
					where.append(",");
				} else {
					where.append(")");
				}
			}
		}

		return this.readTransactions(coinId, where.toString(), db);
	}

	protected List<OWTransaction> readPendingTransactions(OWCoin coinId, SQLiteDatabase db) {

		StringBuilder where = new StringBuilder("");

		where.append(COLUMN_NUM_CONFIRMATIONS);
		where.append(" < ");
		int confirmationsRequired = coinId.getNumRecommendedConfirmations();
		where.append(DatabaseUtils.sqlEscapeString(String.valueOf(confirmationsRequired)));

		return this.readTransactions(coinId, where.toString(), db);
	}

	protected List<OWTransaction> readConfirmedTransactions(OWCoin coinId, SQLiteDatabase db) {

		StringBuilder where = new StringBuilder("");

		where.append(COLUMN_NUM_CONFIRMATIONS);
		where.append(" >= ");
		int confirmationsRequired = coinId.getNumRecommendedConfirmations();
		where.append(DatabaseUtils.sqlEscapeString(String.valueOf(confirmationsRequired)));

		return this.readTransactions(coinId, where.toString(), db);
	}

	/**
	 * Gets a list of txs from the database that have the hashes specified. 
	 * If where is empty then it does not add a WHERE modifier.
	 * Orders them by most recent.
	 * 
	 * @param coinId
	 * @param where
	 * @param db
	 * @return
	 */
	public List<OWTransaction> readTransactions(OWCoin coinId, String where, SQLiteDatabase db) {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM ");
		sb.append(getTableName(coinId));

		// If null, we take that to mean that they want all addresses
		if (where != null && !where.isEmpty()) {
			sb.append(" WHERE ");
			sb.append(where);

		}
		sb.append(" ORDER BY ");
		sb.append(COLUMN_CREATION_TIMESTAMP);
		sb.append(" DESC ");

		sb.append(";");
		String toQuery = sb.toString();
		Cursor c = db.rawQuery(toQuery, null);

		List<OWTransaction> newTxs = new ArrayList<OWTransaction>();

		// Move to first returns false if cursor is empty
		if (c.moveToFirst()) {
			// Recreate key from stored private key
			do {
				// TODO deal with encryption of private key
				newTxs.add(cursorToTransaction(coinId, c, db));
			} while (c.moveToNext());
		}

		// Make sure we close the cursor
		c.close();

		return newTxs;
	}

	protected void updateTransaction(OWTransaction tx, SQLiteDatabase db) throws OWNoTransactionFoundException {
		try {
			ContentValues values = txToContentValues(tx);
			int numUpdated = db.update(getTableName(tx.getCoinId()), values, 
					getWhereClaus(tx), null);
			if (numUpdated == 0) {
				// Will happen when we try to do an update but not in there. 
				// In this case an insert should be called. 
				throw new OWNoTransactionFoundException("Error: No such entry.");
			}
		} catch (SQLiteException sqle) {
			throw new OWNoTransactionFoundException("Error: No such entry.");
		}
	}

	protected void updateTransactionNumConfirmations(OWTransaction tx, SQLiteDatabase db) throws OWNoTransactionFoundException {
		try {
			ContentValues values = new ContentValues();
			values.put(COLUMN_NUM_CONFIRMATIONS, tx.getNumConfirmations());
			ZLog.log(getWhereClaus(tx));
			int numUpdated = db.update(getTableName(tx.getCoinId()), values, 
					getWhereClaus(tx), null);

			if (numUpdated == 0) {
				// Will happen when we try to do an update but not in there. 
				// In this case an insert should be called. 
				throw new OWNoTransactionFoundException("Error: No such entry.");
			}
		} catch (SQLiteException sqle) {
			throw new OWNoTransactionFoundException("Error: No such entry.");
		}
	}

	protected void updateTransactionNote(OWTransaction tx, SQLiteDatabase db) throws OWNoTransactionFoundException {
		try {
			ContentValues values = new ContentValues();
			values.put(COLUMN_NOTE, tx.getTxNote());
			ZLog.log(getWhereClaus(tx));
			int numUpdated = db.update(getTableName(tx.getCoinId()), values, 
					getWhereClaus(tx), null);

			if (numUpdated == 0) {
				// Will happen when we try to do an update but not in there. 
				// In this case an insert should be called. 
				throw new OWNoTransactionFoundException("Error: No such entry.");
			}
		} catch (SQLiteException sqle) {
			throw new OWNoTransactionFoundException("Error: No such entry.");
		}
	}

	protected void deleteTransaction(OWTransaction tx, SQLiteDatabase db) {

		db.delete(getTableName(tx.getCoinId()), getWhereClaus(tx), null);
	}

	private OWTransaction cursorToTransaction(OWCoin coinId, Cursor c,
			SQLiteDatabase db) {

		OWTransaction tx = new OWTransaction(coinId,
				c.getString(c.getColumnIndex(COLUMN_NOTE)), 
				c.getLong(c.getColumnIndex(COLUMN_CREATION_TIMESTAMP)), 
				new BigInteger(c.getString(c.getColumnIndex(COLUMN_AMOUNT))) );

		tx.setSha256Hash(new OWSha256Hash(c.getString(c.getColumnIndex(COLUMN_HASH))));

		tx.setTxFee(new BigInteger(c.getString(c.getColumnIndex(COLUMN_FEE))));

		OWAddressesTable correctTable = tx.getTxAmount().compareTo(BigInteger.ZERO) >= 0 ? 
				this.receivingAddressesTable : this.sendingAddressesTable;
		
		tx.setDisplayAddresses(this.parseDisplayAddresses(coinId, 
				c.getString(c.getColumnIndex(COLUMN_DISPLAY_ADDRESSES)), db, correctTable));

		tx.setNumConfirmations(c.getInt(c.getColumnIndex(COLUMN_NUM_CONFIRMATIONS)));

		return tx;

	}

	private ContentValues txToContentValues(OWTransaction tx) {
		ContentValues values = new ContentValues();

		// values.put(COLUMN_ID, tx.getId());
		values.put(COLUMN_HASH, tx.getSha256Hash().toString());
		values.put(COLUMN_AMOUNT, tx.getTxAmount().toString());
		values.put(COLUMN_FEE, tx.getTxFee().toString());
		values.put(COLUMN_NOTE, tx.getTxNote());
		values.put(COLUMN_CREATION_TIMESTAMP, tx.getTxTime());
		values.put(COLUMN_NUM_CONFIRMATIONS, tx.getNumConfirmations());
		values.put(COLUMN_DISPLAY_ADDRESSES, tx.getAddressAsCommaListString());

		return values;
	}

	/**
	 * When there are multiple display addresses, they have to be parsed.
	 * Entries in table look like ,1xy...,1jH...,18f..., (note the beginning
	 * and ending with commas). 
	 * 
	 * @param displayAddressesStr
	 */
	private List<String> parseDisplayAddresses(OWCoin coinId, 
			String displayAddressesStr, SQLiteDatabase db, OWAddressesTable addressTable) {
		if (displayAddressesStr == null || displayAddressesStr.trim().isEmpty()) {
			return null;
		} else {
			String[] addressArr = displayAddressesStr.split(",");
			List<String> displayAddresses = new ArrayList<String>();
			for (String a : addressArr) {
				displayAddresses.add(a);
			}
			return displayAddresses;
		}

	}

	private String getWhereClaus(OWTransaction tx) {
		StringBuilder sb = new StringBuilder();

		sb.append(COLUMN_HASH);
		sb.append(" = ");
		sb.append(DatabaseUtils.sqlEscapeString(tx.getSha256Hash().toString()));
		sb.append(" ");
		
		return sb.toString();
	}
	
	
}
