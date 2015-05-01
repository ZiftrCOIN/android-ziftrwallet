package com.ziftr.android.ziftrwallet;

import java.util.Arrays;

import android.content.Context;
import android.content.SharedPreferences;

import com.ziftr.android.ziftrwallet.crypto.ZWFiat;
import com.ziftr.android.ziftrwallet.crypto.ZWPbeAesCrypter;
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
	static byte[] getStoredPasswordHash() {
		String storedPassword = ZWWalletManager.getInstance().getAppDataString(PREFS_PASSWORD_KEY);
		
		if(storedPassword != null) {
			return ZiftrUtils.hexStringToBytes(storedPassword);
		}
		
		return null;
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
	 * Tells if the given hash matches the stored password
	 * hash.
	 * 
	 * @param inputHash - the hash to check agains
	 * @return as above
	 */
	public static boolean inputHashMatchesStoredHash(byte[] inputHash) {
		byte[] storedHash = getStoredPasswordHash();
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
		setStoredPasswordHash(null);
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
		return CacheThread.getCache();
	}
	
	
	public static String getSalt(){
		String salt = ZWWalletManager.getInstance().getAppDataString(PREFS_SALT_KEY);
		
		if(salt == null || salt.length() == 0) {
			salt = ZWPbeAesCrypter.generateSalt();
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
	

}