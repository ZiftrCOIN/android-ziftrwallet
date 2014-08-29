package com.ziftr.android.onewallet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.CheckpointManager;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.PeerAddress;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.SPVBlockStore;
import com.google.bitcoin.store.UnreadableWalletException;
import com.ziftr.android.onewallet.dialog.OWSimpleAlertDialog;
import com.ziftr.android.onewallet.sqlite.OWSQLiteOpenHelper;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWUtils;
import com.ziftr.android.onewallet.util.ZLog;

/** 
 * This class controls all of the wallets and is responsible
 * for setting them up and closing them down when the application exits.
 * 
 * TODO maybe this class should be a background fragment? Ask Justin. 
 */
public class OWWalletManager {

	/** The map which holds all of the wallets. */
	private Map<OWCoin.Type, Wallet> walletMap = new HashMap<OWCoin.Type, Wallet>();

	/** The wallet files for each of the coin types. */
	private Map<OWCoin.Type, File> walletFiles = new HashMap<OWCoin.Type, File>();

	/** The file where the BlockStore for this coin will be stored. */
	private Map<OWCoin.Type, File> blockStoreFiles = new HashMap<OWCoin.Type, File>();

	/** A map of hashes to StoredBlocks to save StoredBlock objects to disk. */
	private Map<OWCoin.Type, BlockStore> blockStores = 
			new HashMap<OWCoin.Type, BlockStore>();

	/** The PeerGroup that this SPV node is connected to. */
	private Map<OWCoin.Type, PeerGroup> peerGroups = 
			new HashMap<OWCoin.Type, PeerGroup>();

	/** 
	 * When the app is started it searches through all of these and looks for 
	 * any wallet files that alread exists. If they do, it sets them up.  
	 */
	private OWCoin.Type[] enabledCoinTypes = new OWCoin.Type[] {
			OWCoin.Type.BTC_TEST
	};

	/** 
	 * This should be cleared when saving this object 
	 * in {@link OWMainFragmentActivity}. 
	 * 
	 * TODO maybe we want to get this everytime?
	 * or maybe a weak reference? 
	 */
	private OWMainFragmentActivity activity;

