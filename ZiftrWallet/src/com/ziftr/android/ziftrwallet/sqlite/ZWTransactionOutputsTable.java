package com.ziftr.android.ziftrwallet.sqlite;

import java.util.ArrayList;
import java.util.Locale;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWTransactionOutput;
import com.ziftr.android.ziftrwallet.util.ZLog;

public class ZWTransactionOutputsTable extends ZWCoinSpecificTable {
	
	private static final String TABLE_NAME_BASE = "_transaction_outputs";

	private static final String COLUMN_ADDRESS = "address";
	private static final String COLUMN_TRANSACTION_ID = "transaction_id";
	private static final String COLUMN_VOUT_INDEX = "vout_index";
	private static final String COLUMN_VALUE = "value";
	private static final String COLUMN_MULTISIG = "multisig";
	
	@Override
	protected void createBaseTable(ZWCoin coin, SQLiteDatabase database) {
		StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(getTableName(coin)).append(" (");
		sql.append(COLUMN_ADDRESS).append(" TEXT NOT NULL, ");
		sql.append(COLUMN_TRANSACTION_ID).append(" TEXT NOT NULL, ");
		sql.append(COLUMN_VOUT_INDEX).append(" INTEGER NOT NULL, ");
		sql.append(COLUMN_VALUE).append(" INTEGER NOT NULL)");
		
		database.execSQL(sql.toString());
	}

	@Override
	protected void createTableColumns(ZWCoin coin, SQLiteDatabase database) {
		this.addColumn(coin, COLUMN_MULTISIG, "INTEGER", database);
	}

	
	@Override
	protected String getTableName(ZWCoin coin) {
		return coin.getSymbol().toLowerCase(Locale.US) + TABLE_NAME_BASE;
	}

	
	protected void addTransactionOutput(ZWCoin coin, ZWTransactionOutput output, SQLiteDatabase database) {
		this.addTransactionOutput(coin, output.getAddress(), output.getTransactionId(), output.getIndex(), output.getValueString(), output.isMultiSig(), database);
	}
	
	protected void addTransactionOutput(ZWCoin coin, String address, String txId, int index, String value, boolean isMultiSig, SQLiteDatabase database) {
	
		ContentValues contentValues = new ContentValues(4);
		contentValues.put(COLUMN_ADDRESS, address);
		contentValues.put(COLUMN_TRANSACTION_ID, txId);
		contentValues.put(COLUMN_VOUT_INDEX, index);
		contentValues.put(COLUMN_VALUE, value);
		contentValues.put(COLUMN_MULTISIG, isMultiSig);
		
		String whereClause = COLUMN_TRANSACTION_ID + " = " + DatabaseUtils.sqlEscapeString(txId) + 
				" AND " + COLUMN_VOUT_INDEX + " = " + String.valueOf(index) +
				" AND " + COLUMN_ADDRESS + " = " + DatabaseUtils.sqlEscapeString(address);
		
		//since there are no unique constraints on this table, always attempt an update first
		//that will prevent having multiple rows with the same data
		int updateCount = database.update(getTableName(coin), contentValues, whereClause, null);
		if(updateCount == 0) {
			database.insert(getTableName(coin), null, contentValues);
		}
		else if(updateCount > 1) {
			ZLog.log("Error: Multiple transaction outputs with the same address, transaction id, and output index.");
		}
	}
	
	
	protected ArrayList<ZWTransactionOutput> getTransactionOutputs(ZWCoin coin, String txId, int index, SQLiteDatabase database) {
		
		ArrayList<ZWTransactionOutput> outputs = new ArrayList<ZWTransactionOutput>();
		
		String sql = "SELECT * FROM " + getTableName(coin) + " WHERE " + 
				COLUMN_TRANSACTION_ID + " = " + DatabaseUtils.sqlEscapeString(txId) + " AND " + 
				COLUMN_VOUT_INDEX + " = " + String.valueOf(index);
		
		Cursor cursor = database.rawQuery(sql, null);
		
		if(cursor.moveToFirst()) {
			do {
				ZWTransactionOutput output = this.createTransactionOutput(cursor);
				outputs.add(output);
			}
			while(cursor.moveToNext());
		}
		
		return outputs;
	}
	
	
	private ZWTransactionOutput createTransactionOutput(Cursor cursor) {
		
		String address = cursor.getString(cursor.getColumnIndex(COLUMN_ADDRESS));
		String transactionId = cursor.getString(cursor.getColumnIndex(COLUMN_TRANSACTION_ID));
		int index = cursor.getInt(cursor.getColumnIndex(COLUMN_VOUT_INDEX));
		String value = cursor.getString(cursor.getColumnIndex(COLUMN_VALUE));
		boolean isMultiSig = cursor.getInt(cursor.getColumnIndex(COLUMN_MULTISIG)) > 0;
		
		ZWTransactionOutput output = new ZWTransactionOutput(address, transactionId, index, value, isMultiSig);
		return output;
	}
	
	
}





