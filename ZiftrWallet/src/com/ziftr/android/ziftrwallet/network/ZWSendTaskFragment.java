package com.ziftr.android.ziftrwallet.network;

import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.util.List;

import org.json.JSONObject;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.ziftr.android.ziftrwallet.ZWPreferencesUtils;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

/**
 * non-UI fragment for sending coins
 * @author henryphu
 *
 */
public class ZWSendTaskFragment extends Fragment{
	
	public ZWSendTaskFragment(){
		
	}
	
	public static interface SendTaskCallback {
		public void updateSendStatus(ZWCoin coin);
		public void destroySendTaskFragment();
	}
	
	private WeakReference<SendTaskCallback> callback = new WeakReference<SendTaskCallback>(null);
	private ZWCoin sentCoin = null;
	
	private boolean done = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		this.setRetainInstance(true);
	}
	
	public void sendCoins(final ZWCoin coin, final BigInteger fee, final BigInteger amount, final List<String> inputs, final String output, 
			final String passphrase){
		
		this.sentCoin = coin;
		
		ZiftrUtils.runOnNewThread(new Runnable(){

			@Override
			public void run() {
				JSONObject jsonRes = ZWDataSyncHelper.sendCoins(coin, fee, amount, inputs, output, passphrase);
				if (jsonRes != null){
					if (ZWPreferencesUtils.getMempoolIsSpendable() || !ZWDataSyncHelper.checkSpendingUnconfirmedTxn(coin, jsonRes, inputs, passphrase)){
						ZWDataSyncHelper.signSentCoins(coin, jsonRes, passphrase);
						updateCallback();
					}
				}
			}
		
		});
	}
	
	public synchronized void setCallBack(SendTaskCallback cb){
		this.callback = new WeakReference<SendTaskCallback>(cb);
		if (this.done){
			callback.get().destroySendTaskFragment();					
		} else {
			updateCallback();
		}
	}
	
	public synchronized void updateCallback(){
		SendTaskCallback currentCallback = callback.get();
		if (currentCallback!=null && this.sentCoin != null){
			currentCallback.updateSendStatus(this.sentCoin);
		}
	}
	
	public synchronized void setDone(boolean done){
		this.done = done;
	}
}
