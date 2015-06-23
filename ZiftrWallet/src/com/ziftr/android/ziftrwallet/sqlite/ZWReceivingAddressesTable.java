/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.sqlite;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.spongycastle.crypto.CryptoException;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWEncryptedData;
import com.ziftr.android.ziftrwallet.crypto.ZWKeyCrypter;
import com.ziftr.android.ziftrwallet.crypto.ZWPrivateKey;
import com.ziftr.android.ziftrwallet.crypto.ZWPublicKey;
import com.ziftr.android.ziftrwallet.crypto.ZWReceivingAddress;
import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.util.CryptoUtils;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWReceivingAddressesTable extends ZWAddressesTable<ZWReceivingAddress> {

	/** The postfix that assists in making the names for the users addresses table. */
	private static final String TABLE_NAME_BASE = "_receiving_addresses";

	/** The private key encoded as a string using WIF. */
	public static final String COLUMN_PRIV_KEY = "priv_key";

	/** The public key, encoded as a string. */
	public static final String COLUMN_PUB_KEY = "pub_key";

	/** whether a receiving address is hidden from the user or not (for change addresses) */
	public static final String COLUMN_HIDDEN = "hidden";

	/** whether an address has spent money or not */
	public static final String COLUMN_SPENT_FROM = "spent_From";

	public static int VISIBLE_TO_USER = 0;
	public static int HIDDEN_FROM_USER = 1;

	public static int UNSPENT_FROM = 0;
	public static int SPENT_FROM = 1;

	public static enum reencryptionStatus{
		success,
		encrypted, //first address was already encrypted
		error //non-first address was already encrypted
	};

	@Override
	protected String getTableName(ZWCoin coin) {
		return coin.getSymbol() + TABLE_NAME_BASE;
	}


	@Override
	protected void createBaseTable(ZWCoin coin, SQLiteDatabase database) {

		String createSql = "CREATE TABLE IF NOT EXISTS " + getTableName(coin) + 
				" (" + COLUMN_PRIV_KEY + " TEXT UNQIUE NOT NULL, " + 
				COLUMN_PUB_KEY + " TEXT UNIQUE NOT NULL, " + 
				COLUMN_ADDRESS + " TEXT UNIQUE NOT NULL)";

		database.execSQL(createSql);
	}

	@Override
	protected void createTableColumns(ZWCoin coin, SQLiteDatabase database) {

		//create columns that all address tables use
		super.createTableColumns(coin, database);

		//add is_spent_from column
		addColumn(coin, COLUMN_SPENT_FROM, "INTEGER", database);

		//add is_hidden column
		addColumn(coin, COLUMN_HIDDEN, "INTEGER", database);
	}

	@Override
	protected ZWReceivingAddress cursorToAddress(ZWCoin coinId, Cursor c) throws ZWAddressFormatException {
		String dataInPrivColumn = c.getString(c.getColumnIndex(COLUMN_PRIV_KEY));
		byte[] pubKeyBytes = ZiftrUtils.hexStringToBytes(c.getString(c.getColumnIndex(COLUMN_PUB_KEY)));

		// If a constructor is used that doesn't pass the pubKeyBytes it recalculates them
		// here and that is pretty slow. Hence, whenever loading in addresses from the db, 
		// use a constructor where you can provide the pubKeyBytes.
		ZWPrivateKey newKey = getKeyFromStoredPrivData(dataInPrivColumn, pubKeyBytes);
		ZWReceivingAddress newAddress = new ZWReceivingAddress(coinId, newKey);

		// Only receiving addresses have these columns 
		newAddress.setCreationTimeSeconds(c.getLong(c.getColumnIndex(COLUMN_CREATION_TIMESTAMP)));
		newAddress.setSpentFrom(c.getInt(c.getColumnIndex(COLUMN_SPENT_FROM)) != 0);
		newAddress.setHidden(c.getInt(c.getColumnIndex(COLUMN_HIDDEN)) != 0);

		// All addresses ahve these columns
		newAddress.setLabel(c.getString(c.getColumnIndex(COLUMN_LABEL)));
		newAddress.setLastKnownBalance(c.getInt(c.getColumnIndex(COLUMN_BALANCE)));
		newAddress.setLastTimeModifiedSeconds(c.getLong(c.getColumnIndex(COLUMN_MODIFIED_TIMESTAMP)));
		return newAddress;
	}

	//called in wrapper with transaction begin and transaction end to ensure atomicity
	protected reencryptionStatus recryptAllAddresses(ZWCoin coin, ZWKeyCrypter oldCrypter, ZWKeyCrypter newCrypter, SQLiteDatabase db) throws CryptoException {

		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ");
		sb.append(COLUMN_PRIV_KEY);
		sb.append(" FROM ");
		sb.append(getTableName(coin));
		sb.append(";");
		Cursor c = db.rawQuery(sb.toString(), null);
		List<String> oldPrivKeysInDb = new ArrayList<String>();
		//read private keys
		if (c.moveToFirst()) {
			do {
				oldPrivKeysInDb.add(c.getString(c.getColumnIndex(COLUMN_PRIV_KEY)));
			} while (c.moveToNext());
		}
		c.close();
		//decrypt private keys
		for (int i = 0; i < oldPrivKeysInDb.size(); i++){
			String oldPrivKeyValInDb = oldPrivKeysInDb.get(i);
			String data = oldPrivKeyValInDb.substring(1);
			ZWPrivateKey key = null;

			boolean haveOldCrypter = oldCrypter != null;
			boolean dataIsEncrypted = CryptoUtils.isEncryptedIdentifier(oldPrivKeyValInDb.charAt(0));
			if (haveOldCrypter && dataIsEncrypted) {
				key = new ZWPrivateKey(new ZWEncryptedData(data), null, oldCrypter);
				key = key.decrypt();
			} else if (!haveOldCrypter && !dataIsEncrypted) {
				byte[] privBytes = ZiftrUtils.hexStringToBytes(data);
				key = new ZWPrivateKey(new BigInteger(1, privBytes));
			} else if (i == 0 && !haveOldCrypter && dataIsEncrypted) {
				// This can happen if the user upgrades from an only wallet and the password hash is erased.
				// The app will think that the user doesn't have a password, but the data in the database is still
				// encrypted. 
				// TODO In this case, we should make sure that the user has entered the right password and attempt 
				// to recrypt
				ZLog.log("ERROR tried to encrypt already encrypted first key possible recovery by entering old password.");
				return reencryptionStatus.encrypted;
			} else {
				//shouldn't happen
				ZLog.log("ERROR tried to encrypt already non-first encrypted keys, inconsistently encrypted keys uh oh");
				return reencryptionStatus.error;
			}

			ContentValues cv = new ContentValues();

			if (newCrypter != null) {
				key = key.encrypt(newCrypter);
			}
			cv.put(COLUMN_PRIV_KEY, this.getPrivDataForInsert(key));

			StringBuilder where = new StringBuilder();
			where.append(COLUMN_PRIV_KEY).append(" = ").append(DatabaseUtils.sqlEscapeString(oldPrivKeyValInDb));

			int numUpdated = db.update(getTableName(coin), cv, where.toString(), null);

			if (numUpdated != 1) {
				throw new CryptoException("Tables were not updated properly");
			}
		}
		return reencryptionStatus.success;
	}

	//	protected boolean attemptDecryptAllAddresses(ZWCoin coin, ZWKeyCrypter crypter, SQLiteDatabase db){
	//		StringBuilder sb = new StringBuilder();
	//		sb.append("SELECT ");
	//		sb.append(COLUMN_PRIV_KEY);
	//		sb.append(",");
	//		sb.append(COLUMN_PUB_KEY);
	//		sb.append(" FROM ");
	//		sb.append(getTableName(coin));
	//		sb.append(";");
	//		Cursor c = db.rawQuery(sb.toString(), null);
	//		//read private keys
	//		if (c.moveToFirst()) {
	//			do {
	//				String encryptedPrivKeyString = c.getString(c.getColumnIndex(COLUMN_PRIV_KEY));
	//				String storedPubKeyString = c.getString(c.getColumnIndex(COLUMN_PUB_KEY));
	//				if (encryptedPrivKeyString.charAt(0) != ZWKeyCrypter.PBE_AES_ENCRYPTION){
	//					ZLog.log("Error tried to decrypt already decrypted key...");
	//				} else {
	//					try {
	//						// Create public address from decrypted and see if it matches the stored one in the database
	//						// as a safety check
	//						ZWEncryptedData encryptedPrivKey = new ZWEncryptedData(encryptedPrivKeyString.substring(1));
	//						ZWPrivateKey key = new ZWPrivateKey(encryptedPrivKey, null, crypter);
	//						key = key.decrypt();
	//						if (!storedPubKeyString.equals(ZiftrUtils.bytesToHexString(key.getPub().getPubKeyBytes()))) {
	//							return false;
	//						}
	//					} catch(ZWKeyCrypterException e){
	//						ZLog.log("Crypter error " + e);
	//						return false;
	//					}
	//
	//				}
	//			} while (c.moveToNext());
	//		}
	//		return true;
	//	}

	public List<String> getHiddenAddresses(ZWCoin coin, SQLiteDatabase db, boolean includeSpentFrom){

		String sql = "SELECT " + COLUMN_ADDRESS + " FROM " + getTableName(coin) + " WHERE " + COLUMN_HIDDEN + " = " + HIDDEN_FROM_USER;
		if(!includeSpentFrom) {
			sql += " AND " + COLUMN_SPENT_FROM + " = " + UNSPENT_FROM;
		}

		Cursor cursor = db.rawQuery(sql, null);
		return this.readAddressList(coin, cursor);
	}


	/**
	 * @param dataInPrivColumn
	 * @param privDataWithoutEncryptionPrefix
	 * @param pubKeyBytes
	 * @param newKey
	 * @return
	 */
	private ZWPrivateKey getKeyFromStoredPrivData(String dataInPrivColumn, byte[] pubKeyBytes) {
		ZWPrivateKey newKey = null;
		String privDataWithoutEncryptionPrefix = dataInPrivColumn.substring(1);
		ZWPublicKey pubkey = new ZWPublicKey(pubKeyBytes);

		if (dataInPrivColumn.charAt(0) == ZWKeyCrypter.NO_ENCRYPTION) {
			byte[] privBytes = ZiftrUtils.hexStringToBytes(privDataWithoutEncryptionPrefix);
			newKey = new ZWPrivateKey(new BigInteger(1, privBytes), pubkey);
		} else if (dataInPrivColumn.charAt(0) == ZWKeyCrypter.PBE_AES_ENCRYPTION) {
			newKey = new ZWPrivateKey(new ZWEncryptedData(privDataWithoutEncryptionPrefix), pubkey, null);
		}

		if (newKey == null) {
			ZLog.log("Ruh roh, why couldn't we make a key? First char: " + dataInPrivColumn.charAt(0));
		}

		return newKey;
	}

	/**
	 * @param address
	 * @return
	 */
	private String getPrivDataForInsert(ZWPrivateKey key) {
		// TODO does this use the keyCrypter correctly/at all? 
		String privData;
		if (key.isEncrypted()) {
			privData = key.getEncryptedPrivateKey().toString();
		} else {
			privData = ZiftrUtils.bytesToHexString(key.getPrivKeyBytes());
		}
		return getPrivDataForInsert(privData, key.isEncrypted());
	}

	private String getPrivDataForInsert(String data, boolean encrypted) {
		return encrypted ? (ZWKeyCrypter.PBE_AES_ENCRYPTION + data) : (ZWKeyCrypter.NO_ENCRYPTION + data);
	}


	protected List<ZWReceivingAddress> getAllVisibleAddresses(ZWCoin coin, SQLiteDatabase db) {
		ZWAddressFilter<ZWReceivingAddress> filter = new ZWAddressFilter<ZWReceivingAddress>() {
			@Override public boolean include(ZWReceivingAddress address) { return true; }
			@Override public String where() { return COLUMN_HIDDEN + " = " + VISIBLE_TO_USER; }
		};
		return this.getAddresses(coin, filter, db);
	}

	protected ZWPrivateKey getKeyForAddress(ZWCoin coin, String address, SQLiteDatabase db) throws ZWAddressFormatException {
		String where = COLUMN_ADDRESS + " = " + address;
		Cursor cursor = db.query(getTableName(coin), null, where, null, null, null, null);
		return this.cursorToAddress(coin, cursor).getPriv();
	}


	@Override
	protected ContentValues addressToContentValues(ZWReceivingAddress address) {
		ZWPrivateKey privKey = address.getPriv();
		ContentValues values = super.addressToContentValues(address);
		values.put(COLUMN_PRIV_KEY, getPrivDataForInsert(privKey));
		values.put(COLUMN_PUB_KEY, ZiftrUtils.bytesToHexString(privKey.getPub().getPubKeyBytes()));
		values.put(COLUMN_CREATION_TIMESTAMP, address.getCreationTimeSeconds());
		values.put(COLUMN_HIDDEN, address.isHidden());
		values.put(COLUMN_SPENT_FROM, address.isSpentFrom());
		
		return values;
	}

}