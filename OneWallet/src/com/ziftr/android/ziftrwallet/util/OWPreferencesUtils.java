package com.ziftr.android.ziftrwallet.util;

import java.util.Arrays;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.ziftr.android.ziftrwallet.crypto.OWKeyCrypterException;
import com.ziftr.android.ziftrwallet.crypto.OWPbeAesCrypter;

public abstract class OWPreferencesUtils {

	/** For putting the passphrase into a bundle and/or intent extras. */
	public static final String BUNDLE_PASSPHRASE_KEY = "bundle_passphrase_key";
	
	/** For putting the passphrase into a bundle and/or intent extras. */
	public static final String BUNDLE_NAME_KEY = "bundle_name_key";

	/** Used for getting the preferences */
	public static final String PREFERENCES_FILE_NAME = "ziftrWALLET_Prefs";
	
	/** The key for getting the passphrase hash from the preferences. */
	public static final String PREFS_PASSPHRASE_KEY = "ow_passphrase_key_1";

	/** The key for getting the name of the user from the preferences. */
	public static final String PREFS_USER_NAME_KEY = "ow_name_key";
	
	/** The key to get and save the salt as used by the specific user of the application. */
	static final String PREFS_SALT_KEY = "ziftrWALLET_salt_key";
	

	/** So we can skip the welcome screen and save the boolean. */
	public final static String PASSPHRASE_DISABLED_KEY = "ow_disabled_passphrase_key";

	/** So we can save the boolean from settings, whether fees are edititable or not. */
	public final static String EDITABLE_FEES_KEY = "ow_editable_fees_key";

	/** Save the selected Fiat Currency of user*/
	public final static String FIAT_CURRENCY_KEY = "ow_fiat_currency_key";

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
	public static boolean inputHashMatchesStoredHash(Context a, byte[] inputHash) {
		byte[] storedHash = getStoredPassphraseHash(a);
		if (storedHash != null && inputHash != null) {
			return Arrays.equals(storedHash, inputHash); 
		} else if (storedHash == null && inputHash == null) {
			return true;
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

	public static boolean getFeesAreEditable(Context a) {
		SharedPreferences prefs = getPrefs(a);
		return prefs.getBoolean(EDITABLE_FEES_KEY, false);
	}

	public static void setFeesAreEditable(Context a, boolean feesAreEditable) {
		SharedPreferences prefs = getPrefs(a);
		Editor editor = prefs.edit();
		editor.putBoolean(EDITABLE_FEES_KEY, feesAreEditable);
		editor.commit();
	}

	public static boolean getPassphraseDisabled(Context a) {
		SharedPreferences prefs = getPrefs(a);
		return prefs.getBoolean(PASSPHRASE_DISABLED_KEY, false);
	}

	public static void setPassphraseDisabled(Context a, boolean isDisabled) {
		SharedPreferences prefs = getPrefs(a);
		Editor editor = prefs.edit();
		editor.putBoolean(PASSPHRASE_DISABLED_KEY, isDisabled);
		editor.commit();
	}

	public static OWFiat getFiatCurrency(Context a){
		SharedPreferences prefs = getPrefs(a);
		return OWFiat.valueOf(prefs.getString(FIAT_CURRENCY_KEY, "US Dollars"));

	}

	public static void setFiatCurrency(Context a, String fiatSelected){
		SharedPreferences prefs = getPrefs(a);
		Editor editor = prefs.edit();
		editor.putString(FIAT_CURRENCY_KEY, fiatSelected);
		editor.commit();
	}
	
	public static String getUserName(Context a) {
		SharedPreferences prefs = getPrefs(a);
		return prefs.getString(PREFS_USER_NAME_KEY, null);
	}
	
	public static boolean userHasSetName(Context a) {
		return getUserName(a) != null;
	}
	
	public static void setUserName(Context a, String userName) {
		SharedPreferences prefs = getPrefs(a);
		Editor editor = prefs.edit();
		editor.putString(PREFS_USER_NAME_KEY, userName);
		editor.commit();
	}

	public static SharedPreferences getPrefs(Context a) {
		return a.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
	}

}