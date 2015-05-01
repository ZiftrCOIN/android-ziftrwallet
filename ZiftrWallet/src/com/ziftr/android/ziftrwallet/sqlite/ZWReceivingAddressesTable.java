package com.ziftr.android.ziftrwallet.sqlite;

import java.util.ArrayList;
import java.util.List;

import org.spongycastle.crypto.CryptoException;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.ziftrwallet.crypto.ZWAddress;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWECKey;
import com.ziftr.android.ziftrwallet.crypto.ZWEncryptedData;
import com.ziftr.android.ziftrwallet.crypto.ZWKeyCrypter;
import com.ziftr.android.ziftrwallet.crypto.ZWKeyCrypterException;
import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.util.Base58;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWReceivingAddressesTable extends ZWAddressesTable {

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
	protected ZWAddress cursorToAddress(ZWCoin coinId, Cursor c) throws ZWAddressFormatException {

		String dataInPrivColumn = c.getString(c.getColumnIndex(COLUMN_PRIV_KEY));
		byte[] pubKeyBytes = ZiftrUtils.hexStringToBytes(c.getString(c.getColumnIndex(COLUMN_PUB_KEY)));
		
		// If a constructor is used that doesn't pass the pubKeyBytes it recalculates them
		// here and that is pretty slow. Hence, whenever loading in addresses from the db, 
		// use a constructor where you can provide the pubKeyBytes.
		ZWECKey newKey = getKeyFromStoredPrivData(dataInPrivColumn, pubKeyBytes);
		ZWAddress newAddress = new ZWAddress(coinId, newKey);

		// Reset all the address' parameters for use elsewhere
		newAddress.setLabel(c.getString(c.getColumnIndex(COLUMN_LABEL)));
		newAddress.setLastKnownBalance(c.getInt(c.getColumnIndex(COLUMN_BALANCE)));
		newAddress.getKey().setCreationTimeSeconds(c.getLong(c.getColumnIndex(COLUMN_CREATION_TIMESTAMP)));
		newAddress.setLastTimeModifiedSeconds(c.getLong(c.getColumnIndex(COLUMN_MODIFIED_TIMESTAMP)));
		newAddress.setHidden(c.getInt(c.getColumnIndex(COLUMN_HIDDEN)) != 0);
		newAddress.setSpentFrom(c.getInt(c.getColumnIndex(COLUMN_SPENT_FROM)) != 0);
		return newAddress;
	}

	@Override
	protected ContentValues addressToContentValues(ZWAddress address) {
		// Can assume here that address.getKey() doesn't return null.

		ContentValues values = new ContentValues();
		
		values.put(COLUMN_PRIV_KEY, getPrivDataForInsert(address.getKey()));
		values.put(COLUMN_PUB_KEY, ZiftrUtils.bytesToHexString(address.getKey().getPubKey()));
		values.put(COLUMN_ADDRESS, Base58.encode(address.getCoin().getPubKeyHashPrefix(), address.getHash160()));
		values.put(COLUMN_LABEL, address.getLabel());
		values.put(COLUMN_BALANCE, address.getLastKnownBalance());
		values.put(COLUMN_CREATION_TIMESTAMP, address.getKey().getCreationTimeSeconds());
		values.put(COLUMN_MODIFIED_TIMESTAMP, address.getLastTimeModifiedSeconds());
		values.put(COLUMN_HIDDEN, address.isHidden());
		values.put(COLUMN_SPENT_FROM, address.isSpentFrom());
		// The id will be generated upon insertion.
		return values;
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
		for (int i=0; i<oldPrivKeysInDb.size(); i++){
			String oldPrivKeyValInDb = oldPrivKeysInDb.get(i);
			String decrypted = oldPrivKeyValInDb.substring(1);
			if (oldCrypter != null) {
				decrypted = oldCrypter.decrypt(new ZWEncryptedData(oldPrivKeyValInDb.substring(1)));
			} else if (oldPrivKeyValInDb.charAt(0) == ZWKeyCrypter.PBE_AES_ENCRYPTION){
				if (i==0){
					ZLog.log("ERROR tried to encrypt already encrypted first key possible recovery by entering old password.");
					return reencryptionStatus.encrypted;
				} else {
				//shouldn't happen
					ZLog.log("ERROR tried to encrypt already non-first encrypted keys, inconsistently encrypted keys uh oh");
				}
				return reencryptionStatus.error;
			}
			
			ContentValues cv = new ContentValues();
			
			if(newCrypter != null) {
				ZWEncryptedData newlyEncryptedPrivateKey = newCrypter.encrypt(decrypted);
				cv.put(COLUMN_PRIV_KEY, this.getPrivDataForInsert(newlyEncryptedPrivateKey.toString(), true));
			}
			else {
				//just remove encryption if there's no new crypter
				cv.put(COLUMN_PRIV_KEY, this.getPrivDataForInsert(decrypted, false));
			}

			StringBuilder where = new StringBuilder();
			where.append(COLUMN_PRIV_KEY).append(" = ").append(DatabaseUtils.sqlEscapeString(oldPrivKeyValInDb));

			int numUpdated = db.update(getTableName(coin), cv, where.toString(), null);

			if (numUpdated != 1) {
				throw new CryptoException("Tables were not updated properly");
			}
		}
		return reencryptionStatus.success;
	}
	
	protected boolean attemptDecryptAllAddresses(ZWCoin coin, ZWKeyCrypter crypter, SQLiteDatabase db){
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ");
		sb.append(COLUMN_PRIV_KEY);
		sb.append(",");
		sb.append(COLUMN_PUB_KEY);
		sb.append(" FROM ");
		sb.append(getTableName(coin));
		sb.append(";");
		Cursor c = db.rawQuery(sb.toString(), null);
		//read private keys
		if (c.moveToFirst()) {
			do {
				String encryptedPrivKey = c.getString(c.getColumnIndex(COLUMN_PRIV_KEY));
				String storedPubKey = c.getString(c.getColumnIndex(COLUMN_PUB_KEY));
				if (encryptedPrivKey.charAt(0) != ZWKeyCrypter.PBE_AES_ENCRYPTION){
					ZLog.log("Error tried to decrypt already decrypted key...");
				} else {
					encryptedPrivKey = encryptedPrivKey.substring(1);
					try {
						byte[] decryptedPrivKey = crypter.decryptToBytes(new ZWEncryptedData(encryptedPrivKey));
						//create public address from decrypted and see if it matches the stored one in the database
						ZWECKey key = new ZWECKey(decryptedPrivKey, null);
						if (!storedPubKey.equals(ZiftrUtils.bytesToHexString(key.getPubKey()))){
							return false;
						}
					} catch(ZWKeyCrypterException e){
						ZLog.log("Crypter error " + e);
						return false;
					}

				}
			} while (c.moveToNext());
		}
		return true;
	}
	
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
	private ZWECKey getKeyFromStoredPrivData(String dataInPrivColumn, byte[] pubKeyBytes) {
		ZWECKey newKey = null;
		String privDataWithoutEncryptionPrefix = dataInPrivColumn.substring(1);

		if (dataInPrivColumn.charAt(0) == ZWKeyCrypter.NO_ENCRYPTION) {
			newKey = new ZWECKey(ZiftrUtils.hexStringToBytes(privDataWithoutEncryptionPrefix), pubKeyBytes);
		} else if (dataInPrivColumn.charAt(0) == ZWKeyCrypter.PBE_AES_ENCRYPTION) {
			newKey = new ZWECKey(new ZWEncryptedData(privDataWithoutEncryptionPrefix), pubKeyBytes, null);
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
	private String getPrivDataForInsert(ZWECKey key) {
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


	protected List<ZWAddress> getAllVisibleAddresses(ZWCoin coin, SQLiteDatabase db) {
		String sql = "SELECT * FROM " + getTableName(coin) + " WHERE " + COLUMN_HIDDEN + " = " + VISIBLE_TO_USER;
		
		Cursor cursor = db.rawQuery(sql, null);
		List<ZWAddress> addresses = this.readAddresses(coin, cursor);
		
		return addresses;
	}
	
	
}