	/**
	 * Make a new manager. This context should be cleared and then re-added when
	 * saving and bringing back the wallet manager. 
	 */
	public OWWalletManager(OWMainFragmentActivity activity) {
		this.activity = activity;

		for (OWCoin.Type enabledType : enabledCoinTypes) {
			if (this.userHasCreatedWalletBefore(enabledType)) {
				// TODO see java docs for this method. Is it okay to 
				// have this called here? 
				this.setUpWallet(enabledType);
			}
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
	public boolean walletHasBeenSetUp(OWCoin.Type id) {
		// There is no add wallet method, so the only way
		// a wallet can be added to the map is by setupWallet.
		return walletMap.get(id) != null;
	}

	/** 
	 * Returns a boolean describing whether the user has created a wallet
	 * of the specified type before. This is not session specific as wallet.dat 
	 * files (per bitcoinj) are saved on the device when wallets are created and
	 * these files will still exist whether the wallet has been set up yet or not.
	 *  
	 * @param id - The type of coin wallet to test for
	 * @return as above
	 */
	public boolean userHasCreatedWalletBefore(OWCoin.Type id) {
		if (this.walletHasBeenSetUp(id)) {
			return true;
		}

		File externalDirectory = this.activity.getExternalFilesDir(null);
		return (new File(externalDirectory, id.getShortTitle() + "_wallet.dat")).exists();
	}

	/**
	 * Gives a list of all the coin types for which the user has created
	 * a wallet before. i.e. all wallet types for which userHasCreatedWalletBefore()
	 * will return true.
	 * 
	 * @return as above
	 */
	public List<OWCoin.Type> getAllUsersWalletTypes() {
		ArrayList<OWCoin.Type> list = new ArrayList<OWCoin.Type>();
		for (OWCoin.Type type : OWCoin.Type.values()) {
			if (this.userHasCreatedWalletBefore(type)) {
				list.add(type);
			}
		}
		return list;
	}

	/**
	 * Gives a list of all the coin types for which the user has set up a wallet
	 * this session. i.e. all wallet types for which walletHasBeenSetUp()
	 * will return true.
	 * 
	 * @return as above
	 */
	public List<OWCoin.Type> getAllSetupWalletTypes() {
		ArrayList<OWCoin.Type> list = new ArrayList<OWCoin.Type>();
		for (OWCoin.Type type : OWCoin.Type.values()) {
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
		for (OWCoin.Type type : this.getAllSetupWalletTypes()) {
			this.closeWallet(type);
		}
	}

	/**
	 * @return the activity
	 */
	public OWMainFragmentActivity getActivity() {
		return activity;
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
	public Wallet getWallet(OWCoin.Type id) {
		return this.walletMap.get(id);
	}

	/** 
	 * Gets the file where the wallet for the specified
	 * coin type is stored. 
	 * 
	 * @param id - The coin type of the wallet file to get
	 * @return as above
	 */
	public File getWalletFile(OWCoin.Type id) {
		return this.walletFiles.get(id);
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
	public boolean setUpWallet(OWCoin.Type id) {

		// Set up the SQLite tables
		OWSQLiteOpenHelper database = this.getActivity().getDatabaseHelper();
		database.ensureCoinTypeActivated(id);

		// Here we recreate the files or create them if this is the first
		// time the user opens the app.
		this.walletFiles.put(id, null);

		// This is application specific storage, will be deleted when app is uninstalled.
		File externalDirectory = this.activity.getExternalFilesDir(null);
		if (externalDirectory != null) {
			this.walletFiles.put(id, new File(
					externalDirectory, id.getShortTitle() + "_wallet.dat"));
		} else {
			ZLog.log("null NULL EXTERNAL DIR");
			OWSimpleAlertDialog alertUserDialog = new OWSimpleAlertDialog();
			alertUserDialog.setupDialog("OneWallet", "Error: No external storage detected.", null, "OK", null);
			alertUserDialog.show(this.activity.getSupportFragmentManager(), "null_externalDirectory");
			return false;
		}

		this.walletMap.put(id, null);

		// Try to load the wallet from a file
		try {
			this.walletMap.put(id, Wallet.loadFromFile(this.walletFiles.get(id)));
		} catch (UnreadableWalletException e) {
			ZLog.log("Exception trying to load wallet file: ", e);
		}

		// If the load was unsucecssful (presumably only if this is the first 
		// time this type of wallet was set up) then we make a new wallet and add
		// a new key to the wallet.
		if (this.walletMap.get(id) == null) {
			this.walletMap.put(id, new Wallet(id.getNetworkParameters()));
			try {
				this.walletMap.get(id).addKey(new ECKey());
				this.walletMap.get(id).saveToFile(this.walletFiles.get(id));
			} catch (IOException e) {
				ZLog.log("Exception trying to save new wallet file: ", e);
				// TODO this is just test code of course but this is probably 
				// "fatal" as if we can't save our wallet, that's a problem
				return false;
			}
		}

		// How to watch an outside address, note: we probably 
		// won't do this client side in the actual app
		//		Address watchedAddress;
		//		try {
		//			watchedAddress = new Address(networkParams, 
		//					"mrbgTx73gQiu8oHJfSadFHYQose3Arj3Db");
		//			wallet.addWatchedAddress(watchedAddress, 1402200000);
		//		} catch (AddressFormatException e) {
		//			ZLog.log("Exception trying to add a watched address: ", e);
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

	public void closeWallet(OWCoin.Type id) {
		if (peerGroups.get(id) != null) {
			peerGroups.get(id).stop();
		}

		// Close the blockstore because we are no longer going to be receiving
		// and storing blocks
		if (blockStores.get(id) != null) {
			try {
				blockStores.get(id).close();
			} catch (BlockStoreException e) {
				ZLog.log("Exception closing block store: ", e);
			}
		}

		// Save the wallet to the file. Not that this will save the wallet as either
		// encrypted or decrypted, depending on which was set last using the encrypt
		// or decrypt methods. 
		if (walletMap.get(id) != null) {
			try {
				walletMap.get(id).saveToFile(walletFiles.get(id));
			} catch (IOException e) {
				ZLog.log("Exception saving wallet file on shutdown: ", e);
			}
		}
	}

	/**
	 * 
	 */
	private void beginSyncWithNetwork(final OWCoin.Type id) {
		OWUtils.runOnNewThread(new Runnable() {
			@Override
			public void run() {

				try {
					ZLog.log("RUNNING");

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
							ZLog.log("activity was null and we returned early.");
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
							ZLog.log("Failed to load chain checkpoint: ", e);
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

						for(Transaction tx : pendingTransactions) {
							//ZLog.log("There is a pending transaction "
							//		+ "in the wallet: ", tx.toString(chain));
							//wallet.commitTx(tx);

							try {
								//wallet.commitTx(tx);
								//wallet.cleanup();

								if (!walletMap.get(id).isConsistent()) {
									ZLog.log("Wallet is inconsistent!!");
								}

								if (walletMap.get(id).isTransactionRisky(tx, null)) {
									ZLog.log("Transaction is risky...");
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

								ZLog.log(logMsg);

								//wallet.clearTransactions(0); //for testing... drop a bomb on our wallet

							} catch(Exception e) {
								ZLog.log("Caught Excpetion: ", e);
							}

							try {
								//peerGroup.broadcastTransaction(tx);
							} catch(Exception e) {
								ZLog.log("Caught Excpetion: ", e);
							}

						} 
					}

					/**
		        List<Transaction> recentTransactions = wallet.getRecentTransactions(20, true);
		        for(Transaction tx : recentTransactions) {
		        	ZLog.log("Recent transaction: ", tx);
		        }
					 ***/

				} catch (BlockStoreException e) {
					ZLog.log("Exeption creating block store: ", e);
				} 

				ZLog.log("STOPPING");

			}
		});
	}

}