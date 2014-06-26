package com.ziftr.android.onewallet.dialog;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public abstract class OWDialogFragment extends DialogFragment implements OnClickListener {

	/** The title of the dialog. */
	protected String title;
	/** The message of the dialog. */
	protected String message;
	/** The positive button's text. */
	protected String positiveButtonText;
	/** The neutral button's text. */
	protected String neutralButtonText;
	/** The negative button's text. */
	protected String negativeButtonText;
	
	/** The key to save the Dialog's title text in the bundle. */
	private static final String TITLE_KEY = "title";
	/** The key to save the Dialog's message text in the bundle. */
	private static final String MESSAGE_KEY = "message";
	/** The key to save the positive button text in the bundle. */
	private static final String POSITIVE_BUTTON_TEXT_KEY = "yes";
	/** The key to save the neutral button text in the bundle. */
	private static final String NEUTRAL_BUTTON_TEXT_KEY = "yes";
	/** The key to save the negative button text in the bundle. */
	private static final String NEGATIVE_BUTTON_TEXT_KEY = "no";

	/**
	 * Set's up a basic dialog with all of its content.
	 * 
	 * @param title - The Dialog's title.
	 * @param message - The Dialog's message/instructions to user.
	 * @param posButtonText - The 
	 * @param negButtonText
	 */
	public void setupDialog(String title, String message, 
			String posButtonText, String neuButtonText, String negButtonText) {
		this.title = title;
		this.message = message;
		this.positiveButtonText = posButtonText;
		this.neutralButtonText = neuButtonText;
		this.negativeButtonText = negButtonText;
	}
	
	/**
	 * Creates and returns the dialog to show the user.
	 * Sets all the basic text fields that all dialogs have 
	 * to be what this diaglog was set up with. 
	 */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = this.createBuilder(savedInstanceState);
		return builder.create();
	}

	/**
	 * Makes a builder for a basic dialog with all of the common
	 * dialog elements (title, message, etc).
	 * 
	 * @param savedInstanceState - The saved instance state to build from.
	 * @return The new builder
	 */
	public AlertDialog.Builder createBuilder(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (title == null) {
				title = savedInstanceState.getString(TITLE_KEY);
			}
			if (message == null) {
				message = savedInstanceState.getString(MESSAGE_KEY);
			}
			if (positiveButtonText == null) {
				positiveButtonText = savedInstanceState.getString(
						POSITIVE_BUTTON_TEXT_KEY);
			}
			if (neutralButtonText == null) {
				neutralButtonText = savedInstanceState.getString(
						NEUTRAL_BUTTON_TEXT_KEY);
			}
			if (negativeButtonText == null) {
				negativeButtonText = savedInstanceState.getString(
						NEGATIVE_BUTTON_TEXT_KEY);
			}
		}
		
		AlertDialog.Builder builder = 
				new AlertDialog.Builder(this.getActivity());
		
		builder.setTitle(title);
		if (message != null) {
			builder.setMessage(message);
		}
		if (positiveButtonText != null) {
			builder.setPositiveButton(positiveButtonText, this);
		}
		if (neutralButtonText != null) {
			builder.setNeutralButton(this.neutralButtonText, this);
		}
		if (negativeButtonText != null) {
			builder.setNegativeButton(negativeButtonText, this);
		}
		return builder;
	}
	
	/**
	 * When we save the instance, we put the four basic text fields
	 * in the bundle.
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Save all of the important strings in the button
		outState.putString(TITLE_KEY, this.title);
		outState.putString(MESSAGE_KEY, this.message);
		outState.putString(POSITIVE_BUTTON_TEXT_KEY, this.positiveButtonText);
		outState.putString(NEUTRAL_BUTTON_TEXT_KEY, this.neutralButtonText);
		outState.putString(NEGATIVE_BUTTON_TEXT_KEY, this.negativeButtonText);
		
		super.onSaveInstanceState(outState);
	}

}