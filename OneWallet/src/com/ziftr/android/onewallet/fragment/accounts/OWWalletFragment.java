package com.ziftr.android.onewallet.fragment.accounts;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.bitcoin.core.AbstractWalletEventListener;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.utils.BriefLogFormatter;
import com.ziftr.android.onewallet.OWMainFragmentActivity;
import com.ziftr.android.onewallet.OWWalletManager;
import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWConverter;
import com.ziftr.android.onewallet.util.OWFiat;
import com.ziftr.android.onewallet.util.OWUtils;
import com.ziftr.android.onewallet.util.ZLog;

/**
 * This is the abstract superclass for all of the individual Wallet type
 * Fragments. It provides some generic methods that will be useful
 * to all Fragments of the OneWallet. 
 * 
 * TODO in the evenListener for the search bar, make a call to notifyDatasetChanged
 * so that all the transactions that don't match are filtered out. 
 */
public abstract class OWWalletFragment extends Fragment {

	/** The root view for this application. */
	private View rootView;

	private ListView txListView;

	private List<OWWalletTransactionListItem> txList;

	/**
	 * Get the market exchange prefix for the 
	 * actual sub-classing coin fragment type.
	 * 
	 * @return "BTC" for Bitcoin, "LTC" for Litecoin, etc.
	 */
	public abstract OWCoin.Type getCoinId();

	/**
	 * When this fragment is created, this method is called
	 * unless it is overridden. 
	 * 
	 * @param savedInstanceState - The last state of this fragment.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	/**
	 * When the view is created, we must initialize the header, the list
	 * view with all of the transactions in it, and the buttons at the
	 * bottom of the screen. 
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {

		BriefLogFormatter.initVerbose();

		if (getWallet() == null) {
			throw new IllegalArgumentException(
					"Shouldn't happen, this is just a safety check because we "
							+ "need a wallet to send coins.");
		}

		rootView = inflater.inflate(R.layout.accounts_wallet, container, false);

		this.initializeWalletHeaderView();

		this.initializeTxListView();

		this.initializeButtons();

		return this.rootView;
	}

	//	private File getWalletFile() {
	//		OWWalletManager m = ((OWMainFragmentActivity) this.getActivity()
	//				).getWalletManager();
	//		if (m != null) {
	//			return m.getWalletFile(getCoinId());
	//		}
	//		return null;
	//	}

	//	private void updateWalletBalance(BigInteger newBalance) {
	//		if (walletBalanceText != null) {
	//			//			String balanceString = 
	//			//					Utils.bitcoinValueToFriendlyString(newBalance);
	//
	//			walletBalanceText.setText((new BigDecimal(newBalance, 8)).toPlainString());
	//
	//			String watchedBalance = 
	//					Utils.bitcoinValueToFriendlyString(getWallet().getWatchedBalance());
	//			outsideBalanceText.setText(watchedBalance);
	//		}
	//	}


	/*
	 * When this fragment is being destroyed we need to shut
	 * down all of the wallet stuff and make sure the wallet is
	 * saved to a file.
	 */
	//	@Override
	//	public void onDestroy() {
	// If the thread that is connecting with peers is still working,
	// then we stop it here
	//		if (peerGroup != null) {
	//			peerGroup.stop();
	//		}
	//
	//		// Close the blockstore because we are no longer going to be receiving
	//		// and storing blocks
	//		if (blockStore != null) {
	//			try {
	//				blockStore.close();
	//			} catch (BlockStoreException e) {
	//				ZLog.log("Exception closing block store: ", e);
	//			}
	//		}
	//
	//		// Save the wallet to the file. Not that this will save the wallet as either
	//		// encrypted or decrypted, depending on which was set last using the encrypt
	//		// or decrypt methods. 
	//		if (wallet != null) {
	//			try {
	//				wallet.saveToFile(walletFile);
	//			} catch (IOException e) {
	//				ZLog.log("Exception saving wallet file on shutdown: ", e);
	//			}
	//		}
	//
	//	super.onDestroy();
	//
	//}

