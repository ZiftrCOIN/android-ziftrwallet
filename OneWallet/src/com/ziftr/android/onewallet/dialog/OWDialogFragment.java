package com.ziftr.android.onewallet.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public abstract class OWDialogFragment extends DialogFragment implements OnClickListener {

	/** The title of the dialog. */
	protected String title;
	/** The message of the dialog. */
	protected String message;
	/** The positive button's text. */
	protected String posButtonText;
	/** The negative button's text. */
	protected String negButtonText;
	
	/** The key to save the Dialog's title text in the bundle. */
	private static final String TITLE_KEY = "title";
	/** The key to save the Dialog's message text in the bundle. */
	private static final String MESSAGE_KEY = "message";
	/** The key to save the positive button text in the bundle. */
	private static final String POS_BUTTON_TEXT_KEY = "yes";
	/** The key to save the negative button text in the bundle. */
	private static final String NEG_BUTTON_TEXT_KEY = "no";
	

	/**
	 * Set's up a basic dialog with all of its content.
	 * 
	 * @param title - The Dialog's title.
	 * @param message - The Dialog's message/instructions to user.
	 * @param posButtonText - The 
	 * @param negButtonText
	 */
	public void setupDialog(String title, String message, 
			String posButtonText, String negButtonText) {
		this.title = title;
		this.message = message;
		this.posButtonText = posButtonText;
		this.negButtonText = negButtonText;
	}
	
	/**
	 * Creates and returns the dialog to show the user.
	 * Sets all the basic text fields that all dialogs have 
	 * to be what this diaglog was set up with. 
	 */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		if (savedInstanceState != null) {
			if (title == null) {
				title = savedInstanceState.getString(TITLE_KEY);
			}
			if (message == null) {
				message = savedInstanceState.getString(MESSAGE_KEY);
			}
			if (posButtonText == null) {
				posButtonText = savedInstanceState.getString(POS_BUTTON_TEXT_KEY);
			}
			if (negButtonText == null) {
				negButtonText = savedInstanceState.getString(NEG_BUTTON_TEXT_KEY);
			}
		}
		
		AlertDialog.Builder builder = 
				new AlertDialog.Builder(this.getActivity());
		
		builder.setTitle(title);
		builder.setMessage(message);
		
		if (posButtonText != null) {
			builder.setPositiveButton(posButtonText, this);
		}
		if (negButtonText != null) {
			builder.setNegativeButton(negButtonText, this);
		}
		
		return builder.create();
	}
	
	/**
	 * When we save the instance, we put the four basic text fields
	 * in the bundle.
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		
		outState.putString(TITLE_KEY, this.title);
		outState.putString(MESSAGE_KEY, this.message);
		outState.putString(POS_BUTTON_TEXT_KEY, this.posButtonText);
		outState.putString(NEG_BUTTON_TEXT_KEY, this.negButtonText);
		
		super.onSaveInstanceState(outState);
	}

	
	@Override
	public abstract void onCancel(DialogInterface dialog);
	
	@Override
	public abstract void onClick(DialogInterface dialogInterface, int whichButtonType);
	
	
}