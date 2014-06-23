package com.ziftr.android.onewallet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Collection;
import java.util.ListIterator;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.google.bitcoin.core.AbstractWalletEventListener;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.CheckpointManager;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.InsufficientMoneyException;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.PeerAddress;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.SPVBlockStore;
import com.google.bitcoin.store.UnreadableWalletException;
import com.google.bitcoin.utils.BriefLogFormatter;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * This is the abstract superclass for all of the individual Wallet type
 * Fragments. It provides some generic methods that will be useful
 * to all Fragments of the OneWallet. 
 */
public abstract class OWWalletFragment extends Fragment implements OnClickListener {

	/**
	 * Get the market exchange prefix for the 
	 * actual sub-classing coin fragment type.
	 * 
	 * @return "BTC" for Bitcoin, "LTC" for Litecoin, etc.
	 */
	public abstract String getCoinPrefix();
	
	/**
	 * Get the specific coin's network parameters. This 
	 * is an instance which selects the network (production 
	 * or test) you are on.
	 * 
	 * @return A network parameters object. 
	 * For example, to get the TestNetwork, use 
	 * <pre>'return TestNet3Params.get();'.</pre>
	 */
	public abstract NetworkParameters getCoinNetworkParameters();

	/** The root view for this application. */
	protected View rootView;
	/** The TextView to hold the Wallet's public address. */ 
	protected TextView publicAddressText;
	/** The TextView to hold the Wallet's current balance. */
	protected TextView walletBalanceText;
	/** The TextView to hold a different address' balance. */
	protected TextView outsideBalanceText;

	/** The file where the wallet for this coin will be stored. */
	protected File walletFile;
	/** The file where the BlockStore for this coin will be stored. */
	protected File blockStoreFile;

	/** The wallet for this coin. */
	protected Wallet wallet;
	/** The Network parameters for this coin. */
	protected NetworkParameters networkParams = this.getCoinNetworkParameters();
	/** A map of hashes to StoredBlocks to save StoredBlock objects to disk. */
	protected BlockStore blockStore;
	/** The PeerGroup that this SPV node is connected to. */
	protected PeerGroup peerGroup;

