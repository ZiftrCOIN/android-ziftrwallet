package com.ziftr.android.onewallet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import android.content.Context;

import com.google.bitcoin.core.AbstractWalletEventListener;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.CheckpointManager;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.InsufficientMoneyException;
import com.google.bitcoin.core.PeerAddress;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.SPVBlockStore;
import com.google.bitcoin.store.UnreadableWalletException;
import com.google.bitcoin.utils.Threading;
import com.ziftr.android.onewallet.crypto.OWAddress;
import com.ziftr.android.onewallet.crypto.OWECKey;
import com.ziftr.android.onewallet.crypto.OWSha256Hash;
import com.ziftr.android.onewallet.crypto.OWTransaction;
import com.ziftr.android.onewallet.dialog.OWSimpleAlertDialog;
import com.ziftr.android.onewallet.exceptions.OWAddressFormatException;
import com.ziftr.android.onewallet.exceptions.OWInsufficientMoneyException;
import com.ziftr.android.onewallet.fragment.accounts.OWWalletTransactionListAdapter;
import com.ziftr.android.onewallet.sqlite.OWSQLiteOpenHelper;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWFiat;
import com.ziftr.android.onewallet.util.ZLog;
import com.ziftr.android.onewallet.util.ZiftrUtils;

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
	private Map<OWCoin, Wallet> walletMap = new HashMap<OWCoin, Wallet>();

	/** The wallet files for each of the coin types. */
	private Map<OWCoin, File> walletFiles = new HashMap<OWCoin, File>();

	/** The file where the BlockStore for this coin will be stored. */
	private Map<OWCoin, File> blockStoreFiles = new HashMap<OWCoin, File>();

	/** A map of hashes to StoredBlocks to save StoredBlock objects to disk. */
	private Map<OWCoin, BlockStore> blockStores = 
			new HashMap<OWCoin, BlockStore>();

	/** The PeerGroup that this SPV node is connected to. */
	private Map<OWCoin, PeerGroup> peerGroups = 
			new HashMap<OWCoin, PeerGroup>();

	/** 
	 * When the app is started it searches through all of these and looks for 
	 * any wallet files that alread exists. If they do, it sets them up.  
	 */
	public static OWCoin[] enabledCoinTypes = new OWCoin[] {
		OWCoin.BTC_TEST,
		OWCoin.BTC
	};

	/** 
	 * This should be cleared when saving this object 
	 * in {@link OWMainFragmentActivity}. 
	 * 
	 * TODO maybe we want to get this everytime?
	 * or maybe a weak reference? 
	 */
	private Context activity;

	///////////////////////////////////////////////////////
	//////////  Static Singleton Access Members ///////////
	///////////////////////////////////////////////////////

	/** The path where the database will be stored. */
	private static String databasePath;

	/** The single instance of the database helper that we use. */
	private static OWWalletManager instance;

	private static boolean logging = false;

	private static void log(Object... messages) {
		if (logging) {
			ZLog.log(messages);
		}
	}

	/** 
	 * Gets the singleton instance of the database helper, making it if necessary. 
	 * 
	 * TODO what if context passed in isn't the context of instance's?
	 */
	public static synchronized OWWalletManager getInstance(Context context) {

		if (instance == null) {

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
		} else {
			// If used right shouldn't happen because close should always
			// be called after every get instance call.
			log("instance wasn't null and we called getInstance...");
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
		this.activity = context;

		List<OWCoin> list = readAllActivatedTypes();
		for (OWCoin enabledType : list) {
			this.setUpWallet(enabledType);
		}
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
		return this.typeIsActivated(id) && walletMap.get(id) != null;
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
	 * A convenience method that closes all wallets that have been 
	 * set up. 
	 */
	public void closeAllSetupWallets() {
		for (OWCoin type : this.getAllSetupWalletTypes()) {
			this.closeWallet(type);
		}
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
		File externalDirectory = this.activity.getExternalFilesDir(null);
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

		this.walletMap.put(id, null);

		// Try to load the wallet from a file
		try {
			this.walletMap.put(id, Wallet.loadFromFile(this.walletFiles.get(id)));
		} catch (UnreadableWalletException e) {
			log("Exception trying to load wallet file: ", e);
		}

		// If the load was unsucecssful (presumably only if this is the first 
		// time this type of wallet was set up) then we make a new wallet and add
		// a new key to the wallet.
		if (this.walletMap.get(id) == null) {
			this.walletMap.put(id, new Wallet(id.getNetworkParameters()));
			try {
				this.walletMap.get(id).saveToFile(this.walletFiles.get(id));
			} catch (IOException e) {
				log("Exception trying to save new wallet file: ", e);
				// TODO this is just test code of course but this is probably 
				// "fatal" as if we can't save our wallet, that's a problem
				return false;
			}
		}

		this.getWallet(id).addEventListener(new AbstractWalletEventListener() {
			@Override
			public synchronized void onCoinsReceived(
					Wallet w, 
					Transaction tx, 
					BigInteger prevBalance, 
					final BigInteger newBalance) {
				// TODO need to update the data in the list of transactions
				log(id.toString() + ": coins received...!");
			}

			// TODO override more methods here to do things like changing 
			// transactions from pending to finalized, etc.
		});

		// How to watch an outside address, note: we probably 
		// won't do this client side in the actual app
		//		Address watchedAddress;
		//		try {
		//			watchedAddress = new Address(networkParams, 
		//					"mrbgTx73gQiu8oHJfSadFHYQose3Arj3Db");
		//			wallet.addWatchedAddress(watchedAddress, 1402200000);
		//		} catch (AddressFormatException e) {
		//			log("Exception trying to add a watched address: ", e);
		//		}

		// If it is encrypted, bitcoinj just works with it encrypted as long
		// as all sendRequests etc have the 
		//KeyParameter keyParam = wallet.encrypt("password");

		// TODO autosave using 
		// this.wallet.autosaveToFile(f, delayTime, timeUnit, eventListener);

		// To decrypt the wallet. Note that this should only be done if 
		// the user specifically requests that their wallet be unencrypted. 
		//wallet.decrypt(wallet.getKeyCrypter().deriveKey("password"));

		// Note: wallets should only be encrypted once using the 
		// password, decryption is specifically for removing encryption 
		// completely instead any spending transactions should set 
		// SendRequest.aesKey and it will be used to decrypt keys as it signs.
		// TODO Explain???

		this.beginSyncWithNetwork(id);
		return true;
	}

	public void closeWallet(OWCoin id) {
		if (peerGroups.get(id) != null) {
			peerGroups.get(id).stop();
		}

		// Close the blockstore because we are no longer going to be receiving
		// and storing blocks
		if (blockStores.get(id) != null) {
			try {
				blockStores.get(id).close();
			} catch (BlockStoreException e) {
				log("Exception closing block store: ", e);
			}
		}

		// Save the wallet to the file. Not that this will save the wallet as either
		// encrypted or decrypted, depending on which was set last using the encrypt
		// or decrypt methods. 
		if (walletMap.get(id) != null) {
			try {
				walletMap.get(id).saveToFile(walletFiles.get(id));
			} catch (IOException e) {
				log("Exception saving wallet file on shutdown: ", e);
			}
		}
	}

	/**
	 * 
	 */
	private void beginSyncWithNetwork(final OWCoin id) {
		ZiftrUtils.runOnNewThread(new Runnable() {
			@Override
			public void run() {

				try {
					log("RUNNING");

					// TODO do I have multiple of these running? 
					// At one time we did, not sure about now...
					File externalDirectory = activity.getExternalFilesDir(null);

					blockStoreFiles.put(id, new File(externalDirectory, 
							id.getShortTitle() + "_blockchain.store"));

					boolean blockStoreExists = blockStoreFiles.get(id).exists();

					// This creates the file if it doesn't exist, so check first
					blockStores.put(id, new SPVBlockStore(id.getNetworkParameters(), 
							blockStoreFiles.get(id)));

					if (!blockStoreExists) {
						// We're creating a new block store file here, so 
						// use checkpoints to speed up network syncing
						if (activity == null) {
							// If this happens app is dying, so wait and 
							// do this on the next run
							log("activity was null and we returned early.");
							return; 
						}

						InputStream inputSteam = activity.getResources(
								).openRawResource(R.raw.btc_checkpoints);
						try {
							CheckpointManager.checkpoint(
									id.getNetworkParameters(), inputSteam, 
									blockStores.get(id), 
									walletMap.get(id).getEarliestKeyCreationTime());
						} catch (IOException e) {
							// We failed to read the checkpoints file 
							// for some reason, still let the user 
							// attempt sync the chain the slow way
							log("Failed to load chain checkpoint: ", e);
						}
					}

					BlockChain chain = new BlockChain(id.getNetworkParameters(), 
							walletMap.get(id), blockStores.get(id));

					peerGroups.put(id, new PeerGroup(id.getNetworkParameters(), chain));

					peerGroups.get(id).setUserAgent("OneWallet", "1.0");

					// So the wallet receives broadcast transactions.
					peerGroups.get(id).addWallet(walletMap.get(id));

					peerGroups.get(id).addPeerDiscovery(
							new DnsDiscovery(id.getNetworkParameters()));

					//peerGroup.setMaxConnections(25);

					// This is for running local version of bitcoin 
					// network for testing
					//peerGroup.addAddress(InetAddress.getLocalHost()); 

					// This starts the connecting with peers on a new thread
					peerGroups.get(id).start();

					// Note that instead of using the anonymous inner class 
					// based, we could use another class which implements 
					// WalletEventListener, however that requires setting up a 
					// bunch of methods we don't need for this simple test

					// Now download and process the block chain.
					peerGroups.get(id).downloadBlockChain();

					// TODO these are not safe because other threads can 
					// modify the transaction collections while we iterate 
					// through them, however this is just for testing right now

					//also upload any pending transactions
					Collection<Transaction> pendingTransactions = 
							walletMap.get(id).getPendingTransactions();
					//walletMap.get(id).getKeyCrypter();

					synchronized (pendingTransactions) {

						for (Transaction tx : pendingTransactions) {
							//log("There is a pending transaction "
							//		+ "in the wallet: ", tx.toString(chain));
							//wallet.commitTx(tx);

							try {
								//wallet.commitTx(tx);
								//wallet.cleanup();

								if (!walletMap.get(id).isConsistent()) {
									log("Wallet is inconsistent!!");
								}

								if (walletMap.get(id).isTransactionRisky(tx, null)) {
									log("Transaction is risky...");
								}

								String logMsg = "Pending Transaction Details: \n";
								logMsg += "isPending: " + String.valueOf(
										tx.isPending()) +"\n";
								logMsg += "isMature: " + String.valueOf(
										tx.isMature()) + "\n";
								logMsg += "isAnyOutputSpent: " + String.valueOf(
										tx.isAnyOutputSpent()) + "\n";
								logMsg += "sigOpCount: " + String.valueOf(
										tx.getSigOpCount()) +"\n";

								TransactionConfidence confidence = 
										tx.getConfidence();

								logMsg += "Confidence: \n" + 
										confidence.toString() + "\n";

								logMsg += "Confidence Type: " + String.valueOf(
										confidence.getConfidenceType().getValue()) 
										+ "\n";
								logMsg += "Confidence Block Depth: " + 
										String.valueOf(confidence.getDepthInBlocks(
												)) + "\n";
								logMsg += "Number Broadcast Peers: " + 
										String.valueOf(confidence.numBroadcastPeers(
												))+"\n";

								String broadcastByString = "none";
								ListIterator<PeerAddress> broadcastBy = 
										confidence.getBroadcastBy();
								if (broadcastBy != null) {
									broadcastByString = "";
									while(broadcastBy.hasNext()) {
										broadcastByString += broadcastBy.next().toString() + "\n";
									}
								}

								logMsg += "Broadcast by Peers: \n" + broadcastByString + "\n";
								logMsg += "Confidence Source: " + confidence.getSource().name() +"\n";



								logMsg += "version: " + String.valueOf(tx.getVersion()) +"\n";

								String hashes = "null";
								Map<Sha256Hash, Integer> hashesTransactionAppearsIn = tx.getAppearsInHashes();
								if (hashesTransactionAppearsIn != null) {
									hashes = hashesTransactionAppearsIn.toString();
								}

								logMsg += "Transaction Appears in Hashes: \n" + hashes +"\n";

								logMsg += "Transaction Hash: " + tx.getHashAsString() + "\n";

								logMsg += "Transaction Update Time: " + tx.getUpdateTime().toString() +"\n";

								logMsg += "Transaction Value: " + String.valueOf(tx.getValue(walletMap.get(id))) + "\n";

								log(logMsg);

								//wallet.clearTransactions(0); //for testing... drop a bomb on our wallet

							} catch(Exception e) {
								log("Caught Excpetion: ", e);
							}

							try {
								//peerGroup.broadcastTransaction(tx);
							} catch(Exception e) {
								log("Caught Excpetion: ", e);
							}

						} 
					}

					/**
		        List<Transaction> recentTransactions = wallet.getRecentTransactions(20, true);
		        for (Transaction tx : recentTransactions) {
		        	log("Recent transaction: ", tx);
		        }
					 ***/

				} catch (BlockStoreException e) {
					log("Exeption creating block store: ", e);
				} 

				log("STOPPING");

			}
		});
	}

	/**
	 * Sends the type of coin that this thread actually represents to 
	 * the specified address. 
	 * 
	 * @param address - The address to send coins to.
	 * @param value - The number of atomic units (satoshis for BTC) to send
	 * @throws AddressFormatException
	 * @throws InsufficientMoneyException
	 */
	public void sendCoins(OWCoin coinId, String address, BigInteger value, BigInteger feePerKb) 
			throws OWAddressFormatException, OWInsufficientMoneyException {

		// TODO should database be updated here or should we do it after API call, or... ?

		Wallet wallet = this.walletMap.get(coinId);

		// Create an address object based on network parameters in use 
		// and the entered address. This is the address we will send coins to.
		Address sendAddress;
		try {
			sendAddress = new Address(wallet.getNetworkParameters(), address);
		} catch (AddressFormatException e) {
			throw new OWAddressFormatException(e.getMessage());
		}

		// Use the address and the value to create a new send request object
		Wallet.SendRequest sendRequest = Wallet.SendRequest.to(sendAddress, value);


		// If the wallet is encrypted then we have to load the AES key so that the
		// wallet can get at the private keys and sign the data.
		if (wallet.isEncrypted()) {
			sendRequest.aesKey = wallet.getKeyCrypter().deriveKey("password");
		}

		// Make sure we aren't adding any additional fees
		sendRequest.ensureMinRequiredFee = false;

		// Set a fee for the transaction, always the minimum for now.
		// sendRequest.fee = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;
		sendRequest.fee = BigInteger.ZERO;

		// TODO make the rest of the code have variable names that reflect that this
		// is actually a fee per kilobyte.
		sendRequest.feePerKb = feePerKb;

		// I don't think we want to do this. 
		//wallet.decrypt(null);

		Wallet.SendResult sendResult;
		try {
			sendResult = wallet.sendCoins(sendRequest);
		} catch (InsufficientMoneyException e) {
			throw new OWInsufficientMoneyException(coinId, e.missing, e.getMessage());
		}
		sendResult.broadcastComplete.addListener(new Runnable() {
			@Override
			public void run() {
				// TODO if we want something to be done here that relates
				// to the gui thread, how do we do that?
				log("Successfully sent coins to address!");
			}
		}, Threading.SAME_THREAD); // changed from MoreExecutors.sameThreadExecutor()

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
	public OWAddress createReceivingAddress(OWCoin coinId, String note, 
			long balance, long creation, long modified) {
		OWAddress addr = super.createReceivingAddress(coinId, note, 
				balance, creation, modified);
		this.getWallet(coinId).addKey(owKeytoBitcoinjKey(addr.getKey()));
		return addr;
	}

	@Override
	public BigInteger getWalletBalance(OWCoin coinId, BalanceType bType) {
		// TODO After ready to remove bitcoinj, remove this whole method so that super's
		// method is called. 
		return this.walletMap.get(coinId).getBalance(Wallet.BalanceType.valueOf(bType.toString()));
	}

	@Override
	public List<OWTransaction> getPendingTransactions(OWCoin coinId) {
		// TODO After ready to remove bitcoinj, remove this whole method so that super's
		// method is called. 
		// TODO this method is doing to much, should just be a db read. Need to 
		// get sync working.
		List<OWTransaction> pendingTransactions = new ArrayList<OWTransaction>();
		for (Transaction tx : this.walletMap.get(coinId).getPendingTransactions()) {
			OWTransaction owTx = bitcoinjTransactionToOWTransaction(coinId, tx);
			ZLog.log("pending id: " + owTx.getId());
			pendingTransactions.add(owTx);
		}
		return pendingTransactions;
	}

	@Override
	public List<OWTransaction> getConfirmedTransactions(OWCoin coinId) {
		// TODO After ready to remove bitcoinj, remove this whole method so that super's
		// method is called. 
		// Unfortunately, bitcoinj doesn't make it easy to just get confirmed. 
		// For now, we have to make sure we 
		// TODO this method is doing to much, should just be a db read. Need to 
		// get sync working.
		List<OWTransaction> confirmedTransactions = new ArrayList<OWTransaction>();
		List<OWTransaction> pendingTransactions = getPendingTransactions(coinId);

		Set<String> pendingTxHashes = null;

		if (pendingTransactions.size() > 0) {
			pendingTxHashes = new HashSet<String>();
			for (OWTransaction tx : pendingTransactions) {
				pendingTxHashes.add(tx.getSha256Hash().toString());
			}
		}

		for (Transaction tx : this.walletMap.get(coinId).getTransactionsByTime()) {
			if (pendingTxHashes == null || (pendingTxHashes != null && 
					!pendingTxHashes.contains(tx.getHashAsString()))) {
				OWTransaction owTx = bitcoinjTransactionToOWTransaction(coinId, tx);
				ZLog.log("pending id: " + owTx.getId());
				confirmedTransactions.add(owTx);
			}
		}

		// TODO when ready, insert txs into database

		return confirmedTransactions;
	}

	/**
	 * A quick temporary converter method. Makes a OWWalletTransaction that is mainly
	 * just good for use in views since it doesn't have any of the database fields set.
	 * 
	 * @param coinId
	 * @param tx
	 * @return
	 */
	private OWTransaction bitcoinjTransactionToOWTransaction(OWCoin coinId, Transaction tx) {

		OWTransaction owTx = this.readTransactionByHash(coinId, tx.getHashAsString());

		if (owTx != null) {
			this.updateTransactionNumConfirmations(owTx);
			return owTx;
		}

		owTx = new OWTransaction(
				coinId, 
				OWFiat.USD, 
				tx.getHashAsString().substring(0, 6), 
				tx.getUpdateTime().getTime() / 1000,
				tx.getValue(this.walletMap.get(coinId)),
				OWWalletTransactionListAdapter.Type.TRANSACTION,
				R.layout.accounts_wallet_tx_list_item);
		owTx.setSha256Hash(new OWSha256Hash(tx.getHash().toString()));

		List<String> addressStrings = new ArrayList<String>();;

		for (TransactionOutput to : tx.getOutputs()) {
			addressStrings.add(to.getScriptPubKey().getToAddress(coinId.getNetworkParameters()).toString());
		}

		// Here we take the addresses that were used in the tx and only get the ones
		// that were stored in the db. We need to do this because, for example, 
		// if a tx is sent to us but has a change address (that isn't in our db) then we
		// don't need that change address to be shown/saved.
		boolean receiving = tx.getValue(this.getWallet(coinId)).compareTo(BigInteger.ZERO) > 0;
		List<OWAddress> addresses = null;
		if (receiving) {
			addresses = this.readReceivingAddresses(coinId, addressStrings);
		} else {
			addresses = this.readSendingAddresses(coinId, addressStrings);
		}

		for (OWAddress a : addresses) {
			owTx.addDisplayAddress(a.toString());
		}

		// TODO get this from settings
		owTx.setFiatType(OWFiat.USD);

		// The number of confirmations from bitcoin j.
		owTx.setNumConfirmations(tx.getConfidence().getDepthInBlocks());

		owTx.setTxAmount(tx.getValue(this.getWallet(coinId)));
		// TODO not right, but can't get it from bitcoinj apparently
		owTx.setTxFee(BigInteger.ZERO);

		// autofill the note of the transaction as the note of the first non-empty address
		if (addresses == null || addresses.size() == 0) {
			owTx.setTxNote(tx.getHashAsString().substring(0, 6));
		} else {
			String note = null;
			for (OWAddress a : addresses) {
				if (a.getLabel() != null && !a.getLabel().trim().isEmpty()) {
					note = a.getLabel();
					break;
				}
			}
			if (note != null) {
				owTx.setTxNote(note);
			} else {
				owTx.setTxNote(tx.getHashAsString().substring(0, 6));
			}
		}

		// Get the tx's time
		owTx.setTxTime(tx.getUpdateTime().getTime() / 1000);

		return owTx;
	}

	//	private OWAddress bitcoinjAddressToOWAddress(OWCoin coinId, Address address) {
	//		OWAddress owAddress = null;
	//		try {
	//			owAddress = new OWAddress(coinId, (byte) (address.getVersion()&0xff), address.getHash160());
	//		} catch (OWAddressFormatException e) {
	//			// TODO Auto-generated catch block
	//			log("Address could not be converted correctly");
	//			e.printStackTrace();
	//		}
	//		return owAddress;
	//	}

	/**
	 * A quick temporary converter method.
	 * 
	 * @param coinId
	 * @param tx
	 * @return
	 */
	private ECKey owKeytoBitcoinjKey(OWECKey ziftrKey) {
		return new ECKey(new BigInteger(1, ziftrKey.getPrivKeyBytes()), ziftrKey.getPubKey(), ziftrKey.isCompressed());
	}

	/**
	 * @param activity the context to set
	 */
	public void setActivity(OWMainFragmentActivity activity) {
		this.activity = activity;
	}

	/**
	 * Gives the wallet for the specified coin type or null
	 * if the wallet has not been set up yet. 
	 * 
	 * @param id - The coin type to get the wallet for. 
	 * @return as above
	 */
	public Wallet getWallet(OWCoin id) {
		return this.walletMap.get(id);
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

}