	private void initializeWalletHeaderView() {
		View headerView = this.rootView.findViewById(R.id.walletHeader);

		ImageView coinLogo = (ImageView) (headerView.findViewById(R.id.leftIcon));
		Drawable coinImage = this.getActivity().getResources().getDrawable(
				getCoinId().getLogoResId());
		coinLogo.setImageDrawable(coinImage);

		TextView coinTitle = (TextView) headerView.findViewById(R.id.topLeftTextView);
		coinTitle.setText(getCoinId().getLongTitle());

		TextView coinUnitPriceInFiatTextView = (TextView) 
				headerView.findViewById(R.id.bottomLeftTextView);
		BigDecimal unitPriceInFiat = OWConverter.convert(
				BigDecimal.ONE, getCoinId(), OWFiat.Type.USD);
		coinUnitPriceInFiatTextView.setText(OWFiat.Type.USD.getSymbol() + 
				OWFiat.formatFiatAmount(OWFiat.Type.USD, 
						unitPriceInFiat).toPlainString());

		TextView walletBalanceTextView = (TextView) 
				headerView.findViewById(R.id.topRightTextView);
		BigDecimal walletBallance = OWUtils.bigIntToBigDec(getCoinId(), 
				getWallet().getBalance(Wallet.BalanceType.ESTIMATED));
		walletBalanceTextView.setText(
				OWCoin.formatCoinAmount(getCoinId(), walletBallance).toPlainString());

		TextView walletBalanceFiatEquivTextView = (TextView) 
				headerView.findViewById(R.id.bottomRightTextView);
		BigDecimal walletBalanceFiatEquiv = OWConverter.convert(
				walletBallance, getCoinId(), OWFiat.Type.USD);
		walletBalanceFiatEquivTextView.setText(OWFiat.Type.USD.getSymbol() + 
				walletBalanceFiatEquiv.toPlainString());

		ImageView marketIcon = (ImageView) (headerView.findViewById(R.id.rightIcon));
		Drawable marketImage = this.getActivity().getResources().getDrawable(
				R.drawable.stats_up);
		marketIcon.setImageDrawable(marketImage);
	}

	private void initializeTxListView() {
		this.txList = new ArrayList<OWWalletTransactionListItem>();
		// Add the Pending Divider
		this.txList.add(new OWWalletTransactionListItem(null, null, "PENDING", null, null, 
				false, R.layout.accounts_wallet_tx_list_divider));
		// Add all the pending transactions
		for (Transaction tx : this.getWallet().getPendingTransactions()) {
			this.txList.add(new OWWalletTransactionListItem(
					getCoinId(), 
					OWFiat.Type.USD, 
					tx.getHashAsString(), 
					OWUtils.formatter.format(tx.getUpdateTime()),
					OWUtils.bigIntToBigDec(getCoinId(), tx.getValue(getWallet())), 
					true, 
					R.layout.accounts_wallet_tx_list_item));

		}
		// Add the History Divider
		this.txList.add(new OWWalletTransactionListItem(null, null, "HISTORY", null, null, 
				false, R.layout.accounts_wallet_tx_list_divider));
		// Add all the pending transactions
		for (Transaction tx : this.getWallet().getTransactionsByTime()) {
			this.txList.add(new OWWalletTransactionListItem(
					getCoinId(), 
					OWFiat.Type.USD, 
					tx.getHashAsString(), 
					OWUtils.formatter.format(tx.getUpdateTime()),
					OWUtils.bigIntToBigDec(getCoinId(), tx.getValue(getWallet())), 
					false, 
					R.layout.accounts_wallet_tx_list_item));
		}

		this.txListView = (ListView)
				this.rootView.findViewById(R.id.txListView);
		this.txListView.setAdapter(new OWWalletTransactionListAdapter(
				this.getActivity(), this.txList));

		this.getWallet().addEventListener(new AbstractWalletEventListener() {
			@Override
			public synchronized void onCoinsReceived(
					Wallet w, 
					Transaction tx, 
					BigInteger prevBalance, 
					final BigInteger newBalance) {
				// TODO need to update the data in the list of transactions
				ZLog.log("coins received...d..d");
			}

			// TODO override more methods here to do things like changing 
			// transactions from pending to finalized, etc. 
		});
	}

