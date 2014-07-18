package com.ziftr.android.onewallet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.store.UnreadableWalletException;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.ZLog;

public class OWWalletManager {
	
	/** The map which holds all of the wallets. */
	private Map<OWCoin.Type, Wallet> walletMap = new HashMap<OWCoin.Type, Wallet>();
	
	/** The wallet files for each of the coin types. */
	private Map<OWCoin.Type, File> walletFiles = new HashMap<OWCoin.Type, File>();
	
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
	 */
	private Context context;
	
	/**
	 * Make a new manager. This context should be cleared and then re-added when
	 * saving and bringing back the wallet manager. 
	 */
	public OWWalletManager(Context context) {
		this.context = context;
		
		for (OWCoin.Type enabledType : enabledCoinTypes) {
			if (this.userHasCreatedWalletBefore(enabledType)) {
				this.setupWallet(enabledType);
			}
		}
	}
	
	public Wallet getWallet(OWCoin.Type id) {
		return this.walletMap.get(id);
	}
	
	public boolean walletHasBeenSetUp(OWCoin.Type id) {
		// There is no add wallet method, so the only way
		// a wallet can be added to the map is by setupWallet.
		return walletMap.get(id) != null;
	}
	
	public boolean userHasCreatedWalletBefore(OWCoin.Type id) {
		if (this.walletHasBeenSetUp(id)) {
			return true;
		}
		
		File externalDirectory = this.context.getExternalFilesDir(null);
		return (new File(externalDirectory, id.getShortTitle() + "_wallet.dat")).exists();
	}
	
	public File getWalletFile(OWCoin.Type id) {
		return this.walletFiles.get(id);
	}
	
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
	 * @return the context
	 */
	public Context getContext() {
		return context;
	}

	/**
	 * @param context the context to set
	 */
	public void setContext(Context context) {
		this.context = context;
	}
	
