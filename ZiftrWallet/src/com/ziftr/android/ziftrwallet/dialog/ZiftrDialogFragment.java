package com.ziftr.android.ziftrwallet.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.View.OnClickListener;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.util.ZLog;

public abstract class ZiftrDialogFragment extends DialogFragment implements OnClickListener, DialogInterface.OnClickListener {

	protected Object dataObject;
	protected boolean isCancelable = true;
	
	private DialogInterface.OnClickListener clickListener = null;
	private DialogInterface.OnCancelListener cancelListener = null;


	@Override
	public void onAttach(Activity activity) {
		ZLog.log("Attaching a dialog fragment: ", this.getTag());
		super.onAttach(activity);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		restoreInstanceState(savedInstanceState);
		
		View customDialogView = buildRootView();
		customDialogView = initRootView(customDialogView);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
		builder.setView(customDialogView);
		
		Dialog customDialog = builder.create();
		
		customDialog.setCancelable(isCancelable);
		customDialog.setCanceledOnTouchOutside(isCancelable);
		
		return customDialog;
	}

	
	protected abstract View buildRootView();
	
	protected abstract View initRootView(View rootView);	
	
	
	protected void restoreInstanceState(Bundle savedInstanceState) {
		if(savedInstanceState != null) {
			isCancelable = savedInstanceState.getBoolean("isCancelable");
		}

	}

	
	@Override
	public void onSaveInstanceState(Bundle outState) {

		outState.putBoolean("isCancelable", isCancelable);
		
		super.onSaveInstanceState(outState);
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
	

	/**
	 * This can be used as a shortcut to store some data that the handler or other calling code may need to reference later.
	 * Since dialogs can be destroyed and recreated by the Android lifecycle, setting a data object will cause this dialog
	 * fragment to be retained.
	 * @param data any serializable data that the fragment or its handlers may need later
	 */
	public void setData(Object data) {
		this.dataObject = data;
		
		if(this.dataObject != null) {
			this.setRetainInstance(true);
		}
		else {
			this.setRetainInstance(false);
		}
	}
	
	
	/**
	 * Gets the Object stored by {@link #getData()}.
	 * @return the object previously set, or null if no data was ever set
	 */
	public Object getData() {
		return this.dataObject;
	}

	
	public void setCancelable(boolean isCancelable) {
		this.isCancelable = isCancelable;
	}
	
	
	

}
