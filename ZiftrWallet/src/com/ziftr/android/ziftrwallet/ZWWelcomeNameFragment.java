package com.ziftr.android.ziftrwallet;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.ziftr.android.ziftrwallet.dialog.ZiftrDialogFragment;
import com.ziftr.android.ziftrwallet.dialog.ZiftrDialogManager;
import com.ziftr.android.ziftrwallet.dialog.ZiftrTextDialogFragment;
import com.ziftr.android.ziftrwallet.fragment.ZWTags;

public class ZWWelcomeNameFragment extends Fragment implements OnClickListener {

	private View rootView;
	private EditText nameEditText;
	private Button saveNameButton;
	private Button askMeLaterButton;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		this.rootView = inflater.inflate(R.layout.welcome_name, container, false);

		this.initializeViewFields();

		return this.rootView;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}
	
	/**
	 * Finds all the view fields and sets the listeners.  
	 */
	private void initializeViewFields() {
		saveNameButton = (Button) rootView.findViewById(R.id.set_name);
		askMeLaterButton = (Button) rootView.findViewById(R.id.skip_name);

		nameEditText = (EditText) rootView.findViewById(R.id.usersName).findViewWithTag(ZWTags.ZW_EDIT_TEXT);
		nameEditText.setId(R.id.zw_save_user_name_edit_text);
		
		saveNameButton.setOnClickListener(this);
		askMeLaterButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {

		if (v == askMeLaterButton) {
			if (this.getActivity() != null) {
				this.startMain();
			}
		}
		else if (v == saveNameButton) {
			String name = nameEditText.getText().toString();
			if (!name.isEmpty()) {
				if (ZWPreferences.setUserName(name) == -1){
					ZiftrDialogFragment dbErrorFragment = new ZiftrTextDialogFragment();
					dbErrorFragment.setupDialog(R.string.zw_app_name, 
												R.string.zw_dialog_database_error, 
												R.string.zw_dialog_restart, 
												R.string.zw_dialog_cancel);
					
					dbErrorFragment.show(getFragmentManager(), ZWActivityDialogHandler.DIALOG_DATABASE_ERROR_TAG);
				} 
				else {
					this.startMain();
				}
			} 
			else {
				ZiftrDialogManager.showSimpleAlert(getFragmentManager(), R.string.zw_dialog_blank_name);
			}
		}

	}
	
	private void startMain() {
		ZWWelcomeActivity welcomeActivity = (ZWWelcomeActivity) this.getActivity();
		welcomeActivity.startZWMainActivity();
	}
}

