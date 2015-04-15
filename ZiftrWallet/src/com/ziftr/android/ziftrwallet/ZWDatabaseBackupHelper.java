package com.ziftr.android.ziftrwallet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.os.ParcelFileDescriptor;

import com.ziftr.android.ziftrwallet.crypto.ZWAddress;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.sqlite.ZWReceivingAddressesTable;
import com.ziftr.android.ziftrwallet.util.ZLog;

public class ZWDatabaseBackupHelper extends BackupAgent{
	
	@Override
	public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) {
		ZLog.log("backup onbackup called");
		try {

			//class is the lock since we are using static synchronized methods to read/write
			synchronized (ZWWalletManager.class) {
				ByteArrayOutputStream bufStream = new ByteArrayOutputStream();
				DataOutputStream outWriter = new DataOutputStream(bufStream);
				List<ZWCoin> coins = ZWWalletManager.getInstance().getActivatedCoins();
				
				for (ZWCoin coin : coins){
					//write all receiving addresses in db
					List<ZWAddress> addresses = ZWWalletManager.getInstance().getAddresses(coin, null, true);
					//write number of addresses first
					outWriter.writeInt(addresses.size());
					for (ZWAddress addr : addresses){
						outWriter.writeUTF(ZWReceivingAddressesTable.getPrivDataForInsert(addr.getKey()));
					}
					byte[] buffer = bufStream.toByteArray();
					int len = buffer.length;
					data.writeEntityHeader(coin.getSymbol(), len);
					data.writeEntityData(buffer, len);
					bufStream.reset();
				}
			}
		
		} catch (IOException e) {
			ZLog.log("Backup error, Unable to write to file: " + e);
		}
	}
	
	@Override
	public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState){
		ZLog.log("Backup onrestore called");
				
		try {
			//class is the lock since we are using static synchronized methods to read/write
			synchronized (ZWWalletManager.class) {
				while(data.readNextHeader()){
					ZLog.log(data.getKey());
					String key = data.getKey();
					int dataSize = data.getDataSize();
					if (ZWCoin.getCoin(key) != null){
						byte[] dataBuf = new byte[dataSize];
						data.readEntityData(dataBuf, 0, dataSize);
			            ByteArrayInputStream baStream = new ByteArrayInputStream(dataBuf);
			            DataInputStream in = new DataInputStream(baStream);
			            int numAddresses = in.readInt();
			            for (int i=0; i< numAddresses; i++){
			            	String privKey = in.readUTF();
			            	ZLog.log("restoring key: " + privKey);
			            	//TODO load addresskey into the database
			            }
					} else {
						data.skipEntityData();
					}

				}
				
			}
		} catch (IOException e) {
			ZLog.log("Backup error, Unable to read from file: " + e);
		}
	}
	
}
