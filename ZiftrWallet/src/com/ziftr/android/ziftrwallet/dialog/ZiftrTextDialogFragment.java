package com.ziftr.android.ziftrwallet.dialog;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWApplication;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class ZiftrTextDialogFragment extends ZiftrDialogFragment {

	String topHintText;
	String middleHintText;
	String bottomHintText;
	
	EditText topEditText;
	EditText middleEditText;
	EditText bottomEditText;
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		restoreInstanceState(savedInstanceState);
		
		View customTextDialog = getActivity().getLayoutInflater().inflate(R.layout.dialog_text_edit, null);
		
		topEditText = (EditText) customTextDialog.findViewById(R.id.dialog_textbox_top);
		middleEditText = (EditText) customTextDialog.findViewById(R.id.dialog_textbox_middle);
		bottomEditText = (EditText) customTextDialog.findViewById(R.id.dialog_textbox_bottom);
		
		setupTextbox(topEditText, topHintText);
		setupTextbox(middleEditText, middleHintText);
		setupTextbox(bottomEditText, bottomHintText);
		
		return this.buildCustomDialog(customTextDialog);
	}

	
	
	@Override
	protected void restoreInstanceState(Bundle savedInstanceState) {
		super.restoreInstanceState(savedInstanceState);
		
		if(savedInstanceState != null) {
			if(topHintText == null) topHintText = savedInstanceState.getString("topHintText");
			if(middleHintText == null) middleHintText = savedInstanceState.getString("middleHintText");
			if(bottomHintText == null) bottomHintText = savedInstanceState.getString("bottomHintText");
		}
	}


	private void setupTextbox(EditText textbox, String hintText) {
		if(hintText != null) {
			textbox.setHint(hintText);
			textbox.setVisibility(View.VISIBLE);
		}
		else {
			textbox.setVisibility(View.GONE);
		}
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putString("topHintText", topHintText);
		outState.putString("middleHintText", middleHintText);
		outState.putString("bottomHintText", bottomHintText);
	}
	
	
	
	/**
	 * setup the textboxes {@link EditText} to be displayed by this dialog,
	 * sets the dialog to only have a single textbox (using the top slot),
	 * with no text hint
	 */
	public void setupTextboxes() {
		this.setupTextboxes(R.string.zw_empty_string, 0, 0);
	}
	
	
	
	/**
	 * setup the textboxes {@link EditText} to be displayed by this dialog,
	 * The strings resource ids passed in will be used as the "hint text" for the 3 text boxes.
	 * Passing 0 will cause the text box to be removed.
	 * @param topHintText the resource id for hint text for the top text box, or 0 for this text box to be removed
	 * @param middleHintText the resource id for hint text for the middle text box, or 0 for this text box to be removed
	 * @param bottomHintText the resource id for hint text for the bottom text box, or 0 for this text box to be removed
	 */
	public void setupTextboxes(int topHintResId, int middleHintResId, int bottomHintResId) {
		Context appContext = ZWApplication.getApplication(); 
		
		if(topHintResId > 0) {
			this.topHintText = appContext.getString(topHintResId);
		}
		if(middleHintResId > 0) {
			this.middleHintText = appContext.getString(middleHintResId);
		}
		if(bottomHintResId > 0) {
			this.bottomHintText = appContext.getString(bottomHintResId);
		}
	}

	
	public String getEnteredTextTop() {
		if(topEditText != null && topEditText.getVisibility() == View.VISIBLE) {
			return topEditText.getText().toString();
		}
		
		return null;
	}

	
	public String getEnteredTextMiddle() {
		if(middleEditText != null && middleEditText.getVisibility() == View.VISIBLE) {
			return middleEditText.getText().toString();
		}
		
		return null;
	}
	
	public String getEnteredTextBottom() {
		if(bottomEditText != null && bottomEditText.getVisibility() == View.VISIBLE) {
			return bottomEditText.getText().toString();
		}
		return null;
	}


	@Override
	public void dismiss() {
		
		Activity activity = getActivity();
		if(activity != null) {
			InputMethodManager inputMan = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
			inputMan.hideSoftInputFromWindow(getDialog().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		}
		
		super.dismiss();
	}
	
	
	
	
}
