package com.ziftr.android.ziftrwallet;

import java.io.IOException;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.os.ParcelFileDescriptor;

import com.ziftr.android.ziftrwallet.util.ZLog;

public class ZWDatabaseBackupHelper extends BackupAgentHelper{
	
	@Override
	public void onCreate(){
		ZLog.log("Backup oncreate called");
		FileBackupHelper hosts = new FileBackupHelper(this, this.getExternalFilesDir(ZWWalletManager.DATABASE_NAME).getAbsolutePath());
		addHelper(ZWWalletManager.DATABASE_NAME,hosts);
	}
	
	@Override
	public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) {
		ZLog.log("backup onbackup called");
		try {
			//class is the lock since we are using static synchronized methods to read/write
			synchronized (ZWWalletManager.class) {
				super.onBackup(oldState, data, newState);
				ZLog.log("Backedup");
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
				super.onRestore(data, appVersionCode, newState);
			}
		} catch (IOException e) {
			ZLog.log("Backup error, Unable to read from file: " + e);
		}
	}
	
}
