/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.dialog;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWApplication;


public class ZiftrSimpleDialogFragment extends ZiftrDialogFragment  {
	
	protected String title;
	protected String message;
	protected String yes;
	protected String no;
	
	
	public ZiftrSimpleDialogFragment() {
		//this.setRetainInstance(true);
	}
	
	public void setupDialog(String title, String message, String yes, String no) {
		this.title = title;
		this.message = message;
		this.yes = yes;
		this.no = no;
	}
	
	
	/**
	 * a quick factory method to create a dialog that uses the app title as the title,
	 * and "Continue" and "Cancel" for buttons
	 * @param messageResId the resource id of the string to be displayed as this dialogs message
	 * @param listener the on click listener that will handle the user pressing this dialog's buttons
	 * @return a new instance of a ZiftrDialogFragment ready to be shown
	 */
	public static ZiftrSimpleDialogFragment buildContinueCancelDialog(int messageResId) {
		
		ZiftrSimpleDialogFragment dialog = new ZiftrSimpleDialogFragment();
		dialog.setupDialog(messageResId);
		
		return dialog;
	}
	
	
	public void setupDialog(int titleResId, int messageResId, int yesResId, int noResId) {
		
		//note if we wanted to make this class truly portable we'd just load these up in oncreate when getActivity() works
		//but we have an easily accessible app context, so might as well just do it now
		Context appContext = ZWApplication.getApplication(); 
		
		if(appContext != null) {
			if(messageResId > 0) {
				this.message = appContext.getString(messageResId);
			}
			
			if(titleResId > 0) {
				this.title = appContext.getString(titleResId);
			}
			else {
				this.title = appContext.getString(R.string.app_name);
			}
			
			if(yesResId > 0) {
				this.yes = appContext.getString(yesResId);
			}
			if(noResId > 0) {
				this.no = appContext.getString(noResId);
			}
		}
	}
	
	/**
	 * setup the dialog with defaults for title, continue and cancel buttons
	 * @param messageResId
	 */
	public void setupDialog(int messageResId) {
		
		setupDialog(0, messageResId, R.string.zw_dialog_continue, R.string.zw_dialog_cancel);
	}

	

	@Override
	protected View buildRootView() {
		View customDialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_message, null);
		return customDialogView;
	}
	
	
	@Override
	protected View initRootView(View rootView) {
		
		//TODO -this crap is crazy, should just be using default dialog builder methods and setting a theme
		//This entire method should be scrapped and replaced with themes as well as all the stupid onClick wrapper stuff.
		
		/***
		builder.setTitle(title);
		builder.setMessage(message);
		
		if(yes != null) {
			builder.setPositiveButton(yes, this);
		}
		if(no != null) {
			builder.setNegativeButton(no, this);
		}
		***/

		TextView titleTextView = (TextView) rootView.findViewById(R.id.dialog_title);
		titleTextView.setText(title);
		
		
		TextView messageTextView = (TextView) rootView.findViewById(R.id.dialog_message);
		messageTextView.setText(message);

		Button yesButton = (Button) rootView.findViewById(R.id.right_dialog_button);
		if(yes != null) {
			yesButton.setText(yes);
			yesButton.setOnClickListener(this);
		}
		else {
			yesButton.setVisibility(View.GONE);
		}
		
		Button noButton = (Button) rootView.findViewById(R.id.left_dialog_button);
		if(no != null) {
			noButton.setText(no);
			noButton.setOnClickListener(this);
		}
		else {
			noButton.setVisibility(View.GONE);
		}
		
		return rootView;
	}
	
	
	protected void restoreInstanceState(Bundle savedInstanceState) {
		
		super.restoreInstanceState(savedInstanceState);
		
		if(savedInstanceState != null) {
			if(title == null) title = savedInstanceState.getString("title");
			if(message == null) message = savedInstanceState.getString("message");
			if(yes == null) yes = savedInstanceState.getString("yes");
			if(no == null) no = savedInstanceState.getString("no");
	
		}
	}

	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		
		outState.putString("title", title);
		outState.putString("message", message);
		outState.putString("yes", yes);
		outState.putString("no", no);
		
		super.onSaveInstanceState(outState);
	}

	
}