	/**
	 * When this fragment is created, this method is called
	 * unless it is overridden. 
	 * 
	 * @param savedInstanceState - The last state of this fragment.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupWallet();
		refreshWallet();
	}

	/**
	 * 
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		BriefLogFormatter.initVerbose();

		rootView = inflater.inflate(R.layout.fragment_wallet, container, false);

		View sendButton = rootView.findViewById(R.id.buttonSendCoins);
		sendButton.setOnClickListener(this);

		publicAddressText = (TextView) rootView.findViewById(R.id.textWalletMainAddress);
		walletBalanceText = (TextView) rootView.findViewById(R.id.textWalletMainBalance);
		outsideBalanceText = (TextView) rootView.findViewById(R.id.textWalletOutsideBalance);


		if(wallet != null) {	
			ECKey key = wallet.getKeys().get(0);
			publicAddressText.setText(key.toAddress(networkParams).toString());

			updateWalletBalance(wallet.getBalance(Wallet.BalanceType.AVAILABLE));
		}


		return rootView;


		// TODO
		// 1. Ability to generate new address upon user request
		// and be able to turn that into a QR code
		// 
		// 2. Get transaction history for all addresses in wallet and
		// be able to display them.
		// 
		// 3. Start making layouts.
		// 
		// 4. Organizing tasks that need to be done and appr. difficulty
		// 
		// 5. OW to start all of our classes.
		// 
		// 6. ZiftrUtils and Zlog for static useful methods.
		// ex. ZLog.log("aa", "b"); (get's exception message, as well)
		// also autotags comments with class name and shuts itself off at
		// launch time for release build.

	}

	@Override
	public void onClick(View v) {

		if(v.getId() == R.id.buttonSendCoins) {

			//for testing purposes at the moment, let's just always send 0.001 btc
			EditText receivingAddressEdit = (EditText) rootView.findViewById(R.id.editSendAddress);
			String address = receivingAddressEdit.getText().toString();

			try {
				sendCoins(address, Utils.toNanoCoins("0.00378"));
			} 
			catch (AddressFormatException e) {
				ZLog.log("Exception trying send coins: ", e);
			} 
			catch (InsufficientMoneyException e) {
				ZLog.log("Exception trying send coins: ", e);
			}
		}
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
	private void sendCoins(String address, BigInteger value) 
			throws AddressFormatException, InsufficientMoneyException {

		//create an address object based on network parameters in use and the entered address
		Address sendAddress = new Address(networkParams, address);

		//use the address and the value to create a new send request object
		Wallet.SendRequest sendRequest = Wallet.SendRequest.to(sendAddress, value);

		// Use the wallet to setup the send request's transaction, 
		// giving it proper outputs and a signed input
		//wallet.completeTx(sendRequest); //calling sendCoins internally calls this, so calling it here causes exceptions to be thrown

		//sendRequest.fee = new BigInteger("0"); //don't ever really want a 0 fee, just experimenting with setting fees

		//Transaction tx = sendRequest.tx;

		wallet.decrypt(null);

		Wallet.SendResult sendResult = wallet.sendCoins(sendRequest);
		sendResult.broadcastComplete.addListener(new Runnable() {

			@Override
			public void run() {
				ZLog.log("Successfully sent coins to address!");
			}
		}, MoreExecutors.sameThreadExecutor());

	}

	/**
	 * Setup a wallet object for this fragment by either loading it 
	 * from an existing store file or by creating a new one from 
	 * network parameters. Note, though Android won't crash out because 
	 * of it, this shouldn't be called from a UI thread due to the 
	 * blocking nature of creating files.
	 */
	private void setupWallet() {

		walletFile = null;
		File externalDirectory = getActivity().getExternalFilesDir(null);
		if(externalDirectory != null) {
			walletFile = new File(externalDirectory, getCoinPrefix() + "_wallet.dat");
			blockStoreFile = new File(externalDirectory, getCoinPrefix() + "_blockchain.store");
		}

		wallet = null;

		try {
			wallet = Wallet.loadFromFile(walletFile);
		} catch (UnreadableWalletException e) {
			ZLog.log("Exception trying to load wallet file: ", e);
		}

		// If this is the first time this type of wallet was set up
		if (wallet == null) {
			wallet = new Wallet(networkParams);
			try {
				wallet.addKey(new ECKey());
				wallet.saveToFile(walletFile);
			} catch (IOException e) {
				ZLog.log("Exception trying to save new wallet file: ", e);
				return;
				//TODO -this is just test code of course but this is probably "fatal" as if we can't save our wallet, that's a problem
				//super.onBackPressed(); //just kill the activity for testing purposes
			}
		}

		//how to watch an outside address, note: we probably won't do this client side in the actual app
		Address watchedAddress;
		try {
			watchedAddress = new Address(networkParams, 
					"mrbgTx73gQiu8oHJfSadFHYQose3Arj3Db");
			wallet.addWatchedAddress(watchedAddress, 1402200000);
		} catch (AddressFormatException e) {
			ZLog.log("Exception trying to add a watched address: ", e);
		}

		//KeyParameter keyParam = wallet.encrypt("password");
		//ZLog.log("Wallet locked with key parameter: ", keyParam);

		//wallet.decrypt(new KeyParameter("password".getBytes()));

		//ECKey key = new ECKey(null, null);



		//how to encrypt a wallet for safety
		//KeyParameter keyParam = wallet.encrypt("password");

		//how to decrypt the wallet
		//wallet.decrypt(wallet.getKeyCrypter().deriveKey("password"));

		//note: wallets should only be encrypted once using the password, decryption is specifically for removing encryption completely
		//instead any spending transactions should set SendRequest.aesKey and it will be used to decrypt keys as it signs.

		ZLog.log("Test Wallet: ", wallet.toString());
		ZLog.log("Earliest wallet creation time: ", String.valueOf(
				wallet.getEarliestKeyCreationTime()));
		ZLog.log("Wallet keys: ", wallet.getKeys().toString());

	}

