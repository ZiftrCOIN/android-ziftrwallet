package com.ziftr.android.ziftrwallet;

import java.io.File;

import android.app.Application;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

import com.ziftr.android.ziftrwallet.dialog.ZWDialogFragment;
import com.ziftr.android.ziftrwallet.dialog.ZWSimpleAlertDialog;
import com.ziftr.android.ziftrwallet.dialog.ZWValidatePassphraseDialog;
import com.ziftr.android.ziftrwallet.dialog.handlers.ZWNeutralDialogHandler;
import com.ziftr.android.ziftrwallet.dialog.handlers.ZWValidatePassphraseDialogHandler;
import com.ziftr.android.ziftrwallet.fragment.ZWRequestCodes;
import com.ziftr.android.ziftrwallet.fragment.ZWTags;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWManageSpaceActivity extends FragmentActivity implements OnClickListener, ZWValidatePassphraseDialogHandler, ZWNeutralDialogHandler{
	
	private Button continueButton;
	private Button cancelButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.manage_space);
		
		this.continueButton = (Button) this.findViewById(R.id.continue_button);
		this.cancelButton = (Button) this.findViewById(R.id.cancel_button);
		this.continueButton.setOnClickListener(this);
		this.cancelButton.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		if (v == continueButton){
			if (ZWPreferences.userHasPassphrase()){
				this.showContinuePassphraseDialog();
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
	
	public void showContinuePassphraseDialog(){
		ZWValidatePassphraseDialog passphraseDialog = new ZWValidatePassphraseDialog();
		String message = "Please input your passphrase. ";
		passphraseDialog.setupDialog("ziftrWALLET", message, "Continue", null, "Cancel");
		passphraseDialog.show(this.getSupportFragmentManager(), "delete_data");
	}
	
	public void showAlert(String message, String tag){
		ZWSimpleAlertDialog alertUserDialog = new ZWSimpleAlertDialog();
		Bundle args = new Bundle();
		args.putInt(ZWDialogFragment.REQUEST_CODE_KEY, ZWRequestCodes.ALERT_USER_DIALOG);
		alertUserDialog.setArguments(args);

		// Set negative text to null to not have negative button
		alertUserDialog.setupDialog("ziftrWALLET", message, null, "OK", null);
		alertUserDialog.show(this.getSupportFragmentManager(), tag);

	}

	@Override
	public void handlePassphrasePositive(int requestCode, String passphrase) {
		byte[] inputHash = ZiftrUtils.saltedHash(passphrase);
		if (ZWPreferences.inputHashMatchesStoredHash(inputHash)) {
			this.clearAppData();
			this.finish();

		} else {
			this.showAlert(getResources().getString(R.string.zw_incorrect_passphrase), ZWTags.PASSPHRASE_INCORRECT);
		}
	}

	@Override
	public void handleNegative(int requestCode) {
		//Nothing
	}

	@Override
	public void handleNeutral(int requestCode) {
		// TODO Auto-generated method stub
		
	}
}
