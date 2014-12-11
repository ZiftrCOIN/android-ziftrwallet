package com.ziftr.android.ziftrwallet.sqlite;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWTransaction;
import com.ziftr.android.ziftrwallet.util.ZLog;

public class ZWTransactionTable extends ZWCoinRelativeTable {

	private static final String TABLE_NAME_BASE = "_transactions";
	
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
	 * {@link ZWCoin#getNumRecommendedConfirmations()} then it can be considered save.
	 */
	public static final String COLUMN_NUM_CONFIRMATIONS = "num_confirmations";

	/**
	 * Within the transactions' output addresses, we get all the addresses that are 
	 * relevent to us (known to us) and put them here.
	 */
	public static final String COLUMN_DISPLAY_ADDRESSES = "display_addresses";

	protected ZWTransactionTable() {

	}

	
	@Override
	protected String getTableName(ZWCoin coin) {
		return coin.getShortTitle() + TABLE_NAME_BASE;
	}

	
	@Override
	protected void createBaseTable(ZWCoin coin, SQLiteDatabase database) {
		String createSql = "CREATE TABLE IF NOT EXISTS " + getTableName(coin) + 
				" (" + COLUMN_HASH + " TEXT UNIQUE NOT NULL)";
		database.execSQL(createSql);
	}


	@Override
	protected void createTableColumns(ZWCoin coin, SQLiteDatabase database) {
		
		//add transaction amount column
		addColumn(coin, COLUMN_AMOUNT, "INTEGER", database);
		
		//add fee paid column
		addColumn(coin, COLUMN_FEE, "INTEGER", database);
		
		//add transaction note column
		addColumn(coin, COLUMN_NOTE, "TEXT", database);
		
		//add column for number of confirmations that transaction has
		addColumn(coin, COLUMN_NUM_CONFIRMATIONS, "INTEGER", database);
		
		//add column for which address to display
		addColumn(coin, COLUMN_DISPLAY_ADDRESSES, "TEXT", database);
		
		//add transaction creation timestamp
		addColumn(coin, COLUMN_CREATION_TIMESTAMP, "INTEGER", database);
	}

	protected void addTransaction(ZWTransaction transaction, SQLiteDatabase db) {
		
		StringBuilder sqlBuilder = new StringBuilder("INSERT OR IGNORE INTO ");
		sqlBuilder.append(getTableName(transaction.getCoin())).append(" (").append(COLUMN_HASH).append(", ");
		sqlBuilder.append(COLUMN_AMOUNT).append(", ");
		sqlBuilder.append(COLUMN_FEE).append(", ");
		sqlBuilder.append(COLUMN_NOTE).append(", ");
		sqlBuilder.append(COLUMN_CREATION_TIMESTAMP).append(", ");
		sqlBuilder.append(COLUMN_DISPLAY_ADDRESSES);
		sqlBuilder.append(") VALUES (").append(DatabaseUtils.sqlEscapeString(transaction.getSha256Hash())).append(", ");
		sqlBuilder.append(transaction.getAmount().toString()).append(", ");
		sqlBuilder.append(transaction.getFee().toString()).append(", ");
		sqlBuilder.append(DatabaseUtils.sqlEscapeString(transaction.getNote())).append(", ");
		sqlBuilder.append(transaction.getTxTime()).append(", ");
		sqlBuilder.append(DatabaseUtils.sqlEscapeString(transaction.getAddressAsCommaListString()));
		sqlBuilder.append(")");
		//first do an insert, ignoring any errors, with just primary key
		db.execSQL(sqlBuilder.toString());
		
		
		sqlBuilder = new StringBuilder("UPDATE ").append(getTableName(transaction.getCoin())).append(" SET ");
		sqlBuilder.append(COLUMN_NUM_CONFIRMATIONS).append(" = ").append(transaction.getConfirmationCount());
		sqlBuilder.append(" WHERE ").append(COLUMN_HASH).append(" = ").append(DatabaseUtils.sqlEscapeString(transaction.getSha256Hash())).append(";");
		
		ZLog.log("Updating transaction: ", sqlBuilder.toString());
		
		//now update the transaction with all the data
		int updated = db.compileStatement(sqlBuilder.toString()).executeUpdateDelete();
		if(updated == 0) {
			ZLog.log("Error updating transaction: ", sqlBuilder.toString());
		}
		
	}


	protected ZWTransaction readTransactionByHash(ZWCoin coinId, String hash, SQLiteDatabase db) {
		if (hash == null) {
			return null;
		}

		List<String> hashes = new ArrayList<String>();
		hashes.add(hash);

		List<ZWTransaction> readAddresses = readTransactionsByHash(coinId, hashes, db);
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
	protected List<ZWTransaction> readTransactionsByAddress(ZWCoin coinId, 
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
	protected List<ZWTransaction> readTransactionsByHash(ZWCoin coinId, 
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

	protected List<ZWTransaction> readPendingTransactions(ZWCoin coinId, SQLiteDatabase db) {

		StringBuilder where = new StringBuilder("");

		where.append(COLUMN_NUM_CONFIRMATIONS);
		where.append(" < ");
		int confirmationsRequired = coinId.getNumRecommendedConfirmations();
		where.append(DatabaseUtils.sqlEscapeString(String.valueOf(confirmationsRequired)));

		return this.readTransactions(coinId, where.toString(), db);
	}

	protected List<ZWTransaction> readConfirmedTransactions(ZWCoin coinId, SQLiteDatabase db) {

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
	public List<ZWTransaction> readTransactions(ZWCoin coinId, String where, SQLiteDatabase db) {
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

		List<ZWTransaction> newTxs = new ArrayList<ZWTransaction>();

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

	protected void updateTransaction(ZWTransaction tx, SQLiteDatabase db) throws ZWNoTransactionFoundException {
		try {
			ContentValues values = txToContentValues(tx);
			int numUpdated = db.update(getTableName(tx.getCoin()), values, 
					getWhereClaus(tx), null);
			if (numUpdated == 0) {
				// Will happen when we try to do an update but not in there. 
				// In this case an insert should be called. 
				throw new ZWNoTransactionFoundException("Error: No such entry.");
			}
		} catch (SQLiteException sqle) {
			throw new ZWNoTransactionFoundException("Error: No such entry.");
		}
	}

	protected void updateTransactionNumConfirmations(ZWTransaction tx, SQLiteDatabase db) throws ZWNoTransactionFoundException {
		try {
			ContentValues values = new ContentValues();
			values.put(COLUMN_NUM_CONFIRMATIONS, tx.getConfirmationCount());
			ZLog.log("Updating transaction confirmations with:  " + getWhereClaus(tx));
			int numUpdated = db.update(getTableName(tx.getCoin()), values, 
					getWhereClaus(tx), null);

			if (numUpdated == 0) {
				// Will happen when we try to do an update but not in there. 
				// In this case an insert should be called. 
				throw new ZWNoTransactionFoundException("Error: No such entry.");
			}
		} catch (SQLiteException sqle) {
			throw new ZWNoTransactionFoundException("Error: No such entry.");
		}
	}

	protected void updateTransactionNote(ZWTransaction tx, SQLiteDatabase db) throws ZWNoTransactionFoundException {
		try {
			ContentValues values = new ContentValues();
			values.put(COLUMN_NOTE, tx.getNote());
			ZLog.log("Updating transaction note with:  " + getWhereClaus(tx));
			int numUpdated = db.update(getTableName(tx.getCoin()), values, 
					getWhereClaus(tx), null);

			if (numUpdated == 0) {
				// Will happen when we try to do an update but not in there. 
				// In this case an insert should be called. 
				throw new ZWNoTransactionFoundException("Error: No such entry.");
			}
		} catch (SQLiteException sqle) {
			throw new ZWNoTransactionFoundException("Error: No such entry.");
		}
	}

	protected void deleteTransaction(ZWTransaction tx, SQLiteDatabase db) {

		db.delete(getTableName(tx.getCoin()), getWhereClaus(tx), null);
	}

	private ZWTransaction cursorToTransaction(ZWCoin coin, Cursor c, SQLiteDatabase db) {
		
		String hash = c.getString(c.getColumnIndex(COLUMN_HASH));
		ZWTransaction tx = new ZWTransaction(coin, hash);

		tx.setNote(c.getString(c.getColumnIndex(COLUMN_NOTE)));
		tx.setTxTime(c.getLong(c.getColumnIndex(COLUMN_CREATION_TIMESTAMP)));
		tx.setAmount(new BigInteger(c.getString(c.getColumnIndex(COLUMN_AMOUNT))));
		tx.setFee(new BigInteger(c.getString(c.getColumnIndex(COLUMN_FEE))));
		tx.setConfirmationCount(c.getInt(c.getColumnIndex(COLUMN_NUM_CONFIRMATIONS)));

		String addressesString = c.getString(c.getColumnIndex(COLUMN_DISPLAY_ADDRESSES));
		List<String> addressList = new ArrayList<String>();
		String[] addressArray = addressesString.split(",");
		for (String address : addressArray) {
			addressList.add(address);
		}
		tx.setDisplayAddresses(addressList);
		
		return tx;

	}

	private ContentValues txToContentValues(ZWTransaction tx) {
		ContentValues values = new ContentValues();

		values.put(COLUMN_HASH, tx.getSha256Hash());
		values.put(COLUMN_AMOUNT, tx.getAmount().toString());
		values.put(COLUMN_FEE, tx.getFee().toString());
		values.put(COLUMN_NOTE, tx.getNote());
		values.put(COLUMN_CREATION_TIMESTAMP, tx.getTxTime());
		values.put(COLUMN_NUM_CONFIRMATIONS, tx.getConfirmationCount());
		values.put(COLUMN_DISPLAY_ADDRESSES, tx.getAddressAsCommaListString());

		return values;
	}


	private String getWhereClaus(ZWTransaction tx) {
		StringBuilder sb = new StringBuilder();

		sb.append(COLUMN_HASH);
		sb.append(" = ");
		sb.append(DatabaseUtils.sqlEscapeString(tx.getSha256Hash()));
		sb.append(" ");
		
		return sb.toString();
	}
	
	
}
