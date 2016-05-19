package com.ziftr.android.ziftrwallet.fragment;

import java.security.SecureRandom;
import java.util.List;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.google.common.base.Joiner;
import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWApplication;
import com.ziftr.android.ziftrwallet.ZWPreferences;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWMnemonicGenerator;
import com.ziftr.android.ziftrwallet.crypto.ZWPrivateData;
import com.ziftr.android.ziftrwallet.dialog.ZiftrDialogManager;
import com.ziftr.android.ziftrwallet.dialog.ZiftrTextDialogFragment;
import com.ziftr.android.ziftrwallet.exceptions.ZWDataEncryptionException;
import com.ziftr.android.ziftrwallet.sqlite.ZWWalletManager;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWManageHdAccountFragment extends ZWFragment implements OnClickListener {
	
	public static final String FRAGMENT_TAG = "manage_hd_account";
	public static final String DIALOG_READ_HD_SENTENCE_TAG = "manage_hd_read_sentence";
	public static final String DIALOG_WRITE_HD_SENTENCE_TAG = "manage_hd_write_sentence";
	
	View rootView;
	TextView mnemonicText;
	View leftButton;
	View rightButton;
	
	//String hdMnemonic;
	ZWPrivateData hdMnemonicData;
	
	private STATE accountState = STATE.NO_ACCOUNT;
	
	private enum STATE {
		NO_ACCOUNT,
		UNLOCKED_ACCOUNT,
		LOCKED_ACCOUNT,
		CREATING_ACCOUNT,
		RESTORING_ACCOUNT
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.accounts_hd_management, container, false);
		
		mnemonicText = (TextView) rootView.findViewById(R.id.hdAccountCurrentMnemonicText);
		
		leftButton = rootView.findViewById(R.id.hdAccountLeftButton);
		leftButton.setOnClickListener(this);
		
		rightButton = rootView.findViewById(R.id.hdAccountRightButton);
		rightButton.setOnClickListener(this);
		
		resetState();
		
		return rootView;
	}

	
	@Override
	public void onResume() {
		super.onResume();
		
		this.getZWMainActivity().changeActionBar(R.string.zw_actionbar_hd_account, true, true, false);
	}

	
	@Override
	public void onClick(View v) {
		
		if(v.getId() == R.id.hdAccountLeftButton) {
			switch(accountState) {
				case NO_ACCOUNT:
					//create a new account
					this.generateMnemonic();
					break;
				case LOCKED_ACCOUNT:
					//enter password to see mnemonic
					this.showEnterPasswordDialgo(DIALOG_READ_HD_SENTENCE_TAG);
					break;
				case UNLOCKED_ACCOUNT:
					//should be no buttons displaying when account is unlocked
					break;
				case CREATING_ACCOUNT:
					//use wants to generate a different sentence to use
					this.generateMnemonic();
					break;
				case RESTORING_ACCOUNT:
					verifyMnemonic();
					break;
				default:
					break;				
			}
		}
		else if(v.getId() == R.id.hdAccountRightButton) {
			switch(accountState) {
				case NO_ACCOUNT:
					//restore an account
					this.setupRestoreMnemonic();
					break;
				case LOCKED_ACCOUNT:
					//button should be hidden
					break;
				case UNLOCKED_ACCOUNT:
					//button should be hidden
					break;
				case CREATING_ACCOUNT:
					//accept the displayed mnemonic
					saveMnemonic();
					break;
				case RESTORING_ACCOUNT:
					if(verifyMnemonic()) {
						saveMnemonic();
					}
					break;
				default:
					break;
			}
		}
	}
	
	
	private void startAccountScan() {
		//TODO show some sort of dialog explaining process to user
		
	}
	
	
	
	
	private void saveMnemonic() {
		String password = ZWPreferences.getCachedPassword();
		if(password == null && ZWPreferences.userHasPassword()) {
			//show password dialog
			showEnterPasswordDialgo(DIALOG_WRITE_HD_SENTENCE_TAG);
		}
		else {
			this.writeMnemonic(password);
		}
		
		resetState();
	}
	
	
	private void resetState() {
		
		View enterMnemonic = rootView.findViewById(R.id.hdAccountEnterMnemonic);
		enterMnemonic.setVisibility(View.GONE);
		
		View statusText = rootView.findViewById(R.id.hdAccountStatus);
		statusText.setVisibility(View.GONE);
		
		//hdMnemonic = ZWPreferences.getEncryptedHdMnemonic();
		
		if(hdMnemonicData == null) {
			hdMnemonicData = ZWWalletManager.getInstance().getHdMnemonic();
		}
		
		if(hdMnemonicData != null) {
			if(hdMnemonicData.isEncrypted()) {
				accountState = STATE.LOCKED_ACCOUNT;
				rightButton.setVisibility(View.GONE);
				
				TextView text = (TextView) leftButton.findViewById(R.id.hdAccountLeftButtonText);
				text.setText(R.string.zw_button_enter_password);
			}
			else {
				accountState = STATE.UNLOCKED_ACCOUNT;
				mnemonicText.setText(hdMnemonicData.getDataString());
				leftButton.setVisibility(View.GONE);
				
				//set the right button the start scanning accounts
				rightButton.setVisibility(View.VISIBLE);
				TextView rightText = (TextView) rightButton.findViewById(R.id.hdAccountRightButtonText);
				rightText.setText(R.string.zw_button_account_scan);
			}
		}
		else {
			accountState = STATE.NO_ACCOUNT;
			
			leftButton.setVisibility(View.VISIBLE);
			TextView leftText = (TextView) leftButton.findViewById(R.id.hdAccountLeftButtonText);
			leftText.setText(R.string.zw_button_hd_new_account);
			
			rightButton.setVisibility(View.VISIBLE);
			TextView rightText = (TextView) rightButton.findViewById(R.id.hdAccountRightButtonText);
			rightText.setText(R.string.zw_button_hd_restore_account);
		}
	}
	
	
	private void setupRestoreMnemonic() {
		
		accountState = STATE.RESTORING_ACCOUNT;
		
		this.mnemonicText.setText(R.string.zw_accounts_hd_enter_mnemonic);
		
		View enterMnemonic = rootView.findViewById(R.id.hdAccountEnterMnemonic);
		enterMnemonic.setVisibility(View.VISIBLE);
	
		leftButton.setVisibility(View.VISIBLE);
		TextView leftText = (TextView) leftButton.findViewById(R.id.hdAccountLeftButtonText);
		leftText.setText(R.string.zw_button_verify);
		
		rightButton.setVisibility(View.VISIBLE);
		TextView rightText = (TextView) rightButton.findViewById(R.id.hdAccountRightButtonText);
		rightText.setText(R.string.zw_button_restore);
	}
	
	
	private boolean verifyMnemonic() {
		
		EditText enterMnemonic = (EditText) rootView.findViewById(R.id.hdAccountEnterMnemonic);
		TextView statusText = (TextView) rootView.findViewById(R.id.hdAccountStatus);
		statusText.setVisibility(View.VISIBLE);
		
		String mnemonic = enterMnemonic.getText().toString();
		boolean passed = ZWMnemonicGenerator.passesChecksum(mnemonic);
		
		if(passed) {
			hdMnemonicData = ZWPrivateData.createFromUnecryptedData(mnemonic);
			statusText.setText(R.string.zw_accounts_hd_verify_pass);
		}
		else {
			statusText.setText(R.string.zw_accounts_hd_verify_fail);
		}
		
		return passed;
	}
	
	
	
	
	private void generateMnemonic() {
		
		accountState = STATE.CREATING_ACCOUNT;
		
		leftButton.setVisibility(View.VISIBLE);
		TextView leftText = (TextView) leftButton.findViewById(R.id.hdAccountLeftButtonText);
		leftText.setText(R.string.zw_button_hd_new_sentence);
		
		rightButton.setVisibility(View.VISIBLE);
		TextView rightText = (TextView) rightButton.findViewById(R.id.hdAccountRightButtonText);
		rightText.setText(R.string.zw_button_accept_hd_account);
		
		byte[] entropy = this.getRandomEntropy();
		if(entropy != null) {
			String[] mnemonic = ZWMnemonicGenerator.createHdWalletMnemonic(entropy);
			String mnemonicString = Joiner.on(" ").join(mnemonic); 
			this.hdMnemonicData = ZWPrivateData.createFromUnecryptedData(mnemonicString);
			this.mnemonicText.setText(mnemonicString);
		}
		else {
			this.hdMnemonicData = null;
			this.mnemonicText.setText("???");
		}
	}
	
	
	
	private byte[] getRandomEntropy() {
		SecureRandom random = ZiftrUtils.createTrulySecureRandom();
		if(random == null) {
			String rngError = ZWApplication.getApplication().getString(R.string.zw_dialog_error_rng);
			ZiftrDialogManager.showSimpleAlert(rngError);
			return null;
		}
		
		byte[] entropy = new byte[16];
		random.nextBytes(entropy);

		return entropy;
	}
	
	
	private void showEnterPasswordDialgo(String dialogTag) {
		ZiftrTextDialogFragment passwordFragment = new ZiftrTextDialogFragment();
		passwordFragment.setupDialog(R.string.zw_dialog_enter_password);
		passwordFragment.addEmptyTextbox(true);

		//note the tag, removing a password is just changing it to an empty password
		passwordFragment.show(getFragmentManager(), dialogTag);
	}
	
	
	public void readMnemonic(String password) {
		
		try {
			ZWPrivateData mnemonic = ZWWalletManager.getInstance().getHdMnemonic();
			String decryptedMnemonic = hdMnemonicData.decrypt(password).getDataString();
			
			if(ZWMnemonicGenerator.passesChecksum(decryptedMnemonic)) {
				hdMnemonicData = mnemonic;
				this.mnemonicText.setText(decryptedMnemonic);
			}
		} 
		catch (ZWDataEncryptionException e) {
			ZLog.log("Error decrypting stored hd mnemonic: ", e);
		}
		
		
	}
	
	
	public void writeMnemonic(String password) {
		if(this.hdMnemonicData != null) {
			try {
				//calculate the seed from the mnemonic we have
				this.hdMnemonicData.encrypt(password);
				String mnemonicString = hdMnemonicData.getDataString();
				
				if(ZWMnemonicGenerator.passesChecksum(mnemonicString)) {
					byte [] hdSeed = ZWMnemonicGenerator.generateHdSeed(mnemonicString, null);
					
					//create private data for our seed, for storing it in our local db
					String hdSeedString = ZiftrUtils.bytesToHexString(hdSeed);
					ZWPrivateData hdSeedData = ZWPrivateData.createFromUnecryptedData(hdSeedString);
					
					//encrypt the mnemonic and the seed
					hdMnemonicData.encrypt(password);
					hdSeedData.encrypt(password);
					
					ZWWalletManager.getInstance().updateHdSeedData(hdSeedData, hdMnemonicData, true);
				
					//now make sure any active coins are switched over to hd accounts
					List<ZWCoin> activatedCoins = ZWWalletManager.getInstance().getActivatedCoins();
					for(ZWCoin coin : activatedCoins) {
						ZWWalletManager.getInstance().activateCoin(coin, password);
					}
				}
				else {
					ZLog.log("Failed double check of mnemonic validation. Not much we can do...");
				}
			}
			catch(Exception e) {
				ZLog.log("Exception storing mnemonic sentence: ", e);
			}
		}
	}
	

}
