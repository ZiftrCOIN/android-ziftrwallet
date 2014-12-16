package com.ziftr.android.ziftrwallet.network;

import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.util.List;

import android.app.Activity;
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
		public void updateSendStatus(ZWCoin coin, String message);
		public void destroySendTaskFragment();
	}
	
	private WeakReference<SendTaskCallback> callback = new WeakReference<SendTaskCallback>(null);
	private String msg = null;
	private ZWCoin sentCoin = null;
	
	private boolean done = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		this.setRetainInstance(true);
	}
	
	@Override
	public void onAttach(Activity activity){
		ZLog.log("SendTask attached");
		super.onAttach(activity);
		this.callback = new WeakReference<SendTaskCallback>((SendTaskCallback) activity);
		if (done){
			callback.get().destroySendTaskFragment();					
		} else {
			updateCallback();
		}
	}
	
	@Override
	public void onDetach(){
		super.onDetach();
		this.callback = null;
	}
	
	public void sendCoins(final ZWCoin coin, final BigInteger fee, final BigInteger amount, final List<String> inputs, final String output, 
			final String passphrase){
		
		this.sentCoin = coin;
		
		ZiftrUtils.runOnNewThread(new Runnable(){

			@Override
			public void run() {
				msg = ZWDataSyncHelper.sendCoins(coin, fee, amount, inputs, output, passphrase);
				updateCallback();
			}
		
		});
	}
	
	public synchronized void setCallBack(SendTaskCallback cb){
		this.callback = new WeakReference<SendTaskCallback>(cb);
		updateCallback();
	}
	
	private synchronized void updateCallback(){
		SendTaskCallback currentCallback = callback.get();
		if (currentCallback!=null && this.sentCoin != null && this.msg != null){
			currentCallback.updateSendStatus(this.sentCoin, this.msg);
		}
	}
	
	public synchronized void setDone(boolean done){
		this.done = done;
	}
}
