/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */
package com.ziftr.android.ziftrwallet;

import java.util.Arrays;

import com.ziftr.android.ziftrwallet.crypto.ZWFiat;
import com.ziftr.android.ziftrwallet.crypto.ZWPbeAesCrypter;
import com.ziftr.android.ziftrwallet.sqlite.ZWWalletManager;
import com.ziftr.android.ziftrwallet.util.CryptoUtils;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public abstract class ZWPreferences { 

	/** The key for getting the password hash from the preferences. */
	public static final String PREFS_PASSWORD_KEY = "password_hash"; 

	/** key for getting and saving salt in the options file */
	static final String PREFS_SALT_KEY = "salt";

	/** The key for getting the name of the user from the preferences. */
	public static final String PREFS_USER_NAME_KEY = "user_name";

	/** skip the screen for name */
	public final static String NAME_DISABLED_KEY = "disabled_name";

	/** So we can skip the welcome screen and save the boolean. */
	public final static String PASSWORD_WARNING_KEY = "disabled_password";

	/** So we can save the boolean from settings, whether fees are edititable or not. */
	public final static String EDITABLE_FEES_KEY = "editable_fees";

	/** Save the selected Fiat Currency of user*/
	public final static String FIAT_CURRENCY_KEY = "fiat_currency";

	/** Save whether we are in debug mode or not*/
	public final static String DEBUG_SETTING_KEY = "debug_setting";

	/** Save whether we are in logging to filee or not*/
	public final static String LOG_TO_FILE_KEY = "log_to_file";

	/** Save whether we can spend unconfirmed txns or not*/
	public final static String UNCONFIRMED_WARNING_KEY = "unconfirmed_warning";

	/** Get the seed for the HD wallet. */
	public final static String HD_WALLET_SEED = "hd_wallet_seed";

	private static final String UPDATE_CHECK_KEY = "update_check";

	private static final String AGREED_TO_TOS_KEY = "agreed_to_tos";


	/**
	 * A simple thread implementation that will keep a string in memory for a certain period of time.
	 * Thread simply checks the time, and sleeps longer if it's not ready yet.
	 * Reading or writing the string will reset the timer.
	 * Static setter/getter protect against null/dead thread and start new thread if needed
	 *
	 */
	private static final class CacheThread extends Thread {

		private static CacheThread cacheThread = null;
		private static final int cacheTime = 300000;

		long cacheCleanupTime = 0;
		String cache = null;

		private CacheThread() {}

		@Override
		public void run() {

			while(true) {
				synchronized(CacheThread.class) {
					if(System.currentTimeMillis() < cacheCleanupTime) {
						try {
							//just wait on the class object, since we've already aquired the lock for 
							//other synchronization reasons
							CacheThread.class.wait(cacheCleanupTime - System.currentTimeMillis());
						} 
						catch (InterruptedException e) {
							//do nothing, if the thread gets woken up
							//then just check time again and go back to sleep
						}
					}
					else {
						cache = null;
						cacheThread = null;
						break;
					}

				}
			}

		}


		public static synchronized void setCache(String cacheString) {
			if(cacheThread == null) {
				cacheThread = new CacheThread();
				cacheThread.cache = cacheString;
				cacheThread.cacheCleanupTime = System.currentTimeMillis() + cacheTime;
				cacheThread.start();
			}
			else {
				cacheThread.cache = cacheString;
				cacheThread.cacheCleanupTime = System.currentTimeMillis() + cacheTime;
			}
		}


		public static synchronized String getCache() {
			if(cacheThread != null) {
				//getting the cache also extends the timer
				cacheThread.cacheCleanupTime = System.currentTimeMillis() + cacheTime;
				return cacheThread.cache;
			}

			return null;
		}

	}


	/**
	 * Gets the stored hash of the users password, if there is one.
	 * 
	 * @return the stored hash of the users password, if there is one.
	 */
	private static byte[] getStoredPasswordHash() {
		String storedPassword = ZWWalletManager.getInstance().getAppDataString(PREFS_PASSWORD_KEY);

		if(storedPassword != null) {
			return ZiftrUtils.hexStringToBytes(storedPassword);
		}

		return null;
	}


	public static int setStoredPassword(String password) {
		String saltedHash = ZWPreferences.saltedHashString(password);
		int numPrefsUpdated = ZWWalletManager.getInstance().upsertAppDataVal(PREFS_PASSWORD_KEY, saltedHash);

		if(numPrefsUpdated > 0) {
			//if any of the preferences have been updated (the password hash was successfully stored)
			//then update the cache
			setCachedPassword(password);
		}

		return numPrefsUpdated;
	}

	static int setStoredPasswordHash(String saltedHash) {
		return ZWWalletManager.getInstance().upsertAppDataVal(PREFS_PASSWORD_KEY, saltedHash);
	}


	/**
	 * Get a boolean describing whether or not the user 
	 * has entered a password before.
	 * 
	 * @return a boolean describing whether or not the user 
	 * has entered a password before.
	 */
	public static boolean userHasPassword() {
		return getStoredPasswordHash() != null;
	}


	/**
	 * Tests to see if the entered password matches the stored password,
	 * by comparing hashes
	 * 
	 * @param inputPassword a String representing the password to be checked
	 * @return true if a salted hash of the input string matches the stored hash of the password
	 */
	public static boolean inputPasswordMatchesStoredPassword(String inputPassword) {

		byte[] inputPasswordHash = ZWPreferences.saltedHash(inputPassword);
		byte[] storedHash = getStoredPasswordHash();

		if (storedHash != null && inputPasswordHash != null) {
			return Arrays.equals(storedHash, inputPasswordHash); 
		} 
		else if (storedHash == null && inputPasswordHash == null) {
			return true;
		}
		else {
			ZLog.log("Error, A hash value was null that shouldn't have been.");
			return false;
		}
	}


	public static boolean getFeesAreEditable() {
		return getOption(EDITABLE_FEES_KEY, false);
	}


	public static void setFeesAreEditable(boolean feesAreEditable) {
		ZWWalletManager.getInstance().upsertAppDataVal(EDITABLE_FEES_KEY, feesAreEditable);
	}

	public static boolean getWarningUnconfirmed() {
		return getOption(UNCONFIRMED_WARNING_KEY, true);
	}

	public static void setWarningUnconfirmed(boolean warn) {
		ZWWalletManager.getInstance().upsertAppDataVal(UNCONFIRMED_WARNING_KEY, warn);
	}

	public static boolean getPasswordWarningDisabled() {
		return getOption(PASSWORD_WARNING_KEY, false);
	}


	public static void setPasswordWarningDisabled(boolean isDisabled) {
		ZWWalletManager.getInstance().upsertAppDataVal(PASSWORD_WARNING_KEY, isDisabled);
	}

	public static void disablePassword(){
		setStoredPassword(null);
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

	public static String getCustomAPIServer() {
		return getOption("custom_server", null);
	}


	public static void setCustomAPIServer(String customServer) {
		ZWWalletManager.getInstance().upsertAppDataVal("custom_server", customServer);
	}


	public static String getCachedPassword(){

		if(ZWPreferences.userHasPassword()) {
			//as a catch-all, never return a cached password if the user doesn't have one
			return CacheThread.getCache();
		}

		return null;
	}


	public static String getSalt(){
		String salt = ZWWalletManager.getInstance().getAppDataString(PREFS_SALT_KEY);

		if(salt == null || salt.length() == 0) {
			if (!ZWPreferences.userHasPassword()){
				return null;
			}
			try {
				salt = ZWPbeAesCrypter.generateSalt();
				ZWWalletManager.getInstance().upsertAppDataVal(PREFS_SALT_KEY, salt);
			}
			catch(Exception e) {
				salt = null;
				ZLog.log("Exception creating salt: ", e);
			}
		}

		return salt;
	}

	public static synchronized void setCachedPassword(String password){
		CacheThread.setCache(password);
	}

	//reads option from appdata table in database, if none found, checks preferences for old settings
	private static String getOption(String key, String defaultVal) {
		String dbVal = ZWWalletManager.getInstance().getAppDataString(key);

		if (dbVal == null || dbVal.length() == 0){
			dbVal = defaultVal;
		}

		return dbVal;
	}


	private static boolean getOption(String key, boolean defaultVal) {
		Boolean dbVal = ZWWalletManager.getInstance().getAppDataBoolean(key);

		if(dbVal == null) {
			dbVal = defaultVal;
		}

		return dbVal;
	}


	public static long getLastUpdateCheck() {
		String databaseValue = ZWWalletManager.getInstance().getAppDataString(UPDATE_CHECK_KEY);

		if(databaseValue == null || databaseValue.length() == 0) {
			return 0;
		}

		return Long.valueOf(databaseValue);
	}


	public static void setLastUpdateCheck(long timestamp) {
		String lastCheck = String.valueOf(timestamp);
		ZWWalletManager.getInstance().upsertAppDataVal(UPDATE_CHECK_KEY, lastCheck);
	}


	public static boolean userAgreedToTos() {
		return getOption(AGREED_TO_TOS_KEY, false);
	}

	public static void setUserAgreedToTos(boolean agreed) {
		ZWWalletManager.getInstance().upsertAppDataVal(AGREED_TO_TOS_KEY, agreed);
	}

	private static String saltedHashString(String newPassword) {
		String saltedHashString = null;
		if(newPassword != null && newPassword.length() > 0) {
			byte[] saltedHash = saltedHash(newPassword);
			if(saltedHash != null) {
				saltedHashString = ZiftrUtils.bytesToHexString(saltedHash);
			}
		}

		return saltedHashString;
	}


	public static byte[] saltedHash(String password) {
		return saltedHash(ZWPreferences.getSalt(), password);
	}


	public static byte[] saltedHash(String salt, String password) {
		if (password == null || salt == null) {
			return null;
		}

		byte[] newPasswordBytes = password.getBytes();
		byte[] concat = new byte[32 + password.getBytes().length];
		System.arraycopy(CryptoUtils.Sha256Hash(salt.getBytes()), 0, concat, 0, 32);
		System.arraycopy(newPasswordBytes, 0, concat, 32, newPasswordBytes.length);
		return CryptoUtils.Sha256Hash(concat);
	}

	public static String getHdWalletSeed() {
		return ZWWalletManager.getInstance().getAppDataString(HD_WALLET_SEED);
	}

	public static void setHdWalletSeed(String data) {
		ZWWalletManager.getInstance().upsertAppDataVal(HD_WALLET_SEED, data);
	}

}