package com.ziftr.android.ziftrwallet.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWApplication;
import com.ziftr.android.ziftrwallet.util.ZLog;


public class ZiftrDialogFragment extends DialogFragment implements OnClickListener, DialogInterface.OnClickListener {
	
	protected String title;
	protected String message;
	protected String yes;
	protected String no;
	
	private DialogInterface.OnClickListener clickListener = null;
	private DialogInterface.OnCancelListener cancelListener = null;
	
	public ZiftrDialogFragment() {
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
	public static ZiftrDialogFragment buildContinueCancelDialog(int messageResId) {
		
		ZiftrDialogFragment dialog = new ZiftrDialogFragment();
		dialog.setupDialog(messageResId);
		
		return dialog;
	}
	
	
	/**
	 * a quick factory method to create a dialog that uses the app title as the title,
	 * and has a "Continue" button
	 * @param messageResId the resource id of the string to be displayed as this dialogs message
	 * @param listener the on click listener that will handle the user pressing this dialog's buttons
	 * @return a new instance of a ZiftrDialogFragment ready to be shown
	 */
	public static ZiftrDialogFragment buildContinueDialog(int messageResId) {
		
		ZiftrDialogFragment dialog = new ZiftrDialogFragment();
		dialog.setupDialog(0, messageResId, R.string.zw_dialog_continue, 0);
		
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
	public void onAttach(Activity activity) {
		ZLog.log("Attaching a dialog fragment: ", this.getTag());
		super.onAttach(activity);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		restoreInstanceState(savedInstanceState);
		
		View customDialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_generic, null);
		return buildCustomDialog(customDialogView);
	}
	
	
	protected void restoreInstanceState(Bundle savedInstanceState) {
		if(savedInstanceState != null) {
			if(title == null) title = savedInstanceState.getString("title");
			if(message == null) message = savedInstanceState.getString("message");
			if(yes == null) yes = savedInstanceState.getString("yes");
			if(no == null) no = savedInstanceState.getString("no");
		}
	}
	
	protected Dialog buildCustomDialog(View customDialogView) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
		
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
		
		builder.setView(customDialogView);
		
		TextView titleTextView = (TextView) customDialogView.findViewById(R.id.dialog_title);
		titleTextView.setText(title);
		
		TextView messageTextView = (TextView) customDialogView.findViewById(R.id.dialog_message);
		messageTextView.setText(message);

		Button yesButton = (Button) customDialogView.findViewById(R.id.right_dialog_button);
		if(yes != null) {
			yesButton.setText(yes);
			yesButton.setOnClickListener(this);
		}
		else {
			yesButton.setVisibility(View.GONE);
		}
		
		Button noButton = (Button) customDialogView.findViewById(R.id.left_dialog_button);
		if(no != null) {
			noButton.setText(no);
			noButton.setOnClickListener(this);
		}
		else {
			noButton.setVisibility(View.GONE);
		}
		
		
		return builder.create();
	}
	
	

	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		
		outState.putString("title", title);
		outState.putString("message", message);
		outState.putString("yes", yes);
		outState.putString("no", no);
		
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		
		ZLog.log("Dismissing dialog...");
		
		//this.getFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
		
		super.onDismiss(dialog);
	}

	
	public void setOnClickListener(DialogInterface.OnClickListener listener) {
		this.clickListener = listener;
	}
	
	public void setOnCancelListener(DialogInterface.OnCancelListener listener) {
		this.cancelListener = listener;
	}
	
	
	@Override
	public void onCancel(DialogInterface dialog) {
		
		if(this.cancelListener != null) {
			cancelListener.onCancel(dialog);
			return;
		}
		
		//tell the dialog manager that this dialog has been cancelled
		ZiftrDialogManager.dialogCancelled(this);
		
		super.onCancel(dialog);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		
		if(clickListener != null) {
			clickListener.onClick(dialog, which);
			return;
		}
		
		//tell the dialog manager that a button has been pressed on the active dialog
		if(which == DialogInterface.BUTTON_POSITIVE) {
			ZiftrDialogManager.dialogClickedYes(this);
		}
		else if(which == DialogInterface.BUTTON_NEGATIVE) {
			ZiftrDialogManager.dialogClickedNo(this);
		}
	
	}

	@Override
	public void onClick(View v) {
		//wrapping around basic dialog handling stuff becasue at the moment we have a stupid implementation of custom dialog UI
		if(v.getId() == R.id.right_dialog_button) {
			this.onClick(this.getDialog(), DialogInterface.BUTTON_POSITIVE);
		}
		else if(v.getId() == R.id.left_dialog_button) {
			this.onClick(this.getDialog(), DialogInterface.BUTTON_NEGATIVE);
		}
		
		this.dismiss();
	}

	
}