	private void initializeButtons() {

		//		// The transaction that will take place to show the new fragment
		//		FragmentTransaction transaction = 
		//				this.getActivity().getSupportFragmentManager().beginTransaction();
		//
		//		// TODO add animation to transaciton here
		//
		//		// Make the new fragment of the correct type
		//		Fragment fragToShow = this.getActivity().getSupportFragmentManager(
		//				).findFragmentByTag(this.getCoinId().getShortTitle() + 
		//						"_send_coins_fragment");
		//		if (fragToShow == null) {
		//			// If the fragment doesn't exist yet, make a new one
		//			fragToShow = new OWSendBitcoinTestnetCoinsFragment();
		//		} else if (fragToShow.isVisible()) {
		//			// If the fragment is already visible, no need to do anything
		//			return;
		//		}
		//
		//		transaction.replace(R.id.oneWalletBaseFragmentHolder, 
		//				fragToShow, "send_coins_fragment");
		//		transaction.addToBackStack(null);
		//		transaction.commit();


		Button sendButton = (Button) 
				this.rootView.findViewById(R.id.leftButton);
		sendButton.setText("SEND");
		sendButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO do something when we click send
			}
		});

		Button receiveButton = (Button) 
				this.rootView.findViewById(R.id.rightButton);
		receiveButton.setText("RECEIVE");
		receiveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO do something when we click receive
			}
		});
	}

	private Wallet getWallet() {
		OWWalletManager m = ((OWMainFragmentActivity) this.getActivity()
				).getWalletManager();
		if (m != null) {
			return m.getWallet(getCoinId());
		}

		return null;
	}

	//	private File getWalletFile() {
	//		OWWalletManager m = ((OWMainFragmentActivity) this.getActivity()
	//				).getWalletManager();
	//		if (m != null) {
	//			return m.getWalletFile(getCoinId());
	//		}
	//		return null;
	//	}

	//	private void updateWalletBalance(BigInteger newBalance) {
	//		if (walletBalanceText != null) {
	//			//			String balanceString = 
	//			//					Utils.bitcoinValueToFriendlyString(newBalance);
	//
	//			walletBalanceText.setText((new BigDecimal(newBalance, 8)).toPlainString());
	//
	//			String watchedBalance = 
	//					Utils.bitcoinValueToFriendlyString(getWallet().getWatchedBalance());
	//			outsideBalanceText.setText(watchedBalance);
	//		}
	//	}


	//	/**
	//	 * 
	//	 */
	//	private void beginSyncWithNetwork() {
	//		ZLog.log("");
	//		ZLog.log("Sync wallet with network");
	//		ZLog.log("");
	//		
	//		// TODO should all of this actually go into the wallet manager? 
	//		
	//		
	//		OWUtils.runOnNewThread(new Runnable() {
	//			@Override
	//			public void run() {
	//
	//				try {
	//					ZLog.log("RUNNING");
	//					File externalDirectory = getActivity().getExternalFilesDir(null);
	//					blockStoreFile = new File(externalDirectory, 
	//							getCoinId().getShortTitle() + "_blockchain.store");
	//					
	//					boolean blockStoreExists = blockStoreFile.exists();
	//					// This creates the file if it doesn't exist, so check first
	//					blockStore = new SPVBlockStore(
	//							networkParams, blockStoreFile); 
	//
	//					if (!blockStoreExists) {
	//						// We're creating a new block store file here, so 
	//						// use checkpoints to speed up network syncing
	//						Activity act = getActivity();
	//						if (act == null) {
	//							// If this happens app is dying, so wait and 
	//							// do this on the next run
	//							return; 
	//						}
	//
	//						InputStream inputSteam = act.getResources(
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
	//		        List<Transaction> recentTransactions = wallet.getRecentTransactions(20, true);
	//		        for(Transaction tx : recentTransactions) {
	//		        	ZLog.log("Recent transaction: ", tx);
	//		        }
	//					 ***/
	//
	//				} catch (BlockStoreException e) {
	//					ZLog.log("Exeption creating block store: ", e);
	//				} 
	//				
	//				ZLog.log("STOPPING");
	//
	//			}
	//		});
	//	}

}