	/**
	 * Setup a wallet object for this fragment by either loading it 
	 * from an existing stored file or by creating a new one from 
	 * network parameters. Note, though Android won't crash out because 
	 * of it, this shouldn't be called from a UI thread due to the 
	 * blocking nature of creating files.
	 */
	public void setupWallet(OWCoin.Type id) {

		// Here we recreate the files or create them if this is the first
		// time the user opens the app.
		this.walletFiles.put(id, null);

		// This is application specific storage, will be deleted when app is uninstalled.
		File externalDirectory = this.context.getExternalFilesDir(null);
		if (externalDirectory != null) {
			this.walletFiles.put(id, new File(
					externalDirectory, id.getShortTitle() + "_wallet.dat"));
		} else {
			ZLog.log("CHECK ON THIS, external directory was null.");
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
				return;
				// TODO this is just test code of course but this is probably 
				// "fatal" as if we can't save our wallet, that's a problem
				//
				// Just kill the activity for testing purposes
				//super.onBackPressed(); 
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

		// if (!wallet.isEncrypted()) {
		//     get a password from the user and encrypt
		// }

		//		ZLog.log("Test Wallet: ", wallet.toString());
		//		ZLog.log("Earliest wallet creation time: ", String.valueOf(
		//				wallet.getEarliestKeyCreationTime()));
		//		ZLog.log("Wallet keys: ", wallet.getKeys().toString());
		
		

	}
	
//	private beginSyncWithNetwork(final OWCoin.Type id) {
//		OWUtils.runOnNewThread(new Runnable() {
//			@Override
//			public void run() {
//
//				try {
//					boolean blockStoreExists = blockStoreExists(id);
//					// This creates the file if it doesn't exist, so check first
//					blockStore = new SPVBlockStore(
//							networkParams, blockStoreFile); 
//					if (!blockStoreExists) {
//						// We're creating a new block store file here, so 
//						// use checkpoints to speed up network syncing
//						if (context == null) {
//							// If this happens app is dying, so wait and 
//							// do this on the next run
//							return; 
//						}
//
//						InputStream inputSteam = context.getResources(
//								).openRawResource(R.raw.btc_checkpoints);
//						try {
//							CheckpointManager.checkpoint(
//									networkParams, inputSteam, blockStore, 
//									wallet.getEarliestKeyCreationTime());
//						} catch (IOException e) {
//							// We failed to read the checkpoints file 
//							// for some reason, still let the user 
//							// attempt sync the chain the slow way
//							ZLog.log("Failed to load chain checkpoint: ", e);
//						}
//					}
//
//					BlockChain chain = new BlockChain(
//							networkParams, wallet, blockStore);
//
//					peerGroup = new PeerGroup(networkParams, chain);
//
//					peerGroup.setUserAgent("OneWallet", "1.0");
//
//
//					// So the wallet receives broadcast transactions.
//					peerGroup.addWallet(wallet);
//
//					peerGroup.addPeerDiscovery(new DnsDiscovery(networkParams));
//
//
//					//peerGroup.setMaxConnections(25);
//
//					// This is for running local version of bitcoin 
//					// network for testing
//					//peerGroup.addAddress(InetAddress.getLocalHost()); 
//
//					// This starts the connecting with peers on a new thread
//					peerGroup.start();
//
//					ZLog.log("Connected peers: ", 
//							peerGroup.getConnectedPeers());
//
//					// Note that instead of using the anonymous inner class 
//					// based, we could use another class which implements 
//					// WalletEventListener, however that requires setting up a 
//					// bunch of methods we don't need for this simple test
//					wallet.addEventListener(new AbstractWalletEventListener() {
//						@Override
//						public synchronized void onCoinsReceived(
//								Wallet w, 
//								Transaction tx, 
//								BigInteger prevBalance, 
//								final BigInteger newBalance) {
//
//							try {
//								// Every time the wallet changes, we save it to the file
//								wallet.saveToFile(walletFile);
//							} catch (IOException e) {
//								// TODO what to do if this fails? 
//								ZLog.log("Failed to save updated wallet balance: ", e);
//							}
//
//							// Now update the UI with the new balance
//							Activity activity = getActivity();
//							if (activity != null) {
//								activity.runOnUiThread(new Runnable() {
//									public void run() {
//										updateWalletBalance(newBalance);
//									}
//								});
//							}
//						}
//					});
//
//
//					// Now download and process the block chain.
//					peerGroup.downloadBlockChain();
//
//					// TODO these are not safe because other threads can 
//					// modify the transaction collections while we iterate 
//					// through them, however this is just for testing right now
//
//					//also upload any pending transactions
//					Collection<Transaction> pendingTransactions = 
//							wallet.getPendingTransactions();
//					wallet.getKeyCrypter();
//
//					synchronized (pendingTransactions) {
//
//						for(Transaction tx : pendingTransactions) {
//							//ZLog.log("There is a pending transaction "
//							//		+ "in the wallet: ", tx.toString(chain));
//							//wallet.commitTx(tx);
//
//							try {
//								//wallet.commitTx(tx);
//								//wallet.cleanup();
//
//								if (!wallet.isConsistent()) {
//									ZLog.log("Wallet is inconsistent!!");
//								}
//
//								if (wallet.isTransactionRisky(tx, null)) {
//									ZLog.log("Transaction is risky...");
//								}
//
//								String logMsg = "Pending Transaction Details: \n";
//								logMsg += "isPending: " + String.valueOf(
//										tx.isPending()) +"\n";
//								logMsg += "isMature: " + String.valueOf(
//										tx.isMature()) + "\n";
//								logMsg += "isAnyOutputSpent: " + String.valueOf(
//										tx.isAnyOutputSpent()) + "\n";
//								logMsg += "sigOpCount: " + String.valueOf(
//										tx.getSigOpCount()) +"\n";
//
//								TransactionConfidence confidence = 
//										tx.getConfidence();
//
//								logMsg += "Confidence: \n" + 
//										confidence.toString() + "\n";
//
//								logMsg += "Confidence Type: " + String.valueOf(
//										confidence.getConfidenceType().getValue()) 
//										+ "\n";
//								logMsg += "Confidence Block Depth: " + 
//										String.valueOf(confidence.getDepthInBlocks(
//												)) + "\n";
//								logMsg += "Number Broadcast Peers: " + 
//										String.valueOf(confidence.numBroadcastPeers(
//												))+"\n";
//
//								String broadcastByString = "none";
//								ListIterator<PeerAddress> broadcastBy = 
//										confidence.getBroadcastBy();
//								if (broadcastBy != null) {
//									broadcastByString = "";
//									while(broadcastBy.hasNext()) {
//										broadcastByString += broadcastBy.next().toString() + "\n";
//									}
//								}
//
//								logMsg += "Broadcast by Peers: \n" + broadcastByString + "\n";
//								logMsg += "Confidence Source: " + confidence.getSource().name() +"\n";
//
//
//
//								logMsg += "version: " + String.valueOf(tx.getVersion()) +"\n";
//
//								String hashes = "null";
//								Map<Sha256Hash, Integer> hashesTransactionAppearsIn = tx.getAppearsInHashes();
//								if (hashesTransactionAppearsIn != null) {
//									hashes = hashesTransactionAppearsIn.toString();
//								}
//
//								logMsg += "Transaction Appears in Hashes: \n" + hashes +"\n";
//
//								logMsg += "Transaction Hash: " + tx.getHashAsString() + "\n";
//
//								logMsg += "Transaction Update Time: " + tx.getUpdateTime().toString() +"\n";
//
//								logMsg += "Transaction Value: " + String.valueOf(tx.getValue(wallet)) + "\n";
//
//								ZLog.log(logMsg);
//
//								//wallet.clearTransactions(0); //for testing... drop a bomb on our wallet
//
//							} catch(Exception e) {
//								ZLog.log("Caught Excpetion: ", e);
//							}
//
//							try {
//								//peerGroup.broadcastTransaction(tx);
//							} catch(Exception e) {
//								ZLog.log("Caught Excpetion: ", e);
//							}
//
//						} 
//					}
//
//					/**
//			        List<Transaction> recentTransactions = wallet.getRecentTransactions(20, true);
//			        for(Transaction tx : recentTransactions) {
//			        	ZLog.log("Recent transaction: ", tx);
//			        }
//					 ***/
//
//				} catch (BlockStoreException e) {
//					ZLog.log("Exeption creating block store: ", e);
//				} 
//
//			}
//		});
//	}
	
}