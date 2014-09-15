package com.ziftr.android.onewallet.sqlite;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.crypto.OWAddress;
import com.ziftr.android.onewallet.crypto.OWSha256Hash;
import com.ziftr.android.onewallet.crypto.OWTransaction;
import com.ziftr.android.onewallet.fragment.accounts.OWWalletTransactionListAdapter;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWFiat;
import com.ziftr.android.onewallet.util.ZLog;

public class OWWalletTransactionTable extends OWCoinRelativeTable {

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
		sb.append(COLUMN_CREATION_TIMESTAMP).append(" INTEGER, ");
		sb.append(COLUMN_DISPLAY_ADDRESSES).append(" TEXT ); ");
		return sb.toString();
	}

	protected void insert(OWTransaction tx, SQLiteDatabase db) {
		long insertId = db.insert(getTableName(tx.getCoinId()), 
				null, txToContentValues(tx));
		tx.setId(insertId);
	}

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
		coinId.getNumRecommendedConfirmations();

		return this.readTransactions(coinId, where.toString(), db);
	}
	
	protected List<OWTransaction> readConfirmedTransactions(OWCoin coinId, SQLiteDatabase db) {

		StringBuilder where = new StringBuilder("");

		where.append(COLUMN_NUM_CONFIRMATIONS);
		where.append(" >= ");
		coinId.getNumRecommendedConfirmations();

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
	private List<OWTransaction> readTransactions(OWCoin coinId, String where, SQLiteDatabase db) {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM ");
		sb.append(getTableName(coinId));

		// If null, we take that to mean that they want all addresses
		if (!where.isEmpty()) {
			sb.append(" WHERE ");
			sb.append(where);

		}
		sb.append(" ORDER BY ");
		sb.append(COLUMN_CREATION_TIMESTAMP);
		sb.append(" DESC ");

		sb.append(";");
		String toQuery = sb.toString();
		ZLog.log("Query: " + toQuery);
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

	protected List<OWTransaction> readAllTransactions(OWCoin coinId, SQLiteDatabase db) {
		return this.readTransactions(coinId, null, db);
	}

	protected void updateTransaction(OWTransaction tx, SQLiteDatabase db) {
		if (tx.getId() == -1) {
			// Shouldn't happen
			throw new RuntimeException("Error: id has not been set.");
		}
		ContentValues values = txToContentValues(tx);
		db.update(getTableName(tx.getCoinId()), values, COLUMN_ID + " = " + tx.getId(), null);
	}
	
	protected void deleteTransaction(OWTransaction tx, SQLiteDatabase db) {
		if (tx.getId() == -1) {
			// Shouldn't happen
			throw new RuntimeException("Error: id has not been set.");
		}

		StringBuilder where = new StringBuilder();
		where.append(" WHERE ");
		where.append(COLUMN_ID);
		where.append(" = ");
		where.append(tx.getId());

		db.delete(getTableName(tx.getCoinId()), where.toString(), null);
	}

	private OWTransaction cursorToTransaction(OWCoin coinId, Cursor c,
			SQLiteDatabase db) {

		// TODO get from settings
		OWFiat fiatType = OWFiat.USD;

		OWTransaction tx = new OWTransaction(coinId, fiatType, 
				c.getString(c.getColumnIndex(COLUMN_NOTE)), 
				c.getLong(c.getColumnIndex(COLUMN_CREATION_TIMESTAMP)), 
				new BigInteger(c.getString(c.getColumnIndex(COLUMN_AMOUNT))), 
				OWWalletTransactionListAdapter.Type.TRANSACTION,
				R.layout.accounts_wallet_tx_list_item);

		tx.setId(c.getLong(c.getColumnIndex(COLUMN_ID)));

		tx.setSha256Hash(new OWSha256Hash(c.getString(c.getColumnIndex(COLUMN_HASH))));

		tx.setTxFee(new BigInteger(c.getString(c.getColumnIndex(COLUMN_FEE))));

		OWAddressesTable correctTable = tx.getTxAmount().compareTo(BigInteger.ZERO) >= 0 ? 
				this.receivingAddressesTable : this.sendingAddressesTable;

		tx.setDisplayAddresses(this.parseDisplayAddresses(coinId, 
				c.getString(c.getColumnIndex(COLUMN_DISPLAY_ADDRESSES)), db, correctTable));

		return tx;

	}

	private ContentValues txToContentValues(OWTransaction tx) {
		ContentValues values = new ContentValues();

		values.put(COLUMN_ID, tx.getId());
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
	private List<OWAddress> parseDisplayAddresses(OWCoin coinId, 
			String displayAddressesStr, SQLiteDatabase db, OWAddressesTable addressTable) {
		if (displayAddressesStr == null || displayAddressesStr.trim().isEmpty()) {
			return null;
		} else {
			String[] addressArr = displayAddressesStr.split(",");
			List<String> displayAddresses = new ArrayList<String>();
			for (String a : addressArr) {
				displayAddresses.add(a);
			}
			return addressTable.readAddresses(coinId, displayAddresses, db);
		}

	}

}