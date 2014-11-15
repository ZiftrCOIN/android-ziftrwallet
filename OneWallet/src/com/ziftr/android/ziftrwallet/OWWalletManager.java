package com.ziftr.android.ziftrwallet;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import android.content.Context;

import com.ziftr.android.ziftrwallet.crypto.OWAddress;
import com.ziftr.android.ziftrwallet.crypto.OWECDSASignature;
import com.ziftr.android.ziftrwallet.crypto.OWECKey;
import com.ziftr.android.ziftrwallet.crypto.OWKeyCrypter;
import com.ziftr.android.ziftrwallet.crypto.OWPbeAesCrypter;
import com.ziftr.android.ziftrwallet.crypto.OWSha256Hash;
import com.ziftr.android.ziftrwallet.sqlite.OWReceivingAddressesTable;
import com.ziftr.android.ziftrwallet.sqlite.OWSQLiteOpenHelper;
import com.ziftr.android.ziftrwallet.util.OWCoin;
import com.ziftr.android.ziftrwallet.util.OWPreferencesUtils;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

/** 
 * This class controls all of the wallets and is responsible
 * for setting them up and closing them down when the application exits.
 * 
 * TODO maybe this class should be a background fragment? Ask Justin. 
 * 
 * The goal is that this class will make it easy to switch between using bitocoinj
 * and switching to our API. All database access and bitcoinj access should go in here.
 * More specifically, database access should go in OWSQLiteOpenHelper, and if the database
 * method is not quite correct, then it should be overridden in here to do somthing
 * with bitcoinj.
 * 
 * The database helper object. Open and closed in onCreate/onDestroy
 */
public class OWWalletManager extends OWSQLiteOpenHelper {

	/** The map which holds all of the wallets. */
	//private Map<OWCoin, Wallet> walletMap = new HashMap<OWCoin, Wallet>();

	/** The wallet files for each of the coin types. */
	private Map<OWCoin, File> walletFiles = new HashMap<OWCoin, File>();

	//the application context
	private Context context;

	///////////////////////////////////////////////////////
	//////////  Static Singleton Access Members ///////////
	///////////////////////////////////////////////////////

	/** The path where the database will be stored. */
	private static String databasePath;

	/** The single instance of the database helper that we use. */
	private static OWWalletManager instance;

	private static boolean logging = true;

	private static void log(Object... messages) {
		if (logging) {
			ZLog.log(messages);
		}
	}


