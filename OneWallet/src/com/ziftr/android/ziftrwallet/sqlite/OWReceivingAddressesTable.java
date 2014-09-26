package com.ziftr.android.ziftrwallet.sqlite;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

import org.spongycastle.crypto.CryptoException;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.ziftr.android.ziftrwallet.crypto.OWAddress;
import com.ziftr.android.ziftrwallet.crypto.OWCrypter;
import com.ziftr.android.ziftrwallet.crypto.OWECKey;
import com.ziftr.android.ziftrwallet.exceptions.OWAddressFormatException;
import com.ziftr.android.ziftrwallet.util.Base58;
import com.ziftr.android.ziftrwallet.util.OWCoin;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class OWReceivingAddressesTable extends OWAddressesTable {

	/** The postfix that assists in making the names for the users addresses table. */
	private static final String TABLE_POSTFIX = "_receiving_addresses";

	/** The private key encoded as a string using WIF. */
	public static final String COLUMN_PRIV_KEY = "priv_key";

	/** The public key, encoded as a string. */
	public static final String COLUMN_PUB_KEY = "pub_key";

	@Override
	protected String getTablePostfix() {
		return TABLE_POSTFIX;
	}

	@Override
	protected String getCreateTableString(OWCoin coinId) {
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE IF NOT EXISTS ").append(getTableName(coinId)).append(" (");
		sb.append(COLUMN_ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
		sb.append(COLUMN_PRIV_KEY).append(" TEXT NOT NULL, ");
		sb.append(COLUMN_PUB_KEY).append(" TEXT NOT NULL, ");
		sb.append(COLUMN_ADDRESS).append(" TEXT NOT NULL, ");
		sb.append(COLUMN_NOTE).append(" TEXT, ");
		sb.append(COLUMN_BALANCE).append(" INTEGER, ");
		sb.append(COLUMN_CREATION_TIMESTAMP).append(" INTEGER, ");
		sb.append(COLUMN_MODIFIED_TIMESTAMP).append(" INTEGER );");
		return sb.toString();
	}

	@Override
	protected OWAddress cursorToAddress(OWCoin coinId, Cursor c) throws OWAddressFormatException {
		// TODO deal with encryption
		// TODO make this not use WIF but just use hex
		byte[] wifPrivKeyBytes = Base58.decodeChecked(c.getString(c.getColumnIndex(COLUMN_PRIV_KEY)));
		byte[] privKeyBytes = ZiftrUtils.stripVersionAndChecksum(wifPrivKeyBytes);
		byte[] pubKeyBytes = ZiftrUtils.hexStringToBytes(c.getString(c.getColumnIndex(COLUMN_PUB_KEY)));
		// If a constructor is used that doesn't pass the pubKeyBytes it recalculates them
		// here and that is pretty slow
		OWECKey newKey = new OWECKey(privKeyBytes, pubKeyBytes);
		OWAddress newAddress = new OWAddress(coinId, newKey);

		// Reset all the address' parameters for use elsewhere
		newAddress.setId(c.getLong(c.getColumnIndex(COLUMN_ID)));
		newAddress.setNote(c.getString(c.getColumnIndex(COLUMN_NOTE)));
		newAddress.setLastKnownBalance(c.getInt(c.getColumnIndex(COLUMN_BALANCE)));
		newAddress.getKey().setCreationTimeSeconds(c.getLong(c.getColumnIndex(COLUMN_CREATION_TIMESTAMP)));
		newAddress.setLastTimeModifiedSeconds(c.getLong(c.getColumnIndex(COLUMN_MODIFIED_TIMESTAMP)));
		return newAddress;
	}

	@Override
	protected ContentValues addressToContentValues(OWAddress address) {
		// Can assume here that address.getKey() doesn't return null.

		ContentValues values = new ContentValues();
		// TODO can we do this as a blob?
		// TODO deal with encryption
		values.put(COLUMN_PRIV_KEY, Base58.encode(
				address.getCoinId().getPrivKeyPrefix(), address.getKey().getPrivKeyBytesForAddressEncoding()));
		values.put(COLUMN_PUB_KEY, ZiftrUtils.bytesToHexString(address.getKey().getPubKey()));
		values.put(COLUMN_ADDRESS, Base58.encode(address.getCoinId().getPubKeyHashPrefix(), address.getHash160()));
		values.put(COLUMN_NOTE, address.getLabel());
		values.put(COLUMN_BALANCE, address.getLastKnownBalance());
		values.put(COLUMN_CREATION_TIMESTAMP, address.getKey().getCreationTimeSeconds());
		values.put(COLUMN_MODIFIED_TIMESTAMP, address.getLastTimeModifiedSeconds());

		// The id will be generated upon insertion.
		return values;
	}

	protected void recryptAllAddresses(OWCoin coin, SecretKey oldKey, SecretKey newKey, SQLiteDatabase db) throws CryptoException {

		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ");
		sb.append(COLUMN_ID);
		sb.append(", ");
		sb.append(COLUMN_PRIV_KEY);
		sb.append(" FROM ");
		sb.append(getTableName(coin));
		sb.append(";");
		Cursor c = db.rawQuery(sb.toString(), null);

		List<String> oldPrivKeys = new ArrayList<String>();
		if (c.moveToFirst()) {
			do {
				oldPrivKeys.add(c.getString(c.getColumnIndex(COLUMN_PRIV_KEY)));
			} while (c.moveToNext());
		}
		c.close();

		for (String oldPrivKey : oldPrivKeys) {
			String decrypted = oldPrivKey;
			if (oldKey != null) {
				decrypted = OWCrypter.decrypt(oldKey, oldPrivKey);
			}
			String newlyEncryptedPrivateKey = OWCrypter.encrypt(newKey, decrypted);

			ContentValues cv = new ContentValues();
			cv.put(COLUMN_PRIV_KEY, newlyEncryptedPrivateKey);

			StringBuilder where = new StringBuilder();
			where.append(COLUMN_PRIV_KEY).append(" = ").append(DatabaseUtils.sqlEscapeString(oldPrivKey));

			int numUpdated = db.update(getTableName(coin), cv, where.toString(), null);
			
			if (numUpdated != 1) {
				throw new CryptoException("Tables were not updated properly");
			}
		}

	}

}