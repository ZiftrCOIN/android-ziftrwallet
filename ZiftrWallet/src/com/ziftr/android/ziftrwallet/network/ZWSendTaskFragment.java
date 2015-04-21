package com.ziftr.android.ziftrwallet.network;

import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.util.List;

import org.json.JSONObject;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.util.ZLog;
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
	private static boolean sendCompleted = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null){
			if (savedInstanceState.containsKey("done")){
				this.done = savedInstanceState.getBoolean("done");
			}
			if (savedInstanceState.containsKey("coin")){
				this.sentCoin = ZWCoin.getCoin(savedInstanceState.getString("coin"));
			}
		}
		this.setRetainInstance(true);
	}
	
	@Override
	public void onSaveInstanceState(Bundle bundle){
		ZLog.log("send task save instance called");
		bundle.putBoolean("done", this.done);
		bundle.putString("coin", this.sentCoin.getSymbol());
		super.onSaveInstanceState(bundle);
	}
	
	public void sendCoins(final ZWCoin coin, final BigInteger fee, final BigInteger amount, final List<String> inputs, final String output, 
			final String passphrase){
		this.sentCoin = coin;
		ZiftrUtils.runOnNewThread(new Runnable(){

			@Override
			public void run() {
				JSONObject jsonRes = ZWDataSyncHelper.sendCoins(coin, fee, amount, inputs, output, passphrase);
				if (jsonRes != null){
					
					//TODO -redo checking for mempool spending in a more sane way
					//if (ZWPreferencesUtils.getMempoolIsSpendable() || !ZWDataSyncHelper.checkSpendingUnconfirmedTxn(coin, jsonRes, inputs, passphrase)){
						if (ZWDataSyncHelper.signSentCoins(coin, jsonRes, passphrase)) {
							ZLog.log("sign success");
							sendCompleted = true;
							updateCallback();
						} else {
							cancelCallback();
						}
					//}
				} else {
					cancelCallback();
				}
			}
		
		});
	}
	
	/** This is called if the user has not enabled spending unconfirmed txns and hits continue after the warning to resume the spending*/
	public void signSentCoins(final ZWCoin coin, final JSONObject json, final String pass){
		if (ZWDataSyncHelper.signSentCoins(coin, json, pass)) {
			updateCallback();
		} else {
			cancelCallback();
		}
	}
	
	/** call if there was an error during sending and let the messagemanager handle alerting the error*/
	private void cancelCallback(){
		SendTaskCallback currentCallback = callback.get();
		this.done = true;
		if (currentCallback!= null){
			currentCallback.destroySendTaskFragment();
		}
	}
	
	public synchronized void setCallBack(SendTaskCallback cb){
		this.callback = new WeakReference<SendTaskCallback>(cb);
		if (this.done && this.callback != null){
			callback.get().destroySendTaskFragment();
		} else {
			updateCallback();
		}
	}
	
	public synchronized void updateCallback(){
		SendTaskCallback currentCallback = callback.get();
		if (currentCallback!=null && this.sentCoin != null && ZWSendTaskFragment.sendCompleted){
			ZLog.log("updating");
			currentCallback.updateSendStatus(this.sentCoin);
		} else if (this.sentCoin == null && this.done){
			callback.get().destroySendTaskFragment();					
		}
	}
	
	public synchronized void setDone(boolean done){
		this.done = done;
	}
	
}
