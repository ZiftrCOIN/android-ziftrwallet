package com.ziftr.android.ziftrwallet;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.ziftr.android.ziftrwallet.fragment.ZWRequestCodes;
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

		ZWWelcomeActivity a = (ZWWelcomeActivity) this.getActivity();
		// Need an activity to do pretty much everything below
		if (a == null) {
			return;
		}

		if (v == askMeLaterButton) {
			if (this.getActivity() != null) {
				this.startMain();
			}
		} else if (v == saveNameButton) {
			String name = nameEditText.getText().toString();
			if (!name.isEmpty()) {
				if (ZWPreferencesUtils.setUserName(name) == -1){
					a.alert("Your name was not set! We've encountered a database error, please restart app.", 
							"error_set_password", ZWRequestCodes.UPSERT_DB_ERROR);
				} else {
					this.startMain();
				}
			} else {
				a.alert("An empty name doesn't give any information about you!", "name_is_empty_string");
			}
		}

	}
	
	private void startMain() {
		ZWWelcomeActivity welcomeActivity = (ZWWelcomeActivity) this.getActivity();
		welcomeActivity.startZWMainActivity();
	}
}

