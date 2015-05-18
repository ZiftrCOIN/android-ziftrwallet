/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.dialog;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWApplication;
import com.ziftr.android.ziftrwallet.util.ZLog;

public class ZiftrTextDialogFragment extends ZiftrSimpleDialogFragment {

	
	
	ViewGroup textboxContainer;
	
	private static class TextBoxSetup implements Parcelable {
		
		public static final Parcelable.Creator<TextBoxSetup> CREATOR = new Parcelable.Creator<TextBoxSetup>() {
		    public TextBoxSetup createFromParcel(Parcel in) {
		        return new TextBoxSetup(in);
		    }
		
		    public TextBoxSetup[] newArray(int size) {
		        return new TextBoxSetup[size];
		    }
		};
				
		private String text;
		private String hintText;
		private int inputType;

		private TextBoxSetup(String defaultText, String hint, int type) {
			this.text = defaultText;
			this.hintText = hint;
			this.inputType = type;
		}

		public TextBoxSetup(Parcel in) {
			text = in.readString();
			hintText = in.readString();
			inputType = in.readInt();
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(text);
			dest.writeString(hintText);
			dest.writeInt(inputType);
		}
		
	}
	
	private ArrayList<TextBoxSetup> textboxes = new ArrayList<ZiftrTextDialogFragment.TextBoxSetup>();

	

	@Override
	protected View buildRootView() {
		
		return getActivity().getLayoutInflater().inflate(R.layout.dialog_text_edit, null);
	}
	
	
	@Override
	protected View initRootView(View rootView) {
		super.initRootView(rootView);
		
		textboxContainer = (ViewGroup) rootView.findViewById(R.id.dialogEditTextLayout);
		LayoutInflater inflater = getActivity().getLayoutInflater();
		for(int x = 0; x < textboxes.size(); x++) {
			TextBoxSetup textbox = textboxes.get(x);
			EditText textboxView = (EditText) inflater.inflate(R.layout.dialog_include_edittext, null); //note we pass null, so we're returned the EditText			
			textboxView.setTag(x); //use the array index as a tag (autoboxed to Integer class) to easily get it out of the UI later
			
			textboxView.setId(x+1); //set a non zero id, so on resume functions
			//textboxView.setId(View.generateViewId()); //need higher api level
			
			textboxContainer.addView(textboxView);
			textboxView.setHint(textbox.hintText);
			textboxView.setText(textbox.text);
			if(textbox.inputType > 0) {
				textboxView.setInputType(textbox.inputType);
			}
		}
		
		return rootView;
	}



	@Override
	protected void restoreInstanceState(Bundle savedInstanceState) {
		super.restoreInstanceState(savedInstanceState);
		
		if(savedInstanceState != null) {
			
			ArrayList<TextBoxSetup> storedTextboxes = savedInstanceState.getParcelableArrayList("textboxes");
			if(storedTextboxes != null) {
				textboxes = storedTextboxes;
			}
			
			/***
			if(topHintText == null) topHintText = savedInstanceState.getString("topHintText");
			if(middleHintText == null) middleHintText = savedInstanceState.getString("middleHintText");
			if(bottomHintText == null) bottomHintText = savedInstanceState.getString("bottomHintText");
			***/
		}
	}
	


	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		for(int x = 0; x < textboxes.size(); x++) {
			
			//need to go through the text boxes and store the text in them
			EditText textboxView = (EditText) textboxContainer.findViewWithTag(x);
			if(textboxView != null) {
				TextBoxSetup textbox = textboxes.get(x);
				String text = textboxView.getText().toString();
				ZLog.log("Storing textbox with text: ", text);
				textbox.text = text;
			}
		}
		
		outState.putParcelableArrayList("textboxes", textboxes);
	}


	
	public void addEmptyTextbox(boolean hideText) {
		this.addTextbox(R.string.zw_empty_string, 0, hideText);
	}
	
	public void addTextbox(int defaultTextResId, int hintResId, boolean hideText) {
		Context appContext = ZWApplication.getApplication(); 
		
		String defaultText = null;
		if(defaultTextResId > 0) {
			defaultText = appContext.getString(defaultTextResId);
		}
		
		String hint = null;
		if(hintResId > 0) {
			hint = appContext.getString(hintResId);
		}
		
		this.addTextbox(defaultText, hint, hideText);
	}
	
	
	
	public void addTextbox(String defaultText, String hint, boolean hideText) {
		int type = 0;
		if(hideText) {
			type = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
		}
		
		TextBoxSetup textbox = new TextBoxSetup(defaultText, hint, type);
		textboxes.add(textbox);
	}
	
	
	
	public String getEnteredText(int textboxIndex) {
	
		EditText textbox = (EditText) textboxContainer.findViewWithTag(textboxIndex);
		if(textbox != null) {
			return textbox.getText().toString();
		}
		
		return null;
	}

	
	@Override
	public void dismiss() {
		try {
			InputMethodManager inputMan = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			inputMan.hideSoftInputFromWindow(getDialog().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		}
		catch(Exception e) {
			//any of these UI elements could be null at this point
			//if that's the case, the keyboard likely isn't open, so don't worry about it
		}
		
		
		super.dismiss();
	}
	
	
	
	
}
