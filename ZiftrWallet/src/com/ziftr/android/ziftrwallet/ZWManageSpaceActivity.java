package com.ziftr.android.ziftrwallet;

import java.io.File;

import android.app.Application;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

import com.ziftr.android.ziftrwallet.dialog.ZiftrDialogHandler;
import com.ziftr.android.ziftrwallet.dialog.ZiftrDialogManager;
import com.ziftr.android.ziftrwallet.dialog.ZiftrTextDialogFragment;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWManageSpaceActivity extends FragmentActivity implements OnClickListener, ZiftrDialogHandler {
	
	private Button continueButton;
	private Button cancelButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.manage_space);
		
		ZiftrDialogManager.registerHandler(this);
		
		this.continueButton = (Button) this.findViewById(R.id.continue_button);
		this.cancelButton = (Button) this.findViewById(R.id.cancel_button);
		this.continueButton.setOnClickListener(this);
		this.cancelButton.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		if (v == continueButton){
			if (ZWPreferences.userHasPassword()){
				this.showContinuePasswordDialog();
			} else {
				this.clearAppData();
				this.finish();

			}
		} else if (v == cancelButton) {
			this.finish();
		}
	}
	
	private void clearAppData(){
		Application applicationContext = ZWApplication.getApplication();
		//delete external files database and logs
		File externalDirectory = applicationContext.getExternalFilesDir(null);
		if (externalDirectory != null) {
			String[] children = externalDirectory.list();
			for (int i=0; i< children.length; i++){
				new File(externalDirectory, children[i]).delete();
			}
		}
	}
	
	public void showContinuePasswordDialog(){
		
		ZiftrTextDialogFragment passwordDialog = new ZiftrTextDialogFragment();
		passwordDialog.setupDialog(R.string.zw_dialog_enter_password);
		passwordDialog.setupTextboxes();
		
		passwordDialog.show(getSupportFragmentManager(), "password_dialog");
	}


	@Override
	public void handleDialogYes(DialogFragment fragment) {
		
		if("password_dialog".equals(fragment.getTag())) {
			ZiftrTextDialogFragment passwordDialog = (ZiftrTextDialogFragment) fragment;
			String password = passwordDialog.getEnteredTextTop();
			
			byte[] inputHash = ZiftrUtils.saltedHash(password);
			
			if (ZWPreferences.inputHashMatchesStoredHash(inputHash)) {
				this.clearAppData();
				this.finish();
			}
			else {
				//incorrect password
				ZiftrDialogManager.showSimpleAlert(getSupportFragmentManager(), R.string.zw_incorrect_password);
			}
		}
	}

	@Override
	public void handleDialogNo(DialogFragment fragment) {
		//do nothing, just let the dialog dismiss itself
	}

	@Override
	public void handleDialogCancel(DialogFragment fragment) {
		//do nothing, just let the dialog dismiss itself		
	}
}
