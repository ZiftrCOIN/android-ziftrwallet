package com.ziftr.android.ziftrwallet;

import java.util.Arrays;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.ziftr.android.ziftrwallet.crypto.OWPbeAesCrypter;
import com.ziftr.android.ziftrwallet.crypto.OWKeyCrypterException;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public abstract class OWPreferencesUtils {

	/** For putting the passphrase into a bundle and/or intent extras. */
	static final String BUNDLE_PASSPHRASE_KEY = "bundle_passphrase_key";

	/** Used for getting the preferences */
	static final String PREFERENCES_NAME = "ziftrWALLET_Prefs";

	/** The key for getting the passphrase hash from the preferences. */
	static final String PREFS_PASSPHRASE_KEY = "ow_passphrase_key_1";

	/** The key to get and save the salt as used by the specific user of the application. */
	static final String PREFS_SALT_KEY = "ziftrWALLET_salt_key";

	/**
	 * Gets the stored hash of the users passphrase, if there is one.
	 * 
	 * @return the stored hash of the users passphrase, if there is one.
	 */
	static byte[] getStoredPassphraseHash(Context a) {
		// Get the preferences
		SharedPreferences prefs = getPrefs(a);
		// Get the passphrase hash
		String storedPassphrase = prefs.getString(PREFS_PASSPHRASE_KEY, null);
		// If it's not null, convert it back to a byte array
		byte[] storedHash = (storedPassphrase == null) ?
				null : ZiftrUtils.hexStringToBytes(storedPassphrase);
		// Return the result
		return storedHash;
	}

	/**
	 * Get a boolean describing whether or not the user 
	 * has entered a passphrase before.
	 * 
	 * @return a boolean describing whether or not the user 
	 * has entered a passphrase before.
	 */
	public static boolean userHasPassphrase(Context a) {
		return getStoredPassphraseHash(a) != null;
	}

	/**
	 * Tells if the given hash matches the stored passphrase
	 * hash.
	 * 
	 * @param inputHash - the hash to check agains
	 * @return as above
	 */
	static boolean inputHashMatchesStoredHash(Context a, byte[] inputHash) {
		byte[] storedHash = getStoredPassphraseHash(a);
		if (storedHash != null && inputHash != null) {
			return Arrays.equals(storedHash, inputHash); 
		} else {
			ZLog.log("Error, An hash value was null that shouldn't have been.");
			return false;
		}
	}

	public static String getSalt(Context a) {
		SharedPreferences prefs = getPrefs(a);
		String salt = prefs.getString(PREFS_SALT_KEY, null);
		if (salt != null && !salt.isEmpty()) {
			return salt;
		}

		try {
			salt = OWPbeAesCrypter.generateSalt();
		} catch (OWKeyCrypterException e) {
			ZLog.log("error: " + e.getMessage());
			return null;
		}

		Editor editor = prefs.edit();
		editor.putString(PREFS_SALT_KEY, salt);
		editor.commit();

		return salt;
	}

	/**
	 * @param a
	 * @return
	 */
	private static SharedPreferences getPrefs(Context a) {
		return a.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
	}

}