	/**
	 * 
	 */
	private void refreshWallet() {

		ZiftrUtils.runOnNewThread(new Runnable() {

			@Override
			public void run() {

				try {

					boolean blockStoreExists = blockStoreFile.exists();
					// This creates the file if it doesn't exist, so check first
					blockStore = new SPVBlockStore(
							networkParams, blockStoreFile); 

					if(!blockStoreExists) {
						// We're creating a new block store file here, so 
						// use checkpoints to speed up network syncing
						Activity act = getActivity();
						if (act == null) {
							// If this happens app is dying, so wait and 
							// do this on the next run
							return; 
						}

						InputStream inputSteam = act.getResources(
								).openRawResource(R.raw.btc_checkpoints);
						try {
							CheckpointManager.checkpoint(
									networkParams, inputSteam, blockStore, 
									wallet.getEarliestKeyCreationTime());
						} catch (IOException e) {
							// We failed to read the checkpoints file 
							// for some reason, still let the user 
							// attempt sync the chain the slow way
							ZLog.log("Failed to load chain checkpoint: ", e);
						}
					}

					BlockChain chain = new BlockChain(
							networkParams, wallet, blockStore);

					peerGroup = new PeerGroup(networkParams, chain);


					// So the wallet receives broadcast transactions.
					peerGroup.addWallet(wallet);

					peerGroup.addPeerDiscovery(new DnsDiscovery(networkParams));
					//peerGroup.setMaxConnections(25);

					// This is for running local version of bitcoin 
					// network for testing
					//peerGroup.addAddress(InetAddress.getLocalHost()); 


					//peerGroup.downloadBlockChain();
					//peerGroup.awaitRunning();

					peerGroup.start();

					ZLog.log("Connected peers: ", 
							peerGroup.getConnectedPeers());

					// Note that instead of using the anonymous inner class 
					// based, we could use another class which implements 
					// WalletEventListener, however that requires setting up a 
					// bunch of methods we don't need for this simple test
					wallet.addEventListener(new AbstractWalletEventListener() {
						@Override
						public synchronized void onCoinsReceived(
								Wallet w, Transaction tx, 
								BigInteger prevBalance, 
								final BigInteger newBalance) {

							try {
								wallet.saveToFile(walletFile);
							} catch (IOException e) {
								ZLog.log("Failed to save "
										+ "updated wallet balance: ", e);
							}

							// Now update the UI with the new balance
							Activity activity = getActivity();
							if(activity != null) {
								activity.runOnUiThread(new Runnable() {
									public void run() {
										updateWalletBalance(newBalance);
									}
								});
							}
						}
					});


					// Now download and process the block chain.
					peerGroup.downloadBlockChain();

					// TODO these are not safe because other threads can 
					// modify the transaction collections while we iterate 
					// through them, however this is just for testing right now

					//also upload any pending transactions
					Collection<Transaction> pendingTransactions = 
							wallet.getPendingTransactions();

					for(Transaction tx : pendingTransactions) {
						//ZLog.log("There is a pending transaction "
						//		+ "in the wallet: ", tx.toString(chain));
						//wallet.commitTx(tx);

						try {
							//wallet.commitTx(tx);
							//wallet.cleanup();

							if(!wallet.isConsistent()) {
								ZLog.log("Wallet is inconsistent!!");
							}
							if(wallet.isTransactionRisky(tx, null)) {
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
							if(broadcastBy != null) {
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
							if(hashesTransactionAppearsIn != null) {
								hashes = hashesTransactionAppearsIn.toString();
							}

							logMsg += "Transaction Appears in Hashes: \n" + hashes +"\n";

							logMsg += "Transaction Hash: " + tx.getHashAsString() + "\n";

							logMsg += "Transaction Update Time: " + tx.getUpdateTime().toString() +"\n";

							logMsg += "Transaction Value: " + String.valueOf(tx.getValue(wallet)) + "\n";

							ZLog.log(logMsg);

							//wallet.clearTransactions(0); //for testing... drop a bomb on our wallet

						}
						catch(Exception e) {
							ZLog.log("Caught Excpetion: ", e);
						}

						try {
							//peerGroup.broadcastTransaction(tx);
						}
						catch(Exception e) {
							ZLog.log("Caught Excpetion: ", e);
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

			}
		});

	}

	private void updateWalletBalance(BigInteger newBalance) {
		if(walletBalanceText != null) {
			String balanceString = 
					Utils.bitcoinValueToFriendlyString(newBalance);
			walletBalanceText.setText(balanceString);

			String watchedBalance = 
					Utils.bitcoinValueToFriendlyString(wallet.getWatchedBalance());
			outsideBalanceText.setText(watchedBalance);
		}
	}


	@Override
	public void onDestroy() {

		if(peerGroup != null) {
			peerGroup.stop();
		}

		if(blockStore != null) {
			try {
				blockStore.close();
			} 
			catch (BlockStoreException e) {
				ZLog.log("Exception closing block store: ", e);
			}
		}

		if(wallet != null) {
			try {
				wallet.saveToFile(walletFile);
			}
			catch (IOException e) {
				ZLog.log("Exception saving wallet file on shutdown: ", e);
			}
		}

		super.onDestroy();

	}

}
