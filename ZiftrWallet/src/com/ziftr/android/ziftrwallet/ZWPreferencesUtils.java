package com.ziftr.android.ziftrwallet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.Arrays;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;

import com.ziftr.android.ziftrwallet.crypto.ZWFiat;
import com.ziftr.android.ziftrwallet.crypto.ZWKeyCrypterException;
import com.ziftr.android.ziftrwallet.crypto.ZWPbeAesCrypter;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public abstract class ZWPreferencesUtils {
	public static final String ZWOPTIONS_KEY = "ziftrwallet_stored_options";

	/** For putting the passphrase into a bundle and/or intent extras. */
	public static final String BUNDLE_PASSPHRASE_KEY = "bundle_passphrase_key";
	
	/** For putting the passphrase into a bundle and/or intent extras. */
	public static final String BUNDLE_NAME_KEY = "bundle_name_key";

	/** Used for getting the preferences */
	public static final String PREFERENCES_FILE_NAME = "ziftrWALLET_Prefs";
	
	/** The key for getting the passphrase hash from the preferences. */
	public static final String PREFS_PASSPHRASE_KEY = "zw_passphrase_key_1";

	/** The key for getting the name of the user from the preferences. */
	public static final String PREFS_USER_NAME_KEY = "zw_name_key";
	
	/** The key to get and save the salt as used by the specific user of the application. */
	static final String PREFS_SALT_KEY = "ziftrWALLET_salt_key";
	
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
	
	/** local in memory copy of options read from file */
	private static JSONObject options = null;
	

	/**
	 * Gets the stored hash of the users passphrase, if there is one.
	 * 
	 * @return the stored hash of the users passphrase, if there is one.
	 */
	static byte[] getStoredPassphraseHash() {
		String storedPassphrase = getOption(PREFS_PASSPHRASE_KEY, null);
		byte[] storedHash;
		if(storedPassphrase == null || storedPassphrase.isEmpty()) {
			storedHash = null;
		}
		else {
			// If it's not null, convert it back to a byte array
			storedHash = ZiftrUtils.hexStringToBytes(storedPassphrase);
		}
		// Return the result
		return storedHash;
	}
	
	static void setStoredPassphraseHash(String saltedHash) {
		setOption(PREFS_PASSPHRASE_KEY, saltedHash);
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

	public static String getSalt() {
		String salt = getOption(PREFS_SALT_KEY, null);
		if (salt != null && !salt.isEmpty()) {
			return salt;
		}
		//if we have no saved salt, create a new one
		try {
			salt = ZWPbeAesCrypter.generateSalt();
		} catch (ZWKeyCrypterException e) {
			ZLog.log("error: " + e.getMessage());
			return null;
		}
		setOption(PREFS_SALT_KEY, salt);
		return salt;
	}

	public static boolean getFeesAreEditable() {
		return getOption(EDITABLE_FEES_KEY, false);
	}

	public static void setFeesAreEditable(boolean feesAreEditable) {
		setOption(EDITABLE_FEES_KEY, feesAreEditable);
	}

	public static boolean getMempoolIsSpendable() {
		return getOption(SPENDABLE_MEMPOOL_KEY, false);
	}

	public static void setMempoolIsSpendable(boolean spendable) {
		setOption(SPENDABLE_MEMPOOL_KEY, spendable);
	}
	
	public static boolean getPassphraseWarningDisabled() {
		return getOption(PASSPHRASE_WARNING_KEY, false);
	}

	
	public static void setPassphraseWarningDisabled(boolean isDisabled) {
		setOption(PASSPHRASE_WARNING_KEY, isDisabled);
	}
	
	public static void disablePassphrase(){
		setStoredPassphraseHash(null);
	}

	
	public static ZWFiat getFiatCurrency(){
		String fiatString = getOption(FIAT_CURRENCY_KEY, "US Dollars");
		return ZWFiat.valueOf(fiatString);
	}

	public static void setFiatCurrency(String fiatSelected){
		setOption(FIAT_CURRENCY_KEY, fiatSelected);
	}
	
	public static String getUserName() {
		return getOption(PREFS_USER_NAME_KEY, null);
	}
	
	public static boolean userHasSetName() {
		return getUserName() != null;
	}
	
	public static void setUserName(String userName) {
		setOption(PREFS_USER_NAME_KEY, userName);
	}
	
	public static void setDisabledName(boolean isDisabled){
		setOption(NAME_DISABLED_KEY, isDisabled);
	}
	
	public static boolean getDisabledName(){
		return getOption(NAME_DISABLED_KEY, false);
	}

	public static String getWidgetCoin(){
		return getOption(ZWWalletWidget.WIDGET_CURR, null);
	}
	
	public static void setWidgetCoin(String coin){
		setOption(ZWWalletWidget.WIDGET_CURR, coin);
	}

	public static boolean getDebugMode(){
		return getOption(DEBUG_SETTING_KEY, false);
	}
	
	public static void setDebugMode(boolean enabled){
		setOption(DEBUG_SETTING_KEY, enabled);
	}
	
	public static boolean getLogToFile(){
		return getOption(LOG_TO_FILE_KEY, false);
	}
	
	public static void setLogToFile(boolean enabled){
		setOption(LOG_TO_FILE_KEY, enabled);
	}
	
	public static String getCachedPassphrase(){
		if (ZWPreferencesUtils.cachedPassphrase != null){
			return ZWPreferencesUtils.cachedPassphrase.get();
		} else {
			return null;
		}
	}
	
	public static void setCachedPassphrase(String pass){
		ZWPreferencesUtils.cachedPassphrase = new WeakReference<String>(pass);
		/** handler that schedules clearing of cachedPassphrase */
		final Handler cacheHandler = new Handler();
		/** runnable to clear cached passphrase */
		Runnable clearPassEvent = new Runnable(){ 
			@Override
			public void run() {
				//if we are using cachedpass, don't clear yet and schedule to clear it later
				if (ZWPreferencesUtils.usingCachedPass){
					cacheHandler.postDelayed(this, clearPassInterval);
				} else {
					// clear cached passphrase
					ZWPreferencesUtils.cachedPassphrase = null;
				}
			}
		};
		
		cacheHandler.postDelayed(clearPassEvent, ZWPreferencesUtils.clearPassInterval);
	}
	
	private static String getOption(String key, String defaultVal) {
		if (ZWPreferencesUtils.options == null){
			ZWPreferencesUtils.loadOptions();
		}
		try {
			if (options.has(key)){
				return ZWPreferencesUtils.options.getString(key);
			}
		} catch (JSONException e) {
			//no value found return default
		}
		
		//if we have old preferences, set the default value to the old preference so that if we have no 
		//new settings, we will read from the old prefs.
		SharedPreferences prefs = getPrefs();
		if (prefs != null && prefs.contains(key)){
			defaultVal = prefs.getString(key, defaultVal);
			//update options file with old pref and then remove from prefs
			setOption(key, defaultVal);
			Editor editor = prefs.edit();
			editor.remove(key);
		}
		
		return defaultVal;
	}
	
	
	private static boolean getOption(String key, boolean defaultVal) {
		if (ZWPreferencesUtils.options == null){
			ZWPreferencesUtils.loadOptions();
		}
		try {
			if (options.has(key)){
				return ZWPreferencesUtils.options.getBoolean(key);
			}
		} catch (JSONException e) {
			//no value found return default
		}
		
		//if we have old preferences, set the default value to the old preference so that if we have no 
		//new settings, we will read from the old prefs.
		SharedPreferences prefs = getPrefs();
		if (prefs != null && prefs.contains(key)){
			defaultVal = prefs.getBoolean(key, defaultVal);
			//update options file with old pref and then remove from prefs
			setOption(key, defaultVal);
			Editor editor = prefs.edit();
			editor.remove(key);
		}
		
		return defaultVal;
	}
	
	private static void setOption(String key, boolean value){
		if (ZWPreferencesUtils.options == null){
			ZWPreferencesUtils.loadOptions();
		}
		try {
			ZWPreferencesUtils.options.put(key, value);
			ZWPreferencesUtils.commitOptions();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void setOption(String key, String value){

		if (ZWPreferencesUtils.options == null){
			ZWPreferencesUtils.loadOptions();
		}
		try {
			ZWPreferencesUtils.options.put(key, value);
			ZWPreferencesUtils.commitOptions();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void commitOptions(){
		Context app = ZWApplication.getApplication();
		try {
			FileOutputStream fos = app.openFileOutput(ZWOPTIONS_KEY, Context.MODE_PRIVATE);
			if (ZWPreferencesUtils.options != null){
				fos.write(ZWPreferencesUtils.options.toString().getBytes());
				fos.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			ZLog.log("Error getting options input File not Found: " + e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void loadOptions(){
		Context app = ZWApplication.getApplication();
		try {
			FileInputStream f = app.openFileInput(ZWOPTIONS_KEY);
			StringBuffer data = new StringBuffer("");
			InputStreamReader isr = new InputStreamReader(f);
			BufferedReader reader = new BufferedReader(isr);
			String readString = reader.readLine();
			while (readString != null){
				data.append(readString);
				readString = reader.readLine();
			}
			if (data.toString().isEmpty()){
				//no data found on internal storage initialize new jsonobject for options
				ZWPreferencesUtils.options = new JSONObject("{}");
			} else {
				ZWPreferencesUtils.options = new JSONObject(data.toString());
			}
		} catch (FileNotFoundException e1) {	
			ZLog.log("Error getting options output File not Found: " + e1 + "\n creating new file");
			try {
				app.openFileOutput(ZWOPTIONS_KEY, Context.MODE_PRIVATE | Context.MODE_APPEND);
				ZWPreferencesUtils.options = new JSONObject("{}");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
	
	//clears and deletes options file and local data
	public static void clearOptions(){
		ZWPreferencesUtils.options = null;
		File options = new File(ZWApplication.getApplication().getFilesDir().getAbsoluteFile(), ZWPreferencesUtils.ZWOPTIONS_KEY);
		options.delete();
	}

}