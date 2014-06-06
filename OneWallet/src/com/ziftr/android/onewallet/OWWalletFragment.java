package com.ziftr.android.onewallet;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

import com.google.bitcoin.core.AbstractWalletEventListener;
import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.SPVBlockStore;
import com.google.bitcoin.store.UnreadableWalletException;
import com.google.bitcoin.utils.BriefLogFormatter;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public abstract class OWWalletFragment extends Fragment {

	
	//abstract methods
	public abstract String getCoinPrefix();
	public abstract NetworkParameters getCoinNetworkParameters();
	
	View rootView;
	
	TextView publicAddressText;
	TextView walletBalanceText;
	
	File walletFile;
	File blockStoreFile;
	
	Wallet wallet;
	NetworkParameters networkParams = this.getCoinNetworkParameters();
	BlockStore blockStore;
	PeerGroup peerGroup;
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		BriefLogFormatter.initVerbose();
		
		rootView = inflater.inflate(R.layout.fragment_wallet, container, false);
		
		publicAddressText = (TextView) rootView.findViewById(R.id.textWalletMainAddress);
		walletBalanceText = (TextView) rootView.findViewById(R.id.textWalletMainBalance);
		
		walletFile = null;
		File externalDirectory = getActivity().getExternalFilesDir(null);
		if(externalDirectory != null) {
			walletFile = new File(externalDirectory, getCoinPrefix() + "_wallet.dat");
			blockStoreFile = new File(externalDirectory, getCoinPrefix() + "_blockchain.store");
		}
		
		setupWallet();
		
		if(wallet != null) {
			
			ECKey key = wallet.getKeys().get(0);
			publicAddressText.setText(key.toAddress(networkParams).toString());
			walletBalanceText.setText(Utils.bitcoinValueToFriendlyString(wallet.getBalance(Wallet.BalanceType.ESTIMATED)));
		}
			
		
		
		refreshWallet();
		
		return rootView;
	}
	
	
	
	
	/*
	 * Setup a wallet object for this fragment by either loading it from an existing store file,
	 * or by creating a new one from network parameters. Note, though Android won't crash out because of it,
	 * this shouldn't be called from a UI thread due to the blocking nature of creating files.
	 */
	private void setupWallet() {

		wallet = null;

		try {
			wallet = Wallet.loadFromFile(walletFile);
		} 
		catch (UnreadableWalletException e) {
			ZLog.log("Excepting trying to load wallet file: ", e);
		}
		
		if(wallet == null) {
			wallet = new Wallet(networkParams);
			try {
				wallet.addKey(new ECKey());
				wallet.saveToFile(walletFile);
			} 
			catch (IOException e) {
				ZLog.log("Exception trying to save new wallet file: ", e);
				
				
				return;
				//TODO -this is just test code of course but this is probably "fatal" as if we can't save our wallet, that's a problem
				//super.onBackPressed(); //just kill the activity for testing purposes
			}
		}
		
		ZLog.log("Test Wallet: ", wallet.toString());
		ZLog.log("Earliest wallet creation time: ", String.valueOf(wallet.getEarliestKeyCreationTime()));
		
	}
	
	
	
	private void refreshWallet() {
		
		ZiftrUtils.runOnNewThread(new Runnable() {
			
			@Override
			public void run() {
			
				try {
					
		        	blockStore = new SPVBlockStore(networkParams, blockStoreFile); 
		        	
		        	BlockChain chain = new BlockChain(networkParams, wallet, blockStore);
					
					peerGroup = new PeerGroup(networkParams, chain);
					

			        //TODO -what does doing this actually do?
					peerGroup.addWallet(wallet);
			        chain.addWallet(wallet);
			        

					peerGroup.addPeerDiscovery(new DnsDiscovery(networkParams));
			        //peerGroup.setMaxConnections(25);
					
					
					//peerGroup.addAddress(InetAddress.getLocalHost()); //this is for running local version of bitcoin network for testing

	
					//peerGroup.downloadBlockChain();
					//peerGroup.awaitRunning();
					
					peerGroup.start();
					
					ZLog.log("Connected peers: ", peerGroup.getConnectedPeers());
					
					
	
					//note instead of using the anonymous inner class based, we could use another class which implements WalletEventListener
					//however that requires setting up a bunch of methods we don't need for this simple test
					wallet.addEventListener(new AbstractWalletEventListener() {
			            @Override
			            public synchronized void onCoinsReceived(Wallet w, Transaction tx, BigInteger prevBalance, final BigInteger newBalance) {
			            	
			            	try {
								wallet.saveToFile(walletFile);
							} 
			            	catch (IOException e) {
			            		ZLog.log("Failed to save updated wallet balance: ", e);
							}
			            	
			            	//now update the UI with the new balance
			            	Activity activity = getActivity();
			            	if(activity != null) {
				                activity.runOnUiThread(new Runnable() {
									public void run() {
										updateWalletBalance(Utils.bitcoinValueToFriendlyString(newBalance));
									}
								});
			            	}
			            }
			        });
					
					
					// Now download and process the block chain.
			        peerGroup.downloadBlockChain();
				} 
		        catch (BlockStoreException e) {
		        	ZLog.log("Exeption creating block store: ", e);
				} 
				
			}
		});
		
	}
	
	
	private void updateWalletBalance(String newBalance) {
		walletBalanceText.setText(newBalance);
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