	public static synchronized OWWalletManager getInstance() {
		if (instance == null) {
			Context context = OWApplication.getApplication();

			// Here we build the path for the first time if have not yet already
			if (databasePath == null) {
				File externalDirectory = context.getExternalFilesDir(null);
				if (externalDirectory != null) {
					databasePath = new File(externalDirectory, DATABASE_NAME).getAbsolutePath();
				} else {
					// If we couldn't get the external directory the user is doing something weird with their sd card
					// Leaving databaseName as null will let the database exist in memory

					//TODO -at flag and use it to trigger UI to let user know they are running on an in memory database
					log("CANNOT ACCESS LOCAL STORAGE!");
				}
			}

			instance = new OWWalletManager(context);
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
		} else {
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
	private OWWalletManager(Context context) {
		// Making the wallet manager opens up a connection with the database.

		super(context, databasePath);
		this.context = context;
	}

	/**
	 * Returns a boolean describing whether or not the wallet for
	 * the specified coin type has been setup yet. This is session 
	 * specific as the wallets are closed down everytime the app is 
	 * destroyed.
	 * 
	 * @param id - The coin type to test for
	 * @return as above
	 */
	public boolean walletHasBeenSetUp(OWCoin id) {
		// There is no add wallet method, so the only way
		// a wallet can be added to the map is by setupWallet.
		return this.typeIsActivated(id);
	}

	/**
	 * Gives a list of all the coin types for which the user has set up a wallet
	 * this session. i.e. all wallet types for which walletHasBeenSetUp()
	 * will return true.
	 * 
	 * @return as above
	 */
	public List<OWCoin> getAllSetupWalletTypes() {
		ArrayList<OWCoin> list = new ArrayList<OWCoin>();
		for (OWCoin type : OWCoin.values()) {
			if (this.walletHasBeenSetUp(type)) {
				list.add(type);
			}
		}
		return list;
	}


	/**
	 * Sets up a wallet object for this fragment by either loading it 
	 * from an existing stored file or by creating a new one from 
	 * network parameters. Note, though Android won't crash out because 
	 * of it, this shouldn't be called from a UI thread due to the 
	 * blocking nature of creating files.
	 * 
	 * TODO check to make sure all places where this is called is okay.
	 */
	public boolean setUpWallet(final OWCoin id) {
		// Have to do this for both SQLite and bitcoinj right now.

		// Set up the SQLite tables
		this.ensureCoinTypeActivated(id);

		// Here we recreate the files or create them if this is the first
		// time the user opens the app.
		this.walletFiles.put(id, null);

		// This is application specific storage, will be deleted when app is uninstalled.
		File externalDirectory = this.context.getExternalFilesDir(null);
		if (externalDirectory != null) {
			this.walletFiles.put(id, new File(
					externalDirectory, id.getShortTitle() + "_wallet.dat"));
		} else {
			log("null NULL EXTERNAL DIR");
			/***
			OWSimpleAlertDialog alertUserDialog = new OWSimpleAlertDialog();
			alertUserDialog.setupDialog("OneWallet", "Error: No external storage detected.", null, "OK", null);
			alertUserDialog.show(this.activity.getSupportFragmentManager(), "null_externalDirectory");
			 **/
			return false;
		}

		return true;
	}


	
	
	public OWAddress createReceivingAddress(String passphrase, OWCoin coinId, int hidden) {
		return super.createReceivingAddress(this.passphraseToCrypter(passphrase), coinId, hidden);
	}
	
	public OWAddress createReceivingAddress(String passphrase, OWCoin coinId, String note, int hidden) {
		return super.createReceivingAddress(passphraseToCrypter(passphrase), coinId, note, hidden);
	}
	
	/**
	 * As part of the C in CRUD, this method adds a receiving (owned by the user)
	 * address to the correct table within our database.
	 * 
	 * Temporarily, we need to make the address and tell bitcoinj about it.
	 * 
	 * @param coinId - The coin type to determine which table we use. 
	 * @param key - The key to use.
	 */
	public OWAddress createReceivingAddress(String passphrase, OWCoin coinId, String note, 
			long balance, long creation, long modified) {
		OWAddress addr = super.createReceivingAddress(passphraseToCrypter(passphrase), coinId, note, balance, creation, 
				modified, OWReceivingAddressesTable.VISIBLE_TO_USER, OWReceivingAddressesTable.UNSPENT_FROM);
		return addr;
	}

	/**
	 * @param passphrase
	 * @return
	 */
	private OWKeyCrypter passphraseToCrypter(String passphrase) {
		OWKeyCrypter crypter = null;
		if (passphrase != null) {
			SecretKey secretKey = OWPbeAesCrypter.generateSecretKey(passphrase, OWPreferencesUtils.getSalt());
			crypter = new OWPbeAesCrypter(secretKey);
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
	public void changeEncryptionOfReceivingAddresses(String oldPassphrase, String newPassphrase) {
		super.changeEncryptionOfReceivingAddresses(this.passphraseToCrypter(oldPassphrase), this.passphraseToCrypter(newPassphrase));
	}


	/** 
	 * Gets the file where the wallet for the specified
	 * coin type is stored. 
	 * 
	 * @param id - The coin type of the wallet file to get
	 * @return as above
	 */
	public File getWalletFile(OWCoin id) {
		return this.walletFiles.get(id);
	}

	
	
	/**
	 * gets an {@link OWECKey} that can be used for signing from the public address,
	 * note the user must "own" the address (have a private key in the local db that corresponds to this public address)
	 * @param publicAddress the public address of the key to be used for signing
	 * @return 
	 */
	public OWECKey getKeyForAddress(OWCoin coin, String publicAddress, String passphrase) {
		OWAddress signingAddress = OWWalletManager.getInstance().readAddress(coin, publicAddress, true);
		OWECKey key = signingAddress.getKey();
		key.setKeyCrypter(this.passphraseToCrypter(passphrase));
		return key;
	}


}





