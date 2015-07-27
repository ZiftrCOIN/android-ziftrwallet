/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */
package com.ziftr.android.ziftrwallet;

import java.io.File;

import javax.crypto.SecretKey;

import android.app.Application;

import com.ziftr.android.ziftrwallet.crypto.ZWAddress;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWECKey;
import com.ziftr.android.ziftrwallet.crypto.ZWKeyCrypter;
import com.ziftr.android.ziftrwallet.crypto.ZWNullCrypter;
import com.ziftr.android.ziftrwallet.crypto.ZWPbeAesCrypter;
import com.ziftr.android.ziftrwallet.sqlite.ZWReceivingAddressesTable.EncryptionStatus;
import com.ziftr.android.ziftrwallet.sqlite.ZWSQLiteOpenHelper;
import com.ziftr.android.ziftrwallet.util.ZLog;

/** 
 * This class controls all of the wallets and is responsible
 * for setting them up and closing them down when the application exits.
 * 
 * TODO maybe this class should be a background fragment? Ask Justin. 
 * 
 * The goal is that this class will make it easy to switch between using bitocoinj
 * and switching to our API. All database access and bitcoinj access should go in here.
 * More specifically, database access should go in ZWSQLiteOpenHelper, and if the database
 * method is not quite correct, then it should be overridden in here to do somthing
 * with bitcoinj.
 * 
 * The database helper object. Open and closed in onCreate/onDestroy
 */
public class ZWWalletManager extends ZWSQLiteOpenHelper {

	/** The wallet files for each of the coin types. */
	//private Map<ZWCoin, File> walletFiles = new HashMap<ZWCoin, File>();
	
	/** Following the standard, we name our wallet file as below. */
	public static final String DATABASE_NAME = "wallet.dat";

	///////////////////////////////////////////////////////
	//////////  Static Singleton Access Members ///////////
	///////////////////////////////////////////////////////

	/** The path where the database will be stored. */
	private static String databasePath;

	/** The single instance of the database helper that we use. */
	private static ZWWalletManager instance;

	private static boolean logging = true;

	private static void log(Object... messages) {
		if (logging) {
			ZLog.log(messages);
		}
	}


	public static synchronized ZWWalletManager getInstance() {
		if (instance == null) {
			try {
				Application applicationContext = ZWApplication.getApplication();
					// Here we build the path for the first time if have not yet already
					if (databasePath == null) {
						File externalDirectory = applicationContext.getExternalFilesDir(null);
						if (externalDirectory != null) {
							databasePath = new File(externalDirectory, DATABASE_NAME).getAbsolutePath();
						} else {
							// If we couldn't get the external directory the user is doing something weird with their sd card
							// Leaving databaseName as null will let the database exist in memory
		
							//TODO -at flag and use it to trigger UI to let user know they are running on an in memory database
							log("CANNOT ACCESS LOCAL STORAGE!");
						}
					}
		
					instance = new ZWWalletManager(applicationContext);
			} catch (NullPointerException e){
				ZLog.log("applicationContext was null");
				return null;
			}
		}
			return instance;
	}

	/**
	 * Closes the database helper instance. Also resets the singleton instance
	 * to be null.
	 */
	public static synchronized void closeInstance() {
		if (instance != null) {
			instance.close();
		} 
		else {
			// If used right shouldn't happen because get instance should always
			// be called before every close call.
			log("instance was null when we called closeInstance...");
		}
		instance = null;
	}

	/**
	 * Make a new manager. This context should be cleared and then re-added when
	 * saving and bringing back the wallet manager. 
	 */
	private ZWWalletManager(Application context) {
		
		// super constructor opens up a connection with the database.
		super(context, databasePath);
	}

	
	public ZWAddress createChangeAddress(String passphrase, ZWCoin coinId) {
		return super.createChangeAddress(this.passphraseToCrypter(passphrase), coinId);
	}

	
	/**
	 * this method creates a visible unspentfrom receiving (owned by the user) {@link ZWAddress} object and adds it 
	 * to the correct table within our database
	 * 
	 * @param passphrase
	 * @param coinId
	 * @param note
	 * @param balance
	 * @param creation
	 * @param modified
	 * @return
	 */
	public ZWAddress createReceivingAddress(String passphrase, ZWCoin coinId, String note, long balance, long creation, long modified) {
		ZWAddress addr = super.createReceivingAddress(passphraseToCrypter(passphrase), coinId, note, balance, creation, modified, false, false);
		return addr;
	}

	/**
	 * @param passphrase
	 * @return
	 */
	private ZWKeyCrypter passphraseToCrypter(String passphrase) {
		ZWKeyCrypter crypter = null;
		if (passphrase != null && passphrase.length() > 0) {
			try {
				SecretKey secretKey = ZWPbeAesCrypter.generateSecretKey(passphrase, ZWPreferences.getSalt());
				crypter = new ZWPbeAesCrypter(secretKey);
			}
			catch(Exception e) {
				ZLog.log("Exception creating key crypter: ", e);
			}
		} else {
			crypter = new ZWNullCrypter();
		}
		return crypter;
	}

	/**
	 * Caution! Only use this method if you know that the oldPassphrase is the string
	 * that has encrypted all of the private keys in the wallets and the salt is the correct
	 * salt that was used in the encryption. In addition, ensure that null is passed in for 
	 * the old passphrase if there was no previous encryption.
	 * 
	 * This could have very bad effects if the preconditions are not met!
	 * 
	 * @param curPassphrase - null if no encryption
	 * @param salt
	 */
	public synchronized EncryptionStatus changeEncryptionOfReceivingAddresses(String oldPassword, String newPassword) {
		
		//having null passwords and as such null key crypter objects is ok (it's how a password could be removed)
		//however, if the passwords aren't null, we fail to make key crypter objects out of them, then that's an error
		ZWKeyCrypter oldCrypter = null;
		if(oldPassword != null && oldPassword.length() > 0) {
			oldCrypter = passphraseToCrypter(oldPassword);
			if(oldCrypter == null) {
				return EncryptionStatus.ERROR;
			}
		}
		
		ZWKeyCrypter newCrypter = null;
		if(newPassword != null && newPassword.length() > 0) {
			newCrypter = passphraseToCrypter(newPassword);
			if(newCrypter == null) {
				return EncryptionStatus.ERROR;
			}
		}
		
		return super.changeEncryptionOfReceivingAddresses(this.passphraseToCrypter(oldPassword), this.passphraseToCrypter(newPassword));
	}

	/**
	 * this is used to test passphrases when trying to recover from a state where a user
	 * tried to reencrypt already encrypted password and is entering his/her old passphrase
	 * to decrypt keys
	 */
	public synchronized boolean attemptDecrypt(String passphrase){
		return super.attemptDecrypt(passphraseToCrypter(passphrase));
	}
	
	/**
	 * gets an {@link ZWECKey} that can be used for signing from the public address,
	 * note the user must "own" the address (have a private key in the local db that corresponds to this public address)
	 * @param publicAddress the public address of the key to be used for signing
	 * @return 
	 */
	public ZWECKey getKeyForAddress(ZWCoin coin, String publicAddress, String passphrase) {
		ZWAddress signingAddress = ZWWalletManager.getInstance().getAddress(coin, publicAddress, true);
		ZWECKey key = signingAddress.getKey();
		key.setKeyCrypter(this.passphraseToCrypter(passphrase));
		return key;
	}


	@Override
	protected void finalize() throws Throwable {
		
		try {
			this.close();
		}
		catch(Exception e) {
			//just making sure the database is properly closed
		}
	}
	
	
	

}





