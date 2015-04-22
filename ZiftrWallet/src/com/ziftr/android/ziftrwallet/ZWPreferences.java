package com.ziftr.android.ziftrwallet;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;

import com.ziftr.android.ziftrwallet.crypto.ZWFiat;
import com.ziftr.android.ziftrwallet.crypto.ZWPbeAesCrypter;
import com.ziftr.android.ziftrwallet.sqlite.ZWAppDataTable;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public abstract class ZWPreferences {
	public static final String ZWOPTIONS_KEY = "ziftrwallet_stored_options";

	/** For putting the passphrase into a bundle and/or intent extras. */
	public static final String BUNDLE_PASSPHRASE_KEY = "bundle_passphrase_key";
	
	/** For putting the passphrase into a bundle and/or intent extras. */
	public static final String BUNDLE_NAME_KEY = "bundle_name_key";

	/** Used for getting the preferences */
	public static final String PREFERENCES_FILE_NAME = "ziftrWALLET_Prefs";
	
	/** The key for getting the passphrase hash from the preferences. */
	public static final String PREFS_PASSPHRASE_KEY = "zw_passphrase_key_1";
	
	/** key for getting and saving salt in the options file */
	static final String PREFS_SALT_KEY = "ziftrWALLET_salt_key";

	/** The key for getting the name of the user from the preferences. */
	public static final String PREFS_USER_NAME_KEY = "zw_name_key";
	
	/** skip the screen for name */
	public final static String NAME_DISABLED_KEY = "zw_disabled_name_key";
	
	/** So we can skip the welcome screen and save the boolean. */
	public final static String PASSPHRASE_WARNING_KEY = "zw_disabled_passphrase_key";

	/** So we can save the boolean from settings, whether fees are edititable or not. */
	public final static String EDITABLE_FEES_KEY = "zw_editable_fees_key";

	/** Save the selected Fiat Currency of user*/
	public final static String FIAT_CURRENCY_KEY = "zw_fiat_currency_key";
	
	/** Save whether we are in debug mode or not*/
	public final static String DEBUG_SETTING_KEY = "debug_setting_key";
	
	/** Save whether we are in logging to filee or not*/
	public final static String LOG_TO_FILE_KEY = "log_to_file_key";
	
	/** Save whether we can spend unconfirmed txns or not*/
	public final static String SPENDABLE_MEMPOOL_KEY = "spendable_mempool_key";
	
	/** save passphrase temporarily */
	private static WeakReference<String> cachedPassphrase;
	
	/** if true we are currently in a state where we should extend the timer and keep the cachedpassphrase*/
	public static boolean usingCachedPass = false;

	/** interval in milliseconds to check if we can clear the cached passphrase */
	private static final int clearPassInterval = 300000;
	

	/**
	 * Gets the stored hash of the users passphrase, if there is one.
	 * 
	 * @return the stored hash of the users passphrase, if there is one.
	 */
	static byte[] getStoredPassphraseHash() {
		String storedPassphrase = ZWWalletManager.getInstance().getAppDataString(ZWAppDataTable.PASS_KEY);
		
		if (storedPassphrase == null || storedPassphrase.isEmpty()){
			//check old preferences since none in db
			storedPassphrase = getPrefString(PREFS_PASSPHRASE_KEY);
			if (storedPassphrase == null || storedPassphrase.isEmpty()){
				return null;
			} else {
				ZWWalletManager.getInstance().upsertAppDataVal(ZWAppDataTable.PASS_KEY, storedPassphrase);
				return ZiftrUtils.hexStringToBytes(storedPassphrase);
			}
		} else {
			// If it's not null, convert it back to a byte array
			return ZiftrUtils.hexStringToBytes(storedPassphrase);
		}
	}
	
	static int setStoredPassphraseHash(String saltedHash) {
		return ZWWalletManager.getInstance().upsertAppDataVal(ZWAppDataTable.PASS_KEY, saltedHash);
	}

	/**
	 * Get a boolean describing whether or not the user 
	 * has entered a passphrase before.
	 * 
	 * @return a boolean describing whether or not the user 
	 * has entered a passphrase before.
	 */
	public static boolean userHasPassphrase() {
		return getStoredPassphraseHash() != null;
	}

	/**
	 * Tells if the given hash matches the stored passphrase
	 * hash.
	 * 
	 * @param inputHash - the hash to check agains
	 * @return as above
	 */
	public static boolean inputHashMatchesStoredHash(byte[] inputHash) {
		byte[] storedHash = getStoredPassphraseHash();
		if (storedHash != null && inputHash != null) {
			return Arrays.equals(storedHash, inputHash); 
		} else if (storedHash == null && inputHash == null) {
			return true;
		} else {
			ZLog.log("Error, An hash value was null that shouldn't have been.");
			return false;
		}
	}

	public static boolean getFeesAreEditable() {
		return getOption(EDITABLE_FEES_KEY, false);
	}

	public static void setFeesAreEditable(boolean feesAreEditable) {
		ZWWalletManager.getInstance().upsertAppDataVal(EDITABLE_FEES_KEY, feesAreEditable);
	}

	public static boolean getMempoolIsSpendable() {
		return getOption(SPENDABLE_MEMPOOL_KEY, false);
	}

	public static void setMempoolIsSpendable(boolean spendable) {
		ZWWalletManager.getInstance().upsertAppDataVal(SPENDABLE_MEMPOOL_KEY, spendable);
	}
	
	public static boolean getPassphraseWarningDisabled() {
		return getOption(PASSPHRASE_WARNING_KEY, false);
	}

	
	public static void setPassphraseWarningDisabled(boolean isDisabled) {
		ZWWalletManager.getInstance().upsertAppDataVal(PASSPHRASE_WARNING_KEY, isDisabled);
	}
	
	public static void disablePassphrase(){
		setStoredPassphraseHash(null);
	}

	
	public static ZWFiat getFiatCurrency(){
		String fiatString = getOption(FIAT_CURRENCY_KEY, "US Dollars");
		return ZWFiat.valueOf(fiatString);
	}

	public static void setFiatCurrency(String fiatSelected){
		ZWWalletManager.getInstance().upsertAppDataVal(FIAT_CURRENCY_KEY, fiatSelected);
	}
	
	public static String getUserName() {
		return getOption(PREFS_USER_NAME_KEY, null);
	}
	
	public static boolean userHasSetName() {
		return getUserName() != null;
	}
	
	public static int setUserName(String userName) {
		return ZWWalletManager.getInstance().upsertAppDataVal(PREFS_USER_NAME_KEY, userName);
	}
	
	public static void setDisabledName(boolean isDisabled){
		ZWWalletManager.getInstance().upsertAppDataVal(NAME_DISABLED_KEY, isDisabled);
	}
	
	public static boolean getDisabledName(){
		return getOption(NAME_DISABLED_KEY, false);
	}

	public static String getWidgetCoin(){
		return getOption(ZWWalletWidget.WIDGET_CURR, null);
	}
	
	public static void setWidgetCoin(String coin){
		ZWWalletManager.getInstance().upsertAppDataVal(ZWWalletWidget.WIDGET_CURR, coin);
	}

	public static boolean getDebugMode(){
		return getOption(DEBUG_SETTING_KEY, false);
	}
	
	public static void setDebugMode(boolean enabled){
		ZWWalletManager.getInstance().upsertAppDataVal(DEBUG_SETTING_KEY, enabled);
	}
	
	public static boolean getLogToFile(){
		return getOption(LOG_TO_FILE_KEY, false);
	}
	
	public static void setLogToFile(boolean enabled){
		ZWWalletManager.getInstance().upsertAppDataVal(LOG_TO_FILE_KEY, enabled);
	}
	
	public static String getCachedPassphrase(){
		if (ZWPreferences.cachedPassphrase != null){
			return ZWPreferences.cachedPassphrase.get();
		} else {
			return null;
		}
	}
	
	public static String getSalt(){
		String salt = ZWWalletManager.getInstance().getAppDataString(ZWAppDataTable.SALT);
		//if not in db, create new salt
		if (salt == null){
			//check preferences
			salt = getPrefString(PREFS_SALT_KEY);
			if (salt == null){
				salt = ZWPbeAesCrypter.generateSalt();
			}
			ZWWalletManager.getInstance().upsertAppDataVal(ZWAppDataTable.SALT, salt);
		}
		return salt;
	}
	
	public static void setCachedPassphrase(String pass){
		ZWPreferences.cachedPassphrase = new WeakReference<String>(pass);
		/** handler that schedules clearing of cachedPassphrase */
		final Handler cacheHandler = new Handler();
		/** runnable to clear cached passphrase */
		Runnable clearPassEvent = new Runnable(){ 
			@Override
			public void run() {
				//if we are using cachedpass, don't clear yet and schedule to clear it later
				if (ZWPreferences.usingCachedPass){
					cacheHandler.postDelayed(this, clearPassInterval);
				} else {
					// clear cached passphrase
					ZWPreferences.cachedPassphrase = null;
				}
			}
		};
		
		cacheHandler.postDelayed(clearPassEvent, ZWPreferences.clearPassInterval);
	}
	
	//reads option from appdata table in database, if none found, checks preferences for old settings
	private static String getOption(String key, String defaultVal) {
		String dbVal = ZWWalletManager.getInstance().getAppDataString(key);
		if (dbVal == null){
			SharedPreferences prefs = getPrefs();
			if (prefs != null && prefs.contains(key)){
				String prefVal = prefs.getString(key, defaultVal);
				//add old preference to db for future use
				ZWWalletManager.getInstance().upsertAppDataVal(key, prefVal);
				//remove from preferences in case of conflicts
				Editor editor = prefs.edit();
				editor.remove(key);
				editor.apply();
				return prefVal;
			} else {
				return defaultVal;
			}
		} else {
			return dbVal;
		}
	}
	
	
	private static boolean getOption(String key, boolean defaultVal) {
		Boolean dbVal = ZWWalletManager.getInstance().getAppDataBoolean(key);
		if (dbVal == null){
			//check prefs if none in db
			SharedPreferences prefs = getPrefs();
			if (prefs != null && prefs.contains(key)){
				boolean prefVal = prefs.getBoolean(key, defaultVal);
				ZWWalletManager.getInstance().upsertAppDataVal(key, prefVal);
				Editor editor = prefs.edit();
				editor.remove(key);
				editor.apply();
				return prefVal;
			} else {
				return defaultVal;
			}
		} else {
			return dbVal;
		}
	}


	//backwards compatible with old versions using preferences
	private static SharedPreferences getPrefs() {
		try {
			Context app = ZWApplication.getApplication();
			return app.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
		} catch (NullPointerException e){
			return null;	
		}
	}
	
	//returns a string associated with key from sharedPreferences if there is any, used for backwards compatibility with old versions
	private static String getPrefString(String prefKey){
		//check preferences if none in db
		SharedPreferences prefs = getPrefs();
		if (prefs != null && prefs.contains(prefKey)){
			String val = prefs.getString(prefKey, null);
			return val;
		}
		return null;
	}

}